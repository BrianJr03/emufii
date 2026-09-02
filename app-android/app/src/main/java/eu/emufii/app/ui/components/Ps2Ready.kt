package eu.emufii.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import eu.emufii.app.ps2.Ps2NetworkProfile

/**
 * `Ps2NetworkProfile.isReady` costs ~175 ms of file reads, and a composable body runs
 * again at every recomposition: the launch card paid that three times for one opening.
 * The first frame draws with a cached verdict or the flag the provisioning driver set,
 * and the real check runs off the main thread straight after, correcting only where the
 * card left slot 1 since.
 */
@Composable
fun rememberPs2Ready(): Boolean {
    val context = LocalContext.current
    val ready by produceState(initialValue = Ps2NetworkProfile.isReadyQuick(context), context) {
        value = Ps2NetworkProfile.verifyReady(context)
    }
    return ready
}
