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
 * Quelles consoles apparaissent dans la grille, et **avec quelle build**.
 *
 * Une console porte une rangee et non une tuile : c'est la question de la build
 * qui a tranche la forme.
 * pourquoi : docs/decisions/reglages-ecran.md § Une console porte une rangée, pas une tuile
 */
@Composable
internal fun ConsolesPage(
    hidden: Set<Console>,
    onSetVisible: (Console, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Relu quand un choix de build change : l'icone, le nom et la version de la
    // rangee viennent tous du paquet choisi, donc la rangee ment jusqu'a ce
    // qu'on la relise. La lecture coute une requete de paquet par console, donc
    // pas a chaque composition.
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
        // Serre contre les rangees (10 dp) la ou la page respire au-dessus : la
        // phrase leur appartient, elle n'est pas une section.
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Bornee : la phrase courait sur toute la largeur, soit pres de
            // 1700 px sur la Thor, ou l'oeil perd la ligne avant d'en trouver
            // la fin.
            // pourquoi : docs/decisions/reglages-ecran.md § La grille des consoles n'a pas le droit à une orpheline
            DetailNote(
                stringResource(R.string.consoles_pick_body),
                modifier = Modifier.widthIn(max = 560.dp)
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                // Deux colonnes des que chacune tient une rangee entiere sans
                // abreger un nom de build. En dessous, une seule : deux
                // colonnes de rangees tronquees valent moins qu'une colonne
                // qui se lit.
                val columns = if (maxWidth >= TWO_COLUMN_ROWS) 2 else 1
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(columns) { side ->
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            emulators.forEachIndexed { index, info ->
                                // En alternance, jamais coupees au milieu : une
                                // console appartient a une colonne entiere,
                                // sinon son interrupteur et son choix de build
                                // finissent de part et d'autre de la gouttiere.
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

/** La largeur en dessous de laquelle les rangees restent sur une colonne. */
private val TWO_COLUMN_ROWS = 640.dp
