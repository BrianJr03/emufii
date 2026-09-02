package eu.emufii.app.ui.components

import eu.emufii.app.ui.sounded
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import eu.emufii.app.secondscreen.SecondScreen
import eu.emufii.app.secondscreen.SecondScreenModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import eu.emufii.app.ui.ActionShape
import eu.emufii.app.ui.controlRing
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.LocalAccent
import eu.emufii.app.ui.SilenceSystemSfx

/**
 * White on the light cyan sits at 2.2:1, so the light theme fills with the deep cut
 * (4.6:1) and the dark themes write in the ink cut on the bright one (5:1). Every
 * choosable accent carries the same three cuts at those ratios.
 */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null
) {
    val dark = LocalEmufiiDarkTheme.current
    val accent = LocalAccent.current
    val container = if (dark) accent.bright else accent.deep
    val ink = if (dark) accent.ink else Color.White
    Button(
        onClick = sounded(onClick),
        enabled = enabled,
        shape = ActionShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = ink,
            // Material's grey-on-grey slab reads as an absence, not as a button waiting.
            disabledContainerColor = container.copy(alpha = 0.16f),
            disabledContentColor = container.copy(alpha = 0.55f)
        ),
        modifier = modifier.heightIn(min = 48.dp).controlRing(ActionShape)
    ) {
        if (leading != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                leading()
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Replaces `AlertDialog`: its buttons are `TextButton`s, and the Material focus veil is
 * off across the app, so on a pad the cursor went into a dialog and disappeared.
 * Actions are end-aligned, cancel first: the destructive answer is never under the thumb
 * when the dialog opens.
 */
@Composable
fun PadDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnOutside: Boolean = true,
    /** The body is a `@Composable` and unreadable from here: the sentence is given again. */
    panelDetail: String? = null,
    /** Coral when the question is about a session or someone. */
    panelSocial: Boolean = false,
    actions: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    // pourquoi : docs/decisions/second-ecran.md § What travels to the panel
    DisposableEffect(title, panelDetail, panelSocial) {
        val token = SecondScreen.putAside(
            SecondScreenModel.Asking(
                title = title,
                detail = panelDetail.orEmpty(),
                social = panelSocial
            )
        )
        onDispose { SecondScreen.takeBack(token) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = dismissOnOutside,
            dismissOnBackPress = true,
            // The dialog measures itself: a long body wraps instead of being squeezed
            // into the platform's default width.
            usePlatformDefaultWidth = false
        )
    ) {
        SilenceSystemSfx()
        // Left unbounded on the Thor, 1920 wide in landscape, the plate ran edge to
        // edge and read as a bar across the tray.
        SoftCard(
            modifier = modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                content()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actions()
                }
            }
        }
    }
}

@Composable
fun PadDialogText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
