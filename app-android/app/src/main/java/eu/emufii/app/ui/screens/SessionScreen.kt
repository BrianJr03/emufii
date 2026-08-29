package eu.emufii.app.ui.screens

import eu.emufii.app.ui.sounded
import android.icu.text.ListFormatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import eu.emufii.app.ui.ActionShape
import eu.emufii.app.ui.CONFIRM_KEYS
import androidx.compose.foundation.layout.heightIn
import eu.emufii.app.ui.theme.PillShape
import eu.emufii.app.ui.controlRing
import eu.emufii.app.R
import eu.emufii.app.azahar.AzaharLauncher
import eu.emufii.app.azahar.LaunchResult
import eu.emufii.app.azahar.NetplayAutomation
import eu.emufii.app.azahar.NetplayPlan
import eu.emufii.app.azahar.NetplayProgress
import eu.emufii.app.azahar.PlanStore
import eu.emufii.app.dolphin.DolphinLauncher
import eu.emufii.app.dolphin.DolphinTarget
import eu.emufii.app.ps2.Ps2Launcher
import eu.emufii.app.ps2.Ps2GameSettings
import eu.emufii.app.ps2.Ps2NetworkProfile
import eu.emufii.app.ps2.Ps2ProvisioningPlan
import eu.emufii.app.ps2.Ps2Target
import eu.emufii.app.eden.EdenLauncher
import eu.emufii.app.netplay.NetplayNames
import eu.emufii.app.psp.HOST_SENTINEL
import eu.emufii.app.psp.PpssppConfigStore
import eu.emufii.app.psp.PpssppLauncher
import eu.emufii.app.library.Backend
import eu.emufii.app.network.CoordinatorClient
import eu.emufii.app.network.CoordinatorError
import eu.emufii.app.network.Member
import eu.emufii.app.profile.Profile
import eu.emufii.app.profile.playerDisplayName
import eu.emufii.app.session.Session
import eu.emufii.app.ui.components.WarnIcon
import eu.emufii.app.ui.components.AvatarStack
import eu.emufii.app.ui.components.EmufiiScaffold
import eu.emufii.app.ui.components.LocalScaffoldFocus
import eu.emufii.app.ui.components.GhostButton
import eu.emufii.app.ui.components.SectionHeader
import eu.emufii.app.ui.components.SoftCard
import eu.emufii.app.ui.components.softCardFill
import eu.emufii.app.ui.components.padEntry
import androidx.compose.foundation.layout.BoxWithConstraints
import eu.emufii.app.library.Rom
import eu.emufii.app.ui.components.CrossIcon
import eu.emufii.app.ui.components.PadDialog
import eu.emufii.app.ui.components.PadDialogText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import eu.emufii.app.library.RomsRepository
import eu.emufii.app.secondscreen.PanelStep
import eu.emufii.app.ui.components.RomArtwork
import eu.emufii.app.secondscreen.SecondScreen
import eu.emufii.app.secondscreen.rememberPresentationDisplay
import eu.emufii.app.settings.SettingsStore
import eu.emufii.app.ui.copyToClipboard
import eu.emufii.app.ui.theme.Coral
import eu.emufii.app.ui.theme.ErrorDark
import eu.emufii.app.ui.theme.ErrorLight
import eu.emufii.app.ui.theme.GoodDark
import eu.emufii.app.ui.theme.GoodLight
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.socket
import eu.emufii.app.ui.theme.plate
import eu.emufii.app.ui.theme.LocalEmufiiOledTheme
import eu.emufii.app.ui.LocalRingTone
import eu.emufii.app.ui.RingTone
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import eu.emufii.app.ui.theme.edgeColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import eu.emufii.app.ui.tap
import eu.emufii.app.ui.Sfx

/** L'erreur tiree vers le corail, la coupe du fond courant. */
@Composable
private fun danger() = if (LocalEmufiiDarkTheme.current) ErrorDark else ErrorLight

/** Le bon token, tire vers le turquoise. */
@Composable
private fun good() = if (LocalEmufiiDarkTheme.current) GoodDark else GoodLight

/** La coupe corail lisible sur le fond courant (texte). */
@Composable
private fun coralText() = if (LocalEmufiiDarkTheme.current) Coral.darkBright else Coral.ink

@Composable
fun SessionScreen(
    session: Session,
    profile: Profile,
    client: CoordinatorClient,
    onLeave: () -> Unit,
    onSessionEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val azahar = remember { AzaharLauncher(context) }
    val eden = remember { EdenLauncher(context) }
    val ppsspp = remember { PpssppLauncher(context) }
    val pspAutomatic = remember(session.code, session.rom) {
        val rom = session.rom
        rom != null && PpssppConfigStore(context).canApply(
            rom.productCode,
            rom.filename,
            rom.displayName,
        )
    }
    // First frame with what the library scan already knows, then the disc
    // itself: a library last scanned by an older build carries no ELF CRC, and
    // without this second look the direct path would stay hidden until a
    // rescan nobody tells the player to run.
    val ps2Automatic by produceState(
        initialValue = session.rom != null && session.backend == Backend.ARMSX2 &&
            Ps2GameSettings.canConfigure(context, session.rom),
        session.code, session.rom
    ) {
        value = session.rom != null && session.backend == Backend.ARMSX2 &&
            Ps2GameSettings.canConfigureNow(context, session.rom)
    }
    var status by remember { mutableStateOf<String?>(null) }
    var members by remember { mutableStateOf<List<Member>>(emptyList()) }
    // Our own name in the list for *this* session, see `Heartbeat.memberHandle`.
    // Re-read on every beat rather than kept from the first one: if the
    // coordinator let us expire and signs us up again, the handle changes too.
    var myHandle by remember { mutableStateOf<String?>(null) }

    /** Has the room step been run? Gates the launch button, see the pair below. */
    var netplayPrepared by remember(session.code) { mutableStateOf(false) }

    /**
     * Did the automation get all the way to a joined room? Latched, not read
     * off the progress flow, which starting the game resets.
     * pourquoi : docs/decisions/session.md § Deux preuves qu'un salon existe, et la seconde est assumée plus faible
     */
    var netplayDone by remember(session.code) { mutableStateOf(false) }

    /** PPSSPP was opened for the manual fallback used when automatic setup is unavailable. */
    var pspOpened by remember(session.code) { mutableStateOf(false) }
    val netplayProgress by NetplayAutomation.progress.collectAsState()
    LaunchedEffect(netplayProgress) {
        if (netplayProgress is NetplayProgress.Done) netplayDone = true
    }

    /** The coordinator has stopped answering us. Says so; changes nothing else. */
    var offline by remember { mutableStateOf(false) }

    /**
     * Does the host's room already exist in the emulator? True from the start,
     * which is what an ignorant coordinator answers and what holds for a host.
     * pourquoi : docs/decisions/session.md § L'ordre hôte puis invité n'est pas un détail de confort
     */
    var hostReady by remember(session.code) { mutableStateOf(true) }

    /**
     * Does this session have a host step, and is it ours? An upstream room
     * changes nothing: the room and the *game's* session are not the same thing.
     * pourquoi : docs/decisions/session.md § L'ordre hôte puis invité n'est pas un détail de confort
     */
    val hasHostStep = session.backend.hasNetplay
    val weHostTheRoom = hasHostStep && session.role == Session.Role.HOST

    /** The guest is waiting on their host: setting up would only lead to "not found". */
    val waitingForHost = hasHostStep && session.role == Session.Role.GUEST && !hostReady

    var automationOn by remember { mutableStateOf(azahar.isNetplayAutomationEnabled()) }

    /**
     * How many times we have come back into Emufii from outside: the second,
     * weaker proof that a host made their room.
     * pourquoi : docs/decisions/session.md § Deux preuves qu'un salon existe, et la seconde est assumée plus faible
     */
    var returns by remember(session.code) { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                automationOn = azahar.isNetplayAutomationEnabled()
                // The moment to notice the automation was never heard from:
                // silence has a cause the player can act on.
                // pourquoi : docs/decisions/session.md § Deux preuves qu'un salon existe, et la seconde est assumée plus faible
                if (NetplayAutomation.neverStarted()) {
                    NetplayAutomation.report(
                        NetplayProgress.Failed(context.getString(R.string.netplay_automation_silent))
                    )
                }
                returns++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val scope = rememberCoroutineScope()

    // Publish our room once it exists; two signals count as proof.
    // pourquoi : docs/decisions/session.md § Deux preuves qu'un salon existe, et la seconde est assumée plus faible
    LaunchedEffect(weHostTheRoom, netplayDone, returns) {
        if (!weHostTheRoom) return@LaunchedEffect
        if (netplayDone || (netplayPrepared && returns > 0)) {
            client.setHostReady(session.code, true, session.token)
        }
    }

    /**
     * Step 1, pressed. One definition for both layouts: the day one of the two
     * drifted, half the screens changed behaviour without anyone noticing.
     */
    val onNetplayStep: () -> Unit = fun() {
        // Only an image whose boot ELF cannot be read reaches this branch, and
        // it needs the one legacy global assignment per-game files avoid.
        // pourquoi : docs/decisions/session.md § Ce que chaque backend reçoit au lancement
        if (session.backend == Backend.ARMSX2 && !ps2Automatic) {
            val receipt = Ps2NetworkProfile.receipt(context)
            if (receipt != null && !receipt.assigned) {
                if (!automationOn) {
                    status = context.getString(R.string.session_ps2_fallback_accessibility)
                    return
                }
                status = when (val result = Ps2Launcher(context).openForProvisioning(
                    Ps2ProvisioningPlan(
                        receipt.cardName,
                        receipt.cardSha256,
                        receipt.sourceCardForSlot2,
                    )
                )) {
                    LaunchResult.Success -> context.getString(R.string.session_ps2_fallback_assigning)
                    LaunchResult.NotInstalled -> context.getString(R.string.err_not_installed, "ARMSX2")
                    is LaunchResult.Error -> context.getString(R.string.err_generic, result.message)
                    is LaunchResult.NoNetplayUi -> context.getString(R.string.err_not_installed, "ARMSX2")
                }
                return
            }
        }
        netplayDone = false
        // Setting up again destroys the previous room: putting the guests back
        // in the waiting state beats letting them run at a room that is gone.
        if (weHostTheRoom && netplayPrepared) {
            scope.launch { client.setHostReady(session.code, false, session.token) }
        }
        status = session.prepareNetplay(context, azahar, eden, profile.name)
        if (status == null) netplayPrepared = true
    }

    /** ARMSX2's direct path performs its former two steps behind one launch. */
    /**
     * Vrai des que l'emulateur est parti. **Pour le panneau arriere avant tout** :
     * l'ecran de face disparait derriere l'emulateur a la seconde ou l'on presse,
     * et le panneau restait sur une etape que rien ne distinguait d'une etape
     * jamais pressee. Une action dont on ne voit pas l'effet est une action dont
     * on doute, et le premier reflexe est de la presser une seconde fois.
     * pourquoi : docs/decisions/second-ecran.md § Un panneau qui affirme le faux est une panne
     */
    var launched by remember(session.code) { mutableStateOf(false) }
    val onLaunchStep: () -> Unit = fun() {
        scope.launch {
            status = session.launch(
                context, azahar, eden, ppsspp,
                onPs2Started = {
                    if (ps2Automatic) {
                        netplayPrepared = true
                        netplayDone = true
                    }
                },
                onLaunched = { launched = true }
            )
        }
    }


    // Presence: announce ourselves on a timer, and read back who else is here.
    // The same loop serves both roles, the host mostly cares about the list,
    // the guest about being counted, so there's one loop, not two.
    LaunchedEffect(session.code, profile.id) {
        var gone = 0
        var mute = 0
        while (true) {
            client.heartbeat(session.code, profile.id, profile.name)
                .onSuccess { beat -> beat.memberHandle?.let { myHandle = it } }
            client.getSession(session.code)
                .onSuccess {
                    members = it.members
                    hostReady = it.hostReady
                    gone = 0; mute = 0; offline = false
                }
                .onFailure { err ->
                    if (err is CoordinatorError.NotFound) gone++ else mute++
                }

            // Only a coordinator that *answers* 404 proves the room is gone; a
            // silent one proves only that we cannot reach it.
            // pourquoi : docs/decisions/session.md § Seul un 404 prouve qu'un salon est fermé
            if (gone >= MAX_PRESENCE_MISSES && session.role == Session.Role.GUEST) {
                onSessionEnded()
                return@LaunchedEffect
            }
            if (mute >= MAX_PRESENCE_MISSES) offline = true
            delay(PRESENCE_MS)
        }
    }

    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Both comparisons, not one: the handle is what an up-to-date coordinator
    // returns, the friend code what the old one returned. Keeping both avoids
    // seeing yourself turn up as your own neighbour until the server is
    // deployed.
    val others = members.filter { it.id != myHandle && it.id != profile.id }

    val configuration = LocalConfiguration.current
    val landscape = configuration.screenWidthDp > configuration.screenHeightDp

    // Hoisted, because a value computed inside one column would only be true
    // on one side.
    // pourquoi : docs/decisions/session.md § L'adresse affichée est celle qu'on doit taper, jamais une autre
    val psp = session.backend == Backend.PPSSPP
    // With a VPS room nobody hosts, so the host's address is the address of
    // nothing and must not appear under the word "host".
    // pourquoi : docs/decisions/session.md § L'adresse affichée est celle qu'on doit taper, jamais une autre
    val room = session.room
    val shownAddress = session.shownAddress
    val shownPort = session.shownPort
    val addressLabel = stringResource(
        when {
            room != null -> R.string.session_room_address
            psp -> R.string.session_psp_address
            else -> R.string.session_host_address
        }
    )
    // Le code, et lui seul, se copie encore : c'est ce qu'on envoie a un ami
    // dans une autre application. L'adresse et le port ne se copient plus —
    // Emufii les ecrit dans l'emulateur, et le panneau arriere les affiche tous
    // les deux a la fois, ce que le presse-papier ne sait pas faire.
    // pourquoi : docs/decisions/session.md § Copier l'adresse n'a plus de sens depuis qu'Emufii la remplit
    val onCopyCode = {
        copyToClipboard(context, "Emufii", session.code)
        status = context.getString(R.string.common_copied, session.code)
    }

    // Le panneau arriere est-il vraiment allume ? Le reglage ne suffit pas :
    // l'appareil peut n'avoir qu'un ecran. La meme regle que
    // [secondScreenWanted], vue depuis un ecran qui sait deja qu'il est en
    // session.
    // pourquoi : docs/decisions/session.md § Ce que le panneau arrière porte, l'écran de face ne le redit pas
    val panelDisplay by rememberPresentationDisplay()
    val panelWanted by remember(context) { SettingsStore.get(context).secondScreen }
        .collectAsState()
    val panelLive = panelWanted && panelDisplay != null

    // La session ne porte qu'une **reference** de ROM : ni icone, ni couleur
    // extraite. La jaquette se retrouve donc dans la bibliotheque, par son URI,
    // dans le cache que l'app a deja chauffe au demarrage — hors du fil
    // principal, et sans jamais declencher de scan a elle seule.
    // pourquoi : docs/decisions/session.md § Le jeu s'affiche dans le vide que le panneau a laissé
    var sessionArt by remember(session.code) { mutableStateOf<Rom?>(null) }
    LaunchedEffect(session.rom?.uri) {
        val uri = session.rom?.uri ?: return@LaunchedEffect
        sessionArt = withContext(Dispatchers.IO) {
            runCatching { RomsRepository(context).cachedOrScan() }
                .getOrDefault(emptyList())
                .firstOrNull { it.uri == uri }
        }
    }

    // Les etapes, resolues **une fois** et servies aux deux ecrans : les
    // libelles voyagent deja traduits.
    // pourquoi : docs/decisions/second-ecran.md § Ce qui voyage au panneau voyage déjà résolu
    // pourquoi : docs/decisions/second-ecran.md § Le panneau prend les etapes, parce qu'il est tactile
    val showNetplayStep = session.backend.hasNetplay && !ps2Automatic
    val showPspStep = session.backend == Backend.PPSSPP && !pspAutomatic
    val netplayLabel = stringResource(
        when {
            waitingForHost -> R.string.session_netplay_waiting_host
            netplayDone -> R.string.session_netplay_done
            netplayPrepared -> R.string.session_netplay_again
            else -> R.string.session_netplay_open
        },
        session.backend.emulatorName
    )
    val pspLabel = stringResource(
        if (pspOpened) R.string.session_psp_setup_again else R.string.session_psp_setup
    )
    val launchedLabel = stringResource(
        if (session.backend.hasNetplay && !ps2Automatic) R.string.session_launch_done_step2
        else R.string.session_launch_done
    )
    val launchLabel = launchLabel(
        session = session,
        netplayPrepared = netplayPrepared,
        directPs2 = ps2Automatic,
        waitingForHost = ps2Automatic && waitingForHost
    )
    val panelSteps = buildList {
        if (showNetplayStep) {
            add(
                PanelStep(
                    label = netplayLabel,
                    done = netplayDone,
                    enabled = session.rom != null && !waitingForHost,
                    onPress = onNetplayStep
                )
            )
        }
        if (showPspStep) {
            add(
                PanelStep(
                    label = pspLabel,
                    done = pspOpened,
                    enabled = true,
                    onPress = {
                        status = openPpssppForSetup(context, ppsspp) { pspOpened = true }
                    }
                )
            )
        }
        add(
            PanelStep(
                // Une fois partie, l'etape garde sa place et change de visage :
                // elle reste pressable, parce que revenir au jeu depuis le
                // panneau est exactement ce qu'on veut apres avoir bascule
                // ailleurs.
                label = if (launched) launchedLabel else launchLabel,
                done = launched,
                enabled = launchEnabled(
                    session = session,
                    netplayPrepared = netplayPrepared,
                    directPs2 = ps2Automatic,
                    waitingForHost = ps2Automatic && waitingForHost
                ),
                onPress = onLaunchStep
            )
        )
    }
    // Les lambdas appartiennent a cette composition : l'ecran qui les pose doit
    // les retirer en partant, sinon le panneau garde une session morte sous le
    // doigt.
    DisposableEffect(panelLive, panelSteps) {
        SecondScreen.publishSteps(if (panelLive) panelSteps else emptyList())
        onDispose { SecondScreen.publishSteps(emptyList()) }
    }

    // Le pilotage des etapes du panneau arriere. Le focus ne traverse pas les
    // fenetres : la manette de l'ecran de face conduit donc un curseur virtuel,
    // publie dans [SecondScreen], que le panneau dessine de son cote — le meme
    // parti pris que R pour tourner la page.
    // pourquoi : docs/decisions/second-ecran.md § R tourne la page depuis les deux écrans
    val panelCursor by SecondScreen.stepCursor.collectAsState()

    // Le retour **ferme** la session : une croix rouge, et une question avant
    // de couper le tunnel. Il y avait deux controles pour un seul geste.
    // pourquoi : docs/decisions/session.md § Le retour ferme la session, donc il porte une croix et il demande
    var confirmingLeave by remember { mutableStateOf(false) }

    // Le domaine social : le curseur manette y devient corail.
    // pourquoi : docs/decisions/theme-duotone-shelves.md § FOCUS MANETTE
    CompositionLocalProvider(LocalRingTone provides RingTone.CORAL) {
    EmufiiScaffold(
        title = if (session.role == Session.Role.HOST) stringResource(R.string.session_mine) else stringResource(R.string.session_joined),
        modifier = modifier,
        onBack = { confirmingLeave = true },
        backIcon = { CrossIcon(size = 20.dp, color = danger()) },
        // En paysage, « quitter » monte dans l'en-tete, et les 60 dp rendus au
        // volet gauche sont ce qui manquait a l'adresse. La pastille du code ne
        // parait que si le panneau ne la porte pas deja.
        // pourquoi : docs/decisions/session.md § Ce que le panneau porte, l'écran de face le rend en place
        trailing = if (landscape && !panelLive) {
            { SessionCodeChip(code = session.code, onCopy = onCopyCode) }
        } else null,
        // Both panes fit on screen: nothing rises under the header, and the
        // fade margin was an empty band between the title and the cards.
        contentScrolls = !landscape
    ) { topPadding ->
        // Le pilote vit **dans** la coquille : rendre le curseur au-dela de la
        // premiere etape, c'est le poser sur la croix de l'en-tete, et le
        // requester de cette croix ne se lit que derriere le fournisseur de la
        // coquille. Quand les commandes sont au panneau, la page n'a plus de
        // premier controle — le pilote est donc aussi le `first` de la coquille,
        // pour que Bas depuis la croix redescende jusqu'a lui.
        val scaffoldFocus = LocalScaffoldFocus.current

        /**
         * Poser le curseur des que les etapes existent, **sans attendre que le
         * pilote prenne le focus** — il ne le prend jamais. La bonne condition
         * n'a jamais eu de rapport avec le focus.
         * pourquoi : docs/decisions/second-ecran.md § Le curseur du panneau ne dépend pas du focus, qui n'arrive jamais
         */
        LaunchedEffect(panelLive, panelSteps) {
            if (panelLive &&
                panelSteps.isNotEmpty() &&
                SecondScreen.stepCursor.value == null
            ) {
                SecondScreen.selectStep(0)
            }
        }

        // **Un seul `focusRequester`, et c'est celui de la coquille** : deux
        // empiles et le noeud ne prenait jamais le focus. Le pilote *recoit* les
        // touches sans etre designe, et une destination n'a qu'une adresse.
        // pourquoi : docs/decisions/session.md § Un seul `focusRequester` par nœud, et c'est celui de la coquille
        val pilotFocus = remember(scaffoldFocus) { scaffoldFocus?.first ?: FocusRequester() }

        /**
         * Le pilote reclame le curseur image par image : une seule demande apres
         * 150 ms perdait contre le focus initial de Compose.
         * pourquoi : docs/decisions/session.md § Un seul `focusRequester` par nœud, et c'est celui de la coquille
         */
        LaunchedEffect(panelLive) {
            if (!panelLive) return@LaunchedEffect
            repeat(PILOT_FOCUS_FRAMES) {
                withFrameNanos { }
                runCatching { pilotFocus.requestFocus() }
            }
        }

        val panelPilot = if (!panelLive) Modifier else Modifier
            .focusRequester(pilotFocus)
            // **Avant `focusable()`, jamais apres.** Un `onFocusChanged`
            // observe ce qui le suit dans la chaine : place derriere, il ne
            // voyait rien du noeud qu'il etait cense surveiller, et la trace
            // annoncait « le pilote ne prend jamais le focus » alors qu'il le
            // prenait. Une sonde mal placee ment plus surement qu'elle
            // n'informe.
            .onFocusChanged { state ->
                // Rendre le curseur au pilote, c'est le rendre aux etapes.
                // C'est le chemin du retour depuis la croix : l'en-tete
                // consomme Bas et demande le focus ici, et sans cette ligne il
                // arrivait sans que rien ne soit designe — d'ou la seconde
                // pression.
                if (state.isFocused &&
                    SecondScreen.stepCursor.value == null &&
                    SecondScreen.steps.value.isNotEmpty()
                ) {
                    SecondScreen.selectStep(0)
                }
            }
            .focusable()
            // Des que la page tient le focus, le curseur est **directement** sur
            // les etapes du panneau : un arret intermediaire invisible n'etait
            // pas un etat, c'etait un silence.

            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown && event.type != KeyEventType.KeyUp) {
                    return@onKeyEvent false
                }
                // Sortir du panneau rend le curseur a l'ecran de face, sur la
                // croix : la ou l'on voit qu'on est rendu.
                fun leavePanel() {
                    SecondScreen.clearStepCursor()
                    scaffoldFocus?.header?.let { runCatching { it.requestFocus() } }
                }
                val cursor = panelCursor
                // L'entree : Bas, quand le curseur de face n'a plus rien sous la
                // main — c'est-a-dire depuis la derniere commande de la page.
                if (cursor == null) {
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown &&
                        SecondScreen.steps.value.isNotEmpty()
                    ) {
                        SecondScreen.selectStep(0)
                        true
                    } else {
                        false
                    }
                } else {
                    val steps = SecondScreen.steps.value
                    when {
                        // A et ses synonymes pressent l'etape designee. Le KeyDown
                        // est avale pour qu'une pression ne lise pas deux fois,
                        // comme partout ailleurs au pad.
                        event.key in CONFIRM_KEYS -> {
                            if (event.type == KeyEventType.KeyUp) {
                                steps.getOrNull(cursor)?.takeIf { it.enabled }
                                    ?.let { Sfx.click(); it.onPress() }
                            }
                            true
                        }
                        event.type == KeyEventType.KeyDown -> when (event.key) {
                            Key.DirectionLeft -> { SecondScreen.moveStep(-1); true }
                            Key.DirectionRight -> { SecondScreen.moveStep(1); true }
                            Key.DirectionUp -> {
                                // Remonter au-dela de la premiere etape rend le
                                // curseur a l'ecran de face, sur la croix.
                                if (cursor == 0) leavePanel() else SecondScreen.moveStep(-1)
                                true
                            }
                            // Les etapes forment une rangee : Bas n'a nulle part ou
                            // aller, mais il ne doit pas rendre le curseur non plus.
                            Key.DirectionDown -> true
                            // B reprend le curseur, sans fermer le panneau.
                            Key.ButtonB, Key.Back -> { leavePanel(); true }
                            else -> false
                        }
                        else -> false
                    }
                }
            }
        if (landscape) {
            // Two panes: state on the left, what is left to do on the right,
            // with every answer under the button that produced it.
            // pourquoi : docs/decisions/session.md § Deux panneaux, parce qu'empilé cet écran ne tient pas
            Row(
                modifier = panelPilot
                    .fillMaxSize()
                    .padding(
                        top = topPadding,
                        bottom = bottomInset + 16.dp,
                        start = 20.dp,
                        end = 20.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pas de `verticalScroll` : un volet d'etat qui peut cacher son
                // etat ne fait pas son travail. Plus etroit panneau allume — il ne
                // reste que la presence, et les 52 dp vont a droite.
                // pourquoi : docs/decisions/session.md § Le panneau d'état ne défile pas, donc il doit tenir
                // pourquoi : docs/decisions/session.md § Ce que le panneau porte, l'écran de face le rend en place
                Column(
                    modifier = Modifier.width(if (panelLive) 220.dp else 272.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Presence gives way, never the address: the weight is what
                    // reverses Compose's measuring order to guarantee it.
                    // pourquoi : docs/decisions/session.md § Le panneau d'état ne défile pas, donc il doit tenir
                    PresenceCard(
                        youName = profile.name,
                        youAvatar = profile.avatarFile,
                        others = others,
                        isHost = session.role == Session.Role.HOST,
                        live = !offline,
                        scrollable = true,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Le panneau arriere porte deja l'adresse et le port, tous
                    // les deux a la fois et sans qu'on les demande. Quand il est
                    // la, cette carte redirait la meme chose de face, et la
                    // place qu'elle prend revient a l'explication.
                    // pourquoi : docs/decisions/session.md § Ce que le panneau arrière porte, l'écran de face ne le redit pas
                    if (!panelLive) {
                        ConnectionCard(
                            hostIp = shownAddress,
                            addressLabel = addressLabel,
                            port = shownPort,
                            romName = session.rom?.displayName
                        )
                    }

                    // Le jeu, encadre, **et seulement quand le panneau a libere la
                    // place** : sans panneau il n'y a pas de vide a remplir, et ce
                    // bloc plafonnait la carte de presence a la moitie d'une colonne.
                    // pourquoi : docs/decisions/session.md § Ce que le panneau porte, l'écran de face le rend en place
                    if (panelLive) {
                        sessionArt?.let { art ->
                            Box(
                                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                                contentAlignment = Alignment.Center
                            ) {
                                BoxWithConstraints {
                                    val side = minOf(maxWidth, maxHeight)
                                    if (side >= 96.dp) {
                                        RomArtwork(rom = art, size = minOf(side, 208.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Ce qui cede quand il n'y a plus de place : l'explication, jamais
                    // les boutons. Et le fondu **n'existe qu'en mono-ecran**.
                    // pourquoi : docs/decisions/session.md § Ce que le panneau porte, l'écran de face le rend en place
                    val fade = !panelLive
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .then(
                                if (!fade) Modifier else Modifier
                                    .graphicsLayer {
                                        compositingStrategy = CompositingStrategy.Offscreen
                                    }
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                0f to Color.Transparent,
                                                0.05f to Color.Black,
                                                0.94f to Color.Black,
                                                1f to Color.Transparent
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    }
                            )
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (offline) OfflineCard()
                        if (session.backend == Backend.PPSSPP) PspHintCard(pspAutomatic)
                        EmulatorHintCard(
                            session = session,
                            automationOn = automationOn,
                        )
                    }

                    // Quand le panneau arriere porte les etapes, l'ecran de face
                    // ne les redessine pas : c'est toute la hauteur qu'elles
                    // prenaient qui revient a l'explication au-dessus.
                    // pourquoi : docs/decisions/second-ecran.md § Le panneau prend les etapes, parce qu'il est tactile
                    if (!panelLive) {
                    // The first button that exists AND responds: a disabled one
                    // does not take focus. Then a real gap before the buttons.
                    // pourquoi : docs/decisions/session.md § Descendre vise le premier bouton qui répond
                    Spacer(Modifier.height(2.dp))
                    if (session.backend.hasNetplay && !ps2Automatic) {
                        NetplayButton(
                            session = session,
                            netplayDone = netplayDone,
                            netplayPrepared = netplayPrepared,
                            waitingForHost = waitingForHost,
                            onClick = onNetplayStep,
                            modifier = Modifier.padEntry()
                        )
                    }
                    if (session.backend == Backend.PPSSPP && !pspAutomatic) {
                        PspSetupButton(
                            pspOpened = pspOpened,
                            onClick = {
                                status = openPpssppForSetup(context, ppsspp) { pspOpened = true }
                            },
                            modifier = if (session.backend.hasNetplay) Modifier
                                       else Modifier.padEntry()
                        )
                    }
                    LaunchButton(
                        session = session,
                        netplayPrepared = netplayPrepared,
                        directPs2 = ps2Automatic,
                        waitingForHost = ps2Automatic && waitingForHost,
                        onClick = onLaunchStep,
                        // Last resort: when no step precedes it, this is the
                        // first button on the page.
                        modifier = if ((session.backend.hasNetplay && !ps2Automatic) ||
                                       (session.backend == Backend.PPSSPP && !pspAutomatic)) Modifier
                                   else Modifier.padEntry()
                    )
                    }
                    status?.let { StatusLine(it) }
                }
            }
            return@EmufiiScaffold
        }

        Column(
            modifier = panelPilot
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = topPadding,
                    bottom = bottomInset + 24.dp,
                    start = 20.dp,
                    end = 20.dp
                ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CodeCard(code = session.code, isHost = session.role == Session.Role.HOST)

            // Before everything else: something to do in another program, once.
            // pourquoi : docs/decisions/session.md § Ce qui se fait à la main se dit avant le bouton, jamais après
            if (session.backend == Backend.PPSSPP) PspHintCard(pspAutomatic)

            // Above the member list, because that list is the thing that has
            // gone stale: whoever is shown there was here the last time we
            // heard back, not necessarily now.
            if (offline) OfflineCard()

            PresenceCard(
                youName = profile.name,
                youAvatar = profile.avatarFile,
                others = others,
                isHost = session.role == Session.Role.HOST,
                live = !offline
            )

            // Meme regle qu'en paysage : ce que le panneau arriere rapporte,
            // l'ecran de face ne le redit pas.
            // pourquoi : docs/decisions/session.md § Ce que le panneau arrière porte, l'écran de face ne le redit pas
            if (!panelLive) {
                ConnectionCard(
                    hostIp = shownAddress,
                    addressLabel = addressLabel,
                    // And no port: the ad hoc server's is fixed and PPSSPP does
                    // not ask for it. One more field to fill in is one more
                    // field to fill in wrong.
                    port = shownPort,
                    romName = session.rom?.displayName
                )
            }

            // Before the buttons: Azahar refuses the room over the nickname
            // while blaming the address.
            // pourquoi : docs/decisions/session.md § Ce qui se fait à la main se dit avant le bouton, jamais après
            EmulatorHintCard(
                session = session,
                automationOn = automationOn,
            )

            // Meme regle qu'en paysage : le panneau arriere porte les etapes
            // quand il est la.
            // pourquoi : docs/decisions/second-ecran.md § Le panneau prend les etapes, parce qu'il est tactile
            if (!panelLive) {
            // Two steps, in the order the emulator itself expects: join the room
            // from its main menu, then boot the game. One button did both, so
            // the ROM started in an emulator that had joined nothing, and the
            // player learned it from the game instead of from Emufii.
            if (session.backend.hasNetplay && !ps2Automatic) {
                NetplayButton(
                    session = session,
                    netplayDone = netplayDone,
                    netplayPrepared = netplayPrepared,
                    waitingForHost = waitingForHost,
                    onClick = onNetplayStep,
                    modifier = Modifier.padEntry()
                )
            }

            // The button does not apply the settings, it opens the emulator,
            // and its label says so.
            // pourquoi : docs/decisions/session.md § Les cartes par console, et ce que chacune doit empêcher
            if (session.backend == Backend.PPSSPP && !pspAutomatic) {
                PspSetupButton(
                    pspOpened = pspOpened,
                    onClick = { status = openPpssppForSetup(context, ppsspp) { pspOpened = true } },
                    modifier = if (session.backend.hasNetplay) Modifier else Modifier.padEntry()
                )
            }

            LaunchButton(
                session = session,
                netplayPrepared = netplayPrepared,
                directPs2 = ps2Automatic,
                waitingForHost = ps2Automatic && waitingForHost,
                onClick = onLaunchStep,
                modifier = if ((session.backend.hasNetplay && !ps2Automatic) ||
                               (session.backend == Backend.PPSSPP && !pspAutomatic)) Modifier
                           else Modifier.padEntry()
            )
            }

            // Directly under the button that produces it: rendered last, a
            // refusal landed off-screen and read as a dead button.
            // pourquoi : docs/decisions/session.md § Ce qui se fait à la main se dit avant le bouton, jamais après
            status?.let { StatusLine(it) }

            LeaveButton(session = session, onLeave = { confirmingLeave = true })
        }
    }
    }

    if (confirmingLeave) {
        val host = session.role == Session.Role.HOST
        PadDialog(
            title = stringResource(if (host) R.string.session_close else R.string.session_leave),
            onDismiss = { confirmingLeave = false },
            // C'est le dialogue qui rendait le panneau le plus faux : il
            // continuait d'afficher le code et les etapes pendant qu'on
            // demandait s'il fallait tout couper. Il porte la meme phrase que le
            // corps, parce que c'est la meme question.
            panelDetail = stringResource(
                if (host) R.string.session_close_confirm else R.string.session_leave_confirm
            ),
            panelSocial = true,
            actions = {
                GhostButton(
                    label = stringResource(R.string.common_cancel),
                    onClick = { confirmingLeave = false }
                )
                GhostButton(
                    label = stringResource(if (host) R.string.session_close else R.string.session_leave),
                    onClick = {
                        confirmingLeave = false
                        onLeave()
                    },
                    tint = danger()
                )
            }
        ) {
            // L'hote et l'invite ne risquent pas la meme chose : l'un ferme la
            // session pour tout le monde, l'autre s'en retire.
            PadDialogText(
                stringResource(
                    if (host) R.string.session_close_confirm else R.string.session_leave_confirm
                )
            )
        }
    }
}

/**
 * What the player has to set in the emulator, if anything. One definition, so
 * the two layouts cannot drift apart.
 */
@Composable
private fun EmulatorHintCard(
    session: Session,
    automationOn: Boolean,
) {
    if (session.rom == null) {
        MissingRomCard()
        return
    }
    when (session.backend) {
        Backend.AZAHAR -> AzaharHintCard(
            automationOn = automationOn,
            isHost = session.role == Session.Role.HOST,
            hostIp = session.hostIp,
            port = session.port
        )
        // Unreachable from the library, which routes DS straight to the Kaeru
        // screen. Handled anyway: a session joined from the finder carries
        // whatever console the host had.
        Backend.EDEN -> EdenHintCard(
            automationOn = automationOn,
            // With a room on the VPS the host joins like everyone else: telling
            // them "Create" would send them to open a second, empty room next to
            // the one where their guest is waiting.
            isHost = session.role == Session.Role.HOST && session.room == null,
            // What the player would type by hand if the automation failed: the
            // room when there is one, the host otherwise.
            hostIp = session.room?.host ?: session.hostIp,
            port = session.room?.port?.toString() ?: session.port
        )
        Backend.DOLPHIN -> DolphinHintCard(
            automationOn = automationOn,
            isHost = session.role == Session.Role.HOST,
            hostIp = session.hostIp,
            port = DolphinTarget.DEFAULT_PORT.toString()
        )
        Backend.ARMSX2 -> Ps2HintCard(
            automationOn = session.rom.let {
                Ps2GameSettings.canConfigure(LocalContext.current, it)
            } == true,
            isHost = session.role == Session.Role.HOST,
            hostIp = session.hostIp,
            port = Ps2Target.DEFAULT_PORT.toString()
        )
        Backend.PPSSPP -> Unit
        Backend.MELONDS_WFC -> WfcNotASessionCard()
        Backend.NONE -> UnsupportedHintCard(session.console?.label)
    }
}

/**
 * Step 1: join the room, without launching the game. Green once the room is
 * actually joined, not once the emulator was merely opened.
 * pourquoi : docs/decisions/session.md § Deux preuves qu'un salon existe, et la seconde est assumée plus faible
 */
@Composable
private fun NetplayButton(
    session: Session,
    netplayDone: Boolean,
    netplayPrepared: Boolean,
    /**
     * The host has not opened their room yet, so the button greys out and says
     * so rather than sending the guest to a room that does not exist.
     * pourquoi : docs/decisions/session.md § L'ordre hôte puis invité n'est pas un détail de confort
     */
    waitingForHost: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = session.rom != null && !waitingForHost
    Button(
        onClick = sounded(onClick),
        enabled = enabled,
        shape = ActionShape,
        colors = if (netplayDone) {
            ButtonDefaults.buttonColors(containerColor = good())
        } else {
            ButtonDefaults.buttonColors()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .controlRing(ActionShape)
            // Greyed out but still reachable: focus does not promise a click
            // will land, it says where you are.
            // pourquoi : docs/decisions/session.md § Descendre vise le premier bouton qui répond
            .then(if (enabled) Modifier else Modifier.focusable())
    ) {
        if (netplayDone) {
            CheckMark(color = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(10.dp))
        }
        Text(
            stringResource(
                when {
                    waitingForHost -> R.string.session_netplay_waiting_host
                    netplayDone -> R.string.session_netplay_done
                    netplayPrepared -> R.string.session_netplay_again
                    else -> R.string.session_netplay_open
                },
                // The emulator this session actually drives. It was written
                // "Azahar" in the string, so a Switch session announced the
                // wrong program by name.
                session.backend.emulatorName
            ),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

/**
 * PPSSPP has no netplay to drive: this opens the emulator so the player can
 * enter the settings, and its label says exactly that.
 * pourquoi : docs/decisions/session.md § Les cartes par console, et ce que chacune doit empêcher
 */
@Composable
private fun PspSetupButton(
    pspOpened: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = sounded(onClick),
        shape = ActionShape,
        colors = if (pspOpened) {
            ButtonDefaults.buttonColors(containerColor = good())
        } else {
            ButtonDefaults.buttonColors()
        },
        modifier = modifier.fillMaxWidth().height(56.dp).controlRing(ActionShape)
    ) {
        if (pspOpened) {
            CheckMark(color = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(10.dp))
        }
        Text(
            stringResource(
                if (pspOpened) R.string.session_psp_setup_again
                else R.string.session_psp_setup
            ),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

/** Step 2: launch the game, once the room has been joined. */
@Composable
private fun LaunchButton(
    session: Session,
    netplayPrepared: Boolean,
    directPs2: Boolean = false,
    waitingForHost: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = sounded(onClick),
        enabled = launchEnabled(session, netplayPrepared, directPs2, waitingForHost),
        shape = ActionShape,
        // Disabled, but still legibly the next step: Material's grey-on-grey
        // slab read as an absence rather than as a button waiting for step 1.
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        ),
        modifier = modifier.fillMaxWidth().height(56.dp).controlRing(ActionShape)
    ) {
        Text(
            launchLabel(session, netplayPrepared, directPs2, waitingForHost),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

/**
 * Ce que le bouton de lancement dit, et s'il repond — **une seule definition**,
 * parce que deux ecrans le dessinent.
 * pourquoi : docs/decisions/second-ecran.md § Le panneau prend les etapes, parce qu'il est tactile
 */
@Composable
private fun launchLabel(
    session: Session,
    netplayPrepared: Boolean,
    directPs2: Boolean,
    waitingForHost: Boolean
): String = when {
    waitingForHost -> stringResource(
        R.string.session_netplay_waiting_host,
        session.backend.emulatorName,
    )
    // Rejoindre depuis le chercheur un jeu qu'on ne possede pas est une autre
    // situation qu'une console non prise en charge, et dire la mauvaise envoie
    // chercher au mauvais endroit.
    session.rom == null -> stringResource(R.string.session_no_rom)
    session.backend == Backend.AZAHAR ||
        session.backend == Backend.EDEN ||
        session.backend == Backend.PPSSPP ||
        // La PS2 est dans le lot : la `MainActivity` d'ARMSX2 est exportee et
        // prend un `content://`, donc le jeu se lance bien d'ici. Dolphin est
        // l'exception, pas elle.
        session.backend == Backend.ARMSX2 ->
        // Numerote seulement la ou il y a une etape 1 au-dessus.
        stringResource(
            if (session.backend.hasNetplay && !directPs2) R.string.session_launch_step2
            else R.string.session_launch_emulation
        )
    session.backend == Backend.MELONDS_WFC -> stringResource(R.string.session_wfc_not_a_session)
    // Dolphin n'a pas d'etape 2, et le dire vaut mieux que de retomber sur
    // « pas encore pris en charge », qui etait faux et decourageant.
    // pourquoi : docs/decisions/session.md § Les cartes par console, et ce que chacune doit empêcher
    session.backend == Backend.DOLPHIN -> stringResource(R.string.session_dolphin_lobby)
    else -> stringResource(R.string.session_unsupported_short)
}

/**
 * Grise tant que l'etape du salon n'est pas passee : lancer d'abord est
 * l'erreur que cette paire existe pour empecher.
 */
private fun launchEnabled(
    session: Session,
    netplayPrepared: Boolean,
    directPs2: Boolean,
    waitingForHost: Boolean
): Boolean =
    session.rom != null && session.backend != Backend.NONE && !waitingForHost &&
        (!session.backend.hasNetplay || netplayPrepared || directPs2)

/**
 * The session code, in the header: it is what you read out loud to someone
 * else, so it stays visible at all times. A tap copies.
 * pourquoi : docs/decisions/session.md § Les partis pris de dessin de cet écran
 */
@Composable
private fun SessionCodeChip(code: String, onCopy: () -> Unit) {
    val dark = LocalEmufiiDarkTheme.current
    // La pilule est ronde et sa hauteur explicite, sinon `Surface(onClick)`
    // reserve 48 dp et peint un fond plus petit dedans. Elle porte l'axe social.
    // pourquoi : docs/decisions/theme-duotone-shelves.md § Session / Join — domaine corail
    Surface(
        onClick = sounded(onCopy),
        shape = CircleShape,
        color = if (dark) Coral.bright else Coral.deep,
        border = BorderStroke(1.dp, edgeColor(dark, oled = false)),
        shadowElevation = 4.dp,
        modifier = Modifier.controlRing(CircleShape)
    ) {
        Row(
            modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.session_code_label).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (dark) Coral.ink else Color.White.copy(alpha = 0.80f),
                letterSpacing = 1.sp
            )
            Text(
                code.ifBlank { "—" },
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = if (dark) Coral.ink else Color.White
            )
        }
    }
}

/** The answer to a tap, in its reserved place under the buttons. */
@Composable
private fun StatusLine(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun LeaveButton(
    session: Session,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
    /** False in the header, where the button hugs the edge. */
    fillWidth: Boolean = true
) {
    // A moulded pill, like everything else that can be pressed: the
    // destructive control must not be the one made of nothing.
    // pourquoi : docs/decisions/session.md § Les partis pris de dessin de cet écran
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = (if (fillWidth) modifier.fillMaxWidth() else modifier)
            .heightIn(min = 48.dp)
            .controlRing(PillShape)
            .plate(shape = PillShape, dark = dark, oled = oled, lift = 4.dp, pressed = pressed)
            .tap(interactionSource = interaction, indication = null, onClick = onLeave)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (session.role == Session.Role.HOST) stringResource(R.string.session_close)
            else stringResource(R.string.session_leave),
            style = MaterialTheme.typography.labelLarge,
            color = danger()
        )
    }
}

/**
 * A tick, drawn rather than imported: it sits where it is put, where a glyph is
 * centred on its line box rather than its ink.
 * pourquoi : docs/decisions/session.md § Les partis pris de dessin de cet écran
 */
@Composable
private fun CheckMark(color: Color, size: Dp = 18.dp) {
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

@Composable
private fun CodeCard(code: String, isHost: Boolean) {
    SoftCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionHeader(stringResource(R.string.session_code_label))
            Text(
                code.ifBlank { "—" },
                fontSize = 44.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                // Le code est le lien que l'on donne : il porte l'axe corail.
                color = coralText()
            )
            if (isHost) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.session_code_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Who is in the room, live — the whole point of the presence loop.
 * pourquoi : docs/decisions/session.md § Les partis pris de dessin de cet écran
 */
@Composable
private fun PresenceCard(
    youName: String,
    youAvatar: java.io.File?,
    others: List<Member>,
    isHost: Boolean,
    live: Boolean,
    /**
     * True only in the pane. Not taste: the single-column page already scrolls,
     * and Compose throws when measuring scrolling content unbounded.
     * pourquoi : docs/decisions/session.md § Les partis pris de dessin de cet écran
     */
    scrollable: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState()
    // A line cut in half reads as a rendering glitch; the same line fading into
    // the card's background reads as "there is more". The gradient only lights
    // up when something is left below the fold.
    val fill = softCardFill()
    val fade = scrollable && scroll.canScrollForward

    SoftCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // BEFORE the scroll: placed after, it works in the unrolled
                // content's coordinates and lands below the fold, invisible.
                // pourquoi : docs/decisions/session.md § Les partis pris de dessin de cet écran
                .then(
                    if (!fade) Modifier else Modifier.drawWithContent {
                        drawContent()
                        val h = FADE_HEIGHT.toPx()
                        drawRect(
                            // Opaque before the edge, not at it: measured, a
                            // linear run left the last line legible and sliced.
                            // pourquoi : docs/decisions/session.md § Les partis pris de dessin de cet écran
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    0.65f to fill,
                                    1f to fill
                                ),
                                startY = size.height - h,
                                endY = size.height
                            ),
                            topLeft = Offset(0f, size.height - h),
                            size = Size(size.width, h)
                        )
                    }
                )
                .then(if (scrollable) Modifier.verticalScroll(scroll) else Modifier)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader(
                    if (others.isEmpty()) stringResource(R.string.session_members_label)
                    else pluralStringResource(
                        R.plurals.session_members_count,
                        others.size + 1,
                        others.size + 1
                    )
                )
                Spacer(Modifier.weight(1f))
                // Nothing is live while we're not hearing back: the dot would
                // be vouching for a list we can no longer refresh.
                if (others.isNotEmpty() && live) LiveDot()
            }
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarStack(
                    names = listOf(playerDisplayName(youName)) + others.map { playerDisplayName(it.name) },
                    size = 40.dp
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        if (others.isEmpty()) stringResource(R.string.session_you_alone)
                        else nameList(
                            listOf(stringResource(R.string.session_you)) +
                                others.map { playerDisplayName(it.name) }
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (others.isEmpty() && isHost) {
                        Text(
                            stringResource(R.string.session_waiting),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Announce arrivals rather than just growing the row silently.
            AnimatedVisibility(
                visible = others.isNotEmpty(),
                enter = fadeIn() + expandVertically()
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    others.forEach { m ->
                        Text(
                            stringResource(
                                R.string.session_member_since,
                                playerDisplayName(m.name),
                                humanDuration(m.forSeconds)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Enough to erase a whole line and its leading, not just bite into it —
 * measured, 28 dp left the cut line half legible.
 * pourquoi : docs/decisions/session.md § Les partis pris de dessin de cet écran
 */
private val FADE_HEIGHT = 44.dp

/** Slow pulse, something is live without being a spinner demanding attention. */
@Composable
private fun LiveDot() {
    val transition = rememberInfiniteTransition(label = "live")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "live-alpha"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .alpha(alpha)
                .clip(CircleShape)
                .background(good())
        )
        Spacer(Modifier.size(6.dp))
        Text(
            stringResource(R.string.session_live),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConnectionCard(
    hostIp: String,
    addressLabel: String,
    /** Null when the console does not ask for one, the column then disappears. */
    port: String?,
    romName: String?,
    /**
     * False in the pane, where its forty dp are exactly what clipped the card —
     * and a game name is not a state you act on.
     * pourquoi : docs/decisions/session.md § Les partis pris de dessin de cet écran
     */
    showGame: Boolean = true
) {
    SoftCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            romName?.takeIf { showGame }?.let {
                Column {
                    SectionHeader(stringResource(R.string.session_game))
                    Text(it, style = MaterialTheme.typography.titleMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionHeader(addressLabel)
                    Text(hostIp.ifBlank { "—" }, style = MaterialTheme.typography.titleMedium)
                }
                if (port != null) {
                    Column {
                        SectionHeader(stringResource(R.string.session_port))
                        Text(port.ifBlank { "—" }, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            // **Plus de boutons « copier »** : Emufii remplit le formulaire, et le
            // presse-papier ne tient qu'une valeur quand la boite en veut deux.
            // pourquoi : docs/decisions/session.md § Copier l'adresse n'a plus de sens depuis qu'Emufii la remplit
        }
    }
}

/**
 * A line the player cannot afford to skim, inside a card of lines they can.
 * pourquoi : docs/decisions/session.md § Les partis pris de dessin de cet écran
 */
@Composable
private fun ImportantNote(text: String) {
    // A recess, ordinary ink, and a drawn bead — never a red field. Red is
    // spent exactly twice in the whole app, and spending it here wears it out.
    // pourquoi : docs/decisions/session.md § Les partis pris de dessin de cet écran
    val dark = LocalEmufiiDarkTheme.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .socket(shape, dark)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        WarnIcon(
            size = 17.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // Aligned to the first line's ink rather than centred on the block:
            // a mark that drifts to the middle of a three-line note reads as
            // decoration instead of as a mark on the sentence it opens.
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AzaharHintCard(
    automationOn: Boolean,
    isHost: Boolean,
    hostIp: String,
    port: String,
) {
    SoftCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionHeader(stringResource(R.string.hint_azahar_title))
            // Loud, not a footnote, and on both paths: getting it wrong
            // produces an error that accuses the address.
            // pourquoi : docs/decisions/session.md § Ce qui se fait à la main se dit avant le bouton, jamais après
            ImportantNote(stringResource(R.string.hint_azahar_username))
            ImportantNote(stringResource(R.string.hint_same_version))
            if (automationOn) {
                Text(
                    stringResource(R.string.hint_azahar_automated),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    stringResource(
                        R.string.hint_azahar_manual,
                        "$hostIp:$port"
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Eden's multiplayer is in the app's own settings, not a game drawer. Host is
 * told to Create and guest to Join: the same words would put both on one side.
 * pourquoi : docs/decisions/session.md § Les cartes par console, et ce que chacune doit empêcher
 */
@Composable
private fun EdenHintCard(
    automationOn: Boolean,
    isHost: Boolean,
    hostIp: String,
    port: String,
) {
    SoftCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionHeader(stringResource(R.string.hint_eden_title))
            Text(
                stringResource(
                    if (isHost) R.string.hint_eden_host else R.string.hint_eden_guest
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            // As loud as Azahar's nickname, and for the same reason: it is a
            // prerequisite the emulator does not mention. A differing game
            // version lets the room form, then the game never starts, and
            // nothing on screen points at the cause.
            ImportantNote(stringResource(R.string.hint_same_version))
            if (automationOn) {
                Text(
                    stringResource(R.string.hint_eden_automated),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    stringResource(R.string.hint_eden_username),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    stringResource(R.string.hint_eden_manual, "$hostIp:$port"),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Dolphin, whose flow is one step where the others are two: the game is picked
 * in the lobby. Both sides need the same dump, byte for byte.
 * pourquoi : docs/decisions/session.md § Le prérequis Dolphin que personne ne vérifie
 */
@Composable
private fun DolphinHintCard(
    automationOn: Boolean,
    isHost: Boolean,
    hostIp: String,
    port: String,
) {
    SoftCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionHeader(stringResource(R.string.hint_dolphin_title))
            Text(
                stringResource(
                    if (isHost) R.string.hint_dolphin_host else R.string.hint_dolphin_guest
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            ImportantNote(stringResource(R.string.hint_dolphin_same_dump))
            // The save is more treacherous than the dump: nobody checks it,
            // and mismatched saves desync silently. We warn, we cannot act.
            // pourquoi : docs/decisions/session.md § Le prérequis Dolphin que personne ne vérifie
            ImportantNote(stringResource(R.string.hint_dolphin_same_save))
            if (automationOn) {
                Text(
                    stringResource(R.string.hint_dolphin_automated),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    stringResource(R.string.hint_dolphin_manual, "$hostIp:$port"),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * The PS2 card, and the one thing it must prevent: ARMSX2 has two unrelated
 * multiplayers, and Emufii serves only the local one.
 * pourquoi : docs/decisions/session.md § Les cartes par console, et ce que chacune doit empêcher
 */
@Composable
private fun Ps2HintCard(
    automationOn: Boolean,
    isHost: Boolean,
    hostIp: String,
    port: String,
) {
    SoftCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionHeader(stringResource(R.string.hint_ps2_title))
            ImportantNote(stringResource(R.string.hint_ps2_lan_only))
            Text(
                stringResource(if (isHost) R.string.hint_ps2_host else R.string.hint_ps2_guest),
                style = MaterialTheme.typography.bodyMedium
            )
            // Said by ARMSX2 itself, and it is not guessable: with the network
            // adapter attached, some games stop responding to the pad. Without
            // this line, that reads as a frozen app.
            ImportantNote(stringResource(R.string.hint_ps2_pad))
            if (automationOn) {
                Text(
                    stringResource(R.string.hint_ps2_automated),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    stringResource(R.string.hint_ps2_manual, "$hostIp:$port"),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun MissingRomCard() {
    SoftCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeader(stringResource(R.string.hint_missing_rom_title))
            Text(
                stringResource(R.string.hint_missing_rom_body),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * The coordinator has gone quiet. Deliberately not an error: a running game
 * keeps running, only the presence list stops being trustworthy.
 * pourquoi : docs/decisions/session.md § Seul un 404 prouve qu'un salon est fermé
 */
@Composable
private fun OfflineCard() {
    SoftCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeader(stringResource(R.string.session_offline_title))
            Text(
                stringResource(R.string.session_offline_body),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * The four PSP settings, with the address copied on *display* rather than on
 * tap — the player is about to leave for PPSSPP.
 * pourquoi : docs/decisions/session.md § Les cartes par console, et ce que chacune doit empêcher
 */
@Composable
private fun PspHintCard(automatic: Boolean) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(automatic) {
        if (!automatic) {
            copyToClipboard(context, "Emufii", HOST_SENTINEL)
            copied = true
        }
    }
    SoftCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionHeader(stringResource(R.string.hint_psp_title))
            Text(
                stringResource(
                    if (automatic) R.string.hint_psp_automated
                    else R.string.hint_psp_body
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (automatic) {
                Text(
                    stringResource(R.string.hint_psp_automatic_ready),
                    style = MaterialTheme.typography.bodySmall,
                    color = good()
                )
                Text(
                    stringResource(R.string.hint_psp_step4),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    HOST_SENTINEL,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = coralText()
                )
                for (step in listOf(
                    stringResource(R.string.hint_psp_step1),
                    stringResource(R.string.hint_psp_step2, HOST_SENTINEL),
                    stringResource(R.string.hint_psp_step2b),
                    stringResource(R.string.hint_psp_step3),
                    stringResource(R.string.hint_psp_step4)
                )) {
                    Text("· " + step, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    stringResource(R.string.hint_psp_relay_why),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.hint_psp_why),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (copied) {
                    Text(
                        stringResource(R.string.hint_psp_copied),
                        style = MaterialTheme.typography.bodySmall,
                        color = good()
                    )
                }
                GhostButton(
                    label = stringResource(R.string.hint_psp_copy),
                    onClick = { copyToClipboard(context, "Emufii", HOST_SENTINEL) }
                )
            }
            Text(
                stringResource(R.string.hint_psp_exit_before_switch),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )

            // At the end, and deliberately neither an ImportantNote (reserved
            // for what stops you playing) nor the grey advisory voice.
            // pourquoi : docs/decisions/session.md § Les partis pris de dessin de cet écran
            SectionHeader(stringResource(R.string.hint_psp_wifi_title))
            Text(
                stringResource(R.string.hint_psp_wifi),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun WfcNotASessionCard() {
    SoftCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeader(stringResource(R.string.hint_wfc_title))
            Text(
                stringResource(R.string.hint_wfc_body),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun UnsupportedHintCard(consoleLabel: String?) {
    SoftCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeader(consoleLabel ?: stringResource(R.string.hint_unknown_console))
            Text(
                stringResource(R.string.hint_unsupported_body),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * "Toi, Bibi et Théo" / "You, Bibi and Théo". ICU does it: the conjunction and
 * the comma placement differ per locale.
 */
@Composable
private fun nameList(names: List<String>): String {
    val locale = LocalConfiguration.current.locales[0]
    return when (names.size) {
        0 -> ""
        1 -> names[0]
        else -> ListFormatter.getInstance(locale).format(names)
    }
}

@Composable
private fun humanDuration(seconds: Int): String = when {
    seconds < 60 -> stringResource(R.string.duration_seconds)
    seconds < 3600 -> stringResource(R.string.duration_minutes, seconds / 60)
    else -> stringResource(R.string.duration_hours, seconds / 3600)
}

/**
 * Opens PPSSPP so the player can enter their settings there; returns the
 * message to show, null when it went fine. Not a step of [Session.launch].
 */
private fun openPpssppForSetup(
    context: android.content.Context,
    ppsspp: PpssppLauncher,
    onOpened: () -> Unit
): String? = when (val result = ppsspp.openApp()) {
    LaunchResult.Success -> { onOpened(); null }
    LaunchResult.NotInstalled -> context.getString(R.string.err_not_installed, "PPSSPP")
    is LaunchResult.Error -> context.getString(R.string.err_generic, result.message)
    // PPSSPP exposes no netplay to drive: this case cannot come from it.
    is LaunchResult.NoNetplayUi -> null
}

private suspend fun Session.launch(
    context: android.content.Context,
    azahar: AzaharLauncher,
    eden: EdenLauncher,
    ppsspp: PpssppLauncher,
    onPs2Started: () -> Unit = {},
    /**
     * Appele quand l'emulateur est **reellement** parti, et seulement alors.
     *
     * La valeur de retour ne suffit pas a le savoir : c'est un message d'etat,
     * et « Lancement de X… » comme « Console non prise en charge » sont tous
     * deux des chaines. Le panneau arriere, lui, doit distinguer les deux — il
     * est le seul ecran qui reste visible une fois l'emulateur devant, et il ne
     * peut pas cocher une etape sur une phrase.
     * pourquoi : docs/decisions/second-ecran.md § Un panneau qui affirme le faux est une panne
     */
    onLaunched: () -> Unit = {},
): String {
    val rom = this.rom ?: return context.getString(R.string.session_no_rom_attached)
    // Step two runs after the room has been joined, so the plan has done its
    // work, and an armed plan that stays armed is what made the automation
    // fight the player for the in-game drawer. Disarming here is the one moment
    // we know for certain it is spent.
    if (backend.hasNetplay) NetplayAutomation.clear(PlanStore(context))
    val (result, emulator) = when (backend) {
        Backend.AZAHAR -> azahar.launchGame(rom.uri, plan = null) to "Azahar"
        Backend.EDEN -> eden.launchGame(
            rom.uri,
            plan = null,
            automationOn = azahar.isNetplayAutomationEnabled()
        ) to "Eden"
        Backend.PPSSPP -> ppsspp.launchPrivateGame(rom) to "PPSSPP"
        // A return, not a launch: resume the existing task, and above all NO
        // armed plan — re-arming refills the form over a running game.
        // pourquoi : docs/decisions/session.md § Ce que chaque backend reçoit au lancement
        Backend.DOLPHIN -> {
            val result = DolphinLauncher(context).launch()
            return if (result == LaunchResult.Success) {
                onLaunched()
                context.getString(R.string.session_dolphin_lobby_opened)
            } else {
                context.getString(R.string.err_not_installed, "Dolphin")
            }
        }
        // A real launch, unlike Dolphin: ARMSX2's MainActivity is exported with
        // a VIEW filter on `content`. Still no armed plan.
        // pourquoi : docs/decisions/session.md § Ce que chaque backend reçoit au lancement
        Backend.ARMSX2 -> {
            val launcher = Ps2Launcher(context)
            val plan = netplayPlan(profileName = null)
            val result = if (plan != null && Ps2GameSettings.canConfigureNow(context, rom)) {
                launcher.launchPrivateGame(rom, plan)
            } else {
                // Unsupported CHD codec or a pre-migration profile: keep the
                // proven accessibility setup as the compatibility fallback.
                launcher.launchGame(rom.uri)
            }
            if (result == LaunchResult.Success) onPs2Started()
            result to "ARMSX2"
        }
        Backend.MELONDS_WFC ->
            return context.getString(R.string.session_wfc_launch_from_library)
        Backend.NONE -> return context.getString(R.string.session_unsupported_console)
    }
    return when (result) {
        LaunchResult.Success -> {
            onLaunched()
            context.getString(R.string.session_launching, rom.displayName)
        }
        LaunchResult.NotInstalled -> context.getString(R.string.err_not_installed, emulator)
        is LaunchResult.NoNetplayUi -> context.getString(
            R.string.err_no_netplay_ui,
            emulator,
            result.versionName ?: "?"
        )
        is LaunchResult.Error -> context.getString(R.string.err_generic, result.message)
    }
}

/**
 * Step one: open the emulator on its multiplayer screen with the plan armed.
 * Null when it worked, a message otherwise.
 */
private fun Session.prepareNetplay(
    context: android.content.Context,
    azahar: AzaharLauncher,
    eden: EdenLauncher,
    profileName: String?
): String? {
    val plan = netplayPlan(profileName)
        ?: return context.getString(R.string.session_netplay_no_address)
    val (result, emulator) = when (backend) {
        Backend.AZAHAR -> azahar.openForNetplay(plan) to "Azahar"
        Backend.EDEN -> eden.openForNetplay(plan) to "Eden"
        // Dolphin has no multiplayer-less version to detect: netplay arrived in
        // the same screen as everything else, and a build that is too old simply
        // lacks the menu entry, at which point the driver stops and the card
        // says what to type.
        Backend.DOLPHIN -> DolphinLauncher(context).openForNetplay(
            plan,
            automationOn = azahar.isNetplayAutomationEnabled()
        ) to "Dolphin"
        // The PS2, whose driver goes and sets ARMSX2's Network screen. The game
        // is not passed here: Local Link is set in the app's settings, and the
        // DEV9 adapter initialises when the game boots, so a port set afterwards
        // would not be read back. The game goes at step two.
        Backend.ARMSX2 -> Ps2Launcher(context).openForLocalLink(
            plan,
            automationOn = azahar.isNetplayAutomationEnabled()
        ) to "ARMSX2"
        else -> return null
    }
    return when (result) {
        LaunchResult.Success -> null
        LaunchResult.NotInstalled -> context.getString(R.string.err_not_installed, emulator)
        is LaunchResult.NoNetplayUi -> context.getString(
            R.string.err_no_netplay_ui, emulator, result.versionName ?: "?"
        )
        is LaunchResult.Error -> context.getString(R.string.err_generic, result.message)
    }
}

/**
 * Both roles point Azahar at the host's tunnel address — `netPlayCreateRoom`
 * binds and self-joins on the same address (PHASE0_AZAHAR.md).
 * pourquoi : docs/decisions/session.md § Ce que chaque backend reçoit au lancement
 */
internal fun Session.netplayPlan(profileName: String?): NetplayPlan? {
    // With a room on the VPS nobody hosts: both players join it, and the host
    // stops being a link in the network. That is the whole point of the work,
    // the game no longer depends on a phone being reachable, and the tunnel does
    // not even have to be up in order to dial.
    room?.let {
        return NetplayPlan(
            role = NetplayPlan.Role.Guest,
            ip = it.host,
            port = it.port,
            username = NetplayNames.usernameFor(backend, profileName),
            password = it.password
        )
    }
    if (hostIp.isBlank()) return null
    return NetplayPlan(
        role = when (role) {
            Session.Role.HOST -> NetplayPlan.Role.Host
            Session.Role.GUEST -> NetplayPlan.Role.Guest
        },
        ip = hostIp,
        // The session's port when it carries one, otherwise the target
        // emulator's, 2626 for Dolphin, 24872 for the others. A shared default
        // would send the Dolphin guest to a silent port.
        port = port.toIntOrNull() ?: backend.defaultNetplayPort,
        // Eden only: it ships one default nickname to everybody, and two
        // players sharing one cannot share a room. Azahar keeps its own.
        // pourquoi : docs/decisions/session.md § Ce que chaque backend reçoit au lancement
        username = NetplayNames.usernameFor(backend, profileName),
        roomName = if (role == Session.Role.HOST) NetplayNames.roomName(code) else null,
        preferredGame = if (role == Session.Role.HOST) rom?.displayName else null,
        // On PS2 the session code doubles as the room code: ARMSX2 requires
        // one, identical on both sides, and negotiates nothing.
        // pourquoi : docs/decisions/session.md § Ce que chaque backend reçoit au lancement
        password = if (backend == Backend.ARMSX2) code else null
    )
}

private const val PRESENCE_MS = 5000L

/** ~15 s of silence before concluding the room is gone rather than the network flaky. */
private const val MAX_PRESENCE_MISSES = 3

/**
 * Combien d'images le pilote passe a reclamer le curseur quand les commandes
 * descendent au panneau. Six, comme la coquille, et pour la meme raison.
 */
private const val PILOT_FOCUS_FRAMES = 6
