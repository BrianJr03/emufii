package eu.emufii.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * DUOTONE SHELVES: two axes, not one accent. Corail = the social, turquoise =
 * play and system. Read them from [Coral] and [Teal]; go through
 * `colorScheme.primary` where Material has a slot.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § Deux axes sémantiques
 */

/**
 * A colour axis in the cuts the world actually spends.
 *
 * @property bright the cursor ring, the glow, the filled action on dark themes.
 * @property deep the filled action on the light theme, under white text.
 * @property ink what is written on [bright].
 * @property soft the ghost pills' fill, [bright] at a fifth.
 */
data class AccentCuts(
    val bright: Color,
    val deep: Color,
    val ink: Color
) {
    /** Never a solid fill: the secondary pills take the colour at a fifth. */
    val soft: Color get() = bright.copy(alpha = 0.20f)
}

/** The axis in force for the game/system domain: teal by default. */
val TealCuts = AccentCuts(Teal.bright, Teal.deep, Teal.ink)

/** The social axis. */
val CoralCuts = AccentCuts(Coral.bright, Coral.deep, Coral.ink)

/**
 * The cuts behind `colorScheme.primary` and the default ring. Kept as a local
 * because the ring is drawn by hand; it carries the teal axis (play + system),
 * corail zones override it via [LocalRingTone].
 */
val LocalAccent = staticCompositionLocalOf { TealCuts }

/**
 * **Il n'y a plus d'accent configurable, ni de Material You.**
 *
 * Le monde duotone tient sur deux axes qui *veulent dire* quelque chose —
 * turquoise le jeu et le systeme, corail le lien social — et l'anneau du
 * curseur nomme la zone ou l'on se trouve. Un accent pris du fond d'ecran
 * repeignait le turquoise sans repeindre le corail : la distinction cessait
 * d'etre lisible, et c'est la seule chose que la couleur dit dans cette app.
 *
 * Le reglage a donc ete retire le 2026-08-28. Il ne tenait deja plus qu'a un
 * fil : l'anneau, les pastilles d'etat, l'interrupteur et les boutons prennent
 * leurs coupes des deux axes en dur, si bien que « Couleur systeme » ne teintait
 * que quelques elements secondaires — un reglage qu'on active sans voir ce qu'il
 * fait. Le rendre visible aurait demande de sacrifier la semantique ; le retirer
 * ne coute rien qu'un interrupteur.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § Réglages
 */
