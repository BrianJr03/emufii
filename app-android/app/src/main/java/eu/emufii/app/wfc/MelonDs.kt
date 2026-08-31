package eu.emufii.app.wfc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import eu.emufii.app.R
import eu.emufii.app.azahar.LaunchResult
import eu.emufii.app.library.Console
import eu.emufii.app.library.EmulatorPick

/**
 * Read off the 2.0.1 APK's manifest: `EmulatorActivity` is exported with an
 * intent-filter on melonDS's `LAUNCH_ROM`/`LAUNCH_FIRMWARE`, and takes the ROM
 * from `intent.data` as a URI. It reads its library through SAF, so a
 * `content://` is the expected way in, which is what our library holds and what
 * Dolphin's path-based `AutoStartFile` cannot take.
 *
 * melonDS DualS is a rebrand: the classes are still `me.magnum.melonds.*`, so
 * [EMULATOR_ACTIVITY] is unchanged, but its actions carry the applicationId,
 * hence [actionLaunchRom] deriving from the installed package.
 * pourquoi : docs/PHASE1_SCOUT_MELONDS_DUALS.md
 */
object MelonDsPackage {
    const val MAIN = "me.magnum.melonds"
    const val DEBUG = "me.magnum.melonds.debug"
    const val DUALS = "me.magnum.melondualds"

    val candidates = listOf(MAIN, DEBUG, DUALS)

    const val EMULATOR_ACTIVITY = "me.magnum.melonds.ui.emulator.EmulatorActivity"

    fun actionLaunchRom(pkg: String) = "$pkg.LAUNCH_ROM"

    /** melonDS also looks for the URI under this extra; harmless to send both. */
    const val EXTRA_URI = "uri"
}

class MelonDs(private val context: Context) {

    fun installedPackage(): String? = EmulatorPick.packageFor(context, Console.DS)

    fun launchGame(romUri: Uri): LaunchResult {
        val pkg = installedPackage() ?: return LaunchResult.NotInstalled
        val intent = Intent(MelonDsPackage.actionLaunchRom(pkg)).apply {
            component = ComponentName(pkg, MelonDsPackage.EMULATOR_ACTIVITY)
            data = romUri
            putExtra(MelonDsPackage.EXTRA_URI, romUri.toString())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching {
            context.startActivity(intent)
            LaunchResult.Success
        }.getOrElse { LaunchResult.Error(it.message ?: context.getString(R.string.err_launch)) }
    }
}
