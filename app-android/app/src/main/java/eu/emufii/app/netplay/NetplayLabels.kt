package eu.emufii.app.netplay

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/**
 * Everything else in this package matches on view ids ([NetplayTarget]), but Azahar's home
 * settings list is a RecyclerView whose rows share the same ids (`option_card`,
 * `option_title`), so only the text tells Multiplayer from System Files. Requires the
 * package in `<queries>`.
 */
object NetplayLabels {

    const val MULTIPLAYER = "multiplayer"

    /** Most specific first: which of the hub's title and description carries the word differs between builds. */
    val MULTIPLAYER_STRINGS = listOf(MULTIPLAYER, "multiplayer_description")

    /**
     * Every translation the emulator ships, not just the expected one: a third-party app's
     * language is per application since Android 13 and no public API reads it. On the Thor the
     * system announces `[en, fr_FR]` and Azahar still displays "Multijoueur".
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

    /** Closed on purpose: a missing language breaks only the automatic opening, never the form filling. */
    private val CANDIDATE_LANGUAGES = listOf(
        "en", "fr", "de", "es", "it", "pt", "nl", "pl", "ru", "tr",
        "ja", "ko", "zh", "ar", "cs", "da", "fi", "hu", "id", "nb",
        "ro", "sv", "uk", "vi", "el", "he", "th", "ca", "sr", "hr"
    )

}
