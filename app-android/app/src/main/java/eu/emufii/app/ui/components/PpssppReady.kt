package eu.emufii.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import eu.emufii.app.psp.PpssppConfigStore

/**
 * `PpssppConfigStore.isReady` walks the granted memory-stick tree through SAF, which is
 * not main-thread work. The first frame answers with the cheap part, a folder picked once
 * and still remembered; the walk corrects it right after if the grant was revoked or the
 * folder no longer holds `PSP/SYSTEM/ppsspp.ini`.
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
