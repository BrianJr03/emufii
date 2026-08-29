package eu.emufii.app.azahar

/**
 * Sous quels noms Azahar s'installe.
 *
 * **Ne jamais remplacer un nom par un autre** : les trois cohabitent selon d'ou
 * vient l'installation. L'ordre est celui de la preference. Ce qui decide
 * vraiment de la pilotabilite est [NetplayUiSupport], pas le nom.
 * pourquoi : docs/decisions/pilotes-emulateurs.md § Azahar n'a pas fini de changer d'identifiant
 */
object AzaharPackage {
    const val MAIN = "org.azahar_emu.azahar"
    const val DEBUG = "org.azahar_emu.azahar.debug"

    /** L'identifiant hérité de Lime3DS, que des builds Azahar portent encore. */
    const val LEGACY = "io.github.lime3ds.android"

    val candidates = listOf(MAIN, DEBUG, LEGACY)
}
