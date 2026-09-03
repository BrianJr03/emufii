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
 * Served and cached like `/compat` and `/meta`. The overlay replaces a name derived from
 * the filename, never one read out of the file.
 * pourquoi : docs/decisions/scan-bibliotheque.md § A name the file does not give is asked of the index
 */
object GameTitles {

    private fun fileFor(lang: String) = "game_titles-$lang.json"

    @Volatile
    private var caches: Map<String, Map<String, String>> = emptyMap()

    /** Never touches the network. */
    fun cached(context: Context, lang: String = TitleLanguage.tag): Map<String, String> {
        caches[lang]?.let { return it }
        val read = runCatching {
            val file = File(context.filesDir, fileFor(lang))
            if (file.exists()) parse(file.readText()) else emptyMap()
        }.getOrDefault(emptyMap())
        caches = caches + (lang to read)
        return read
    }

    fun apply(titles: Map<String, String>, rom: Rom): Rom {
        if (titles.isEmpty()) return rom
        val name = resolve(titles, rom.displayName, rom.filename, rom.compatKeys())
            ?: return rom
        return rom.copy(displayName = name)
    }

    /** Split out of [Rom] like `compatKeys`: pure string work, no `Uri` to drag into a test. */
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
     * A title the index does not know is asked for again next launch: remembering the
     * absence would mean a second cache to invalidate when a title is published.
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
        val answer = HashMap<String, String>()
        // A batch that comes back empty does not sink the others: what did arrive is
        // kept, and a key with no answer is asked again next launch regardless.
        for (batch in batches(keys)) {
            answer += fetch(baseUrl, lang, batch) ?: continue
        }
        if (answer.isEmpty()) return@withContext false

        val merged = known + answer
        runCatching {
            val out = JSONObject()
            for ((k, v) in merged) out.put(k, v)
            File(context.filesDir, fileFor(lang)).writeText(out.toString())
        }
        caches = caches + (lang to merged)

        roms.any { apply(merged, it).displayName != it.displayName }
    }

    /**
     * Two ceilings, and one request honoured neither. The coordinator answers the first
     * 500 keys and says nothing about the rest, and Node shuts the connection past 16 KB
     * of request line. Measured on the VPS on 2026-09-03: a library of 1717 keys made an
     * URL of 21657 characters and came back with nothing at all. Between the two limits
     * is the worse case, an answer that looks whole and is not.
     */
    private const val KEYS_PER_REQUEST = 400

    /** Null on anything but a 200: an unreachable index reads as "we do not know". */
    private fun fetch(baseUrl: String, lang: String, keys: List<String>): Map<String, String>? =
        runCatching {
            val query = URLEncoder.encode(keys.joinToString(","), "UTF-8")
            val conn = (URL("$baseUrl/titles?lang=$lang&keys=$query").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 6000
            }
            try {
                if (conn.responseCode != 200) return@runCatching null
                parse(conn.inputStream.bufferedReader().use { it.readText() })
            } finally {
                conn.disconnect()
            }
        }.getOrNull()

    /** Split out like [resolve]: the batching is a rule to pin, not a network call. */
    fun batches(keys: List<String>): List<List<String>> = keys.chunked(KEYS_PER_REQUEST)

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
