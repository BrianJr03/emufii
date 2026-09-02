package eu.emufii.app.azahar

import android.content.ComponentName
import android.content.Context
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import eu.emufii.app.netplay.NetplayUiSupport
import eu.emufii.app.library.Console
import eu.emufii.app.library.EmulatorPick

sealed class LaunchResult {
    data object Success : LaunchResult()
    data object NotInstalled : LaunchResult()

    /** Distinct from [Error]: the fix is the user's, updating the emulator. */
    data class NoNetplayUi(val versionName: String?) : LaunchResult()

    data class Error(val message: String) : LaunchResult()
}

class AzaharLauncher(private val context: Context) {

    fun installedPackage(): String? = EmulatorPick.packageFor(context, Console.THREE_DS)

    fun installedVersionName(pkg: String): String? = runCatching {
        context.packageManager.getPackageInfo(pkg, 0).versionName
    }.getOrNull()

    fun hasNetplayUi(): Boolean {
        val pkg = installedPackage() ?: return false
        return NetplayUiSupport.isPresent(context, pkg)
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

    /**
     * The plan is armed only when the service is running: otherwise it lingers and fires on
     * some later launch. A build with no multiplayer UI is refused rather than launched.
     */
    fun launchGame(romUri: Uri, plan: NetplayPlan? = null): LaunchResult {
        val pkg = installedPackage() ?: return LaunchResult.NotInstalled
        if (plan != null && !NetplayUiSupport.isPresent(context, pkg)) {
            return LaunchResult.NoNetplayUi(installedVersionName(pkg))
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(romUri, "application/octet-stream")
            component = ComponentName(pkg, "org.citra.citra_emu.activities.EmulationActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching {
            val store = PlanStore(context)
            if (plan != null && isNetplayAutomationEnabled()) {
                NetplayAutomation.arm(plan, store)
            } else {
                NetplayAutomation.clear(store)
            }
            context.startActivity(intent)
            LaunchResult.Success
        }.getOrElse { LaunchResult.Error(it.message ?: "Unknown launch error") }
    }

    /**
     * The room is joined before the game starts: Azahar connects from the main menu, then
     * boots. Bundling both into one button launched the ROM into an emulator that had
     * joined nothing.
     */
    fun openForNetplay(plan: NetplayPlan): LaunchResult {
        val pkg = installedPackage() ?: return LaunchResult.NotInstalled
        if (!NetplayUiSupport.isPresent(context, pkg)) {
            return LaunchResult.NoNetplayUi(installedVersionName(pkg))
        }
        NetplayAutomation.arm(plan, PlanStore(context))
        return launch()
    }

    /**
     * Compared as [ComponentName]s, not strings: Android stores this setting either long or
     * short (`eu.emufii.app/.azahar.AzaharNetplayService`), and `flattenToString` only
     * produces the long form, so a string comparison answered "off" on every device holding
     * the short one.
     */
    fun isNetplayAutomationEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val us = ComponentName(context, AzaharNetplayService::class.java)
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        return splitter.any { ComponentName.unflattenFromString(it) == us }
    }

    /**
     * `FLAG_ACTIVITY_NEW_TASK` set unconditionally put the settings screen in its own task,
     * and Back landed in a different app instead of the onboarding step that asked for it.
     */
    fun openAccessibilitySettings(): LaunchResult = runCatching {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        LaunchResult.Success
    }.getOrElse { LaunchResult.Error(it.message ?: "Unknown launch error") }
}
