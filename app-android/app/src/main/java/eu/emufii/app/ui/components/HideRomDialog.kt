package eu.emufii.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.emufii.app.R
import eu.emufii.app.library.HiddenRoms
import eu.emufii.app.library.Rom
import androidx.compose.material3.MaterialTheme

/**
 * The effect is invisible: the tile disappears and nothing on the grid says where it
 * went, so the two lines say the file is untouched and the settings bring it back,
 * rather than asking "are you sure?".
 */
@Composable
fun HideRomDialog(
    rom: Rom,
    onHidden: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hidden = remember(context) { HiddenRoms(context.applicationContext) }

    PadDialog(
        title = stringResource(R.string.hide_title, rom.displayName),
        onDismiss = onDismiss,
        actions = {
            GhostButton(
                label = stringResource(R.string.hide_cancel),
                onClick = onDismiss
            )
            GhostButton(
                label = stringResource(R.string.hide_confirm),
                onClick = {
                    hidden.hide(rom)
                    onHidden()
                },
                // The destructive answer, from the centralized semantic set:
                // error pulled towards coral, never a hand-written red.
                tint = MaterialTheme.colorScheme.error
            )
        }
    ) {
        PadDialogText(stringResource(R.string.hide_body))
    }
}
