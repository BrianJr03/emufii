package eu.emufii.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Two colour axes rather than one accent: coral for the social areas, teal for
 * play and system. There is no configurable accent and no Material You; a
 * wallpaper colour would repaint teal without repainting coral.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § Two semantic axes
 */

/**
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
    val soft: Color get() = bright.copy(alpha = 0.20f)
}

val TealCuts = AccentCuts(Teal.bright, Teal.deep, Teal.ink)

val CoralCuts = AccentCuts(Coral.bright, Coral.deep, Coral.ink)

/** Backs `colorScheme.primary` and the ring; coral zones override via [LocalRingTone]. */
val LocalAccent = staticCompositionLocalOf { TealCuts }
