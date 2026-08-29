package eu.emufii.app.ui.screens.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.emufii.app.BuildConfig
import eu.emufii.app.R
import eu.emufii.app.azahar.AzaharLauncher
import eu.emufii.app.library.Console
import eu.emufii.app.library.HiddenRoms
import eu.emufii.app.library.Rom
import eu.emufii.app.library.RomsRepository
import eu.emufii.app.profile.FriendStore
import eu.emufii.app.profile.Profile
import eu.emufii.app.profile.ProfileStore
import eu.emufii.app.profile.playerDisplayName
import eu.emufii.app.ps2.Ps2NetworkProfile
import eu.emufii.app.psp.PpssppConfigStore
import eu.emufii.app.secondscreen.PanelMark
import eu.emufii.app.secondscreen.SecondScreen
import eu.emufii.app.secondscreen.SecondScreenModel
import eu.emufii.app.settings.SettingsStore
import eu.emufii.app.ui.components.Avatar
import eu.emufii.app.ui.components.ChipMark
import eu.emufii.app.ui.components.DetailTone
import eu.emufii.app.ui.components.GhostButton
import eu.emufii.app.ui.components.GridMark
import eu.emufii.app.ui.components.InfoMark
import eu.emufii.app.ui.components.PersonMark
import eu.emufii.app.ui.components.PadDialog
import eu.emufii.app.ui.components.PadDialogText
import eu.emufii.app.ui.components.PaintMark
import eu.emufii.app.ui.components.ShelfMark
import eu.emufii.app.ui.components.SectionHeader
import eu.emufii.app.ui.components.SlidersMark
import eu.emufii.app.ui.components.labelRes
import eu.emufii.app.wg.WgKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Les reglages : un hub qui ne contient que des entrees, et sept pages. Le hub
 * ne se lit pas, il se traverse.
 * pourquoi : docs/decisions/reglages-ecran.md § Un hub et sept pages, plus un accordéon
 */
@Composable
fun SettingsScreen(
    profile: Profile,
    profileStore: ProfileStore,
    friendStore: FriendStore,
    settingsStore: SettingsStore,
    romsRepo: RomsRepository,
    libraryFolder: String?,
    /** Le second dossier de ROMs, optionnel : il s'ajoute au premier. */
    librarySecondFolder: String?,
    libraryScanning: Boolean,
    libraryCount: Int?,
    onFolderPicked: (Uri) -> Unit,
    onSecondFolderPicked: (Uri) -> Unit,
    onSecondFolderRemoved: () -> Unit,
    onRescan: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    /** La page ouverte. Le hub est la racine de cet ecran, pas une page de plus. */
    var page by remember { mutableStateOf(SettingsPageId.HUB) }

    var name by remember(profile.id) {
        mutableStateOf(profile.name.takeIf { profile.isNamed } ?: "")
    }
    var photoError by remember { mutableStateOf<String?>(null) }
    var confirmingReset by remember { mutableStateOf(false) }

    val language by settingsStore.language.collectAsState()
    val theme by settingsStore.theme.collectAsState()
    val artworkKey by settingsStore.steamGridDbKey.collectAsState()
    val hiddenConsoles by settingsStore.hiddenConsoles.collectAsState()

    val ppssppConfig = remember(context) { PpssppConfigStore(context) }
    var ppssppConfigReady by remember { mutableStateOf(ppssppConfig.isReady()) }

    // Le joueur l'a-t-il importee dans ARMSX2 ? Rien ici ne peut le verifier.
    // Prise a la reponse pas chere, confirmee hors du fil principal juste
    // apres : ouvrir les reglages ne doit pas attendre 175 ms de lecture de
    // carte.
    var ps2ProfileReady by remember { mutableStateOf(Ps2NetworkProfile.isReadyQuick(context)) }
    LaunchedEffect(Unit) { ps2ProfileReady = Ps2NetworkProfile.verifyReady(context) }

    var hiddenCount by remember { mutableStateOf(HiddenRoms(context).count()) }

    // Quelques jaquettes reelles pour le bloc des icones de jeu. Prises dans le
    // cache que l'app a deja chauffe au demarrage, hors du fil principal, et
    // seulement celles qui portent une image : une bande de plaques vides ne
    // montrerait rien.
    // pourquoi : docs/decisions/reglages-ecran.md § Les images des pages viennent de l'appareil, pas d'une banque
    var artworkSample by remember { mutableStateOf<List<Rom>>(emptyList()) }
    LaunchedEffect(libraryCount) {
        artworkSample = withContext(Dispatchers.IO) {
            runCatching { romsRepo.cachedOrScan() }.getOrDefault(emptyList())
                .filter { it.iconFile != null }
                .take(ARTWORK_SAMPLE)
        }
    }

    // Relu tant que l'ecran est la, et pas une fois : la reponse n'existe
    // qu'au retour des reglages d'Android.
    // pourquoi : docs/decisions/reglages-ecran.md § Les lignes d'état, et ce que personne ne devinerait
    val autofillLauncher = remember { AzaharLauncher(context) }
    var autofillOn by remember { mutableStateOf(autofillLauncher.isNetplayAutomationEnabled()) }
    LaunchedEffect(Unit) {
        while (true) {
            autofillOn = autofillLauncher.isNetplayAutomationEnabled()
            delay(700)
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) photoError = profileStore.setAvatar(uri).exceptionOrNull()?.message
    }
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? -> if (uri != null) onFolderPicked(uri) }
    val secondFolderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? -> if (uri != null) onSecondFolderPicked(uri) }

    /** Le pseudo est ecrit au moment de quitter les reglages, pas a chaque touche. */
    val leave = {
        profileStore.setName(name)
        onBack()
    }

    // Une page est un sous-niveau : B revient au hub avant de quitter l'ecran.
    BackHandler(enabled = page != SettingsPageId.HUB) { page = SettingsPageId.HUB }

    /**
     * Le panneau garde la categorie ou l'on est : le hub publie la case visee et
     * se vide en partant. Republie a chaque changement d'etat, et rien n'est
     * publie sur le hub — deux publieurs pour une face.
     * pourquoi : docs/decisions/reglages-ecran.md § Le hub est une grille, et le panneau montre la case visée
     */
    val face = settingsFace(
        page = page,
        displayName = playerDisplayName(name.ifBlank { Profile.DEFAULT_NAME }),
        libraryFolder = libraryFolder,
        libraryCount = libraryCount,
        libraryScanning = libraryScanning,
        hiddenConsoleCount = hiddenConsoles.size,
        emulatorsReady = listOf(ppssppConfigReady, ps2ProfileReady, autofillOn).count { it },
        themeLabel = stringResource(theme.labelRes),
        languageLabel = stringResource(language.labelRes),
    )
    LaunchedEffect(face) { face?.let { SecondScreen.publish(it) } }
    // Filet : quitter l'ecran depuis une page ne doit pas laisser une face de
    // reglages allumee derriere. Le hub a le sien, mais il n'est pas la quand
    // une page est ouverte.
    DisposableEffect(Unit) { onDispose { SecondScreen.clear() } }

    val toHub = { page = SettingsPageId.HUB }

    when (page) {
        SettingsPageId.HUB -> SettingsHub(
            profile = profile,
            name = name,
            libraryFolder = libraryFolder,
            libraryCount = libraryCount,
            libraryScanning = libraryScanning,
            hiddenConsoleCount = hiddenConsoles.size,
            emulatorsReady = listOf(ppssppConfigReady, ps2ProfileReady, autofillOn).count { it },
            // L'accent configurable est parti : la ligne ne nomme plus que le
            // theme. pourquoi : theme-duotone-shelves.md § Réglages
            themeLabel = stringResource(theme.labelRes),
            languageLabel = stringResource(language.labelRes),
            onOpen = { page = it },
            onBack = leave,
            modifier = modifier
        )

        SettingsPageId.PROFILE -> ProfilePage(
            profile = profile,
            name = name,
            onNameChange = { name = it },
            photoError = photoError,
            onPickPhoto = {
                picker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onClearPhoto = { profileStore.clearAvatar() },
            onReset = { confirmingReset = true },
            onBack = toHub,
            modifier = modifier
        )

        SettingsPageId.LIBRARY -> LibraryPage(
            folder = libraryFolder,
            secondFolder = librarySecondFolder,
            scanning = libraryScanning,
            count = libraryCount,
            onPickFolder = { folderPicker.launch(null) },
            onPickSecondFolder = { secondFolderPicker.launch(null) },
            onRemoveSecondFolder = onSecondFolderRemoved,
            onRescan = onRescan,
            artworkKey = artworkKey,
            onArtworkKeyChange = { settingsStore.setSteamGridDbKey(it) },
            artworkSample = artworkSample,
            hiddenCount = hiddenCount,
            onRestoreHidden = {
                HiddenRoms(context).clear()
                hiddenCount = 0
            },
            onBack = toHub,
            modifier = modifier
        )

        SettingsPageId.CONSOLES -> ConsolesPage(
            hidden = hiddenConsoles,
            onSetVisible = { console, visible -> settingsStore.setConsoleVisible(console, visible) },
            onBack = toHub,
            modifier = modifier
        )

        SettingsPageId.EMULATORS -> EmulatorsPage(
            ppssppConfig = ppssppConfig,
            ppssppReady = ppssppConfigReady,
            onPpssppReadyChanged = { ppssppConfigReady = it },
            ps2Ready = ps2ProfileReady,
            profileName = name.ifBlank { Profile.DEFAULT_NAME },
            onPs2ReadyChanged = { ps2ProfileReady = it },
            autofillOn = autofillOn,
            onOpenAutofill = { autofillLauncher.openAccessibilitySettings() },
            onBack = toHub,
            modifier = modifier
        )

        SettingsPageId.APPEARANCE -> AppearancePage(
            theme = theme,
            onTheme = settingsStore::setTheme,
            onBack = toHub,
            modifier = modifier
        )

        SettingsPageId.GENERAL -> GeneralPage(
            settingsStore = settingsStore,
            language = language,
            onBack = toHub,
            modifier = modifier
        )

        SettingsPageId.ABOUT -> AboutPage(onBack = toHub, modifier = modifier)
    }

    if (confirmingReset) {
        val done = stringResource(R.string.profile_reset_done)
        PadDialog(
            title = stringResource(R.string.profile_reset),
            onDismiss = { confirmingReset = false },
            actions = {
                GhostButton(
                    label = stringResource(R.string.friends_cancel),
                    onClick = { confirmingReset = false }
                )
                GhostButton(
                    label = stringResource(R.string.profile_reset),
                    onClick = {
                        // Les deux, toujours : la liste d'amis est indexee sur
                        // une identite qui n'existe plus, et la laisser
                        // afficherait des rangees qui ne reviendront jamais en
                        // ligne.
                        friendStore.clear()
                        profileStore.reset()
                        // La cle publique WireGuard est un identifiant stable
                        // que le coordinator voit ; la laisser survivrait au
                        // profil auquel elle allait.
                        WgKeys.reset(context)
                        name = ""
                        confirmingReset = false
                        Toast.makeText(context, done, Toast.LENGTH_SHORT).show()
                    },
                    tint = dangerInk()
                )
            }
        ) {
            PadDialogText(stringResource(R.string.profile_reset_confirm))
        }
    }
}

/** Combien de jaquettes la bande du bloc des icones montre. */
private const val ARTWORK_SAMPLE = 5

/** Les sept pages, et le hub qui y mene. */
/**
 * La face que le panneau montre pour une categorie : **une seule source pour les
 * deux moments**, la tuile et la page, sinon elles racontent deux choses.
 * `@Composable` parce que tout y est traduit et date.
 * pourquoi : docs/decisions/reglages-ecran.md § Le hub est une grille, et le panneau montre la case visée
 */
@Composable
private fun settingsFace(
    page: SettingsPageId,
    displayName: String,
    libraryFolder: String?,
    libraryCount: Int?,
    libraryScanning: Boolean,
    hiddenConsoleCount: Int,
    emulatorsReady: Int,
    themeLabel: String,
    languageLabel: String,
): SecondScreenModel.SettingsEntry? {
    val root = stringResource(R.string.settings_title)
    fun face(title: String, summary: String, mark: PanelMark, social: Boolean = false) =
        SecondScreenModel.SettingsEntry(
            title = title,
            summary = summary,
            root = root,
            mark = mark,
            social = social
        )
    return when (page) {
        // Le hub n'a pas de face a lui : c'est la case visee qui parle.
        SettingsPageId.HUB -> null
        SettingsPageId.PROFILE -> face(
            stringResource(R.string.settings_page_profile),
            displayName,
            PanelMark.PROFILE,
            social = true
        )
        SettingsPageId.LIBRARY -> face(
            stringResource(R.string.settings_page_library),
            stringResource(R.string.settings_sub_library),
            PanelMark.LIBRARY
        )
        SettingsPageId.CONSOLES -> face(
            stringResource(R.string.settings_page_consoles),
            stringResource(
                R.string.settings_pill_consoles,
                Console.entries.size - hiddenConsoleCount,
                Console.entries.size
            ),
            PanelMark.CONSOLES
        )
        SettingsPageId.EMULATORS -> face(
            stringResource(R.string.settings_page_emulators),
            stringResource(R.string.settings_sub_emulators),
            PanelMark.EMULATORS
        )
        SettingsPageId.APPEARANCE -> face(
            stringResource(R.string.settings_page_appearance),
            themeLabel,
            PanelMark.APPEARANCE
        )
        SettingsPageId.GENERAL -> face(
            stringResource(R.string.settings_page_general),
            languageLabel + " · " + stringResource(R.string.settings_sub_general),
            PanelMark.GENERAL
        )
        SettingsPageId.ABOUT -> face(
            stringResource(R.string.settings_page_about),
            BuildConfig.VERSION_NAME,
            PanelMark.ABOUT
        )
    }
}

internal enum class SettingsPageId {
    HUB, PROFILE, LIBRARY, CONSOLES, EMULATORS, APPEARANCE, GENERAL, ABOUT
}

/**
 * Le hub : quatre groupes d'entrees, et rien d'autre. **Aucun reglage ne se
 * change ici**, et c'est la seule regle de cette page.
 * pourquoi : docs/decisions/reglages-ecran.md § Un hub et sept pages, plus un accordéon
 * pourquoi : docs/decisions/reglages-ecran.md § Une entrée du hub est une plaque, pas une rangée
 */
@Composable
private fun SettingsHub(
    profile: Profile,
    name: String,
    libraryFolder: String?,
    libraryCount: Int?,
    libraryScanning: Boolean,
    hiddenConsoleCount: Int,
    emulatorsReady: Int,
    themeLabel: String,
    languageLabel: String,
    onOpen: (SettingsPageId) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val root = stringResource(R.string.settings_title)

    // Le panneau arriere montre la case visee. Il ne se vide plus en partant :
    // la page qu'on ouvre republie la face de sa categorie, et un `clear` pose
    // ici l'effacerait juste apres. C'est l'ecran des reglages entier, un cran
    // au-dessus, qui eteint le panneau en le quittant.
    // pourquoi : docs/decisions/reglages-ecran.md § Le hub est une grille, et le panneau montre la case visée

    SettingsPage(
        title = root,
        onBack = onBack,
        modifier = modifier
    ) {
        val displayName = playerDisplayName(name.ifBlank { Profile.DEFAULT_NAME })

        // La tuile publie exactement ce que la page republiera en s'ouvrant :
        // passer le curseur sur « Bibliotheque » puis y entrer ne doit rien
        // changer au panneau, sinon l'entree se lit comme un changement d'ecran.
        @Composable
        fun faceOf(page: SettingsPageId) = settingsFace(
            page = page,
            displayName = displayName,
            libraryFolder = libraryFolder,
            libraryCount = libraryCount,
            libraryScanning = libraryScanning,
            hiddenConsoleCount = hiddenConsoleCount,
            emulatorsReady = emulatorsReady,
            themeLabel = themeLabel,
            languageLabel = languageLabel,
        )!!

        // Les intitules de famille sont partis avec la colonne : une grille n'a
        // pas de rayons, elle a des cases, et sept cases se cherchent au nom.
        // pourquoi : docs/decisions/reglages-ecran.md § Le hub est une grille, et le panneau montre la case visée
        val entries = listOf<@Composable (Boolean, Modifier) -> Unit>(
            { first, mod ->
                val face = faceOf(SettingsPageId.PROFILE)
                val label = face.title
                val summary = face.summary
                SettingsEntry(
                    label = label,
                    summary = summary,
                    onOpen = { onOpen(SettingsPageId.PROFILE) },
                    entry = first,
                    modifier = mod,
                    // Le profil est le seul domaine social du hub.
                    domain = EntryDomain.SOCIAL,
                    // L'avatar tient lieu de marque : c'est la seule entree dont
                    // l'etat est une image, et la seule couleur du hub — elle
                    // vient du contenu, jamais du chrome.
                    leading = {
                        Avatar(name = displayName, imageFile = profile.avatarFile, size = 34.dp)
                    },
                    onFocused = { if (it) SecondScreen.publish(face) }
                )
            },
            { first, mod ->
                val face = faceOf(SettingsPageId.LIBRARY)
                val label = face.title
                val summary = face.summary
                SettingsEntry(
                    label = label,
                    summary = summary,
                    onOpen = { onOpen(SettingsPageId.LIBRARY) },
                    entry = first,
                    modifier = mod,
                    icon = { ShelfMark(color = it) },
                    state = when {
                        libraryScanning -> EntryState(
                            DetailTone.BUSY,
                            stringResource(R.string.settings_pill_scanning)
                        )
                        libraryFolder == null -> EntryState(
                            DetailTone.WARN,
                            stringResource(R.string.settings_pill_no_folder)
                        )
                        else -> EntryState(
                            DetailTone.GOOD,
                            libraryCount?.let {
                                pluralStringResource(R.plurals.settings_pill_games, it, it)
                            } ?: stringResource(R.string.settings_pill_ready)
                        )
                    },
                    onFocused = { if (it) SecondScreen.publish(face) }
                )
            },
            { first, mod ->
                // Pas de pastille : masquer une console est un gout, pas un
                // etat a rattraper, et une pastille verte y dirait « rien a
                // faire » sur une page ou il n'y a jamais rien a faire. Le
                // compte tient dans le resume, la ou il se lit comme un fait et
                // non comme un verdict.
                val face = faceOf(SettingsPageId.CONSOLES)
                val label = face.title
                val summary = face.summary
                SettingsEntry(
                    label = label,
                    summary = summary,
                    onOpen = { onOpen(SettingsPageId.CONSOLES) },
                    entry = first,
                    modifier = mod,
                    icon = { GridMark(color = it) },
                    onFocused = { if (it) SecondScreen.publish(face) }
                )
            },
            { first, mod ->
                val face = faceOf(SettingsPageId.EMULATORS)
                val label = face.title
                val summary = face.summary
                SettingsEntry(
                    label = label,
                    summary = summary,
                    onOpen = { onOpen(SettingsPageId.EMULATORS) },
                    entry = first,
                    modifier = mod,
                    icon = { ChipMark(color = it) },
                    state = EntryState(
                        // Verte seulement quand les trois preparations sont
                        // faites : cette page existe pour ce qui reste a
                        // preparer, et « 2 / 3 » en vert se lirait comme
                        // « rien a faire ».
                        if (emulatorsReady == EMULATOR_STEPS) DetailTone.GOOD else DetailTone.WARN,
                        stringResource(R.string.settings_pill_ratio, emulatorsReady, EMULATOR_STEPS)
                    ),
                    onFocused = { if (it) SecondScreen.publish(face) }
                )
            },
            { first, mod ->
                val face = faceOf(SettingsPageId.APPEARANCE)
                SettingsEntry(
                    label = face.title,
                    summary = face.summary,
                    onOpen = { onOpen(SettingsPageId.APPEARANCE) },
                    entry = first,
                    modifier = mod,
                    icon = { PaintMark(color = it) },
                    onFocused = { if (it) SecondScreen.publish(face) }
                )
            },
            { first, mod ->
                val face = faceOf(SettingsPageId.GENERAL)
                val label = face.title
                val summary = face.summary
                SettingsEntry(
                    label = label,
                    summary = summary,
                    onOpen = { onOpen(SettingsPageId.GENERAL) },
                    entry = first,
                    modifier = mod,
                    icon = { SlidersMark(color = it) },
                    onFocused = { if (it) SecondScreen.publish(face) }
                )
            },
            { first, mod ->
                val face = faceOf(SettingsPageId.ABOUT)
                SettingsEntry(
                    label = face.title,
                    summary = face.summary,
                    onOpen = { onOpen(SettingsPageId.ABOUT) },
                    entry = first,
                    modifier = mod,
                    icon = { InfoMark(color = it) },
                    onFocused = { if (it) SecondScreen.publish(face) }
                )
            }
        )

        HubGrid(entries)
    }
}

/**
 * Les cases du hub : deux colonnes a parts egales, et ca descend.
 *
 * Rien de paresseux ici, et c'est le point : les sept cases sont composees, donc
 * la traversee de focus trouve toujours sa destination. Sept cases ne valent pas
 * la machinerie de la bibliotheque.
 * pourquoi : docs/decisions/reglages-ecran.md § Deux colonnes, et ça descend — jamais de côté
 * pourquoi : docs/decisions/reglages-ecran.md § Le hub est une grille, et le panneau montre la case visée
 */
@Composable
private fun HubGrid(entries: List<@Composable (Boolean, Modifier) -> Unit>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(HUB_GAP),
        modifier = Modifier.fillMaxWidth()
    ) {
        entries.chunked(HUB_COLUMNS).forEachIndexed { row, chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(HUB_GAP)) {
                chunk.forEachIndexed { column, entry ->
                    entry(
                        row == 0 && column == 0,
                        Modifier.weight(1f).height(HUB_TILE_HEIGHT)
                    )
                }
                // Le rang incomplet garde les places manquantes : sans cela la
                // derniere case s'etire sur la largeur de deux et se lit comme
                // plus importante que ses voisines.
                repeat(HUB_COLUMNS - chunk.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** Combien de cases de front. Deux, a parts egales, de chaque cote de la page. */
private const val HUB_COLUMNS = 2

/** L'ecart entre deux cases, dans les deux sens. */
private val HUB_GAP = 12.dp

/**
 * La hauteur d'une case, la meme pour toutes : un resume sur deux lignes
 * grandirait sa case seule et casserait l'alignement du rang.
 * pourquoi : docs/decisions/reglages-ecran.md § Deux colonnes, et ça descend — jamais de côté
 */
private val HUB_TILE_HEIGHT = 92.dp

/** Combien de preparations la page des emulateurs compte : PPSSPP, PS2, remplissage. */
private const val EMULATOR_STEPS = 3
