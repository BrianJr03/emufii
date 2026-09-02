package eu.emufii.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.emufii.app.R
import eu.emufii.app.library.Rom
import eu.emufii.app.library.RomNames

/**
 * The file on disk is not touched: the ROM keeps its name, its saves stay paired with it,
 * and a third-party emulator knowing it by path sees nothing change. Last resort when the
 * SMDH or banner title is truncated, Japanese, or a tagline; no rule catches every case.
 */
@Composable
fun RenameRomDialog(
    rom: Rom,
    onRenamed: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val names = remember(context) { RomNames(context.applicationContext) }
    var name by remember(rom.uri) { mutableStateOf(rom.displayName) }

    PadDialog(
        title = stringResource(R.string.rename_title),
        onDismiss = onDismiss,
        actions = {
            GhostButton(
                label = stringResource(R.string.rename_cancel),
                onClick = onDismiss
            )
            PrimaryButton(
                label = stringResource(R.string.rename_save),
                onClick = {
                    names.setName(rom, name)
                    onRenamed()
                }
            )
        }
    ) {
        PadDialogText(stringResource(R.string.rename_body))
        // Not a bare `OutlinedTextField`: a field that merely takes focus opens the soft
        // keyboard, so on a pad the cursor passing over it covered the dialog and swallowed
        // the directions. The frame is the traversal step; A goes in, B comes back out.
        PadTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true
        )
    }
}
