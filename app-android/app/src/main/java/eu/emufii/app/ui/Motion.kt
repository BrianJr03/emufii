package eu.emufii.app.ui

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Whether the app is allowed to move on its own.
 *
 * False when the system has turned animations off (`ANIMATOR_DURATION_SCALE`
 * at zero). That setting exists for people bothered by movement, and anything
 * that animates forever without being asked is exactly what it points at. It is
 * also the setting taken by those sparing their battery on a handheld, so
 * everything that reads it must have a frozen state that still looks composed,
 * never a blank one.
 *
 * Read once and remembered: it is a system setting, and a screen that re-reads
 * it every frame pays for a lookup that cannot have changed.
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
