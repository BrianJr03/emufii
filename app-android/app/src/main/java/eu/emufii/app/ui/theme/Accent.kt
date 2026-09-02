package eu.emufii.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * No configurable accent and no Material You: a wallpaper colour would repaint teal
 * without repainting coral.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § Two semantic axes
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
