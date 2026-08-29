package eu.emufii.app.ui.screens

import eu.emufii.app.ui.sounded
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.emufii.app.R
import kotlinx.coroutines.delay
import eu.emufii.app.ui.components.SoftCard
import eu.emufii.app.ui.components.waitTrim
import eu.emufii.app.ui.wallpaper.TrayBackdrop
import eu.emufii.app.ui.theme.CardShape
import eu.emufii.app.ui.theme.Coral
import androidx.compose.ui.graphics.Color
import eu.emufii.app.ui.controlRing
import eu.emufii.app.ui.components.padEntry
import eu.emufii.app.ui.theme.PillShape
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.Teal


@Composable
fun PreparingScreen(
    label: String,
    /**
     * Ce que fait « Laisser tomber ». Nul quand l'appelant n'a pas de sortie
     * sure a offrir, auquel cas rien ne s'affiche : un bouton qui ne repond pas
     * est pire que pas de bouton.
     */
    onGiveUp: (() -> Unit)? = null
) {
    // Sits on the same backdrop as everywhere else. On a plain surface this
    // screen read as a different app for the ten seconds it's up.
    TrayBackdrop(modifier = Modifier.fillMaxSize(), dark = LocalEmufiiDarkTheme.current)

    // Faded in rather than snapped on.
    //
    // The launch card now carries the first leg of the flow with a spinner of
    // its own, so this screen is only ever reached for the tunnel, and when the
    // tunnel happens to be up already, it comes and goes in a few frames. Cut
    // hard, that read as a glitch between the card and the session.
    //
    // An earlier attempt held the content back for 400 ms instead, which just
    // traded a flashing spinner for a blank backdrop, worse, because a bare
    // wallpaper says nothing at all. Fading from the first frame keeps the
    // screen honest when the wait is real and makes it a soft pulse when it is
    // not.
    var shown by remember(label) { mutableStateOf(false) }
    LaunchedEffect(label) { shown = true }

    /**
     * Passe le seuil, l'attente n'est plus « la premiere fois, c'est long » :
     * elle est suspecte. Vingt secondes, soit largement au-dela de ce qu'un
     * tunnel froid demande, et bien en deca des quarante-cinq du delai de
     * garde — le joueur reprend la main avant que le code ne renonce pour lui.
     */
    var overdue by remember(label) { mutableStateOf(false) }
    LaunchedEffect(label) {
        delay(OVERDUE_MS)
        overdue = true
    }
    val appearance by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "preparing-appearance"
    )

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp).alpha(appearance),
        contentAlignment = Alignment.Center
    ) {
        // On a plate, not floating on the tray.
        //
        // The two lines and the spinner sat straight on the wallpaper, which
        // made this the one screen in the app whose content was not an object:
        // the engraving of the tray ran right behind the text, and the whole
        // thing read as a system overlay rather than as a room of Emufii. A
        // panel gives the wait somewhere to happen.
        SoftCard(modifier = Modifier.widthIn(max = 360.dp).waitTrim()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // **Plus gros et plus epais, parce qu'on le fixe.**
                //
                // Le temoin par defaut de Material fait 40 dp sur un trait de
                // 4 : une taille reglee pour un rond qui passe dans un coin de
                // liste pendant une demi-seconde. Ici c'est le seul objet en
                // mouvement d'un ecran qu'on regarde dix secondes sans rien
                // pouvoir faire, et a cette taille il se lisait comme un detail
                // plutot que comme la reponse a « est-ce que ca avance ? ».
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 5.dp,
                    modifier = Modifier.size(54.dp)
                )
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                // **Le sous-titre change quand l'attente devient anormale.**
                //
                // « C'est un peu long la premiere fois » est vrai a la dixieme
                // seconde et devient un mensonge a la trentieme : le joueur bloque
                // par un tunnel qui ne montera jamais lisait un texte qui lui
                // affirmait que tout allait bien. Passe le seuil, l'ecran cesse
                // de rassurer et dit ce qu'il en est.
                Text(
                    stringResource(
                        if (overdue) R.string.prep_taking_long else R.string.prep_first_time
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                // La sortie n'apparait qu'avec le doute. Offerte des la premiere
                // seconde, elle inviterait a renoncer a une attente parfaitement
                // normale ; offerte jamais, elle laisse la touche Home comme
                // seule issue, ce qui etait le cas.
                if (onGiveUp != null && overdue) {
                    Button(
                        onClick = sounded(onGiveUp),
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.controlRing(PillShape).padEntry()
                    ) { Text(stringResource(R.string.prep_give_up)) }
                }
            }
        }
    }
}

/** Au-dela, l'ecran cesse de rassurer et propose de renoncer. */
private const val OVERDUE_MS = 20_000L
