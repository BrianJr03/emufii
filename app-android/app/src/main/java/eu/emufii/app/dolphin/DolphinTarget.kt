package eu.emufii.app.dolphin

/**
 * Dolphin's netplay screen. Apart from [eu.emufii.app.netplay.NetplayTarget]
 * because it is Compose and exposes no resource ids: every field arrives as a
 * bare `android.widget.EditText`, so addressing is text and geometry only.
 * pourquoi : docs/NOTES_DOLPHIN.md
 */
object DolphinTarget {

    val packages = listOf(
        "org.dolphinemu.dolphinemu",
        "org.dolphinemu.dolphinemu.debug"
    )

    fun owns(pkg: String): Boolean = pkg in packages

    const val LABEL_MENU_NETPLAY = "grid_menu_netplay"

    /** Belongs to appcompat, so it resolves in Dolphin's locale. */
    const val OVERFLOW_DESCRIPTION = "abc_action_menu_overflow_description"

    /**
     * String names, not values: Dolphin ships some forty translations and an app's
     * language is per-app since Android 13. See [eu.emufii.app.netplay.NetplayLabels].
     */
    const val LABEL_NICKNAME = "netplay_nickname_label"
    const val LABEL_IP_ADDRESS = "netplay_ip_address_label"
    const val LABEL_PORT = "netplay_port_label"
    const val LABEL_CONNECTION_TYPE = "netplay_connection_type"
    const val LABEL_DIRECT_CONNECTION = "netplay_connection_type_direct_connection"
    const val LABEL_TRAVERSAL_SERVER = "netplay_connection_type_traversal_server"

    /**
     * Also the two confirm buttons: Dolphin reuses one string for both, and the
     * button is the one wrapped in `android.widget.Button`.
     */
    const val LABEL_ROLE_CONNECT = "netplay_connection_role_connect"
    const val LABEL_ROLE_HOST = "netplay_connection_role_host"

    /** Dolphin restores it from `[NetPlay] Game` in `Dolphin.ini`; set it explicitly. */
    const val LABEL_GAME = "netplay_game_label"

    /**
     * `DEFAULT_LISTEN_PORT` in `Source/Core/Core/Config/NetplaySettings.cpp`.
     * Unlike Azahar's 24872, the plan must carry this one explicitly.
     */
    const val DEFAULT_PORT = 2626

    const val UI_READ_FROM = "dolphin-master-2606-302 (read live on the Thor, 2026-08-15)"
}
