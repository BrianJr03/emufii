package eu.emufii.app.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.dp
import eu.emufii.app.ui.theme.CardShape
import eu.emufii.app.ui.theme.Coral
import eu.emufii.app.ui.theme.Teal

/**
 * The coral-to-teal edge light: both axes carried on a card's contour. The path is the
 * contour itself and goes all the way round: it borders, it does not bar. [phase]
 * drifts the gradient; at zero it does not move.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § The two-axis rim borders, it does not bar
 */
fun Modifier.waitTrim(phase: Float = 0f): Modifier = drawWithContent {
    drawContent()
    val stroke = TRIM_STROKE.toPx()
    // Inset by half a stroke: a contour straddles its path, and the outer half would
    // fall off the plate.
    inset(stroke / 2f) {
        val outline = Path().apply {
            addOutline(CardShape.createOutline(size, layoutDirection, this@drawWithContent))
        }
        // The transition sweeps the diagonal rather than sitting in the middle: at
        // phase 0 the contour is nearly all teal, at 1 nearly all coral, and in between
        // it turns without either side changing role.
        val travel = 2f * phase - 0.5f
        val start = Offset(size.width * travel, size.height * travel)
        drawPath(
            outline,
            brush = Brush.linearGradient(
                colors = listOf(Coral.bright, Teal.bright),
                start = start,
                end = Offset(start.x + size.width, start.y + size.height)
            ),
            style = Stroke(width = stroke)
        )
    }
}

/**
 * Three, not six: laid on the edge it visually doubles the card's contour, where the
 * same weight across the face was a scar.
 */
private val TRIM_STROKE = 3.dp
