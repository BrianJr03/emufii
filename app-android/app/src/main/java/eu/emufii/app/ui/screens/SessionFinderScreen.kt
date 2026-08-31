package eu.emufii.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.emufii.app.library.Rom
import eu.emufii.app.library.RomsRepository
import eu.emufii.app.ui.components.RomArtwork
import eu.emufii.app.ui.components.PadTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import eu.emufii.app.R
import eu.emufii.app.profile.playerDisplayName
import eu.emufii.app.network.CoordinatorClient
import androidx.compose.ui.platform.LocalContext
import eu.emufii.app.library.Console
import eu.emufii.app.network.OpenSession
import eu.emufii.app.ps2.Ps2NetworkProfile
import eu.emufii.app.ui.components.rememberPs2Ready
import eu.emufii.app.ui.components.rememberPpssppReady
import eu.emufii.app.ui.components.Avatar
import eu.emufii.app.ui.components.EmufiiScaffold
import eu.emufii.app.ui.components.GhostButton
import eu.emufii.app.ui.components.SectionHeader
import eu.emufii.app.ui.components.SoftCard
import eu.emufii.app.ui.components.LensMark
import eu.emufii.app.ui.components.PersonMark
import eu.emufii.app.ui.controlRing
import eu.emufii.app.ui.LocalRingTone
import eu.emufii.app.ui.RingTone
import eu.emufii.app.ui.theme.Coral
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.LocalEmufiiOledTheme
import eu.emufii.app.ui.theme.plate
import eu.emufii.app.ui.theme.socket
import eu.emufii.app.ui.components.padEntry
import eu.emufii.app.ui.components.SignalMark
import eu.emufii.app.ui.theme.TileShape
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import kotlinx.coroutines.delay
import eu.emufii.app.ui.tap

/**
 * Every session currently open, joinable in one tap. Polled rather than listened to: a
 * socket would be a lot for a screen you stay on for twenty seconds.
 * pourquoi : docs/decisions/lancement-et-navigation.md § The session finder polls, it does not listen
 */
@Composable
fun SessionFinderScreen(
    client: CoordinatorClient,
    /**
     * The local library, to put a face on a session: the coordinator knows only
     * a title, which we match against what we hold locally.
     * pourquoi : docs/decisions/lancement-et-navigation.md § The session finder polls, it does not listen
     */
    romsRepo: RomsRepository,
    onBack: () -> Unit,
    onJoin: (OpenSession) -> Unit
) {
    var sessions by remember { mutableStateOf<List<OpenSession>>(emptyList()) }
    var library by remember { mutableStateOf<List<Rom>>(emptyList()) }
    var query by remember { mutableStateOf("") }

    // The cache, never a fresh scan: opening the session list must not trigger a
    // read of a multi-GB SAF tree.
    LaunchedEffect(Unit) {
        library = withContext(Dispatchers.IO) { runCatching { romsRepo.scan() }.getOrDefault(emptyList()) }
    }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Read outside the effect: stringResource needs a composable scope, and the
    // fallback used to be a French literal that showed up in an English app.
    val unreachable = stringResource(R.string.finder_unreachable)

    LaunchedEffect(Unit) {
        while (true) {
            client.listSessions()
                .onSuccess { sessions = it; error = null }
                // Never `it.message`: an unreachable coordinator carries the
                // IOException's text, which names a host and a port and means
                // nothing to a player.
                .onFailure { error = unreachable }
            loading = false
            delay(REFRESH_MS)
        }
    }

    // The filter covers what is read on the card: the game, the host, the code.
    // Searching the code is as useful as searching a title, that being what a
    // friend sends you in a message.
    val shown = remember(sessions, query) {
        val q = query.trim()
        if (q.isBlank()) sessions
        else sessions.filter {
            listOfNotNull(it.romTitle, it.hostName, it.code)
                .any { field -> field.contains(q, ignoreCase = true) }
        }
    }

    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // The social domain: the pad cursor turns coral here.
    // pourquoi : docs/decisions/theme-duotone-shelves.md § GAMEPAD FOCUS
    CompositionLocalProvider(LocalRingTone provides RingTone.CORAL) {
    EmufiiScaffold(
        title = stringResource(R.string.finder_title),
        onBack = onBack
    ) { topPadding ->
        Box(Modifier.fillMaxSize()) {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            error != null -> FinderMessage(
                mark = { tint -> SignalMark(color = tint) },
                title = stringResource(R.string.finder_unreachable),
                subtitle = error!!,
                topPadding = topPadding
            )

            sessions.isEmpty() && query.isBlank() -> FinderMessage(
                // A socket, as the grid's last row leaves an empty slot, but with its
                // mark inside. "Nobody yet" speaks of absent players, and the
                // silhouette is what the app already draws for one.
                mark = { tint -> PersonMark(size = 40.dp, color = tint) },
                hollow = true,
                title = stringResource(R.string.finder_nobody_yet),
                subtitle = stringResource(R.string.finder_empty),
                topPadding = topPadding
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = 20.dp, end = 20.dp,
                    top = topPadding,
                    bottom = bottomInset + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    SearchField(
                        query = query,
                        onQueryChange = { query = it },
                        modifier = Modifier.fillMaxWidth().padEntry()
                    )
                }
                item { SectionHeader(pluralSessions(shown.size)) }
                if (shown.isEmpty()) {
                    item {
                        // A search with no results is not an empty list: saying
                        // "nobody" here would suggest the sessions had vanished
                        // when we have simply filtered too hard.
                        Text(
                            stringResource(R.string.finder_no_match),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                items(shown, key = { it.code }) { session ->
                    SessionCard(
                        session = session,
                        rom = library.firstOrNull { rom ->
                            rom.displayName.equals(session.romTitle, ignoreCase = true)
                        },
                        onJoin = { onJoin(session) }
                    )
                }
            }
        }
        }
    }
    }
}

/**
 * The search bar. The system keyboard writes here: the socket existed to hold the IME
 * off and open the app's own keypad instead, at the price of a keyboard that is
 * nobody's. It stays a socket to the eye, with a field underneath.
 * pourquoi : docs/decisions/lancement-et-navigation.md § Search opens the app's keyboard
 */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalEmufiiDarkTheme.current
    val shape = RoundedCornerShape(14.dp)
    val field = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val tint = MaterialTheme.colorScheme.onSurface
    val raise = {
        runCatching { field.requestFocus() }
        keyboard?.show()
        Unit
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .controlRing(shape)
            .socket(shape, dark)
            .tap(onClick = raise)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        LensMark(size = 20.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            // Search lowers the keyboard without emptying the field: the list is
            // already filtered on every keystroke.
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = tint),
            cursorBrush = SolidColor(tint),
            modifier = Modifier.weight(1f).focusRequester(field)
        ) { inner ->
            if (query.isEmpty()) {
                Text(
                    stringResource(R.string.finder_search),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            inner()
        }
    }
}

@Composable
private fun SessionCard(session: OpenSession, rom: Rom?, onJoin: () -> Unit) {
    val dark = LocalEmufiiDarkTheme.current
    val host = session.hostName?.let { playerDisplayName(it) }
        ?: stringResource(R.string.finder_host)

    // Only knowable for a game we own: the coordinator publishes a title, not a
    // console. The PSP refusal mirrors the PS2's: a guest without the memory
    // stick grant never receives the session address in PPSSPP.
    val ps2Blocked = rom?.console == Console.PS2 && !rememberPs2Ready()
    val pspBlocked = rom?.console == Console.PSP && !rememberPpssppReady()
    val joinBlocked = ps2Blocked || pspBlocked

    // No `padEntry` here: a `FocusRequester` shared between twelve nodes points at
    // nothing.
    // pourquoi : docs/decisions/lancement-et-navigation.md § One named destination per screen
    SoftCard(
        onClick = onJoin,
        modifier = Modifier.animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // The cover art when we have the game, the host otherwise. No empty
            // square in the middle: a card with no visual is worse than a card
            // showing something else that is true.
            if (rom != null) RomArtwork(rom = rom, size = 64.dp)
            else Avatar(name = host, size = 56.dp)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    session.romTitle ?: stringResource(R.string.finder_unknown_game),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // The console comes from the local ROM: the coordinator does
                    // not know it, and guessing it from a title would be a bet.
                    rom?.let { MetaChip(it.console.label) }
                    // The ROM's identifier, when it has one, is what tells two
                    // editions of the same game apart, and therefore the only
                    // honest answer to "is this really my version?".
                    (rom?.titleIdHex ?: rom?.productCode)?.let { MetaChip(it) }
                    // The code is the link you hand out: a coral pill.
                    MetaChip(session.code, highlight = true)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "$host · ${playersLabel(session.players)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                if (session.ready && !joinBlocked) {
                    GhostButton(
                        label = stringResource(R.string.finder_join),
                        onClick = onJoin
                    )
                } else if (joinBlocked) {
                    // Joining would come back with the same refusal as the
                    // launch card: a PS2 game whose memory card carries no
                    // network profile never opens its local menu, a PSP game
                    // without the memory stick grant never hears the session
                    // address. Said on the card, so the session does not look
                    // joinable when it is not.
                    Text(
                        stringResource(
                            if (ps2Blocked) R.string.finder_ps2_profile
                            else R.string.finder_ppsspp_setup
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // The host's tunnel isn't up yet; joining now would just
                    // spin. Say so rather than offer a button that stalls.
                    Text(
                        stringResource(R.string.finder_starting),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun playersLabel(n: Int): String = when (n) {
    0 -> stringResource(R.string.finder_nobody_yet)
    1 -> stringResource(R.string.finder_one_player)
    else -> stringResource(R.string.finder_n_players, n)
}

/** "3 sessions en cours" / "3 sessions in progress", plural per language. */
@Composable
private fun pluralSessions(count: Int): String {
    val sessions = if (count == 1) stringResource(R.string.finder_one_session, count)
    else stringResource(R.string.finder_many_sessions, count)
    return stringResource(R.string.finder_in_progress, sessions)
}

@Composable
private fun FinderMessage(
    mark: (@Composable (Color) -> Unit)?,
    title: String,
    subtitle: String,
    topPadding: androidx.compose.ui.unit.Dp,
    /**
     * True when the block speaks of an absence: the mark then sits in a socket rather
     * than on a plate. The tray already has a word for nothing here, and it is the
     * hollow.
     */
    hollow: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // The same margin top and bottom, which is what centres it: an empty screen
            // has nothing else to look at, so the offset shows.
            // pourquoi : docs/decisions/lancement-et-navigation.md § An empty screen is centred on the screen, not under the header
            .padding(top = topPadding, bottom = topPadding, start = 32.dp, end = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The mark sits on a moulded object; a socket only when the screen speaks of an
        // absence, and with its mark inside, since bare it reads as an icon that failed
        // to load.
        // pourquoi : docs/decisions/lancement-et-navigation.md § An empty screen is centred on the screen, not under the header
        Box(
            modifier = Modifier
                .size(88.dp)
                .then(
                    if (hollow) Modifier.socket(TileShape, LocalEmufiiDarkTheme.current)
                    else Modifier.plate(
                        shape = CircleShape,
                        dark = LocalEmufiiDarkTheme.current,
                        oled = LocalEmufiiOledTheme.current,
                        lift = 6.dp
                    )
                ),
            contentAlignment = Alignment.Center
        ) { mark?.invoke(MaterialTheme.colorScheme.onSurfaceVariant) }

        Spacer(Modifier.height(20.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            // This column sits straight on the wallpaper: nothing supplies a content
            // colour, and a Text without one falls back to black, invisible on the dark
            // theme.
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private const val REFRESH_MS = 4000L

/**
 * A metadata pill: console, ROM id, session code. Pills rather than a sentence: a
 * sentence is read whole, pills are scanned.
 * pourquoi : docs/decisions/lancement-et-navigation.md § The session finder polls, it does not listen
 */
@Composable
private fun MetaChip(text: String, highlight: Boolean = false) {
    val dark = LocalEmufiiDarkTheme.current
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = if (highlight) (if (dark) Coral.darkBright else Coral.ink)
                else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        // An ellipsis rather than the default hard clip: a word sliced mid-glyph reads
        // as a rendering fault, three dots say something is missing.
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (highlight) Coral.soft
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
