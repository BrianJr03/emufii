package eu.emufii.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import eu.emufii.app.ps2.Ps2NetworkProfile

/**
 * Is the PS2 network profile in place, answered without stalling the frame.
 *
 * Proving it properly costs ~175 ms of file reads (`Ps2NetworkProfile.isReady`),
 * and a composable body runs again at every recomposition: the launch card was
 * paying that three times for one opening, which is the hitch the popup showed.
 *
 * So the first frame draws with what is already known: a cached verdict, or the
 * flag the provisioning driver set, and the real check runs off the main thread
 * straight after, correcting the answer only in the rare case where the card
 * left slot 1 since. Nothing flashes in the common case, because the cheap
 * answer and the real one agree.
 */
@Composable
fun rememberPs2Ready(): Boolean {
    val context = LocalContext.current
    val ready by produceState(initialValue = Ps2NetworkProfile.isReadyQuick(context), context) {
        value = Ps2NetworkProfile.verifyReady(context)
    }
    return ready
}
