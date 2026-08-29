package eu.emufii.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.RippleDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The three schemes of DUOTONE SHELVES: warm neutrals, the teal axis in
 * Material's primary slot, good/error from the centralised semantic set.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § PALETTE
 */
private fun lightScheme(accent: AccentCuts) = lightColorScheme(
    primary = accent.deep,
    onPrimary = Color.White,
    primaryContainer = accent.soft,
    onPrimaryContainer = accent.ink,
    secondary = GoodLight,
    onSecondary = Color.White,
    tertiary = Coral.deep,
    onTertiary = Color.White,
    background = ShellLight,
    onBackground = InkText,
    surface = PlateLight,
    onSurface = InkText,
    surfaceVariant = PlateLightLow,
    onSurfaceVariant = InkTextMuted,
    surfaceContainer = PlateLight,
    surfaceContainerLow = PlateLightLow,
    surfaceContainerHigh = PlateLight,
    outline = EdgeLight,
    outlineVariant = EdgeLight,
    error = ErrorLight,
    onError = Color.White
)

private fun darkScheme(accent: AccentCuts) = darkColorScheme(
    primary = accent.bright,
    onPrimary = accent.ink,
    primaryContainer = accent.soft,
    onPrimaryContainer = Color.White,
    secondary = GoodDark,
    onSecondary = Color(0xFF04241C),
    tertiary = Coral.darkBright,
    onTertiary = Coral.ink,
    background = ShellDark,
    onBackground = InkDarkText,
    surface = PlateDark,
    onSurface = InkDarkText,
    surfaceVariant = PlateDarkLow,
    onSurfaceVariant = InkDarkTextMuted,
    surfaceContainer = PlateDark,
    surfaceContainerLow = PlateDarkLow,
    surfaceContainerHigh = PlateDark,
    outline = EdgeDark,
    outlineVariant = EdgeDark,
    error = ErrorDark,
    onError = Color(0xFF2B0805)
)

private fun oledScheme(accent: AccentCuts) = darkScheme(accent).copy(
    background = ShellOled,
    surface = PlateOled,
    surfaceVariant = PlateOledLow,
    surfaceContainer = PlateOled,
    surfaceContainerLow = PlateOledLow,
    surfaceContainerHigh = PlateOled,
    outline = EdgeOled,
    outlineVariant = EdgeOled
)

/**
 * Whether the app is drawing dark right now.
 *
 * Components have to read this rather than [isSystemInDarkTheme], because the
 * in-app theme setting can disagree with the phone. Half the app's surfaces pick
 * their own colours, the wallpaper, the cards, the floating pills, so a single
 * component still asking the system would light up wrongly the moment someone
 * chooses Light on a dark phone.
 */
val LocalEmufiiDarkTheme = staticCompositionLocalOf { false }

/**
 * Whether that dark is the OLED one: pure black background, cards switched off.
 *
 * Deliberately kept separate from [LocalEmufiiDarkTheme] rather than replacing it
 * with a three-valued enum: OLED is a dark, and the forty-four places asking "am
 * I dark?" always want "yes". Only the background and the cards' fill need the
 * distinction, and there are three of them.
 *
 * Invariant: `LocalEmufiiOledTheme.current` is only true when
 * `LocalEmufiiDarkTheme.current` is.
 */
val LocalEmufiiOledTheme = staticCompositionLocalOf { false }

@Composable
fun EmufiiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    oled: Boolean = false,
    content: @Composable () -> Unit
) {
    // L'axe du jeu et du systeme, en dur : il n'y a plus d'accent a resoudre.
    // Les zones corail lisent leurs coupes dans `Coral` / `colorScheme.tertiary`,
    // pas dans ce local.
    val cuts = TealCuts
    val oledTheme = oled && darkTheme
    CompositionLocalProvider(
        LocalEmufiiDarkTheme provides darkTheme,
        LocalEmufiiOledTheme provides oledTheme,
        LocalAccent provides cuts
    ) {
        // Focus tints nothing. Material lays a grey veil over any focused or
        // hovered control; on a handheld, where the cursor is permanently
        // somewhere, that paints the selected element grey over the teal ring
        // that already says where you are.
        CompositionLocalProvider(LocalRippleConfiguration provides NoFocusRipple) {
            MaterialTheme(
                colorScheme = when {
                    oledTheme -> oledScheme(cuts)
                    darkTheme -> darkScheme(cuts)
                    else -> lightScheme(cuts)
                },
                typography = Typography,
                content = content
            )
        }
    }
}

/**
 * Material's ripple, stripped of its focus and hover veils.
 *
 * `null` would have disabled the ripple entirely, press included, and that is
 * the only one of the four that answers a gesture from the user.
 */
@OptIn(ExperimentalMaterial3Api::class)
private val NoFocusRipple = RippleConfiguration(
    rippleAlpha = RippleAlpha(
        draggedAlpha = RippleDefaults.RippleAlpha.draggedAlpha,
        focusedAlpha = 0f,
        hoveredAlpha = 0f,
        pressedAlpha = RippleDefaults.RippleAlpha.pressedAlpha
    )
)
