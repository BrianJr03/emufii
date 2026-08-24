package eu.emufii.app.meta

import org.json.JSONObject
import java.util.Locale

/**
 * What a game is about, for the panel's second page.
 *
 * Everything here is *editorial*: a paragraph, a date, a genre, a few pictures.
 * None of it is ever needed to play, and that is the rule that shapes the whole
 * file — an entry that is missing costs a page nobody had to open, never a
 * launch, a badge or a session. So every field is nullable, nothing is inferred
 * from silence, and the app has to be perfectly usable with the whole database
 * absent.
 *
 * Matched on the same keys as the compatibility list ([eu.emufii.app.library.compatKeys]),
 * for the same reason: a title is the one thing that changes with the region and
 * the language, so it cannot be the identity.
 */
data class GameMeta(
    val keys: List<String>,
    /**
     * `action-RPG · cross-over`, in each language the source had one.
     *
     * Two fields rather than one translated at read time: a genre is a word
     * somebody wrote, and Wikidata already publishes it in both languages. The
     * app has no business guessing that "course" is "racing".
     */
    val genreFr: String? = null,
    val genreEn: String? = null,
    /** ISO-ish, `2004-11-16` or just `2004`: whatever the source could say. */
    val released: String? = null,
    val summaryFr: String? = null,
    val summaryEn: String? = null,
    /** Absolute URLs, in the order they are shown. Usually two or three. */
    val screenshots: List<String> = emptyList(),
    /**
     * Who wrote the paragraph, printed under it.
     *
     * Not decoration: the text is Wikipedia's, under CC BY-SA, and attribution
     * is the condition it comes with. It also tells the player why the tone of
     * the page is what it is.
     */
    val source: String? = null,
) {
    /**
     * The paragraph in the player's language, falling back to the other one.
     *
     * A fallback rather than nothing: a French player reading an English
     * synopsis still learns what the game is, where an empty page teaches them
     * the feature is broken. The reverse is just as true, and most sources only
     * carry English.
     */
    fun summaryFor(locale: Locale): String? =
        if (locale.language.equals("fr", ignoreCase = true)) summaryFr ?: summaryEn
        else summaryEn ?: summaryFr

    /**
     * The genre in the player's language, falling back like the summary does.
     *
     * The first one only, though the catalogue may carry two. They arrive
     * ordered from the source, the first is the one that answers "what is
     * this", and the second sits on a line that already holds a region: "Europe
     * · jeu d'action · jeu de tir à la troisième personne" is a line nobody
     * reads at arm's length. The second is kept in the file rather than dropped
     * at build time, so a screen with room for it can print it without a
     * rebuild of the catalogue.
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
 * The metadata database as the app holds it: a flat map from key to entry.
 *
 * Flattened on parse, like [eu.emufii.app.compat.CompatDb], and for the same
 * reason — the lookup happens while a cursor is moving over a grid.
 */
class GameMetaDb private constructor(private val byKey: Map<String, GameMeta>) {
    val size: Int get() = byKey.size

    /** The entry for a ROM, given its keys most specific first, or null. */
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
                    // https only. A picture fetched in clear on a public network
                    // is a picture anybody can replace, and this one is drawn
                    // full width on a screen the player is not looking at.
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
 * The database in force, for whoever draws a game.
 *
 * Empty by default, which is the honest state before the first fetch and the
 * permanent state of a player who is offline: the second page then simply says
 * it has nothing, instead of the app pretending to know.
 */
val LocalGameMetaDb = androidx.compose.runtime.staticCompositionLocalOf { GameMetaDb.EMPTY }
