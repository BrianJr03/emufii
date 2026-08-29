package eu.emufii.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import eu.emufii.app.ui.CONFIRM_KEYS
import eu.emufii.app.ui.RING_IN_MS
import eu.emufii.app.ui.Sfx
import eu.emufii.app.ui.focusRing
import eu.emufii.app.ui.tap
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.LocalEmufiiOledTheme
import eu.emufii.app.ui.theme.edgeColor
import eu.emufii.app.ui.theme.plateColors

/**
 * La saisie d'un code de session : **une etagere de touches, pas une dalle
 * gravee**.
 *
 * Reecrit le 2026-08-29. Le clavier etait le dernier morceau du monde « HOME
 * MENU » : une piece de plastique unique ou les cases etaient *creusees* au
 * burin, chacune allumee par un remplissage translucide dans son propre creux.
 * DUOTONE SHELVES a retire exactement ces trois choses de l'app — la gravure,
 * le relief inverse, le remplissage-comme-etat — et le clavier les gardait
 * toutes les trois. Il ne ressemblait donc a rien d'autre de l'ecran qui le
 * porte, ce qui est la definition du morceau qui detonne.
 *
 * Ce qu'il est maintenant : **un clavier de console**, sur la reference que
 * l'utilisateur a nommee — celui de la 3DS et de la Switch. Des touches plates,
 * serrees, toutes pareilles, posees a meme le fond. Pas d'ombre, pas de
 * moulage, pas de plateau dessous.
 *
 * La version intermediaire faisait de chaque touche une tuile de bibliotheque
 * en petit, avec ombre portee et lip eclaire. C'etait fidele au systeme et
 * mauvais a l'oeil : quarante objets qui flottent chacun sur leur ombre, ca
 * n'est pas un clavier, c'est une vitrine. **Un clavier est un fond et des
 * lettres**, et tout le relief qu'il a le droit d'avoir est celui du curseur —
 * qui, lui, reste l'anneau de l'app. C'est le seul element que l'utilisateur
 * ait valide des le premier essai.
 *
 * **C'est le seul clavier que l'app dessine encore.** Celui de la recherche est
 * reparti au systeme le meme jour : corriger un titre de jeu veut dire la
 * disposition, les langues et la correction du joueur, qu'on ne refera pas. Un
 * code, lui, se lit sur un ecran et se recopie caractere par caractere — le
 * clavier de l'app y gagne ce que l'IME ne sait pas faire : des cibles larges
 * atteignables a la manette, et aucune suggestion qui s'en mele.
 * pourquoi : docs/decisions/coquille-ecrans.md § Le clavier de code n'est pas le clavier de recherche
 * pourquoi : docs/decisions/theme-duotone-shelves.md § Les creux deviennent des encoches
 */
@Composable
fun EmufiiCodeKeyboard(
    onKey: (Char) -> Unit,
    maxHeight: Dp,
    modifier: Modifier = Modifier,
    /** Ou le curseur entre dans le clavier : porte par la premiere touche. */
    firstKeyFocus: FocusRequester? = null
) {
    val cursor = remember { SlabCursor(CODE_ROWS) }
    var holds by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    /**
     * **Une touche est un peu plus large que haute, jamais l'inverse.**
     *
     * La hauteur se deduisait de la place offerte, divisee en quatre : sur un
     * panneau etroit, dix colonnes donnaient des touches deux fois plus hautes
     * que larges — des barres verticales, ce qu'aucun clavier n'est. La largeur
     * d'une colonne commande donc la hauteur, et la place disponible ne fait
     * plus que la plafonner. Ce qui reste en hauteur devient du vide, et le
     * clavier se centre dedans.
     */
    val keyWidth = (maxWidth - KEY_GAP * (CODE_COLUMNS - 1)) / CODE_COLUMNS
    val roomPerRow = (maxHeight - KEY_GAP * (CODE_ROWS.size - 1)) / CODE_ROWS.size
    val keyHeight = minOf(keyWidth * KEY_ASPECT, roomPerRow)

    Column(
        verticalArrangement = Arrangement.spacedBy(KEY_GAP),
        modifier = Modifier
            // La largeur exacte de dix colonnes : le clavier ne s'etire pas
            // pour remplir son plateau, il garde ses proportions et se centre.
            .width(keyWidth * CODE_COLUMNS + KEY_GAP * (CODE_COLUMNS - 1))
            // Un seul noeud focalisable, un curseur tenu ici : les touches ne
            // decident pas si elles sont visees, on le leur dit.
            // pourquoi : CLAUDE.md § Navigation à la manette : la grille tient son propre curseur
            .slabKeys(CODE_ROWS, cursor) { label -> onKey(label.first()) }
            .then(if (firstKeyFocus != null) Modifier.focusRequester(firstKeyFocus) else Modifier)
            .onFocusChanged { holds = it.isFocused }
            .focusable()
    ) {
        CODE_ROWS.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(KEY_GAP),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Une rangee plus courte se centre par deux marges de largeur
                // entiere : les touches gardent alors la largeur d'une colonne
                // et restent en face de celles du dessus. Sans ca, six touches
                // se partageraient toute la rangee et le clavier perdrait son
                // alignement — c'est ce qui le faisait lire de travers.
                val margin = (CODE_COLUMNS - row.size) / 2f
                if (margin > 0f) Spacer(Modifier.weight(margin).height(keyHeight))
                row.forEachIndexed { keyIndex, label ->
                    Key(
                        label = label,
                        selected = holds && cursor.row == rowIndex && cursor.col == keyIndex,
                        onClick = { onKey(label.first()) },
                        height = keyHeight
                    )
                }
                if (margin > 0f) Spacer(Modifier.weight(margin).height(keyHeight))
            }
        }
    }
    }
}

/**
 * La hauteur d'une touche, en parts de sa largeur. Les claviers physiques et
 * ceux d'Android sont tous un peu plus larges que hauts ; a 1 on obtient un
 * damier, au-dela une colonne de barres.
 */
private const val KEY_ASPECT = 0.86f

/**
 * L'alphabet puis les chiffres, dans l'ordre ou on les recite — pas en AZERTY :
 * on lit un code, on ne le tape pas de memoire musculaire.
 *
 * **Dix colonnes partout, et rien d'autre que des caracteres du code.** Le
 * clavier tenait trois rangees de neuf lettres puis une de dix chiffres, avec un
 * effacement plus large glisse en bout de rangee : aucune colonne ne tombait en
 * face d'une autre. L'effacement est parti avec — c'est **B** qui efface, une
 * lettre par appui, comme il defait partout ailleurs dans l'app. Une touche pour
 * ca ne faisait que reprendre a la manette ce qu'elle avait deja, et tordre la
 * grille pour l'y loger.
 * pourquoi : docs/decisions/coquille-ecrans.md § Le clavier de code n'est pas le clavier de recherche
 */
private val CODE_ROWS = listOf(
    listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J"),
    listOf("K", "L", "M", "N", "O", "P", "Q", "R", "S", "T"),
    listOf("U", "V", "W", "X", "Y", "Z"),
    listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
)

/** La largeur du clavier, en touches. Toutes les rangees s'y alignent. */
private const val CODE_COLUMNS = 10

/**
 * L'ecart entre deux touches. Serre : sur une console, un clavier est un pave
 * qu'on lit d'un coup, pas une collection d'objets. A 6 dp les touches se
 * detachaient une a une et le bloc perdait sa forme.
 */
private val KEY_GAP = 3.dp

/**
 * Le coin d'une touche. Plus petit que l'inset de l'app (14 dp) : a l'echelle
 * d'une touche, ce rayon-la mange la moitie du bord et fait des galets.
 */
private val KEY_CORNER = 8.dp

/**
 * Une touche : une tuile de bibliotheque en petit.
 *
 * Trois etats, dans le vocabulaire de partout ailleurs — le curseur l'agrandit
 * et pose l'anneau de l'axe en vigueur (corail ici, on est dans le domaine
 * social) ; l'appui l'enfonce et retourne son moulage. Rien n'est peint *dans*
 * la touche pour dire un etat : l'etat deplace la touche.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § MATIÈRE
 */
@Composable
private fun RowScope.Key(
    label: String,
    onClick: () -> Unit,
    height: Dp,
    /** Vrai quand le clavier designe cette touche. Elle ne le decide pas. */
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = remember { RoundedCornerShape(KEY_CORNER) }

    // Sur l'horloge de l'anneau, et repartant sans fondu comme lui : deux
    // touches allumees a la fois, c'est deux endroits ou l'on croit etre.
    // pourquoi : docs/decisions/navigation-manette.md § Le curseur ne s'attarde jamais
    val mark by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(if (selected) RING_IN_MS else 0),
        label = "key-mark"
    )
    // Le seul relief du clavier, et il appartient au curseur. Moins que les 7 %
    // d'une tuile : les touches sont voisines de trois points de dp, et au-dela
    // la touche visee mord sur celles d'a cote au lieu de passer devant elles.
    val lift by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "key-lift"
    )

    // La face : la couleur haute de la plaque, a plat. Pas de degrade vertical
    // ici — a 40 dp de haut ses trois points de luminance ne se voient pas, et
    // les calculer quarante fois par recomposition ne rend rien.
    val face = plateColors(dark, oled).first()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .weight(1f)
            .height(height)
            // La touche visee passe devant ses voisines, anneau compris.
            .zIndex(if (selected) 1f else 0f)
            .graphicsLayer {
                scaleX = lift
                scaleY = lift
            }
            .focusRing(
                focused = selected,
                shape = shape,
                // Un petit controle prend un anneau reduit : le poids des tuiles,
                // sur une touche de la taille d'un pouce, remplirait l'ecart qui
                // separe deux touches.
                // pourquoi : docs/decisions/navigation-manette.md § L'anneau garde le même poids partout
                width = 3.dp,
                glowRadius = 16.dp
            )
            .clip(shape)
            .background(face)
            // L'appui assombrit la face, il ne l'enfonce pas : une touche de
            // clavier n'a pas de course visible, et le scale de la tuile ferait
            // sautiller la lettre a chaque caractere d'un code de six.
            .then(
                if (pressed) Modifier.background(PressInk.copy(alpha = if (dark) 0.24f else 0.10f))
                else Modifier
            )
            .border(1.dp, edgeColor(dark, oled), shape)
            // **Cliquable au doigt, jamais focalisable** : `clickable` rend
            // focalisable par defaut, ce qui doublerait le curseur du clavier.
            .tap(interactionSource = interaction, indication = null, onClick = onClick)
            .focusProperties { canFocus = false }
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            // La touche visee passe en Black : le monde tient sur deux graisses,
            // et c'est la marque qui reste lisible sous le pouce, la ou l'anneau
            // sort du champ de vision central.
            fontWeight = if (mark > 0f) FontWeight.Black else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** L'encre de l'appui : le noir chaud des ombres, jamais un gris bleute. */
private val PressInk = Color(0xFF241610)

/**
 * Le clavier tient son propre curseur : un seul noeud focalisable, un index
 * (rangee, colonne) qu'il calcule, et des touches a qui on le dit. Les bords ne
 * consomment pas la touche, l'ecran hote en fait ce qu'il veut.
 * pourquoi : docs/decisions/coquille-ecrans.md § La dalle tient son propre curseur
 * pourquoi : CLAUDE.md § Navigation à la manette : la grille tient son propre curseur
 */
private class SlabCursor(rows: List<List<String>>) {
    var row by mutableIntStateOf(0)
    var col by mutableIntStateOf(0)

    /** Rend vrai quand le mouvement a ete absorbe ; faux quand il sort. */
    fun move(rows: List<List<String>>, dx: Int, dy: Int): Boolean {
        if (dy != 0) {
            val next = row + dy
            if (next !in rows.indices) return false
            // La colonne se conserve en proportion : les rangees n'ont pas
            // toutes le meme nombre de touches, et garder l'index brut faisait
            // sauter le curseur d'un bout a l'autre en passant sur la rangee
            // courte.
            val ratio = (col + 0.5f) / rows[row].size
            row = next
            col = (ratio * rows[next].size).toInt().coerceIn(0, rows[next].lastIndex)
            return true
        }
        val next = col + dx
        if (next !in rows[row].indices) return false
        col = next
        return true
    }
}

/**
 * Les touches du clavier : quatre directions et la confirmation.
 *
 * `onPreviewKeyEvent`, parce que le clavier est le seul noeud focalisable. Ce
 * qui sort par un bord n'est pas consomme : l'ecran hote en decide.
 * pourquoi : docs/decisions/coquille-ecrans.md § La dalle tient son propre curseur
 */
private fun Modifier.slabKeys(
    rows: List<List<String>>,
    cursor: SlabCursor,
    onPress: (String) -> Unit
): Modifier = onPreviewKeyEvent { event ->
    if (event.type == KeyEventType.KeyUp && event.key in CONFIRM_KEYS) {
        rows.getOrNull(cursor.row)?.getOrNull(cursor.col)?.let { Sfx.click(); onPress(it) }
        return@onPreviewKeyEvent true
    }
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    when (event.key) {
        Key.DirectionLeft -> cursor.move(rows, -1, 0)
        Key.DirectionRight -> cursor.move(rows, 1, 0)
        Key.DirectionUp -> cursor.move(rows, 0, -1)
        Key.DirectionDown -> cursor.move(rows, 0, 1)
        // Avale l'appui dont on traitera le relachement, sinon la plateforme le
        // relaie et une pression se lit deux fois.
        in CONFIRM_KEYS -> true
        else -> false
    }
}
