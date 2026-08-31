package eu.emufii.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import eu.emufii.app.R
import eu.emufii.app.settings.AppTheme
import eu.emufii.app.ui.controlRing
import eu.emufii.app.ui.theme.AccentCuts
import eu.emufii.app.ui.theme.ArtworkShape
import eu.emufii.app.ui.theme.Coral
import eu.emufii.app.ui.theme.EdgeDark
import eu.emufii.app.ui.theme.EdgeLight
import eu.emufii.app.ui.theme.InsetShape
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.PlateDark
import eu.emufii.app.ui.theme.PlateLight
import eu.emufii.app.ui.theme.PlateOled
import eu.emufii.app.ui.theme.ShellDark
import eu.emufii.app.ui.theme.ShellDarkLow
import eu.emufii.app.ui.theme.ShellLight
import eu.emufii.app.ui.theme.ShellLightLow
import eu.emufii.app.ui.theme.ShellOled
import eu.emufii.app.ui.theme.TealCuts
import eu.emufii.app.ui.tap

/**
 * The app's look: four trays to compare. It replaced a folding row that stacked nine
 * named lines, four brightnesses then five colours, in a card the rest of the settings
 * had to scroll past. Beyond the length, a list of names asks you to imagine.
 * pourquoi : docs/decisions/reglages-ecran.md § One hub and seven pages, plus an accordion
 */
@Composable
fun ThemeSwatches(
    theme: AppTheme,
    onTheme: (AppTheme) -> Unit,
    modifier: Modifier = Modifier,
    /** True when the first tray is the page's first control. */
    firstIsEntry: Boolean = false
) {
    // The play axis, hardcoded: there is no configurable accent any more.
    val cuts = TealCuts
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppTheme.entries.forEachIndexed { index, option ->
            ThemeSwatch(
                theme = option,
                accent = cuts,
                selected = option == theme,
                onClick = { onTheme(option) },
                entry = firstIsEntry && index == 0,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * The theme's names, at two lengths, and the accent's.
 *
 * They live here rather than on the settings screen because the panel is now
 * where they are read; the settings row borrows them back for its value.
 */
@Composable
private fun ThemeSwatch(
    theme: AppTheme,
    accent: AccentCuts,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    entry: Boolean = false
) {
    val panelDark = LocalEmufiiDarkTheme.current
    /**
     * The choice carries no ring: the ring belongs to the cursor and keeps one weight
     * everywhere. A choice is said with a mark, which an outline cannot imitate.
     * pourquoi : docs/decisions/navigation-manette.md § The ring keeps the same weight everywhere
     */
    // Read here as well as in [controlRing], which keeps it to itself: the thumbnail
    // has to pass in front of its neighbours before the band is drawn, and only layout
    // can do that.
    var ringed by remember { mutableStateOf(false) }
    val mark by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        label = "theme-swatch-mark"
    )
    Column(
        // Above its neighbours under the cursor. The cursor's band spills past the
        // thumbnail, and a `Row` draws its children in order, so the right thumbnail
        // passed over the left one's band and cut it clean off.
        modifier = modifier.zIndex(if (ringed) 1f else 0f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.3f)
                .then(if (entry) Modifier.padEntry() else Modifier)
                .onFocusEvent { ringed = it.hasFocus }
                // A thinner band than elsewhere: the thumbnails are 10 dp apart and
                // carry their name just below, so the default share bit into both.
                .controlRing(
                    InsetShape,
                    width = 3.dp,
                    glowRadius = 18.dp,
                    bandFraction = 0.042f
                )
                .clip(InsetShape)
                .tap(onClick = onClick)
        ) {
            when (theme) {
                AppTheme.SYSTEM -> {
                    // One plate on each side rather than a full pair per half.
                    //
                    // Drawing both halves complete gave four plates in a swatch
                    // the size of a thumbnail, which read as two thumbnails
                    // pushed together instead of as one tray lit from two
                    // sides. Split like this, the pair spans the seam and the
                    // swatch says the one thing it is for: the same screen,
                    // either way round.
                    //
                    // `fillMaxHeight` on each half is not decoration: a weight
                    // only settles the width, so the halves fell back to
                    // wrapping their content and the swatch became a 16 dp
                    // strip pinned to its top-left corner.
                    Row(Modifier.fillMaxSize()) {
                        TrayHalf(
                            AppTheme.LIGHT, accent, Plates.CURSOR,
                            Modifier.weight(1f).fillMaxHeight()
                        )
                        TrayHalf(
                            AppTheme.DARK, accent, Plates.PLAIN,
                            Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
                else -> TrayHalf(theme, accent, Plates.BOTH, Modifier.fillMaxSize())
            }
            // Laid over the whole swatch, after the halves, so the split theme
            // gets one outline instead of two touching rectangles.
            Box(
                Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.dp,
                        // The resting outline follows the panel it sits on, not
                        // the theme it depicts. `EdgeLight` is a dark hairline:
                        // on the dark panel the unselected swatches lost their
                        // bounds entirely and the dark one became a hole.
                        color = if (panelDark) EdgeDark else EdgeLight,
                        shape = InsetShape
                    )
            )
            // The token, in the corner nothing else uses. It grows as it settles:
            // choosing a theme repaints the whole panel behind it, and a mark appearing
            // at once while the surfaces cross would read as a second event.
            if (mark > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .scale(mark)
                        .size(20.dp)
                        .shadow(3.dp, CircleShape)
                        .clip(CircleShape)
                        .background(accent.bright),
                    contentAlignment = Alignment.Center
                ) {
                    CheckIcon(size = 13.dp, color = accent.ink)
                }
            }
        }
        Text(
            stringResource(theme.labelShortRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            // 12 dp, not 6: the cursor's band runs below the thumbnail and the name
            // read through it.
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

/** Which of the two plates a half draws; see [AppTheme.SYSTEM] above. */
private enum class Plates { BOTH, CURSOR, PLAIN }

/** One theme's tray, its plates and its cursor, at swatch size. */
@Composable
private fun TrayHalf(
    theme: AppTheme,
    accent: AccentCuts,
    plates: Plates,
    modifier: Modifier = Modifier
) {
    val shell = when (theme) {
        AppTheme.OLED -> listOf(ShellOled, ShellOled)
        AppTheme.DARK -> listOf(ShellDark, ShellDarkLow)
        else -> listOf(ShellLight, ShellLightLow)
    }
    val plate = when (theme) {
        AppTheme.OLED -> PlateOled
        AppTheme.DARK -> PlateDark
        else -> PlateLight
    }
    val dark = theme == AppTheme.DARK || theme == AppTheme.OLED
    Box(
        modifier = modifier.background(Brush.verticalGradient(shell)),
        contentAlignment = Alignment.Center
    ) {
        // Two plates and a cursor on one of them: the smallest arrangement that
        // still reads as this app rather than as a colour sample.
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (plates != Plates.PLAIN) {
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(ArtworkShape)
                        .background(plate)
                        .border(1.dp, accent.bright, ArtworkShape)
                )
            }
            if (plates != Plates.CURSOR) {
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(ArtworkShape)
                        .background(plate)
                        .border(1.dp, if (dark) EdgeDark else EdgeLight, ArtworkShape)
                )
            }
        }
    }
}

/**
 * One accent, as the colour itself.
 *
 * The chosen one is marked from the inside, with a dot in its own ink cut: an
 * outer ring would speak the cursor's language, and on a row where the cursor is
 * also present that gives two rings meaning two different things.
 */

internal val AppTheme.labelRes: Int
    get() = when (this) {
        AppTheme.SYSTEM -> R.string.settings_theme_system
        AppTheme.LIGHT -> R.string.settings_theme_light
        AppTheme.DARK -> R.string.settings_theme_dark
        AppTheme.OLED -> R.string.settings_theme_oled
    }

/** For the swatches, where "OLED (true black)" is three lines wide. */
internal val AppTheme.labelShortRes: Int
    get() = when (this) {
        AppTheme.SYSTEM -> R.string.settings_theme_short_system
        AppTheme.LIGHT -> R.string.settings_theme_short_light
        AppTheme.DARK -> R.string.settings_theme_short_dark
        AppTheme.OLED -> R.string.settings_theme_short_oled
    }

