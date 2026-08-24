package eu.emufii.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.emufii.app.R
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.PillShape
import eu.emufii.app.ui.theme.TileShape
import eu.emufii.app.ui.theme.TrayCyan
import eu.emufii.app.ui.theme.socket

/**
 * The app's own keyboard.
 *
 * The system IME was built for a phone held upright: on a landscape handheld
 * its extract mode takes the whole screen, and the library the player is
 * searching for disappears exactly when they need to see it. This one never
 * rises past half the screen, so the grid stays in view while typing.
 *
 * It is one moulded panel, not a pile of buttons: the keys are recesses cut
 * into the tray, the way a console engraves its silk-print legends, and the
 * panel is frosted by the caller. A key that goes down lights cyan, the one
 * accent, like the cursor it types for.
 *
 * Uppercase only, and that is not a budget cut: a search here is compared
 * without case, and the tiles themselves shout their titles.
 */
@Composable
fun EmufiiKeyboard(
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    /** The most the panel may take: the caller hands it half its screen. */
    maxHeight: Dp,
    modifier: Modifier = Modifier
) {
    // Letters, then digits: game titles are spelled in both, and the toggle
    // costs one key where a fifth row would cost a quarter of the panel.
    var digits by remember { mutableStateOf(false) }

    val keyHeight = (maxHeight - PANEL_PADDING * 2 - ROW_GAP * 3) / 4
    val rows = if (digits) DIGIT_ROWS else LETTER_ROWS

    Column(
        verticalArrangement = Arrangement.spacedBy(ROW_GAP),
        modifier = modifier
            .fillMaxWidth()
            .padding(PANEL_PADDING)
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(KEY_GAP),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { label ->
                    Key(
                        label = label,
                        onClick = {
                            when (label) {
                                BACKSPACE -> onBackspace()
                                SPACE -> onKey(" ")
                                MODE -> digits = !digits
                                else -> onKey(label)
                            }
                        },
                        height = keyHeight,
                        // The service keys stand wider than a letter, so the
                        // rows read as one block with a rhythm, not as grids
                        // of different sizes.
                        weight = when (label) {
                            BACKSPACE, MODE -> 1.7f
                            SPACE -> 9f
                            else -> 1f
                        }
                    )
                }
            }
        }
    }
}

/** Labels that are characters, and three that are not. */
private const val BACKSPACE = "⌫"
private const val SPACE = "ESPACE"
private const val MODE = "123"

private val LETTER_ROWS = listOf(
    listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
    listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
    listOf(MODE, "Z", "X", "C", "V", "B", "N", "M", BACKSPACE),
    listOf(SPACE),
)

private val DIGIT_ROWS = listOf(
    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
    listOf("-", "&", "'", "É", "È", "Ê", "À", "Ç", "Û"),
    listOf(MODE, "!", "?", ".", ",", "(", ")", BACKSPACE),
    listOf(SPACE),
)

private val ROW_GAP = 7.dp
private val KEY_GAP = 7.dp
private val PANEL_PADDING = 12.dp

/**
 * One recess in the panel. At rest it is a socket like the tray's empty slots;
 * held down, it lights cyan, the pressed-in reading of this design and the
 * cursor's own colour in one gesture.
 */
@Composable
private fun androidx.compose.foundation.layout.RowScope.Key(
    label: String,
    onClick: () -> Unit,
    height: Dp,
    weight: Float,
    modifier: Modifier = Modifier
) {
    val dark = LocalEmufiiDarkTheme.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = if (label == SPACE) PillShape else TileShape

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .weight(weight)
            .height(height)
            .socket(shape, dark)
            .drawBehind { if (pressed) drawPressed() }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        when (label) {
            BACKSPACE -> BackspaceGlyph()
            SPACE -> Text(
                stringResource(R.string.lib_keyboard_space),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MODE -> Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** The pressed fill: the accent, lit from inside the recess. */
private fun DrawScope.drawPressed() {
    drawRoundRect(
        color = TrayCyan.copy(alpha = 0.30f),
        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
        size = size
    )
}

/** A cross in a box that leans left: the eraser, drawn at the key's scale. */
@Composable
private fun BackspaceGlyph() {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(22.dp)) { drawBackspace(tint) }
}

private fun DrawScope.drawBackspace(color: androidx.compose.ui.graphics.Color) {
    val w = size.width
    val h = size.height
    val nose = Offset(w * 0.22f, h * 0.5f)
    val top = Offset(w * 0.42f, h * 0.14f)
    val bottom = Offset(w * 0.42f, h * 0.86f)
    val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
    drawLine(color, nose, top, stroke.width, StrokeCap.Round)
    drawLine(color, top, Offset(w * 0.92f, top.y), stroke.width, StrokeCap.Round)
    drawLine(color, Offset(w * 0.92f, top.y), Offset(w * 0.92f, bottom.y), stroke.width, StrokeCap.Round)
    drawLine(color, Offset(w * 0.92f, bottom.y), bottom, stroke.width, StrokeCap.Round)
    drawLine(color, bottom, nose, stroke.width, StrokeCap.Round)
    drawLine(color, Offset(w * 0.52f, h * 0.34f), Offset(w * 0.78f, h * 0.66f), stroke.width, StrokeCap.Round)
    drawLine(color, Offset(w * 0.78f, h * 0.34f), Offset(w * 0.52f, h * 0.66f), stroke.width, StrokeCap.Round)
}
