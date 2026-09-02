package eu.emufii.app.artwork

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Where the app goes for real game icons. Icons, never vertical cover art. Nothing is
 * bundled in the APK and each player brings their own key: with none, no request leaves
 * and the tiles keep their built-in icon.
 * pourquoi : docs/decisions/jaquettes.md § The icon, never the box art
 * pourquoi : docs/decisions/jaquettes.md § Every player brings their own key
 */
data class SgdbGame(val id: Int, val name: String)

data class SgdbIcon(val url: String, val thumb: String, val px: Int)

object SteamGridDb {

    private const val BASE = "https://www.steamgriddb.com/api/v2"
    private const val TIMEOUT_MS = 6000

    /** The tiles are ~150 dp: a 64 px icon would be as blurry as the ROM's, with a
     * network round trip on top. */
    private const val MIN_PX = 128

    /**
     * Null means "no icon", never "error": network down, quota exceeded, unknown
     * title all answer the same way, the caller having nothing different to do.
     */
    suspend fun iconUrl(title: String, apiKey: String): String? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        runCatching {
            val gameId = searchGameId(title, apiKey) ?: return@runCatching null
            bestIconUrl(gameId, apiKey)
        }.getOrNull()
    }

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
     * Largest first, and deliberately without [bestIconUrl]'s size filter: here the
     * player's eye decides, where the automatic choice has nobody to judge.
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

    private fun searchGameId(title: String, apiKey: String): Int? {
        val query = URLEncoder.encode(searchTerm(title), "UTF-8")
        val json = get("$BASE/search/autocomplete/$query", apiKey) ?: return null
        val data = json.optJSONArray("data") ?: return null
        if (data.length() == 0) return null
        return data.getJSONObject(0).optInt("id").takeIf { it != 0 }
    }

    /**
     * Size first, rating only to break ties: the flaw being fixed is resolution, and
     * a much-loved icon at 64 px fixes nothing. `types=static` rules out animated
     * ones, twenty tiles moving together being unreadable and battery for nothing.
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
     * Dumps drag their origin along, `(USA)`, `[!]`, `(Rev 1)`, and no game catalogue
     * carries those marks. The console suffix (`3D`, `3DS`) is kept: it is often part
     * of the real title ("Ocarina of Time 3D").
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
