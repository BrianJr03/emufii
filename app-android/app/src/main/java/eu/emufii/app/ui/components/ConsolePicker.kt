package eu.emufii.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import eu.emufii.app.R
import eu.emufii.app.library.Console
import eu.emufii.app.library.EmulatorInfo
import eu.emufii.app.library.allEmulators
import eu.emufii.app.ui.controlRing
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.LocalEmufiiOledTheme
import eu.emufii.app.ui.theme.plate
import eu.emufii.app.ui.theme.socket

/**
 * The consoles and the emulators that play them, on one screen, as tiles.
 *
 * This was two onboarding pages until 2026-08-19: an inventory to read, then a
 * list of switches. They asked the same question twice. The tile carries the
 * emulator's icon and version *and* is the control, so "what plays this" and
 * "do I want it" are one glance and one press.
 *
 * A grid rather than rows because it has to fit without scrolling, and it is the
 * only shape that does: seven rows need more height than this screen has, seven
 * tiles need two lines of it. It is also the shape the library already uses, so
 * the page reads as a preview of what the player is about to get.
 */
@Composable
fun ConsoleGrid(
    hidden: Set<Console>,
    onSetVisible: (Console, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Vrai quand cette grille est le premier controle de sa page : la premiere
     * tuile devient la destination nommee de la manette. Le nommage doit se
     * poser sur un controle reellement focalisable, jamais sur un conteneur.
     * pourquoi : docs/decisions/coquille-ecrans.md § L'en-tête est déclaré avant le contenu, et dessiné par-dessus
     */
    firstTileIsEntry: Boolean = false
) {
    val context = LocalContext.current
    // Read once: a row costs a package query and an icon rasterisation, and the
    // answer cannot change without the player leaving to install something,
    // which recreates this anyway.
    val emulators = remember { allEmulators(context) }

    // How many fit on a line, from the width this grid is actually given.
    //
    // Measured here rather than taken from the screen. The onboarding hands it
    // the full width and gets all seven on the Thor, which is the layout the
    // page was drawn for: one line, nothing to scroll, the whole answer at once.
    // The settings panel is the same screen but some 90 dp narrower once the
    // card and its padding are paid for, and a screen-wide count put seven tiles
    // in that space too: "GameCube" came out as "GameCu" and a version as
    // "v2126.0-va". A tile that has to abbreviate its own console has stopped
    // doing its job.
    //
    // [MIN_TILE] is the width at which a tile still holds the longest console
    // name and the longest version on one line each. Below three columns the
    // grid stops being a grid, so that is the floor.
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val fits = ((maxWidth + GRID_GAP) / (MIN_TILE + GRID_GAP))
            .toInt()
            .coerceIn(3, emulators.size)
        val columns = balancedColumns(emulators.size, fits)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(GRID_GAP)
        ) {
            emulators.chunked(columns).forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(GRID_GAP)) {
                    row.forEachIndexed { index, info ->
                        ConsoleTile(
                            info = info,
                            visible = info.console !in hidden,
                            onToggle = { onSetVisible(info.console, info.console in hidden) },
                            entry = firstTileIsEntry && rowIndex == 0 && index == 0,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Le dernier rang se complete en alveoles vides : un plateau
                    // reste rectangulaire, et des tuiles qui s'arretent au
                    // milieu d'une ligne se lisent comme une grille
                    // interrompue.
                    // pourquoi : docs/decisions/reglages-ecran.md § La grille des consoles n'a pas le droit à une orpheline
                    repeat(columns - row.size) {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(TILE_HEIGHT)
                                .socket(TILE_SHAPE, LocalEmufiiDarkTheme.current)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Combien de colonnes, une fois qu'on sait combien tiennent.
 *
 * Prendre le maximum qui tient etait le reflexe, et il donne le pire resultat
 * du lot : a sept consoles dans une carte qui en porte six, ca faisait six
 * tuiles puis **une seule** sur la ligne suivante. Une orpheline se lit comme
 * un oubli, pas comme une grille.
 *
 * On choisit donc le nombre qui remplit le mieux le dernier rang, du plus large
 * au plus etroit — sept dans six colonnes devient quatre plus trois. Quand tout
 * tient sur une ligne, ca reste une ligne : c'est la mise en page pour laquelle
 * la page d'accueil a ete dessinee.
 * pourquoi : docs/decisions/reglages-ecran.md § La grille des consoles n'a pas le droit à une orpheline
 */
internal fun balancedColumns(count: Int, fits: Int): Int {
    if (count <= fits) return count
    var best = fits
    var bestGap = Int.MAX_VALUE
    for (c in fits downTo 3) {
        val gap = (c - count % c) % c
        if (gap < bestGap) {
            best = c
            bestGap = gap
        }
    }
    return best
}

/** The narrowest a tile can be and still spell out its console and version. */
private val MIN_TILE = 118.dp

private val GRID_GAP = 8.dp

@Composable
private fun ConsoleTile(
    info: EmulatorInfo,
    visible: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    entry: Boolean = false
) {
    // Off is dimmed, not greyed out and not removed: the tile still has to say
    // which console it is, because turning one back on is the other half of the
    // gesture and a blank square gives nothing to aim at.
    val alpha = if (visible) 1f else 0.38f
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current

    Column(
        modifier = modifier
            .height(TILE_HEIGHT)
            // Avant le `clickable` : un `focusRequester` pose apres ne vise
            // plus le noeud de focus que le clickable vient de creer, et la
            // demande echoue en silence.
            .then(if (entry) Modifier.padEntry() else Modifier)
            // L'anneau avant le clip, toujours. Apres, sa lueur est tranchee a
            // la forme de la tuile et la remplit d'un lavis a bord dur au lieu
            // de deborder.
            .controlRing(TILE_SHAPE)
            // **Allumee, c'est une plaque ; eteinte, c'est un trou.**
            //
            // C'etait une plaque dans les deux cas, avec une barrette d'accent
            // dessous pour dire laquelle etait laquelle : deux tuiles voisines
            // se ressemblaient a 4 dp pres, et sur les themes sombres la
            // barrette etait la seule chose a lire. Le plateau sait deja dire
            // « pose dessus » et « creuse dedans », et c'est exactement la
            // distinction que cette page fait.
            // pourquoi : docs/decisions/reglages-ecran.md § Une console éteinte est un trou dans le plateau
            .then(
                if (visible) Modifier.plate(shape = TILE_SHAPE, dark = dark, oled = oled, lift = 5.dp)
                else Modifier.socket(TILE_SHAPE, dark)
            )
            .clickable { onToggle() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .alpha(alpha)
                .clip(RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (info.icon != null) {
                Image(
                    bitmap = info.icon,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // L'abreviation de la console plutot qu'un point
                // d'interrogation : un emulateur absent est le cas ordinaire
                // sur un appareil neuf, et la tuile doit quand meme nommer sa
                // machine.
                Text(
                    info.console.shortLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            info.console.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Text(
            // Le nom de l'emulateur, sur sa propre ligne, jamais traduit : c'est
            // un produit. Une tuile qui ne disait que « Switch » laissait la
            // page incapable de repondre a la question qu'elle pose, qui est
            // quoi installer.
            info.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.85f),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Text(
            // La version seule, jamais « Installe, version x » : la phrase ne
            // tient pas sur une tuile, et le numero est la partie qui se lit.
            info.version?.let { stringResource(R.string.emulators_version_short, shortVersion(it)) }
                ?: if (info.installed) stringResource(R.string.emulators_installed_unknown)
                else stringResource(R.string.emulators_absent_short),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * La hauteur d'une tuile, fixe.
 *
 * Fixe parce que les alveoles du dernier rang doivent faire la meme, et qu'une
 * hauteur intrinseque ne se partage pas entre freres sans mesurer. Le contenu
 * est de toute facon uniforme : une icone et trois lignes.
 *
 * La valeur, elle, est reglee pour que les deux rangs, l'en-tete et la phrase
 * tiennent sur l'ecran de la Thor.
 */
private val TILE_HEIGHT = 124.dp

/** The tile's corner, matching the library's own. */
private val TILE_SHAPE = RoundedCornerShape(16.dp)

/**
 * The version as it fits on a tile.
 *
 * PPSSPP names its builds "v1.20.4", already carrying the letter the label adds,
 * and "vv1.20.4" is what came out on the Thor. Trimming here rather than
 * dropping the prefix from the string: the other five report a bare number, and
 * a column of versions with one of them unmarked reads worse than either.
 */
private fun shortVersion(version: String): String =
    version.removePrefix("v").removePrefix("V")
