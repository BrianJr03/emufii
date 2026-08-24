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
 * Finding the panel, and noticing when it goes.
 *
 * Measured on the Thor (`docs/PHASE1_SCOUT_THOR_SCREEN2.md`): the second screen
 * is a plain Android display, id 4, named `Screen-2`, 1240 x 1080 in use, and
 * it carries `FLAG_PRESENTATION`. That flag is the whole reason no vendor SDK
 * is involved: it is what puts a display in [DisplayManager.DISPLAY_CATEGORY_PRESENTATION],
 * which is a public API and the same one an HDMI dongle or a car head unit
 * arrives through.
 *
 * So nothing here is Thor-specific, and that is on purpose. The device is not
 * named anywhere: a handheld with a second panel, a phone in a dock, a TV on a
 * cable all present the same way, and hard-coding display id 4 would have
 * bought us a feature that works on exactly one machine and breaks on the next
 * firmware that renumbers it.
 */
@Composable
fun rememberPresentationDisplay(): State<Display?> {
    val context = LocalContext.current
    val manager = remember(context) {
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }
    val state = remember { mutableStateOf(manager.presentationDisplay()) }

    // A display can arrive and leave while the app runs: docked, undocked, or
    // simply turned off by the system to save power. Polling would be the wrong
    // shape and listening is free.
    DisposableEffect(manager) {
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {
                state.value = manager.presentationDisplay()
            }

            override fun onDisplayRemoved(displayId: Int) {
                state.value = manager.presentationDisplay()
            }

            override fun onDisplayChanged(displayId: Int) {
                // A panel switched off reports as changed, not removed, and a
                // window left on a dark display is a window nobody can read.
                state.value = manager.presentationDisplay()
            }
        }
        manager.registerDisplayListener(listener, null)
        onDispose { manager.unregisterDisplayListener(listener) }
    }
    return state
}

/**
 * The first presentation display that is actually lit.
 *
 * The state check is not belt and braces: the Thor reports its panel present
 * and `STATE_OFF` when the lid logic turns it down, and a `Presentation` shown
 * on it then throws rather than doing nothing quietly.
 */
private fun DisplayManager.presentationDisplay(): Display? =
    getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
        .firstOrNull { it.isValid && it.state == Display.STATE_ON }
