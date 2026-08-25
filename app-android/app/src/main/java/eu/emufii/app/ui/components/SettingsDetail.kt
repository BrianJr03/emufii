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

/**
 * The inside of an unfolded settings row, and the reason this file exists.
 *
 * Closed, the settings list is a stack of moulded plates and reads at arm's
 * length. Open, every section had grown its own habits: a paragraph, then two
 * buttons of equal weight, then three or four sentences in four different
 * colours saying what had happened. Each line was defensible and the result was
 * a wall — the PS2 profile ended up with eleven stacked texts, the most
 * important of them last.
 *
 * So an unfolded section is now made of three things and nothing else:
 *
 *  1. [DetailNote] — at most one paragraph, and only while it still teaches
 *     something. Once the thing is done, the explanation yields to the state.
 *  2. [DetailActions] — the actions, the first one filled, the rest ghosts.
 *     Two peer pills side by side said "these are the same kind of thing",
 *     which was never true.
 *  3. [DetailStatus] — what the app currently knows, in a recess: a moulded
 *     bead for the state, one sentence, and the facts as aligned rows rather
 *     than as prose with middle dots.
 *
 * The recess is the tray's own vocabulary — the same hole the library grid uses
 * for an empty slot — so a settings screen made of plates now has exactly one
 * kind of hollow, and it means "this is what is, not what you can do".
 */

/** The one paragraph an unfolded section is allowed. */
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
 * behind it — a filename, a BIOS, a console id — which belong in a column of
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
            .then(if (oled) Modifier.border(1.dp, Color(0x33FFFFFF), shape) else Modifier)
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
    val fill = when (tone) {
        DetailTone.GOOD -> listOf(Color(0xFF12A55C), Color(0xFF0C6A3B))
        DetailTone.BUSY -> listOf(Color(0xFF3C82C4), Color(0xFF255C93))
        DetailTone.WARN -> listOf(Color(0xFFC78005), Color(0xFF865603))
        DetailTone.BAD -> listOf(Color(0xFFEB5D47), Color(0xFFD83218))
    }
    Box(
        modifier = Modifier
            .shadow(3.dp, CircleShape)
            .clip(CircleShape)
            .background(Brush.verticalGradient(fill))
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
