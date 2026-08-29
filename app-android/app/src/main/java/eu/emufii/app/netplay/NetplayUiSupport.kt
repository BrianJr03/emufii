package eu.emufii.app.netplay

import android.content.Context

/**
 * Does the installed build actually *have* a multiplayer UI to drive?
 *
 * Demande ses ressources a l'emulateur plutot que de comparer des numeros de
 * version : c'est la meme question que le service d'accessibilite posera a
 * l'execution, donc elle ne peut pas etre en desaccord avec lui.
 * pourquoi : docs/decisions/pilotes-emulateurs.md § Demander aux ressources, pas au numéro de version
 */
object NetplayUiSupport {

    /**
     * The ids that must resolve for the automation to have anything to fill in:
     * the entry buttons and the address field of the room form. Deliberately not
     * the whole of [NetplayUi], [NetplayUi.PREFERRED_GAME] is Eden-only and
     * [NetplayUi.MENU_MULTIPLAYER] is Azahar-only, so requiring either would
     * report a false negative on the other.
     */
    val PROBE_IDS = listOf(
        NetplayUi.BTN_CREATE,
        NetplayUi.BTN_JOIN,
        NetplayUi.IP_ADDRESS,
        NetplayUi.BTN_CONFIRM
    )

    /**
     * True if [pkg] exposes a netplay dialog Emufii can drive.
     *
     * Returns false when the package is absent or its resources can't be read,
     * the caller wants to know "can I drive this", and both answers are no.
     */
    fun isPresent(context: Context, pkg: String): Boolean {
        val res = runCatching {
            context.packageManager.getResourcesForApplication(pkg)
        }.getOrNull() ?: return false
        return PROBE_IDS.all { name ->
            runCatching { res.getIdentifier(name, "id", pkg) }.getOrDefault(0) != 0
        }
    }
}
