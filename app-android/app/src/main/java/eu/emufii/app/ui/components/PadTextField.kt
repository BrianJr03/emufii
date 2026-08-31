package eu.emufii.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import eu.emufii.app.ui.CONFIRM_KEYS
import eu.emufii.app.ui.controlRing
import eu.emufii.app.ui.ringColor
import eu.emufii.app.ui.Sfx

/**
 * The field is not a step in the traversal, its frame is: confirm on the frame opens
 * the field.
 * pourquoi : docs/decisions/coquille-ecrans.md § A text field must not be a cursor stop
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PadTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    shape: Shape = RoundedCornerShape(FIELD_CORNER)
) {
    var editing by remember { mutableStateOf(false) }
    val frame = remember { FocusRequester() }
    val field = remember { FocusRequester() }
    val interaction = remember { MutableInteractionSource() }

    val framed by interaction.collectIsFocusedAsState()
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(editing) {
        if (editing) {
            runCatching { field.requestFocus() }
            keyboard?.show()
        } else {
            keyboard?.hide()
        }
    }

    // Without it, closing the keyboard left focus in a field that had stopped editing.
    BackHandler(enabled = editing) {
        editing = false
        runCatching { frame.requestFocus() }
    }

    // The keyboard swallows the first B, so its disappearance ends the edit.
    // pourquoi : docs/decisions/coquille-ecrans.md § It is the keyboard disappearing that ends editing, not the key
    val imeVisible = WindowInsets.isImeVisible
    var opened by remember { mutableStateOf(false) }
    LaunchedEffect(editing, imeVisible) {
        if (!editing) {
            opened = false
        } else if (imeVisible) {
            opened = true
        } else if (opened) {
            editing = false
            runCatching { frame.requestFocus() }
        }
    }

    Column(modifier = modifier) {
        // The label sits above the frame, not in it: `OutlinedTextField` reserves room
        // for its own.
        // pourquoi : docs/decisions/coquille-ecrans.md § The ring is the field's outline, and it is the only arrangement that holds
        if (label != null) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isError) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
        }
        Box(
            modifier = Modifier
                // One outline at a time, so there is nothing left to align. Before the
                // fill.
                // pourquoi : docs/decisions/coquille-ecrans.md § The ring is the field's outline, and it is the only arrangement that holds
                .controlRing(shape, enabled = !editing)
                // Opaque, or the glow shows through: the cursor's glow is a shadow.
                // pourquoi : docs/decisions/reglages-ecran.md § The opaque fill exists for the cursor, not for the look
                .cardSliceFill(shape)
                .focusRequester(frame)
                .focusable(interactionSource = interaction)
                .onKeyEvent { event ->
                    if (editing) return@onKeyEvent false
                    if (event.key in CONFIRM_KEYS) {
                        // Opened on release, as everywhere else; the key-down is
                        // swallowed so one press counts once.
                        if (event.type == KeyEventType.KeyUp) { Sfx.click(); editing = true }
                        true
                    } else {
                        false
                    }
                }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = null,
                placeholder = placeholder?.let { { Text(it) } },
                isError = isError,
                singleLine = singleLine,
                shape = shape,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                // Transparent exactly when the ring is drawn, so the two never show at
                // once.
                // pourquoi : docs/decisions/theme-duotone-shelves.md § Session / Join, coral domain
                colors = OutlinedTextFieldDefaults.colors(
                    cursorColor = ringColor(),
                    focusedBorderColor = ringColor(),
                    unfocusedBorderColor =
                        if (framed) Color.Transparent
                        else MaterialTheme.colorScheme.outline,
                    disabledBorderColor =
                        if (framed) Color.Transparent
                        else MaterialTheme.colorScheme.outline,
                    // The error outline clears like the others: Material puts it ahead
                    // of the other three.
                    // pourquoi : docs/decisions/coquille-ecrans.md § The ring is the field's outline, and it is the only arrangement that holds
                    errorBorderColor =
                        if (framed) Color.Transparent
                        else MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(field)
                    .focusProperties { canFocus = editing }
                    // Focus can leave other than through B, the system gesture for
                    // instance.
                    .onFocusChanged { if (editing && !it.isFocused) editing = false }
            )

            // Compose tests children first, and the field consumed taps it then did
            // nothing with.
            // pourquoi : docs/decisions/coquille-ecrans.md § The finger could not reach the frame
            if (!editing) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) { detectTapGestures { editing = true } }
                )
            }
        }
        // Outside the ring: the helper text is not the control being aimed at.
        if (supportingText != null) {
            Box(modifier = Modifier.padding(start = 20.dp, top = 4.dp)) {
                ProvideTextStyle(
                    MaterialTheme.typography.bodySmall.copy(
                        color = if (isError) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) { supportingText() }
            }
        }
    }
}

private val FIELD_CORNER = 16.dp

