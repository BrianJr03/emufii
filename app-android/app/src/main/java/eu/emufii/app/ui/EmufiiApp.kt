package eu.emufii.app.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.CompositionLocalProvider
import eu.emufii.app.compat.LocalCompatDb
import eu.emufii.app.compat.CompatCheck
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.emufii.app.LocalEnsureVpnPermission
import eu.emufii.app.R
import eu.emufii.app.library.Backend
import eu.emufii.app.library.Rom
import eu.emufii.app.library.RomsRepository
import eu.emufii.app.network.CoordinatorClient
import eu.emufii.app.azahar.NetplayAutomation
import eu.emufii.app.azahar.PlanStore
import eu.emufii.app.network.CoordinatorError
import eu.emufii.app.network.CreatedSession
import eu.emufii.app.library.Console
import eu.emufii.app.library.GameTitles
import eu.emufii.app.artwork.ArtworkPreload
import eu.emufii.app.library.allEmulators
import eu.emufii.app.notify.AppForeground
import eu.emufii.app.notify.FriendEvent
import eu.emufii.app.notify.FriendWatcher
import eu.emufii.app.notify.FriendWatchJob
import eu.emufii.app.notify.Notifications
import eu.emufii.app.profile.FriendStore
import eu.emufii.app.meta.LocalGameMetaDb
import eu.emufii.app.meta.MetaCheck
import eu.emufii.app.secondscreen.PanelFeed
import eu.emufii.app.ui.components.FriendAlert
import eu.emufii.app.profile.ProfileStore
import eu.emufii.app.ps2.Ps2NetworkProfile
import eu.emufii.app.settings.SettingsStore
import eu.emufii.app.session.RomRef
import eu.emufii.app.profile.Friend
import eu.emufii.app.profile.Profile
import eu.emufii.app.secondscreen.PanelFriend
import eu.emufii.app.secondscreen.SecondScreen
import eu.emufii.app.secondscreen.SecondScreenModel
import eu.emufii.app.session.Session
import eu.emufii.app.session.SessionCodes
import eu.emufii.app.tunnel.TunnelHolder
import eu.emufii.app.tunnel.slotIsFree
import eu.emufii.app.tunnel.tunnelHolder
import eu.emufii.app.ui.components.TunnelConflictDialog
import eu.emufii.app.ui.screens.FriendsScreen
import eu.emufii.app.ui.screens.JoinScreen
import eu.emufii.app.ui.screens.LibraryScreen
import eu.emufii.app.ui.screens.OnboardingScreen
import eu.emufii.app.ui.screens.PreparingScreen
import eu.emufii.app.ui.screens.PspOnlineScreen
import eu.emufii.app.ui.screens.SessionFinderScreen
import eu.emufii.app.ui.screens.SessionScreen
import eu.emufii.app.ui.screens.settings.SettingsScreen
import eu.emufii.app.ui.screens.SplashScreen
import eu.emufii.app.ui.screens.WfcScreen
import eu.emufii.app.wfc.WfcManager
import eu.emufii.app.wfc.WfcState
import eu.emufii.app.wg.EmufiiWgManager
import eu.emufii.app.wg.WgState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private sealed interface Screen {
    data object Library : Screen
    data object Finder : Screen
    data class Preparing(val label: String) : Screen
    data class Join(val rom: RomRef) : Screen
    data class InSession(val session: Session) : Screen

    /**
     * The Kaeru route: a Rom rather than a Session, because there is no session.
     * pourquoi : docs/decisions/lancement-et-navigation.md § Deux routes qui ne sont pas des sessions
     */
    data class Wfc(val rom: Rom) : Screen

    /**
     * Public PSP ad hoc. A screen rather than a card: you leave it to set PPSSPP
     * up and must find your place again on the way back.
     * pourquoi : docs/decisions/lancement-et-navigation.md § Deux routes qui ne sont pas des sessions
     */
    data class PspOnline(val rom: Rom) : Screen

    /** Profile and app settings. A place you visit, hence a screen. */
    data object ProfileAndSettings : Screen

    data object Friends : Screen
}

private fun Rom.toRef() =
    RomRef(
        uri = uri,
        displayName = displayName,
        console = console,
        titleIdHex = titleIdHex,
        filename = filename,
        productCode = productCode,
        ps2ElfCrc = ps2ElfCrc,
    )

/**
 * The splash screen's token, at process scope — un `rememberSaveable` revient
 * avec l'activite. [rearm] est appele a chaque demarrage reel, sauf en session.
 * pourquoi : docs/decisions/lancement-et-navigation.md § Le préchargement tourne, et l'app se compose derrière lui
 */
internal object SplashGate {
    var pending by mutableStateOf(true)
    var sessionAlive = false

    fun rearm() {
        if (!sessionAlive) pending = true
    }

}

private const val DEFAULT_PORT = 24872

/** A cold tunnel takes seconds to come up; past this something is wrong, not slow. */
private const val TUNNEL_TIMEOUT_MS = 45_000L

private const val CODE_ATTEMPTS = 5

/**
 * How often we tell the coordinator we're around while *outside* a session;
 * inside one, the member heartbeat reports it and this stops.
 * pourquoi : docs/decisions/lancement-et-navigation.md § Ce qui est hissé au niveau de l'app, et pourquoi
 */
private const val PRESENCE_MS = 45_000L

/**
 * Tearing a tunnel down is local work: it is quick, or it is stuck.
 * pourquoi : docs/decisions/lancement-et-navigation.md § Le créneau VPN unique d'Android
 */
private const val TUNNEL_RELEASE_MS = 6_000L

/**
 * Polls until the host publishes its address. Do not use `return@repeat` here:
 * it only ends the current iteration.
 * pourquoi : docs/decisions/lancement-et-navigation.md § Le créneau VPN unique d'Android
 */
private suspend fun pollHostIp(client: CoordinatorClient, code: String): String? {
    repeat(20) {
        delay(500)
        client.getSession(code).getOrNull()?.hostIp?.let { return it }
    }
    return null
}

@Composable
fun EmufiiApp(settings: SettingsStore) {
    SilenceSystemSfx()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { CoordinatorClient() }
    val profileStore = remember { ProfileStore(context) }
    val friendStore = remember { FriendStore.get(context) }
    // Handed in rather than built here: the theme is applied above this
    // composable, so it has to read the same store the settings page writes.
    val settingsStore = settings
    val romsRepo = remember { RomsRepository(context) }
    val profile by profileStore.profile.collectAsState()
    /**
     * Survives the activity recreation a language change causes, so the player
     * lands back where they were.
     * pourquoi : docs/decisions/lancement-et-navigation.md § Le logo, une fois par processus et jamais au premier lancement
     */
    var onProfilePage by rememberSaveable { mutableStateOf(false) }
    var screen by remember {
        mutableStateOf<Screen>(if (onProfilePage) Screen.ProfileAndSettings else Screen.Library)
    }
    // The gate must not re-arm while a session lives: coming back from the
    // emulator has to land back in the session's screen, not in front of the
    // logo. Preparation counts too — the VPN prompt and the emulator's launch
    // both pass through activity stops on their way there.
    SideEffect {
        SplashGate.sessionAlive =
            screen is Screen.InSession || screen is Screen.Preparing
    }
    // Derive de `screen`, jamais pousse depuis un site d'appel : le panneau ne
    // peut pas etre en desaccord avec l'app, et il ne lui survit pas.
    // pourquoi : docs/decisions/lancement-et-navigation.md § Ce que le second écran reçoit
    DisposableEffect(Unit) { onDispose { SecondScreen.clear() } }

    // Once, before anything else: the app is useless without a ROM folder, and
    // the notification is what keeps the network alive inside the emulator.
    var onboarding by remember { mutableStateOf(!settingsStore.onboardingDone) }

    /**
     * The splash screen, never on the first launch: the onboarding owns that
     * moment. Read straight off the gate's snapshot state rather than copied
     * into a `remember`, so a re-arm from the activity is picked up without
     * waiting for a recomposition the gate cannot itself cause.
     */
    val splashing = SplashGate.pending && settingsStore.onboardingDone


    val ensureVpn = LocalEnsureVpnPermission.current


    /**
     * Owned here, the one place both screens hang off. The revision is what
     * tells the grid the repository's shared cache moved.
     * pourquoi : docs/decisions/lancement-et-navigation.md § Ce qui est hissé au niveau de l'app, et pourquoi
     */
    var libraryFolder by remember { mutableStateOf(romsRepo.savedFolderLabel()) }
    /** Le second dossier, optionnel : null tant que le joueur n'en a pas ajoute. */
    var librarySecondFolder by remember { mutableStateOf(romsRepo.secondFolderLabel()) }
    var libraryScanning by remember { mutableStateOf(false) }
    var libraryCount by remember { mutableStateOf<Int?>(null) }
    var libraryRevision by remember { mutableStateOf(0) }

    fun rescanLibrary() {
        if (libraryScanning) return
        libraryScanning = true
        scope.launch {
            // Off the main thread ALWAYS: walking a SAF tree over a multi-GB
            // ROM has ANR'd there (9e1f9fd), and force skips the cache.
            val roms = withContext(Dispatchers.IO) { romsRepo.scan(force = true) }
            libraryCount = roms.size
            libraryScanning = false
            libraryRevision++
        }
    }

    fun changeLibraryFolder(uri: Uri) {
        romsRepo.setFolder(uri)
        libraryFolder = romsRepo.savedFolderLabel()
        libraryCount = null
        // setFolder drops the cache and this scan refills it: the library would
        // do it anyway, but then the settings page has no count to report.
        rescanLibrary()
    }

    /** Le second dossier s'ajoute au premier ; le refus dit qu'ils etaient le meme. */
    fun changeSecondLibraryFolder(uri: Uri) {
        if (!romsRepo.setSecondFolder(uri)) return
        librarySecondFolder = romsRepo.secondFolderLabel()
        libraryCount = null
        rescanLibrary()
    }

    fun removeSecondLibraryFolder() {
        romsRepo.clearSecondFolder()
        librarySecondFolder = null
        libraryCount = null
        rescanLibrary()
    }

    /**
     * Le numero de la tentative en cours : renoncer l'incremente, ce qui rend
     * orpheline toute tentative en vol sans savoir ou elle en est.
     * pourquoi : docs/decisions/lancement-et-navigation.md § Une tentative en vol ne doit pas téléporter quelqu'un qui a renoncé
     */
    var prepEpoch by remember { mutableIntStateOf(0) }

    fun fail(message: String, back: Screen = Screen.Library) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        screen = back
    }

    fun fail(message: Int, back: Screen = Screen.Library) =
        fail(context.getString(message), back)

    /** The tunnel about to be displaced. Non-null while the prompt is up. */
    var conflict by remember { mutableStateOf<Pair<TunnelHolder, () -> Unit>?>(null) }

    /**
     * Runs [proceed] once [want] can have Android's single VPN slot. Nothing
     * here relies on the system's own revocation, which is silent.
     * pourquoi : docs/decisions/lancement-et-navigation.md § Le créneau VPN unique d'Android
     */
    fun withTunnelSlot(want: TunnelHolder, proceed: () -> Unit) {
        val session = EmufiiWgManager.state.value
        val wfc = WfcManager.state.value
        if (slotIsFree(session, wfc, want)) proceed()
        else conflict = tunnelHolder(session, wfc) to proceed
    }

    /** Frees the slot [held] holds, then runs [proceed]. */
    fun releaseTunnelThen(held: TunnelHolder, proceed: () -> Unit) {
        scope.launch {
            val freed = when (held) {
                TunnelHolder.WFC -> {
                    WfcManager.stop(context)
                    withTimeoutOrNull(TUNNEL_RELEASE_MS) {
                        WfcManager.state.first { it !is WfcState.Active }
                    }
                }
                // No coordinator call to make: we are only here because the app
                // came back without the code. The GC reaps the room on its TTL.
                TunnelHolder.SESSION -> {
                    EmufiiWgManager.stop(context)
                    withTimeoutOrNull(TUNNEL_RELEASE_MS) {
                        EmufiiWgManager.state.first { it is WgState.Idle || it is WgState.Error }
                    }
                }
                TunnelHolder.NONE -> Unit
            }
            if (freed == null) fail(R.string.tunnel_conflict_stuck) else proceed()
        }
    }

    /**
     * Waits for the tunnel, but not forever: null if it errored or took too
     * long. There is no address to wait for; the coordinator assigns it first.
     * pourquoi : docs/decisions/lancement-et-navigation.md § Le créneau VPN unique d'Android
     */
    suspend fun awaitTunnel(): WgState.Online? = withTimeoutOrNull(TUNNEL_TIMEOUT_MS) {
        EmufiiWgManager.state.first { it is WgState.Error || it is WgState.Online } as? WgState.Online
    }

    fun startHostSession(rom: Rom, private: Boolean = false) = withTunnelSlot(TunnelHolder.SESSION) {
        // No screen change on purpose: the launch card is still spinning and
        // carries this leg itself.
        // pourquoi : docs/decisions/lancement-et-navigation.md § Le créneau VPN unique d'Android
        scope.launch {
            // Codes are short and random, so collisions happen; the coordinator
            // rejects duplicates and a fresh draw is all it takes.
            var created: CreatedSession? = null
            var code = ""
            var lastError: Throwable? = null
            for (attempt in 1..CODE_ATTEMPTS) {
                code = SessionCodes.generate()
                val outcome = client.createSession(
                    code, rom.sessionId, rom.displayName, profile.name, profile.id,
                    // Stated, never guessed: 3DS and Switch write titleId the
                    // same way, and this decides whether a VPS room is raised.
                    console = rom.console.wireName,
                    private = private
                )
                created = outcome.getOrNull()
                if (created != null) break
                lastError = outcome.exceptionOrNull()
                // Only a collision is worth another draw: retrying an
                // unreachable coordinator just costs three timeouts.
                val collision = lastError.let { it is CoordinatorError.Http && it.status == 409 }
                if (!collision) break
            }
            val session = created ?: return@launch fail(
                if (lastError is CoordinatorError.Unreachable) R.string.flow_coordinator_unreachable
                else R.string.flow_create_failed
            )

            ensureVpn(
                onGranted = {
                    val epoch = prepEpoch
                    screen = Screen.Preparing(context.getString(R.string.flow_connecting_tunnel))
                    scope.launch {
                        // Claiming the address also publishes host_ip: the
                        // profile id lets the coordinator recognise its host.
                        val hostToken = session.token
                        val info = client.claimAddress(
                            code, EmufiiWgManager.publicKey(context), profile.name, profile.id
                        ).getOrNull() ?: run {
                            client.deleteSession(code, hostToken)
                            return@launch fail(R.string.flow_tunnel_failed)
                        }
                        // DNS for the PS2 only: its keyboard cannot type a dot,
                        // so its emulator dials a name instead of an address.
                        EmufiiWgManager.start(
                            context, code, info,
                            announceDns = rom.console.backend == Backend.ARMSX2
                        )
                        if (awaitTunnel() == null) {
                            client.deleteSession(code, hostToken)
                            EmufiiWgManager.stop(context)
                            return@launch fail(R.string.flow_tunnel_failed)
                        }
                        // The target emulator's port, never a shared default:
                        // Dolphin listens on 2626, the others on 24872.
                        // pourquoi : docs/decisions/lancement-et-navigation.md § Le créneau VPN unique d'Android
                        val netplayPort = rom.console.backend.defaultNetplayPort
                        client.patchSession(code, info.address, netplayPort, hostToken)
                        if (prepEpoch != epoch) return@launch
                        screen = Screen.InSession(
                            Session(
                                code = code,
                                hostIp = info.address,
                                port = netplayPort.toString(),
                                role = Session.Role.HOST,
                                rom = rom.toRef(),
                                token = hostToken,
                                // With a VPS room the host joins like everyone
                                // else: their phone stops being a link.
                                room = session.room
                            )
                        )
                    }
                },
                onDenied = {
                    // The session already exists, undo it rather than leave a room
                    // nobody can enter.
                    scope.launch { client.deleteSession(code, session.token) }
                    fail(R.string.flow_no_vpn_host)
                }
            )
        }
    }

    fun startJoinFlow(rom: RomRef?, code: String) {
        // The PS2 profile gates joining too, not just hosting, and is said
        // before the VPN prompt and before the tunnel slot.
        // pourquoi : docs/decisions/lancement-et-navigation.md § Les refus se disent avant l'invite VPN
        if (rom?.console == Console.PS2 && !Ps2NetworkProfile.isReady(context)) {
            return fail(R.string.launch_ps2_profile_missing, screen)
        }
        withTunnelSlot(TunnelHolder.SESSION) {
            screen = Screen.Preparing(context.getString(R.string.flow_finding_session))
            scope.launch {
                val back = if (rom != null) Screen.Join(rom) else Screen.Finder
                val remote = client.getSession(code).getOrElse { err ->
                    // A missing code is the player's to fix, a silent
                    // coordinator is ours: one message for both misled.
                    return@launch fail(
                        if (err is CoordinatorError.NotFound) R.string.flow_session_not_found
                        else R.string.flow_coordinator_unreachable,
                        back
                    )
                }

                // Only different *titles* are caught: two regional dumps share a
                // title id and are indistinguishable here.
                // pourquoi : docs/decisions/lancement-et-navigation.md § Les refus se disent avant l'invite VPN
                if (rom?.titleIdHex != null && remote.romTitleId != null &&
                    !rom.titleIdHex.equals(remote.romTitleId, ignoreCase = true)
                ) {
                    return@launch fail(
                        context.getString(
                            R.string.flow_wrong_game,
                            remote.romTitle ?: context.getString(R.string.flow_wrong_game_unnamed)
                        ),
                        back
                    )
                }

                ensureVpn(
                    onGranted = {
                        val epoch = prepEpoch
                        screen = Screen.Preparing(context.getString(R.string.flow_connecting_tunnel))
                        scope.launch {
                            // Full is not broken: 503 means full, 429 means
                            // asking too fast.
                            val info = client.claimAddress(
                                code, EmufiiWgManager.publicKey(context), profile.name, profile.id
                            ).getOrElse { err ->
                                val why = when {
                                    err is CoordinatorError.Http && err.status == 503 ->
                                        R.string.flow_session_full
                                    err is CoordinatorError.Http && err.status == 429 ->
                                        R.string.flow_too_many_requests
                                    else -> R.string.flow_tunnel_failed
                                }
                                return@launch fail(why)
                            }
                            EmufiiWgManager.start(
                                context, code, info,
                                announceDns = rom?.console?.backend == Backend.ARMSX2
                            )
                            if (awaitTunnel() == null) {
                                EmufiiWgManager.stop(context)
                                return@launch fail(R.string.flow_tunnel_failed)
                            }
                            // The host publishes its address once its own tunnel is
                            // up, which may be after we got here.
                            val hostIp = remote.hostIp ?: pollHostIp(client, code)
                                ?: run {
                                    EmufiiWgManager.stop(context)
                                    return@launch fail(R.string.flow_host_not_ready)
                                }
                            // A first heartbeat before going in: it brings back
                            // the token that lets us withdraw ourselves later.
                            // pourquoi : docs/decisions/lancement-et-navigation.md § Le créneau VPN unique d'Android
                            val memberToken = client.heartbeat(code, profile.id, profile.name)
                                .getOrNull()?.memberToken
                            if (prepEpoch != epoch) return@launch
                            screen = Screen.InSession(
                                Session(
                                    code = code,
                                    hostIp = hostIp,
                                    // The host's published port is
                                    // authoritative; the fallback follows the emulator.
                                    port = (
                                        remote.port
                                            ?: rom?.console?.backend?.defaultNetplayPort
                                            ?: DEFAULT_PORT
                                        ).toString(),
                                    role = Session.Role.GUEST,
                                    rom = rom,
                                    token = memberToken,
                                    room = remote.room
                                )
                            )
                        }
                    },
                    onDenied = {
                        scope.launch { client.leaveSession(code, profile.id, token = null) }
                        fail(R.string.flow_no_vpn_guest)
                    }
                )
            }
        }
    }

    /** Join a found session. Not owning the ROM is fine: it just cannot launch. */
    fun joinKnownSession(code: String, romTitleId: String?, romTitle: String? = null) {
        scope.launch {
            val rom = withContext(Dispatchers.IO) {
                val library = romsRepo.cachedOrScan()
                library.firstOrNull { r ->
                    romTitleId != null && r.sessionId.equals(romTitleId, ignoreCase = true)
                }
                // Title as a last resort: two regional PSP dumps carry two disc
                // ids, and refusing on that would be wrong.
                    ?: library.firstOrNull { r ->
                        romTitle != null && r.displayName.equals(romTitle, ignoreCase = true)
                    }
            }
            startJoinFlow(rom?.toRef(), code)
        }
    }

    /**
     * Presence, so friends holding our code can see we're around. Silent while
     * in a session; its first call on leaving clears "in a game".
     * pourquoi : docs/decisions/lancement-et-navigation.md § Ce qui est hissé au niveau de l'app, et pourquoi
     */
    val inSession = screen is Screen.InSession
    LaunchedEffect(profile.id, profile.name, inSession) {
        if (inSession) return@LaunchedEffect
        while (true) {
            client.announcePresence(profile.id, profile.name, inSession = false)
            delay(PRESENCE_MS)
        }
    }

    /**
     * Who is around, asked once for the whole app: presence is not the friends
     * screen's private business.
     * pourquoi : docs/decisions/lancement-et-navigation.md § Ce qui est hissé au niveau de l'app, et pourquoi
     */
    val friends by friendStore.friends.collectAsState()
    val watcher = remember { FriendWatcher(context, client) }
    val friendStatuses by watcher.statuses.collectAsState()
    val friendCodes = friends.map { it.code }
    LaunchedEffect(friendCodes) { watcher.run(friendCodes) }

    // Les libelles d'etat des amis, resolus **ici**, du cote qui parle la langue
    // de l'interface : la fenetre du panneau a son propre contexte d'affichage.
    // pourquoi : docs/decisions/second-ecran.md § La liste d'amis descend au dos, les deux cartes restent devant
    val friendPlayingUnknown = stringResource(R.string.friends_playing_unknown)
    // Le nom de repli, resolu ici aussi : `playerDisplayName` est composable, et
    // un effet n'est pas une composition.
    val friendUnnamed = stringResource(R.string.profile_default_name)
    val friendOnline = stringResource(R.string.friends_online)
    val friendOffline = stringResource(R.string.friends_offline)

    LaunchedEffect(screen, friends, friendStatuses) {
        if (screen is Screen.Friends) {
            SecondScreen.publish(
                SecondScreenModel.Friends(
                    entries = friends
                        // Le meme ordre que l'ecran de face : en jeu, puis en
                        // ligne, puis les autres par nom. Deux ordres pour une
                        // meme liste, ce serait deux listes.
                        .sortedWith(
                            compareByDescending<Friend> {
                                friendStatuses[it.code]?.inSession == true
                            }
                                .thenByDescending { friendStatuses[it.code]?.online == true }
                                .thenBy { (it.name ?: it.displayCode).lowercase() }
                        )
                        .map { friend ->
                            val status = friendStatuses[friend.code]
                            PanelFriend(
                                name = friend.name?.takeIf { it.isNotBlank() }
                                    ?.takeIf { it != Profile.DEFAULT_NAME }
                                    ?: friend.displayCode.ifBlank { friendUnnamed },
                                line = when {
                                    status?.inSession == true ->
                                        status.romTitle ?: friendPlayingUnknown
                                    status?.online == true -> friendOnline
                                    else -> friendOffline
                                },
                                online = status?.online == true,
                                inSession = status?.inSession == true,
                                onRemove = { friendStore.remove(friend.code) },
                            )
                        }
                )
            )
            return@LaunchedEffect
        }
        SecondScreen.publish(
            (screen as? Screen.InSession)?.session?.let { active ->
                SecondScreenModel.InSession(
                    code = active.code,
                    role = active.role,
                    console = active.console,
                    gameTitle = active.rom?.displayName,
                    // Les memes valeurs que l'ecran de face, par la meme
                    // definition : le panneau recevait `hostIp` brut, donc une
                    // session Eden avec salon publiait au dos une adresse que
                    // l'emulateur n'attend pas.
                    // pourquoi : docs/decisions/session.md § Ce que le panneau arrière porte, l'écran de face ne le redit pas
                    hostAddress = active.shownAddress,
                    port = active.shownPort,
                )
            } ?: SecondScreenModel.Idle
        )
    }

    var alert by remember { mutableStateOf<FriendEvent?>(null) }
    LaunchedEffect(watcher) {
        watcher.alerts.collect { event ->
            alert = event
            // Mirrored onto the rear panel, the card above unchanged: a single
            // screen must not lose an alert because a panel exists elsewhere.
            PanelFeed.post(friendNoteText(context, event), PanelFeed.Kind.FRIEND)
        }
    }

    // A tapped notification asks for a screen, and it is honoured here rather
    // than in the activity: this is the only place that knows what a screen is.
    val pendingOpen by Notifications.PendingOpen.target.collectAsState()
    LaunchedEffect(pendingOpen) {
        if (Notifications.PendingOpen.consume() == Notifications.OPEN_FRIENDS) {
            onProfilePage = false
            screen = Screen.Friends
        }
    }

    /**
     * The watch that keeps running once the app is closed. Scheduling is
     * idempotent, and an app with nothing to watch schedules nothing.
     * pourquoi : docs/decisions/lancement-et-navigation.md § Ce qui est hissé au niveau de l'app, et pourquoi
     */
    val notifyFriends by settingsStore.notifyFriends.collectAsState()
    val notifyUpdates by settingsStore.notifyUpdates.collectAsState()
    // `foreground` is a key, not a decoration: the notification permission is
    // granted outside anything this app can observe. Found on the Thor.
    val foreground by AppForeground.visible.collectAsState()
    LaunchedEffect(notifyFriends, notifyUpdates, friends.size, foreground) {
        if (!foreground) return@LaunchedEffect
        Notifications.ensureChannels(context)
        FriendWatchJob.sync(context, notifyFriends, notifyUpdates)
    }

    if (onboarding) {
        // The first launch spends the token without showing the screen, or the
        // logo lands right after onboarding, where the library is expected.
        LaunchedEffect(Unit) { SplashGate.pending = false }
        OnboardingScreen(
            initialName = profile.name,
            onSetName = { profileStore.setName(it) },
            onPickFolder = { uri -> changeLibraryFolder(uri) },
            onSetArtworkKey = { settingsStore.setSteamGridDbKey(it) },
            onDone = {
                settingsStore.onboardingDone = true
                onboarding = false
            }
        )
        return
    }

    /**
     * Le prechargement tourne, et l'app se compose **derriere** le logo : quand
     * il s'en va, il decouvre une image deja finie.
     * pourquoi : docs/decisions/lancement-et-navigation.md § Le préchargement tourne, et l'app se compose derrière lui
     */
    var libraryReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val roms = withContext(Dispatchers.IO) {
            runCatching { romsRepo.cachedOrScan() }.getOrDefault(emptyList())
        }
        val warm = launch(Dispatchers.IO) {
            // Les noms que les fichiers chiffrés ne donnent pas.
            runCatching { GameTitles.refresh(context, roms) }
            // Les pastilles, pour qu'elles soient sur la première image.
            runCatching { CompatCheck.refresh(context) }
            // Les paquets d'émulateurs et leurs icônes : sept requêtes au
            // système et autant de tramages, payés une fois ici plutôt qu'à
            // l'ouverture de la page des consoles.
            runCatching { allEmulators(context) }
            // Et les jaquettes : les index de dossier, les adresses, puis le
            // décodage des deux premiers écrans de grille.
            runCatching { ArtworkPreload.warm(context, roms) }
        }
        withTimeoutOrNull(PRELOAD_MS) { warm.join() }
        libraryReady = true
    }

    /**
     * What "back" means, screen by screen — the gap that used to close the app.
     *
     * Null on the library (the root), and null during preparation and in
     * session, where back is nonetheless *consumed* rather than passed up.
     * pourquoi : docs/decisions/lancement-et-navigation.md § Ce que « retour » veut dire, écran par écran
     */
    val goBack: (() -> Unit)? = when (screen) {
        Screen.Library -> null
        is Screen.Preparing -> null
        is Screen.InSession -> null
        Screen.ProfileAndSettings -> ({ onProfilePage = false; screen = Screen.Library })
        else -> ({ screen = Screen.Library })
    }
    // Live everywhere except on the library, which is the root: there, and only
    // there, the gesture belongs to the system.
    BackHandler(enabled = screen != Screen.Library) { goBack?.invoke() }

    /**
     * The compatibility ratings: cache lu *synchroniquement* pour que les
     * pastilles soient sur la premiere image, reseau derriere, et jamais de
     * remplacement par moins.
     * pourquoi : docs/decisions/lancement-et-navigation.md § Ce qui est hissé au niveau de l'app, et pourquoi
     */
    var compat by remember { mutableStateOf(CompatCheck.cached(context)) }
    LaunchedEffect(Unit) { compat = CompatCheck.refresh(context) }

    /** The editorial catalogue: same pattern, read by one page, so nothing waits. */
    var gameMeta by remember { mutableStateOf(MetaCheck.cached(context)) }
    LaunchedEffect(Unit) { gameMeta = MetaCheck.refresh(context) }

    CompositionLocalProvider(
        LocalCompatDb provides compat,
        LocalGameMetaDb provides gameMeta,
    ) {
    when (val s = screen) {
        Screen.Library -> LibraryScreen(
            profile = profile,
            onOpenProfile = { onProfilePage = true; screen = Screen.ProfileAndSettings },
            onOpenFriends = { screen = Screen.Friends },
            onOpenFinder = { screen = Screen.Finder },
            // DS online play shares nothing with the session flow, no code to
            // create, none to join, so both entry points lead to the same place.
            onCreate = { rom, private ->
                if (rom.console.backend == Backend.MELONDS_WFC) screen = Screen.Wfc(rom)
                else startHostSession(rom, private)
            },
            onJoinWith = { rom ->
                if (rom.console.backend == Backend.MELONDS_WFC) screen = Screen.Wfc(rom)
                else screen = Screen.Join(rom.toRef())
            },
            // No session, no tunnel: the player picks a server inside PPSSPP.
            // pourquoi : docs/decisions/lancement-et-navigation.md § Deux routes qui ne sont pas des sessions
            onPlayPublic = { rom -> screen = Screen.PspOnline(rom) },
            onFolderPicked = { uri -> changeLibraryFolder(uri) },
            libraryRevision = libraryRevision
        )
        is Screen.PspOnline -> PspOnlineScreen(
            rom = (screen as Screen.PspOnline).rom,
            onBack = { screen = Screen.Library }
        )
        Screen.Finder -> SessionFinderScreen(
            client = client,
            romsRepo = romsRepo,
            onBack = { screen = Screen.Library },
            onJoin = { open -> joinKnownSession(open.code, open.romTitleId, open.romTitle) }
        )
        Screen.Friends -> FriendsScreen(
            profile = profile,
            friendStore = friendStore,
            statuses = friendStatuses,
            onJoin = { code, romTitleId, romTitle -> joinKnownSession(code, romTitleId, romTitle) },
            onBack = { screen = Screen.Library }
        )
        is Screen.Preparing -> PreparingScreen(
            label = s.label,
            onGiveUp = {
                // Le compteur d'abord, le tunnel ensuite : la tentative en vol
                // devient orpheline avant qu'on lui retire le sol.
                prepEpoch++
                EmufiiWgManager.stop(context)
                screen = Screen.Library
            }
        )
        is Screen.Join -> JoinScreen(
            rom = s.rom,
            client = client,
            onBack = { screen = Screen.Library },
            onSubmitCode = { code -> startJoinFlow(s.rom, code) }
        )
        Screen.ProfileAndSettings -> SettingsScreen(
            profile = profile,
            profileStore = profileStore,
            friendStore = friendStore,
            settingsStore = settingsStore,
            romsRepo = romsRepo,
            libraryFolder = libraryFolder,
            librarySecondFolder = librarySecondFolder,
            libraryScanning = libraryScanning,
            libraryCount = libraryCount,
            onFolderPicked = { uri -> changeLibraryFolder(uri) },
            onSecondFolderPicked = { uri -> changeSecondLibraryFolder(uri) },
            onSecondFolderRemoved = { removeSecondLibraryFolder() },
            onRescan = { rescanLibrary() },
            onBack = {
                onProfilePage = false
                screen = Screen.Library
            }
        )
        is Screen.Wfc -> WfcScreen(
            rom = s.rom,
            onRequestTunnelSlot = { proceed -> withTunnelSlot(TunnelHolder.WFC, proceed) },
            onBack = { screen = Screen.Library }
        )
        is Screen.InSession -> SessionScreen(
            session = s.session,
            profile = profile,
            client = client,
            onSessionEnded = {
                scope.launch { EmufiiWgManager.stop(context) }
                fail(R.string.flow_host_closed)
            },
            onLeave = {
                // The plan outlives the process on purpose; it must not outlive
                // the session that justified it.
                NetplayAutomation.clear(PlanStore(context))
                scope.launch {
                    if (s.session.role == Session.Role.HOST) {
                        client.deleteSession(s.session.code, s.session.token)
                    } else {
                        // Drop out of the member list at once, so the host sees
                        // the departure now rather than at the TTL.
                        client.leaveSession(s.session.code, profile.id, s.session.token)
                    }
                    EmufiiWgManager.stop(context)
                }
                screen = Screen.Library
            }
        )
    }
    }

    // **Le logo, par-dessus tout le reste et en dernier.**
    //
    // Opaque et plein écran : rien de ce qui est composé dessous ne se voit, et
    // tout y est déjà mesuré et peint quand il s'efface.
    if (splashing) {
        SplashScreen(
            ready = libraryReady,
            onDone = { SplashGate.pending = false }
        )
    }

    // Over every screen, and before the conflict dialog in source order: the
    // dialog covers it, which is the right way round when both happen.
    FriendAlert(
        event = alert,
        onOpen = { alert = null; screen = Screen.Friends },
        onDismiss = { alert = null }
    )

    // Over whatever screen asked: the answer decides whether that screen's
    // action happens at all, so it belongs outside the when.
    conflict?.let { (held, proceed) ->
        TunnelConflictDialog(
            held = held,
            onConfirm = {
                conflict = null
                releaseTunnelThen(held, proceed)
            },
            onDismiss = { conflict = null }
        )
    }
}

/**
 * A friend event as one sentence, for the rear panel. Deliberately the strings
 * the Android notification already uses.
 * pourquoi : docs/decisions/lancement-et-navigation.md § Ce que le second écran reçoit
 */
private fun friendNoteText(context: android.content.Context, event: FriendEvent): String {
    val name = event.name ?: context.getString(R.string.notify_friend_unnamed)
    return when (event) {
        is FriendEvent.CameOnline -> context.getString(R.string.notify_friend_online, name)
        is FriendEvent.StartedPlaying -> event.game
            ?.let { context.getString(R.string.notify_friend_playing, name, it) }
            ?: context.getString(R.string.notify_friend_in_game, name)
    }
}

/**
 * Ce qu'on accorde aux prechauffages : quatre secondes gratuites, deux de plus
 * pour un demarrage a froid. Le plafond du splash reste au-dessus.
 * pourquoi : docs/decisions/lancement-et-navigation.md § Le préchargement tourne, et l'app se compose derrière lui
 */
private const val PRELOAD_MS = 6_000L

