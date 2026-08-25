package eu.emufii.app.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.emufii.app.R
import eu.emufii.app.library.Console
import eu.emufii.app.ui.components.ConsoleGrid
import eu.emufii.app.ui.components.DetailNote
import eu.emufii.app.ui.components.DetailTone

/**
 * Quelles consoles apparaissent dans la grille.
 *
 * Sa propre page et pas un bloc de la bibliotheque : c'est une grille de sept
 * tuiles, elle veut la largeur, et une grille tassee dans une demi-colonne
 * abregeait ses propres noms de console. C'est aussi la seule page qui n'a rien
 * a expliquer — les tuiles portent l'icone de l'emulateur et sa version, donc
 * elles disent en meme temps ce qui est affiche et ce qui est installe.
 */
@Composable
internal fun ConsolesPage(
    hidden: Set<Console>,
    onSetVisible: (Console, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsPage(
        title = stringResource(R.string.settings_page_consoles),
        onBack = onBack,
        modifier = modifier
    ) {
        SettingsBlock(
            // Un titre, et pas celui de la page : sans lui, la pastille d'etat
            // se retrouvait seule au bout d'une ligne vide, ou elle se lisait
            // comme un accident. Il dit ce que la page fait, la ou le titre de
            // l'ecran ne dit que de quoi elle parle.
            title = stringResource(R.string.settings_consoles_block),
            state = BlockState(
                DetailTone.GOOD,
                stringResource(
                    R.string.settings_pill_ratio,
                    Console.entries.size - hidden.size,
                    Console.entries.size
                )
            )
        ) {
            // Bornee : la phrase courait sur toute la largeur de la carte, soit
            // pres de 1700 px sur la Thor, ou l'oeil perd la ligne avant d'en
            // trouver la fin. Une explication garde la mesure d'un paragraphe
            // meme quand la carte qui la porte est large.
            // pourquoi : docs/decisions/reglages-ecran.md § La grille des consoles n'a pas le droit à une orpheline
            DetailNote(
                stringResource(R.string.consoles_pick_body),
                modifier = Modifier.widthIn(max = 560.dp)
            )
            // Une seule phrase, et elle passe **avant** la grille. La seconde
            // — « eprouve aux versions affichees ici » — disait ce que les
            // numeros sous chaque tuile disent deja, et c'est elle qui faisait
            // passer la page sous la ligne de flottaison de la Thor.
            ConsoleGrid(
                hidden = hidden,
                onSetVisible = onSetVisible,
                firstTileIsEntry = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
