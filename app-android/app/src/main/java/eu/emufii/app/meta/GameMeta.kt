package eu.emufii.app.meta

import org.json.JSONObject
import java.util.Locale

/**
 * What a game is about, for the panel's second page. Editorial only: a missing
 * entry costs a page nobody had to open, never a launch, a badge or a session,
 * so every field is nullable and nothing is inferred from silence. Matched on
 * the same keys as the compatibility list ([eu.emufii.app.library.compatKeys]).
 */
data class GameMeta(
    val keys: List<String>,
    /**
     * `action-RPG · cross-over`, in each language the source had one. Two fields
     * rather than one translated at read time: Wikidata already publishes both,
     * and the app has no business guessing that "course" is "racing".
     */
    val genreFr: String? = null,
    val genreEn: String? = null,
    val released: String? = null,
    val summaryFr: String? = null,
    val summaryEn: String? = null,
    val screenshots: List<String> = emptyList(),
    /**
     * Who wrote the paragraph, printed under it. The text is Wikipedia's under
     * CC BY-SA, and attribution is the condition it comes with.
     */
    val source: String? = null,
) {
    /**
     * The paragraph in the player's language, falling back to the other one: an
     * English synopsis still says what the game is, an empty page does not.
     */
    fun summaryFor(locale: Locale): String? =
        if (locale.language.equals("fr", ignoreCase = true)) summaryFr ?: summaryEn
        else summaryEn ?: summaryFr

    /**
     * The genre in the player's language, falling back like the summary. The
     * first only: the line already holds a region, and the catalogue keeps the
     * second so a roomier screen can print it without a rebuild.
     */
    fun genreFor(locale: Locale): String? {
        val both = if (locale.language.equals("fr", ignoreCase = true)) genreFr ?: genreEn
        else genreEn ?: genreFr
        return both?.substringBefore(" · ")
    }

    fun isEmpty(locale: Locale): Boolean =
        genreFor(locale) == null && released == null && screenshots.isEmpty() &&
            summaryFor(locale) == null
}

/**
 * A flat map from key to entry, flattened on parse like
 * [eu.emufii.app.compat.CompatDb]: the lookup happens while a cursor moves.
 */
class GameMetaDb private constructor(private val byKey: Map<String, GameMeta>) {
    val size: Int get() = byKey.size

    fun metaFor(keys: List<String>): GameMeta? = keys.firstNotNullOfOrNull { byKey[it] }

    companion object {
        val EMPTY = GameMetaDb(emptyMap())

        /** Entry by entry, skipping whatever it cannot read: one bad line costs one game. */
        fun parse(json: String): GameMetaDb = runCatching {
            val games = JSONObject(json).optJSONArray("games") ?: return@runCatching EMPTY
            val map = LinkedHashMap<String, GameMeta>()
            for (i in 0 until games.length()) {
                val obj = games.optJSONObject(i) ?: continue
                val keysArray = obj.optJSONArray("keys") ?: continue
                val keys = (0 until keysArray.length())
                    .mapNotNull { keysArray.optString(it).trim().takeIf(String::isNotEmpty) }
                if (keys.isEmpty()) continue
                val shots = obj.optJSONArray("screenshots")
                val entry = GameMeta(
                    keys = keys,
                    genreFr = obj.text("genre_fr"),
                    genreEn = obj.text("genre_en"),
                    released = obj.text("released"),
                    summaryFr = obj.text("summary_fr"),
                    summaryEn = obj.text("summary_en"),
                    source = obj.text("source"),
                    // https only: a picture fetched in clear is one anybody can
                    // replace, and this is drawn full width on the back panel.
                    screenshots = (0 until (shots?.length() ?: 0))
                        .mapNotNull { shots?.optString(it)?.trim() }
                        .filter { it.startsWith("https://") }
                        .take(4),
                )
                for (key in keys) map.putIfAbsent(key, entry)
            }
            GameMetaDb(map)
        }.getOrDefault(EMPTY)

        private fun JSONObject.text(key: String): String? =
            optString(key).takeIf { it.isNotBlank() && it != "null" }
    }
}

/**
 * Empty by default, which is the state before the first fetch and the permanent
 * state of an offline player: the second page says it has nothing.
 */
val LocalGameMetaDb = androidx.compose.runtime.staticCompositionLocalOf { GameMetaDb.EMPTY }
