package eu.emufii.app.compat

import android.content.Context
import eu.emufii.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Served rather than shipped: a rating given today has to reach a build from three
 * months ago. Unauthenticated GET, like `/latest`, the fact being public.
 *
 * The library reads the disk cache, the network answer only replaces it: badges have to
 * be there on the first frame of a cold start, and on a handheld that is often offline a
 * missing badge reads as "this game is fine".
 */
object CompatCheck {

    private const val FILE = "compat.json"

    fun cached(context: Context): CompatDb = runCatching {
        val file = File(context.filesDir, FILE)
        if (!file.exists()) CompatDb.EMPTY else CompatDb.parse(file.readText())
    }.getOrDefault(CompatDb.EMPTY)

    /**
     * On any failure the cache is returned untouched: a server that is down, a captive
     * portal answering HTML or a truncated read are no reason to forget what we knew.
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
                // 204 = nothing published yet: not an error, and not a reason to drop a
                // cache from a server that used to publish.
                if (conn.responseCode != 200) return@runCatching null
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }.getOrNull() ?: return@withContext cached(context)

        val parsed = CompatDb.parse(fetched)
        // Overwriting a working cache with something that parsed to nothing would turn a
        // server-side typo into every badge in the app disappearing.
        if (parsed.size > 0) {
            runCatching { File(context.filesDir, FILE).writeText(fetched) }
        }
        parsed.takeIf { it.size > 0 } ?: cached(context)
    }
}
