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
 * A console keyboard: flat keys, tight, all alike, laid straight on the ground.
 * No shadow, no moulding, no tray under them; the only relief is the cursor ring.
 *
 * The only keyboard the app still draws. Search went back to the system IME,
 * since correcting a game title means layouts, languages and autocorrect. A code
 * is copied character by character, and gains wide pad-reachable targets with no
 * suggestions in the way.
 * pourquoi : docs/decisions/coquille-ecrans.md § The code keyboard is not the search keyboard
 * pourquoi : docs/decisions/theme-duotone-shelves.md § Hollows become notches
 */
@Composable
fun EmufiiCodeKeyboard(
    onKey: (Char) -> Unit,
    maxHeight: Dp,
    modifier: Modifier = Modifier,
    /** Where the cursor enters the keypad, carried by the first key. */
    firstKeyFocus: FocusRequester? = null
) {
    val cursor = remember { SlabCursor(CODE_ROWS) }
    var holds by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    /**
     * A key is a little wider than it is tall, never the reverse. Height used to be
     * derived from the room offered, divided by four: on a narrow panel, ten columns
     * gave keys twice as tall as they were wide, which is what no keyboard is. Column
     * width now sets the height and the available room only caps it; the leftover
     * becomes empty space the keypad centres in.
     */
    val keyWidth = (maxWidth - KEY_GAP * (CODE_COLUMNS - 1)) / CODE_COLUMNS
    val roomPerRow = (maxHeight - KEY_GAP * (CODE_ROWS.size - 1)) / CODE_ROWS.size
    val keyHeight = minOf(keyWidth * KEY_ASPECT, roomPerRow)

    Column(
        verticalArrangement = Arrangement.spacedBy(KEY_GAP),
        modifier = Modifier
            // Exactly ten columns wide: the keypad keeps its proportions and centres
            // rather than stretching to fill its tray.
            .width(keyWidth * CODE_COLUMNS + KEY_GAP * (CODE_COLUMNS - 1))
            // One focusable node and a cursor held here: a key is told whether it is
            // aimed at, it does not decide.
            // pourquoi : CLAUDE.md § Gamepad navigation: the grid holds its own cursor
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
                // A shorter row centres with two full-column margins, so its keys keep
                // one column's width and stay under those above. Sharing the whole row
                // between six keys lost the alignment, and that is what made the keypad
                // read crooked.
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
 * Key height as a share of its width. Physical and Android keyboards are all slightly
 * wider than tall; at 1 you get a chequerboard, beyond it a column of bars.
 */
private const val KEY_ASPECT = 0.86f

/**
 * The alphabet then the digits, in the order they are recited rather than a keyboard
 * layout: a code is read, not typed from muscle memory. Ten columns throughout, and
 * nothing but the characters a code uses. Three rows of nine letters and one of ten
 * digits, with a wider erase key at the end of a row, left no column under any other.
 * Erase went with it: B erases, one letter per press, as it undoes everywhere else.
 * pourquoi : docs/decisions/coquille-ecrans.md § The code keyboard is not the search keyboard
 */
private val CODE_ROWS = listOf(
    listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J"),
    listOf("K", "L", "M", "N", "O", "P", "Q", "R", "S", "T"),
    listOf("U", "V", "W", "X", "Y", "Z"),
    listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
)

private const val CODE_COLUMNS = 10

/**
 * Tight: on a console a keypad is a block read at a glance, not a collection of
 * objects. At 6 dp the keys detached one by one and the block lost its shape.
 */
private val KEY_GAP = 3.dp

/**
 * Smaller than the app's 14 dp: at a key's scale that radius eats half the edge and
 * makes pebbles.
 */
private val KEY_CORNER = 8.dp

/**
 * A library tile in miniature. Three states in the vocabulary used everywhere else: the
 * cursor enlarges it and lays the ring of the axis in force, coral here; a press sinks
 * it and flips its moulding. Nothing is painted inside a key to say a state, the state
 * moves the key.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § MATERIAL (replaces Plastic.kt)
 */
@Composable
private fun RowScope.Key(
    label: String,
    onClick: () -> Unit,
    height: Dp,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = remember { RoundedCornerShape(KEY_CORNER) }

    // On the ring's clock, and leaving without a fade like it: two lit keys are two
    // places to think you are.
    // pourquoi : docs/decisions/navigation-manette.md § The cursor never lingers
    val mark by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(if (selected) RING_IN_MS else 0),
        label = "key-mark"
    )
    // The keypad's only relief, and it belongs to the cursor. Less than a tile's 7 %:
    // keys are three dp apart, and beyond that the aimed key bites into its neighbours
    // instead of passing in front of them.
    val lift by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "key-lift"
    )

    // The plate's high colour, flat. No vertical gradient here: at 40 dp tall its three
    // points of luminance do not show, and computing them forty times per recomposition
    // returns nothing.
    val face = plateColors(dark, oled).first()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .weight(1f)
            .height(height)
            // The aimed key passes in front of its neighbours, ring included.
            .zIndex(if (selected) 1f else 0f)
            .graphicsLayer {
                scaleX = lift
                scaleY = lift
            }
            .focusRing(
                focused = selected,
                shape = shape,
                // A small control takes a reduced ring: the tiles' weight on a
                // thumb-sized key would fill the gap between two keys.
                // pourquoi : docs/decisions/navigation-manette.md § The ring keeps the same weight everywhere
                width = 3.dp,
                glowRadius = 16.dp
            )
            .clip(shape)
            .background(face)
            // A press darkens the face rather than sinking it: a key has no visible
            // travel, and a tile's scale would make the letter hop at every character
            // of a six-character code.
            .then(
                if (pressed) Modifier.background(PressInk.copy(alpha = if (dark) 0.24f else 0.10f))
                else Modifier
            )
            .border(1.dp, edgeColor(dark, oled), shape)
            // Clickable, never focusable: `clickable` makes a node focusable by
            // default, which would double the keypad's cursor.
            .tap(interactionSource = interaction, indication = null, onClick = onClick)
            .focusProperties { canFocus = false }
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            // The aimed key goes Black: the world holds two weights, and this is the
            // mark that stays legible under a thumb, where the ring leaves central
            // vision.
            fontWeight = if (mark > 0f) FontWeight.Black else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** The press ink: the shadows' warm black, never a blue-leaning grey. */
private val PressInk = Color(0xFF241610)

/**
 * The keypad holds its own cursor: one focusable node, a (row, column) index it
 * computes, and keys it tells. An edge is not consumed, so the host screen decides what
 * happens there.
 * pourquoi : docs/decisions/coquille-ecrans.md § The slab holds its own cursor
 * pourquoi : CLAUDE.md § Gamepad navigation: the grid holds its own cursor
 */
private class SlabCursor(rows: List<List<String>>) {
    var row by mutableIntStateOf(0)
    var col by mutableIntStateOf(0)

    fun move(rows: List<List<String>>, dx: Int, dy: Int): Boolean {
        if (dy != 0) {
            val next = row + dy
            if (next !in rows.indices) return false
            // The column is kept in proportion: rows do not all hold the same number of
            // keys, and a raw index made the cursor jump end to end across the short
            // row.
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
 * Four directions and confirm. `onPreviewKeyEvent`, since the keypad is the only
 * focusable node. What leaves by an edge is not consumed: the host screen decides.
 * pourquoi : docs/decisions/coquille-ecrans.md § The slab holds its own cursor
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
        // Swallows the press whose release will be handled, or the platform relays it
        // and one press reads as two.
        in CONFIRM_KEYS -> true
        else -> false
    }
}
