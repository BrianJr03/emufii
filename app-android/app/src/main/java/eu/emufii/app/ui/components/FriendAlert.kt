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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.emufii.app.R
import eu.emufii.app.notify.FriendEvent
import kotlinx.coroutines.delay

/**
 * "Clément is online", while the player is already in the app.
 *
 * A card that arrives from the top and leaves on its own. It is deliberately not
 * a dialog and not a system notification: the player is holding the app, the
 * news is small, and anything that has to be dismissed would make a friend
 * coming online feel like a problem to deal with.
 *
 * Tapping it goes to the friends list, which is the only thing anybody would
 * want next. Ignoring it costs nothing, which is the point.
 */
@Composable
fun FriendAlert(
    event: FriendEvent?,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Keyed on the event, so a second friend arriving restarts the countdown
    // rather than inheriting what was left of the first one's.
    LaunchedEffect(event) {
        if (event != null) {
            delay(VISIBLE_MS)
            onDismiss()
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = event != null,
            enter = slideInVertically(animationSpec = tween(220)) { -it } + fadeIn(tween(220)),
            exit = slideOutVertically(animationSpec = tween(180)) { -it } + fadeOut(tween(180))
        ) {
            // Held across the exit animation: reading `event` directly would blank
            // the text the moment it goes null, and the card would slide away
            // empty.
            val shown = lastNonNull(event)
            SoftCard(
                onClick = onOpen,
                modifier = Modifier
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(horizontal = 14.dp, vertical = 8.dp)
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

/**
 * The last event that was not null.
 *
 * Compose keeps the composable alive through the exit animation, so the card
 * needs something to draw after the state has already been cleared.
 */
@Composable
private fun lastNonNull(event: FriendEvent?): FriendEvent? {
    val holder = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<FriendEvent?>(null)
    }
    if (event != null) holder.value = event
    return holder.value
}

private const val VISIBLE_MS = 4_000L
