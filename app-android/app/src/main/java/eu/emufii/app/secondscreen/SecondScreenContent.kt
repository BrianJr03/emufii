package eu.emufii.app.secondscreen

import eu.emufii.app.ui.sounded
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.Image
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import eu.emufii.app.R
import eu.emufii.app.artwork.rememberTileArt
import eu.emufii.app.library.Console
import eu.emufii.app.BuildConfig
import eu.emufii.app.meta.GameMeta
import eu.emufii.app.session.Session
import eu.emufii.app.ui.components.CompatBadge
import eu.emufii.app.ui.components.compatLabel
import eu.emufii.app.ui.components.VpsLamp
import eu.emufii.app.ui.theme.ArtworkShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import eu.emufii.app.ui.theme.GoodLight
import eu.emufii.app.ui.theme.GoodDark
import eu.emufii.app.secondscreen.PanelFriend
import eu.emufii.app.ui.components.Avatar
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import eu.emufii.app.ui.components.CrossIcon
import eu.emufii.app.ui.components.GhostButton
import eu.emufii.app.ui.theme.ErrorLight
import eu.emufii.app.ui.theme.ErrorDark
import eu.emufii.app.ui.theme.InkText
import eu.emufii.app.ui.ActionShape
import eu.emufii.app.ui.focusRing
import eu.emufii.app.ui.theme.CardShape
import eu.emufii.app.ui.theme.LocalAccent
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.LocalEmufiiOledTheme
import eu.emufii.app.ui.theme.PillShape
import eu.emufii.app.ui.theme.TileShape
import eu.emufii.app.ui.theme.moldedRim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import eu.emufii.app.ui.theme.tilePlateBrush
import eu.emufii.app.ui.theme.plate
import eu.emufii.app.ui.theme.socket
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import eu.emufii.app.ui.wallpaper.TrayBackdrop
import eu.emufii.app.ui.components.InfoMark
import eu.emufii.app.ui.components.SlidersMark
import eu.emufii.app.ui.components.PaintMark
import eu.emufii.app.ui.components.ChipMark
import eu.emufii.app.ui.components.GridMark
import eu.emufii.app.ui.components.LensMark
import eu.emufii.app.ui.components.SignalMark
import eu.emufii.app.ui.components.ShelfMark
import eu.emufii.app.ui.components.PersonMark
import eu.emufii.app.ui.theme.Teal
import eu.emufii.app.ui.theme.Coral
import eu.emufii.app.ui.tap
import eu.emufii.app.ui.SilenceSystemSfx

/**
 * What the second panel draws, whoever is holding the window.
 *
 * pourquoi : docs/decisions/second-ecran.md § Le panneau n'a pas de style à lui
 */
@Composable
fun SecondScreenContent(model: SecondScreenModel) {
    SilenceSystemSfx()
    val dark = LocalEmufiiDarkTheme.current
    val page by SecondScreen.page.collectAsState()

    // Deux ecoutes, une par ecran : le focus clavier va a une fenetre, pas a
    // l'appareil, et le panneau est tactile depuis qu'il porte les etapes.
    // pourquoi : docs/decisions/second-ecran.md § R tourne la page depuis les deux écrans
    val keys = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { keys.requestFocus() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .focusRequester(keys)
            // Focalisable sans etre focalisable a la croix : ce n'est pas une
            // destination de curseur, c'est une oreille. Rien ici ne se
            // selectionne.
            .focusProperties { canFocus = true }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.ButtonR1) {
                    SecondScreen.flipPage()
                    true
                } else {
                    false
                }
            }
    ) {
        // This window has no wallpaper behind it, so the tray is painted rather
        // than shown through.
        TrayBackdrop(modifier = Modifier.fillMaxSize(), dark = dark)

        // Toutes les faces se centrent au meme endroit : un fondu croise ne
        // tolere pas deux geometries.
        // pourquoi : docs/decisions/second-ecran.md § Toutes les faces se centrent au même endroit
        Column(modifier = Modifier.fillMaxSize()) {
            // The panel reports on the app as well as on the game, and both
            // bands are permanent: the eye learns where a thing appears once
            // and stops searching for it.
            PanelHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 26.dp, end = 26.dp, top = 18.dp)
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)

                    .padding(start = 36.dp, end = 36.dp, top = 10.dp)
            ) {
                // Cle sur l'identite du jeu, pas sur le modele : les faits
                // tardifs se remplissent sans dissoudre une face en elle-meme.
                // Et `fillMaxSize`, sinon la face qui part se recentre dans une
                // boite qui n'est plus la sienne.
                // pourquoi : docs/decisions/second-ecran.md § Le fondu entre deux faces n'est pas une décoration
                // pourquoi : docs/decisions/second-ecran.md § Toutes les faces se centrent au même endroit
                Crossfade(
                    targetState = faceKey(model),
                    animationSpec = tween(220),
                    label = "panel-face",
                    modifier = Modifier.fillMaxSize()
                ) { key ->
                    // The model is read here rather than captured with the key:
                    // during a fade the outgoing face is still composed, and it
                    // must not redraw itself with the incoming face's content.
                    val shown = remember(key) { model }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (shown) {
                            is SecondScreenModel.Idle -> Idle()
                            // Live, not frozen: every console shares one key, so
                            // the remembered value would never change its text.
                            // pourquoi : docs/decisions/second-ecran.md § La console se lit en direct, les autres faces sont gelées
                            is SecondScreenModel.ConsoleFolder -> ConsoleCard(
                                (model as? SecondScreenModel.ConsoleFolder)?.console
                                    ?: shown.console
                            )
                            is SecondScreenModel.Browsing -> BrowsingPages(shown, page)
                            // En direct, comme la fiche console : toutes les
                            // entrees partagent une cle de face, donc la valeur
                            // gelee resterait sur la premiere visee.
                            // pourquoi : docs/decisions/second-ecran.md § La console se lit en direct, les autres faces sont gelées
                            is SecondScreenModel.SettingsEntry -> SettingsFace(
                                (model as? SecondScreenModel.SettingsEntry) ?: shown
                            )
                            is SecondScreenModel.Friends -> FriendsFace(
                                // En direct, comme la fiche console : la liste
                                // change pendant qu'on la regarde — quelqu'un se
                                // connecte, quelqu'un lance un jeu — et la valeur
                                // gelee sur la cle de face resterait sur l'etat du
                                // moment de l'ouverture.
                                // pourquoi : docs/decisions/second-ecran.md § La console se lit en direct, les autres faces sont gelées
                                (model as? SecondScreenModel.Friends) ?: shown
                            )
                            is SecondScreenModel.Asking -> AskingFace(
                                // En direct : la question peut changer sans que
                                // la couche modale se ferme — l'interrupteur de
                                // session privee reecrit sa propre consequence.
                                (model as? SecondScreenModel.Asking) ?: shown
                            )
                            is SecondScreenModel.InSession -> InSession(shown)
                        }
                    }
                }
            }

            Legend(
                legend = model.legend,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 16.dp)
            )
        }
    }
}

/**
 * What counts as a different face, for the fade: identity, not content.
 * pourquoi : docs/decisions/second-ecran.md § Le fondu entre deux faces n'est pas une décoration
 */
private fun faceKey(model: SecondScreenModel): String = when (model) {
    is SecondScreenModel.Idle -> "idle"
    // Every console shares one key: the card must not be replaced when the
    // cursor moves from one folder to the next, because it animates that change
    // itself — see [ConsoleCard]. The outer fade is for changing *face*.
    is SecondScreenModel.ConsoleFolder -> "console"
    // Une seule cle pour toutes les entrees : la face ne doit pas etre
    // remplacee quand le curseur passe d'une case a la voisine — c'est le
    // meme objet qui change de contenu, pas une autre face.
    is SecondScreenModel.SettingsEntry -> "settings"
    is SecondScreenModel.Browsing -> "rom:${model.rom.uri}"
    is SecondScreenModel.Friends -> "friends"
    // Une seule cle : le contenu de la question change en place (l'interrupteur
    // de session privee reecrit sa consequence), et remplacer la face a chaque
    // mot ferait clignoter le panneau.
    is SecondScreenModel.Asking -> "asking"
    is SecondScreenModel.InSession -> "session:${model.code}"
}

/**
 * The band across the top: are we reachable, and is there any news.
 * pourquoi : docs/decisions/second-ecran.md § La lumière de service a sa propre couleur
 */
@Composable
private fun PanelHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        VpsLamp()
        NoteStrip(modifier = Modifier.weight(1f))
    }
}

/**
 * Where the news comes out. It leaves on its own: nothing here can be
 * dismissed, so anything needing acknowledgement would stay forever.
 * pourquoi : docs/decisions/second-ecran.md § Les nouvelles arrivent d'en haut et repartent seules
 */
@Composable
private fun NoteStrip(modifier: Modifier = Modifier) {
    val note by PanelFeed.note.collectAsState()
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current

    // Retired by the note's own id, so the friend who came online a second
    // later is not swept away with the one before them.
    LaunchedEffect(note?.id) {
        val shown = note ?: return@LaunchedEffect
        delay(NOTE_LIFETIME_MS)
        PanelFeed.dismiss(shown.id)
    }

    AnimatedContent(
        targetState = note,
        transitionSpec = {
            (slideInVertically(tween(260)) { -it } + fadeIn(tween(260)))
                .togetherWith(fadeOut(tween(200)))
        },
        label = "panel-note",
        modifier = modifier
    ) { shown ->
        if (shown == null) {
            // Nothing to say takes no room and leaves no empty plate: a strip
            // sitting there greyed out reads as a thing that is broken.
            Box(Modifier.fillMaxWidth().height(1.dp))
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .plate(CardShape, dark = dark, oled = oled, lift = 4.dp)
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Text(
                    shown.text,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private const val NOTE_LIFETIME_MS = 12_000L

@Composable
private fun Idle() {
    val dark = LocalEmufiiDarkTheme.current
    val axis = if (dark) Teal.darkBright else Teal.deep

    // Une marque, pas un vide avec un numero dedans : cette face parait
    // plusieurs fois par minute depuis que le curseur monte dans l'en-tete.
    // pourquoi : docs/decisions/second-ecran.md § La marque au repos est une marque, pas un vide avec un numéro dedans
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Le vrai logo, pas une citation dessinee a la main : une marque
        // approchee posee a cote de la vraie se lit comme un logo rate.
        Image(
            painter = painterResource(R.drawable.emufii_logo_v3),
            contentDescription = null,
            modifier = Modifier.size(108.dp)
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                // Dimmed on purpose: at rest this panel is the least interesting
                // thing in the room, not a second logo competing with the front
                // screen.
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // A step smaller and dimmer than the name: a footnote to it.
            // pourquoi : docs/decisions/second-ecran.md § La version s'affiche sur la face au repos
            Text(
                stringResource(R.string.panel_idle_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * The cursor is on a console's folder: what playing together means on that
 * machine. The machine's name leads, then two lines, then a warning if it has
 * one.
 * pourquoi : docs/decisions/second-ecran.md § La fiche console : ce qu'elle dit, et ce qu'elle ne dit pas
 */
@Composable
private fun ConsoleCard(console: Console) {
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current

    // One plate that stays and is resized, never replaced. Centred, so it
    // opens from its middle in both directions.
    // pourquoi : docs/decisions/second-ecran.md § La fiche console est une plaque qui grandit, pas une plaque qu'on remplace
    AnimatedContent(
        targetState = console,
        transitionSpec = {
            (fadeIn(tween(200, delayMillis = 80)) togetherWith fadeOut(tween(140)))
                // The frame takes longer than the text on purpose: the words are
                // gone before the plate has finished travelling, so nothing is
                // ever read while it moves.
                .using(SizeTransform(clip = false) { _, _ -> tween(280) })
        },
        label = "console-card",
        modifier = Modifier
            .fillMaxWidth(0.86f)
            .plate(CardShape, dark = dark, oled = oled, lift = 8.dp)
    ) { shown ->
        val brief = remember(shown) { consoleBrief(shown) }
        Column(
            verticalArrangement = Arrangement.spacedBy(15.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 34.dp, vertical = 26.dp)
        ) {
            Text(
                stringResource(R.string.brief_console_title, shown.label),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(brief.first),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(brief.second),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            brief.warning?.let { warning ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // A bar, never a warning triangle: this panel does not shout.
                    // pourquoi : docs/decisions/second-ecran.md § Le panneau ne crie pas
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(34.dp)
                            .clip(PillShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        stringResource(warning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Le curseur est sur une case du hub des reglages : la meme chose, en grand.
 *
 * Le panneau complete, il ne prend rien a personne. Un fondu croise et pas un
 * glissement : le hub est un tableau, pas une suite ordonnee.
 * pourquoi : docs/decisions/second-ecran.md § La face du hub complète, elle ne prend rien
 */
@Composable
private fun SettingsFace(model: SecondScreenModel.SettingsEntry) {
    val dark = LocalEmufiiDarkTheme.current
    val ink = if (model.social) {
        if (dark) Coral.darkBright else Coral.deep
    } else {
        if (dark) Teal.darkBright else Teal.deep
    }
    val axis = if (model.social) Coral.bright else Teal.bright

    // Hauteur constante par construction : chaque texte a son compte de lignes
    // fige, sinon la marque remonte a chaque mouvement du curseur.
    // pourquoi : docs/decisions/second-ecran.md § Toutes les faces se centrent au même endroit
    Crossfade(
        targetState = model,
        animationSpec = tween(180),
        label = "settings-face"
    ) { shown ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(0.86f)
        ) {
            // La marque dans son encoche, comme sur la tuile de face — a la
            // taille du panneau. Une icone nue a cette echelle flotte.
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(axis.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) { PanelMarkGlyph(shown.mark, ink) }

            Text(
                shown.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                shown.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                minLines = SUMMARY_LINES,
                maxLines = SUMMARY_LINES,
                overflow = TextOverflow.Ellipsis
            )
            // Le chemin, en dernier et en petit : il situe, il ne titre pas.
            Text(
                shown.root + "  ›  " + shown.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


/**
 * Ce que le panneau montre pendant qu'une couche modale attend devant :
 * **il repete la question, il n'en pose pas une autre**. Un rond, un titre,
 * une ligne — la reponse se donne sur l'ecran de face.
 * pourquoi : docs/decisions/second-ecran.md § La face d'une question répète, elle n'en pose pas une autre
 */
@Composable
private fun AskingFace(model: SecondScreenModel.Asking) {
    val dark = LocalEmufiiDarkTheme.current
    val ink = if (model.social) {
        if (dark) Coral.darkBright else Coral.deep
    } else {
        if (dark) Teal.darkBright else Teal.deep
    }
    val axis = if (model.social) Coral.bright else Teal.bright

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxWidth(0.78f)
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(axis.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) { AskGlyph(ink) }

        Text(
            model.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            model.detail,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Le point d'interrogation, dessine plutot qu'ecrit : un caractere prendrait
 * la police du texte, son italique et ses metriques, dans une pastille ou les
 * trois se voient. Chaque symbole de cette app est trace par son fichier.
 */
@Composable
private fun AskGlyph(tint: Color) {
    Canvas(Modifier.size(30.dp)) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.11f
        val hook = Path().apply {
            moveTo(w * 0.30f, h * 0.32f)
            cubicTo(w * 0.30f, h * 0.10f, w * 0.76f, h * 0.10f, w * 0.72f, h * 0.34f)
            cubicTo(w * 0.69f, h * 0.52f, w * 0.50f, h * 0.50f, w * 0.50f, h * 0.68f)
        }
        drawPath(hook, tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
        drawCircle(tint, radius = stroke * 0.62f, center = Offset(w * 0.50f, h * 0.86f))
    }
}

/**
 * Combien de lignes le resume occupe, remplies ou non.
 *
 * Deux : c'est le plus long des sept resumes des reglages une fois mis a la
 * largeur du panneau. Une troisieme ne servirait a personne et creuserait un
 * trou sous les six autres.
 */
private const val SUMMARY_LINES = 2

/** La marque nommee par le modele, dessinee ici. Voir [PanelMark]. */
@Composable
private fun PanelMarkGlyph(mark: PanelMark, tint: Color) {
    val size = 38.dp
    when (mark) {
        PanelMark.PROFILE -> PersonMark(color = tint, size = size)
        PanelMark.LIBRARY -> ShelfMark(color = tint, size = size)
        PanelMark.CONSOLES -> GridMark(color = tint, size = size)
        PanelMark.EMULATORS -> ChipMark(color = tint, size = size)
        PanelMark.APPEARANCE -> PaintMark(color = tint, size = size)
        PanelMark.GENERAL -> SlidersMark(color = tint, size = size)
        PanelMark.ABOUT -> InfoMark(color = tint, size = size)
        PanelMark.SEARCH -> LensMark(color = tint, size = size)
        PanelMark.LAYOUT -> GridMark(color = tint, size = size)
        PanelMark.SORT -> SlidersMark(color = tint, size = size)
        PanelMark.SESSIONS -> SignalMark(color = tint, size = size)
        PanelMark.FRIENDS -> PersonMark(color = tint, size = size)
    }
}

/**
 * The game, on two pages, the second reached from the *front* screen.
 *
 * Sliding rather than cross-fading: the page arrives the way the player asked.
 * pourquoi : docs/decisions/second-ecran.md § La face de survol : deux pages, la seconde vraiment optionnelle
 */
@Composable
private fun BrowsingPages(model: SecondScreenModel.Browsing, page: Int) {
    AnimatedContent(
        targetState = page,
        transitionSpec = {
            val forward = targetState > initialState
            val enter = slideInVertically(tween(320)) { if (forward) it else -it } + fadeIn(tween(220))
            val exit = slideOutVertically(tween(320)) { if (forward) -it else it } + fadeOut(tween(220))
            enter togetherWith exit
        },
        label = "panel-page"
    ) { shown ->
        if (shown == 0) Browsing(model) else Details(model)
    }
}

/**
 * The game under the cursor: its box on the left, what we know of it on the right.
 * pourquoi : docs/decisions/second-ecran.md § La face de survol : deux pages, la seconde vraiment optionnelle
 */
@Composable
private fun Browsing(model: SecondScreenModel.Browsing) {
    val rom = model.rom
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(30.dp),
            modifier = Modifier.fillMaxWidth().align(Alignment.Center)
        ) {
            // The control sits under the thing it acts on, not mid-panel.
            // pourquoi : docs/decisions/second-ecran.md § Le contrôle appartient à ce sur quoi il agit
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.width(196.dp)
            ) {
                Cover(model, modifier = Modifier.fillMaxWidth())
                PageTurn(up = false, label = stringResource(R.string.panel_page_details))
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                ConsoleBadge(rom.console)
                Text(
                    rom.displayName,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                model.rating?.let { rating ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        CompatBadge(rating)
                        Text(
                            compatLabel(rating),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                // Absent halves are not printed: nothing here is guessed.
                // pourquoi : docs/decisions/second-ecran.md § La face de survol : deux pages, la seconde vraiment optionnelle
                DumpLine(model)
            }
        }
    }
}

/** Region, revision and genre, on one engraved line. Nothing when nothing is known. */
@Composable
private fun DumpLine(model: SecondScreenModel.Browsing) {
    val parts = listOfNotNull(
        model.tags.region,
        model.meta?.genreFor(panelLocale()),
    )
    if (parts.isEmpty()) return
    Text(
        parts.joinToString(" · "),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * The second page: what the game is, rather than which file it is. Everything
 * here is editorial and can be missing; nothing is claimed.
 * pourquoi : docs/decisions/second-ecran.md § La face de survol : deux pages, la seconde vraiment optionnelle
 */
@Composable
private fun Details(model: SecondScreenModel.Browsing) {
    val locale = panelLocale()
    val meta = model.meta

    // Laid out to fit, never to scroll; the paragraph yields its lines first.
    // pourquoi : docs/decisions/second-ecran.md § Rien ne défile, donc tout doit tenir
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            model.rom.displayName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val facts = listOfNotNull(
            meta?.genreFor(locale),
            // The year alone. A glance at a panel is answering "how old is
            // this", and `2016-01-21` makes the eye parse a date to get to a
            // number that was already there.
            meta?.released?.take(4)?.let { stringResource(R.string.panel_released, it) },
            model.tags.line(),
        )
        if (facts.isNotEmpty()) {
            // `FlowRow`, not `Row`: the French genre of a game can be twice the
            // length of its English one ("jeu de construction de paquet de
            // cartes roguelike"), and a plain row sent the date pill off the
            // panel instead of onto the line below.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                facts.forEach { fact ->
                    Text(
                        fact,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .socket(PillShape, LocalEmufiiDarkTheme.current)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // The pictures already on the card come first; the catalogue's links
        // only fill in for a game Cocoon has never seen.
        val local = rememberCocoonStills(model.rom)
        val stills = local.ifEmpty { meta?.screenshots.orEmpty() }
        val summary = meta?.summaryFor(locale)

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (summary != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        // Fewer lines when there are pictures to leave room for.
                        // A synopsis is a taste, not a manual read at arm's
                        // length under the player's hands.
                        maxLines = if (stills.isEmpty()) 9 else 5,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Whose words these are. The licence the text comes under
                    // asks for it, and the panel has room for four quiet
                    // characters.
                    meta.source?.let { source ->
                        Text(
                            source,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (stills.isEmpty() && (meta == null || meta.isEmpty(locale))) {
                // Said only when the page really has nothing — pictures count.
                // A page showing two stills and the sentence "nothing is known"
                // is the app arguing with itself in front of the player.
                Text(
                    stringResource(R.string.panel_details_unknown),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (stills.isNotEmpty()) Screenshots(stills)

        PageTurn(
            up = true,
            label = stringResource(R.string.panel_page_back),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

/**
 * The language this window is drawn in, read off *this* window's configuration
 * rather than the process default — `Locale.getDefault()` would be right by
 * accident.
 * pourquoi : docs/decisions/second-ecran.md § La langue vient de la fenêtre, pas du processus
 */
@Composable
private fun panelLocale(): java.util.Locale {
    val context = LocalContext.current
    return remember(context, context.resources.configuration) {
        androidx.core.os.ConfigurationCompat.getLocales(context.resources.configuration)
            .get(0) ?: java.util.Locale.getDefault()
    }
}

/**
 * The stills Cocoon has already downloaded for this exact file, if any.
 *
 * Off the main thread: the folder listing is a real provider query, and it
 * happens while a page is turning.
 */
@Composable
private fun rememberCocoonStills(rom: eu.emufii.app.library.Rom): List<Any> {
    val context = LocalContext.current
    val settings = remember(context) { eu.emufii.app.settings.SettingsStore.get(context) }
    val cocoon by settings.cocoonFolder.collectAsState()
    val stills = remember(rom.uri, cocoon) { mutableStateOf<List<Any>>(emptyList()) }
    LaunchedEffect(rom.uri, cocoon) {
        stills.value = withContext(Dispatchers.IO) {
            runCatching {
                eu.emufii.app.artwork.CocoonMedia.stillsFor(
                    context,
                    cocoon.takeIf { it.isNotBlank() }?.let(android.net.Uri::parse),
                    rom
                )
            }.getOrDefault(emptyList())
        }
    }
    return stills.value
}

/** A row of stills, each moulded into the tray like the cover is. */
@Composable
private fun Screenshots(urls: List<Any>) {
    val context = LocalContext.current
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        urls.take(3).forEach { url ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(16f / 9f)
                    .plate(TileShape, dark = dark, oled = oled, lift = 0.dp)
                    .padding(5.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(url).build(),
                    contentDescription = null,
                    // Fit, not crop: these are pictures of a screen, and a
                    // screen cropped loses exactly the words printed on it.
                    contentScale = ContentScale.Fit,
                    placeholder = ColorPainter(Color.Transparent),
                    // A still that will not load leaves the empty frame it was
                    // going to fill, which is the shape this tray already uses
                    // for nothing.
                    error = ColorPainter(Color.Transparent),
                    modifier = Modifier.fillMaxSize().clip(ArtworkShape)
                )
            }
        }
    }
}

/**
 * The way to the other page: an arrow on a cap, and the button that turns it.
 * pourquoi : docs/decisions/second-ecran.md § Le contrôle appartient à ce sur quoi il agit
 */
@Composable
private fun PageTurn(up: Boolean, label: String, modifier: Modifier = Modifier) {
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current
    val tint = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .plate(PillShape, dark = dark, oled = oled, lift = 3.dp)
        ) {
            ArrowGlyph(tint = MaterialTheme.colorScheme.onSurface, up = up)
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = tint
        )
    }
}

/**
 * The arrow, drawn rather than typed.
 * pourquoi : docs/decisions/second-ecran.md § La légende, et pourquoi les symboles sont dessinés
 */
@Composable
private fun ArrowGlyph(tint: Color, up: Boolean) {
    Canvas(Modifier.size(13.dp).rotate(if (up) 180f else 0f)) {
        val w = size.width
        val h = size.height
        val stem = w * 0.26f
        // A shaft and a head, the proportions of a moulded arrow on a shell:
        // a head about half the height, and wide enough to be seen before it is
        // recognised.
        drawRoundRect(
            color = tint,
            topLeft = Offset((w - stem) / 2f, 0f),
            size = Size(stem, h * 0.55f),
            cornerRadius = CornerRadius(stem / 2f, stem / 2f)
        )
        val head = Path().apply {
            moveTo(w * 0.12f, h * 0.48f)
            lineTo(w * 0.88f, h * 0.48f)
            lineTo(w / 2f, h)
            close()
        }
        drawPath(head, tint)
    }
}

/**
 * The box, moulded onto the tray, its shadow tinted with the colour the artwork
 * gave up (`Rom.accentArgb`). No extracted tone: the tray's own shadow, and
 * nothing else changes.
 * pourquoi : docs/decisions/second-ecran.md § La jaquette est moulée dans le plateau, et son ombre est de sa couleur
 */
/**
 * Les coins de la jaquette du panneau, en part de son cote et non en dp : elle
 * est la meme tuile que celles de la grille, dessinee 1,7 fois plus grand.
 * pourquoi : docs/decisions/second-ecran.md § Les coins de la jaquette suivent son échelle, pas sa mesure
 */
private val CoverShape = RoundedCornerShape(17)
private val CoverArtworkShape = RoundedCornerShape(15)

@Composable
private fun Cover(model: SecondScreenModel.Browsing, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current
    val art by rememberTileArt(model.rom)

    val tone = model.rom.accentArgb?.let { Color(it) }
    val shadow = tone ?: InkText

    Box(
        modifier = modifier
            .aspectRatio(1f)
            // The game's own tone, and it arrives as depth rather than as a
            // wash over the chrome: a real offset shadow, in the colour the
            // artwork gave up. A title with none casts the tray's own.
            .shadow(
                // On OLED the tray is truly off and a shadow draws nothing; the
                // edge and bevel below carry the separation alone.
                elevation = if (oled) 0.dp else 20.dp,
                shape = CoverShape,
                clip = false,
                ambientColor = shadow.copy(alpha = if (dark) 0.55f else 0.30f),
                spotColor = shadow.copy(alpha = if (dark) 0.75f else 0.42f)
            )
            // A plate with the picture inset, not a rim: measured, a rim
            // survives 0.38% of the cover's width here and vanishes.
            // pourquoi : docs/decisions/second-ecran.md § La jaquette est moulée dans le plateau, et son ombre est de sa couleur
            .plate(CoverShape, dark = dark, oled = oled, lift = 0.dp)
            .padding(9.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CoverArtworkShape)
                // White under the artwork in daylight, as the tiles do: box art
                // of any colour has to read against something, and the tray is
                // not it.
                .background(tilePlateBrush(dark, oled))
                // The picture keeps its own contour inside the frame, drawn over
                // the image the way a tile does it.
                .moldedRim(CoverArtworkShape, dark = dark, oled = oled)
        ) {
            val cover = art.model
            if (cover != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(cover).build(),
                    contentDescription = null,
                    contentScale = if (art.isPixelArt) ContentScale.Fit else ContentScale.Crop,
                    filterQuality = if (art.isPixelArt) FilterQuality.None else FilterQuality.High,
                    placeholder = ColorPainter(Color.Transparent),
                    error = ColorPainter(Color.Transparent),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // No art: an empty slot rather than a broken picture, which is
                // the shape this tray already uses for nothing.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().socket(CoverArtworkShape, dark)
                ) {
                    Text(
                        model.rom.console.shortLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** The machine, engraved into the tray: a label on the shell, not an object on it. */
@Composable
private fun ConsoleBadge(console: Console) {
    val dark = LocalEmufiiDarkTheme.current
    Text(
        console.label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .socket(PillShape, dark)
            .padding(horizontal = 13.dp, vertical = 5.dp)
    )
}

/**
 * A session is up, and the code is the whole point. It carries no label.
 * pourquoi : docs/decisions/second-ecran.md § Le code de session ne porte pas d'étiquette
 */
@Composable
private fun InSession(model: SecondScreenModel.InSession) {
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current
    val accent = LocalAccent.current
    val steps by SecondScreen.steps.collectAsState()

    // Une seule colonne : coupee en deux, chaque moitie tombait a 268 dp et le
    // port s'ecrivait un chiffre par ligne.
    // pourquoi : docs/decisions/second-ecran.md § Une seule colonne, et le code prend toute la largeur
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        // **Aucune compensation de hauteur.** Il y en avait une — 62 dp de creux
        // en bas — au motif que la legende de cette face est vide et que son
        // centre tombait donc plus bas que celui des autres. Ce n'est plus vrai :
        // la bande de legende garde sa hauteur meme vide, et la boite qui centre
        // les faces l'exclut deja. Les 62 dp remontaient donc la colonne de 31,
        // mesures a la capture du panneau : le contenu tenait dans les deux tiers
        // hauts et laissait un vide de la hauteur d'un bouton sous lui.
        // pourquoi : docs/decisions/second-ecran.md § Les deux bandes sont permanentes, la légende comprise
        modifier = Modifier.fillMaxWidth()
    ) {
        // La console et le jeu sur une meme ligne : deux etiquettes de ce que
        // la session est, et aucune des deux ne merite sa rangee.
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            model.console?.let { ConsoleBadge(it) }
            model.gameTitle?.let { title ->
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 300.dp)
                )
            }
        }

        // Ten of lift rather than the usual four: this is the one object on the
        // screen and it has to read as such from across a room. **Jamais sur
        // deux lignes** : un code coupe en deux n'est plus un code, c'est deux
        // morceaux qu'il faut recoller a voix haute.
        Box(
            modifier = Modifier
                .plate(CardShape, dark = dark, oled = oled, lift = 10.dp)
                .padding(horizontal = 34.dp, vertical = if (steps.isEmpty()) 20.dp else 12.dp)
        ) {
            val codeSize = if (steps.isEmpty()) 80.sp else 64.sp
            Text(
                model.code,
                fontSize = codeSize,
                lineHeight = codeSize * 1.05f,
                // Monospace : a distance, un 2 et un Z se distinguent par la
                // largeur de leur trace autant que par sa forme.
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
                color = accent.bright,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )
        }

        // Engraved, not plated: a reference to read off, not an object to reach for.
        // pourquoi : docs/decisions/second-ecran.md § Le code de session ne porte pas d'étiquette
        if (model.hostAddress != null || model.port != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                model.hostAddress?.let { Fact(stringResource(R.string.session_host_address), it) }
                model.port?.let { Fact(stringResource(R.string.session_port), it) }
            }
        }

        // Les commandes cote a cote, et la manette en designe une par un index
        // qui voyage par le singleton : le focus ne traverse pas les fenetres.
        // pourquoi : docs/decisions/second-ecran.md § Une seule colonne, et le code prend toute la largeur
        if (steps.isNotEmpty()) {
            val stepCursor by SecondScreen.stepCursor.collectAsState()
            // `IntrinsicSize.Min` : les deux plaques prennent la hauteur du plus
            // long des deux libelles. C'etait une hauteur fixe, pour qu'un
            // libelle sur deux lignes ne fasse pas grandir sa plaque a cote de sa
            // voisine — la paire se serait lue comme deux boutons de rangs
            // differents. La hauteur commune tient cette promesse sans plafonner
            // le texte : « 1. Paramétrage automatique de Dolphin » ne rentrait pas
            // et se coupait en plein mot.
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp).height(IntrinsicSize.Min)
            ) {
                steps.forEachIndexed { index, step ->
                    StepButton(step, selected = index == stepCursor, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * La liste d'amis, au dos.
 *
 * Deux colonnes des que ca depasse cinq : le panneau est large et court, et une
 * colonne de dix rangees deborderait la boite centree, qui rogne en silence.
 * pourquoi : docs/decisions/second-ecran.md § La liste d'amis descend au dos, les deux cartes restent devant
 */
@Composable
private fun FriendsFace(model: SecondScreenModel.Friends) {
    // La question de retrait vit **ici**, pas sur l'ecran de face : le doigt
    // vient de presser au dos, et une question posee de l'autre cote de la
    // machine ne se voit pas.
    // pourquoi : docs/decisions/second-ecran.md § La liste d'amis descend au dos, les deux cartes restent devant
    var confirming by remember { mutableStateOf<PanelFriend?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // En haut a gauche, et en gras : c'est le nom de ce que la face
            // porte, pas une legende posee au-dessus d'un objet centre.
            Text(
                stringResource(R.string.friends_panel_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (model.entries.isEmpty()) {
                Text(
                    stringResource(R.string.friends_none_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            // **Deux colonnes, toujours.** Une casse ne depasse jamais la moitie
            // de l'ecran : a pleine largeur, un nom et deux mots s'etalent sur
            // 500 dp et la rangee se lit comme une barre. Avec un seul ami, la
            // colonne de droite reste vide plutot que de laisser la casse
            // grandir.
            val half = (model.entries.size + 1) / 2
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(model.entries.take(half), model.entries.drop(half)).forEach { column ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        column.forEach { PanelFriendRow(it, onRemove = { confirming = it }) }
                    }
                }
            }
        }

        confirming?.let { friend ->
            PanelConfirm(
                friend = friend,
                onCancel = { confirming = null },
                onConfirm = {
                    friend.onRemove()
                    confirming = null
                }
            )
        }
    }
}

/**
 * La question, posee sur le panneau lui-meme.
 *
 * Pas un `Dialog` : une fenetre de dialogue appartient a l'ecran qui la lance,
 * et celle-ci s'ouvrirait devant le joueur au lieu de sous ses pouces. C'est un
 * voile et une plaque, dans la fenetre du panneau.
 * pourquoi : docs/decisions/second-ecran.md § La liste d'amis descend au dos, les deux cartes restent devant
 */
@Composable
private fun PanelConfirm(friend: PanelFriend, onCancel: () -> Unit, onConfirm: () -> Unit) {
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Le plateau s'assombrit, il ne se givre pas.
            .background(InkText.copy(alpha = if (dark) 0.74f else 0.62f))
            .tap(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCancel
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .widthIn(max = 420.dp)
                .plate(CardShape, dark = dark, oled = oled, lift = 8.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(24.dp)
        ) {
            Text(
                stringResource(R.string.friends_remove_confirm, friend.name),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GhostButton(
                    label = stringResource(R.string.friends_cancel),
                    onClick = onCancel
                )
                GhostButton(
                    label = stringResource(R.string.friends_remove),
                    onClick = onConfirm,
                    tint = if (dark) ErrorDark else ErrorLight
                )
            }
        }
    }
}

/** Un ami : son avatar, son nom avec son point, ce qu'il fait, et la croix. */
@Composable
private fun PanelFriendRow(friend: PanelFriend, onRemove: () -> Unit) {
    val dark = LocalEmufiiDarkTheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .socket(RoundedCornerShape(14.dp), dark)
            .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Avatar(name = friend.name, size = 34.dp)
        Column(modifier = Modifier.weight(1f)) {
            // Le point de presence contre le pseudo, et non a l'autre bout de la
            // casse : c'est une propriete de la personne, pas une colonne.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    friend.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(
                            if (friend.online) if (dark) GoodDark else GoodLight
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                )
            }
            Text(
                friend.line,
                style = MaterialTheme.typography.bodySmall,
                color = if (friend.inSession) if (dark) GoodDark else GoodLight
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // 48 dp : la cible faisait 38, sous le minimum, et c'est le seul endroit
        // du panneau ou une erreur de visee retire quelqu'un de la liste d'amis.
        // La croix reste a 16 — on agrandit la zone, pas le symbole.
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .tap(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            CrossIcon(size = 16.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Une etape, pressee au dos.
 *
 * Le meme dessin que sur l'ecran de face — plaque d'action, verte et cochee une
 * fois faite — parce que c'est le meme geste : un joueur qui apprend le bouton
 * devant doit le reconnaitre derriere.
 * pourquoi : docs/decisions/second-ecran.md § Le panneau prend les etapes, parce qu'il est tactile
 */
@Composable
private fun StepButton(step: PanelStep, selected: Boolean, modifier: Modifier = Modifier) {
    val dark = LocalEmufiiDarkTheme.current
    // L'anneau de selection est **le meme dessin que partout ailleurs** —
    // trait et halo de focusRing — pose autour de la plaque, dans une marge
    // reservee. Un contour maison colle au bouton mordait le libelle ; ici
    // l'anneau flotte a distance comme il flotte autour des commandes de
    // l'ecran de face.
    Box(
        modifier = modifier
            .padding(6.dp)
            .focusRing(selected, ActionShape)
    ) {
        Button(
            onClick = sounded(step.onPress),
            enabled = step.enabled,
            shape = ActionShape,
            // Une etape verrouillee garde une plaque et une encre franches :
            // elle n'est pas hors service, elle est **la suivante**. C'est
            // l'absence d'anneau qui dit qu'on ne peut pas encore la presser.
            // pourquoi : docs/decisions/second-ecran.md § Une étape verrouillée doit rester lisible à bout de bras
            colors = if (step.done) {
                ButtonDefaults.buttonColors(containerColor = if (dark) GoodDark else GoodLight)
            } else {
                ButtonDefaults.buttonColors(
                    disabledContainerColor =
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    disabledContentColor =
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                )
            },
            // Les 24 dp de Material de chaque cote sont regles pour un bouton de
            // dialogue, pas pour deux plaques qui se partagent la largeur d'un
            // panneau : ils prenaient un tiers de la place du libelle.
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            // Le plancher, pas la mesure : la rangee donne la hauteur commune.
            modifier = Modifier.fillMaxWidth().fillMaxHeight().heightIn(min = 64.dp)
        ) {
            // **La coche, que le vert seul ne remplace pas.** Une plaque verte
            // dit « c'est fait » a qui connait le code couleur ; la coche le dit
            // a tout le monde, et elle survit aux deux themes comme au coup
            // d'oeil de trois quarts qu'on jette a un panneau pose a plat.
            // pourquoi : docs/decisions/second-ecran.md § Le panneau prend les etapes, parce qu'il est tactile
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step.done) StepCheck(color = LocalContentColor.current, size = 20.dp)
                Text(
                    step.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    // Trois, parce qu'un libelle en porte deja deux et qu'une
                    // langue plus longue que le francais n'a pas a se faire
                    // couper.
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** One reference value in a recess: what it is, then what it says. */
@Composable
private fun Fact(label: String, value: String) {
    val dark = LocalEmufiiDarkTheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier
            .socket(RoundedCornerShape(14.dp), dark)
            .padding(horizontal = 18.dp, vertical = 9.dp)
    ) {
        // Ni l'etiquette ni la valeur ne passent a la ligne. Serre entre deux
        // colonnes, « Port » s'ecrivait « Por / t » et le numero un chiffre par
        // ligne : un creux de reference qui se casse ne se lit plus, il se
        // dechiffre.
        // pourquoi : docs/decisions/second-ecran.md § Le panneau prend les etapes, parce qu'il est tactile
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false
        )
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * The button legend, in the two bottom corners: leave on the left, act on the
 * right. An empty side takes no room.
 * pourquoi : docs/decisions/second-ecran.md § La légende, et pourquoi les symboles sont dessinés
 */
@Composable
private fun Legend(legend: PadLegend, modifier: Modifier = Modifier) {
    Row(
        // Elle garde sa hauteur meme vide : la face au repos n'a pas de legende,
        // et sa disparition faisait plonger le fondu de 29 dp avant qu'il ne
        // commence.
        // pourquoi : docs/decisions/second-ecran.md § Les deux bandes sont permanentes, la légende comprise
        modifier = modifier.heightIn(min = LEGEND_CAP),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (legend.isEmpty) return@Row
        Cluster(legend.left)
        Cluster(legend.right)
    }
}

@Composable
private fun Cluster(hints: List<PadHint>) {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
        hints.forEach { hint -> PadHintRow(hint) }
    }
}

/**
 * La coche d'une etape faite, dessinee plutot qu'importee : elle se pose ou on
 * la met, la ou un glyphe se centre sur sa boite de ligne et non sur son encre.
 * Le meme trace que celle de l'ecran de face — c'est la meme etape.
 * pourquoi : docs/decisions/second-ecran.md § Le panneau prend les etapes, parce qu'il est tactile
 */
@Composable
private fun StepCheck(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val stroke = Stroke(width = w * 0.16f, cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(w * 0.16f, w * 0.55f)
            lineTo(w * 0.40f, w * 0.79f)
            lineTo(w * 0.86f, w * 0.24f)
        }
        drawPath(path, color = color, style = stroke)
    }
}
