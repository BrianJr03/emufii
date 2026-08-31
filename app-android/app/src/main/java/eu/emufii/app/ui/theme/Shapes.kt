package eu.emufii.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * One corner language, the logo's squircles. The foreground is rounder than
 * the old world: cards 28, tiles 20, artwork 16.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § SHAPES
 */
val TileShape = RoundedCornerShape(20.dp)

/** The artwork inside a tile, tucked just inside the contour. */
val ArtworkShape = RoundedCornerShape(16.dp)

/**
 * The panel radius, named rather than written twice: the cursor's ring reads
 * it too, and a second copy went stale once already.
 */
val CardCorner = 28.dp

/** The panels screens are built out of. */
val CardShape = RoundedCornerShape(CardCorner)

/** The status strip and the small inset "screens" it is made of. */
val InsetShape = RoundedCornerShape(14.dp)

val PillShape = RoundedCornerShape(50)
