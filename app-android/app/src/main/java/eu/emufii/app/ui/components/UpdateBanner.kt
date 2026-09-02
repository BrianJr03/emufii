package eu.emufii.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import eu.emufii.app.R
import eu.emufii.app.ui.LocalRingTone
import eu.emufii.app.ui.RingTone
import eu.emufii.app.update.LatestVersion
import eu.emufii.app.update.UpdateInstaller
import eu.emufii.app.update.UpdateOutcome
import kotlinx.coroutines.launch

/** A constant, not a measurement: reading the text back would cost a layout pass per opening. */
val UPDATE_BANNER_ROOM = 96.dp

/**
 * Emufii is sideloaded: no store gives notice on its behalf. The locks are in
 * [UpdateInstaller]; tapping is the only consent asked for, Android 12 and later
 * installing a self-update with no confirmation dialog, verified here, so the label says
 * "Install". The dismissal holds for that version only.
 */
@Composable
fun UpdateBanner(
    latest: LatestVersion,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var busy by remember { mutableStateOf(false) }
    // Replaces the notes line rather than adding below it: the card has a fixed height,
    // and a fourth line pushes the buttons out of frame in the Thor's landscape.
    var failure by remember { mutableStateOf<Int?>(null) }

    fun install() {
        if (busy) return
        busy = true
        failure = null
        scope.launch {
            when (val outcome = UpdateInstaller.downloadAndInstall(context, latest)) {
                is UpdateOutcome.HandedToAndroid -> Unit
                is UpdateOutcome.NeedsPermission ->
                    runCatching { context.startActivity(outcome.settings) }
                        .onFailure { failure = R.string.update_failed_permission }
                UpdateOutcome.Unavailable -> failure = R.string.update_failed_unavailable
                UpdateOutcome.DownloadFailed -> failure = R.string.update_failed_download
                UpdateOutcome.Rejected -> failure = R.string.update_failed_rejected
            }
            busy = false
        }
    }

    // pourquoi : docs/decisions/theme-duotone-shelves.md § Two semantic axes
    val coral = MaterialTheme.colorScheme.tertiary
    CompositionLocalProvider(LocalRingTone provides RingTone.CORAL) {
    SoftCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                stringResource(R.string.update_available, latest.versionName),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            // Off the configuration, not `Locale.getDefault()`: this is the language the
            // system actually applied to the app.
            val locale = LocalConfiguration.current.locales[0]
            val secondLine = failure?.let { stringResource(it) } ?: latest.notesFor(locale)
            secondLine?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (failure != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GhostButton(
                    label = stringResource(R.string.update_later),
                    onClick = { if (!busy) onDismiss() },
                    tint = coral
                )
                latest.url?.let { url ->
                    GhostButton(
                        label = stringResource(R.string.update_open),
                        tint = coral,
                        onClick = {
                            if (busy) return@GhostButton
                            // A device with no browser has nothing to open; that must not
                            // bring the library down.
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }.onFailure { if (it !is ActivityNotFoundException) throw it }
                        }
                    )
                }
                GhostButton(
                    label = stringResource(R.string.update_install),
                    onClick = { if (!busy) install() },
                    tint = coral,
                    icon = if (!busy) null else { tint ->
                        CircularProgressIndicator(
                            color = tint,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                )
            }
        }
    }
    }
}
