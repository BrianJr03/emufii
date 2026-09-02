package eu.emufii.app.library

import android.content.Context
import eu.emufii.app.eden.pickEden

/**
 * Which build of an emulator to open when the player has several. The heuristic stays
 * the default; this only makes it overridable. A choice pointing at an absent package
 * is ignored and erased, so it cannot come back if the package does.
 * pourquoi : docs/decisions/pilotes-emulateurs.md § A build is chosen, it is no longer guessed
 */
object EmulatorPick {

    private const val PREFS = "emufii_emulator_pick"

    fun chosen(context: Context, console: Console): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pkg = prefs.getString(console.name, null) ?: return null
        if (isInstalled(context, pkg)) return pkg
        prefs.edit().remove(console.name).apply()
        return null
    }

    fun choose(context: Context, console: Console, pkg: String?) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (pkg == null) prefs.edit().remove(console.name).apply()
        else prefs.edit().putString(console.name, pkg).apply()
    }

    /**
     * The single point of passage: the six launchers and the consoles page read this
     * same function, so none can open a build the page does not announce.
     */
    fun packageFor(context: Context, console: Console): String? =
        chosen(context, console) ?: defaultPackage(context, console)

    /**
     * On the Switch the last installed wins: Eden ships a matrix of packages, and the
     * one just laid down is the one meant to open. On equal dates the list's order
     * decides, which keeps our fork first.
     */
    private fun defaultPackage(context: Context, console: Console): String? {
        val variants = variants(context, console)
        return if (console == Console.SWITCH) {
            pickEden(variants.map { it.packageName to it.lastUpdate })
        } else {
            variants.firstOrNull()?.packageName
        }
    }

    /**
     * In the backend's candidate order. The label comes from `getApplicationLabel`
     * rather than a table here: it is what the player reads on their own launcher.
     */
    fun variants(context: Context, console: Console): List<EmulatorVariant> {
        val pm = context.packageManager
        return console.emulatorPackages.mapNotNull { pkg ->
            val info = runCatching { pm.getPackageInfo(pkg, 0) }.getOrNull() ?: return@mapNotNull null
            EmulatorVariant(
                packageName = pkg,
                label = runCatching { pm.getApplicationLabel(info.applicationInfo!!).toString() }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: pkg.substringAfterLast('.'),
                version = info.versionName,
                lastUpdate = info.lastUpdateTime
            )
        }
    }

    private fun isInstalled(context: Context, pkg: String): Boolean =
        runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
}

data class EmulatorVariant(
    val packageName: String,
    val label: String,
    val version: String?,
    val lastUpdate: Long
)
