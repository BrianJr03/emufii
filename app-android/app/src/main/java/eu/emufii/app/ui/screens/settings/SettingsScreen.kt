package eu.emufii.app.ui.screens.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.emufii.app.BuildConfig
import eu.emufii.app.R
import eu.emufii.app.azahar.AzaharLauncher
import eu.emufii.app.library.Console
import eu.emufii.app.library.ConsoleKeysStore
import eu.emufii.app.library.HiddenRoms
import eu.emufii.app.library.Rom
import eu.emufii.app.library.RomsRepository
import eu.emufii.app.profile.FriendStore
import eu.emufii.app.profile.Profile
import eu.emufii.app.profile.ProfileStore
import eu.emufii.app.profile.playerDisplayName
import eu.emufii.app.ps2.Ps2NetworkProfile
import eu.emufii.app.psp.PpssppConfigStore
import eu.emufii.app.settings.SettingsStore
import eu.emufii.app.ui.components.Avatar
import eu.emufii.app.ui.components.ChipMark
import eu.emufii.app.ui.components.DetailTone
import eu.emufii.app.ui.components.GhostButton
import eu.emufii.app.ui.components.GridMark
import eu.emufii.app.ui.components.InfoMark
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
 * Les reglages : un hub qui ne contient que des entrees, et sept pages.
 *
 * Ce qui etait un ecran unique de quatorze rangees depliantes — une seule
 * ouverte a la fois, le reste de la page poussee vers le bas a chaque geste —
 * est devenu un menu et des pages entieres. Le hub ne se lit pas, il se
 * traverse : chaque entree dit ou elle mene et dans quel etat s'y trouve ce
 * qu'elle contient.
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
    libraryScanning: Boolean,
    libraryCount: Int?,
    onFolderPicked: (Uri) -> Unit,
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
    val accent by settingsStore.accent.collectAsState()
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

    // Les cles de console. Un dump Switch ne dit rien de lui-meme sans elles.
    val keysStore = remember { ConsoleKeysStore(context) }
    var hasKeys by remember { mutableStateOf(keysStore.hasKeys) }
    var keysRejected by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) photoError = profileStore.setAvatar(uri).exceptionOrNull()?.message
    }
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? -> if (uri != null) onFolderPicked(uri) }
    val keysPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val ok = keysStore.import(uri)
            hasKeys = keysStore.hasKeys
            keysRejected = !ok
            // Les cles changent ce qu'un scan sait lire : la bibliotheque vaut
            // d'etre reparcourue, sinon les tuiles restent muettes et le
            // reglage a l'air inerte.
            if (ok) onRescan()
        }
    }

    /** Le pseudo est ecrit au moment de quitter les reglages, pas a chaque touche. */
    val leave = {
        profileStore.setName(name)
        onBack()
    }

    // Une page est un sous-niveau : B revient au hub avant de quitter l'ecran.
    BackHandler(enabled = page != SettingsPageId.HUB) { page = SettingsPageId.HUB }

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
            themeLabel = stringResource(theme.labelRes) + " · " + stringResource(accent.labelRes),
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
            scanning = libraryScanning,
            count = libraryCount,
            onPickFolder = { folderPicker.launch(null) },
            onRescan = onRescan,
            artworkKey = artworkKey,
            onArtworkKeyChange = { settingsStore.setSteamGridDbKey(it) },
            hasKeys = hasKeys,
            keysRejected = keysRejected,
            onPickKeys = {
                keysRejected = false
                // Un fichier de cles n'a pas de type declare ; tout le reste le
                // cacherait.
                keysPicker.launch(arrayOf("*/*"))
            },
            onForgetKeys = {
                keysStore.clear()
                hasKeys = false
                keysRejected = false
            },
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
            accent = accent,
            onTheme = settingsStore::setTheme,
            onAccent = settingsStore::setAccent,
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
                    tint = DANGER
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
internal enum class SettingsPageId {
    HUB, PROFILE, LIBRARY, CONSOLES, EMULATORS, APPEARANCE, GENERAL, ABOUT
}

/**
 * Le hub : quatre groupes d'entrees, et rien d'autre.
 *
 * Aucun reglage ne se change ici, et c'est la seule regle de cette page. C'est
 * ce qui lui permet de tenir sur l'ecran de la Thor : on y voit d'un coup tout
 * ce que l'app sait regler, et l'etat de ce qui demande a etre prepare.
 *
 * Des rangees, pas des tuiles : la grille de tuiles est la grammaire de la
 * bibliotheque, ou le contenu est la jaquette. Ici il n'y a rien a montrer,
 * seulement des noms a lire, et un nom se lit dans une rangee.
 * pourquoi : docs/decisions/reglages-ecran.md § Un hub et sept pages, plus un accordéon
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
    SettingsPage(
        title = stringResource(R.string.settings_title),
        onBack = onBack,
        modifier = modifier
    ) {
        val displayName = playerDisplayName(name.ifBlank { Profile.DEFAULT_NAME })

        // Quatre familles, chacune coiffee de son nom.
        //
        // Ils avaient ete retires : le deuxieme repetait mot pour mot le nom de
        // l'entree qu'il coiffait — « Bibliotheque » au-dessus de
        // « Bibliotheque » — et les quatre ensemble prenaient assez de hauteur
        // pour qu'il ne reste que deux entrees et demie a l'ecran.
        //
        // Ce n'etait pas le principe qui clochait, c'etait le mot. Un intitule
        // doit nommer **la famille**, pas son premier membre : « Bibliotheque
        // et consoles » coiffe ses deux entrees sans repeter ni l'une ni
        // l'autre, et le hub retrouve ce qu'un menu de reglages doit avoir —
        // des rayons, pas une liste.
        // pourquoi : docs/decisions/reglages-ecran.md § Le hub tient sur un écran, sans intitulés de groupe
        //
        // Il se borne lui-meme : une rangee etiree sur toute la largeur met son
        // nom et sa pastille aux deux bords opposes, et la paire cesse de se
        // lire comme une seule chose.
        // pourquoi : docs/decisions/reglages-ecran.md § Les trois constantes de forme d'une rangée
        Column(
            modifier = Modifier.widthIn(max = SETTINGS_MAX_WIDTH),
            verticalArrangement = Arrangement.spacedBy(HUB_FAMILY_GAP)
        ) {
            HubGroup(stringResource(R.string.settings_sec_you)) {
            SettingsEntry(
                label = stringResource(R.string.settings_page_profile),
                summary = displayName,
                onOpen = { onOpen(SettingsPageId.PROFILE) },
                // Premier controle de la page : la manette y descend depuis
                // l'en-tete, et y remonte.
                entry = true,
                // L'avatar tient lieu de marque : c'est la seule entree dont
                // l'etat est une image, et la seule couleur du hub — elle vient
                // du contenu, jamais du chrome.
                leading = {
                    Avatar(name = displayName, imageFile = profile.avatarFile, size = 34.dp)
                }
            )
            }

            HubGroup(stringResource(R.string.settings_sec_library)) {
            SettingsEntry(
                label = stringResource(R.string.settings_page_library),
                summary = stringResource(R.string.settings_sub_library),
                onOpen = { onOpen(SettingsPageId.LIBRARY) },
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
                }
            )
            // Pas de pastille : masquer une console est un gout, pas un etat a
            // rattraper, et une pastille verte y dirait « rien a faire » sur une
            // page ou il n'y a jamais rien a faire. Le compte tient dans le
            // resume, la ou il se lit comme un fait et non comme un verdict.
            SettingsEntry(
                label = stringResource(R.string.settings_page_consoles),
                summary = stringResource(
                    R.string.settings_pill_consoles,
                    Console.entries.size - hiddenConsoleCount,
                    Console.entries.size
                ),
                onOpen = { onOpen(SettingsPageId.CONSOLES) },
                icon = { GridMark(color = it) }
            )
            }

            HubGroup(stringResource(R.string.settings_sec_emulators)) {
            SettingsEntry(
                label = stringResource(R.string.settings_page_emulators),
                summary = stringResource(R.string.settings_sub_emulators),
                onOpen = { onOpen(SettingsPageId.EMULATORS) },
                icon = { ChipMark(color = it) },
                state = EntryState(
                    // Verte seulement quand les trois preparations sont faites :
                    // cette page existe pour ce qui reste a preparer, et
                    // « 2 / 3 » en vert se lirait comme « rien a faire ».
                    if (emulatorsReady == EMULATOR_STEPS) DetailTone.GOOD else DetailTone.WARN,
                    stringResource(R.string.settings_pill_ratio, emulatorsReady, EMULATOR_STEPS)
                )
            )
            }

            HubGroup(stringResource(R.string.settings_sec_app)) {
            SettingsEntry(
                label = stringResource(R.string.settings_page_appearance),
                summary = themeLabel,
                onOpen = { onOpen(SettingsPageId.APPEARANCE) },
                icon = { PaintMark(color = it) }
            )
            SettingsEntry(
                label = stringResource(R.string.settings_page_general),
                summary = languageLabel + " · " + stringResource(R.string.settings_sub_general),
                onOpen = { onOpen(SettingsPageId.GENERAL) },
                icon = { SlidersMark(color = it) }
            )
            SettingsEntry(
                label = stringResource(R.string.settings_page_about),
                summary = BuildConfig.VERSION_NAME,
                onOpen = { onOpen(SettingsPageId.ABOUT) },
                icon = { InfoMark(color = it) }
            )
            }
        }
    }
}

/**
 * Une famille du hub : son nom, puis ses entrees, chacune sur sa plaque.
 *
 * L'intitule est en casse de phrase, au poids du texte courant — pas un
 * micro-label en capitales trackees, que ce depot a construit puis retire.
 * pourquoi : docs/decisions/coquille-ecrans.md § Le titre de groupe parle la voix de l'app
 */
@Composable
private fun HubGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(HUB_ROW_GAP)) {
        SectionHeader(title, modifier = Modifier.padding(start = 4.dp))
        content()
    }
}

/** L'ecart entre deux entrees d'une meme famille. */
private val HUB_ROW_GAP = 10.dp

/** L'ecart entre deux familles, intitule compris. */
private val HUB_FAMILY_GAP = 18.dp

/** Combien de preparations la page des emulateurs compte : PPSSPP, PS2, remplissage. */
private const val EMULATOR_STEPS = 3
