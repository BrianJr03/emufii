package eu.emufii.app.ui.theme

import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import eu.emufii.app.settings.AppAccent

/**
 * The one accent, in the three cuts the world actually spends. One hue cannot
 * do all three jobs.
 *
 * @property bright the cursor, its glow, and the filled action on the dark themes.
 * @property deep the filled action on the light theme, under white text.
 * @property ink what is written on [bright].
 * @property soft the ghost pills' fill and the primary container, [bright] veiled.
 * pourquoi : docs/decisions/direction-visuelle.md § Un accent, mais toujours en trois coupes
 */
data class AccentCuts(
    val bright: Color,
    val deep: Color,
    val ink: Color
) {
    /** Never a solid fill: the secondary pills take the accent at a fifth. */
    val soft: Color get() = bright.copy(alpha = 0.20f)
}

/**
 * The accent in force, for the only two places that cannot read it off the
 * colour scheme. Everything else goes through `colorScheme.primary`.
 * pourquoi : docs/decisions/direction-visuelle.md § Les deux seuls endroits qui lisent l'accent à la main
 */
val LocalAccent = staticCompositionLocalOf { AccentCuts(TrayCyan, TrayCyanDeep, TrayCyanInk) }

/**
 * The cuts for a chosen accent, measured rather than picked by eye:every deep
 * clears 4.6:1 under white, every ink 5:1 on its own base.
 *
 * Green is deliberately absent (it means connected). Red is offered despite
 * colliding with the shell's error red, on the user's call.
 * pourquoi : docs/decisions/direction-visuelle.md § Un accent, mais toujours en trois coupes
 */
@Composable
fun accentCuts(accent: AppAccent): AccentCuts = when (accent) {
    AppAccent.SYSTEM -> systemAccent()
    AppAccent.CYAN -> AccentCuts(TrayCyan, TrayCyanDeep, TrayCyanInk)
    AppAccent.AMBER -> AccentCuts(Color(0xFFF0A62B), Color(0xFFA2690B), Color(0xFF583906))
    AppAccent.VIOLET -> AccentCuts(Color(0xFFA183F0), Color(0xFF8058EB), Color(0xFF290E71))
    AppAccent.ROSE -> AccentCuts(Color(0xFFF072B6), Color(0xFFDD1782), Color(0xFF5C0A36))
    AppAccent.YELLOW -> AccentCuts(Color(0xFFF2CE1B), Color(0xFF887308), Color(0xFF5F5005))
    AppAccent.RED -> AccentCuts(Color(0xFFF04747), Color(0xFFE71313), Color(0xFF2D0404))
    // White's ink is the app's dark ink, not the grey the ratio would stop at:
    // the rule is a floor, not a target.
    // pourquoi : docs/decisions/direction-visuelle.md § Un accent, mais toujours en trois coupes
    AppAccent.WHITE -> AccentCuts(Color(0xFFFFFFFF), Color(0xFF757575), Color(0xFF1B2430))
}

/**
 * The wallpaper's colour, taken from the platform's two schemes rather than
 * derived, because they already carry the contrast guarantees.
 *
 * Below Android 12 the tray's cyan stands in; the setting still appears.
 * pourquoi : docs/decisions/direction-visuelle.md § L'accent système est pris à la plateforme, pas dérivé
 */
@Composable
private fun systemAccent(): AccentCuts {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return AccentCuts(TrayCyan, TrayCyanDeep, TrayCyanInk)
    }
    val context = LocalContext.current
    val dark = dynamicDarkColorScheme(context)
    val light = dynamicLightColorScheme(context)
    return AccentCuts(
        bright = dark.primary,
        deep = light.primary,
        ink = dark.onPrimary
    )
}
