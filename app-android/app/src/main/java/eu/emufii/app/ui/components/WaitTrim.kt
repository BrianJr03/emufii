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
 * Le liseré corail→turquoise : les deux axes portés par le contour d'une carte.
 *
 * Le tracé est le contour lui-même et en fait le tour — il borde, il ne barre
 * pas. [phase] fait dériver le dégradé ; à zéro il ne bouge pas.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § Le liseré des deux axes borde, il ne barre pas
 */
fun Modifier.waitTrim(phase: Float = 0f): Modifier = drawWithContent {
    drawContent()
    val stroke = TRIM_STROKE.toPx()
    // Rentré d'un demi-trait : un contour chevauche son tracé, et la moitié
    // extérieure tomberait hors de la plaque.
    inset(stroke / 2f) {
        val outline = Path().apply {
            addOutline(CardShape.createOutline(size, layoutDirection, this@drawWithContent))
        }
        // La bande de transition balaie la diagonale plutôt que de rester au
        // milieu : à phase 0 le contour est presque tout turquoise, à 1 presque
        // tout corail, et entre les deux il tourne sans qu'aucun côté ne
        // change de rôle.
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
 * L'épaisseur du liseré. Trois, pas six : posé sur l'arête il double
 * visuellement le contour de la carte, là où le même poids en travers de la
 * face était une balafre.
 */
private val TRIM_STROKE = 3.dp
