package eu.emufii.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The DUOTONE SHELVES material. See [Direction].
 *
 * A tile is four things, in this order: a shadow that grows with logical
 * elevation, a face (flat fill or a vertical micro-gradient), a 1 dp contour,
 * and a **moulding** — a lit rim along the top inner edge fading to a shaded
 * one along the bottom. One light source for the whole app: high, slightly
 * left.
 *
 * The flat pass of 2026-08-27 removed the moulding and kept only the shadow.
 * It read as a single undifferentiated sheet: a white plate sits four points of
 * luminance above a cream shell, and neither a hairline at 24 % nor an ambient
 * shadow at 14 % is enough to say "this is on top of that". The relief came
 * back on 2026-08-28, the bicolour palette stayed. What does *not* come back is
 * the old world's hard offset shadow and its engraved tray: volume now lives in
 * the rim, and the ground is the backdrop's two shelves.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § MATIÈRE
 */

/** The shadow's ink: warm black on the warm neutrals, never blue-black. */
private val ShadowInk = Color(0xFF241610)

/**
 * The plate's face, as a brush: the gentlest vertical pair, on light as on
 * dark. A face lit from above is a hair brighter at the top — three points of
 * luminance, which the eye reads as curvature and not as a gradient.
 */
@Composable
fun plateBrush(dark: Boolean, oled: Boolean): Brush =
    Brush.verticalGradient(plateColors(dark, oled))

/**
 * The plate's two colours, exposed so a caller can build the gradient over
 * shifted bounds rather than copying the values and drifting.
 */
fun plateColors(dark: Boolean, oled: Boolean): List<Color> = when {
    oled -> listOf(PlateOled, PlateOledLow)
    dark -> listOf(PlateDark, PlateDarkLow)
    else -> listOf(PlateLight, PlateLightLow)
}

/** The tile's 1 dp contour. Carries the whole separation on OLED. */
fun edgeColor(dark: Boolean, oled: Boolean): Color =
    if (oled) EdgeOled else if (dark) EdgeDark else EdgeLight

/**
 * The moulding's two ends, top-lit then bottom-shaded — or the reverse, for a
 * recess, which is the same light striking a hollow.
 */
private fun bevelStops(dark: Boolean, oled: Boolean, inverted: Boolean): Pair<Color, Color> {
    val lit = if (dark || oled) BevelDark else BevelLight
    val shade = if (dark || oled) BevelShadeDark else BevelShadeLight
    return if (inverted) shade to lit else lit to shade
}

/**
 * Draws the moulding: one stroke around the shape, inset by half its width so
 * it lands *inside* the tile, painted with a vertical gradient that is lit at
 * the top and shaded at the bottom.
 *
 * One stroke rather than two arcs: a rounded rectangle's outline already turns
 * the corners, and a gradient along the vertical axis lights exactly the parts
 * of it that face up. Two arcs would need to know where the corners are.
 */
private fun DrawScope.drawMoulding(
    shape: Shape,
    dark: Boolean,
    oled: Boolean,
    inverted: Boolean,
    width: Dp
) {
    val (top, bottom) = bevelStops(dark, oled, inverted)
    val w = width.toPx()
    // **Une plaque plus petite que sa propre levre ne se moule pas, elle passe.**
    //
    // `inset` exige ce qu'il reste apres retrait ; en deca de la largeur du
    // trait il rend une taille negative et **leve**. Ce n'etait pas theorique :
    // `IllegalArgumentException: Width and height must be greater than or equal
    // to zero`, en pleine phase de dessin, donc l'app entiere tombe — vu sur le
    // panneau arriere en ouvrant la page de details de deux jeux, ou une vignette
    // traverse le zero pendant le fondu entre deux faces.
    //
    // Compose dessine des noeuds de taille nulle tout a fait legitimement : le
    // temps qu'une mesure arrive, pendant un fondu, sous une contrainte a zero.
    // Un helper de theme n'a donc pas le droit d'en faire une panne — il n'a
    // simplement rien a dessiner.
    if (size.width <= w || size.height <= w) return
    // Inset by half the stroke: a stroke straddles its path, and the outer half
    // would sit on top of the 1 dp contour and mud it.
    val brush = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to top,
            0.42f to top.copy(alpha = top.alpha * 0.12f),
            0.62f to bottom.copy(alpha = bottom.alpha * 0.12f),
            1f to bottom
        ),
        startY = 0f,
        endY = size.height
    )
    inset(w / 2f) {
        drawOutline(
            shape.createOutline(size, layoutDirection, this),
            brush = brush,
            style = Stroke(width = w)
        )
    }
}

/**
 * A tile: shadow, face, contour, moulding. The shadow and the moulding both
 * grow with [lift] — a thing further from the tray casts further and catches
 * more light on its lip.
 *
 * `bevel = false` drops the moulding, for the few surfaces that are a colour
 * field rather than an object (a full-bleed artwork, a filled action).
 * pourquoi : docs/decisions/theme-duotone-shelves.md § MATIÈRE
 */
@Composable
fun Modifier.plate(
    shape: Shape,
    dark: Boolean,
    oled: Boolean,
    lift: Dp = 4.dp,
    bevel: Boolean = true,
    /**
     * True while the control is held down: the tile sinks *and* flips its
     * light — the lip that was catching the light now faces away from it.
     */
    pressed: Boolean = false
): Modifier {
    val brush = plateBrush(dark, oled)
    val edge = edgeColor(dark, oled)
    val elevation = if (pressed) (lift / 3) else lift
    // A wider lip on the surfaces that stand highest, so a dialog does not
    // carry the same rim as a chip.
    val lipWidth = if (lift >= 10.dp) 2.dp else 1.5.dp
    return this
        .graphicsLayer {
            scaleX = if (pressed) 0.98f else 1f
            scaleY = if (pressed) 0.98f else 1f
        }
        .shadow(
            elevation = if (oled) 0.dp else elevation,
            shape = shape,
            clip = false,
            ambientColor = ShadowInk.copy(alpha = if (dark) 0.55f else 0.20f),
            spotColor = ShadowInk.copy(alpha = if (dark) 0.62f else 0.26f)
        )
        .clip(shape)
        .background(brush)
        .then(
            if (pressed) Modifier.background(ShadowInk.copy(alpha = if (dark) 0.22f else 0.07f))
            else Modifier
        )
        .then(
            if (bevel) Modifier.drawWithContent {
                drawContent()
                drawMoulding(shape, dark, oled, inverted = pressed, width = lipWidth)
            } else Modifier
        )
        .border(1.dp, edge, shape)
}

/**
 * The moulding on its own, for a caller that has already drawn its own face
 * and only wants the volume. Draws over its content, so a full-bleed fill does
 * not swallow the lip.
 */
@Composable
fun Modifier.bevel(shape: Shape, dark: Boolean): Modifier {
    val oled = false
    return this.drawWithContent {
        drawContent()
        drawMoulding(shape, dark, oled, inverted = false, width = 1.5.dp)
    }
}

/**
 * Kept for compatibility, and deliberately empty: the tray's texture is the
 * backdrop's two shelves now, and an engraved grid drawn *over* them would give
 * the ground two competing patterns.
 */
fun DrawScope.engravedGrid(
    @Suppress("UNUSED_PARAMETER") step: Float,
    @Suppress("UNUSED_PARAMETER") line: Color,
    @Suppress("UNUSED_PARAMETER") highlight: Color
) {
    // See the KDoc: the shelves are the ground's relief.
}

/** The artwork plate a game icon sits on, in the library and on the cards. */
@Composable
fun tilePlateBrush(dark: Boolean, oled: Boolean): Brush = when {
    oled -> Brush.verticalGradient(listOf(PlateOled, PlateOledLow))
    dark -> Brush.verticalGradient(listOf(PlateDark, PlateDarkLow))
    // Left white: artwork of every possible colour reads against white, which
    // is why it was chosen in the first place.
    else -> SolidColor(PlateLight)
}

/**
 * The tile's contour *and* its moulding, drawn over whatever fills the shape —
 * [plate] draws its edge before the content, which a full-bleed artwork would
 * cover. This is what gives a cover-art tile the same lip as an empty one.
 */
@Composable
fun Modifier.moldedRim(shape: Shape, dark: Boolean, oled: Boolean): Modifier {
    val edge = edgeColor(dark, oled)
    return this.drawWithContent {
        drawContent()
        drawMoulding(shape, dark, oled, inverted = false, width = 1.5.dp)
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(outline, color = edge, style = Stroke(width = 1.dp.toPx()))
    }
}

/**
 * Une place vide : un contour pointille, et rien dedans.
 *
 * A ne pas confondre avec le creux de [socket], qui est une *chose eteinte*.
 * Les deux cohabitent sur la page des consoles — une console retiree de la
 * grille y est un creux, l'alveole qui complete le dernier rang est une place —
 * et tant qu'ils se dessinaient pareil, la page comptait huit consoles dont une
 * sans nom. Le pointille dit « il pourrait y avoir quelque chose ici », ce qui
 * est vrai des deux emplois : celui-ci, et le second dossier de ROMs.
 */
fun Modifier.dashedSlot(shape: Shape, color: Color, corner: Dp = 20.dp): Modifier =
    this.drawBehind {
        val stroke = 1.5.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(corner.toPx()),
            style = Stroke(
                width = stroke,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
            )
        )
    }

/**
 * A socket: a recess carved into the plate. The plate's low tint, and the
 * moulding run backwards — shaded along the top inner edge where the lip
 * overhangs, lit along the bottom where the light finally reaches the floor.
 *
 * This is the same light as [plate], striking a hollow instead of a rise, which
 * is the only honest way to say "sunken" once the app has committed to a light
 * source.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § Les creux
 */
@Composable
fun Modifier.socket(shape: Shape, dark: Boolean): Modifier {
    // L'OLED se lit ici plutot que dans la signature : les vingt appels passent
    // deja `dark`, et un creux qui resterait a la teinte de nuit sur fond eteint
    // est exactement ce qu'on voyait des etageres de la barre du haut — une
    // plaque grise autour des pastilles, plus claire que tout ce qu'elle porte.
    val oled = LocalEmufiiOledTheme.current && dark
    val fill = when {
        oled -> PlateOledLow
        dark -> PlateDarkLow
        else -> PlateLightLow
    }
    val edge = when {
        // Le creux perd son remplissage comme separation : c'est son contour qui
        // le dessine, au meme cran que celui des plaques.
        oled -> EdgeOled
        dark -> Color(0x1FFFFFFF)
        else -> Color(0x1F241610)
    }
    return this
        .clip(shape)
        .background(fill)
        .drawWithContent {
            drawContent()
            drawMoulding(shape, dark, oled = oled, inverted = true, width = 2.dp)
        }
        .border(1.dp, edge, shape)
}
