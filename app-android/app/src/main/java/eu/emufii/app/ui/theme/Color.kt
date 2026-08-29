package eu.emufii.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The DUOTONE SHELVES palette. See [Direction].
 *
 * Two colour axes that cross — corail for the social, turquoise for play and
 * system — over warm neutral shells. The logo is the grammar; nothing else
 * carries a hue.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § PALETTE
 */

// ---------------------------------------------------------------- the shell

/**
 * Daylight. A near-white that is only just warm — the shell is the tray the
 * white plates sit on, and it has to be read as light before it is read as
 * cream.
 *
 * Twice moved, in opposite directions, and both moves were right. It began at
 * `#F5F1E8`, four points of luminance under the white plate: a card had no
 * ground and every screen read as one sheet. Deepened to `#EDE6D6`, it got its
 * ground but the whole app went yellow — that value carries ten points of
 * saturation, which on a full screen is not a neutral, it is a colour. The
 * value here keeps a gap the moulding and the shadow can work in while pulling
 * the yellow most of the way out.
 */
val ShellLight = Color(0xFFF1EFEA)
val ShellLightLow = Color(0xFFE2DFD7)

/** Night. Violet-leaning, never neutral grey: the bottom of the logo's teal. */
val ShellDark = Color(0xFF120F1D)
val ShellDarkLow = Color(0xFF090711)

/** OLED. Exactly off. A black pixel is a pixel not lit, and 0xFF050505 is lit. */
val ShellOled = Color(0xFF000000)

// --------------------------------------------------------------- the plates

/** The light theme's plate: white, the foreground tile of the logo. */
val PlateLight = Color(0xFFFFFFFF)
val PlateLightLow = Color(0xFFF7F5F1)

/** Night plate. A flat tile, low-contrast pair for the notch tint. */
val PlateDark = Color(0xFF272238)
val PlateDarkLow = Color(0xFF1C1929)

/**
 * Assombries le 2026-08-28 : depuis 0xFF16131F / 0xFF0F0D17, qui sur un fond
 * exactement eteint lisaient comme du gris clair pose sur du noir — l'ecart
 * plaque/fond faisait plus de bruit que la tuile n'avait de matiere. Le violet
 * de nuit reste (jamais un gris neutre), il descend juste assez pres du fond
 * pour que la separation revienne a l'arete et au biseau, pas au remplissage.
 */
val PlateOled = Color(0xFF0C0A14)
val PlateOledLow = Color(0xFF07060D)

/**
 * The tile's 1 dp edge: warm black, not blue-black. It draws the contour; the
 * bevel below draws the volume.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § Neutres chauds (light)
 */
val EdgeLight = Color(0x52241610)
val EdgeDark = Color(0x2EFFFFFF)
// Remontee avec les plaques : c'est elle qui porte desormais la separation.
val EdgeOled = Color(0x52FFFFFF)

/**
 * The moulding, restored on 2026-08-28. A tile has a lit top-inner rim and a
 * shaded bottom-inner one: one light source, high and slightly left, the same
 * for every surface in the app.
 *
 * These names survived the flat pass as constants nothing drew. They are drawn
 * again — the bicolour palette stayed, the flatness did not.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § MATIÈRE
 */
val BevelLight = Color(0xF2FFFFFF)
val BevelDark = Color(0x33FFFFFF)

/** The bevel's other half: the shade under the lip, warm and never blue. */
val BevelShadeLight = Color(0x1F241610)
val BevelShadeDark = Color(0x59000000)

// ------------------------------------------------------------------- the ink

val InkText = Color(0xFF221B26)
val InkTextMuted = Color(0xFF6E6475)
val InkDarkText = Color(0xFFF0EAF5)
val InkDarkTextMuted = Color(0xFF9B93AC)

/** The glyph ink, warm: named because it is shared with the logo's dark marks. */
val GlyphInk = InkText

// ----------------------------------------------------------------- the axes

/**
 * The coral axis: the social. Sessions, friends, join, presence. Creating a link.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § Deux axes sémantiques
 */
object Coral {
    val bright = Color(0xFFEE6FA3)
    val deep = Color(0xFFC24B7E)
    val ink = Color(0xFF5A1D3E)
    val darkBright = Color(0xFFF793BC)
    val soft: Color get() = bright.copy(alpha = 0.20f)
}

/**
 * The teal axis: play and system. Launch, confirm, navigate, the library — and
 * the default cursor ring.
 */
object Teal {
    val bright = Color(0xFF3FCFC0)
    val deep = Color(0xFF0E9C8F)
    val ink = Color(0xFF0A4A44)
    val darkBright = Color(0xFF5CE0D2)
    val soft: Color get() = bright.copy(alpha = 0.20f)
}

/**
 * The backdrop's two shelves: the logo's coral and teal tiles, blown up behind
 * the app. Held apart from [Coral]/[Teal] because a ground tint and a control
 * colour drift for different reasons — the ground answers to the cover art it
 * sits under, the control to contrast.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § MATIÈRE (fond)
 */
object Shelf {
    /** Fill alpha of a shelf tile, per theme. */
    const val fillLight = 0.17f
    const val fillDark = 0.14f
    /**
     * **L'OLED tire plus haut que les deux autres, exprès.**
     *
     * Le noir absolu ne renvoie rien : une etagere a 8 % y perdait sa couleur
     * avant d'avoir dit de quel axe elle parlait, et il ne restait d'elle que
     * son contour — donc un trait, ce qui est precisement ce que l'etagere
     * n'est pas. Montee a 13 %, la tuile redevient une surface, et le contour
     * cesse d'etre la seule chose qu'on voie d'elle.
     * pourquoi : docs/decisions/theme-duotone-shelves.md § MATIÈRE (fond)
     */
    const val fillOled = 0.13f

    /**
     * Its contour, which is what makes it a tile rather than a halo — but only
     * just. Drawn at 30 % on the first try, it cut a hard line straight through
     * a row of game titles: the ground had started competing with the content
     * it is under. Enough to find the corner, not enough to read as a rule.
     */
    const val edgeLight = 0.13f
    const val edgeDark = 0.11f
    const val edgeOled = 0.10f
}

/** Depth violet: links, the backdrop's sheen, the logo's gradient bottom. */
val Violet = Color(0xFF6B72E0)
val VioletDark = Color(0xFF8E93EC)

// -------------------------------------------------------------- semantic set

/** Good, pulled towards teal. */
val GoodLight = Color(0xFF1FA98B)
val GoodDark = Color(0xFF3BC4A6)

/** Warning amber. */
val WarnLight = Color(0xFFC98A12)
val WarnDark = Color(0xFFE3A83C)

/** Error, pulled towards coral. */
val ErrorLight = Color(0xFFE5604F)
val ErrorDark = Color(0xFFF0796A)

/** Info blue. */
val InfoLight = Color(0xFF5A8FD8)
val InfoDark = Color(0xFF82AFE6)

// ------------------------------------------------------ legacy public aliases
// The migrated screens still name the old world's colours. They map onto the
// new palette; do not add new uses.

/** @deprecated Use [Teal].bright. */
@Deprecated("DUOTONE SHELVES: use Teal.bright", ReplaceWith("Teal.bright"))
val TrayCyan = Teal.bright

/** @deprecated Use [Teal].soft. */
@Deprecated("DUOTONE SHELVES: use Teal.soft", ReplaceWith("Teal.soft"))
val TrayCyanSoft = Teal.soft

/** @deprecated Use [Teal].ink. */
@Deprecated("DUOTONE SHELVES: use Teal.ink", ReplaceWith("Teal.ink"))
val TrayCyanInk = Teal.ink

/** @deprecated Use [Teal].deep. */
@Deprecated("DUOTONE SHELVES: use Teal.deep", ReplaceWith("Teal.deep"))
val TrayCyanDeep = Teal.deep

/** @deprecated Use [ErrorLight] on light themes, [ErrorDark] on dark. */
@Deprecated("DUOTONE SHELVES: use ErrorLight/ErrorDark", ReplaceWith("ErrorLight"))
val ShellRed = ErrorLight

/** @deprecated The green ring is gone; "ready" is [GoodLight] / [GoodDark]. */
@Deprecated("DUOTONE SHELVES: use GoodLight/GoodDark", ReplaceWith("GoodLight"))
val AccentGreen = GoodLight

/** @deprecated The single accent is gone; this is the teal axis's bright cut. */
@Deprecated("DUOTONE SHELVES: use Teal.bright", ReplaceWith("Teal.bright"))
val Accent = Teal.bright

/** @deprecated The single accent is gone; this is the teal axis's soft cut. */
@Deprecated("DUOTONE SHELVES: use Teal.soft", ReplaceWith("Teal.soft"))
val AccentSoft = Teal.soft
