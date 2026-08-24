package eu.emufii.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The HOME MENU palette. See [Direction].
 *
 * Three grounds, one accent, and nothing else has a hue.
 * pourquoi : docs/decisions/direction-visuelle.md § Trois sols, un accent, et rien d'autre n'a de teinte
 */

// ---------------------------------------------------------------- the shell

/** Daylight. A cool silver, not white: the plates are white. */
val ShellLight = Color(0xFFDCE1E9)
val ShellLightLow = Color(0xFFC9D0DB)

/** Night. The same tray under a lamp: blue-black, never neutral grey. */
val ShellDark = Color(0xFF121721)
val ShellDarkLow = Color(0xFF0C1017)

/** OLED. Exactly off. A black pixel is a pixel not lit, and 0xFF050505 is lit. */
val ShellOled = Color(0xFF000000)

// --------------------------------------------------------------- the plates

/** White plastic, the light theme's plate. */
val PlateLight = Color(0xFFFDFDFE)
val PlateLightLow = Color(0xFFF0F2F6)

/** Night plastic. Lit from the top, so it comes in a pair. */
val PlateDark = Color(0xFF222A38)
val PlateDarkLow = Color(0xFF19202B)

val PlateOled = Color(0xFF15181F)
val PlateOledLow = Color(0xFF0B0D11)

/**
 * The moulded edge, and the only separator left on OLED where a shadow draws
 * nothing. Do not lighten it back: at 0x1F the contour vanished at a glance.
 * pourquoi : docs/decisions/direction-visuelle.md § Trois sols, un accent, et rien d'autre n'a de teinte
 */
val EdgeLight = Color(0x3D0B1220)
val EdgeDark = Color(0x1FFFFFFF)
val EdgeOled = Color(0x33FFFFFF)

/** The lit top of a moulded edge, one hairline inside the plate's contour. */
val BevelLight = Color(0xCCFFFFFF)
val BevelDark = Color(0x1AFFFFFF)

// ------------------------------------------------------------------- the ink

val InkText = Color(0xFF1B2430)
val InkTextMuted = Color(0xFF5C6675)
val InkDarkText = Color(0xFFEAEFF6)
val InkDarkTextMuted = Color(0xFF95A0B1)

// --------------------------------------------------------------- the cursor

/**
 * Tray cyan: the cursor, the primary action, the current selection. Nothing else.
 * pourquoi : docs/decisions/direction-visuelle.md § Le cyan est dépensé sur le curseur, et sur rien d'autre
 */
val TrayCyan = Color(0xFF14B4E4)
val TrayCyanSoft = Color(0x3314B4E4)

/** Its deep end, for text and icons that have to sit ON cyan-lit surfaces. */
val TrayCyanInk = Color(0xFF04384B)

/**
 * The filled-button cyan: [TrayCyan] under white text measures 2.2:1, this cut
 * 4.6:1.
 * pourquoi : docs/decisions/direction-visuelle.md § Le cyan est dépensé sur le curseur, et sur rien d'autre
 */
val TrayCyanDeep = Color(0xFF0A7899)

/** Warning red. Errors and destructive confirmations only, nothing else. */
val ShellRed = Color(0xFFE0452F)

/** Legacy alias: the green ring is gone, but screens still name it for "ready". */
val AccentGreen = Color(0xFF3ECF9A)
val Accent = TrayCyan
val AccentSoft = TrayCyanSoft
