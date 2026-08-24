package eu.emufii.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The material every raised thing in this app is made of. See [Direction].
 *
 * Four things, always in this order: an offset shadow, a top-lighter fill, a
 * hairline edge, a lit bevel. **Take one away and the plate flattens.**
 * pourquoi : docs/decisions/direction-visuelle.md § Une plaque moulée est quatre choses, dans cet ordre
 */

/** How deep the lit edge of a moulding runs, whatever the object's size. */
private val BEVEL_DEPTH = 20.dp

/** The plate's face, as a brush, lit from the top. */
@Composable
fun plateBrush(dark: Boolean, oled: Boolean): Brush =
    Brush.verticalGradient(plateColors(dark, oled))

/**
 * The plate's two colours, exposed so a caller can build the gradient over
 * shifted bounds rather than copying the values and drifting.
 * pourquoi : docs/decisions/direction-visuelle.md § Le biseau ne fait que le tiers supérieur, et sa profondeur est fixe
 */
fun plateColors(dark: Boolean, oled: Boolean): List<Color> = when {
    oled -> listOf(PlateOled, PlateOledLow)
    dark -> listOf(PlateDark, PlateDarkLow)
    else -> listOf(PlateLight, PlateLightLow)
}

/** The moulding's contour. Carries the whole separation on OLED. */
fun edgeColor(dark: Boolean, oled: Boolean): Color =
    if (oled) EdgeOled else if (dark) EdgeDark else EdgeLight

/**
 * A moulded plate: shadow, face, edge, bevel. The shadow's offset follows
 * [lift], or the plate reads as growing rather than lifting.
 * pourquoi : docs/decisions/direction-visuelle.md § Une plaque moulée est quatre choses, dans cet ordre
 */
@Composable
fun Modifier.plate(
    shape: Shape,
    dark: Boolean,
    oled: Boolean,
    lift: Dp = 4.dp,
    bevel: Boolean = true,
    /**
     * True while the control is held down: the plate loses its lift and its lit
     * edge. A moulded button that never travels is a picture of a button.
     * pourquoi : docs/decisions/direction-visuelle.md § Une plaque moulée est quatre choses, dans cet ordre
     */
    pressed: Boolean = false
): Modifier {
    val brush = plateBrush(dark, oled)
    val edge = edgeColor(dark, oled)
    return this
        .shadow(
            elevation = if (oled || pressed) 0.dp else lift,
            shape = shape,
            clip = false,
            // Warm-neutral rather than pure black: a black shadow on a cool grey
            // tray turns the ground green under the plate.
            ambientColor = Color(0xFF0A1220).copy(alpha = if (dark) 0.55f else 0.16f),
            spotColor = Color(0xFF0A1220).copy(alpha = if (dark) 0.65f else 0.22f)
        )
        .clip(shape)
        .background(brush)
        .then(
            if (pressed) Modifier.background(Color(0xFF0A1220).copy(alpha = if (dark) 0.50f else 0.12f))
            else Modifier
        )
        .then(if (bevel && !pressed) Modifier.bevel(shape, dark) else Modifier)
        .border(1.dp, edge, shape)
}

/**
 * The lit top edge of a moulding, along the **top third only** — all the way
 * round it would just be a second stroke. Drawn over the content.
 * pourquoi : docs/decisions/direction-visuelle.md § Le biseau ne fait que le tiers supérieur, et sa profondeur est fixe
 */
@Composable
fun Modifier.bevel(shape: Shape, dark: Boolean): Modifier {
    val color = if (dark) BevelDark else BevelLight
    return this.drawWithContent {
        drawContent()
        // A FIXED depth, never a fraction of the height: an edge does not get
        // deeper because the object is bigger.
        // pourquoi : docs/decisions/direction-visuelle.md § Le biseau ne fait que le tiers supérieur, et sa profondeur est fixe
        val h = BEVEL_DEPTH.toPx().coerceAtMost(size.height)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(color, Color.Transparent),
                startY = 0f,
                endY = h
            ),
            size = Size(size.width, h),
            // A single hairline of it, at the very top, plus the faintest wash
            // below: the wash is what keeps the line from looking drawn on.
            alpha = 0.55f
        )
        drawRect(color = color, size = Size(size.width, 1.dp.toPx()))
    }
}

/**
 * The tray's engraved grid: a scale reference, so plates read as objects of a
 * size. Two hairline families a millimetre apart — an engraving, not a board.
 * pourquoi : docs/decisions/direction-visuelle.md § Le plateau est gravé, et un creux s'éclaire à l'envers
 */
fun androidx.compose.ui.graphics.drawscope.DrawScope.engravedGrid(
    step: Float,
    line: Color,
    highlight: Color
) {
    var x = 0f
    while (x < size.width) {
        drawRect(color = line, topLeft = Offset(x, 0f), size = Size(1f, size.height))
        drawRect(color = highlight, topLeft = Offset(x + 1f, 0f), size = Size(1f, size.height))
        x += step
    }
    var y = 0f
    while (y < size.height) {
        drawRect(color = line, topLeft = Offset(0f, y), size = Size(size.width, 1f))
        drawRect(color = highlight, topLeft = Offset(0f, y + 1f), size = Size(size.width, 1f))
        y += step
    }
}

/** The artwork plate a game icon sits on, in the library and on the cards. */
@Composable
fun tilePlateBrush(dark: Boolean, oled: Boolean): Brush = when {
    oled -> Brush.verticalGradient(listOf(PlateOled, PlateOledLow))
    dark -> Brush.verticalGradient(listOf(PlateDark, PlateDarkLow))
    // Left white: artwork of every possible colour reads against white, which
    // is why it was chosen in the first place.
    else -> SolidColor(Color.White)
}

/**
 * The moulding's contour, drawn *over* whatever fills the shape — [plate] draws
 * its edge before the content, which a full-bleed artwork would cover.
 * pourquoi : docs/decisions/direction-visuelle.md § Le biseau ne fait que le tiers supérieur, et sa profondeur est fixe
 */
@Composable
fun Modifier.moldedRim(shape: Shape, dark: Boolean, oled: Boolean): Modifier {
    val edge = edgeColor(dark, oled)
    val lit = if (dark) BevelDark else BevelLight
    return this.drawWithContent {
        drawContent()
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(outline, color = edge, style = Stroke(width = 1.5.dp.toPx()))
        // The lit hairline stops a third of the way down each side: light from
        // above catches the top of a moulding, never its flanks, and carrying it
        // round turns the rim into a drawn stroke.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(lit.copy(alpha = lit.alpha * 0.9f), Color.Transparent),
                startY = 0f,
                endY = size.height * 0.18f
            ),
            size = Size(size.width, size.height * 0.18f)
        )
    }
}

/**
 * An empty socket: a recess, not a plate, lit **inversely** — dark at the top
 * where a plate is lit, which is why a hole reads as a hole.
 * pourquoi : docs/decisions/direction-visuelle.md § Le plateau est gravé, et un creux s'éclaire à l'envers
 */
@Composable
fun Modifier.socket(shape: Shape, dark: Boolean): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            if (dark) listOf(Color(0x1F000000), Color(0x0AFFFFFF))
            else listOf(Color(0x14000000), Color(0x66FFFFFF))
        )
    )
    .border(1.dp, if (dark) Color(0x14FFFFFF) else Color(0x14000000), shape)
