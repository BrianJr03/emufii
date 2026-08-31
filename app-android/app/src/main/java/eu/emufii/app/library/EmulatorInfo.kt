package eu.emufii.app.library

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import eu.emufii.app.dolphin.DolphinTarget
import eu.emufii.app.netplay.NetplayTarget
import eu.emufii.app.ps2.Ps2Target
import eu.emufii.app.psp.PpssppPackage
import eu.emufii.app.wfc.MelonDsPackage

/**
 * Which emulator plays a console, and whether it is on the device. Read from the
 * system, not from a table here: the version cannot go stale, and the icon stays
 * the emulator's own rather than a copy of someone else's mark in our resources.
 * A console with no emulator is not an error and blocks nothing.
 */
data class EmulatorInfo(
    val console: Console,
    val name: String,
    val installedPackage: String?,
    val version: String?,
    val icon: ImageBitmap?,
    /**
     * Every installed build, not only the one that will open. Empty or single in
     * the ordinary case; beyond that the consoles page has something to ask.
     */
    val variants: List<EmulatorVariant> = emptyList(),
    val chosenExplicitly: Boolean = false
) {
    val installed: Boolean get() = installedPackage != null

    val variant: EmulatorVariant? get() = variants.firstOrNull { it.packageName == installedPackage }
}

/**
 * Gathers the per-backend lists, which stay the authority. Duplicating them here
 * is how the accessibility service and `<queries>` drifted apart once; a test
 * now pins the two together.
 */
val Console.emulatorPackages: List<String>
    get() = when (this) {
        Console.THREE_DS -> NetplayTarget.AZAHAR.packages
        Console.SWITCH -> NetplayTarget.EDEN.packages
        Console.PSP -> PpssppPackage.candidates
        Console.DS -> MelonDsPackage.candidates
        Console.GAMECUBE, Console.WII -> DolphinTarget.packages
        Console.PS2 -> Ps2Target.packages
    }

/**
 * The first installed variant wins, as the launchers pick. Everything is wrapped:
 * a package can vanish between the query and the read, so an unreadable emulator
 * reads as an absent one.
 */
fun emulatorInfo(context: Context, console: Console): EmulatorInfo {
    val pm = context.packageManager
    // The same call the launchers make, so this page announces what will open.
    val variants = EmulatorPick.variants(context, console)
    val pkg = EmulatorPick.packageFor(context, console)
    val version = pkg?.let {
        runCatching { pm.getPackageInfo(it, 0).versionName }.getOrNull()
    }
    val icon = pkg?.let {
        runCatching {
            pm.getApplicationIcon(it).toBitmap(ICON_PX, ICON_PX).asImageBitmap()
        }.getOrNull()
    }
    return EmulatorInfo(
        console = console,
        name = console.backend.emulatorName,
        installedPackage = pkg,
        version = version,
        icon = icon,
        variants = variants,
        chosenExplicitly = EmulatorPick.chosen(context, console) != null
    )
}

/**
 * One entry per console, in enum order. GameCube and Wii both answer Dolphin and
 * stay two lines: someone holding only Wii dumps should not have to infer that
 * the GameCube row covers them.
 */
fun allEmulators(context: Context): List<EmulatorInfo> =
    Console.entries.map { emulatorInfo(context, it) }

/**
 * Big enough for the emulator list, the largest place it is drawn. Rasterised
 * once at load: a launcher icon is often an adaptive drawable.
 */
private const val ICON_PX = 144
