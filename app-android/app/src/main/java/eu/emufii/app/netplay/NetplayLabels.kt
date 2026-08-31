package eu.emufii.app.netplay

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/**
 * Reads a label out of the emulator's own resources.
 *
 * Everything else in this package matches on view ids ([NetplayTarget]), but
 * Azahar's home settings list is a RecyclerView whose rows share the same ids
 * (`option_card`, `option_title`), so only the text tells Multiplayer from
 * System Files. Requires the package in `<queries>`.
 *
 * Returns null when the package or the string is missing; callers stop there
 * rather than click something arbitrary.
 */
object NetplayLabels {

    /** The emulator's own name for the multiplayer entry, in the current locale. */
    const val MULTIPLAYER = "multiplayer"

    /**
     * Most specific first. The hub shows a title and a description, and which of
     * the two carries the word differs between builds.
     */
    val MULTIPLAYER_STRINGS = listOf(MULTIPLAYER, "multiplayer_description")

    /**
     * Every translation the emulator ships for this string, not just the expected
     * one: a third-party app's language is set per application since Android 13
     * and no public API reads it. On the Thor the system announces `[en, fr_FR]`
     * and Azahar still displays "Multijoueur". Some thirty string reads per screen.
     */
    fun of(context: Context, pkg: String, name: String): List<String> {
        val res = runCatching {
            context.packageManager.getResourcesForApplication(pkg)
        }.getOrNull() ?: return emptyList()
        val id = runCatching { res.getIdentifier(name, "string", pkg) }.getOrDefault(0)
        if (id == 0) return emptyList()

        val out = LinkedHashSet<String>()
        runCatching { res.getString(id) }.getOrNull()?.let { out += it }
        for (tag in CANDIDATE_LANGUAGES) {
            runCatching {
                val cfg = Configuration(res.configuration)
                cfg.setLocale(Locale.forLanguageTag(tag))
                @Suppress("DEPRECATION")
                Resources(res.assets, res.displayMetrics, cfg).getString(id)
            }.getOrNull()?.let { out += it }
        }
        return out.toList()
    }

    /**
     * Closed on purpose: a missing language breaks only the automatic opening,
     * never the form filling. One language too many costs a single string read.
     */
    private val CANDIDATE_LANGUAGES = listOf(
        "en", "fr", "de", "es", "it", "pt", "nl", "pl", "ru", "tr",
        "ja", "ko", "zh", "ar", "cs", "da", "fi", "hu", "id", "nb",
        "ro", "sv", "uk", "vi", "el", "he", "th", "ca", "sr", "hr"
    )

}
