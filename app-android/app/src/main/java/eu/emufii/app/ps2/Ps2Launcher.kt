package eu.emufii.app.ps2

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import eu.emufii.app.azahar.LaunchResult
import eu.emufii.app.azahar.NetplayAutomation
import eu.emufii.app.azahar.NetplayPlan
import eu.emufii.app.azahar.PlanStore
import eu.emufii.app.session.RomRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import eu.emufii.app.library.Console
import eu.emufii.app.library.EmulatorPick

/**
 * Opens ARMSX2 with the Local Link autofill armed. By named component: its `VIEW`
 * filter declares no MIME type, so a SAF URI can never resolve there. And the setup
 * happens before the game starts, where DEV9 would re-read nothing.
 * pourquoi : docs/decisions/pilotes-emulateurs.md § ARMSX2 is launched by named component, never by filtering
 */
class Ps2Launcher(private val context: Context) {

    /**
     * The installed ARMSX2, if there is one.
     *
     * `xyz.aethersx2.android` does not count, even when present: that is the
     * original AetherSX2, with no network layer. Both live side by side on the
     * Thor, and that is exactly the kind of neighbourhood that would have us
     * driving the wrong one.
     */
    fun installedPackage(): String? = EmulatorPick.packageFor(context, Console.PS2)

    fun isInstalled(): Boolean = installedPackage() != null

    /**
     * Opens ARMSX2 with [plan] armed, and the ROM if we have one.
     *
     * Arming precedes launching, as everywhere else: the driver sets off from the
     * library and has to be ready before the first screen.
     */
    fun openForLocalLink(
        plan: NetplayPlan,
        rom: Uri? = null,
        automationOn: Boolean = true
    ): LaunchResult {
        val pkg = installedPackage() ?: return LaunchResult.NotInstalled
        val intent = if (rom != null) {
            viewIntent(pkg, rom)
        } else {
            // `CLEAR_TOP`, and the named component. Without it, an already open
            // ARMSX2 comes back to the foreground *where the player left it*, mid
            // game, in another settings tab, and the driver finds itself facing a
            // screen it cannot read. It then gives up silently, which reads as
            // "the automatic setup does not work". Measured on 2026-08-17.
            Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName(pkg, VIEW_ACTIVITY)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            val store = PlanStore(context)
            Ps2ProvisioningAutomation.clear(Ps2ProvisioningStore(context))
            if (automationOn) NetplayAutomation.arm(plan, store) else NetplayAutomation.clear(store)
            context.startActivity(intent)
            LaunchResult.Success
        }.getOrElse { LaunchResult.Error(it.message ?: "Unknown launch error") }
    }

    /** Opens the ARMSX2 library while the separate global-card setup is armed. */
    fun openForProvisioning(plan: Ps2ProvisioningPlan): LaunchResult {
        val pkg = installedPackage() ?: return LaunchResult.NotInstalled
        val intent = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName(pkg, VIEW_ACTIVITY)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            NetplayAutomation.clear(PlanStore(context))
            Ps2ProvisioningAutomation.arm(plan, Ps2ProvisioningStore(context))
            context.startActivity(intent)
            LaunchResult.Success
        }.getOrElse {
            Ps2ProvisioningAutomation.clear(Ps2ProvisioningStore(context))
            LaunchResult.Error(it.message ?: "Unknown launch error")
        }
    }

    /**
     * Starts the ROM, arming nothing.
     *
     * This is the session's second step: the network was set at the first, and
     * re-arming the driver would send it to fill the form in again over a running
     * game. Dolphin has no such screen, it cannot be handed a game from outside,
     * but ARMSX2 can.
     */
    fun launchGame(rom: Uri): LaunchResult {
        val pkg = installedPackage() ?: return LaunchResult.NotInstalled
        val intent = viewIntent(pkg, rom).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        return runCatching {
            Ps2ProvisioningAutomation.clear(Ps2ProvisioningStore(context))
            context.startActivity(intent)
            LaunchResult.Success
        }.getOrElse { LaunchResult.Error(it.message ?: "Unknown launch error") }
    }

    /**
     * Writes ARMSX2's native per-game layer and boots the ROM in one operation.
     * No accessibility plan is armed: the emulator reads this file after its
     * private global preferences and before DEV9 initialises.
     */
    suspend fun launchPrivateGame(rom: RomRef, plan: NetplayPlan): LaunchResult {
        val pkg = installedPackage() ?: return LaunchResult.NotInstalled
        when (val configured = withContext(Dispatchers.IO) {
            Ps2GameSettings.apply(context, rom, plan)
        }) {
            is Ps2GameSettings.Outcome.Success -> Unit
            Ps2GameSettings.Outcome.MissingFolderGrant ->
                return LaunchResult.Error("ARMSX2 folder access is missing")
            Ps2GameSettings.Outcome.MissingPreparedCard ->
                return LaunchResult.Error("the prepared PS2 network card is missing")
            Ps2GameSettings.Outcome.UnknownDiscIdentity ->
                return LaunchResult.Error("the PS2 boot ELF CRC is unavailable")
            is Ps2GameSettings.Outcome.WriteFailed ->
                return LaunchResult.Error(configured.detail)
        }
        val intent = viewIntent(pkg, rom.uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        return runCatching {
            NetplayAutomation.clear(PlanStore(context))
            Ps2ProvisioningAutomation.clear(Ps2ProvisioningStore(context))
            context.startActivity(intent)
            LaunchResult.Success
        }.getOrElse { LaunchResult.Error(it.message ?: "Unknown launch error") }
    }

    /**
     * The intent that opens a ROM, with a named component.
     *
     * `com.armsx2.Main` is the activity behind the manifest's `MainActivity`
     * alias, it is what `am start` resolves to when resolution works (with a
     * `file://`), and it is therefore what we target for a `content://`, which
     * filtering cannot reach.
     */
    private fun viewIntent(pkg: String, rom: Uri): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            component = ComponentName(pkg, VIEW_ACTIVITY)
            data = rom
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    /** Opens ARMSX2 arming nothing, for whoever wants to set it up themselves. */
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
        const val VIEW_ACTIVITY = "com.armsx2.Main"
    }
}
