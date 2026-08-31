package eu.emufii.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.LocalEmufiiOledTheme
import eu.emufii.app.ui.theme.plateColors

/**
 * The cursor's opaque fill. Taken out of `screens/settings/SettingsPieces.kt`, where it
 * was `internal` to the settings package while the problem it solves is everywhere: a
 * glow is a shadow, and a shadow crosses anything that is not opaque.
 */

/** Where a settings card sits and how tall it is, in window coordinates. */
data class CardBounds(val top: Float, val height: Float)

/**
 * The card the caller draws into. Root coordinates, not a parent's: what needs it is
 * not at the same depth.
 * pourquoi : docs/decisions/reglages-ecran.md § The opaque fill exists for the cursor, not for the look
 */
val LocalCardBounds = compositionLocalOf { CardBounds(0f, 0f) }

/**
 * An opaque fill that is, to the pixel, what the card already painted here. It exists
 * for the cursor, not for the look.
 * pourquoi : docs/decisions/reglages-ecran.md § The opaque fill exists for the cursor, not for the look
 */
@Composable
fun Modifier.cardSliceFill(shape: Shape, tint: Color = Color.Transparent): Modifier {
    val card = LocalCardBounds.current
    val colors = plateColors(
        dark = LocalEmufiiDarkTheme.current,
        oled = LocalEmufiiOledTheme.current
    )
    var top by remember { mutableFloatStateOf(Float.NaN) }
    return this
        .onGloballyPositioned { top = it.positionInRoot().y }
        .background(
            brush = if (card.height <= 0f || top.isNaN()) SolidColor(colors.first())
            else Brush.verticalGradient(
                colors = colors,
                startY = card.top - top,
                endY = card.top - top + card.height
            ),
            shape = shape
        )
        .then(if (tint == Color.Transparent) Modifier else Modifier.background(tint, shape))
}
