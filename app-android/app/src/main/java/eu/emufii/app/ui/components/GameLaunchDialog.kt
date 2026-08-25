package eu.emufii.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import eu.emufii.app.R
import eu.emufii.app.compat.CompatEntry
import eu.emufii.app.compat.LocalCompatDb
import eu.emufii.app.library.Backend
import eu.emufii.app.library.Console
import androidx.compose.ui.platform.LocalContext
import eu.emufii.app.ps2.Ps2NetworkProfile
import eu.emufii.app.library.Rom
import eu.emufii.app.library.compatKeys
import eu.emufii.app.ui.controlRing
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.PillShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalConfiguration

/**
 * What you get when you pick a game: the game itself, what is about to happen,
 * and the one button that starts it.
 * pourquoi : docs/decisions/lancement-et-navigation.md § La carte a remplacé une feuille du bas, et deux fois pour cause
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameLaunchDialog(
    rom: Rom,
    onDismiss: () -> Unit,
    /** [private]: the session will not show up in the finder. */
    onPrimary: (private: Boolean) -> Unit,
    onJoinWithCode: (() -> Unit)?,
    /**
     * Open the game straight into its console's *public* multiplayer, with no
     * session and no tunnel; null for a console that has no such thing.
     * pourquoi : docs/decisions/lancement-et-navigation.md § Le choix du monde vient en premier, pas en dernier
     */
    onPlayOnline: (() -> Unit)? = null,
) {
    val dark = LocalEmufiiDarkTheme.current
    var starting by remember { mutableStateOf(false) }

    /**
     * A PS2 session with no network profile on the memory card cannot be played
     * whatever the tunnel does: the game's local menu never opens.
     * pourquoi : docs/decisions/lancement-et-navigation.md § Ce qui remplace les boutons quand un prérequis manque
     */
    val ps2Blocked = rom.console == Console.PS2 && !rememberPs2Ready()

    /**
     * Will the session be hidden from the finder? Public by default.
     * pourquoi : docs/decisions/lancement-et-navigation.md § « Session privée » promet exactement ce que le coordinator livre
     */
    var isPrivate by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    // Laid on its side when the screen is: stacked, it runs floor to ceiling
    // on a landscape handheld while leaving ~470 dp of width empty.
    // pourquoi : docs/decisions/lancement-et-navigation.md § La carte a remplacé une feuille du bas, et deux fois pour cause
    val wide = configuration.screenWidthDp > configuration.screenHeightDp
    // Still needed in the stacked arrangement, which portrait keeps.
    val compact = !wide && configuration.screenHeightDp < 520

    // Starts on "with friends": the public side rewrites the card rather than
    // opening a second screen.
    var publicMode by remember { mutableStateOf(false) }
    val online = rom.console.backend == Backend.MELONDS_WFC || publicMode

    /**
     * A PSP session leans on the per-game INI written to the memory stick; the
     * public online mode is not blocked, needing no grant.
     * pourquoi : docs/decisions/lancement-et-navigation.md § Ce qui remplace les boutons quand un prérequis manque
     */
    val pspBlocked = rom.console == Console.PSP && !online && !rememberPpssppReady()
    val setupBlocked = ps2Blocked || pspBlocked

    // A fixed beat, not a measurement: the work it precedes has its own
    // progress screen, and this one is for the eye.
    LaunchedEffect(starting) {
        if (starting) {
            delay(START_PAUSE_MS)
            if (publicMode) onPlayOnline?.invoke() else onPrimary(isPrivate)
        }
    }

    // Always live, including while starting up: disabled, a B during the launch
    // closed the app. It swallows the gesture and does nothing.
    // pourquoi : docs/decisions/lancement-et-navigation.md § Le curseur doit entrer dans la carte, et ne plus en sortir
    BackHandler { if (!starting) onDismiss() }

    // Flipped from a LaunchedEffect rather than started at 1f: an animation whose
    // initial value already equals its target never runs. Same idiom as the
    // tiles' arrival.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    // The cursor enters the box by its primary button. Fails in touch mode, by
    // design — note `adb input tap` opens in touch mode and mimics a focus bug.
    // pourquoi : docs/decisions/lancement-et-navigation.md § Le curseur doit entrer dans la carte, et ne plus en sortir
    val firstAction = remember { FocusRequester() }
    /**
     * The card's own root: a plain `focusable()` can take focus in touch mode
     * where a `clickable` cannot, so the card claims the keys either way.
     * pourquoi : docs/decisions/lancement-et-navigation.md § Le curseur doit entrer dans la carte, et ne plus en sortir
     */
    val cardRoot = remember { FocusRequester() }
    var rootHasCursor by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // `getOrDefault`, not `isSuccess`: `requestFocus` returns false without
        // throwing, so `runCatching` succeeds with false.
        // pourquoi : docs/decisions/lancement-et-navigation.md § Le curseur doit entrer dans la carte, et ne plus en sortir
        repeat(10) {
            if (runCatching { firstAction.requestFocus() }.getOrDefault(false)) {
                return@LaunchedEffect
            }
            delay(40)
        }
        // Touch mode: the buttons refused, so the card itself takes the keys.
        runCatching { cardRoot.requestFocus() }
    }

    val entrance by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "launch-card-entrance"
    )
    val steps = if (publicMode) {
        // Not the DS's steps: the DS dials its revival server on its own, the
        // PSP player has two settings to pick in PPSSPP first.
        // pourquoi : docs/decisions/lancement-et-navigation.md § Le choix du monde vient en premier, pas en dernier
        listOf(
            stringResource(R.string.launch_psp_public_1),
            stringResource(R.string.launch_psp_public_2),
            stringResource(R.string.launch_psp_public_3)
        )
    } else if (online) {
        listOf(
            stringResource(R.string.launch_online_1),
            stringResource(R.string.launch_online_2),
            stringResource(R.string.launch_online_3)
        )
    } else {
        listOf(
            stringResource(R.string.launch_session_1),
            stringResource(R.string.launch_session_2),
            stringResource(R.string.launch_session_3)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // The tray dims, it does not frost.
            // pourquoi : docs/decisions/lancement-et-navigation.md § Le plateau s'assombrit, il ne se dépolit pas
            .background(
                Color(0xFF060A12).copy(
                    alpha = (if (dark) 0.74f else 0.62f) * entrance
                )
            )
            // The backdrop swallows taps and is NOT a cursor stop: traversal
            // used to halt on it, at a node with no ring and no visible effect.
            // pourquoi : docs/decisions/lancement-et-navigation.md § Le curseur doit entrer dans la carte, et ne plus en sortir
            .focusProperties { canFocus = false }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !starting,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        SoftCard(
            modifier = Modifier
                .focusRequester(cardRoot)
                .onFocusEvent { rootHasCursor = it.isFocused }
                // The root holds the keys but never the cursor's look: the
                // first direction is spent handing it over, and swallowed.
                .onPreviewKeyEvent { event ->
                    if (!rootHasCursor || event.type != KeyEventType.KeyDown) {
                        return@onPreviewKeyEvent false
                    }
                    runCatching { firstAction.requestFocus() }.getOrDefault(false)
                }
                .focusable()
                // `exit` refuses the crossing in EVERY direction. Not to be
                // confused with `canFocus = false`, which kills the subtree.
                // pourquoi : docs/decisions/lancement-et-navigation.md § Le curseur doit entrer dans la carte, et ne plus en sortir
                .focusGroup()
                .focusProperties { onExit = { cancelFocusChange() } }
                // B closes here, in preview: measured, the first press only took
                // the cursor off the button and a second was needed.
                // pourquoi : docs/decisions/lancement-et-navigation.md § Le curseur doit entrer dans la carte, et ne plus en sortir
                .onPreviewKeyEvent { event ->
                    val back = event.key == Key.Back || event.key == Key.ButtonB
                    if (!back || starting) return@onPreviewKeyEvent false
                    if (event.type == KeyEventType.KeyUp) onDismiss()
                    true
                }
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .widthIn(max = if (wide) 648.dp else 360.dp)
                // Bounded by the screen, never by a number.
                // pourquoi : docs/decisions/lancement-et-navigation.md § La carte a remplacé une feuille du bas, et deux fois pour cause
                .heightIn(max = (configuration.screenHeightDp - 32).dp)
                // Arrives from slightly under its final size, like the tiles.
                .scale(0.92f + 0.08f * entrance)
                .alpha(entrance)
                // Swallows taps only. NO `canFocus = false` here: on the card it
                // disables the whole subtree, buttons included.
                // pourquoi : docs/decisions/lancement-et-navigation.md § Le curseur doit entrer dans la carte, et ne plus en sortir
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            val primaryLabel = stringResource(
                when {
                    publicMode -> R.string.lib_open_emulator
                    online -> R.string.lib_play_online
                    else -> R.string.lib_create_session
                }
            )

            if (wide) {
                // Two columns: the object on the left, what happens to it on
                // the right — room taken from spare width, not from each other.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(26.dp)
                ) {
                    // Centred: the geometry tells the two cases apart on its
                    // own, so there is no rule to add.
                    // pourquoi : docs/decisions/lancement-et-navigation.md § Ce qui cède, et dans quel ordre
                    Column(
                        modifier = Modifier
                            .width(186.dp)
                            .align(Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        RomArtwork(rom, size = 120.dp)
                        TitleBlock(rom, online)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            // Centred too, for the opposite reason to the left.
                            // pourquoi : docs/decisions/lancement-et-navigation.md § Ce qui cède, et dans quel ordre
                            .align(Alignment.CenterVertically),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // First, not last, and a selector rather than a link:
                        // it rewrites the card, it does not act.
                        // pourquoi : docs/decisions/lancement-et-navigation.md § Le choix du monde vient en premier, pas en dernier
                        if (onPlayOnline != null) {
                            ModeSwitch(
                                publicMode = publicMode,
                                enabled = !starting,
                                onPick = { publicMode = it }
                            )
                        }
                        // Yields first and alone: the explanation scrolls, the
                        // actions never do.
                        // pourquoi : docs/decisions/lancement-et-navigation.md § Ce qui cède, et dans quel ordre
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            steps.forEachIndexed { index, text -> Step(index + 1, text) }
                        }

                        // Stacked, never side by side: two pills sharing ~400 dp
                        // clip their labels silently. The switch sits above them.
                        // pourquoi : docs/decisions/lancement-et-navigation.md § Les boutons sont empilés, et c'est un piège évité
                        if (!online) {
                            PrivacyToggle(
                                checked = isPrivate,
                                enabled = !starting,
                                onChange = { isPrivate = it }
                            )
                        }
                        // Braced, and it is not a style point: an `else` whose branch sits
                        // at the same indentation as the `if` reads to a human as two
                        // statements, and Lint fails the build over it (SuspiciousIndentation).
                        if (ps2Blocked) {
                            Ps2ProfileMissing()
                        } else if (pspBlocked) {
                            PpssppSetupMissing()
                        } else {
                            PrimaryAction(
                                label = primaryLabel,
                                starting = starting,
                                onClick = { starting = true },
                                modifier = Modifier.fillMaxWidth().focusRequester(firstAction)
                            )
                        }
                        if (!setupBlocked && onJoinWithCode != null && !publicMode) {
                            OutlinedButton(
                                onClick = onJoinWithCode,
                                enabled = !starting,
                                shape = PillShape,
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                                    .controlRing(PillShape)
                            ) { Text(stringResource(R.string.lib_join_by_code)) }
                        }

                    }
                }
                return@SoftCard
            }

            Column(
                // Tighter when height is the scarce resource. Every dp taken off
                // the padding is a dp the explanation gets to keep, and the
                // explanation is the only part of this card that says anything.
                modifier = Modifier.fillMaxWidth().padding(if (compact) 16.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)
            ) {
                // As in two columns: the choice of world opens the card, it does
                // not conclude it.
                if (onPlayOnline != null) {
                    ModeSwitch(
                        publicMode = publicMode,
                        enabled = !starting,
                        onPick = { publicMode = it }
                    )
                }

                // The explanation scrolls; the two buttons never do.
                // pourquoi : docs/decisions/lancement-et-navigation.md § Ce qui cède, et dans quel ordre
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)
                ) {
                    // The artwork is decoration and yields first.
                    // pourquoi : docs/decisions/lancement-et-navigation.md § Ce qui cède, et dans quel ordre
                    RomArtwork(rom, size = if (compact) 72.dp else 104.dp)

                    TitleBlock(rom, online)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        steps.forEachIndexed { index, text -> Step(index + 1, text, compact) }
                    }
                }

                // Same reason as in two columns: no session, nothing to hide.
                if (!online) {
                    PrivacyToggle(
                        checked = isPrivate,
                        enabled = !starting,
                        onChange = { isPrivate = it }
                    )
                }

                // Braced, and it is not a style point: an `else` whose branch sits
                // at the same indentation as the `if` reads to a human as two
                // statements, and Lint fails the build over it (SuspiciousIndentation).
                if (ps2Blocked) {
                    Ps2ProfileMissing()
                } else if (pspBlocked) {
                    PpssppSetupMissing()
                } else {
                    PrimaryAction(
                        label = primaryLabel,
                        starting = starting,
                        onClick = { starting = true },
                        modifier = Modifier.fillMaxWidth().focusRequester(firstAction)
                    )
                }

                // Hidden in public mode: there is no session to join, exactly as
                // for DS online play.
                if (!setupBlocked && onJoinWithCode != null && !publicMode) {
                    OutlinedButton(
                        onClick = onJoinWithCode,
                        enabled = !starting,
                        shape = PillShape,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                            .controlRing(PillShape)
                    ) { Text(stringResource(R.string.lib_join_by_code)) }
                }

            }
        }
    }
}

/**
 * How long the card holds before handing over: long enough for the press to
 * register, and no longer.
 * pourquoi : docs/decisions/lancement-et-navigation.md § Le bouton garde sa couleur pendant qu'il travaille
 */
private const val START_PAUSE_MS = 350L

/** The title and the one line that says what pressing the button will do. */
@Composable
private fun TitleBlock(rom: Rom, online: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            rom.displayName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            stringResource(
                if (online) R.string.launch_mode_online else R.string.launch_mode_session,
                // The full label, not the tile badge's short one: "GC/Wii" is an
                // abbreviation that only makes sense squeezed into a corner of a
                // square.
                rom.console.label
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // The tile is scanned, this card is read: here there is room to say
        // what the mark means, and it is the last moment before it costs.
        // pourquoi : docs/decisions/lancement-et-navigation.md § Le verdict de compatibilité, là où la décision se prend
        LocalCompatDb.current.ratingFor(rom.compatKeys())?.let { known ->
            CompatNote(known)
        }
    }
}

/**
 * The verdict under the title: the bead, and its meaning in words. The rater's
 * own note is deliberately not shown.
 * pourquoi : docs/decisions/lancement-et-navigation.md § Le verdict de compatibilité, là où la décision se prend
 */
@Composable
private fun CompatNote(entry: CompatEntry) {
    Row(
        modifier = Modifier.padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompatBadge(entry.rating)
        Text(
            compatLabel(entry.rating),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * The button the card exists to get pressed. It keeps its colour while it
 * works: a grey button under a spinner reads as a fault.
 * pourquoi : docs/decisions/lancement-et-navigation.md § Le bouton garde sa couleur pendant qu'il travaille
 */

/**
 * "Private session": the label promises exactly what the coordinator delivers —
 * the session leaves the finder, and nothing more.
 * pourquoi : docs/decisions/lancement-et-navigation.md § « Session privée » promet exactement ce que le coordinator livre
 */
@Composable
private fun PrivacyToggle(
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .controlRing(PillShape)
            .clip(PillShape)
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.lib_private_session),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(
                    if (checked) R.string.lib_private_session_on
                    else R.string.lib_private_session_off
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Le meme interrupteur que les reglages, et c'est le dernier controle
        // Material qui restait a l'ecran : sa piste plate et sa pastille sans
        // relief se lisaient comme un autocollant pose sur une plaque moulee.
        // pourquoi : docs/decisions/reglages-ecran.md § Un réglage qui n'a que deux états est un interrupteur
        SwitchFace(checked = checked)
    }
}

/**
 * With friends, or online: a selector, not two buttons — nothing sets off when
 * it is touched. Each half carries the ring on its own side.
 * pourquoi : docs/decisions/lancement-et-navigation.md § Le choix du monde vient en premier, pas en dernier
 */
@Composable
private fun ModeSwitch(
    publicMode: Boolean,
    enabled: Boolean,
    onPick: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(PillShape)
            // Distinctly darker than the card: at 55 % of `surfaceVariant` the
            // selected half had nothing to stand out against.
            .background(
                if (LocalEmufiiDarkTheme.current) Color(0x1FFFFFFF) else Color(0x14000000)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ModeSegment(
            label = stringResource(R.string.lib_mode_friends),
            selected = !publicMode,
            enabled = enabled,
            onClick = { onPick(false) },
            modifier = Modifier.weight(1f)
        )
        ModeSegment(
            label = stringResource(R.string.lib_mode_public),
            selected = publicMode,
            enabled = enabled,
            onClick = { onPick(true) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ModeSegment(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // The selected half is solid, the other transparent: the selection reads
    // without reading either label. The card's background rather than an accent
    // tint, this app's colour comes from the content, not from the chrome.
    val fill =
        if (selected) softCardFill() else Color.Transparent
    Box(
        modifier = modifier
            .controlRing(PillShape)
            // The shadow makes the selection: two flat tints read as two
            // colours, a pill lifting off reads as a choice taken.
            .then(
                if (selected) Modifier.shadow(3.dp, PillShape, spotColor = Color.Black)
                else Modifier
            )
            .clip(PillShape)
            .background(fill)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color =
                if (selected) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * What replaces the launch buttons when the PS2 profile is missing: the
 * prerequisite and where to settle it, and nothing else.
 * pourquoi : docs/decisions/lancement-et-navigation.md § Ce qui remplace les boutons quand un prérequis manque
 */
@Composable
private fun Ps2ProfileMissing() {
    Text(
        stringResource(R.string.launch_ps2_profile_missing),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error
    )
}

/** As [Ps2ProfileMissing]: the prerequisite, where to settle it, nothing else. */
@Composable
private fun PpssppSetupMissing() {
    Text(
        stringResource(R.string.launch_ppsspp_setup_missing),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
private fun PrimaryAction(
    label: String,
    starting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !starting,
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier.height(52.dp).controlRing(PillShape)

    ) {
        if (starting) {
            // In the button rather than replacing it: the card keeps its size,
            // so nothing jumps while the pause runs.
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            // No maxLines: capping at one clipped "Créer une session" silently.
            // pourquoi : docs/decisions/lancement-et-navigation.md § Les boutons sont empilés, et c'est un piège évité
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * One line of the walkthrough. Numbered dots, not bullets: the lines are a
 * sequence.
 * pourquoi : docs/decisions/lancement-et-navigation.md § Le bouton garde sa couleur pendant qu'il travaille
 */
@Composable
private fun Step(number: Int, text: String, compact: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text,
            // One notch down where the card is height-starved: three steps at
            // bodyMedium do not fit above two pinned buttons in landscape.
            style = if (compact) MaterialTheme.typography.bodySmall
                    else MaterialTheme.typography.bodyMedium,
            color = LocalContentColor.current.copy(alpha = 0.85f)
        )
    }
}
