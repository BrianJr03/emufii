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
 * Opens Dolphin with the netplay autofill armed. No ROM is passed: the game is chosen
 * in the lobby, not at launch. Consequently both players must already have that game
 * with identical content, which netplay checks by hash.
 * pourquoi : docs/decisions/pilotes-emulateurs.md § Dolphin gets no ROM, and it does not mind
 */
class DolphinLauncher(private val context: Context) {

    // Release, beta and dev builds share one package name and one signing key, so the
    // GameCube preference carries the Wii too.
    fun installedPackage(): String? = EmulatorPick.packageFor(context, Console.GAMECUBE)

    fun isInstalled(): Boolean = installedPackage() != null

    /**
     * Armed before the launch, not after: the driver walks from the game grid through the
     * overflow menu, so it must be ready before the first screen. Automation off clears the
     * plan instead; a stale plan once made the service fight the player for a menu.
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
