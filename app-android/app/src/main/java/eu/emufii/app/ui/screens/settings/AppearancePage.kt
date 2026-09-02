package eu.emufii.app.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.emufii.app.R
import eu.emufii.app.settings.AppTheme
import eu.emufii.app.ui.components.DetailTone
import eu.emufii.app.ui.components.ThemeSwatches
import eu.emufii.app.ui.components.labelRes

/**
 * The theme, and that is all: Material You could not repaint both axes, so the accent beads
 * and the system colour went.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § Settings
 */
@Composable
internal fun AppearancePage(
    theme: AppTheme,
    onTheme: (AppTheme) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsPage(
        title = stringResource(R.string.settings_page_appearance),
        onBack = onBack,
        modifier = modifier
    ) {
        SettingsBlock(
            title = stringResource(R.string.settings_theme),
            state = BlockState(DetailTone.GOOD, stringResource(theme.labelRes))
        ) {
            ThemeSwatches(
                theme = theme,
                onTheme = onTheme,
                firstIsEntry = true
            )
        }
    }
}
