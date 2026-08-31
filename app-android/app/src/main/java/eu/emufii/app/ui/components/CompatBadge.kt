package eu.emufii.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import eu.emufii.app.R
import eu.emufii.app.compat.CompatRating
import eu.emufii.app.ui.theme.Coral
import eu.emufii.app.ui.theme.ErrorLight
import eu.emufii.app.ui.theme.GoodLight
import eu.emufii.app.ui.theme.InkText
import eu.emufii.app.ui.theme.InkTextMuted
import eu.emufii.app.ui.theme.Teal
import eu.emufii.app.ui.theme.WarnDark
import eu.emufii.app.ui.theme.WarnLight

/**
 * What Emufii knows about a game, on the game's own tile. Three marks, one per verdict,
 * and a game nobody has rated shows none: silence already means unknown. Three fixed
 * colours, never the chosen accent: a verdict is the same fact for every player.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § The compatibility badge is the documented exception to the single accent
 */
@Composable
fun CompatBadge(rating: CompatRating, modifier: Modifier = Modifier) {
    val fill = when (rating) {
        CompatRating.PERFECT -> GreenBead
        CompatRating.PARTIAL -> AmberBead
        CompatRating.BROKEN -> RedBead
        CompatRating.UNTESTED -> SlateBead
    }
    val description = compatLabel(rating)

    Box(
        modifier = modifier
            .semantics { contentDescription = description }
            .shadow(3.dp, CircleShape)
            .clip(CircleShape)
            .background(Brush.verticalGradient(fill))
            // Inside the clip, so the rim follows the bead's own edge rather
            // than a square around it.
            .border(1.5.dp, Color.White, CircleShape)
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(15.dp), contentAlignment = Alignment.Center) {
            when (rating) {
                CompatRating.PERFECT -> CheckIcon(size = 13.dp, color = Color.White)
                CompatRating.PARTIAL -> WarnIcon(size = 13.dp, color = Color.White)
                // A cross, and not the crossed circle it was: inside a bead that
                // is already a circle, a second outline just thickened the rim
                // and the bar across it read as a scratch. A bare cross against
                // the tick is also the plainer pair: one says yes, one says no,
                // and the triangle between them says "with caveats".
                CompatRating.BROKEN -> CrossIcon(size = 12.dp, color = Color.White)
                CompatRating.UNTESTED -> TildeIcon(size = 14.dp, color = Color.White)
            }
        }
    }
}

/**
 * The three beads, on the theme's semantic set.
 *
 * Good is pulled towards teal, error towards coral: the duotone world's own
 * hues, and each gradient runs from the semantic light cut down to the axis's
 * ink, so the bead keeps its lit-from-above read without a hex of its own. The
 * glyph inside stays white, so the top of each pair must carry white on its own.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § Semantics (centralised)
 */
private val GreenBead = listOf(GoodLight, Teal.ink)
private val AmberBead = listOf(WarnLight, WarnDark)
private val RedBead = listOf(ErrorLight, Coral.ink)

/**
 * Slate, and the only bead that is not a colour.
 *
 * "Not tried yet" is not a verdict, so it does not get a verdict's voice. A
 * slate bead sits back on the tile where the three coloured ones step forward,
 * which is exactly the weight the fact deserves.
 */
private val SlateBead = listOf(InkTextMuted, InkText)

/**
 * The verdict in words, for the places that have room to say it.
 *
 * Shared rather than written twice: the bead reads it out to a screen reader
 * and the launch card prints it, and two copies of this table would drift the
 * day a fifth verdict appears, in the silent direction: a badge whose spoken
 * name no longer matches the line under the title.
 */
@Composable
fun compatLabel(rating: CompatRating): String = stringResource(
    when (rating) {
        CompatRating.PERFECT -> R.string.compat_perfect
        CompatRating.PARTIAL -> R.string.compat_partial
        CompatRating.BROKEN -> R.string.compat_broken
        CompatRating.UNTESTED -> R.string.compat_untested
    }
)
