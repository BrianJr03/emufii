package eu.emufii.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.emufii.app.ui.controlRing
import eu.emufii.app.ui.ringColor
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.plate
import eu.emufii.app.ui.theme.socket
import eu.emufii.app.ui.tap

/**
 * L'interrupteur de l'app : une alvéole creusée dans la plaque, et un bouton
 * moulé qui coulisse dedans.
 *
 * Deux choses le distinguent du `Switch` de Material, et les deux sont la
 * raison de ce fichier. Material dessine une piste teintée et une pastille
 * plate, qui sur une plaque moulée se lit comme un autocollant ; ici la piste
 * est le même creux que les champs de saisie et les emplacements vides, et le
 * bouton est la même plaque que tout le reste — l'éclairage est cohérent parce
 * que c'est le même éclairage. Et Material peint un voile de focus, éteint
 * partout dans cette app, qui se lit comme « désactivé » sur une console où le
 * curseur est en permanence quelque part.
 *
 * Allumé, le creux prend l'accent : c'est le seul endroit de l'interrupteur qui
 * porte de la couleur, et il veut dire « en marche », pas « ici ». Le curseur,
 * lui, garde son anneau.
 * pourquoi : docs/decisions/reglages-ecran.md § Un réglage qui n'a que deux états est un interrupteur
 */
private val TRACK_WIDTH = 52.dp
private val TRACK_HEIGHT = 30.dp
private val KNOB = 24.dp
private val PAD = 3.dp

/**
 * Une rangée d'interrupteur : ce qu'il fait à gauche, l'interrupteur à droite.
 *
 * Toute la rangée est la cible **du doigt**, pas seulement l'interrupteur :
 * viser une pastille de 52 dp au bout d'une ligne à la manette est un travail,
 * et à deux mains sur une console c'est le mauvais geste.
 *
 * Mais le **focus** vit sur l'interrupteur seul. La rangée porte bien un
 * contrôle, seulement son contour faisait le tour de toute la ligne — un
 * cadre sur trois mots de libellé, qui se lisait comme une sélection et pas
 * comme un curseur. La rangée n'est donc pas un arrêt de focus
 * (`canFocus = false`) : au doigt elle toggle, à la manette c'est la pastille
 * qui porte l'anneau, et il n'y a toujours qu'un seul geste.
 * pourquoi : docs/decisions/reglages-ecran.md § Un réglage qui n'a que deux états est un interrupteur
 */
@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    /** La ligne sous le libellé, quand il faut dire ce que l'interrupteur coûte. */
    note: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .focusProperties { canFocus = false }
            .tap(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            note?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Le seul arrêt de focus de la rangée : l'anneau épouse l'interrupteur,
        // pas la ligne entière.
        Box(
            modifier = Modifier
                .controlRing(CircleShape)
                .tap(role = Role.Switch) { onCheckedChange(!checked) }
        ) {
            SwitchFace(checked = checked)
        }
    }
}

/**
 * L'interrupteur sans son clic ni son anneau : la rangee qui le porte les a
 * deja. Publie parce que la carte de lancement a la sienne, et qu'une app n'a
 * le droit qu'a un seul interrupteur.
 *
 * DUOTONE SHELVES : plat, comme le reste. La piste est une encoche (la teinte
 * basse de la plaque), la pastille une tuile sans relief, et l'etat actif
 * prend l'axe en force — turquoise par defaut, corail si la rangee vit dans
 * une zone sociale (`LocalRingTone`).
 * pourquoi : docs/decisions/theme-duotone-shelves.md § Les creux deviennent des encoches
 */
@Composable
fun SwitchFace(checked: Boolean) {
    val dark = LocalEmufiiDarkTheme.current
    val axis = ringColor()
    val knob by animateDpAsState(
        targetValue = if (checked) TRACK_WIDTH - KNOB - PAD else PAD,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 900f),
        label = "switch-row-knob"
    )
    val fill by animateColorAsState(
        targetValue = if (checked) axis.copy(alpha = 0.35f) else Color.Transparent,
        label = "switch-row-track"
    )
    Box(
        modifier = Modifier
            .size(width = TRACK_WIDTH, height = TRACK_HEIGHT)
            .socket(CircleShape, dark)
            .background(fill, CircleShape),
        contentAlignment = Alignment.CenterStart
    ) {
        // Le bouton est **toujours** la plaque claire, quel que soit le theme.
        // En plaque sombre sur un thème sombre, il se lisait comme un trou de
        // plus dans l'alveole au lieu du bouton qui coulisse dedans : un
        // interrupteur doit dire de quel cote il est, de loin, et c'est le nub
        // clair qui le dit.
        // pourquoi : docs/decisions/reglages-ecran.md § Un réglage qui n'a que deux états est un interrupteur
        Box(
            modifier = Modifier
                .offset(x = knob)
                .size(KNOB)
                .plate(shape = CircleShape, dark = false, oled = false, lift = 2.dp)
        )
    }
}
