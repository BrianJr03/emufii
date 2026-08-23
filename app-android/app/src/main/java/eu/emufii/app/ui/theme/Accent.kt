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
 * The one accent, in the three cuts the world actually spends.
 *
 * Three and not one, because a single hue cannot do the accent's three jobs:
 * [bright] has to be seen at a glance on a black tray, [deep] has to carry white
 * text, and [ink] has to be read *on* [bright]. The shipped cyan was already
 * built this way — the deep cut exists because white on the light cyan sits at
 * 2.2:1 — so making the accent a choice means carrying the same three cuts for
 * every colour, not just swapping one hex.
 *
 * @property bright the cursor, its glow, and the filled action on the dark themes.
 * @property deep the filled action on the light theme, under white text.
 * @property ink what is written on [bright].
 * @property soft the ghost pills' fill and the primary container, [bright] veiled.
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
 * The accent in force, for the two places that cannot read it off the colour
 * scheme: the cursor's ring, which is drawn by hand, and the filled action,
 * which needs the deep cut Material has no slot for.
 *
 * Everything else goes through `MaterialTheme.colorScheme.primary`, which
 * [EmufiiTheme] fills from the same source, so a new screen gets the chosen
 * accent without knowing this exists.
 */
val LocalAccent = staticCompositionLocalOf { AccentCuts(TrayCyan, TrayCyanDeep, TrayCyanInk) }

/**
 * The cuts for a chosen accent.
 *
 * The fixed colours were not picked by eye: each [deep] is its base darkened in
 * HSL until it clears 4.6:1 under white, and each [ink] until it clears 5:1 on
 * its own base — the very ratios the shipped cyan measures, so a chosen colour
 * is exactly as legible as the one it replaces. Darkening in lightness rather
 * than by scaling the channels is what keeps the hue: scaling drains a light
 * colour to near-black before it ever reaches the ratio.
 *
 * Green is deliberately absent: it lands on [AccentGreen], which means
 * connected, and an accent that reads as a reserved meaning takes that meaning
 * away from both.
 *
 * Red was absent for the same reason and is now offered anyway, on the user's
 * call (2026-08-23). It is worth knowing what that costs: the cursor then wears
 * very nearly the colour `{colors.shell-red}` spends on errors and destructive
 * confirmations, so on that accent "this is where you are" and "this will
 * delete something" stop being told apart by hue alone. The red chosen here
 * sits at a cooler hue than the shell red to keep some daylight between them.
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
    // White's ink is the app's own dark ink rather than the grey the ratio
    // rule would stop at. The rule sets a floor, not a target: mid-grey on
    // white clears 5:1 and still reads as a disabled label, where the ink used
    // everywhere else clears 14:1 and reads as writing.
    AppAccent.WHITE -> AccentCuts(Color(0xFFFFFFFF), Color(0xFF757575), Color(0xFF1B2430))
}

/**
 * The wallpaper's colour, as Android extracted it.
 *
 * Taken from the platform's own two schemes rather than derived here, because
 * they already carry the contrast guarantees this needs: the dark scheme's
 * `primary` is a light tone, made to be read on a dark ground — the cursor's
 * job; the light scheme's `primary` is a dark tone, made to carry white — the
 * filled action's job; and `onPrimary` of the dark scheme is, by construction,
 * what is legible on the tone we took for [AccentCuts.bright].
 *
 * Below Android 12 there is no extracted colour at all, and the tray's cyan
 * stands in. The setting still appears: it is not a lie, it says "follow the
 * system", and on a phone with nothing to follow that is the app's own colour.
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
