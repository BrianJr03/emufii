package eu.emufii.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.emufii.app.R
import eu.emufii.app.library.EmulatorInfo
import eu.emufii.app.library.EmulatorVariant
import eu.emufii.app.ui.controlRing
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.socket
import eu.emufii.app.ui.tap

/**
 * Une console dans les reglages : ce qui la joue, et si elle est dans la grille.
 *
 * **Une rangee et non plus une tuile.** La grille de tuiles carrees repondait
 * bien a « qu'est-ce qui joue quoi », mal a « laquelle est allumee » (sept
 * tuiles identiques, l'etat porte par un ecart d'encre) et pas du tout a « avec
 * quelle build » — une question qui ne rentre nulle part dans un carre de
 * 200 px deja plein de quatre lignes centrees.
 *
 * La rangee range les trois faits sur un axe de lecture unique : **l'icone dit
 * l'emulateur, le texte dit la console et la build, l'interrupteur dit l'etat**,
 * et il est a droite ou l'oeil va chercher un etat. Le choix de build s'ouvre
 * dessous, la ou il n'existe pas de version a choisir dans le cas ordinaire.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConsoleRow(
    info: EmulatorInfo,
    visible: Boolean,
    onSetVisible: (Boolean) -> Unit,
    onPickVariant: (String) -> Unit,
    modifier: Modifier = Modifier,
    entry: Boolean = false
) {
    val dark = LocalEmufiiDarkTheme.current
    // Eteinte : l'icone perd sa couleur. C'est le signal le plus voyant de la
    // rangee, et il disait « allumee » quand tout le reste disait l'inverse.
    val alpha = if (visible) 1f else 0.45f
    val iconFilter = remember(visible) {
        if (visible) null
        else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .socket(ROW_SHAPE, dark)
            // Comme [SwitchRow] : la ligne entiere bascule au doigt, mais elle
            // n'est pas un arret de curseur — c'est la pastille qui porte
            // l'anneau, sinon un cadre fait le tour de toute la rangee et se
            // lit comme une selection.
            .focusProperties { canFocus = false }
            .tap(role = Role.Switch) { onSetVisible(!visible) }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .alpha(alpha)
                .clip(RoundedCornerShape(10.dp)),
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
                // L'abreviation plutot qu'un point d'interrogation : un
                // emulateur absent est le cas ordinaire sur un appareil neuf,
                // et la rangee doit quand meme nommer sa machine.
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
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // **Le choix vit sur la ligne du nom, et non dessous.**
        //
        // Dessous, chaque rangee gagnait une seconde ligne : sept consoles en
        // deux colonnes passaient sous le bas de l'ecran de la Thor, pour une
        // question qui n'a le plus souvent qu'une reponse. Ici la pastille
        // unique — le cas ordinaire — ne coute aucune hauteur.
        //
        // `FlowRow` et non `Row` : deux builds installees ne tiennent pas a
        // cote du nom, et elles passent alors a la ligne d'elles-memes. La
        // rangee grandit quand le joueur a vraiment un choix a faire, jamais
        // avant — c'est le contenu qui decide de la forme, pas une condition
        // ecrite a la main.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (info.variants.isEmpty()) {
                // Rien d'installe : la place du choix dit ce qui manque. Le nom
                // vient du backend, le systeme n'ayant rien a en dire, et c'est
                // la reponse a la seule question que la rangee pose alors.
                Text(
                    absentLine(info),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                info.variants.forEach { variant ->
                    VariantChip(
                        variant = variant,
                        selected = variant.packageName == info.installedPackage,
                        enabled = visible,
                        onClick = { onPickVariant(variant.packageName) }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                // Plus epais qu'ailleurs : l'interrupteur est une petite
                // pastille au bout d'une rangee large, et la part par defaut y
                // donnait un filet qu'on cherchait des yeux.
                .controlRing(
                    androidx.compose.foundation.shape.CircleShape,
                    bandFraction = 0.165f
                )
                .then(if (entry) Modifier.padEntry() else Modifier)
                .tap(role = Role.Switch) { onSetVisible(!visible) }
        ) {
            SwitchFace(checked = visible)
        }
    }
}

/**
 * Une build, en pastille. Choisie = l'axe du systeme en force ; les autres
 * restent lisibles, parce qu'en choisir une autre est l'autre moitie du geste.
 */
@Composable
private fun VariantChip(
    variant: EmulatorVariant,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val tint =
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.45f)
            .controlRing(CHIP_SHAPE)
            .clip(CHIP_SHAPE)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
            )
            .tap(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            variant.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // **C'est le nom qui cede, jamais le numero.** Une pastille trop
            // large pour sa colonne coupait « Dolphin Emulator 2606-302 » a
            // « …2606 » : or entre deux builds du meme emulateur, le nom est
            // justement la partie identique, et le numero la seule qui
            // distingue. `fill = false` pour qu'une pastille courte ne
            // s'etire pas a toute la largeur disponible.
            modifier = Modifier.weight(1f, fill = false)
        )
        variant.version?.let {
            Text(
                shortVersion(it),
                style = MaterialTheme.typography.labelSmall,
                color = tint.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}

/**
 * Ce qui manque, nomme.
 *
 * Le nom vient du backend et non du systeme — le systeme n'a rien a en dire,
 * puisque rien n'est installe — et c'est la reponse a la seule question que
 * cette rangee pose alors : quoi installer pour jouer a cette console.
 */
@Composable
private fun absentLine(info: EmulatorInfo): String =
    "${info.name} · ${stringResource(R.string.emulators_absent_short)}"

private val ROW_SHAPE = RoundedCornerShape(16.dp)
private val CHIP_SHAPE = RoundedCornerShape(9.dp)

/**
 * La version telle qu'elle tient sur une rangee.
 *
 * PPSSPP nomme ses builds « v1.20.4 », en portant deja la lettre que l'etiquette
 * ajoute, et « vv1.20.4 » est ce qui sortait sur la Thor.
 */
private fun shortVersion(version: String): String =
    version.removePrefix("v").removePrefix("V")
