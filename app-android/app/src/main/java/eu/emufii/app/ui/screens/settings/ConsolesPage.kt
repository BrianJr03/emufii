package eu.emufii.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.emufii.app.R
import eu.emufii.app.library.Console
import eu.emufii.app.library.EmulatorPick
import eu.emufii.app.library.allEmulators
import eu.emufii.app.ui.components.ConsoleRow
import eu.emufii.app.ui.components.DetailNote
import eu.emufii.app.ui.components.DetailTone

/**
 * Which consoles appear in the grid, and with which build. A console carries a row
 * rather than a tile: the build question decided the shape.
 * pourquoi : docs/decisions/reglages-ecran.md § A console carries a row, not a tile
 */
@Composable
internal fun ConsolesPage(
    hidden: Set<Console>,
    onSetVisible: (Console, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Re-read when a build choice changes: the row's icon, name and version all come
    // from the chosen package, so the row lies until it is re-read. It costs one
    // package query per console, so not on every composition.
    var pickRevision by remember { mutableIntStateOf(0) }
    val emulators = remember(pickRevision) { allEmulators(context) }

    SettingsPage(
        title = stringResource(R.string.settings_page_consoles),
        onBack = onBack,
        trailing = {
            StatePill(
                DetailTone.GOOD,
                stringResource(
                    R.string.settings_pill_ratio,
                    Console.entries.size - hidden.size,
                    Console.entries.size
                )
            )
        },
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Bounded: the sentence ran the full width, nearly 1700 px on the Thor,
            // where the eye loses the line before finding its end.
            // pourquoi : docs/decisions/reglages-ecran.md § The console grid is not allowed an orphan
            DetailNote(
                stringResource(R.string.consoles_pick_body),
                modifier = Modifier.widthIn(max = 560.dp)
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                // Two columns as soon as each holds a whole row without abbreviating a
                // build name. Below that, one: two columns of truncated rows are worth
                // less than one that reads.
                val columns = if (maxWidth >= TWO_COLUMN_ROWS) 2 else 1
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(columns) { side ->
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            emulators.forEachIndexed { index, info ->
                                // Alternating, never cut mid-console: a console owns a
                                // whole column, or its switch and its build choice end
                                // up either side of the gutter.
                                if (index % columns != side) return@forEachIndexed
                                ConsoleRow(
                                    info = info,
                                    visible = info.console !in hidden,
                                    onSetVisible = { on -> onSetVisible(info.console, on) },
                                    onPickVariant = { pkg ->
                                        EmulatorPick.choose(context, info.console, pkg)
                                        pickRevision++
                                    },
                                    entry = index == 0
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val TWO_COLUMN_ROWS = 640.dp
