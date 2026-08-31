package eu.emufii.app.library

import android.content.Context

/**
 * Which language a cartridge says its name in: the one the app speaks. [tag]
 * exists because titles are cached on disk, and two languages of one cartridge
 * are two strings under the same game code.
 * pourquoi : docs/decisions/scan-bibliotheque.md § A cartridge's language is the app's
 */
object TitleLanguage {

    /**
     * The language Emufii is displayed in, not the device's. `Locale.getDefault()`
     * ignores the per-app locale, so a French app on an English phone reported
     * English and the library came back in English under French labels. The
     * resources configuration carries it; read once per scan.
     */
    @Volatile
    private var current: String = "en"

    /** The only place the locale enters. */
    fun apply(context: Context) {
        set(context.resources.configuration.locales[0].language)
    }

    fun set(language: String) {
        current = language
    }

    val tag: String get() = if (current == "fr") "fr" else "en"

    private val isFrench: Boolean get() = tag == "fr"

    /**
     * SMDH title slots: 0 Japanese, 1 English, 2 French, 3 German, 4 Italian,
     * 5 Spanish, 6 Simplified Chinese, 7 Korean, 8 Dutch, 9 Portuguese,
     * 10 Russian, 11 Traditional Chinese.
     */
    val smdh: IntArray
        get() = if (isFrench) intArrayOf(2, 1, 0, 3, 4, 5, 8, 9, 10, 6, 11, 7)
        else intArrayOf(1, 2, 0, 3, 4, 5, 8, 9, 10, 6, 11, 7)

    /** DS banner title slots: 0 Japanese, 1 English, 2 French, 3 German, 4 Italian, 5 Spanish. */
    val ndsBanner: IntArray
        get() = if (isFrench) intArrayOf(2, 1, 0, 3, 4, 5) else intArrayOf(1, 2, 0, 3, 4, 5)

    /**
     * Named rather than numbered: `icon_French.dat`. Both English variants are
     * tried, since a European dump often carries only the British one.
     */
    val switch: List<String>
        get() = if (isFrench) {
            listOf("French", "CanadianFrench", "BritishEnglish", "AmericanEnglish", "Spanish", "German", "Italian", "Japanese")
        } else {
            listOf("AmericanEnglish", "BritishEnglish", "French", "CanadianFrench", "Spanish", "German", "Italian", "Japanese")
        }

    /**
     * GameCube `BNR2` slots: 0 English, 1 German, 2 French, 3 Spanish,
     * 4 Italian, 5 Dutch. `BNR1` carries a single title and ignores this.
     */
    val gcBanner: IntArray
        get() = if (isFrench) intArrayOf(2, 0, 1, 3, 4, 5) else intArrayOf(0, 2, 1, 3, 4, 5)

    /**
     * Wii `IMET` slots: 0 Japanese, 1 English, 2 German, 3 French, 4 Spanish,
     * 5 Italian, 6 Dutch, 7 Simplified Chinese, 8 Traditional Chinese, 9 Korean.
     *
     * Not [gcBanner]'s order, which is why they are two lists: slot 2 is French
     * on a GameCube disc and German on a Wii one.
     */
    val wiiImet: IntArray
        get() = if (isFrench) intArrayOf(3, 1, 0, 2, 4, 5, 6) else intArrayOf(1, 3, 0, 2, 4, 5, 6)
}
