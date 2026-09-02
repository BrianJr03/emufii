package eu.emufii.app.meta

import android.content.Context
import eu.emufii.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * The same road as `/compat`: unauthenticated GET of a static document on the
 * coordinator, cached on disk, the network answer only ever replacing the cache.
 * Handhelds are offline half the time.
 *
 * Kept out of the compatibility document on purpose: that one is a small verdict read on
 * the first frame by every tile in the grid, this one is paragraphs and picture URLs read
 * by one panel page, and merging them would make every cold start pay for the prose.
 */
object MetaCheck {

    private const val FILE = "meta.json"

    fun cached(context: Context): GameMetaDb = runCatching {
        val file = File(context.filesDir, FILE)
        if (!file.exists()) GameMetaDb.EMPTY else GameMetaDb.parse(file.readText())
    }.getOrDefault(GameMetaDb.EMPTY)

    /** On any failure, the cache is left untouched. */
    suspend fun refresh(
        context: Context,
        baseUrl: String = BuildConfig.COORDINATOR_BASE_URL
    ): GameMetaDb = withContext(Dispatchers.IO) {
        val fetched = runCatching {
            val conn = (URL("$baseUrl/meta").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 6000
            }
            try {
                // 204 = nothing published yet, which is not an error and not a
                // reason to forget a document a server used to publish.
                if (conn.responseCode != 200) return@runCatching null
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }.getOrNull() ?: return@withContext cached(context)

        val parsed = GameMetaDb.parse(fetched)
        if (parsed.size > 0) {
            runCatching { File(context.filesDir, FILE).writeText(fetched) }
        }
        parsed.takeIf { it.size > 0 } ?: cached(context)
    }
}
