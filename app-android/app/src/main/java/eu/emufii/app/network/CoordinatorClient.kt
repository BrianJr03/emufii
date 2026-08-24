package eu.emufii.app.network

import eu.emufii.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import eu.emufii.app.wg.WgTunnelInfo
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import eu.emufii.app.profile.Profile

/**
 * debug  → host Mac loopback seen from an AVD (cleartext, allowed by
 *          network_security_config for that host only)
 * release → hosted coordinator over HTTPS; override at build time with
 *          -Pemufii.coordinatorUrl=https://...
 */
val COORDINATOR_BASE_URL: String = BuildConfig.COORDINATOR_BASE_URL

/**
 * A freshly created session, and the secret proving we are its host. The code
 * is public (the finder publishes it), so the token is what authorises.
 * Returned here only, at creation, and it never leaves the device.
 * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Un jeton, parce que le code de session est public
 */
data class CreatedSession(
    val code: String,
    val subnet: String,
    val token: String,
    /** The room brought up for this session, or null. See [RoomRef]. */
    val room: RoomRef? = null
)

/**
 * Why a call to the coordinator failed. The distinction is the player's, not
 * the log's: a missing code is theirs to fix, an unreachable server is ours.
 * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Distinguer « ça n'existe pas » de « je n'ai pas pu demander »
 */
sealed class CoordinatorError(message: String) : Exception(message) {
    /** 404: no such session, or one whose TTL has passed and been purged. */
    object NotFound : CoordinatorError("session introuvable")

    /** Nothing answered: no network, DNS failure, TLS failure, timeout. */
    class Unreachable(cause: Throwable) : CoordinatorError(cause.message ?: "injoignable")

    /** Answered, but not with a success: full, rate-limited, broken. */
    class Http(val status: Int) : CoordinatorError("HTTP $status")
}

data class Member(val id: String, val name: String, val forSeconds: Int)

/**
 * What a heartbeat returns: who is there, and the means to remove oneself.
 *
 * [memberHandle] is how this session lists us — compare against it, not against
 * a friend code. [memberToken] arrives on the FIRST heartbeat only.
 * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Un jeton, parce que le code de session est public
 */
data class Heartbeat(
    val players: Int,
    val memberToken: String?,
    val memberHandle: String?
)

/**
 * The Eden room the coordinator holds for this session, on the VPS: both
 * players join it, so nobody hosts on a phone. Null elsewhere, and null when
 * none is offered — the app then falls back on hosting by a player.
 * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Le salon Eden sur le VPS change la forme d'une partie Switch
 */
data class RoomRef(val host: String, val port: Int, val password: String)

data class RemoteSession(
    val code: String,
    val subnet: String,
    val hostIp: String?,
    val port: Int?,
    val romTitleId: String?,
    val romTitle: String?,
    val hostName: String?,
    val room: RoomRef?,
    /**
     * Has the host opened its room yet? **True** by default when the field is
     * missing: the opposite would block every guest until deployment.
     * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Les défauts d'un champ absent sont choisis dans un sens précis
     */
    val hostReady: Boolean,
    val members: List<Member>
)

/**
 * A friend the coordinator currently sees. No "online" flag: being present at
 * all is the signal.
 * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Les défauts d'un champ absent sont choisis dans un sens précis
 */
data class FriendPresence(
    val name: String?,
    val sessionCode: String?,
    val romTitle: String?,
    val romTitleId: String?,
    val players: Int,
    val ready: Boolean
)

/** A session as the finder sees it, no network id, that comes with joining. */
data class OpenSession(
    val code: String,
    val romTitle: String?,
    val romTitleId: String?,
    val hostName: String?,
    val players: Int,
    val ready: Boolean,
    val ageSeconds: Int
)

class CoordinatorClient(private val baseUrl: String = COORDINATOR_BASE_URL) {

    suspend fun createSession(
        code: String,
        romTitleId: String?,
        romTitle: String?,
        hostName: String? = null,
        hostId: String? = null,
        /**
         * The console, sent explicitly: the coordinator sees only a title and a
         * titleId, which 3DS and Switch write the same way.
         * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Les défauts d'un champ absent sont choisis dans un sens précis
         */
        console: String? = null,
        /**
         * A private session does not appear in the finder. Sent only when true;
         * absence means public.
         * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Les défauts d'un champ absent sont choisis dans un sens précis
         */
        private: Boolean = false
    ): Result<CreatedSession> = request(
        path = "/sessions",
        method = "POST",
        body = JSONObject().apply {
            put("code", code)
            if (romTitleId != null) put("rom_title_id", romTitleId)
            if (romTitle != null) put("rom_title", romTitle)
            if (hostName != null) put("host_name", hostName)
            if (hostId != null) put("host_id", hostId)
            if (console != null) put("console", console)
            if (private) put("private", true)
        },
        readTimeout = 15_000
    ).map { text ->
        val json = JSONObject(text)
        CreatedSession(
            json.getString("code"),
            json.getString("subnet"),
            json.optString("token"),
            json.roomOrNull()
        )
    }

    suspend fun patchSession(
        code: String,
        hostIp: String,
        port: Int,
        token: String?
    ): Result<Unit> = request(
        path = "/sessions/$code",
        method = "PATCH",
        body = JSONObject().apply {
            put("host_ip", hostIp)
            put("port", port)
        },
        bearer = token
    ).map { }

    /**
     * States that the host's room exists, or no longer does. Only the host may
     * say so, and only the host has the answer.
     * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Un jeton, parce que le code de session est public
     */
    suspend fun setHostReady(
        code: String,
        ready: Boolean,
        token: String?
    ): Result<Unit> = request(
        path = "/sessions/$code",
        method = "PATCH",
        body = JSONObject().apply { put("host_ready", ready) },
        bearer = token
    ).map { }

    suspend fun getSession(code: String): Result<RemoteSession> =
        request(path = "/sessions/$code", method = "GET").map { text ->
            val json = JSONObject(text)
            RemoteSession(
                code = json.getString("code"),
                subnet = json.getString("subnet"),
                hostIp = json.stringOrNull("host_ip"),
                port = json.intOrNull("port"),
                romTitleId = json.stringOrNull("rom_title_id"),
                romTitle = json.stringOrNull("rom_title"),
                hostName = json.stringOrNull("host_name"),
                room = json.roomOrNull(),
                hostReady = json.optBoolean("host_ready", true),
                members = json.optJSONArray("members").map { m ->
                    Member(
                        id = m.getString("id"),
                        name = m.optString("name", Profile.DEFAULT_NAME),
                        forSeconds = m.optInt("for_s", 0)
                    )
                }
            )
        }

    /** Everything joinable right now. Powers the session finder. */
    suspend fun listSessions(): Result<List<OpenSession>> =
        request(path = "/sessions", method = "GET").map { text ->
            JSONObject(text).optJSONArray("sessions").map { s ->
                OpenSession(
                    code = s.getString("code"),
                    romTitle = s.stringOrNull("rom_title"),
                    romTitleId = s.stringOrNull("rom_title_id"),
                    hostName = s.stringOrNull("host_name"),
                    players = s.optInt("players", 0),
                    ready = s.optBoolean("ready", false),
                    ageSeconds = s.optInt("age_s", 0)
                )
            }
        }

    /**
     * The session heartbeat: the coordinator drops members that fall silent, so
     * this repeats for as long as the session lasts. See [Heartbeat].
     */
    suspend fun heartbeat(code: String, id: String, name: String): Result<Heartbeat> = request(
        path = "/sessions/$code/members",
        method = "POST",
        body = JSONObject().apply {
            put("id", id)
            put("name", name)
        }
    ).map { text ->
        val json = JSONObject(text)
        Heartbeat(
            json.optInt("players", 0),
            json.optString("member_token").ifBlank { null },
            json.optString("member_handle").ifBlank { null }
        )
    }

    /** [token]: the one received on joining, or the host's if it is clearing up. */
    suspend fun leaveSession(code: String, id: String, token: String?): Result<Unit> =
        request(path = "/sessions/$code/members/$id", method = "DELETE", bearer = token).map { }

    suspend fun deleteSession(code: String, token: String?): Result<Unit> =
        request(path = "/sessions/$code", method = "DELETE", readTimeout = 8000, bearer = token)
            .map { }

    /**
     * Say we're here, so friends holding our code can see it. Only needed
     * outside a session, where [heartbeat] already does it.
     * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § La présence hors session, et pourquoi elle s'éteint dedans
     */
    suspend fun announcePresence(
        id: String,
        name: String,
        inSession: Boolean = false
    ): Result<Unit> = request(
        path = "/me",
        method = "POST",
        body = JSONObject().apply {
            put("id", id)
            put("name", name)
            put("in_session", inSession)
        }
    ).map { }

    /**
     * Ask which of these friends are online. Only the codes we send can come
     * back: there is no listing route and no directory behind this.
     * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Les défauts d'un champ absent sont choisis dans un sens précis
     */
    suspend fun friendStatuses(codes: List<String>): Result<Map<String, FriendPresence>> {
        if (codes.isEmpty()) return Result.success(emptyMap())
        return request(
            path = "/friends",
            method = "POST",
            body = JSONObject().apply { put("ids", JSONArray(codes)) }
        ).map { text ->
            JSONObject(text).optJSONArray("friends").map { f ->
                val session = f.optJSONObject("session")
                f.getString("id") to FriendPresence(
                    name = f.stringOrNull("name"),
                    sessionCode = session?.stringOrNull("code"),
                    romTitle = session?.stringOrNull("rom_title"),
                    romTitleId = session?.stringOrNull("rom_title_id"),
                    players = session?.optInt("players", 0) ?: 0,
                    ready = session?.optBoolean("ready", false) ?: false
                )
            }.toMap()
        }
    }

    /**
     * Claims this device's address, presenting the WireGuard public key.
     * Idempotent on the key, so a retry lands on the same address.
     * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Réclamer une adresse est idempotent sur la clé
     */
    suspend fun claimAddress(
        code: String,
        publicKey: String,
        name: String? = null,
        profileId: String? = null
    ): Result<WgTunnelInfo> = request(
        path = "/sessions/$code/peers",
        method = "POST",
        body = JSONObject().apply {
            put("public_key", publicKey)
            if (name != null) put("name", name)
            if (profileId != null) put("id", profileId)
        },
        readTimeout = 15_000
    ).map { text ->
        val json = JSONObject(text)
        val relay = json.optJSONObject("relay")
            ?: error("le coordinator n'a pas de relais configuré")
        WgTunnelInfo(
            address = json.getString("ip"),
            // `isNull`, never `optString`: the latter returns the string
            // "null" on a JSON null.
            // pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Réclamer une adresse est idempotent sur la clé
            hairpinAddress = if (json.isNull("hairpin_ip")) null
            else json.optString("hairpin_ip").takeIf { it.isNotBlank() },
            subnet = json.getString("subnet"),
            relayEndpoint = relay.getString("endpoint"),
            relayPublicKey = relay.getString("public_key"),
            relayAllowedIps = relay.getString("allowed_ips")
        )
    }

    // -- plumbing --

    private suspend fun request(
        path: String,
        method: String,
        body: JSONObject? = null,
        readTimeout: Int = 4000,
        bearer: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = body?.toString()
            val conn = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 4000
                this.readTimeout = readTimeout
                if (body != null) {
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    doOutput = true
                }
                if (bearer != null) setRequestProperty("Authorization", "Bearer $bearer")
                // What tells Emufii apart from any other client. A build with no
                // key sends nothing and talks to a dev coordinator, which demands
                // nothing. See `ClientAuth`.
                ClientAuth.sign(method, path, payload)?.let { s ->
                    setRequestProperty(ClientAuth.HEADER_AUTH, s.value)
                    setRequestProperty(ClientAuth.HEADER_TIMESTAMP, s.timestamp)
                    setRequestProperty(ClientAuth.HEADER_CLIENT, ClientAuth.clientVersion)
                }
            }
            try {
                // `payload` and not `body.toString()`: the signature covers
                // those bytes, and two successive serialisations of the same
                // JSONObject are under no obligation to match.
                payload?.let { conn.outputStream.use { out -> out.write(it.toByteArray(Charsets.UTF_8)) } }
                val status = conn.responseCode
                when {
                    status == 404 -> throw CoordinatorError.NotFound
                    status !in 200..299 -> throw CoordinatorError.Http(status)
                    // 204 has no body, and reading it would throw.
                    status == 204 || conn.contentLength == 0 -> ""
                    else -> conn.inputStream.bufferedReader().use { it.readText() }
                }
            } finally {
                conn.disconnect()
            }
        }.recoverCatching { err ->
            // Everything that is not already a verdict on the answer is a
            // failure to get one at all: `openConnection`, `responseCode` and
            // the body read all surface as IOException when nothing answers.
            throw if (err is CoordinatorError) err else CoordinatorError.Unreachable(err)
        }
    }

    private fun JSONObject.stringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null

    /**
     * The room, or null. An **incomplete** room counts as no room: all three
     * fields are needed to dial.
     * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Le salon Eden sur le VPS change la forme d'une partie Switch
     */
    private fun JSONObject.roomOrNull(): RoomRef? {
        val r = optJSONObject("room") ?: return null
        val host = r.stringOrNull("host") ?: return null
        val port = r.intOrNull("port") ?: return null
        val password = r.stringOrNull("password") ?: return null
        return RoomRef(host, port, password)
    }

    private fun JSONObject.intOrNull(key: String): Int? =
        if (has(key) && !isNull(key)) getInt(key) else null

    private fun <T> JSONArray?.map(transform: (JSONObject) -> T): List<T> =
        if (this == null) emptyList() else (0 until length()).map { transform(getJSONObject(it)) }
}
