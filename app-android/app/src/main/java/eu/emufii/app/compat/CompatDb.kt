package eu.emufii.app.compat

import org.json.JSONObject

/**
 * How well a game runs through Emufii. Four levels: a finer scale would demand a
 * judgement nobody can make consistently across seven consoles.
 */
enum class CompatRating {
    PERFECT,

    /** Runs, with something to know first: slowdowns, a mode that fails. */
    PARTIAL,

    BROKEN,

    /**
     * Has multiplayer Emufii could carry, untried. Distinct from being absent
     * from the database, which says nothing at all.
     */
    UNTESTED;

    companion object {
        fun fromName(name: String?): CompatRating? = when (name?.lowercase()) {
            "perfect" -> PERFECT
            "partial" -> PARTIAL
            "broken" -> BROKEN
            "untested" -> UNTESTED
            else -> null
        }
    }
}

/**
 * What is known about one game, in every region. [name] is for the tool and for
 * a human reading the file; matching is on [keys] alone (`compatKeys`), since a
 * title changes with the language.
 */
data class CompatEntry(
    val name: String,
    val rating: CompatRating,
    val note: String? = null,
    val keys: List<String>
)

/**
 * A flat map from key to verdict, flattened on parse: the library draws hundreds
 * of tiles per scroll and each one asks this question.
 */
class CompatDb private constructor(
    private val byKey: Map<String, CompatEntry>
) {
    val size: Int get() = byKey.size

    /**
     * Null when nothing is known. Keys arrive most specific first and the first
     * hit wins, so a region's rating beats its family's.
     */
    fun ratingFor(keys: List<String>): CompatEntry? = keys.firstNotNullOfOrNull { byKey[it] }

    companion object {
        val EMPTY = CompatDb(emptyMap())

        /**
         * Entry by entry, never all-or-nothing: the file is hand-edited too, and
         * one malformed line must cost one game. An unknown rating is skipped
         * rather than defaulted; defaulting would invent a verdict.
         */
        fun parse(json: String): CompatDb = runCatching {
            val games = JSONObject(json).optJSONArray("games") ?: return@runCatching EMPTY
            val map = LinkedHashMap<String, CompatEntry>()
            for (i in 0 until games.length()) {
                val obj = games.optJSONObject(i) ?: continue
                val rating = CompatRating.fromName(obj.optString("rating")) ?: continue
                val keysArray = obj.optJSONArray("keys") ?: continue
                val keys = (0 until keysArray.length())
                    .mapNotNull { keysArray.optString(it).trim().takeIf(String::isNotEmpty) }
                if (keys.isEmpty()) continue
                val entry = CompatEntry(
                    name = obj.optString("name").ifBlank { keys.first() },
                    rating = rating,
                    note = obj.optString("note").takeIf { it.isNotBlank() && it != "null" },
                    keys = keys
                )
                // First writer wins, so a duplicated key is a no-op rather than a
                // silent change of verdict depending on file order.
                for (key in keys) map.putIfAbsent(key, entry)
            }
            CompatDb(map)
        }.getOrDefault(EMPTY)
    }
}

/**
 * A `CompositionLocal` because the grid, the list and the carousel draw the same
 * tile at three different depths. Empty by default: no badge means nothing is
 * known, never that it works.
 */
val LocalCompatDb = androidx.compose.runtime.staticCompositionLocalOf { CompatDb.EMPTY }
