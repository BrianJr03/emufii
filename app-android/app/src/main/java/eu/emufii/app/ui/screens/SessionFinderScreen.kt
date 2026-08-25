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
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalConfiguration
import eu.emufii.app.ui.components.EmufiiKeyboard
import eu.emufii.app.ui.components.LensMark
import eu.emufii.app.ui.components.PersonMark
import eu.emufii.app.ui.controlRing
import eu.emufii.app.ui.theme.LocalEmufiiOledTheme
import eu.emufii.app.ui.theme.plate
import eu.emufii.app.ui.theme.socket
import eu.emufii.app.ui.components.padEntry
import eu.emufii.app.ui.components.SignalMark
import eu.emufii.app.ui.theme.TileShape
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import kotlinx.coroutines.delay

/**
 * Every session currently open, joinable in one tap.
 *
 * Polls rather than pushes: sessions last an hour at most and the list is
 * small, so a socket would be a lot of machinery for a screen you sit on for
 * twenty seconds.
 */
@Composable
fun SessionFinderScreen(
    client: CoordinatorClient,
    /**
     * The local library, to put a face on a session.
     *
     * The coordinator only knows a title: it has neither cover art nor console to
     * offer, and it has no business having any, these being ROMs, which live on
     * the device. So we match the announced title against what we have locally,
     * and when it lands the card shows the game's real icon. Otherwise it shows
     * the host: a session stays identifiable by whoever opens it.
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

    // Le clavier de l'app, jamais celui d'Android.
    //
    // Le champ etait un `PadTextField`, donc un vrai champ de saisie, donc l'IME
    // du systeme : il recouvre la moitie de l'ecran, il n'a pas la manette pour
    // lui, et il n'a rien a voir avec le reste. La bibliotheque n'a jamais eu de
    // champ — elle affiche la requete et pose son propre clavier — et cet ecran
    // fait desormais pareil.
    // pourquoi : docs/decisions/lancement-et-navigation.md § La recherche ouvre le clavier de l'app
    var searching by remember { mutableStateOf(false) }

    // B ferme le clavier avant de quitter l'ecran : c'est un sous-niveau, comme
    // une rangee depliee l'etait dans les reglages.
    BackHandler(enabled = searching) { searching = false }

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
                // Une alveole, comme le dernier rang de la grille laisse un
                // emplacement vide — mais avec sa marque dedans. « Personne pour
                // l'instant » parle de joueurs absents, et la silhouette est ce
                // que l'app dessine deja pour un joueur.
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
                        onOpen = { searching = true },
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
                // De quoi faire defiler la derniere carte au-dessus du clavier,
                // qui la recouvrirait sinon.
                if (searching) item { Spacer(Modifier.height(KEYBOARD_ROOM)) }
            }
        }

        // Une tape a cote ferme le clavier, comme dans la bibliotheque.
        //
        // Un voile invisible sur tout l'ecran, declare **avant** le panneau pour
        // que les touches restent au-dessus de lui. Sans ca, la seule facon de
        // refermer etait `B`, et une tape sur une carte rejoignait une session
        // en plein mot — le panneau, lui, avale deja les siennes.
        // pourquoi : docs/decisions/lancement-et-navigation.md § La recherche ouvre le clavier de l'app
        if (searching) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .focusProperties { canFocus = false }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { searching = false }
                    )
            )
        }

        AnimatedVisibility(
            visible = searching,
            enter = fadeIn(tween(140)) + slideInVertically(tween(180)) { it },
            exit = fadeOut(tween(120)) + slideOutVertically(tween(180)) { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .plate(
                        shape = RoundedCornerShape(28.dp),
                        dark = LocalEmufiiDarkTheme.current,
                        oled = LocalEmufiiOledTheme.current,
                        lift = 8.dp
                    )
                    // Le panneau avale chaque appui qui n'est pas une touche,
                    // les interstices compris : sans ca, un rate atterrit sur la
                    // carte derriere et rejoint une session en plein mot.
                    .focusProperties { canFocus = false }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                EmufiiKeyboard(
                    onKey = { query += it },
                    onBackspace = { if (query.isNotEmpty()) query = query.dropLast(1) },
                    maxHeight = LocalConfiguration.current.screenHeightDp.dp / 2 - 32.dp,
                )
            }
        }
        }
    }
}

/** Ce que le clavier prend en bas, et qu'il faut pouvoir depasser en defilant. */
private val KEYBOARD_ROOM = 260.dp

/**
 * La barre de recherche : une alveole, pas un champ.
 *
 * Elle n'est pas editable — il n'y a rien a editer, le clavier de l'app ecrit
 * dans la requete — et c'est precisement ce qui empeche l'IME du systeme de
 * s'ouvrir. Elle reste un vrai noeud de focus, donc la manette s'y arrete et
 * l'anneau la montre.
 * pourquoi : docs/decisions/lancement-et-navigation.md § La recherche ouvre le clavier de l'app
 */
@Composable
private fun SearchField(query: String, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val dark = LocalEmufiiDarkTheme.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .controlRing(shape)
            .socket(shape, dark)
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        LensMark(size = 20.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            query.ifBlank { stringResource(R.string.finder_search) },
            style = MaterialTheme.typography.bodyLarge,
            color = if (query.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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

    // **Plus de `padEntry` ici.** La carte etait la destination nommee de la
    // manette, du temps ou elle etait le premier controle de l'ecran. Depuis
    // qu'une barre de recherche la precede, elles le portaient toutes les
    // deux — et un `FocusRequester` partage entre douze noeuds ne designe plus
    // rien : le curseur descendait de l'en-tete vers une carte au hasard en
    // sautant la recherche, et « haut » depuis n'importe quelle carte
    // remontait droit au bouton retour au lieu de passer a la carte du dessus.
    // pourquoi : docs/decisions/lancement-et-navigation.md § Une seule destination nommée par écran
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
                    MetaChip(session.code)
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
     * Vrai quand le bloc parle d'une **absence** : la marque se pose alors dans
     * une alveole plutot que sur une plaque. Le plateau a deja un mot pour
     * « rien ici », et c'est le trou.
     */
    hollow: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // La meme marge en haut et en bas, et c'est ce qui le centre.
            //
            // Il n'y avait que celle du haut — la bande de l'en-tete — donc le
            // bloc etait centre dans ce qui reste **sous** l'en-tete, et son
            // milieu tombait une cinquantaine de dp sous le milieu de l'ecran.
            // Un ecran vide n'a rien d'autre a regarder : le decalage se voit.
            // pourquoi : docs/decisions/lancement-et-navigation.md § Un écran vide se centre sur l'écran, pas sous l'en-tête
            .padding(top = topPadding, bottom = topPadding, start = 32.dp, end = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // La marque tient sur un objet moule, le meme que le bouton rond de
        // l'en-tete : un etat vide fait encore partie du plateau, il n'y est pas
        // un trou. Sauf quand il parle justement d'une absence, et la c'est une
        // alveole — mais **avec sa marque dedans**.
        //
        // L'alveole a d'abord ete laissee nue, au motif qu'un emplacement sans
        // jeu est deja ce que la grille dessine. Vue en vrai, elle ne se lit pas
        // comme une metaphore : elle se lit comme une icone qui n'a pas charge.
        // pourquoi : docs/decisions/lancement-et-navigation.md § Un écran vide se centre sur l'écran, pas sous l'en-tête
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
            // Cette colonne est posee a meme le fond d'ecran : rien ne fournit
            // de couleur de contenu, et un Text sans couleur retombe en noir,
            // invisible sur le theme sombre.
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
 * A metadata pill: console, ROM id, session code.
 *
 * Three short facts lined up, rather than a sentence separated by full stops. A
 * sentence has to be read whole to extract one detail; pills are scanned, which
 * is what people do in front of a list of sessions.
 */
@Composable
private fun MetaChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
