package eu.emufii.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The DUOTONE SHELVES palette. See [Direction].
 *
 * Two colour axes that cross: corail for the social, turquoise for play and
 * system: over warm neutral shells. The logo is the grammar; nothing else
 * carries a hue.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § PALETTE (numbered contract)
 */

// ---------------------------------------------------------------- the shell

/**
 * Daylight: a near-white that is only just warm, read as light before cream. Two
 * bounds it sits between. `#F5F1E8` is four points of luminance under the white
 * plate, so a card has no ground and the screen reads as one sheet; `#EDE6D6`
 * carries ten points of saturation, which over a full screen is a colour rather
 * than a neutral.
 */
val ShellLight = Color(0xFFF1EFEA)
val ShellLightLow = Color(0xFFE2DFD7)

/** Night. Violet-leaning, never neutral grey: the bottom of the logo's teal. */
val ShellDark = Color(0xFF120F1D)
val ShellDarkLow = Color(0xFF090711)

/** OLED. Exactly off. A black pixel is a pixel not lit, and 0xFF050505 is lit. */
val ShellOled = Color(0xFF000000)

// --------------------------------------------------------------- the plates

val PlateLight = Color(0xFFFFFFFF)
val PlateLightLow = Color(0xFFF7F5F1)

val PlateDark = Color(0xFF272238)
val PlateDarkLow = Color(0xFF1C1929)

/**
 * Close to the ground on purpose, so the separation falls to the edge and the
 * bevel rather than the fill: over an exactly-off background a lighter plate
 * (0xFF16131F) reads as grey on black. Still night violet, never neutral grey.
 */
val PlateOled = Color(0xFF0C0A14)
val PlateOledLow = Color(0xFF07060D)

/**
 * Warm black, not blue-black: the edge draws the contour, the bevel the volume.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § Warm neutrals (light), the cream tile extended
 */
val EdgeLight = Color(0x52241610)
val EdgeDark = Color(0x2EFFFFFF)
// Raised with the plates, since it now carries the separation.
val EdgeOled = Color(0x52FFFFFF)

/**
 * The moulding: a tile has a lit top-inner rim and a shaded bottom-inner one,
 * from one light source, high and slightly left, shared by every surface.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § MATERIAL (replaces Plastic.kt)
 */
val BevelLight = Color(0xF2FFFFFF)
val BevelDark = Color(0x33FFFFFF)

/** The shade under the lip, warm and never blue. */
val BevelShadeLight = Color(0x1F241610)
val BevelShadeDark = Color(0x59000000)

// ------------------------------------------------------------------- the ink

val InkText = Color(0xFF221B26)
val InkTextMuted = Color(0xFF6E6475)
val InkDarkText = Color(0xFFF0EAF5)
val InkDarkTextMuted = Color(0xFF9B93AC)

val GlyphInk = InkText

// ----------------------------------------------------------------- the axes

/**
 * The social axis: sessions, friends, join, presence.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § Two semantic axes
 */
object Coral {
    val bright = Color(0xFFEE6FA3)
    val deep = Color(0xFFC24B7E)
    val ink = Color(0xFF5A1D3E)
    val darkBright = Color(0xFFF793BC)
    val soft: Color get() = bright.copy(alpha = 0.20f)
}

/** Play and system: launch, confirm, navigate, and the default cursor ring. */
object Teal {
    val bright = Color(0xFF3FCFC0)
    val deep = Color(0xFF0E9C8F)
    val ink = Color(0xFF0A4A44)
    val darkBright = Color(0xFF5CE0D2)
    val soft: Color get() = bright.copy(alpha = 0.20f)
}

/**
 * The backdrop's two shelves, the logo's tiles blown up behind the app. Apart
 * from [Coral]/[Teal] because a ground tint answers to the cover art it sits
 * under and a control colour to contrast.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § MATERIAL (background)
 */
object Shelf {
    const val fillLight = 0.17f
    const val fillDark = 0.14f
    /**
     * Higher than the other two on purpose: absolute black returns nothing, and
     * at 8 % a shelf lost its colour before naming its axis, leaving only its
     * contour. A shelf is a surface, not a line.
     */
    const val fillOled = 0.13f

    /**
     * Its contour, what makes it a tile rather than a halo, but only just: at
     * 30 % it cuts a hard line through a row of game titles. Enough to find the
     * corner, not enough to read as a rule.
     */
    const val edgeLight = 0.13f
    const val edgeDark = 0.11f
    const val edgeOled = 0.10f
}

val Violet = Color(0xFF6B72E0)
val VioletDark = Color(0xFF8E93EC)

// -------------------------------------------------------------- semantic set

val GoodLight = Color(0xFF1FA98B)
val GoodDark = Color(0xFF3BC4A6)

val WarnLight = Color(0xFFC98A12)
val WarnDark = Color(0xFFE3A83C)

val ErrorLight = Color(0xFFE5604F)
val ErrorDark = Color(0xFFF0796A)

val InfoLight = Color(0xFF5A8FD8)
val InfoDark = Color(0xFF82AFE6)

// ------------------------------------------------------ legacy public aliases
// The migrated screens still name the old world's colours. They map onto the
// new palette; do not add new uses.

@Deprecated("DUOTONE SHELVES: use Teal.bright", ReplaceWith("Teal.bright"))
val TrayCyan = Teal.bright

@Deprecated("DUOTONE SHELVES: use Teal.soft", ReplaceWith("Teal.soft"))
val TrayCyanSoft = Teal.soft

@Deprecated("DUOTONE SHELVES: use Teal.ink", ReplaceWith("Teal.ink"))
val TrayCyanInk = Teal.ink

@Deprecated("DUOTONE SHELVES: use Teal.deep", ReplaceWith("Teal.deep"))
val TrayCyanDeep = Teal.deep

@Deprecated("DUOTONE SHELVES: use ErrorLight/ErrorDark", ReplaceWith("ErrorLight"))
val ShellRed = ErrorLight

@Deprecated("DUOTONE SHELVES: use GoodLight/GoodDark", ReplaceWith("GoodLight"))
val AccentGreen = GoodLight

@Deprecated("DUOTONE SHELVES: use Teal.bright", ReplaceWith("Teal.bright"))
val Accent = Teal.bright

@Deprecated("DUOTONE SHELVES: use Teal.soft", ReplaceWith("Teal.soft"))
val AccentSoft = Teal.soft
