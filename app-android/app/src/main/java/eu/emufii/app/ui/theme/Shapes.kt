package eu.emufii.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * One corner language, the logo's squircles. The foreground is rounder than
 * the old world: cards 28, tiles 20, artwork 16.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § SHAPES
 */
val TileShape = RoundedCornerShape(20.dp)

val ArtworkShape = RoundedCornerShape(16.dp)

/** Named rather than written twice: the cursor's ring reads it too, and a second copy went stale. */
val CardCorner = 28.dp

val CardShape = RoundedCornerShape(CardCorner)

val InsetShape = RoundedCornerShape(14.dp)

val PillShape = RoundedCornerShape(50)
