package eu.emufii.app.secondscreen

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
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
import eu.emufii.app.ui.theme.ArtworkShape
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
import eu.emufii.app.ui.wallpaper.TrayBackdrop

/**
 * What the second panel draws, whoever is holding the window.
 *
 * The same tray as the front screen, never a second style: the engraved ground,
 * the moulded plates, the one accent, as the direction contract pins them
 * (`ui/theme/Direction.kt`). A panel with its own look would read as another
 * app running on the back of the machine.
 *
 * Read at arm's length, off-axis, under the player's hands. So one object leads
 * each face and everything else labels it, and the panel never holds more than
 * a glance's worth. It has no cursor and no controls: it reports.
 *
 * Colour follows the product principle rather than the chrome — the box art is
 * the only thing on the browsing face allowed to be loud, and it even lends its
 * own extracted tone to the shadow it casts.
 */
@Composable
fun SecondScreenContent(model: SecondScreenModel) {
    val dark = LocalEmufiiDarkTheme.current
    val page by SecondScreen.page.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // This window has no wallpaper behind it, so the tray is painted rather
        // than shown through.
        TrayBackdrop(modifier = Modifier.fillMaxSize(), dark = dark)

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
                // Crossfaded, and it is not decoration.
                //
                // Without it the panel *cuts*: a cursor moving along a shelf
                // replaces a whole face per keypress, and text appearing at
                // full contrast in one frame reads as a flash out of the corner
                // of the eye — which is exactly where this screen is. A fade
                // also gives a picture that has not arrived yet the two hundred
                // milliseconds it needs, so a fast pass over the grid stops
                // looking like something loading over and over.
                //
                // Keyed on the game rather than on the model so the badge or
                // the catalogue arriving a moment later fills in silently,
                // instead of dissolving a face into the same face.
                Crossfade(
                    targetState = faceKey(model),
                    animationSpec = tween(220),
                    label = "panel-face"
                ) { key ->
                    // The model is read here rather than captured with the key:
                    // during a fade the outgoing face is still composed, and it
                    // must not redraw itself with the incoming face's content.
                    val shown = remember(key) { model }
                    when (shown) {
                        is SecondScreenModel.Idle -> Idle()
                        // Read live rather than frozen, and it is the one
                        // branch that must be. Every console shares the key
                        // "console", so `shown` — remembered *on the key* —
                        // would stay on whichever console was first shown and
                        // the card would never change its text. Since the key
                        // does not change between two consoles, no fade is
                        // running here, so there is no outgoing face to
                        // protect. The frozen value is still the fallback: on
                        // the way out, when the model has already become
                        // another face, it is what keeps this one intact for
                        // the length of the fade.
                        is SecondScreenModel.ConsoleFolder -> ConsoleCard(
                            (model as? SecondScreenModel.ConsoleFolder)?.console
                                ?: shown.console
                        )
                        is SecondScreenModel.Browsing -> BrowsingPages(shown, page)
                        is SecondScreenModel.InSession -> InSession(shown)
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
 * What counts as a different face, for the fade.
 *
 * A game arrives on the panel before what is known about it does: the
 * compatibility badge and the catalogue entry are published a moment later,
 * against the same ROM. Fading on the whole model would dissolve a face into an
 * almost identical one every time, which looks like a stutter; fading on
 * *identity* lets the late facts fill in where they are.
 */
private fun faceKey(model: SecondScreenModel): String = when (model) {
    is SecondScreenModel.Idle -> "idle"
    // Every console shares one key: the card must not be replaced when the
    // cursor moves from one folder to the next, because it animates that change
    // itself — see [ConsoleCard]. The outer fade is for changing *face*.
    is SecondScreenModel.ConsoleFolder -> "console"
    is SecondScreenModel.Browsing -> "rom:${model.rom.uri}"
    is SecondScreenModel.InSession -> "session:${model.code}"
}

/**
 * The band across the top: are we reachable, and is there any news.
 *
 * Two facts that belong to the app rather than to the game under the cursor, so
 * they keep their own strip and never move: the state of the machine on the
 * left, where a status light lives on every appliance the player owns, and
 * whatever just happened on the right, where there is room for a sentence.
 */
@Composable
private fun PanelHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        VpsMark()
        NoteStrip(modifier = Modifier.weight(1f))
    }
}

/**
 * The service light.
 *
 * A lit dot and two words, and nothing else: this is the one piece of chrome on
 * the panel and it has to be readable without being read — the colour answers
 * the question from across the room, the words only confirm it.
 *
 * Its own colour, not the app accent. The accent means "this is where you are"
 * everywhere else in Emufii, and a status light that borrowed it would make the
 * cursor mean two things. Green and red are what a socket, a router and a
 * console charger already say.
 */
@Composable
private fun VpsMark() {
    val state by VpsStatus.state.collectAsState()
    val dark = LocalEmufiiDarkTheme.current

    val tone = when (state) {
        VpsState.ONLINE -> if (dark) Color(0xFF3DDC84) else Color(0xFF14A05A)
        VpsState.OFFLINE -> if (dark) Color(0xFFFF6B5E) else Color(0xFFD1382B)
        // Grey while nothing is known. Printing "down" because a handheld is in
        // a tunnel would blame our machine for the train.
        VpsState.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(15.dp)
                // A lit lamp, not a printed circle: it carries its own glow the
                // way the tray's plates carry their own shadow. Off on OLED,
                // where there is nothing behind it to catch the light.
                .shadow(
                    elevation = if (state == VpsState.UNKNOWN) 0.dp else 12.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = tone,
                    spotColor = tone
                )
                .clip(CircleShape)
                .background(tone)
        )
        Column {
            Text(
                stringResource(R.string.panel_vps),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(
                    when (state) {
                        VpsState.ONLINE -> R.string.panel_vps_online
                        VpsState.OFFLINE -> R.string.panel_vps_offline
                        VpsState.UNKNOWN -> R.string.panel_vps_unknown
                    }
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Where the news comes out.
 *
 * A friend online, a version published: the front screen still says both, and a
 * player with one screen loses nothing. What this adds is the case the front
 * screen cannot serve — the emulator owns it — where the alternative is a
 * notification shade pulled over a running game.
 *
 * It arrives from above and leaves on its own, because nobody can dismiss it:
 * this window takes no touch by design. Anything that had to be acknowledged
 * here would stay forever.
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
        // The version, and this is the one screen where it belongs without
        // being asked for. It is the answer to the question the panel is
        // actually asked when nothing is running — "which build is this
        // handheld on?" — and answering it here saves walking into the
        // settings to find out. Set a step smaller and a step dimmer than the
        // name, so it reads as a footnote to it rather than a second line of
        // equal weight.
        Text(
            stringResource(R.string.panel_idle_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * The cursor is on a console's folder: what playing together means on that
 * machine, in a frame, in the middle of the panel.
 *
 * One plate and nothing else on the screen. This is the only face of the panel
 * that is *read* rather than glanced at, so it gets the shape a thing to be
 * read has here — a raised plate with room around it — instead of being laid
 * out like the browsing face, whose job is to put a box and a badge side by
 * side.
 *
 * The machine's name leads, because the player is looking at a shelf of folders
 * and the first thing the panel owes them is which one the cursor is on. Then
 * two lines, then a warning if that console has one. Nothing else fits, and
 * nothing else belongs: the front screen keeps every explanation it had, and a
 * player without a panel is not missing a word of it.
 */
@Composable
private fun ConsoleCard(console: Console) {
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current

    // The plate is one object that stays, and it grows and shrinks as the
    // cursor walks the shelf.
    //
    // Three earlier tries each broke something: a card sized to its own text
    // jumped from console to console, a card stretched to the full height was
    // two thirds empty, and a card pinned to the top sat under the header. All
    // three were attempts to stop a *cut* — the panel replacing one card with a
    // differently sized one between two frames.
    //
    // What was missing is that the change itself can be shown. Here the frame
    // is never replaced: it is the same plate throughout, centred, and its size
    // is animated towards whatever the next console needs while the words
    // crossfade inside it. Nothing snaps, nothing is padded out to a common
    // size, and the card is only ever as large as it has to be.
    //
    // Centred, so a card that grows opens from its middle in both directions —
    // growing downward alone would drag the eye, and the panel is read out of
    // the corner of it.
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
                    // A bar rather than a triangle with an exclamation mark in
                    // it. The panel draws its own symbols, and one of the two
                    // things it must never do is shout: this is a thing to know
                    // before starting, not an error that has happened.
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
 * The game, on two pages, and the second one is optional in the strongest sense.
 *
 * Page one is what a player glances at while moving a cursor: the box, the
 * machine, the title, whether it plays together, which dump this is. Page two is
 * what somebody who has stopped on a game wants — what it is about, when it came
 * out, what it looks like — and it is reached by a button on the *front* screen,
 * because this one has no cursor.
 *
 * Sliding rather than cross-fading. The button says "further down", and a page
 * that arrives from below is the same gesture the player just made; a dissolve
 * would say "replaced", which is not what happened.
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
 *
 * The cover is the object and gets the weight — it is the one thing the player
 * recognises before reading anything. The column beside it answers what a box
 * cannot: which machine, whether this one actually plays together, and which
 * dump is in the slot, because two copies of the same game are not the same
 * file and the player is the one who has to know it.
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
            // The box, and the way to its other page directly under it. The
            // control belongs to the thing it acts on: put in the middle of the
            // panel it was a fourth object floating between two columns, and it
            // read as a legend for the whole screen rather than for the game.
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
                // The dump's own line: region, revision, and the genre when the
                // catalogue knows one. Absent halves simply are not printed —
                // a panel that guessed "USA" from silence would be wrong for
                // every European player whose dumper skipped the tag.
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
 * The second page: what the game is, rather than which file it is.
 *
 * Everything here is editorial and everything here can be missing, so the page
 * is built from whatever exists and says so plainly when that is nothing. It
 * never claims: a synopsis in the wrong language is shown as the language it is
 * in, and a game the catalogue has never heard of gets one honest sentence
 * instead of an empty layout.
 */
@Composable
private fun Details(model: SecondScreenModel.Browsing) {
    val locale = panelLocale()
    val meta = model.meta

    // Laid out to fit, never to scroll. Nothing on this window can be scrolled
    // — it has no cursor and takes no touch — so anything below the fold is
    // simply lost, and a page that ends mid-picture looks broken rather than
    // long. The paragraph gives up its lines first, because a synopsis reads
    // fine cut short and a picture does not.
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
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
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
 * The language this window is drawn in.
 *
 * Read off *this* window's configuration rather than the process default: the
 * panel is a second display with its own configuration, and the player's choice
 * of language is applied per configuration. Taking `Locale.getDefault()` would
 * be right by accident and wrong the day the two disagree.
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
 * Off the main thread and keyed on the ROM: the folder listing is a real
 * provider query, and it happens while a page is turning.
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
 *
 * It is drawn as a *pressable* thing — a plate, like the legend's keycaps —
 * because that is what it is: the shoulder button on the front of the machine
 * does this. Nothing on this window is touchable, so a control that looked like
 * a target you could hit would be a lie; a cap with a letter beside it is the
 * same diagram the legend already draws in the corners.
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
 * The arrow, drawn rather than typed, for the reason the d-pad is: a character
 * would arrive from whatever font happens to carry it, and at 12.dp a fallback
 * face's weight and baseline both show.
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
 * The box, moulded onto the tray.
 *
 * Its shadow is tinted with the colour the artwork itself gave up
 * (`Rom.accentArgb`, already extracted for the front screen). That is the
 * content-colour rule taken literally: the tone is the game's, not the app's,
 * and it arrives as depth — a real offset shadow — rather than as a wash laid
 * over the chrome. Games with no extracted tone simply cast the tray's own
 * shadow, and nothing about the layout changes.
 */
@Composable
private fun Cover(model: SecondScreenModel.Browsing, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current
    val art by rememberTileArt(model.rom)

    val tone = model.rom.accentArgb?.let { Color(it) }
    val shadow = tone ?: Color(0xFF0A1220)

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
                shape = TileShape,
                clip = false,
                ambientColor = shadow.copy(alpha = if (dark) 0.55f else 0.30f),
                spotColor = shadow.copy(alpha = if (dark) 0.75f else 0.42f)
            )
            // The frame itself: a moulded plate, and the artwork sits *in* it.
            //
            // A rim alone was not enough here, and the arithmetic says why. The
            // contour is a 1.5.dp stroke centred on the outline, so the clip
            // eats its outer half and 1.73px survive, at 24% opacity, across a
            // 452px cover — 0.38% of the width, half the presence it has on a
            // grid tile, which is why it reads on the front screen and vanishes
            // on this one. Scaling the stroke instead would have been a second
            // rule for one place. A plate with the picture inset is the frame
            // this world already owns: face, edge and lit bevel, at a size the
            // eye can find from across a room.
            .plate(TileShape, dark = dark, oled = oled, lift = 0.dp)
            .padding(9.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(ArtworkShape)
                // White under the artwork in daylight, as the tiles do: box art
                // of any colour has to read against something, and the tray is
                // not it.
                .background(tilePlateBrush(dark, oled))
                // The picture keeps its own contour inside the frame, drawn over
                // the image the way a tile does it.
                .moldedRim(ArtworkShape, dark = dark, oled = oled)
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
                    modifier = Modifier.fillMaxSize().socket(ArtworkShape, dark)
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
 * A session is up, and the code is the whole point.
 *
 * It carries no label above it. The six characters in the app's accent, on the
 * one plate lifted off the tray, are already the only thing on the panel that
 * could be read out to somebody — naming them would be a costume of importance,
 * which this app does not wear.
 */
@Composable
private fun InSession(model: SecondScreenModel.InSession) {
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current
    val accent = LocalAccent.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        model.console?.let { ConsoleBadge(it) }

        // Ten of lift rather than the usual four: this is the one object on the
        // screen and it has to read as such from across a room.
        Box(
            modifier = Modifier
                .plate(CardShape, dark = dark, oled = oled, lift = 10.dp)
                .padding(horizontal = 44.dp, vertical = 22.dp)
        ) {
            Text(
                model.code,
                fontSize = 80.sp,
                lineHeight = 86.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
                color = accent.bright,
                textAlign = TextAlign.Center
            )
        }

        // The two numbers the emulator's dialog asks for, engraved into the
        // tray rather than plated: they are a reference to read off, not an
        // object to reach for. Recessed side by side because they are typed
        // together, and a player copying one at a time has to come back.
        if (model.hostAddress != null || model.port != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                model.hostAddress?.let { Fact(stringResource(R.string.session_host_address), it) }
                model.port?.let { Fact(stringResource(R.string.session_port), it) }
            }
        }

        model.gameTitle?.let { title ->
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * The button legend, in the two bottom corners.
 *
 * Left is where you leave from, right is where you act: the arrangement of
 * every console shell the player already owns. An empty side takes no room, so
 * a face with nothing to say on the left leaves no hole.
 */
@Composable
private fun Legend(legend: PadLegend, modifier: Modifier = Modifier) {
    if (legend.isEmpty) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cluster(legend.left)
        Cluster(legend.right)
    }
}

@Composable
private fun Cluster(hints: List<PadHint>) {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
        hints.forEach { hint ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                KeyCap(hint)
                Text(
                    stringResource(hint.label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * One button, moulded like the machine's own.
 *
 * A plate and not a recess: this is the picture of a thing that sticks out and
 * can be pressed, and the tray's sockets are for holes. Flush, at 2.dp of lift,
 * because a legend is a diagram and must not compete with what it labels.
 */
@Composable
private fun KeyCap(hint: PadHint) {
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current
    val tint = MaterialTheme.colorScheme.onSurface
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(26.dp)
            .plate(
                PillShape,
                dark = dark,
                oled = oled,
                lift = 2.dp,
                // A hint about holding shows a held button: the plate loses its
                // lift and its lit edge and takes the shadow's shade, which is
                // what "pushed in" is made of here. It is the same letter as the
                // press above it, because it is the same button.
                pressed = hint.held
            )
    ) {
        val glyph = hint.glyph
        if (glyph == null) DPadGlyph(tint) else CapLetter(glyph, tint)
    }
}

/**
 * The letter, centred on its own ink.
 *
 * Three separate things pushed it off centre, and laying the text out could
 * only ever fix one of them.
 *
 * **Left.** `labelLarge` carries `letterSpacing = 0.1.sp`, and Compose adds that
 * space *after* the last character as well as between characters. On a
 * one-letter string the measured width is the glyph plus one trailing gap, so
 * centring the measurement leaves the ink sitting left.
 *
 * **Down.** `labelLarge` sets `lineHeight` 18.sp over `fontSize` 14.sp. Trimming
 * that leading still leaves a box running ascent to descent, while a capital
 * with no descender fills only baseline to cap height. Centring that box is not
 * centring the letter, and no amount of trimming makes the two the same.
 *
 * **Right, once the first two were fixed.** Centring on the advance width is
 * still not centring the ink: a glyph's side bearings differ, so B in this face
 * sits measurably right of its own advance centre. Measured offline against
 * `rounded_bold.ttf` at this exact size, since a second display cannot be
 * screenshotted.
 *
 * So the glyph is drawn, and placed from [android.graphics.Paint.getTextBounds],
 * the smallest rectangle enclosing the ink. Put the pen at
 * `w/2 - (left + right)/2` and the ink centre lands on the cap centre by
 * construction, on both axes, for any glyph and any type scale.
 */
@Composable
private fun CapLetter(glyph: String, tint: Color) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val paint = remember(context, tint, density) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            typeface = runCatching { ResourcesCompat.getFont(context, R.font.rounded_bold) }
                .getOrNull() ?: android.graphics.Typeface.DEFAULT_BOLD
            textSize = with(density) { 14.sp.toPx() }
            // LEFT, not CENTER: the pen is positioned from the ink bounds below,
            // and CENTER would subtract half an advance on top of it.
            textAlign = android.graphics.Paint.Align.LEFT
            color = tint.toArgb()
        }
    }
    val bounds = remember(paint, glyph) {
        android.graphics.Rect().also { paint.getTextBounds(glyph, 0, glyph.length, it) }
    }
    Canvas(Modifier.size(26.dp)) {
        drawContext.canvas.nativeCanvas.drawText(
            glyph,
            size.width / 2f - (bounds.left + bounds.right) / 2f,
            size.height / 2f - (bounds.top + bounds.bottom) / 2f,
            paint
        )
    }
}

/**
 * The d-pad, drawn rather than typed.
 *
 * Drawn because a character would arrive from whatever font happens to carry
 * it, and a cap is 26.dp wide: a fallback face's weight and baseline both show
 * at that size. The system says icons are drawn, never characters.
 */
@Composable
private fun DPadGlyph(tint: Color) {
    Canvas(Modifier.size(10.dp)) {
        // Arms a touch wider than a third, the proportion a moulded d-pad
        // actually has: thinner reads as a mathematical plus.
        val arm = size.width * 0.38f
        val radius = CornerRadius(size.width * 0.06f, size.width * 0.06f)
        // Two bars crossing, each centred: the overlap is the hub, so the cross
        // has no seam and needs no third shape.
        drawRoundRect(
            color = tint,
            topLeft = Offset((size.width - arm) / 2f, 0f),
            size = Size(arm, size.height),
            cornerRadius = radius
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, (size.height - arm) / 2f),
            size = Size(size.width, arm),
            cornerRadius = radius
        )
    }
}
