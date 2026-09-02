package eu.emufii.app.secondscreen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import eu.emufii.app.R
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.LocalEmufiiOledTheme
import eu.emufii.app.ui.theme.PillShape
import eu.emufii.app.ui.theme.plate

/**
 * The front screen needs the legend too: one drawing of this motif, or the two drift.
 * pourquoi : docs/decisions/second-ecran.md § The legend, and why the symbols are drawn
 */
@Composable
fun PadHintRow(hint: PadHint, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PadKeyCap(hint)
        Text(
            stringResource(hint.label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

internal val LEGEND_CAP = 26.dp

/**
 * Moulded like the machine's own: a plate, never a recess.
 * pourquoi : docs/decisions/second-ecran.md § The legend, and why the symbols are drawn
 */
@Composable
fun PadKeyCap(hint: PadHint) {
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current
    val tint = MaterialTheme.colorScheme.onSurface
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(LEGEND_CAP)
            .plate(
                PillShape,
                dark = dark,
                oled = oled,
                lift = 2.dp,
                // A hint about holding shows a held button: no lift, no lit edge.
                // pourquoi : docs/decisions/second-ecran.md § The legend, and why the symbols are drawn
                pressed = hint.held
            )
    ) {
        val glyph = hint.glyph
        if (glyph == null) DPadGlyph(tint) else CapLetter(glyph, tint)
    }
}

/**
 * Laying the text out cannot centre a letter on its ink: the glyph is drawn and placed
 * from [android.graphics.Paint.getTextBounds], pen at `w/2 - (left + right)/2`.
 * pourquoi : docs/decisions/second-ecran.md § A letter is centred on its ink, not on its box
 */
@Composable
private fun CapLetter(glyph: String, tint: Color) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val paint = remember(context, tint, density) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            typeface = runCatching { ResourcesCompat.getFont(context, R.font.rounded_bold) }
                .getOrNull() ?: android.graphics.Typeface.DEFAULT_BOLD
            textSize = with(density) { 14.sp.toPx() }
            // The pen is positioned from the ink bounds below; CENTER would subtract
            // half an advance on top of it.
            textAlign = android.graphics.Paint.Align.LEFT
            color = tint.toArgb()
        }
    }
    val bounds = remember(paint, glyph) {
        android.graphics.Rect().also { paint.getTextBounds(glyph, 0, glyph.length, it) }
    }
    Canvas(Modifier.size(26.dp)) {
        drawContext.canvas.nativeCanvas.drawText(
            glyph,
            size.width / 2f - (bounds.left + bounds.right) / 2f,
            size.height / 2f - (bounds.top + bounds.bottom) / 2f,
            paint
        )
    }
}

/**
 * The d-pad, drawn rather than typed.
 * pourquoi : docs/decisions/second-ecran.md § The legend, and why the symbols are drawn
 */
@Composable
private fun DPadGlyph(tint: Color) {
    Canvas(Modifier.size(10.dp)) {
        // The proportion a moulded d-pad has: thinner reads as a mathematical plus.
        val arm = size.width * 0.38f
        val radius = CornerRadius(size.width * 0.06f, size.width * 0.06f)
        drawRoundRect(
            color = tint,
            topLeft = Offset((size.width - arm) / 2f, 0f),
            size = Size(arm, size.height),
            cornerRadius = radius
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, (size.height - arm) / 2f),
            size = Size(size.width, arm),
            cornerRadius = radius
        )
    }
}
