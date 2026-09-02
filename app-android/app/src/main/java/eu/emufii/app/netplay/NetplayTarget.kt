package eu.emufii.app.netplay

import eu.emufii.app.azahar.AzaharPackage

/**
 * Azahar and Eden present the same dialog, having inherited the same Android screens
 * from Citra and yuzu: one automation, only the package name differs. Ids rather than
 * labels, since both ship dozens of locales.
 * pourquoi : docs/NOTES_NETPLAY.md
 */
data class NetplayTarget(
    val packages: List<String>,
    /** Null means "start at the sheet", not "unsupported". */
    val inGameMenuId: String?,
    /** The path to Multiplayer when no game is running. */
    val homeNavId: String? = null,
    val homeListId: String? = null,
    /** The gear and the tab do not always land on the same screen. */
    val extraListIds: List<String> = emptyList(),
    /** Eden's gear is a plain view in one build, a toolbar menu item in another. */
    val homeSettingsButtonIds: List<String> = emptyList(),
    val uiReadFrom: String
) {
    fun owns(pkg: String): Boolean = pkg in packages

    companion object {
        val AZAHAR = NetplayTarget(
            // A second copy went stale when the Lime3DS id was added: the emulator was
            // detected and no longer recognised by the driver.
            packages = AzaharPackage.candidates,
            inGameMenuId = NetplayUi.MENU_MULTIPLAYER,
            homeNavId = NetplayUi.NAV_HOME_SETTINGS,
            homeListId = NetplayUi.HOME_SETTINGS_LIST,
            uiReadFrom = "azahar-android-vanilla-2126.0-rc5 (read live on the Thor, 2026-08-01)"
        )

        val EDEN = NetplayTarget(
            // Three flavours crossed with a `.nightly` suffix, two of them without
            // "eden" in the name, read out of Eden's `build.gradle.kts`. EdenLauncher
            // keeps the first installed, so our fork leads.
            packages = listOf(
                "dev.eden.eden_emulator.emufii",
                "dev.eden.eden_emulator",
                "dev.eden.eden_emulator.nightly",
                "com.miHoYo.Yuanshen",
                "com.miHoYo.Yuanshen.nightly",
                "dev.legacy.eden_emulator"
            ),
            inGameMenuId = NetplayUi.MENU_MULTIPLAYER,
            homeNavId = NetplayUi.NAV_HOME_SETTINGS,
            homeListId = NetplayUi.HOME_SETTINGS_LIST,
            // On Eden the tab id exists in the resources but nothing shows it: the
            // multiplayer entry is only behind the top-bar gear.
            homeSettingsButtonIds = listOf(NetplayUi.SETTINGS_BUTTON, NetplayUi.MENU_SETTINGS),
            extraListIds = listOf(NetplayUi.SETTINGS_LIST, NetplayUi.LIST_SETTINGS),
            uiReadFrom = "eden-android-1f6734c (stable, read on the Thor 2026-08-01)"
        )

        val all = listOf(AZAHAR, EDEN)

        fun forPackage(pkg: String): NetplayTarget? = all.firstOrNull { it.owns(pkg) }
    }
}

/**
 * View ids of the netplay dialog, identical in Azahar and Eden.
 *
 *   menu_in_game               → [MENU_MULTIPLAYER]  (in-game drawer)
 *   dialog_multiplayer_connect → [BTN_CREATE] / [BTN_JOIN] / [BTN_LOBBY_BROWSER]
 *   dialog_multiplayer_room    → [IP_ADDRESS] / [IP_PORT] / … / [BTN_CONFIRM]
 */
object NetplayUi {

    // In-game drawer
    const val MENU_MULTIPLAYER = "menu_multiplayer"

    // The home hub's option cards share their ids: only their text tells them apart,
    // hence NetplayLabels.
    const val NAV_HOME_SETTINGS = "homeSettingsFragment"
    const val HOME_SETTINGS_LIST = "home_settings_list"
    const val OPTION_TITLE = "option_title"

    /** The row is found by its text; this says where to read that text from. */
    val ROW_TITLE_IDS = listOf(OPTION_TITLE, "setting_title")

    /** The gear in Eden's top bar: a view in one build, a menu item in another. */
    const val SETTINGS_BUTTON = "settings_button"
    const val MENU_SETTINGS = "menu_settings"

    const val SETTINGS_LIST = "settings_list"
    const val LIST_SETTINGS = "list_settings"

    // In landscape the sheet cuts off at the bottom and the host's button is routinely
    // off screen: never look these up with a visibility filter.
    const val BTN_CREATE = "btn_create"
    const val BTN_JOIN = "btn_join"
    const val BTN_LOBBY_BROWSER = "btn_lobby_browser"

    const val IP_ADDRESS = "ip_address"
    const val IP_PORT = "ip_port"
    const val USERNAME = "username"
    const val ROOM_NAME = "room_name"
    const val PASSWORD = "password"

    /** Azahar's resources say `prefered_game_name` with one r, Eden's has two. */
    const val PREFERRED_GAME = "dropdown_preferred_game_name"
    const val PREFERRED_GAME_ALT = "dropdown_prefered_game_name"

    val PREFERRED_GAME_IDS = listOf(PREFERRED_GAME, PREFERRED_GAME_ALT)
    const val BTN_CONFIRM = "btn_confirm"

    /** The ENet port both emulators default to, and Citra's long-standing one. */
    const val DEFAULT_PORT = 24872

    fun id(pkg: String, name: String): String = "$pkg:id/$name"
}
