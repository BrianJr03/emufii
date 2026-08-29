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
 * The filled action, the only one of its kind on a screen.
 *
 * Two cuts of the one accent, and the second exists for a measurable reason:
 * white text on the light cyan sits at 2.2:1. The deep cut takes it to 4.6:1,
 * so the light theme fills with that and keeps the bright cut for the cursor,
 * where nothing is written on top of it. The dark themes keep the bright cut
 * and write on it in the ink cut, at 5:1. Every choosable accent carries the
 * same three cuts at those same ratios, so the button is as legible whichever
 * colour is in force.
 */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** Une marque posee avant le libelle : le visage de la destination. */
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
            // Disabled, but still legibly the action: Material's grey-on-grey
            // slab reads as an absence rather than as a button waiting its turn.
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
 * The shape every dialog in Emufii takes: one moulded plate, and its actions
 * made of the same controls as the rest of the app.
 *
 * It replaces `AlertDialog`, which was the last Material component left with a
 * say in the look of a screen. Two things were wrong with it, and only one was
 * cosmetic. Its container is a tonal surface, not a plate — no offset shadow,
 * no lit bevel, no moulding edge — so it landed on the tray as a flat rectangle
 * of colour. And its buttons are `TextButton`s: bare text, focusable, with the
 * Material focus veil turned off across the app. On a pad the cursor went into
 * a dialog and disappeared, which on a screen whose whole job is to ask a
 * question leaves nothing to answer it with.
 *
 * The actions are laid out end-aligned, cancel first: the destructive answer is
 * never the one under the thumb when the dialog opens.
 */
@Composable
fun PadDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    /** Dismissable by tapping outside, except where the choice has to be made. */
    dismissOnOutside: Boolean = true,
    /**
     * La consequence, en une phrase, pour le panneau arriere.
     *
     * Le corps de ce dialogue est un `@Composable`, donc illisible d'ici : la
     * phrase se redonne, elle ne se recupere pas. C'est le prix a payer pour que
     * le panneau cesse de contredire l'ecran de face, et il est modique — un
     * dialogue qui n'a rien a dire de plus que son titre laisse ce parametre
     * nul et le panneau n'affiche que le titre.
     */
    panelDetail: String? = null,
    /** Corail quand la question porte sur une session ou sur quelqu'un. */
    panelSocial: Boolean = false,
    actions: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    // **Le panneau apprend qu'on demande quelque chose devant.**
    //
    // Pose ici et pas chez les appelants : c'est le seul dialogue de
    // confirmation de l'app, donc le seul endroit ou brancher la regle, et un
    // futur dialogue en heritera sans que personne ait a y penser. Le panneau
    // affichait jusqu'ici la scene d'avant — le code d'une session pendant
    // qu'on demandait s'il fallait la fermer.
    // pourquoi : docs/decisions/second-ecran.md § Ce qui voyage jusqu'au panneau
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
            // The dialog measures itself, so a long body wraps instead of being
            // squeezed into the platform's default width.
            usePlatformDefaultWidth = false
        )
    ) {
        SilenceSystemSfx()
        // Bounded, and that bound is the whole reason this measures itself.
        //
        // `usePlatformDefaultWidth = false` is what lets a long sentence wrap
        // instead of being squeezed into the platform's narrow column. Left
        // unbounded it goes the other way on the Thor, whose screen is 1920 wide
        // in landscape: the panel ran edge to edge and read as a bar across the
        // tray, which is the one silhouette this world does not have.
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

/** The body of a dialog that is one sentence to read, which is most of them. */
@Composable
fun PadDialogText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
