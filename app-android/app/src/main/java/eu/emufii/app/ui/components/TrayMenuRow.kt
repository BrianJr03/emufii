package eu.emufii.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import eu.emufii.app.ui.tap

/**
 * A dropdown row: a glyph, a label, and something to highlight it with. Two
 * pixel-identical copies before this. The highlight is a ground, never a ring: this is
 * a list, not an isolated control.
 * pourquoi : docs/decisions/bibliotheque.md § One menu row, not two pixel-identical copies
 */
@Composable
internal fun TrayMenuRow(
    label: String,
    onClick: () -> Unit,
    glyph: DrawScope.(Color) -> Unit,
    /** Where the cursor lands when the menu opens. Null on an ordinary row. */
    landing: FocusRequester? = null,
    /** The row's right end: a tick, or nothing. */
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val highlighted = pressed || focused
    val tint = MaterialTheme.colorScheme.onSurface

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (highlighted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                else Color.Transparent
            )
            .then(if (landing != null) Modifier.focusRequester(landing) else Modifier)
            .tap(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Read here rather than in the `Canvas`: the draw lambda is not composable, so
        // the theme is out of reach there.
        Canvas(Modifier.size(18.dp)) { glyph(tint) }
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = tint,
            modifier = if (trailing != null) Modifier.weight(1f) else Modifier
        )
        trailing?.invoke(this)
    }
}
