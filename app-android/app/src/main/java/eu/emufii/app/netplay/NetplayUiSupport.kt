package eu.emufii.app.netplay

import android.content.Context

/**
 * Asks the emulator's resources rather than comparing version numbers, so it
 * cannot disagree with what the accessibility service finds at runtime.
 * pourquoi : docs/decisions/pilotes-emulateurs.md § Ask the resources, not the version number
 */
object NetplayUiSupport {

    /**
     * Not the whole of [NetplayUi]: PREFERRED_GAME is Eden-only and
     * MENU_MULTIPLAYER Azahar-only, so either would false-negative on the other.
     */
    val PROBE_IDS = listOf(
        NetplayUi.BTN_CREATE,
        NetplayUi.BTN_JOIN,
        NetplayUi.IP_ADDRESS,
        NetplayUi.BTN_CONFIRM
    )

    fun isPresent(context: Context, pkg: String): Boolean {
        val res = runCatching {
            context.packageManager.getResourcesForApplication(pkg)
        }.getOrNull() ?: return false
        return PROBE_IDS.all { name ->
            runCatching { res.getIdentifier(name, "id", pkg) }.getOrDefault(0) != 0
        }
    }
}
