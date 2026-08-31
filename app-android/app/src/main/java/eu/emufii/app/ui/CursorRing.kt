package eu.emufii.app.ui

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * The neon-tube cursor: a band around the control, its glow behind, and two white rules
 * giving it thickness. Every measure is a fraction of the band's width, itself a
 * fraction of the control's size. The older ring is one line away, see
 * [FocusRingStyle].
 * pourquoi : docs/decisions/navigation-manette.md § The four layers of the neon cursor
 */
@Composable
fun Modifier.neonFocusRing(
    focused: Boolean,
    shape: Shape,
    start: Color,
    end: Color,
    /** The band's minimum thickness, which small controls keep. */
    minBand: Dp,
    /**
     * How much of the control the band takes. 0.12 suits icons floating in an airy
     * grid; library cover art wants a little less, our tiles being tighter, where the
     * tube became the subject of the cell.
     */
    bandFraction: Float = 0.12f,
    /** The ceiling, so a large tile does not end up inside a tube. */
    maxBand: Dp = 24.dp,
    inMs: Int,
    outMs: Int,
): Modifier {
    val density = LocalDensity.current
    // The band is born and dies by thickening, like the older ring: that is the gesture
    // the app has always had.
    val grow by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(if (focused) inMs else outMs),
        label = "neon-ring"
    )
    // Hoisted above the animation, and that is the point. The ring sits on every
    // control on screen, and an infinite transition created above this line ran on all
    // forty at once: forty invalidations a frame for one visible cursor. Below it, only
    // the control holding the cursor has one alive, restarting from zero on each
    // arrival.
    if (grow <= 0f) return this

    /**
     * The phase step, written twenty-five times a second. An ordinary
     * `InfiniteTransition` writes its value every frame: on a 120 Hz screen the app
     * redrew 120 times a second for an animation with forty-five distinct positions in
     * 1.8 s. The cursor being permanently on screen, that was permanent full-rate
     * rendering, and half the measured heat came from it.
     */
    val step = rememberFlowStep()

    val paints = remember { NeonPaints() }
    return this.drawWithCache {
        val band = bandWidth(size, bandFraction, minBand, maxBand, density) * grow
        val radius = cornerRadiusOf(shape, size, density)
        // The rule: 16 % of the band, bounded. Under 1.5 dp it disappears, over 4 it
        // stops being a rule and becomes a second band.
        val hair = (0.16f * band).coerceIn(1.5f * density.density, 4f * density.density)
        // The glow: 70 % of the band. The 14 dp ceiling is what stops it becoming fog
        // on large tiles.
        val blur = (0.7f * band).coerceIn(4f * density.density, 14f * density.density)

        val w = size.width
        val h = size.height
        // The band surrounds the control, it does not bite into it. Laid inwards on a
        // 150 dp tile it covered 18 dp of cover art on each edge, so the cursor ate
        // what it points at. What kept it in was not geometry but a `shadow` placed
        // before it in the chain, which clips by default.
        // pourquoi : docs/decisions/navigation-manette.md § The ring surrounds, it does not clip
        val rOuter = radius + band
        val rInner = radius

        // The paths depend only on size and thickness. One rounded rectangle per path,
        // never two: Skia rasterises everything else on the CPU, and the thickness
        // animates.
        // pourquoi : docs/decisions/navigation-manette.md § One rounded rectangle per path, never two
        val midline = Path().apply {
            val half = band / 2f
            val r = (rOuter + rInner) / 2f
            addRoundRect(RectF(-half, -half, w + half, h + half), r, r, Path.Direction.CW)
        }
        val outerEdge = Path().apply {
            val i = -band + hair / 2f
            val r = (rOuter - hair / 2f).coerceAtLeast(0f)
            addRoundRect(RectF(i, i, w - i, h - i), r, r, Path.Direction.CW)
        }
        val innerEdge = Path().apply {
            val i = -hair / 2f
            val r = (rInner + hair / 2f).coerceAtLeast(0f)
            addRoundRect(RectF(i, i, w - i, h - i), r, r, Path.Direction.CW)
        }

        // The glow is stacked strokes, not a blur. `BlurMaskFilter` has no GPU
        // equivalent: Android draws the path on the CPU into an intermediate bitmap,
        // every frame, and the cursor is permanently on screen. Three concentric
        // strokes give the same edge gradient in hardware.
        val halo = listOf(
            (band + blur * 1.6f) to 0.16f,
            (band + blur * 0.8f) to 0.30f,
            band to 0.55f,
        )

        // The fallback when the shape is too small for a full turn to read: the fixed
        // vertical gradient, which is the band unanimated.
        val plain = LinearGradient(
            0f, -band, 0f, h + band,
            start.copy(alpha = start.alpha * grow).toArgb(),
            end.copy(alpha = end.alpha * grow).toArgb(),
            Shader.TileMode.CLAMP
        )
        paints.outer.strokeWidth = hair
        paints.outer.shader = LinearGradient(
            0f, 0f, 0f, h,
            Color.White.copy(alpha = 0.50f * grow).toArgb(),
            Color.White.copy(alpha = 0.30f * grow).toArgb(),
            Shader.TileMode.CLAMP
        )
        paints.inner.strokeWidth = hair
        paints.inner.color = Color.White.copy(alpha = 0.40f * grow).toArgb()

        onDrawWithContent {
            drawContent()
            // The phase is read here, which is what makes the gradient flow without
            // recomposing anything. Its bitmap is computed once per step, then found in
            // the cache.
            paints.band.shader = CursorFlow.shader(
                w = w + 2f * band,
                h = h + 2f * band,
                band = band,
                radius = (rOuter + rInner) / 2f,
                start = start.copy(alpha = start.alpha * grow),
                end = end.copy(alpha = end.alpha * grow),
                step = step
            ) ?: plain
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                for ((width, share) in halo) {
                    paints.glow.strokeWidth = width
                    paints.glow.color =
                        start.copy(alpha = start.alpha * grow * share).toArgb()
                    native.drawPath(midline, paints.glow)
                }
                paints.band.strokeWidth = band
                native.drawPath(midline, paints.band)
                native.drawPath(outerEdge, paints.outer)
                native.drawPath(innerEdge, paints.inner)
            }
        }
    }
}

/**
 * The band's thickness: 12 % of the shorter side, bounded. This fraction is what gives
 * a library tile a tube and a bar pill a thread, without any caller saying so.
 */
private fun bandWidth(size: Size, fraction: Float, min: Dp, max: Dp, density: Density): Float {
    val floor = with(density) { min.toPx() }
    val ceiling = with(density) { max.toPx() }
    return (fraction * minOf(size.width, size.height)).coerceIn(floor, ceiling)
}

/**
 * The control's radius, read from its shape. The band has to hug what it surrounds; a
 * guessed radius would give a square ring around a round button. Every shape the app
 * draws is a [RoundedCornerShape]; for the rest the outline is computed by the shape
 * itself.
 */
private fun cornerRadiusOf(shape: Shape, size: Size, density: Density): Float =
    if (shape is RoundedCornerShape) {
        shape.topStart.toPx(size, density)
    } else {
        val outline = shape.createOutline(size, LayoutDirection.Ltr, density)
        when (outline) {
            is androidx.compose.ui.graphics.Outline.Rounded ->
                outline.roundRect.topLeftCornerRadius.x
            else -> 0f
        }
    }

/** The four brushes, kept frame to frame: allocating costs more than drawing. */
private class NeonPaints {
    val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    /** A stroke, not a fill: its thickness follows the arrival animation. */
    val band = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val outer = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
}
