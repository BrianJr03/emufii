package eu.emufii.app.ui.screens

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.emufii.app.R
import eu.emufii.app.library.Backend
import eu.emufii.app.network.CoordinatorClient
import eu.emufii.app.profile.Profile
import eu.emufii.app.secondscreen.PanelStep
import eu.emufii.app.secondscreen.SecondScreen
import eu.emufii.app.secondscreen.rememberPresentationDisplay
import eu.emufii.app.session.Session
import eu.emufii.app.settings.SettingsStore
import eu.emufii.app.ui.screens.session.CodeCard
import eu.emufii.app.ui.screens.session.ConnectionCard
import eu.emufii.app.ui.screens.session.EmulatorHintCard
import eu.emufii.app.ui.screens.session.LaunchButton
import eu.emufii.app.ui.screens.session.LeaveButton
import eu.emufii.app.ui.screens.session.NetplayButton
import eu.emufii.app.ui.screens.session.OfflineCard
import eu.emufii.app.ui.screens.session.PresenceCard
import eu.emufii.app.ui.screens.session.PspHintCard
import eu.emufii.app.ui.screens.session.PspSetupButton
import eu.emufii.app.ui.screens.session.SessionCodeChip
import eu.emufii.app.ui.screens.session.StatusLine
import eu.emufii.app.ui.screens.session.danger
import eu.emufii.app.ui.screens.session.launchEnabled
import eu.emufii.app.ui.screens.session.launchLabel
import eu.emufii.app.ui.screens.session.rememberSessionScreenState
import eu.emufii.app.ui.CONFIRM_KEYS
import eu.emufii.app.ui.LocalRingTone
import eu.emufii.app.ui.RingTone
import eu.emufii.app.ui.Sfx
import eu.emufii.app.ui.components.CrossIcon
import eu.emufii.app.ui.components.EmufiiScaffold
import eu.emufii.app.ui.components.GhostButton
import eu.emufii.app.ui.components.LocalScaffoldFocus
import eu.emufii.app.ui.components.PadDialog
import eu.emufii.app.ui.components.PadDialogText
import eu.emufii.app.ui.components.RomArtwork
import eu.emufii.app.ui.components.padEntry

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
    val state = rememberSessionScreenState(
        session = session,
        client = client,
        profile = profile,
        onSessionEnded = onSessionEnded,
    )
    val ui by state.uiState.collectAsStateWithLifecycle()
    val status = ui.status
    val netplayPrepared = ui.netplayPrepared
    val netplayDone = ui.netplayDone
    val pspOpened = ui.pspOpened
    val offline = ui.offline
    val automationOn = ui.automationOn
    val launched = ui.launched
    val sessionArt = ui.sessionArt
    val confirmingLeave = ui.confirmingLeave
    val ps2Automatic = ui.ps2Automatic
    val pspAutomatic = state.pspAutomatic
    val waitingForHost = ui.waitingForHost

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) state.onResumed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val onNetplayStep = state::onNetplayStep
    val onLaunchStep = state::onLaunchStep

    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // An up-to-date coordinator returns the handle, an old one the friend code.
    val others = ui.others

    val localWindowInfo = LocalWindowInfo.current
    val landscape = localWindowInfo.containerSize.width > localWindowInfo.containerSize.height

    // Hoisted: computed inside one column the value would be true on one side only.
    // pourquoi : docs/decisions/session.md § The address shown is the one to type, never another
    val psp = session.backend == Backend.PPSSPP
    // With a VPS room nobody hosts, so the host's address is the address of nothing.
    // pourquoi : docs/decisions/session.md § The address shown is the one to type, never another
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
    val onCopyCode = state::onCopyCode

    // The setting is not enough: the device may have only one screen.
    // pourquoi : docs/decisions/session.md § What the rear panel carries, the front screen does not repeat
    val panelDisplay by rememberPresentationDisplay()
    val panelWanted by remember(context) { SettingsStore.get(context).secondScreen }
        .collectAsStateWithLifecycle()
    val panelLive = panelWanted && panelDisplay != null

    // Resolved once and served to both screens: the labels travel already translated.
    // pourquoi : docs/decisions/second-ecran.md § What travels to the panel travels already resolved
    // pourquoi : docs/decisions/second-ecran.md § The panel takes the steps, because it is touch
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
                        state.openPspSetup()
                    }
                )
            )
        }
        add(
            PanelStep(
                // Once launched the step keeps its place and changes face, still pressable.
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
    // The lambdas belong to this composition: clear them on the way out, or the panel keeps a dead session.
    DisposableEffect(panelLive, panelSteps) {
        SecondScreen.publishSteps(if (panelLive) panelSteps else emptyList())
        onDispose { SecondScreen.publishSteps(emptyList()) }
    }

    // Focus does not cross windows, so the front pad drives the panel's steps.
    // pourquoi : docs/decisions/second-ecran.md § R turns the page from both screens
    val panelCursor by SecondScreen.stepCursor.collectAsStateWithLifecycle()

    // The social domain: the pad cursor turns coral here.
    // pourquoi : docs/decisions/theme-duotone-shelves.md § GAMEPAD FOCUS
    CompositionLocalProvider(LocalRingTone provides RingTone.CORAL) {
        EmufiiScaffold(
            title = if (session.role == Session.Role.HOST) stringResource(R.string.session_mine) else stringResource(
                R.string.session_joined
            ),
            modifier = modifier,
            onBack = state::confirmLeave,
            backIcon = { CrossIcon(size = 20.dp, color = danger()) },
            // In landscape, leave moves into the header and the 60 dp go back to the left pane.
            // pourquoi : docs/decisions/session.md § What the panel carries, the front screen gives back in space
            trailing = if (landscape && !panelLive) {
                { SessionCodeChip(code = session.code, onCopy = onCopyCode) }
            } else null,
            // Both panes fit, so nothing rises under the header.
            contentScrolls = !landscape
        ) { topPadding ->
            // The pilot lives in the scaffold: leaving past the first step lands on its cross.
            val scaffoldFocus = LocalScaffoldFocus.current

            /**
             * Placed as soon as the steps exist, without waiting for the pilot to take focus.
             * pourquoi : docs/decisions/second-ecran.md § The panel's cursor does not depend on focus, which never arrives
             */
            LaunchedEffect(panelLive, panelSteps) {
                if (panelLive &&
                    panelSteps.isNotEmpty() &&
                    SecondScreen.stepCursor.value == null
                ) {
                    SecondScreen.selectStep(0)
                }
            }

            // One `focusRequester`, the scaffold's: two stacked and the node never took focus.
            // pourquoi : docs/decisions/session.md § One `focusRequester` per node, and it is the shell's
            val pilotFocus = remember(scaffoldFocus) { scaffoldFocus?.first ?: FocusRequester() }

            /**
             * Frame by frame: one request after 150 ms lost against Compose's initial focus.
             * pourquoi : docs/decisions/session.md § One `focusRequester` per node, and it is the shell's
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
                // Before `focusable()`, never after: `onFocusChanged` observes what follows it.
                .onFocusChanged { state ->
                    // Giving the cursor back to the pilot gives it back to the steps.
                    if (state.isFocused &&
                        SecondScreen.stepCursor.value == null &&
                        SecondScreen.steps.value.isNotEmpty()
                    ) {
                        SecondScreen.selectStep(0)
                    }
                }
                .focusable()
                // Straight onto the panel's steps: an intermediate stop would be invisible.

                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown && event.type != KeyEventType.KeyUp) {
                        return@onKeyEvent false
                    }
                    fun leavePanel() {
                        SecondScreen.clearStepCursor()
                        scaffoldFocus?.header?.let { runCatching { it.requestFocus() } }
                    }

                    val cursor = panelCursor
                    // Down, once the front cursor has nothing left below it.
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
                            // A and its synonyms press the aimed step; KeyDown is swallowed so one press reads once.
                            event.key in CONFIRM_KEYS -> {
                                if (event.type == KeyEventType.KeyUp) {
                                    steps.getOrNull(cursor)?.takeIf { it.enabled }
                                        ?.let { Sfx.click(); it.onPress() }
                                }
                                true
                            }

                            event.type == KeyEventType.KeyDown -> when (event.key) {
                                Key.DirectionLeft -> {
                                    SecondScreen.moveStep(-1); true
                                }

                                Key.DirectionRight -> {
                                    SecondScreen.moveStep(1); true
                                }

                                Key.DirectionUp -> {
                                    if (cursor == 0) leavePanel() else SecondScreen.moveStep(-1)
                                    true
                                }
                                // One row: down has nowhere to go, and must not hand the cursor back.
                                Key.DirectionDown -> true
                                Key.ButtonB, Key.Back -> {
                                    leavePanel(); true
                                }

                                else -> false
                            }

                            else -> false
                        }
                    }
                }
            if (landscape) {
                // State on the left, what is left to do on the right, every answer under the
                // button that produced it.
                // pourquoi : docs/decisions/session.md § Two panels, because stacked this screen does not fit
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
                    // No `verticalScroll`: a state pane that can hide its state is not doing its job.
                    // pourquoi : docs/decisions/session.md § The state panel does not scroll, so it has to fit
                    // pourquoi : docs/decisions/session.md § What the panel carries, the front screen gives back in space
                    Column(
                        modifier = Modifier.width(if (panelLive) 220.dp else 272.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Presence gives way, never the address: the weight reverses Compose's
                        // measuring order to guarantee it.
                        // pourquoi : docs/decisions/session.md § The state panel does not scroll, so it has to fit
                        PresenceCard(
                            youName = profile.name,
                            others = others,
                            isHost = session.role == Session.Role.HOST,
                            live = !offline,
                            scrollable = true,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        // The panel already carries both address and port, unasked.
                        // pourquoi : docs/decisions/session.md § What the rear panel carries, the front screen does not repeat
                        if (!panelLive) {
                            ConnectionCard(
                                hostIp = shownAddress,
                                addressLabel = addressLabel,
                                port = shownPort,
                                romName = session.rom?.displayName
                            )
                        }

                        // Only once the panel has freed the room; without one there is no gap to fill.
                        // pourquoi : docs/decisions/session.md § What the panel carries, the front screen gives back in space
                        if (panelLive) {
                            sessionArt?.let { art ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f, fill = false),
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
                        // The explanation gives way, never the buttons; the fade exists on one screen only.
                        // pourquoi : docs/decisions/session.md § What the panel carries, the front screen gives back in space
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

                        // With the panel carrying the steps, the front screen does not redraw them.
                        // pourquoi : docs/decisions/second-ecran.md § The panel takes the steps, because it is touch
                        if (!panelLive) {
                            // The first button that exists AND responds: a disabled one does not take focus.
                            // pourquoi : docs/decisions/session.md § Down aims at the first button that answers
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
                                    onClick = state::openPspSetup,
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
                                // Last resort: with no step above, this is the first button on the page.
                                modifier = if ((session.backend.hasNetplay && !ps2Automatic) ||
                                    (session.backend == Backend.PPSSPP && !pspAutomatic)
                                ) Modifier
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
                // pourquoi : docs/decisions/session.md § What is done by hand is said before the button, never after
                if (session.backend == Backend.PPSSPP) PspHintCard(pspAutomatic)

                // Above the member list, which is what has gone stale: whoever is shown there was
                // here the last time we heard back.
                if (offline) OfflineCard()

                PresenceCard(
                    youName = profile.name,
                    others = others,
                    isHost = session.role == Session.Role.HOST,
                    live = !offline
                )

                // As in landscape: what the panel reports, the front screen does not repeat.
                // pourquoi : docs/decisions/session.md § What the rear panel carries, the front screen does not repeat
                if (!panelLive) {
                    ConnectionCard(
                        hostIp = shownAddress,
                        addressLabel = addressLabel,
                        // No port: the ad hoc server's is fixed and PPSSPP does not ask for it.
                        port = shownPort,
                        romName = session.rom?.displayName
                    )
                }

                // Before the buttons: Azahar refuses the room over the nickname while blaming the address.
                // pourquoi : docs/decisions/session.md § What is done by hand is said before the button, never after
                EmulatorHintCard(
                    session = session,
                    automationOn = automationOn,
                )

                // As in landscape: the panel carries the steps when it is there.
                // pourquoi : docs/decisions/second-ecran.md § The panel takes the steps, because it is touch
                if (!panelLive) {
                    // The order the emulator expects: join the room from its main menu, then boot the
                    // game. One button did both, and the ROM started in an emulator that had joined nothing.
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

                    // The button does not apply the settings, it opens the emulator, and says so.
                    // pourquoi : docs/decisions/session.md § The per-console cards, and what each must prevent
                    if (session.backend == Backend.PPSSPP && !pspAutomatic) {
                        PspSetupButton(
                            pspOpened = pspOpened,
                            onClick = {
                                state.openPspSetup()
                            },
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
                            (session.backend == Backend.PPSSPP && !pspAutomatic)
                        ) Modifier
                        else Modifier.padEntry()
                    )
                }

                // Under the button that produces it: rendered last, a refusal landed off-screen
                // and read as a dead button.
                // pourquoi : docs/decisions/session.md § What is done by hand is said before the button, never after
                status?.let { StatusLine(it) }

                LeaveButton(session = session, onLeave = state::confirmLeave)
            }
        }
    }

    if (confirmingLeave) {
        val host = session.role == Session.Role.HOST
        PadDialog(
            title = stringResource(if (host) R.string.session_close else R.string.session_leave),
            onDismiss = state::dismissLeave,
            // The dialog that made the panel most wrong: it kept showing the code while asking to leave.
            panelDetail = stringResource(
                if (host) R.string.session_close_confirm else R.string.session_leave_confirm
            ),
            panelSocial = true,
            actions = {
                GhostButton(
                    label = stringResource(R.string.common_cancel),
                    onClick = state::dismissLeave
                )
                GhostButton(
                    label = stringResource(if (host) R.string.session_close else R.string.session_leave),
                    onClick = {
                        state.dismissLeave()
                        onLeave()
                    },
                    tint = danger()
                )
            }
        ) {
            // Host and guest do not risk the same thing: one closes for everyone, the other withdraws.
            PadDialogText(
                stringResource(
                    if (host) R.string.session_close_confirm else R.string.session_leave_confirm
                )
            )
        }
    }
}


/** How many frames the pilot spends claiming the cursor, like the scaffold. */
private const val PILOT_FOCUS_FRAMES = 6
