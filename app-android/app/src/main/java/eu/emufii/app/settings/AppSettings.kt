package eu.emufii.app.settings

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import androidx.core.content.edit
import eu.emufii.app.library.Console
import eu.emufii.app.library.LibraryLayout
import eu.emufii.app.library.LibrarySort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Which language the app speaks. [SYSTEM] is the right default.
 * pourquoi : docs/decisions/reglages-et-consoles.md § Suivre le téléphone est le bon défaut, sauf pour l'accent
 */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    FRENCH("fr"),
    ENGLISH("en");

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}

/**
 * Light or dark, or whatever the phone says. [OLED] is a *dark*, not a third
 * universe: everything reading [isDark] goes on seeing dark.
 * pourquoi : docs/decisions/reglages-et-consoles.md § L'OLED est un sombre, pas un troisième univers
 */
enum class AppTheme {
    SYSTEM, LIGHT, DARK, OLED;

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
        OLED -> true
    }

    /** True for [OLED] only: pure black, instead of midnight blue. */
    val isOled: Boolean get() = this == OLED

    companion object {
        fun fromName(name: String?): AppTheme =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

/**
 * App-wide preferences. Small on purpose. The language goes through the
 * platform's per-app API, never a hand-juggled `Configuration`.
 * pourquoi : docs/decisions/reglages-et-consoles.md § La langue passe par la plateforme, le thème ne peut pas
 */
class SettingsStore private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(readLanguage())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _theme = MutableStateFlow(AppTheme.fromName(prefs.getString(KEY_THEME, null)))
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    /**
     * The player's SteamGridDB key. Every player brings their own — a key frozen
     * into the APK is extractable and carries the whole fleet's quota.
     * pourquoi : docs/decisions/reglages-et-consoles.md § Chaque joueur apporte sa propre clé
     */
    private val _steamGridDbKey = MutableStateFlow(prefs.getString(KEY_SGDB, "").orEmpty())
    val steamGridDbKey: StateFlow<String> = _steamGridDbKey.asStateFlow()

    /**
     * The Cocoon folder, when the player has one: their library then looks here
     * exactly as it looks there, with no key and no network.
     * pourquoi : docs/decisions/reglages-et-consoles.md § Chaque joueur apporte sa propre clé
     */
    private val _cocoonFolder = MutableStateFlow(prefs.getString(KEY_COCOON, "").orEmpty())
    val cocoonFolder: StateFlow<String> = _cocoonFolder.asStateFlow()

    fun setCocoonFolder(uri: String) {
        _cocoonFolder.value = uri
        prefs.edit { putString(KEY_COCOON, uri) }
    }

    /**
     * The library's layout and order, kept here rather than in the screen. An
     * unknown value falls back to the default instead of failing the launch.
     * pourquoi : docs/decisions/reglages-et-consoles.md § Ce qui est stocké, c'est ce qui est refusé
     */
    private val _libraryLayout = MutableStateFlow(
        LibraryLayout.entries.firstOrNull { it.name == prefs.getString(KEY_LAYOUT, null) }
            ?: LibraryLayout.GRID
    )
    val libraryLayout: StateFlow<LibraryLayout> = _libraryLayout.asStateFlow()

    fun setLibraryLayout(layout: LibraryLayout) {
        prefs.edit { putString(KEY_LAYOUT, layout.name) }
        _libraryLayout.value = layout
    }

    private val _librarySort = MutableStateFlow(
        LibrarySort.entries.firstOrNull { it.name == prefs.getString(KEY_SORT, null) }
            ?: LibrarySort.NAME
    )
    val librarySort: StateFlow<LibrarySort> = _librarySort.asStateFlow()

    fun setLibrarySort(sort: LibrarySort) {
        prefs.edit { putString(KEY_SORT, sort.name) }
        _librarySort.value = sort
    }

    /**
     * The consoles the player asked *not* to see. **Stored as what is hidden,
     * never as what is shown** — the only default that cannot lose a game.
     * pourquoi : docs/decisions/reglages-et-consoles.md § Ce qui est stocké, c'est ce qui est refusé
     */
    private val _hiddenConsoles = MutableStateFlow(readHiddenConsoles())
    val hiddenConsoles: StateFlow<Set<Console>> = _hiddenConsoles.asStateFlow()

    private fun readHiddenConsoles(): Set<Console> =
        prefs.getStringSet(KEY_HIDDEN_CONSOLES, null)
            .orEmpty()
            .mapNotNull { name -> Console.entries.firstOrNull { it.name == name } }
            .toSet()

    fun setConsoleVisible(console: Console, visible: Boolean) {
        val next = if (visible) _hiddenConsoles.value - console else _hiddenConsoles.value + console
        // A copy, because SharedPreferences hands back the very set it holds and
        // documents that mutating it is undefined. The bug it produces is the
        // quiet kind: it survives until the process dies.
        prefs.edit { putStringSet(KEY_HIDDEN_CONSOLES, next.map { it.name }.toSet()) }
        _hiddenConsoles.value = next
    }

    /**
     * Whether the second display carries the panel. On by default, stored even
     * on devices that have no second display.
     * pourquoi : docs/decisions/reglages-et-consoles.md § Les défauts « activé », et pourquoi ce sont quand même des interrupteurs
     */
    private val _secondScreen = MutableStateFlow(prefs.getBoolean(KEY_SECOND_SCREEN, true))
    val secondScreen: StateFlow<Boolean> = _secondScreen.asStateFlow()

    fun setSecondScreen(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SECOND_SCREEN, enabled) }
        _secondScreen.value = enabled
    }

    /**
     * Whether a friend's arrival may reach the system shade. On by default: a
     * friends list nobody is told about is an address book.
     * pourquoi : docs/decisions/reglages-et-consoles.md § Les défauts « activé », et pourquoi ce sont quand même des interrupteurs
     */
    private val _notifyFriends = MutableStateFlow(prefs.getBoolean(KEY_NOTIFY_FRIENDS, true))
    val notifyFriends: StateFlow<Boolean> = _notifyFriends.asStateFlow()

    fun setNotifyFriends(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIFY_FRIENDS, enabled) }
        _notifyFriends.value = enabled
    }

    /**
     * Whether a new version announces itself outside the app. On by default:
     * Emufii is sideloaded and no store speaks for it.
     * pourquoi : docs/decisions/reglages-et-consoles.md § Les défauts « activé », et pourquoi ce sont quand même des interrupteurs
     */
    private val _notifyUpdates = MutableStateFlow(prefs.getBoolean(KEY_NOTIFY_UPDATES, true))
    val notifyUpdates: StateFlow<Boolean> = _notifyUpdates.asStateFlow()

    fun setNotifyUpdates(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIFY_UPDATES, enabled) }
        _notifyUpdates.value = enabled
    }

    fun setSteamGridDbKey(key: String) {
        val cleaned = key.trim()
        prefs.edit { putString(KEY_SGDB, cleaned) }
        _steamGridDbKey.value = cleaned
    }

    /**
     * Unlike the language, no platform API owns this, so the choice lives here
     * and the theme reads it — which also makes switching instant.
     * pourquoi : docs/decisions/reglages-et-consoles.md § La langue passe par la plateforme, le thème ne peut pas
     */
    fun setTheme(theme: AppTheme) {
        prefs.edit { putString(KEY_THEME, theme.name) }
        _theme.value = theme
    }

    /** Read by the theme, like [setTheme], so the change is instant. */
    private fun readLanguage(): AppLanguage {
        // The platform is the source of truth once a choice has been made, so a
        // change from Android's own settings screen is reflected here too.
        val fromSystem = localeManager()?.applicationLocales
            ?.takeIf { !it.isEmpty }
            ?.get(0)
            ?.language
        return AppLanguage.fromTag(fromSystem ?: prefs.getString(KEY_LANGUAGE, null))
    }

    /**
     * Whether the first-run walkthrough has been completed. Here and not in the
     * library store: clearing a ROM folder must not replay onboarding.
     */
    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit { putBoolean(KEY_ONBOARDING_DONE, value) }

    fun setLanguage(language: AppLanguage) {
        prefs.edit { putString(KEY_LANGUAGE, language.tag) }
        _language.value = language
        localeManager()?.applicationLocales = language.tag
            ?.let { LocaleList.forLanguageTags(it) }
            ?: LocaleList.getEmptyLocaleList()
    }

    private fun localeManager(): LocaleManager? =
        appContext.getSystemService(LocaleManager::class.java)

    companion object {
        /**
         * The ONE store for the process: `SharedPreferences` is already shared,
         * the `StateFlow` in front of it is not. Building one per screen made
         * onboarding choices silently revert.
         * pourquoi : docs/decisions/reglages-et-consoles.md § Un seul magasin pour le processus
         */
        @Volatile
        private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context).also { instance = it }
            }

        private const val PREFS = "emufii_settings"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_THEME = "theme"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_SGDB = "steamgriddb_key"
        private const val KEY_COCOON = "cocoon_folder"
        private const val KEY_LAYOUT = "library_layout"
        private const val KEY_SORT = "library_sort"
        private const val KEY_HIDDEN_CONSOLES = "hidden_consoles"
        private const val KEY_SECOND_SCREEN = "second_screen"
        private const val KEY_NOTIFY_FRIENDS = "notify_friends"
        private const val KEY_NOTIFY_UPDATES = "notify_updates"
    }
}
