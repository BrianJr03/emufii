package eu.emufii.app.ui.screens.settings

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.emufii.app.R
import eu.emufii.app.settings.AppAccent
import eu.emufii.app.settings.AppTheme
import eu.emufii.app.ui.components.AccentBeads
import eu.emufii.app.ui.components.ThemeSwatches
import eu.emufii.app.ui.components.labelRes

/**
 * L'apparence de l'app, montree plutot que nommee — et **sans defilement**.
 *
 * Les quatre plateaux et les huit perles tenaient dans un seul bloc, l'un
 * au-dessus de l'autre : 4 vignettes carrees pleine largeur, puis deux rangees
 * de perles, puis leur legende. Sur les 1080 px de la Thor, la derniere rangee
 * passait sous la ligne de flottaison — et c'est le seul ecran de l'app ou tout
 * doit se comparer d'un coup d'oeil, puisque comparer est exactement ce qu'on y
 * fait. Cote a cote, l'ensemble tient.
 * pourquoi : docs/decisions/reglages-ecran.md § Apparence se compare d'un seul coup d'oeil
 */
@Composable
internal fun AppearancePage(
    theme: AppTheme,
    accent: AppAccent,
    onTheme: (AppTheme) -> Unit,
    onAccent: (AppAccent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsPage(
        title = stringResource(R.string.settings_page_appearance),
        onBack = onBack,
        modifier = modifier
    ) {
        SettingsColumns(
            {
                SettingsBlock(
                    title = stringResource(R.string.settings_theme),
                    state = BlockState(
                        eu.emufii.app.ui.components.DetailTone.GOOD,
                        stringResource(theme.labelRes)
                    )
                ) {
                    ThemeSwatches(
                        theme = theme,
                        accent = accent,
                        onTheme = onTheme,
                        firstIsEntry = true
                    )
                }
            },
            {
                SettingsBlock(
                    title = stringResource(R.string.settings_accent),
                    state = BlockState(
                        eu.emufii.app.ui.components.DetailTone.GOOD,
                        stringResource(accent.labelRes)
                    )
                ) {
                    // Quatre perles par rangee en demi-colonne, comme avant ;
                    // les huit d'un trait quand la page est sur une colonne
                    // unique et large.
                    BoxWithConstraints {
                        AccentBeads(
                            accent = accent,
                            onAccent = onAccent,
                            perRow = if (maxWidth > 460.dp) 8 else 4
                        )
                    }
                }
            },
        )
    }
}
