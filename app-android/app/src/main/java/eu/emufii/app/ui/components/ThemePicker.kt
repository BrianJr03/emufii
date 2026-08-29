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
 * L'apparence de l'app : quatre plateaux a comparer.
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
    onTheme: (AppTheme) -> Unit,
    modifier: Modifier = Modifier,
    /** Vrai quand le premier plateau est le premier controle de la page. */
    firstIsEntry: Boolean = false
) {
    // L'axe du jeu, en dur : l'accent configurable n'existe plus.
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
     * Le choix ne porte pas d'anneau : l'anneau appartient au curseur, et garde
     * le meme poids partout. Le choix se dit par une marque, qu'un contour ne
     * peut pas imiter.
     * pourquoi : docs/decisions/navigation-manette.md § L'anneau garde le même poids partout
     */
    // Le curseur est lu ici en plus de [controlRing], qui le garde pour lui :
    // la vignette doit passer devant ses voisines *avant* que la bande soit
    // dessinée, et seule la mise en page peut le faire.
    var ringed by remember { mutableStateOf(false) }
    val mark by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        label = "theme-swatch-mark"
    )
    Column(
        // **Au-dessus de ses voisines quand le curseur est dessus.**
        //
        // La bande du curseur déborde de la vignette, et une `Row` dessine ses
        // enfants dans l'ordre : la vignette de droite passe par-dessus la
        // bande de celle de gauche, qui s'y retrouve tranchée net sur un côté.
        // Invisible avec l'ancien anneau, qui se dessinait à l'intérieur.
        //
        // Le `zIndex` va **ici**, sur l'enfant de la `Row`, et non sur la boîte
        // à l'intérieur : il ne réordonne qu'entre frères, et posé sur la boîte
        // il ne départageait que la boîte et son libellé — c'est-à-dire rien.
        modifier = modifier.zIndex(if (ringed) 1f else 0f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.3f)
                .then(if (entry) Modifier.padEntry() else Modifier)
                .onFocusEvent { ringed = it.hasFocus }
                // Une bande plus fine qu'ailleurs : les vignettes sont a 10 dp
                // l'une de l'autre et portent leur nom juste dessous, donc la
                // part par defaut la faisait mordre sur les deux.
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
            // Le jeton, dans le coin ou rien d'autre ne se pose. Il grandit en
            // s'installant : choisir un theme repeint tout le panneau derriere
            // lui, et une marque qui apparaitrait d'un coup pendant que les
            // surfaces se croisent se lirait comme un second evenement.
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
            // 12 dp et non 6 : la bande du curseur descend sous la vignette,
            // et le nom se lisait au travers.
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

