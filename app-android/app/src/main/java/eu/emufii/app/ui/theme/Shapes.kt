package eu.emufii.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * One corner language: the radius of a moulded corner, at four sizes. Plain
 * radii — a sampled superellipse was tried and removed.
 * pourquoi : docs/decisions/direction-visuelle.md § Un seul langage de coins
 */
val TileShape = RoundedCornerShape(16.dp)

/** The artwork inside a tile, tucked just inside the contour. */
val ArtworkShape = RoundedCornerShape(13.dp)

/**
 * The panel radius, named rather than written twice — the cursor's ring reads
 * it too, and a second copy went stale once already.
 * pourquoi : docs/decisions/direction-visuelle.md § Un seul langage de coins
 */
val CardCorner = 22.dp

/** The panels screens are built out of. */
val CardShape = RoundedCornerShape(CardCorner)

/** The status strip and the small inset "screens" it is made of. */
val InsetShape = RoundedCornerShape(14.dp)

val PillShape = RoundedCornerShape(50)
