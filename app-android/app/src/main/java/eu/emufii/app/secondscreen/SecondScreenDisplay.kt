package eu.emufii.app.secondscreen

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Measured on the Thor (`docs/PHASE1_SCOUT_THOR_SCREEN2.md`): the second screen is a plain
 * Android display, id 4, named `Screen-2`, 1240 x 1080 in use, carrying `FLAG_PRESENTATION`.
 * That flag is why no vendor SDK is involved, it puts the display in
 * [DisplayManager.DISPLAY_CATEGORY_PRESENTATION], the same route an HDMI dongle takes.
 * Nothing here is Thor-specific: hard-coding id 4 would break on the firmware that
 * renumbers it.
 */
@Composable
fun rememberPresentationDisplay(): State<Display?> {
    val context = LocalContext.current
    val manager = remember(context) {
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }
    val state = remember { mutableStateOf(manager.presentationDisplay()) }

    DisposableEffect(manager) {
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {
                state.value = manager.presentationDisplay()
            }

            override fun onDisplayRemoved(displayId: Int) {
                state.value = manager.presentationDisplay()
            }

            override fun onDisplayChanged(displayId: Int) {
                // A panel switched off reports as changed, not removed.
                state.value = manager.presentationDisplay()
            }
        }
        manager.registerDisplayListener(listener, null)
        onDispose { manager.unregisterDisplayListener(listener) }
    }
    return state
}

/**
 * The Thor reports its panel present and `STATE_OFF` when the lid logic turns it down,
 * and a `Presentation` shown on it then throws rather than doing nothing quietly.
 */
private fun DisplayManager.presentationDisplay(): Display? =
    getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
        .firstOrNull { it.isValid && it.state == Display.STATE_ON }
