package eu.emufii.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import eu.emufii.app.R
import eu.emufii.app.tunnel.TunnelHolder
import androidx.compose.material3.MaterialTheme

/**
 * Asked before one tunnel takes the VPN slot from the other.
 *
 * The one place in Emufii that stops to ask, because it is the one action the app
 * takes that silently ends something the player is in the middle of, see
 * [eu.emufii.app.tunnel.tunnelHolder]. Everywhere else the app just goes.
 *
 * And because it is the one dialog that has to be answered rather than waved
 * away, it does not close on a tap outside: dismissing it means "leave the
 * tunnel where it is", which is an answer, and an answer is given on a button.
 */
@Composable
fun TunnelConflictDialog(
    held: TunnelHolder,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val body = when (held) {
        TunnelHolder.WFC -> R.string.tunnel_conflict_wfc
        else -> R.string.tunnel_conflict_session
    }
    PadDialog(
        title = stringResource(R.string.tunnel_conflict_title),
        onDismiss = onDismiss,
        dismissOnOutside = false,
        actions = {
            GhostButton(
                label = stringResource(R.string.common_cancel),
                onClick = onDismiss
            )
                // The error tone, because confirming ends a session someone is
                // inside: the centralized coral-leaning error, never a raw red.
                GhostButton(
                    label = stringResource(R.string.tunnel_conflict_confirm),
                    onClick = onConfirm,
                    tint = MaterialTheme.colorScheme.error
                )
        }
    ) {
        PadDialogText(stringResource(body))
    }
}
