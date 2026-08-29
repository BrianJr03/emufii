package eu.emufii.app.dolphin

import android.content.Context
import android.content.Intent
import eu.emufii.app.azahar.LaunchResult
import eu.emufii.app.azahar.NetplayAutomation
import eu.emufii.app.azahar.NetplayPlan
import eu.emufii.app.azahar.PlanStore
import eu.emufii.app.library.Console
import eu.emufii.app.library.EmulatorPick

/**
 * Opens Dolphin with the netplay autofill armed.
 *
 * Aucune ROM ne lui est passee : le jeu se choisit dans le salon, pas au
 * lancement. Consequence : les deux joueurs doivent deja avoir ce jeu, avec un
 * contenu identique — le netplay le verifie par empreinte.
 * pourquoi : docs/decisions/pilotes-emulateurs.md § Dolphin ne reçoit pas de ROM, et ça ne le gêne pas
 */
class DolphinLauncher(private val context: Context) {

    /**
     * The installed Dolphin, if any.
     *
     * Nothing like Eden's matrix to arbitrate: release, beta and dev builds all
     * share one package name and one signing key, so there is at most one to
     * find, and the dev build simply updates the release in place.
     */
    // La GameCube porte le choix pour les deux : Dolphin est un seul paquet
    // qui joue les deux consoles, et deux preferences pour un binaire feraient
    // deux reponses possibles a une question qui n'en a qu'une.
    fun installedPackage(): String? = EmulatorPick.packageFor(context, Console.GAMECUBE)

    fun isInstalled(): Boolean = installedPackage() != null

    /**
     * Opens Dolphin on its Netplay Setup screen, with [plan] armed.
     *
     * Arming before the launch, not after: the driver walks from the game grid
     * through the overflow menu, so it has to be ready before the first screen
     * appears. When the automation is off the plan is cleared instead, a stale
     * plan is what once made the service fight the player for a menu.
     */
    fun openForNetplay(plan: NetplayPlan, automationOn: Boolean = true): LaunchResult {
        val pkg = installedPackage() ?: return LaunchResult.NotInstalled
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            ?: return LaunchResult.Error("No launch intent for $pkg")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            val store = PlanStore(context)
            if (automationOn) NetplayAutomation.arm(plan, store) else NetplayAutomation.clear(store)
            context.startActivity(intent)
            LaunchResult.Success
        }.getOrElse { LaunchResult.Error(it.message ?: "Unknown launch error") }
    }

    /** Opens Dolphin with nothing armed, for a player who wants to drive it themselves. */
    fun launch(): LaunchResult {
        val pkg = installedPackage() ?: return LaunchResult.NotInstalled
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            ?: return LaunchResult.Error("No launch intent for $pkg")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            LaunchResult.Success
        }.getOrElse { LaunchResult.Error(it.message ?: "Unknown launch error") }
    }
}
