package eu.emufii.app.ui.screens

import eu.emufii.app.ui.sounded
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.emufii.app.ui.controlRing
import eu.emufii.app.R
import eu.emufii.app.network.CoordinatorClient
import eu.emufii.app.session.RomRef
import eu.emufii.app.session.SessionCodes
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import eu.emufii.app.ui.components.EmufiiCodeKeyboard
import eu.emufii.app.ui.components.EmufiiScaffold
import eu.emufii.app.ui.components.LandOn
import eu.emufii.app.ui.components.padEntry
import eu.emufii.app.secondscreen.LEGEND_CAP
import eu.emufii.app.secondscreen.PadHint
import eu.emufii.app.secondscreen.PadHintRow
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.socket
import eu.emufii.app.ui.focusRing
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import eu.emufii.app.ui.theme.PillShape
import eu.emufii.app.ui.theme.Coral
import eu.emufii.app.ui.LocalRingTone
import eu.emufii.app.ui.RingTone
import eu.emufii.app.ui.ringColor

/** A session code is six characters; the hyphen only helps to read it. */
private const val CODE_LENGTH = 6

/** La coupe corail lisible sur le fond courant, pour les etats affaiblis. */
private fun coralCut(dark: Boolean) = if (dark) Coral.darkBright else Coral.deep

/**
 * Entering the code you were given, as six boxes rather than a form.
 *
 * La dalle de l'app remplace le champ invisible qui attendait un clavier systeme
 * qui ne s'ouvrait jamais a la manette. En deux colonnes : a gauche ce qu'on
 * lit, a droite ce avec quoi on ecrit.
 * pourquoi : docs/decisions/coquille-ecrans.md § Rejoindre : le clavier de l'app plutôt qu'un champ invisible
 * pourquoi : docs/decisions/coquille-ecrans.md § Six cases plutôt qu'un champ
 */
@Composable
fun JoinScreen(
    rom: RomRef,
    client: CoordinatorClient,
    onBack: () -> Unit,
    onSubmitCode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var code by remember { mutableStateOf("") }
    val complete = code.length == CODE_LENGTH
    val dark = LocalEmufiiDarkTheme.current
    val landing = remember { FocusRequester() }

    // **B efface une case, il ne quitte pas l'ecran tant qu'il reste du code.**
    // La dalle n'a plus de touche d'effacement : elle tordait la grille pour
    // loger une cible que la manette avait deja sous le pouce. Declare avant le
    // scaffold pour passer devant le sien, qui lui sort de l'ecran — ce que B
    // fait de nouveau des que le champ est vide.
    // pourquoi : docs/decisions/coquille-ecrans.md § Le clavier de code n'est pas le clavier de recherche
    BackHandler(enabled = code.isNotEmpty()) { code = code.dropLast(1) }

    // Le domaine social : le curseur manette y devient corail.
    // pourquoi : docs/decisions/theme-duotone-shelves.md § FOCUS MANETTE
    CompositionLocalProvider(LocalRingTone provides RingTone.CORAL) {
    EmufiiScaffold(
        title = stringResource(R.string.join_title),
        modifier = modifier,
        onBack = onBack,
        contentScrolls = false,
        // Le scaffold ne pose pas son curseur : il le poserait sur le premier
        // controle de l'ecran, qui est le bouton « Rejoindre », et celui-ci est
        // desactive tant que le code n'est pas complet. On le pose nous-memes
        // sur le clavier, qui est ce qu'il y a a faire en arrivant.
        autoFocus = false
    ) { _ ->
        LandOn(landing)

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    rom.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    // Laid on the background, outside any Surface: with no
                    // explicit colour it falls back to black and disappears in
                    // the dark theme.
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                // **Le gabarit passe au-dessus des cases.**
                //
                // « ex. KMR-347 » etait sous les encoches et plus discret
                // qu'elles : il se lisait apres, donc trop tard, et rien ne
                // reliait ses caracteres aux cases. Pose juste au-dessus, il
                // devient un modele — trois lettres, un tiret, trois chiffres —
                // et le tiret imprime dans la bande d'encoches y repond.
                Text(
                    stringResource(R.string.join_code_example),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(CODE_LENGTH) { i ->
                        CodeSlot(
                            char = code.getOrNull(i),
                            // The current box, and the last one once the
                            // code is complete: the accent has to land
                            // somewhere.
                            active = i == code.length.coerceAtMost(CODE_LENGTH - 1)
                        )
                        // The dash is a reading aid, in the middle, and
                        // not a character to type.
                        if (i == 2) Separator()
                    }
                }

                // **Ce que fait retour, dit la ou il sert**, et dans la langue
                // que le panneau arriere parle deja : un capuchon moule et un
                // mot. Le clavier de code n'a plus de touche d'effacement — sans
                // cette ligne, un joueur qui se trompe d'une lettre n'a aucun
                // moyen de savoir qu'il peut la reprendre.
                //
                // La bande garde sa hauteur meme vide : elle apparait au premier
                // caractere, et c'est exactement quand retour efface au lieu de
                // sortir. Sans la reserve, la bande d'encoches sautait d'un cran
                // a la premiere lettre tapee.
                Box(modifier = Modifier.height(LEGEND_CAP)) {
                    if (code.isNotEmpty()) PadHintRow(PadHint.ERASE)
                }

                Button(
                    onClick = sounded { onSubmitCode(SessionCodes.normalize(code)) },
                    enabled = complete,
                    shape = PillShape,
                    // Rejoindre est un lien : pilule corail, coupe pleine sur
                    // fond clair, coupe bright sur fond sombre. Desactivee, la
                    // teinte reste lisible — le controle attend ses six caracteres,
                    // il n'est pas absent.
                    // pourquoi : docs/decisions/theme-duotone-shelves.md § Deux axes sémantiques
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (dark) Coral.bright else Coral.deep,
                        contentColor = if (dark) Coral.ink else Color.White,
                        disabledContainerColor = coralCut(dark).copy(alpha = 0.16f),
                        disabledContentColor = coralCut(dark).copy(alpha = 0.55f)
                    ),
                    modifier = Modifier.width(240.dp).height(50.dp).controlRing(PillShape).padEntry()
                ) {
                    // **Le bouton desactive dit ce qui manque.**
                    //
                    // « Rejoindre » en grise ne disait pas pourquoi il ne
                    // marchait pas ; le joueur pouvait croire que le code etait
                    // refuse alors qu'il etait seulement incomplet. Il compte
                    // donc a voix haute tant qu'il manque quelque chose, et ne
                    // reprend son nom qu'au moment ou il devient appuyable —
                    // c'est-a-dire qu'il n'annonce jamais une action qu'il ne
                    // peut pas rendre.
                    Text(
                        if (complete) stringResource(R.string.join_action)
                        else pluralStringResource(
                            R.plurals.join_code_remaining,
                            CODE_LENGTH - code.length,
                            CODE_LENGTH - code.length
                        )
                    )
                }
            }

            // Le clavier, a droite. Il porte le curseur en arrivant : c'est le
            // seul endroit de l'ecran ou il y ait quelque chose a faire tant que
            // le code n'est pas complet.
            //
            // **Rien dessous.** Il a eu une plaque, puis un plateau creuse : les
            // deux ajoutaient une surface entre le clavier et le fond, et les
            // touches se lisaient comme des objets ranges dans une boite. Un
            // clavier de console est pose a meme l'ecran ; sa forme lui vient de
            // ses touches, pas d'un cadre autour.
            Box(modifier = Modifier.weight(1.05f)) {
                EmufiiCodeKeyboard(
                    firstKeyFocus = landing,
                    onKey = { c -> if (code.length < CODE_LENGTH) code += c },
                    maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.70f
                )
            }
        }
    }
    }
}

/**
 * One socket in the code strip: a recess rather than a plate, since a code is
 * typed *into* something. The lit one wears the cursor's own ring.
 * pourquoi : docs/decisions/coquille-ecrans.md § Six cases plutôt qu'un champ
 */
@Composable
private fun CodeSlot(char: Char?, active: Boolean) {
    val dark = LocalEmufiiDarkTheme.current
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            // **Retaille pour la colonne de gauche, pas pour l'ecran entier.**
            //
            // A 56 dp de large, six encoches, leur tiret et leurs ecarts
            // demandaient plus que la moitie de l'ecran : la bande depassait sa
            // colonne et la sixieme case sortait par la droite. Le code se lit
            // toujours a bout de bras a 48, et c'est le nombre de caracteres
            // qui fait la largeur, pas l'inverse.
            .size(width = 48.dp, height = 66.dp)
            .focusRing(active, shape, width = 3.dp, glowRadius = 16.dp)
            // Une encoche crème plate : la teinte basse de la plaque suffit à
            // dire « ici va un caractère », le creux en relief est un monde
            // disparu. L'encoche active se teinte de l'axe social.
            .socket(shape, dark)
            .then(
                if (active) Modifier.background(Coral.soft, shape) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (char != null) {
            Text(
                char.toString(),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else if (active) {
            // A caret, and a thin one. The empty active slot used to paint a
            // pale block the size of a glyph, which read as a character already
            // typed: the player's own code looked half entered before they had
            // touched a key.
            Caret()
        }
    }
}

/** The blink of a text cursor: a bar, on and off, nothing else moving. */
@Composable
private fun Caret() {
    val blink = rememberInfiniteTransition(label = "caret")
    val alpha by blink.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "caret-blink"
    )
    Box(
        modifier = Modifier
            .size(width = 3.dp, height = 34.dp)
            .background(
                // Le caret dit l'axe social : corail dans le domaine corail.
                ringColor().copy(alpha = alpha),
                RoundedCornerShape(2.dp)
            )
    )
}

@Composable
private fun Separator() {
    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .size(width = 12.dp, height = 2.dp)
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
    )
}
