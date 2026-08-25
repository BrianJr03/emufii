package eu.emufii.app.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.emufii.app.R
import eu.emufii.app.settings.AppAccent
import eu.emufii.app.settings.AppTheme
import eu.emufii.app.ui.controlRing
import eu.emufii.app.ui.theme.AccentCuts
import eu.emufii.app.ui.theme.ArtworkShape
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
import eu.emufii.app.ui.theme.accentCuts

/**
 * L'apparence de l'app : quatre plateaux a comparer, puis les accents en perles.
 *
 * Ca remplacait une rangee depliante qui empilait neuf lignes nommees — quatre
 * luminosites puis cinq couleurs — dans une carte que le reste des reglages
 * devait faire defiler. Deux choses clochaient au-dela de la longueur. Une
 * liste de noms demande d'imaginer ce que « OLED » et « Ambre » donnent, quand
 * c'est justement le sujet ; et un choix qui repeint toute l'app n'est pas le
 * detail d'une rangee. Ca a d'abord ete un panneau, puis, le jour ou les
 * reglages sont devenus des pages, c'est devenu **la page Apparence** : un
 * panneau modal qui repeint l'ecran qu'il recouvre se juge a travers son propre
 * voile.
 * pourquoi : docs/decisions/reglages-ecran.md § Un hub et sept pages, plus un accordéon
 */
@Composable
fun ThemeSwatches(
    theme: AppTheme,
    accent: AppAccent,
    onTheme: (AppTheme) -> Unit,
    modifier: Modifier = Modifier,
    /** Vrai quand le premier plateau est le premier controle de la page. */
    firstIsEntry: Boolean = false
) {
    val cuts = accentCuts(accent)
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
 * Les accents, en rangee de perles de leur propre couleur.
 *
 * Les noms vivent **sous** la rangee plutot que sous chaque perle : cinq
 * libelles en travers de 420 dp donnent trois lignes a « Couleur systeme » et la
 * rangee cesse d'etre une rangee de couleurs. Seule celle qui est choisie a
 * besoin d'etre nommee.
 */
@Composable
fun AccentBeads(
    accent: AppAccent,
    onAccent: (AppAccent) -> Unit,
    modifier: Modifier = Modifier,
    /** Combien de perles par rangee. Quatre en colonne etroite, huit en large. */
    perRow: Int = 4
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppAccent.entries.chunked(perRow).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { option ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        AccentBead(
                            cuts = accentCuts(option),
                            selected = option == accent,
                            onClick = { onAccent(option) }
                        )
                    }
                }
                // Une derniere rangee moins fournie garde ses colonnes au lieu
                // de s'etaler : la grille doit survivre a un neuvieme accent
                // ajoute un jour, et une demi-rangee qui se recentre ressemble
                // a une autre mise en page.
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Text(
            stringResource(accent.labelRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * A theme, drawn rather than named: a little tray with a plate on it and the
 * accent where the cursor would be.
 *
 * The proportions are the app's own — a dark plate on a dark tray, a white plate
 * on a silver one — so the swatch answers the only question being asked, which
 * is what the screen will look like. [AppTheme.SYSTEM] is split down the middle,
 * light on the left and dark on the right: it is the one option that is not a
 * look but a promise to follow the phone, and half of each says that without a
 * word.
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
    // The selected outline animates, because picking a theme also repaints the
    // whole panel behind it: a border that snapped while the surfaces crossfaded
    // read as two unrelated things happening.
    val outline by animateColorAsState(
        targetValue = if (selected) accent.bright else Color.Transparent,
        label = "theme-swatch-outline"
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.3f)
                .then(if (entry) Modifier.padEntry() else Modifier)
                .controlRing(InsetShape, width = 3.dp, glowRadius = 18.dp)
                .clip(InsetShape)
                .clickable(onClick = onClick)
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
                        width = if (selected) 2.dp else 1.dp,
                        // The resting outline follows the panel it sits on, not
                        // the theme it depicts. `EdgeLight` is a dark hairline:
                        // on the dark panel the unselected swatches lost their
                        // bounds entirely and the dark one became a hole.
                        color = if (selected) outline
                        else if (panelDark) EdgeDark else EdgeLight,
                        shape = InsetShape
                    )
            )
        }
        Text(
            stringResource(theme.labelShortRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp)
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
@Composable
private fun AccentBead(
    cuts: AccentCuts,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .controlRing(CircleShape, width = 3.dp, glowRadius = 18.dp)
            .clip(CircleShape)
            .background(cuts.bright)
            .border(1.dp, cuts.ink.copy(alpha = 0.5f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(cuts.ink)
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

internal val AppAccent.labelRes: Int
    get() = when (this) {
        AppAccent.SYSTEM -> R.string.settings_accent_system
        AppAccent.CYAN -> R.string.settings_accent_cyan
        AppAccent.AMBER -> R.string.settings_accent_amber
        AppAccent.VIOLET -> R.string.settings_accent_violet
        AppAccent.ROSE -> R.string.settings_accent_rose
        AppAccent.YELLOW -> R.string.settings_accent_yellow
        AppAccent.RED -> R.string.settings_accent_red
        AppAccent.WHITE -> R.string.settings_accent_white
    }
