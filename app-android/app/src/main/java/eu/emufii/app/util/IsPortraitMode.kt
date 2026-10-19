package eu.emufii.app.util

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Returns `true` when the current composition is drawn in portrait orientation.
 *
 * Reads [LocalConfiguration], so callers recompose automatically on rotation.
 * Use to branch layout decisions (Row vs Column, column counts, tile sizing)
 * between portrait and landscape without threading the orientation through
 * composable parameters.
 */
@Composable
fun isPortraitMode(): Boolean = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT