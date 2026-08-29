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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import eu.emufii.app.R
import eu.emufii.app.library.Console
import eu.emufii.app.library.EmulatorInfo
import eu.emufii.app.library.allEmulators
import eu.emufii.app.ui.controlRing
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.LocalEmufiiOledTheme
import eu.emufii.app.ui.theme.plate
import eu.emufii.app.ui.theme.socket
import eu.emufii.app.ui.theme.TileShape

/**
 * The consoles and the emulators that play them, on one screen, as tiles.
 *
 * La tuile porte l'icone et la version **et** est le controle : « qui joue a ca »
 * et « est-ce que j'en veux » tiennent en un coup d'oeil et une pression.
 * pourquoi : docs/decisions/reglages-ecran.md § Une console porte une rangée, pas une tuile
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
    firstTileIsEntry: Boolean = false,
    /**
     * Vrai pour la version courte : sans numero de version, une icone plus
     * petite, une tuile plus basse — de quoi tenir sept consoles sur une ligne.
     * pourquoi : docs/decisions/reglages-ecran.md § La tuile de console a une version courte
     */
    compact: Boolean = false
) {
    val context = LocalContext.current
    // Read once: a row costs a package query and an icon rasterisation, and the
    // answer cannot change without the player leaving to install something,
    // which recreates this anyway.
    val emulators = remember { allEmulators(context) }

    // Le compte se mesure sur la largeur **reellement donnee** a la grille, et
    // jamais sur celle de l'ecran : la page des reglages est 90 dp plus etroite,
    // et « GameCube » y sortait en « GameCu ».
    // pourquoi : docs/decisions/reglages-ecran.md § Combien de tuiles par ligne, et la largeur qui le décide
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val minTile = if (compact) MIN_TILE_COMPACT else MIN_TILE
        val fits = ((maxWidth + GRID_GAP) / (minTile + GRID_GAP))
            .toInt()
            .coerceIn(3, emulators.size)
        val columns = balancedColumns(emulators.size, fits)

        // Le rang qui porte le curseur passe devant les autres : le `zIndex` de
        // `controlRing` ne classe qu'entre freres, donc pas entre rangs.
        // pourquoi : docs/decisions/navigation-manette.md § Le contrôle visé passe devant ses voisins
                var focusedRow by remember { mutableStateOf(-1) }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(GRID_GAP)
        ) {
            emulators.chunked(columns).forEachIndexed { rowIndex, row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(GRID_GAP),
                    modifier = Modifier.zIndex(if (rowIndex == focusedRow) 1f else 0f)
                ) {
                    row.forEachIndexed { index, info ->
                        ConsoleTile(
                            info = info,
                            visible = info.console !in hidden,
                            onToggle = { onSetVisible(info.console, info.console in hidden) },
                            entry = firstTileIsEntry && rowIndex == 0 && index == 0,
                            compact = compact,
                            onFocused = { if (it) focusedRow = rowIndex },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // **Le dernier rang ne se complete pas** : une case dessinee au
                    // bout d'une grille de consoles se lit comme une console. La place
                    // est tenue, pas peinte.
                    // pourquoi : docs/decisions/reglages-ecran.md § La grille des consoles n'a pas le droit à une orpheline
                    repeat(columns - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Combien de colonnes, une fois qu'on sait combien tiennent : celui qui remplit
 * le mieux le dernier rang, jamais le maximum — qui laisse une orpheline.
 * pourquoi : docs/decisions/reglages-ecran.md § La grille des consoles n'a pas le droit à une orpheline
 * pourquoi : docs/decisions/reglages-ecran.md § Combien de tuiles par ligne, et la largeur qui le décide
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
    entry: Boolean = false,
    compact: Boolean = false,
    /** Dit au rang qu'il porte le curseur, pour qu'il passe devant les autres. */
    onFocused: (Boolean) -> Unit = {}
) {
    // Off is dimmed, not greyed out and not removed: the tile still has to say
    // which console it is, because turning one back on is the other half of the
    // gesture and a blank square gives nothing to aim at.
    val alpha = if (visible) 1f else 0.45f

    /**
     * L'icone **perd sa couleur** quand la console est eteinte : desaturee et non
     * voilee, sinon la chose la plus voyante de la tuile disait « allumee »
     * pendant que le reste disait le contraire.
     * pourquoi : docs/decisions/reglages-ecran.md § Une console éteinte est un trou dans le plateau
     */
    val iconFilter = remember(visible) {
        if (visible) null
        else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    }
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current

    Column(
        modifier = modifier
            .height(if (compact) TILE_HEIGHT_COMPACT else TILE_HEIGHT)
            .onFocusEvent { onFocused(it.hasFocus) }
            // Avant le `clickable` : un `focusRequester` pose apres ne vise
            // plus le noeud de focus que le clickable vient de creer, et la
            // demande echoue en silence.
            .then(if (entry) Modifier.padEntry() else Modifier)
            // L'anneau avant le clip, toujours. Apres, sa lueur est tranchee a
            // la forme de la tuile et la remplit d'un lavis a bord dur au lieu
            // de deborder.
            .controlRing(TILE_SHAPE)
            // **Allumee, c'est une plaque ; eteinte, c'est un trou** : le plateau
            // sait deja dire « pose dessus » et « creuse dedans ».
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
                .size(if (compact) 32.dp else 40.dp)
                .alpha(alpha)
                .clip(RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (info.icon != null) {
                Image(
                    bitmap = info.icon,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = iconFilter,
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
            // Trois colonnes etroites et des noms qu'on ne choisit pas : c'est
            // l'endroit de l'app ou la coupe brute par defaut mordait le plus.
            overflow = TextOverflow.Ellipsis,
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
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (!compact) {
            Text(
                // La version seule, jamais « Installe, version x » : la phrase ne
                // tient pas sur une tuile, et le numero est la partie qui se lit.
                info.version?.let { stringResource(R.string.emulators_version_short, shortVersion(it)) }
                    ?: if (info.installed) stringResource(R.string.emulators_installed_unknown)
                    else stringResource(R.string.emulators_absent_short),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * La hauteur d'une tuile, **fixe** : les alveoles du dernier rang doivent faire
 * la meme, et une hauteur intrinseque ne se partage pas entre freres.
 * pourquoi : docs/decisions/reglages-ecran.md § Combien de tuiles par ligne, et la largeur qui le décide
 */
private val TILE_HEIGHT = 124.dp

/** La hauteur de la version courte, reglee pour que sept tuiles tiennent sur une ligne. */
private val TILE_HEIGHT_COMPACT = 92.dp

/** La largeur minimale d'une tuile courte : « GameCube » y passe encore. */
private val MIN_TILE_COMPACT = 92.dp

/**
 * The tile's corner, matching the library's own: the theme's one squircle
 * radius, not a private copy of it.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § FORMES
 */
private val TILE_SHAPE = TileShape

/**
 * The version as it fits on a tile : coupee a l'affichage et non a la source,
 * parce que PPSSPP porte deja son `v` et que les cinq autres non.
 * pourquoi : docs/decisions/reglages-ecran.md § La version d'un émulateur se coupe à l'affichage, pas à la source
 */
private fun shortVersion(version: String): String =
    version.removePrefix("v").removePrefix("V")
