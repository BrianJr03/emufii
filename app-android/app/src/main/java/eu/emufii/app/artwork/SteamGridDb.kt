package eu.emufii.app.artwork

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Where the app goes to find real game icons.
 *
 * Les icones, jamais les jaquettes verticales. Rien n'est empaquete dans l'APK,
 * et chaque joueur apporte sa cle : sans cle, aucune requete ne part et les
 * tuiles gardent leur icone embarquee.
 * pourquoi : docs/decisions/jaquettes.md § L'icône, jamais la jaquette verticale
 * pourquoi : docs/decisions/jaquettes.md § Chaque joueur apporte sa propre clé
 */
/** A game from the catalogue, as offered to the player fixing the match. */
data class SgdbGame(val id: Int, val name: String)

/** A candidate icon. [thumb] serves the picker grid, [url] the tile. */
data class SgdbIcon(val url: String, val thumb: String, val px: Int)

object SteamGridDb {

    private const val BASE = "https://www.steamgriddb.com/api/v2"
    private const val TIMEOUT_MS = 6000

    /**
     * Below this, the remote icon is not worth downloading: the tiles are
     * ~150 dp, so a 64 px icon would be as blurry as the ROM's, with a network
     * round trip on top.
     */
    private const val MIN_PX = 128

    /**
     * The URL of the best icon for this title, or null.
     *
     * Null means "no icon", never "error": network down, quota exceeded, unknown
     * title, everything answers the same way because the caller has nothing
     * different to do with it. A grid of games is not the place to report that a
     * third-party API hiccupped.
     */
    suspend fun iconUrl(title: String, apiKey: String): String? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        runCatching {
            val gameId = searchGameId(title, apiKey) ?: return@runCatching null
            bestIconUrl(gameId, apiKey)
        }.getOrNull()
    }

    /**
     * Every game the catalogue offers for these words, in its own order.
     *
     * Serves the manual picker: automatic matching takes the first result, and
     * that is precisely where it goes wrong. A game with an ambiguous title, a
     * sequel, a port, a regional subtitle, can only be fixed by someone who
     * recognises the right one.
     */
    suspend fun searchGames(query: String, apiKey: String): List<SgdbGame> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank() || query.isBlank()) return@withContext emptyList()
            runCatching {
                val q = URLEncoder.encode(searchTerm(query), "UTF-8")
                val json = get("$BASE/search/autocomplete/$q", apiKey)
                    ?: return@runCatching emptyList()
                val data = json.optJSONArray("data") ?: return@runCatching emptyList()
                (0 until data.length()).mapNotNull { i ->
                    val item = data.optJSONObject(i) ?: return@mapNotNull null
                    val id = item.optInt("id").takeIf { it != 0 } ?: return@mapNotNull null
                    SgdbGame(id, item.optString("name").ifBlank { "#$id" })
                }
            }.getOrDefault(emptyList())
        }

    /**
     * Every icon for a game, largest first.
     *
     * Without [bestIconUrl]'s size filter, deliberately: here it is the player's
     * eye that decides, and hiding an icon from them because it is small would be
     * deciding on their behalf. The automatic choice has nobody to judge and has
     * to stay cautious.
     */
    suspend fun icons(gameId: Int, apiKey: String): List<SgdbIcon> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext emptyList()
            runCatching {
                val url = "$BASE/icons/game/$gameId?types=static&nsfw=false&humor=false"
                val json = get(url, apiKey) ?: return@runCatching emptyList()
                val data = json.optJSONArray("data") ?: return@runCatching emptyList()
                (0 until data.length()).mapNotNull { i ->
                    val item = data.optJSONObject(i) ?: return@mapNotNull null
                    val link = item.optString("url").takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null
                    SgdbIcon(
                        url = link,
                        thumb = item.optString("thumb").ifBlank { link },
                        px = minOf(item.optInt("width", 0), item.optInt("height", 0))
                    )
                }.sortedByDescending { it.px }
            }.getOrDefault(emptyList())
        }

    /** The first autocomplete result, which is also the best ranked. */
    private fun searchGameId(title: String, apiKey: String): Int? {
        val query = URLEncoder.encode(searchTerm(title), "UTF-8")
        val json = get("$BASE/search/autocomplete/$query", apiKey) ?: return null
        val data = json.optJSONArray("data") ?: return null
        if (data.length() == 0) return null
        return data.getJSONObject(0).optInt("id").takeIf { it != 0 }
    }

    /**
     * The largest static icon, ties broken by the community's rating.
     *
     * We sort on size first and only then on rating, because the one flaw we are
     * trying to fix is resolution: a much-loved icon rendered at 64 px would fix
     * nothing.
     *
     * `types=static` rules out animated ones, twenty tiles moving together being
     * unreadable, and on a handheld that is battery for nothing. `nsfw` and
     * `humor` are ruled out: a player's library is not the place for a surprise.
     */
    private fun bestIconUrl(gameId: Int, apiKey: String): String? {
        val url = "$BASE/icons/game/$gameId?types=static&nsfw=false&humor=false"
        val json = get(url, apiKey) ?: return null
        val data = json.optJSONArray("data") ?: return null
        var bestUrl: String? = null
        var bestPx = 0
        var bestScore = Int.MIN_VALUE
        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i) ?: continue
            val link = item.optString("url").takeIf { it.isNotBlank() } ?: continue
            val px = minOf(item.optInt("width", 0), item.optInt("height", 0))
            if (px < MIN_PX) continue
            val score = item.optInt("score", 0)
            if (px > bestPx || (px == bestPx && score > bestScore)) {
                bestPx = px
                bestScore = score
                bestUrl = link
            }
        }
        return bestUrl
    }

    /**
     * What we send off to search for, built from the displayed name.
     *
     * Dumps drag their origin along in their name, `(USA)`, `(Europe)`, `[!]`,
     * `(Rev 1)`, and those marks are in no game catalogue. Leaving them in means
     * searching for a title that does not exist and finding nothing. The console
     * suffix (`3D`, `3DS`) is kept: it is often part of the real title ("Ocarina
     * of Time 3D").
     */
    internal fun searchTerm(title: String): String =
        title
            .replace(Regex("""[\(\[][^)\]]*[\)\]]"""), " ")
            .replace(Regex("""[._]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun get(url: String, apiKey: String): JSONObject? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Authorization", "Bearer $apiKey")
        }
        return try {
            if (conn.responseCode != 200) return null
            JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }
}
