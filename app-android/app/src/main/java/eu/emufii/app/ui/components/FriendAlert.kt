package eu.emufii.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.emufii.app.R
import eu.emufii.app.notify.FriendEvent
import eu.emufii.app.ui.LocalRingTone
import eu.emufii.app.ui.RingTone
import kotlinx.coroutines.delay

/**
 * Neither a dialog nor a system notification: the player is already holding the app, and
 * anything that has to be dismissed would make a friend coming online read as a problem.
 */
@Composable
fun FriendAlert(
    event: FriendEvent?,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(event) {
        if (event != null) {
            delay(VISIBLE_MS)
            onDismiss()
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        AnimatedVisibility(
            visible = event != null,
            enter = slideInVertically(animationSpec = tween(220)) { -it } + fadeIn(tween(220)),
            exit = slideOutVertically(animationSpec = tween(180)) { -it } + fadeOut(tween(180))
        ) {
            // Held across the exit animation: reading `event` directly blanks the text the
            // moment it goes null, and the card slides away empty.
            val shown = lastNonNull(event)
            // pourquoi : docs/decisions/theme-duotone-shelves.md § Two semantic axes
            CompositionLocalProvider(LocalRingTone provides RingTone.CORAL) {
            SoftCard(
                onClick = onOpen,
                modifier = Modifier
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(vertical = 8.dp)
                    // In the gap the top bar leaves, between the service lamp and the
                    // social shelf. Measured on the Thor 2026-09-02: free from 717 to
                    // 1487 px of 1920, so 334 dp starting 188 dp off the right edge.
                    // pourquoi : docs/decisions/bibliotheque.md § The top bar: two shelves, never a bar
                    .padding(end = ALERT_END_INSET)
                    .widthIn(max = ALERT_WIDTH)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    val name = shown?.name ?: stringResource(R.string.notify_friend_unnamed)
                    Avatar(name = name, size = 34.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            when (shown) {
                                is FriendEvent.StartedPlaying -> shown.game
                                    ?.let { stringResource(R.string.alert_friend_playing, it) }
                                    ?: stringResource(R.string.alert_friend_in_game)
                                else -> stringResource(R.string.alert_friend_online)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            }
        }
    }
}

/** Compose keeps the composable alive through the exit animation, with nothing left to draw. */
@Composable
private fun lastNonNull(event: FriendEvent?): FriendEvent? {
    val holder = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<FriendEvent?>(null)
    }
    if (event != null) holder.value = event
    return holder.value
}

private val ALERT_WIDTH = 320.dp

/** Clears the social shelf, which starts 188 dp off the right edge. */
private val ALERT_END_INSET = 196.dp

private const val VISIBLE_MS = 4_000L
