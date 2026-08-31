package eu.emufii.app.compat

import android.content.Context
import eu.emufii.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Served rather than shipped: a rating given today has to reach a build from
 * three months ago, and baking it in would mean a release per verdict. An
 * unauthenticated GET, like `/latest`, since it is the same public fact for
 * everybody.
 *
 * Cached on disk, and the cache is what the library reads; the network answer
 * only replaces it. Badges have to be there on the first frame of a cold start,
 * and these are handhelds that are frequently offline: a list that vanishes
 * without Wi-Fi is worse than none, its absence reading as "this game is fine".
 */
object CompatCheck {

    private const val FILE = "compat.json"

    /** The cached copy, or an empty database. Never touches the network. */
    fun cached(context: Context): CompatDb = runCatching {
        val file = File(context.filesDir, FILE)
        if (!file.exists()) CompatDb.EMPTY else CompatDb.parse(file.readText())
    }.getOrDefault(CompatDb.EMPTY)

    /**
     * Fetches the database and replaces the cache, returning what should now be
     * displayed.
     *
     * On any failure it returns the cache untouched. A server that is down, a
     * captive portal answering HTML, a truncated read: none of them are reasons
     * to forget what we already knew.
     */
    suspend fun refresh(
        context: Context,
        baseUrl: String = BuildConfig.COORDINATOR_BASE_URL
    ): CompatDb = withContext(Dispatchers.IO) {
        val fetched = runCatching {
            val conn = (URL("$baseUrl/compat").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
            }
            try {
                // 204 = nothing published yet, which is not an error and not a
                // reason to drop a cache from a server that used to publish.
                if (conn.responseCode != 200) return@runCatching null
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }.getOrNull() ?: return@withContext cached(context)

        val parsed = CompatDb.parse(fetched)
        // Only a document we could actually read gets written. Overwriting a
        // working cache with something that parsed to nothing would turn a
        // server-side typo into every badge in the app disappearing.
        if (parsed.size > 0) {
            runCatching { File(context.filesDir, FILE).writeText(fetched) }
        }
        parsed.takeIf { it.size > 0 } ?: cached(context)
    }
}
