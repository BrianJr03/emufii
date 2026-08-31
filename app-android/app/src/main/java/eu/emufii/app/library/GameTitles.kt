package eu.emufii.app.library

import android.content.Context
import eu.emufii.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * The name of a game the file would not give up itself, for every console, in the app's
 * own language. Served and cached like `/compat` and `/meta`. The overlay only replaces
 * a name derived from the filename, never one read out of the file.
 * pourquoi : docs/decisions/scan-bibliotheque.md § A name the file does not give is asked of the index
 */
object GameTitles {

    /** One cache file per language: two languages of one game are two strings. */
    private fun fileFor(lang: String) = "game_titles-$lang.json"

    /**
     * Process-wide, like the scanned list: the cache belongs to the process,
     * and both languages stay loaded because the app can be switched between
     * them without one.
     */
    @Volatile
    private var caches: Map<String, Map<String, String>> = emptyMap()

    /** The cached copy for a language, or nothing. Never touches the network. */
    fun cached(context: Context, lang: String = TitleLanguage.tag): Map<String, String> {
        caches[lang]?.let { return it }
        val read = runCatching {
            val file = File(context.filesDir, fileFor(lang))
            if (file.exists()) parse(file.readText()) else emptyMap()
        }.getOrDefault(emptyMap())
        caches = caches + (lang to read)
        return read
    }

    /** The overlay itself: only a filename-derived display name is replaced. */
    fun apply(titles: Map<String, String>, rom: Rom): Rom {
        if (titles.isEmpty()) return rom
        val name = resolve(titles, rom.displayName, rom.filename, rom.compatKeys())
            ?: return rom
        return rom.copy(displayName = name)
    }

    /**
     * The decision, split out from [Rom] for the same reason `compatKeys`
     * is: pure string work, no Android in it, and no `Uri` dragged into a test
     * of an overlay rule that has nothing to do with files.
     */
    fun resolve(
        titles: Map<String, String>,
        displayName: String,
        filename: String,
        keys: List<String>
    ): String? {
        if (displayName != displayNameFromFilename(filename)) return null
        return keys.firstNotNullOfOrNull { titles[it] }
    }

    /**
     * Asks the coordinator for the titles this library is missing, in the
     * app's language, merges the answer into the cache, and returns whether
     * any tile changes: the caller re-reads the (process-cached) list rather
     * than us pushing state at it.
     *
     * A game the index does not know is asked for again next launch: the
     * request is one GET of a handful of keys, and remembering the *absence*
     * would mean a second cache that a newly published title must invalidate.
     * Failure to reach the server keeps the cache untouched, like everywhere
     * else.
     */
    suspend fun refresh(
        context: Context,
        roms: List<Rom>,
        baseUrl: String = BuildConfig.COORDINATOR_BASE_URL
    ): Boolean = withContext(Dispatchers.IO) {
        val lang = TitleLanguage.tag
        val keys = roms
            .filter { it.displayName == displayNameFromFilename(it.filename) }
            .flatMap { it.compatKeys() }
            .distinct()
        if (keys.isEmpty()) return@withContext false

        val known = cached(context, lang)
        val fetched = runCatching {
            val query = URLEncoder.encode(keys.joinToString(","), "UTF-8")
            val conn = (URL("$baseUrl/titles?lang=$lang&keys=$query").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 6000
            }
            try {
                if (conn.responseCode != 200) return@runCatching null
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }.getOrNull() ?: return@withContext false

        val answer = runCatching { parse(fetched) }.getOrDefault(emptyMap())
        if (answer.isEmpty()) return@withContext false

        // Written as one JSON object so the file is its own whole document,
        // readable by nothing fancier than a JSONObject on the way back.
        val merged = known + answer
        runCatching {
            val out = JSONObject()
            for ((k, v) in merged) out.put(k, v)
            File(context.filesDir, fileFor(lang)).writeText(out.toString())
        }
        caches = caches + (lang to merged)

        roms.any { apply(merged, it).displayName != it.displayName }
    }

    private fun parse(raw: String): Map<String, String> = runCatching {
        val obj = JSONObject(raw)
        val titlesField = obj.optJSONObject("titles") ?: obj
        val out = HashMap<String, String>()
        for (key in titlesField.keys()) {
            val name = titlesField.optString(key)
            if (name.isNotBlank()) out[key] = name
        }
        out
    }.getOrDefault(emptyMap())
}
