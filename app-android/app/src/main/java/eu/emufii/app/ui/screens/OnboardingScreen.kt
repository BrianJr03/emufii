package eu.emufii.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.emufii.app.R
import eu.emufii.app.artwork.CocoonMedia
import eu.emufii.app.azahar.AzaharLauncher
import eu.emufii.app.library.Console
import eu.emufii.app.profile.Profile
import eu.emufii.app.ps2.Ps2NetworkProfile
import eu.emufii.app.psp.PpssppConfigStore
import eu.emufii.app.settings.SettingsStore
import eu.emufii.app.ui.components.ChipMark
import eu.emufii.app.ui.components.ConsoleGrid
import eu.emufii.app.ui.components.DetailActions
import eu.emufii.app.ui.components.DetailTone
import eu.emufii.app.ui.components.FolderMark
import eu.emufii.app.ui.components.GhostButton
import eu.emufii.app.ui.components.GridMark
import eu.emufii.app.ui.components.PadTextField
import eu.emufii.app.ui.components.PaintMark
import eu.emufii.app.ui.components.PersonMark
import eu.emufii.app.ui.components.PrimaryButton
import eu.emufii.app.ui.components.SignalMark
import eu.emufii.app.ui.components.SoftCard
import eu.emufii.app.ui.components.SteamGridDbMark
import eu.emufii.app.ui.components.waitTrim
import eu.emufii.app.ui.controlRing
import eu.emufii.app.ui.screens.settings.AutofillBlock
import eu.emufii.app.ui.screens.settings.BlockFact
import eu.emufii.app.ui.screens.settings.BlockNotice
import eu.emufii.app.ui.screens.settings.PpssppBlock
import eu.emufii.app.ui.screens.settings.Ps2Block
import eu.emufii.app.ui.screens.settings.SettingsSteps
import eu.emufii.app.ui.screens.settings.StatePill
import eu.emufii.app.ui.theme.ArtworkShape
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.PillShape
import eu.emufii.app.ui.theme.Teal
import eu.emufii.app.ui.theme.socket
import eu.emufii.app.ui.wallpaper.TrayBackdrop
import kotlinx.coroutines.delay

/**
 * Le premier lancement : ce qu'il faut savoir, puis ce qu'il faut faire.
 *
 * Le parcours n'a pas de longueur fixe — les pages d'emulateur sont tirees de ce
 * que le joueur repond a la page des consoles — et chaque page a deux colonnes,
 * le pourquoi a gauche, le quoi faire a droite.
 * pourquoi : docs/decisions/onboarding.md § Le parcours n'a pas de longueur fixe
 */
@Composable
fun OnboardingScreen(
    initialName: String,
    onSetName: (String) -> Unit,
    onPickFolder: (Uri) -> Unit,
    onSetArtworkKey: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalEmufiiDarkTheme.current
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore.get(context) }
    val hiddenConsoles by settingsStore.hiddenConsoles.collectAsState()
    val cocoonFolder by settingsStore.cocoonFolder.collectAsState()

    var name by remember { mutableStateOf(initialName) }
    var artworkKey by remember { mutableStateOf("") }
    val nameTooShort = name.trim().length < Profile.MIN_NAME_LENGTH

    var romFolder by remember { mutableStateOf<Uri?>(null) }
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            onPickFolder(uri)
            romFolder = uri
        }
    }

    // Lecture seule, comme dans les reglages : on regarde les images que Cocoon
    // a deja telechargees, on n'ecrit rien dans son dossier.
    val cocoonPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            settingsStore.setCocoonFolder(uri.toString())
            CocoonMedia.forget()
        }
    }

    // Depuis la vraie permission, pas depuis « le joueur a-t-il appuye » : elle
    // peut deja etre accordee, et le bouton ne ferait alors rien.
    var notificationsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var notificationsRefused by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Un refus montrait le meme ✓ qu'un accord, ce qui etait un mensonge —
        // et il compte, parce qu'apres deux refus Android cesse d'afficher la
        // demande et le bouton ne ferait plus jamais rien.
        notificationsGranted = granted
        notificationsRefused = !granted
    }

    val ppssppConfig = remember(context) { PpssppConfigStore(context) }
    var ppssppReady by remember { mutableStateOf(ppssppConfig.isReady()) }
    var ps2Ready by remember { mutableStateOf(Ps2NetworkProfile.isReadyQuick(context)) }
    LaunchedEffect(Unit) { ps2Ready = Ps2NetworkProfile.verifyReady(context) }

    // Un aller-retour dans les reglages d'Android : pas de resultat a attendre,
    // la reponse ne se voit qu'au retour, donc on sonde.
    val launcher = remember { AzaharLauncher(context) }
    var autofillOn by remember { mutableStateOf(launcher.isNetplayAutomationEnabled()) }

    // Tenu par sa valeur et non par un index : masquer une console retire une
    // page, et un index designerait alors la suivante.
    val steps = remember(hiddenConsoles) { onboardingSteps(hiddenConsoles) }
    var current by remember { mutableStateOf(OnbStep.WELCOME) }
    val index = steps.indexOf(current).coerceAtLeast(0)
    val last = index == steps.lastIndex

    LaunchedEffect(current) {
        if (current == OnbStep.AUTOFILL) {
            while (true) {
                autofillOn = launcher.isNetplayAutomationEnabled()
                delay(700)
            }
        }
    }

    fun goNext() {
        if (current == OnbStep.NAME) onSetName(name.trim())
        if (current == OnbStep.ARTWORK) onSetArtworkKey(artworkKey.trim())
        if (last) onDone() else current = steps[index + 1]
    }

    fun goBack() {
        if (index > 0) current = steps[index - 1]
    }

    // Le retour se fait au bouton systeme et a la touche B : un troisieme
    // controle ferait trois choses a lire pour une decision.
    BackHandler(enabled = index > 0) { goBack() }

    // Les elements fixes se resserrent aussi : sur 468 dp de haut, leurs seules
    // marges depassaient la place disponible.
    val configuration = LocalConfiguration.current
    val shortScreen = configuration.screenHeightDp < 520
    val wide = configuration.screenWidthDp >= 720
    val gap = if (shortScreen) 12.dp else 18.dp
    val edge = if (shortScreen) 10.dp else 18.dp
    val actionHeight = if (shortScreen) 48.dp else 56.dp

    Box(modifier = modifier.fillMaxSize()) {
        TrayBackdrop(modifier = Modifier.fillMaxSize(), dark = dark)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = if (wide) 40.dp else 22.dp, vertical = edge),
            verticalArrangement = Arrangement.spacedBy(gap),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StepRail(current = index, total = steps.size, label = stringResource(current.railLabel))

            // La page defile, le bouton reste : le poids sert le bouton d'abord.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = current,
                    transitionSpec = {
                        val forward = steps.indexOf(targetState) > steps.indexOf(initialState)
                        (slideInHorizontally { if (forward) it / 3 else -it / 3 } + fadeIn())
                            .togetherWith(
                                slideOutHorizontally { if (forward) -it / 4 else it / 4 } + fadeOut()
                            )
                    },
                    label = "onboarding-step"
                ) { shown ->
                    StepBody(
                        step = shown,
                        wide = wide,
                        name = name,
                        onNameChange = { name = it.take(Profile.MAX_NAME_LENGTH) },
                        nameTooShort = nameTooShort,
                        romFolder = romFolder,
                        onPickFolder = { folderPicker.launch(null) },
                        hiddenConsoles = hiddenConsoles,
                        onSetConsoleVisible = settingsStore::setConsoleVisible,
                        cocoonFolder = cocoonFolder,
                        onPickCocoon = { cocoonPicker.launch(COCOON_DEFAULT_FOLDER) },
                        onForgetCocoon = {
                            settingsStore.setCocoonFolder("")
                            CocoonMedia.forget()
                        },
                        artworkKey = artworkKey,
                        onArtworkKeyChange = { artworkKey = it },
                        ppssppConfig = ppssppConfig,
                        ppssppReady = ppssppReady,
                        onPpssppReady = { ppssppReady = it },
                        ps2Ready = ps2Ready,
                        onPs2Ready = { ps2Ready = it },
                        profileName = name,
                        autofillOn = autofillOn,
                        onOpenAutofill = { launcher.openAccessibilitySettings() },
                        notificationsGranted = notificationsGranted,
                        notificationsRefused = notificationsRefused,
                        onAskNotifications = {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                    )
                }
            }

            // Les deux sorties sur une ligne : empilees, elles coutent une
            // rangee de plus a une page qui n'en a pas.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrimaryButton(
                    label = stringResource(
                        when {
                            current == OnbStep.WELCOME -> R.string.onb_start
                            last -> R.string.onb_finish
                            else -> R.string.onb_next
                        }
                    ),
                    onClick = { goNext() },
                    // La seule page qu'on ne traverse pas d'un geste : le pseudo
                    // part tel quel dans le formulaire de l'emulateur.
                    // pourquoi : docs/decisions/onboarding.md § Tout reste passable, sauf le pseudo
                    enabled = current != OnbStep.NAME || !nameTooShort,
                    modifier = Modifier.weight(1f).height(actionHeight)
                )

                // Offert seulement la ou l'on demande quelque chose : la page
                // d'accueil n'invite pas a sauter une page qui ne demande rien,
                // et le recapitulatif final n'a rien a sauter.
                if (current.skippable) {
                    GhostButton(
                        label = stringResource(R.string.onb_skip),
                        onClick = { goNext() },
                        modifier = Modifier.height(actionHeight)
                    )
                }
            }
        }
    }
}

/** Le dossier ou Cocoon range ses images, pour ouvrir le selecteur au bon endroit. */
private val COCOON_DEFAULT_FOLDER: Uri? = null

/** Une page du parcours. L'ordre de l'enum est l'ordre du parcours. */
private enum class OnbStep(val railLabel: Int, val skippable: Boolean = true) {
    WELCOME(R.string.onb_rail_welcome, skippable = false),
    NAME(R.string.onb_rail_name, skippable = false),
    FOLDER(R.string.onb_rail_folder),
    CONSOLES(R.string.onb_rail_consoles),
    COCOON(R.string.onb_rail_cocoon),
    ARTWORK(R.string.onb_rail_artwork),
    PPSSPP(R.string.onb_rail_ppsspp),
    PS2(R.string.onb_rail_ps2),
    AUTOFILL(R.string.onb_rail_autofill),
    NOTIF(R.string.onb_rail_notif),
    DONE(R.string.onb_rail_done, skippable = false),
}

/**
 * Les consoles dont le multijoueur passe par le pilotage de l'emulateur. Ni la
 * PSP (tunnel) ni la DS (DNS) n'ont d'ecran a remplir.
 * pourquoi : docs/decisions/onboarding.md § Le parcours n'a pas de longueur fixe
 */
private val AUTOMATED = setOf(
    Console.THREE_DS,
    Console.SWITCH,
    Console.GAMECUBE,
    Console.WII,
    Console.PS2,
)

/** Le parcours, taille sur les consoles que le joueur garde. */
private fun onboardingSteps(hidden: Set<Console>): List<OnbStep> = buildList {
    add(OnbStep.WELCOME)
    add(OnbStep.NAME)
    add(OnbStep.FOLDER)
    add(OnbStep.CONSOLES)
    add(OnbStep.COCOON)
    add(OnbStep.ARTWORK)
    if (Console.PSP !in hidden) add(OnbStep.PPSSPP)
    if (Console.PS2 !in hidden) add(OnbStep.PS2)
    if (AUTOMATED.any { it !in hidden }) add(OnbStep.AUTOFILL)
    add(OnbStep.NOTIF)
    add(OnbStep.DONE)
}

/**
 * La mise en page d'une page : le *pourquoi* a gauche, le *quoi faire* a droite.
 * En etroit, la meme chose empilee. Une page sans travail centre sa colonne.
 * pourquoi : docs/decisions/onboarding.md § Deux colonnes, et elles ne disent pas la même chose
 */
@Composable
private fun StepLayout(
    wide: Boolean,
    mark: @Composable () -> Unit,
    title: String,
    body: String,
    /** L'etat de ce que la page demande, quand il y en a un a montrer. */
    state: (@Composable () -> Unit)? = null,
    /**
     * Vrai quand le travail a besoin de toute la largeur : le pourquoi devient
     * alors un bandeau. Une seule page le demande, celle des consoles.
     * pourquoi : docs/decisions/onboarding.md § La page des consoles prend toute la largeur
     */
    fullWidthWork: Boolean = false,
    work: (@Composable () -> Unit)? = null,
) {
    val why: @Composable (Modifier) -> Unit = { m ->
        Column(
            modifier = m,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = if (wide && work != null) Alignment.Start else Alignment.CenterHorizontally
        ) {
            mark()
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = if (wide && work != null) TextAlign.Start else TextAlign.Center
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = if (wide && work != null) TextAlign.Start else TextAlign.Center
            )
            state?.invoke()
        }
    }

    when {
        work == null -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SoftCard(modifier = Modifier.waitTrim()) {
                Box(Modifier.fillMaxWidth().padding(26.dp)) { why(Modifier.fillMaxWidth()) }
            }
        }

        wide && fullWidthWork -> Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                mark()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            state?.invoke()
            work()
        }

        wide -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(26.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sans plaque : elle parle par-dessus le plateau, comme un titre
            // d'ecran, ce qui laisse la seule plaque a ce qui se fait.
            why(Modifier.weight(0.42f))
            Box(Modifier.weight(0.58f)) { work() }
        }

        else -> Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            why(Modifier.fillMaxWidth())
            work()
        }
    }
}

/** La marque d'une page : le glyphe de l'app dans le meme creux que les icones d'emulateur. */
@Composable
private fun StepMark(size: Dp = 64.dp, glyph: @Composable (Color) -> Unit) {
    val dark = LocalEmufiiDarkTheme.current
    Box(
        modifier = Modifier.size(size).socket(ArtworkShape, dark),
        contentAlignment = Alignment.Center
    ) {
        glyph(MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** La marque de l'app elle-meme, pour l'accueil et l'adieu. */
@Composable
private fun LogoMark(size: Dp = 96.dp) {
    Image(
        painter = painterResource(R.drawable.emufii_logo_v3),
        contentDescription = null,
        modifier = Modifier.size(size)
    )
}

/** La carte de travail. Les pages d'emulateur posent le bloc des reglages a la place. */
@Composable
private fun WorkCard(content: @Composable () -> Unit) {
    SoftCard(modifier = Modifier.waitTrim()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) { content() }
    }
}

/** Le contenu d'une page, aiguille par [OnbStep]. */
@Composable
private fun StepBody(
    step: OnbStep,
    wide: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    nameTooShort: Boolean,
    romFolder: Uri?,
    onPickFolder: () -> Unit,
    hiddenConsoles: Set<Console>,
    onSetConsoleVisible: (Console, Boolean) -> Unit,
    cocoonFolder: String,
    onPickCocoon: () -> Unit,
    onForgetCocoon: () -> Unit,
    artworkKey: String,
    onArtworkKeyChange: (String) -> Unit,
    ppssppConfig: PpssppConfigStore,
    ppssppReady: Boolean,
    onPpssppReady: (Boolean) -> Unit,
    ps2Ready: Boolean,
    onPs2Ready: (Boolean) -> Unit,
    profileName: String,
    autofillOn: Boolean,
    onOpenAutofill: () -> Unit,
    notificationsGranted: Boolean,
    notificationsRefused: Boolean,
    onAskNotifications: () -> Unit,
) = when (step) {

    OnbStep.WELCOME -> StepLayout(
        wide = wide,
        mark = { LogoMark() },
        title = stringResource(R.string.onb_welcome_title),
        body = stringResource(R.string.onb_welcome_body),
    )

    OnbStep.NAME -> StepLayout(
        wide = wide,
        mark = { StepMark { PersonMark(size = 34.dp, color = it) } },
        title = stringResource(R.string.onb_name_title),
        body = stringResource(R.string.onb_name_body),
        work = {
            WorkCard {
                PadTextField(
                    value = name,
                    onValueChange = onNameChange,
                    isError = nameTooShort,
                    shape = PillShape,
                    label = stringResource(R.string.onb_name_field),
                    supportingText = {
                        if (nameTooShort) {
                            Text(
                                stringResource(
                                    R.string.onb_name_too_short,
                                    Profile.MIN_NAME_LENGTH
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    stringResource(R.string.onb_name_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )

    OnbStep.FOLDER -> StepLayout(
        wide = wide,
        mark = { StepMark { FolderMark(size = 36.dp, color = it) } },
        title = stringResource(R.string.onb_folder_title),
        body = stringResource(R.string.onb_folder_body),
        work = {
            WorkCard {
                if (romFolder == null) {
                    SettingsSteps(
                        stringResource(R.string.onb_folder_step1),
                        stringResource(R.string.onb_folder_step2),
                        stringResource(R.string.onb_folder_step3),
                    )
                } else {
                    BlockFact(
                        stringResource(R.string.settings_library_fact_folder),
                        folderLabel(romFolder)
                    )
                    BlockNotice(stringResource(R.string.onb_folder_after))
                }
                DetailActions {
                    if (romFolder == null) {
                        PrimaryButton(
                            label = stringResource(R.string.lib_choose_folder),
                            onClick = onPickFolder,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        GhostButton(
                            label = stringResource(R.string.onb_folder_change),
                            onClick = onPickFolder,
                            fillWidth = true
                        )
                    }
                }
            }
        }
    )

    OnbStep.CONSOLES -> StepLayout(
        wide = wide,
        mark = { StepMark(size = 52.dp) { GridMark(size = 28.dp, color = it) } },
        title = stringResource(R.string.consoles_pick_title),
        body = stringResource(R.string.onb_consoles_body),
        fullWidthWork = true,
        work = {
            WorkCard {
                ConsoleGrid(
                    hidden = hiddenConsoles,
                    onSetVisible = onSetConsoleVisible,
                    compact = wide,
                )
                Text(
                    stringResource(R.string.onb_consoles_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )

    OnbStep.COCOON -> {
        val has = cocoonFolder.isNotBlank()
        StepLayout(
            wide = wide,
            mark = { StepMark { PaintMark(size = 34.dp, color = it) } },
            title = stringResource(R.string.onb_cocoon_title),
            body = stringResource(R.string.onb_cocoon_body),
            state = {
                StatePill(
                    if (has) DetailTone.GOOD else DetailTone.WARN,
                    stringResource(
                        if (has) R.string.onb_cocoon_pill_on else R.string.onb_cocoon_pill_off
                    )
                )
            },
            work = {
                WorkCard {
                    if (has) {
                        BlockFact(
                            stringResource(R.string.settings_library_fact_folder),
                            folderLabel(Uri.parse(cocoonFolder))
                        )
                        BlockNotice(stringResource(R.string.onb_cocoon_after))
                    } else {
                        SettingsSteps(
                            stringResource(R.string.onb_cocoon_step1),
                            stringResource(R.string.onb_cocoon_step2),
                            stringResource(R.string.onb_cocoon_step3),
                        )
                    }
                    DetailActions {
                        if (has) {
                            GhostButton(
                                label = stringResource(R.string.settings_cocoon_change),
                                onClick = onPickCocoon,
                                fillWidth = true
                            )
                            GhostButton(
                                label = stringResource(R.string.settings_cocoon_forget),
                                onClick = onForgetCocoon,
                                fillWidth = true
                            )
                        } else {
                            PrimaryButton(
                                label = stringResource(R.string.settings_cocoon_choose),
                                onClick = onPickCocoon,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        )
    }

    OnbStep.ARTWORK -> StepLayout(
        wide = wide,
        mark = { StepMark { PaintMark(size = 34.dp, color = it) } },
        title = stringResource(R.string.onb_artwork_title),
        body = stringResource(R.string.onb_artwork_body),
        work = {
            WorkCard {
                SteamGridDbMark()
                SettingsSteps(
                    stringResource(R.string.onb_artwork_step1),
                    stringResource(R.string.onb_artwork_step2),
                    stringResource(R.string.onb_artwork_step3),
                )
                PadTextField(
                    value = artworkKey,
                    onValueChange = onArtworkKeyChange,
                    shape = PillShape,
                    label = stringResource(R.string.settings_artwork_field),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )

    // Les trois rituels d'emulateur : le bloc des reglages, pose tel quel.
    OnbStep.PPSSPP -> StepLayout(
        wide = wide,
        mark = { StepMark { ChipMark(size = 34.dp, color = it) } },
        title = stringResource(R.string.onb_ppsspp_title),
        body = stringResource(R.string.onb_ppsspp_body),
        work = {
            PpssppBlock(
                store = ppssppConfig,
                ready = ppssppReady,
                onReadyChanged = onPpssppReady,
            )
        }
    )

    OnbStep.PS2 -> StepLayout(
        wide = wide,
        mark = { StepMark { ChipMark(size = 34.dp, color = it) } },
        title = stringResource(R.string.onb_ps2_title),
        body = stringResource(R.string.onb_ps2_body),
        work = {
            Ps2Block(
                ready = ps2Ready,
                profileName = profileName,
                onReadyChanged = onPs2Ready,
            )
        }
    )

    OnbStep.AUTOFILL -> StepLayout(
        wide = wide,
        mark = { StepMark { ChipMark(size = 34.dp, color = it) } },
        title = stringResource(R.string.onb_fill_title),
        body = stringResource(R.string.onb_fill_body),
        work = { AutofillBlock(enabled = autofillOn, onOpen = onOpenAutofill) }
    )

    OnbStep.NOTIF -> StepLayout(
        wide = wide,
        mark = { StepMark { SignalMark(size = 34.dp, color = it) } },
        title = stringResource(R.string.onb_notif_title),
        body = stringResource(R.string.onb_notif_body),
        state = {
            StatePill(
                if (notificationsGranted) DetailTone.GOOD else DetailTone.WARN,
                stringResource(
                    if (notificationsGranted) R.string.onb_notif_pill_on
                    else R.string.onb_notif_pill_off
                )
            )
        },
        work = {
            WorkCard {
                if (notificationsGranted) {
                    BlockNotice(stringResource(R.string.onb_notif_after))
                } else {
                    SettingsSteps(
                        stringResource(R.string.onb_notif_step1),
                        stringResource(R.string.onb_notif_step2),
                    )
                    if (notificationsRefused) {
                        BlockNotice(stringResource(R.string.onb_notif_refused))
                    }
                }
                if (!notificationsGranted && !notificationsRefused) {
                    DetailActions {
                        PrimaryButton(
                            label = stringResource(R.string.onb_notif_enable),
                            onClick = onAskNotifications,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    )

    OnbStep.DONE -> StepLayout(
        wide = wide,
        mark = { LogoMark(size = 76.dp) },
        title = stringResource(R.string.onb_done_title),
        body = stringResource(R.string.onb_done_body),
        work = {
            WorkCard {
                Recap(
                    stringResource(R.string.onb_recap_folder) to (romFolder != null),
                    stringResource(R.string.onb_recap_artwork) to
                        (cocoonFolder.isNotBlank() || artworkKey.isNotBlank()),
                    stringResource(R.string.onb_recap_ppsspp) to ppssppReady,
                    stringResource(R.string.onb_recap_ps2) to ps2Ready,
                    stringResource(R.string.onb_recap_autofill) to autofillOn,
                    stringResource(R.string.onb_recap_notif) to notificationsGranted,
                    hidden = hiddenConsoles,
                )
                BlockNotice(stringResource(R.string.onb_done_where))
            }
        }
    )
}

/**
 * Le releve final. Les lignes qui ne concernent pas ce joueur ne paraissent pas.
 * pourquoi : docs/decisions/onboarding.md § Le récapitulatif nomme ce qui a été sauté
 */
@Composable
private fun Recap(vararg rows: Pair<String, Boolean>, hidden: Set<Console>) {
    val shown = rows.filterIndexed { i, _ ->
        when (i) {
            2 -> Console.PSP !in hidden
            3 -> Console.PS2 !in hidden
            4 -> AUTOMATED.any { it !in hidden }
            else -> true
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        shown.forEach { (label, done) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                StatePill(
                    if (done) DetailTone.GOOD else DetailTone.WARN,
                    stringResource(
                        if (done) R.string.settings_pill_ready else R.string.onb_recap_later
                    )
                )
            }
        }
    }
}

/** Le dernier segment d'un arbre de documents, ce que le joueur reconnait. */
private fun folderLabel(uri: Uri): String {
    val raw = uri.lastPathSegment ?: return uri.toString()
    return raw.substringAfterLast(':').substringAfterLast('/').ifBlank { raw }
}

/**
 * Ou l'on en est, et de quoi il s'agit : les points seuls disaient une longueur
 * qui changeait sous les yeux du joueur.
 * pourquoi : docs/decisions/onboarding.md § Où l'on en est, et de quoi il s'agit
 */
@Composable
private fun StepRail(current: Int, total: Int, label: String) {
    val dark = LocalEmufiiDarkTheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(total) { i ->
                val active = i == current
                Box(
                    Modifier
                        .height(7.dp)
                        .width(if (active) 20.dp else 7.dp)
                        .clip(if (active) PillShape else CircleShape)
                        .background(
                            // L'onboarding parle jeu et systeme : les points
                            // portent l'axe turquoise, deep pour tenir sur le creme.
                            if (active) (if (dark) Teal.darkBright else Teal.deep)
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
                        )
                )
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
