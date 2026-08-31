package eu.emufii.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.LocalEmufiiOledTheme
import eu.emufii.app.ui.theme.socket
import eu.emufii.app.ui.theme.EdgeOled
import eu.emufii.app.ui.theme.GoodLight
import eu.emufii.app.ui.theme.GoodDark
import eu.emufii.app.ui.theme.InfoLight
import eu.emufii.app.ui.theme.InfoDark
import eu.emufii.app.ui.theme.WarnLight
import eu.emufii.app.ui.theme.WarnDark
import eu.emufii.app.ui.theme.ErrorLight
import eu.emufii.app.ui.theme.ErrorDark

/**
 * The inside of an unfolded settings row, and the reason this file exists. Three things
 * and nothing else: [DetailNote], [DetailActions], [DetailStatus]. The state's hollow
 * is the tray's vocabulary: a settings screen has one kind of state.
 * pourquoi : docs/decisions/coquille-ecrans.md § An expanded row is made of three things, and nothing else
 */
@Composable
fun DetailNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/**
 * The actions of a section, in the order they are meant.
 *
 * Nothing here decides what a button looks like: the caller passes a filled
 * [PrimaryButton] first and [GhostButton]s after. What this owns is the gap
 * between them, so that two sections never disagree about it.
 */
@Composable
fun DetailActions(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}

/** What a state looks like. Four, matching the beads the library already uses. */
enum class DetailTone { GOOD, BUSY, WARN, BAD }

/** One fact, as a label and its value. */
data class DetailFact(val label: String, val value: String)

/**
 * What the app knows right now, in a recess under the actions.
 *
 * [headline] is the one sentence that matters, and it comes first because it is
 * what the player opened the section to find out. [facts] are the identifiers
 * behind it, a filename, a BIOS, a console id, which belong in a column of
 * aligned rows: they are looked *up*, not read, and prose separated by middle
 * dots makes that impossible.
 *
 * [caveat] is the one thing that can be wrong while the state is still good,
 * and it sits at the bottom in the error colour, because it qualifies
 * everything above it rather than replacing it.
 */
@Composable
fun DetailStatus(
    tone: DetailTone,
    headline: String,
    modifier: Modifier = Modifier,
    facts: List<DetailFact> = emptyList(),
    caveat: String? = null,
) {
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .socket(shape, dark)
            // On OLED the recess has almost nothing to shade against: the tray
            // is truly off, so the gradient that carves the hole everywhere
            // else lands on black and vanishes. There the edge does the whole
            // job, and it is drawn stronger, exactly as the design system says
            // for that theme.
            .then(if (oled) Modifier.border(1.dp, EdgeOled, shape) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            // Top, not centre: a headline is meant to be short, but a
            // translation can push one onto two lines, and a bead floating at
            // the middle of a two-line sentence stops reading as its mark.
            verticalAlignment = Alignment.Top
        ) {
            StateBead(tone)
            Text(
                headline,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (facts.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (fact in facts) FactRow(fact)
            }
        }
        caveat?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * A label and its value, on one line, with the labels aligned.
 *
 * The fixed label column is the whole point: four facts under each other with
 * their values starting at the same x are scannable, and the same four written
 * as sentences are not. 84 dp holds the longest label the app has in either
 * language without wrapping it.
 */
@Composable
private fun FactRow(fact: DetailFact) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            fact.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(84.dp)
        )
        Text(
            fact.value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * The state as a moulded bead, the same object the tiles wear.
 *
 * Deliberately the same shape, lighting and glyph vocabulary as the
 * compatibility bead on a game tile: an app should have one way of saying "this
 * is fine" and one way of saying "this is not", and a settings screen that
 * invented a second one would be teaching the player twice.
 */
@Composable
fun StateBead(tone: DetailTone, size: Dp = 14.dp) {
    val dark = LocalEmufiiDarkTheme.current
    val fill = when (tone) {
        DetailTone.GOOD -> if (dark) GoodDark else GoodLight
        DetailTone.BUSY -> if (dark) InfoDark else InfoLight
        DetailTone.WARN -> if (dark) WarnDark else WarnLight
        DetailTone.BAD -> if (dark) ErrorDark else ErrorLight
    }
    Box(
        modifier = Modifier
            .shadow(3.dp, CircleShape)
            .clip(CircleShape)
            .background(fill)
            .border(1.5.dp, Color.White, CircleShape)
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            when (tone) {
                DetailTone.GOOD -> CheckIcon(size = size * (12f / 14f), color = Color.White)
                DetailTone.BUSY -> TildeIcon(size = size * (13f / 14f), color = Color.White)
                DetailTone.WARN -> WarnIcon(size = size * (12f / 14f), color = Color.White)
                DetailTone.BAD -> CrossIcon(size = size * (11f / 14f), color = Color.White)
            }
        }
    }
}
