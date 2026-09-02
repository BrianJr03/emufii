package eu.emufii.app.ui

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * False when the system has turned animations off (`ANIMATOR_DURATION_SCALE` at zero),
 * which is also what a handheld spares its battery with: everything reading this needs a
 * frozen state that still looks composed, never a blank one. Read once and remembered.
 */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) != 0f
    }
}
