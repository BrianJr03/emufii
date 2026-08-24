package eu.emufii.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import eu.emufii.app.psp.PpssppConfigStore

/**
 * Is the PPSSPP automatic setup in place, answered without stalling the frame.
 *
 * Same shape as [rememberPs2Ready]: the real check (`PpssppConfigStore.isReady`)
 * walks the granted memory-stick tree through SAF, which is not main-thread
 * work. The first frame answers with the cheap part — a folder was picked once
 * and is still remembered — and the tree walk corrects it right after, in the
 * rare case where the grant was revoked or the folder no longer holds
 * `PSP/SYSTEM/ppsspp.ini`.
 */
@Composable
fun rememberPpssppReady(): Boolean {
    val context = LocalContext.current
    val ready by produceState(
        initialValue = PpssppConfigStore(context).rootUri() != null,
        context
    ) {
        value = PpssppConfigStore(context).isReady()
    }
    return ready
}
