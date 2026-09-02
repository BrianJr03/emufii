package eu.emufii.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import eu.emufii.app.ui.rememberSlowMillis
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The gradient flowing around the cursor. Not an angular sweep: each pixel is reduced
 * to its arc-length position along the perimeter, so the colour advances at the same
 * speed on a straight edge and in a corner. Rendered into a stretched bitmap and
 * cached.
 * pourquoi : docs/decisions/performance-rendu.md § The cursor gradient is not an angular sweep
 */
internal object CursorFlow {

    /** One more step is not one more bitmap, it is one more repainted window. */
    const val STEPS = 27

    const val PERIOD_MS = 1800

    /** Stretched to the real size by the shader: a gradient has no fine detail to lose. */
    private const val MAX_SIDE = 192

    /** Without it a cursor on a tile recomputes 45 bitmaps per cycle, forever. */
    private val cache = object : LinkedHashMap<Key, BitmapShader>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, BitmapShader>) =
            size > 96
    }

    private data class Key(
        val w: Int,
        val h: Int,
        val band: Int,
        val radius: Int,
        val start: Int,
        val end: Int,
        val step: Int,
    )

    /** [w] and [h] cover the control plus a band on each side; [step] is in `0 until STEPS`. */
    fun shader(
        w: Float,
        h: Float,
        band: Float,
        radius: Float,
        start: Color,
        end: Color,
        step: Int,
    ): Shader? {
        if (w < 1f || h < 1f) return null
        val key = Key(
            w = w.roundToInt(),
            h = h.roundToInt(),
            band = (band * 10f).roundToInt(),
            radius = (radius * 10f).roundToInt(),
            start = start.toArgb(),
            end = end.toArgb(),
            step = ((step % STEPS) + STEPS) % STEPS,
        )
        cache[key]?.let { return it }

        val scale = min(1f, MAX_SIDE / max(w, h))
        val bw = max(2, (w * scale).roundToInt())
        val bh = max(2, (h * scale).roundToInt())

        // The band's midline, which the gradient runs along.
        val half = band / 2f
        val hx = max(1f, w / 2f - half)
        val hy = max(1f, h / 2f - half)
        val r = radius.coerceIn(0f, min(hx, hy))
        val ax = hx - r
        val ay = hy - r
        val quarter = (r * PI / 2.0).toFloat()
        val perimeter = 4f * (ax + ay + quarter)
        if (perimeter <= 0f) return null

        // Cumulative bounds, clockwise from the middle of the top edge; the origin is
        // arbitrary, the cycle being closed.
        val s1 = ax
        val s2 = s1 + quarter
        val s3 = s2 + 2f * ay
        val s4 = s3 + quarter
        val s5 = s4 + 2f * ax
        val s6 = s5 + quarter
        val s7 = s6 + 2f * ay

        val phase = key.step.toFloat() / STEPS
        val sr = start.red; val sg = start.green; val sb = start.blue
        val er = end.red; val eg = end.green; val eb = end.blue

        val pixels = IntArray(bw * bh)
        for (row in 0 until bh) {
            val y = (row + 0.5f) / scale - h / 2f
            for (col in 0 until bw) {
                val x = (col + 0.5f) / scale - w / 2f
                val t = arcLength(x, y, ax, ay, r, s1, s2, s3, s4, s5, s6, s7, perimeter)
                var u = (t / perimeter - phase) % 1f
                if (u < 0f) u += 1f
                val mix = 0.5f - 0.5f * cos(2.0 * PI * u).toFloat()
                val cr = ((sr + (er - sr) * mix) * 255f).roundToInt().coerceIn(0, 255)
                val cg = ((sg + (eg - sg) * mix) * 255f).roundToInt().coerceIn(0, 255)
                val cb = ((sb + (eb - sb) * mix) * 255f).roundToInt().coerceIn(0, 255)
                pixels[row * bw + col] = (0xFF shl 24) or (cr shl 16) or (cg shl 8) or cb
            }
        }

        val bitmap = Bitmap.createBitmap(pixels, bw, bh, Bitmap.Config.ARGB_8888)
        val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        shader.setLocalMatrix(
            Matrix().apply {
                setScale(w / bw, h / bh)
                // The cursor box's origin is at (-band, -band): without this the gradient
                // sits one band down and right.
                postTranslate(-band, -band)
            }
        )
        cache[key] = shader
        return shader
    }

    /**
     * Distance along the perimeter for a point in centred coordinates, folded onto the
     * midline: its projection on a straight edge, its angle from the arc's centre in a
     * corner. Pixels far from the band get a value never read, the band's path clips them.
     */
    private fun arcLength(
        x: Float,
        y: Float,
        ax: Float,
        ay: Float,
        r: Float,
        s1: Float,
        s2: Float,
        s3: Float,
        s4: Float,
        s5: Float,
        s6: Float,
        s7: Float,
        perimeter: Float,
    ): Float {
        val halfPi = (PI / 2.0).toFloat()
        return when {
            // The four straight edges, then the four arcs measured from their own centre.
            x in -ax..ax && y < 0f -> if (x >= 0f) x else perimeter + x
            x > ax && y in -ay..ay -> s2 + (y + ay)
            x in -ax..ax && y > 0f -> s4 + (ax - x)
            x < -ax && y in -ay..ay -> s6 + (ay - y)
            x > ax && y < -ay -> s1 + (atan2(y + ay, x - ax) + halfPi).coerceIn(0f, halfPi) * r
            x > ax -> s3 + atan2(y - ay, x - ax).coerceIn(0f, halfPi) * r
            y > ay -> s5 + (atan2(y - ay, x + ax) - halfPi).coerceIn(0f, halfPi) * r
            else -> {
                var a = atan2(y + ay, x + ax)
                if (a < 0f) a += (2.0 * PI).toFloat()
                s7 + (a - PI.toFloat()).coerceIn(0f, halfPi) * r
            }
        }
    }
}

/**
 * The current phase step, written only when it changes. An `InfiniteTransition` publishes
 * every screen frame, 120 times a second on the Thor, each write redrawing the app; this
 * writes 25 times a second, the number of positions the gradient has.
 */
@Composable
internal fun rememberFlowStep(): Int {
    val millis = rememberSlowMillis()
    return ((millis / CursorFlow.PERIOD_MS) * CursorFlow.STEPS).toInt().mod(CursorFlow.STEPS)
}
