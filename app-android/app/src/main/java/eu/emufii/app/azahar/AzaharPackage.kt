package eu.emufii.app.azahar

/**
 * Sous quels noms Azahar s'installe.
 *
 * **Azahar n'a pas fini de changer d'identifiant.** Il vient de Lime3DS, qui
 * venait de Citra, et le renommage s'est arrêté à mi-chemin : ses classes sont
 * toujours `org.citra.citra_emu.*`, et une partie de ses canaux de distribution
 * publie encore sous l'`applicationId` de Lime3DS,
 * `io.github.lime3ds.android`. Constaté sur la Thor le 2026-08-26 : le build
 * installé (`263745c1d-vanilla`) porte ce nom-là, expose bien `btn_create`,
 * `btn_join`, `ip_address`, `btn_confirm` et `menu_multiplayer`, et lance bien
 * `org.citra.citra_emu.activities.EmulationActivity` — c'est Azahar en tout
 * point sauf le nom du paquet. Emufii ne cherchait que `org.azahar_emu.*` et
 * annonçait donc « pas installé » devant un émulateur parfaitement pilotable.
 *
 * Ne pas remplacer un nom par un autre : les trois cohabitent selon d'où vient
 * l'installation, et un joueur peut en avoir deux. L'ordre est celui de la
 * préférence quand c'est le cas — le nom Azahar d'abord, l'héritage en dernier.
 *
 * Ce qui décide *vraiment* si un build est pilotable n'est pas son nom mais
 * `NetplayUiSupport.isPresent`, qui demande à ses ressources si le formulaire
 * existe. Ajouter un nom ici ne fait donc courir aucun risque : un build sans
 * interface multijoueur est refusé au même endroit qu'avant.
 */
object AzaharPackage {
    const val MAIN = "org.azahar_emu.azahar"
    const val DEBUG = "org.azahar_emu.azahar.debug"

    /** L'identifiant hérité de Lime3DS, que des builds Azahar portent encore. */
    const val LEGACY = "io.github.lime3ds.android"

    val candidates = listOf(MAIN, DEBUG, LEGACY)
}
