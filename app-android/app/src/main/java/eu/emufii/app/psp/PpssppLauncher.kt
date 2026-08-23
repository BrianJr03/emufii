package eu.emufii.app.psp

import android.content.Context
import android.content.Intent
import android.net.Uri
import eu.emufii.app.R
import eu.emufii.app.azahar.LaunchResult
import eu.emufii.app.library.Rom
import eu.emufii.app.session.RomRef

/**
 * The address the player sets once inside PPSSPP.
 *
 * It belongs to nobody and never changes: the relay translates it towards the
 * current session's host. That is what replaces the address otherwise retyped
 * every game. Must stay identical to `relay/firewall.js` and to the coordinator.
 */
const val HOST_SENTINEL = "10.66.1.1"

/**
 * Starting a PSP game in PPSSPP.
 *
 * PPSSPP's interface is an opaque native surface, so accessibility cannot drive
 * it. Its memory stick can, however, live in a user-granted SAF tree. Emufii
 * writes PPSSPP's supported per-game INI immediately before opening the ROM;
 * PPSSPP reads that file during boot, even when its menu is already running.
 *
 * PPSSPP accepts `VIEW` with `content://`, verified against the system on the
 * device, so a SAF uri from the library is enough, with no copy and no storage
 * permission.
 */
class PpssppLauncher(private val context: Context) {

    private val config = PpssppConfigStore(context)

    fun installedPackage(): String? = PpssppPackage.candidates.firstOrNull { pkg ->
        runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
    }

    /**
     * Opens PPSSPP on its own screen, with no game.
     *
     * That is what is needed to go and set the network up: the player has to
     * reach the settings, and the settings cannot be reached from a running game.
     * Distinct from [launchGame] for that reason alone, same program, two
     * different moments.
     */
    fun openApp(): LaunchResult {
        val pkg = installedPackage() ?: return LaunchResult.NotInstalled
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            ?: return LaunchResult.NotInstalled
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            LaunchResult.Success
        }.getOrElse { LaunchResult.Error(it.message ?: "Unknown launch error") }
    }

    fun launchGame(romUri: Uri): LaunchResult {
        val pkg = installedPackage() ?: return LaunchResult.NotInstalled
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(romUri, "application/octet-stream")
            setPackage(pkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching {
            context.startActivity(intent)
            LaunchResult.Success
        }.getOrElse { LaunchResult.Error(it.message ?: "Unknown launch error") }
    }

    fun launchPrivateGame(rom: RomRef): LaunchResult {
        if (installedPackage() == null) return LaunchResult.NotInstalled
        return when (val prepared = config.applyPrivate(
            rom.productCode,
            rom.filename,
            rom.displayName,
        )) {
            PpssppConfigResult.Success,
            // Preserve the established manual path. The session screen shows
            // its instructions whenever canApply() is false, so an old setup or
            // an unidentifiable compressed dump remains playable.
            PpssppConfigResult.NotConfigured,
            PpssppConfigResult.PermissionMissing,
            PpssppConfigResult.InvalidRoot,
            PpssppConfigResult.UnknownDiscId -> launchGame(rom.uri)
            else -> prepared.asLaunchError() ?: launchGame(rom.uri)
        }
    }

    fun launchPublicGame(rom: Rom): LaunchResult {
        if (installedPackage() == null) return LaunchResult.NotInstalled
        restorePublic(rom)?.let { return it }
        return launchGame(rom.uri)
    }

    /** Restore before showing settings, otherwise PPSSPP displays Emufii's private values. */
    fun openPublicSettings(rom: Rom): LaunchResult {
        if (installedPackage() == null) return LaunchResult.NotInstalled
        restorePublic(rom)?.let { return it }
        return openApp()
    }

    private fun restorePublic(rom: Rom): LaunchResult.Error? =
        config.restorePublic(rom.productCode, rom.filename, rom.displayName).asLaunchError()

    private fun PpssppConfigResult.asLaunchError(): LaunchResult.Error? = when (this) {
        PpssppConfigResult.Success -> null
        PpssppConfigResult.NotConfigured -> LaunchResult.Error(
            context.getString(R.string.ppsspp_config_not_configured),
        )
        PpssppConfigResult.PermissionMissing -> LaunchResult.Error(
            context.getString(R.string.ppsspp_config_permission_missing),
        )
        PpssppConfigResult.InvalidRoot -> LaunchResult.Error(
            context.getString(R.string.ppsspp_config_invalid_root),
        )
        PpssppConfigResult.UnknownDiscId -> LaunchResult.Error(
            context.getString(R.string.ppsspp_config_unknown_game),
        )
        PpssppConfigResult.ActiveOverrides -> LaunchResult.Error(
            context.getString(R.string.ppsspp_config_active_overrides),
        )
        is PpssppConfigResult.Failure -> LaunchResult.Error(detail)
    }
}

/**
 * The package names PPSSPP installs itself under.
 *
 * Gold and the free version carry the same code and the same interface; only the
 * identifier changes, and nothing says the player has the one we expected.
 */
object PpssppPackage {
    val candidates = listOf("org.ppsspp.ppsspp", "org.ppsspp.ppssppgold")
}
