package eu.emufii.app.library

import android.content.Context
import eu.emufii.app.eden.pickEden

/**
 * Quelle **build** d'un emulateur Emufii doit ouvrir, quand le joueur en a
 * plusieurs.
 *
 * L'heuristique reste le defaut ; ceci ne fait que la rendre surchargeable. Un
 * choix qui pointe un paquet absent est ignore **et efface**, pour ne pas
 * ressusciter si le paquet revient.
 * pourquoi : docs/decisions/pilotes-emulateurs.md § Une build se choisit, elle ne se devine plus
 */
object EmulatorPick {

    private const val PREFS = "emufii_emulator_pick"

    /**
     * La build choisie pour [console], ou null quand le joueur n'a rien choisi
     * ou que son choix n'est plus installe.
     */
    fun chosen(context: Context, console: Console): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pkg = prefs.getString(console.name, null) ?: return null
        if (isInstalled(context, pkg)) return pkg
        prefs.edit().remove(console.name).apply()
        return null
    }

    /** Fixe la build, ou revient au defaut avec [pkg] nul. */
    fun choose(context: Context, console: Console, pkg: String?) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (pkg == null) prefs.edit().remove(console.name).apply()
        else prefs.edit().putString(console.name, pkg).apply()
    }

    /**
     * Le paquet a ouvrir pour [console] : le choix du joueur s'il tient, sinon
     * le defaut.
     *
     * **Seul point de passage.** Les six lanceurs appellent ceci, et la page des
     * consoles lit la meme fonction : aucun ne peut donc ouvrir une build que
     * la page n'annonce pas. C'est la regle qui manquait — chaque lanceur
     * tranchait chez lui, et deux d'entre eux ne tranchaient deja pas pareil.
     */
    fun packageFor(context: Context, console: Console): String? =
        chosen(context, console) ?: defaultPackage(context, console)

    /**
     * Ce qu'on ouvre quand le joueur n'a rien choisi.
     *
     * Pour la Switch, **la derniere installee gagne** : Eden se decline en une
     * matrice de paquets, et celle qu'on vient de poser est precisement celle
     * qu'on voulait ouvrir. A dates egales, l'ordre de la liste tranche, ce qui
     * garde notre fork devant. Le calcul vit dans [pickEden], que des tests
     * figent.
     *
     * Pour les cinq autres, l'ordre de la liste de candidats du backend, qui
     * est deja classee par preference.
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
     * Toutes les builds reellement installees pour [console], dans l'ordre de
     * la liste de candidats du backend.
     *
     * Le libelle vient du systeme (`getApplicationLabel`) et non d'une table
     * ici : c'est ce que le joueur lit sur son propre lanceur, et c'est la
     * seule chose qui distingue « Azahar » de « Lime3DS » quand les deux
     * jouent la 3DS.
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

/** Une build installee : son paquet, son nom tel que le systeme l'affiche, sa version. */
data class EmulatorVariant(
    val packageName: String,
    val label: String,
    val version: String?,
    val lastUpdate: Long
)
