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
 * A text field that waits to be asked before opening: **the field is not a step
 * in the traversal, its frame is**. A field taking focus opens the keyboard, so
 * merely passing over one used to swallow the screen.
 *
 * `canFocus` is denied while not editing — non-clickable is not enough.
 * pourquoi : docs/decisions/coquille-ecrans.md § Un champ de texte ne doit pas être un arrêt du curseur
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

    /** True while the frame holds the cursor, which is when the ring is drawn. */
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

    // Leaving the field returns focus to the frame. Without that, closing the
    // keyboard left focus in a field that had become non-focusable, hence
    // nowhere: the directions stopped responding and the screen had to be
    // touched.
    BackHandler(enabled = editing) {
        editing = false
        runCatching { frame.requestFocus() }
    }

    // The keyboard's disappearance ends the edit, not the key: the keyboard
    // swallows the first B. [opened] covers the instant before it is visible.
    // pourquoi : docs/decisions/coquille-ecrans.md § C'est la disparition du clavier qui termine l'édition, pas la touche
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
        // **L'etiquette est posee au-dessus du cadre, pas dedans.**
        //
        // `OutlinedTextField` reserve en haut la place ou son etiquette ira
        // flotter, meme quand elle est encore au repos : le texte se retrouve
        // assis nettement sous le milieu, avec beaucoup d'air au-dessus et peu
        // en dessous. Dans un cadre dont l'anneau *est* le contour, cette
        // asymetrie se lit comme un anneau mal dimensionne.
        //
        // Et la reserve ne servait a rien ici : une etiquette qui flotte se
        // pose dans l'encoche du contour de Material, contour que ce champ
        // efface au profit de l'anneau. Elle serait donc allee flotter sur
        // l'anneau lui-meme des qu'on aurait tape un caractere.
        //
        // Au-dessus, elle reste lisible en permanence — y compris une fois le
        // champ rempli, ou la version flottante disparaissait dans le trait.
        // pourquoi : docs/decisions/coquille-ecrans.md § L'anneau *est* le contour du champ, et c'est le seul arrangement qui tienne
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
                // The ring *is* the field's outline here: one outline at a
                // time, so there is nothing left to align. Placed BEFORE the
                // `focusable`, or it never sees the frame's focus.
                // pourquoi : docs/decisions/coquille-ecrans.md § L'anneau *est* le contour du champ, et c'est le seul arrangement qui tienne
                .controlRing(shape, enabled = !editing)
                // **Opaque, sinon la lueur passe au travers.**
                //
                // La lueur du curseur est une ombre, et une ombre traverse tout
                // ce qui n'est pas opaque. Le cadre de ce champ ne peignait
                // rien — il laissait voir la carte derriere — donc pendant les
                // 140 ms ou l'elevation monte, l'ombre se voyait *dedans* : un
                // halo qui se remplit puis se vide au milieu du champ.
                //
                // Le remplissage est la tranche exacte du degrade que la carte
                // peignait deja ici, donc rien ne change a l'oeil. Pose apres
                // l'anneau : dans une chaine de modificateurs, l'ombre dessine
                // en premier, ce fond par-dessus, et le trait de l'anneau
                // par-dessus encore.
                // pourquoi : docs/decisions/reglages-ecran.md § Le remplissage opaque existe pour le curseur, pas pour le look
                .cardSliceFill(shape)
                .focusRequester(frame)
                .focusable(interactionSource = interaction)
                .onKeyEvent { event ->
                    if (editing) return@onKeyEvent false
                    if (event.key in CONFIRM_KEYS) {
                        // Opened on release, as everywhere else in the app; the
                        // key-down is swallowed so one press does not count
                        // twice.
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
                // Nulle : voir le commentaire de l'etiquette, plus haut.
                label = null,
                placeholder = placeholder?.let { { Text(it) } },
                isError = isError,
                singleLine = singleLine,
                shape = shape,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                // Transparent exactly when the ring is drawn, so the two never
                // show at once. Editing puts the cursor inside the field, the
                // ring goes out, and Material's own outline comes back to say
                // where the caret is.
                //
                // The caret speaks the zone's axis: teal by default, coral
                // wherever the field feeds the social domain (join codes,
                // session names) — the caller wraps itself in
                // `LocalRingTone provides CORAL` and the field follows.
                // pourquoi : docs/decisions/theme-duotone-shelves.md § Session / Join — domaine corail
                colors = OutlinedTextFieldDefaults.colors(
                    cursorColor = ringColor(),
                    focusedBorderColor = ringColor(),
                    unfocusedBorderColor =
                        if (framed) Color.Transparent
                        else MaterialTheme.colorScheme.outline,
                    disabledBorderColor =
                        if (framed) Color.Transparent
                        else MaterialTheme.colorScheme.outline,
                    // **Le contour d'erreur s'efface comme les autres.**
                    //
                    // Il manquait, et Material le fait passer devant les trois
                    // autres : un champ en erreur gardait donc son trait rouge
                    // sous l'anneau, deux contours de tailles differentes l'un
                    // dans l'autre. Ca se voyait a chaque ouverture du profil,
                    // ou le pseudo est vide donc en erreur des l'arrivee.
                    //
                    // `framed` est faux pendant l'edition — le curseur est alors
                    // dans le champ, pas sur son cadre — donc le rouge revient
                    // exactement quand l'anneau s'eteint, ce qui est la regle
                    // que les trois autres suivaient deja.
                    // pourquoi : docs/decisions/coquille-ecrans.md § L'anneau *est* le contour du champ, et c'est le seul arrangement qui tienne
                    errorBorderColor =
                        if (framed) Color.Transparent
                        else MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(field)
                    .focusProperties { canFocus = editing }
                    // The field can also lose focus other than through B, the
                    // keyboard closed with the system gesture, for instance. We
                    // then go back to frame mode, otherwise the ring never
                    // returns and the screen looks frozen.
                    .onFocusChanged { if (editing && !it.isFocused) editing = false }
            )

            // Drawn after the field, hence touched before it: Compose tests
            // children first, and the field consumed taps it then refused.
            // pourquoi : docs/decisions/coquille-ecrans.md § Le doigt n'atteignait pas le cadre
            if (!editing) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) { detectTapGestures { editing = true } }
                )
            }
        }
        // Outside the frame, and therefore outside the ring: the helper text is
        // not the control being aimed at. It keeps the place and the style
        // `OutlinedTextField` gave it.
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

/** The field's radius. The ring needs it to trace a parallel outline. */
private val FIELD_CORNER = 16.dp

