package eu.emufii.app.ps2

/**
 * ARMSX2's network screen: real Android views, but part of its translations sits in JSON
 * assets rather than resources; see [I18n]. Read on ARMSX2 2.6.6.7 (`com.armsx2`, 1512).
 * pourquoi : docs/PHASE1_SCOUT_PS2_ARMSX2.md
 */
object Ps2Target {

    /**
     * `xyz.aethersx2.android` is deliberately absent: the original AetherSX2, abandoned
     * in 2023, has no network layer. Both coexist on the Thor.
     */
    val packages = listOf("com.armsx2")

    fun owns(pkg: String): Boolean = pkg in packages

    /**
     * The old network settings live in `assets/i18n` (19 languages, 838 keys); the Local
     * Link labels are hardcoded in the dex, so English in every language. [Ps2Labels]
     * falls back to the English constant when the key is missing.
     */
    object I18n {
        const val DIRECTORY = "i18n"

        const val KEY_ENABLE_DEV9 = "network.enableDev9Ethernet"
        const val KEY_PRIMARY_DNS = "network.primaryDns"
        const val KEY_NETWORK_TAB = "tab.network"
    }

    const val LABEL_ENABLE_DEV9 = "Enable DEV9 Ethernet"
    const val LABEL_NETWORK_MODE = "Network mode"
    const val LABEL_MODE_ONLINE = "Online (Sockets)"
    const val LABEL_MODE_HOST = "Host local game"
    const val LABEL_MODE_JOIN = "Join local game"

    const val LABEL_HOST_ADDRESS = "Host IPv4 address"
    const val LABEL_OWN_ADDRESS = "This device's address"
    const val LABEL_PORT = "Local Link port"
    const val LABEL_ROOM_CODE = "Room code"

    /**
     * ARMSX2's own online mode, which Emufii does not drive; mapped anyway.
     * pourquoi : docs/PS2_ONLINE_MIS_DE_COTE.md
     */
    const val LABEL_PRIMARY_DNS = "Primary DNS"
    const val LABEL_DNS_MANUAL = "Manual"

    const val LABEL_SETTINGS = "Settings"
    const val LABEL_NETWORK = "Network"

    /** The emulator negotiates nothing, so the session imposes this at both ends. */
    const val DEFAULT_PORT = 19072

    val PORT_RANGE = 1024..65535

    /** ARMSX2 keeps two nearby sessions apart with this; the relay does the isolating. */
    val ROOM_CODE_LENGTH = 4..12

    fun isValidRoomCode(code: String): Boolean =
        code.length in ROOM_CODE_LENGTH && code.all { it.isLetterOrDigit() && it.code < 128 }

    fun isValidPort(port: Int): Boolean = port in PORT_RANGE
}
