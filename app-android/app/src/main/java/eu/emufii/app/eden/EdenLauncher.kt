package eu.emufii.app.eden

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import eu.emufii.app.azahar.LaunchResult
import eu.emufii.app.netplay.NetplayUiSupport
import eu.emufii.app.azahar.NetplayAutomation
import eu.emufii.app.azahar.NetplayPlan
import eu.emufii.app.azahar.PlanStore
import eu.emufii.app.netplay.NetplayTarget
import eu.emufii.app.library.Console
import eu.emufii.app.library.EmulatorPick

/**
 * Eden's launch contract is the best of the three backends: `EmulationActivity`
 * is exported, with an `ACTION_VIEW` filter on `content:` +
 * `application/octet-stream`. A SAF uri is therefore enough, no file copy, no
 * permission dance beyond the read grant.
 *
 * The class name kept in [ACTIVITY] is `org.yuzu.…`, not `dev.eden.…`: Eden
 * descends from yuzu and never renamed its Java packages. Read from the real
 * APK's manifest, not guessed.
 */
class EdenLauncher(private val context: Context) {

    /**
     * Which Eden variant Emufii drives when the player has several: the last
     * installed, by `lastUpdateTime`. On equal dates [NetplayTarget.EDEN]'s order
     * decides. Overridable through [EmulatorPick].
     * pourquoi : docs/decisions/pilotes-emulateurs.md § Eden: the most recently installed wins
     */
    fun installedPackage(): String? = EmulatorPick.packageFor(context, Console.SWITCH)

    fun isInstalled(): Boolean = installedPackage() != null

    /**
     * Unlike Azahar, arming is useful before or after the launch: Eden's multiplayer
     * lives in the app's settings, not in an in-game drawer, so nothing here depends
     * on the game having started.
     */
    fun launchGame(romUri: Uri, plan: NetplayPlan? = null, automationOn: Boolean = false): LaunchResult {
        val pkg = installedPackage() ?: return LaunchResult.NotInstalled
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(romUri, "application/octet-stream")
            component = ComponentName(pkg, ACTIVITY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching {
            val store = PlanStore(context)
            if (plan != null && automationOn) NetplayAutomation.arm(plan, store)
            else NetplayAutomation.clear(store)
            context.startActivity(intent)
            LaunchResult.Success
        }.getOrElse { LaunchResult.Error(it.message ?: "Unknown launch error") }
    }

    /** Same two-step flow as Azahar: join the room first, boot the game second. */
    fun openForNetplay(plan: NetplayPlan): LaunchResult {
        val pkg = installedPackage() ?: return LaunchResult.NotInstalled
        if (!NetplayUiSupport.isPresent(context, pkg)) {
            return LaunchResult.NoNetplayUi(
                runCatching { context.packageManager.getPackageInfo(pkg, 0).versionName }.getOrNull()
            )
        }
        NetplayAutomation.arm(plan, PlanStore(context))
        return launch()
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

    private companion object {
        const val ACTIVITY = "org.yuzu.yuzu_emu.activities.EmulationActivity"
    }
}

/**
 * Kept apart from the `PackageManager` so it can be exercised. `maxByOrNull` returns
 * the first of the ties, and the order of the list received puts our fork first.
 */
internal fun pickEden(installed: List<Pair<String, Long>>): String? =
    installed.maxByOrNull { it.second }?.first
