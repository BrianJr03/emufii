package eu.emufii.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import eu.emufii.app.R
import eu.emufii.app.library.Console
import eu.emufii.app.profile.playerDisplayName
import eu.emufii.app.ps2.Ps2Armsx2Folder
import eu.emufii.app.ps2.Ps2NetworkProfile
import eu.emufii.app.psp.PpssppConfigResult
import eu.emufii.app.psp.PpssppConfigStore
import eu.emufii.app.ui.components.DetailActions
import eu.emufii.app.ui.components.DetailNote
import eu.emufii.app.ui.components.DetailTone
import eu.emufii.app.ui.components.GhostButton
import eu.emufii.app.ui.components.PrimaryButton
import eu.emufii.app.ui.components.padEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Ce qu'il faut preparer dans les emulateurs avant de pouvoir jouer.
 *
 * Ces trois blocs vivaient dans « Application », entre la langue et le theme,
 * parce qu'il n'existait pas d'endroit ou les mettre. Ils n'ont rien a voir
 * avec l'apparence de l'app : ce sont des rituels hors d'Emufii — parametrer
 * PPSSPP, importer le profil reseau dans ARMSX2, rendre son service a Azahar —
 * et une session est refusee tant qu'ils ne sont pas faits. C'est la seule
 * page des reglages ou l'app a quelque chose a **demander** au joueur, et
 * c'est ce qui lui vaut la sienne.
 *
 * Les trois blocs sont `internal` : l'onboarding les pose tels quels.
 * pourquoi : docs/decisions/onboarding.md § Les rituels d'émulateur sont les blocs des réglages, pas des copies
 * pourquoi : docs/decisions/reglages-ecran.md § Les émulateurs ne sont pas un réglage de l'application
 */
@Composable
internal fun EmulatorsPage(
    ppssppConfig: PpssppConfigStore,
    ppssppReady: Boolean,
    onPpssppReadyChanged: (Boolean) -> Unit,
    ps2Ready: Boolean,
    profileName: String,
    onPs2ReadyChanged: (Boolean) -> Unit,
    autofillOn: Boolean,
    onOpenAutofill: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsPage(
        title = stringResource(R.string.settings_page_emulators),
        onBack = onBack,
        modifier = modifier
    ) {
        SettingsColumns(
            {
                PpssppBlock(
                    store = ppssppConfig,
                    ready = ppssppReady,
                    onReadyChanged = onPpssppReadyChanged,
                )
            },
            {
                Ps2Block(
                    ready = ps2Ready,
                    profileName = profileName,
                    onReadyChanged = onPs2ReadyChanged,
                )
            },
            {
                AutofillBlock(enabled = autofillOn, onOpen = onOpenAutofill)
            },
        )
    }
}

@Composable
internal fun PpssppBlock(
    store: PpssppConfigStore,
    ready: Boolean,
    onReadyChanged: (Boolean) -> Unit,
) {
    var rootUri by remember { mutableStateOf(store.rootUri()) }
    var error by remember { mutableStateOf<Int?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            error = when (store.configureRoot(uri)) {
                PpssppConfigResult.Success -> {
                    rootUri = uri
                    onReadyChanged(true)
                    null
                }
                PpssppConfigResult.PermissionMissing -> R.string.ppsspp_config_permission_missing
                PpssppConfigResult.InvalidRoot -> R.string.ppsspp_config_invalid_root
                PpssppConfigResult.ActiveOverrides -> R.string.ppsspp_config_active_overrides
                PpssppConfigResult.NotConfigured -> R.string.ppsspp_config_not_configured
                PpssppConfigResult.UnknownDiscId -> R.string.ppsspp_config_unknown_game
                is PpssppConfigResult.Failure -> R.string.ppsspp_config_write_failed
            }
            if (error != null) onReadyChanged(store.isReady())
        }
    }

    SettingsBlock(
        title = stringResource(R.string.settings_row_ppsspp_config),
        mark = { EmulatorMark(Console.PSP) },
        state = BlockState(
            if (ready) DetailTone.GOOD else DetailTone.WARN,
            stringResource(
                if (ready) R.string.settings_pill_ready else R.string.settings_pill_todo
            )
        )
    ) {
        // La methode ne s'affiche que tant qu'elle apprend quelque chose. Une
        // fois le dossier choisi, ce que le joueur vient verifier est l'etat,
        // et trois etapes au-dessus sont dans le chemin.
        // pourquoi : docs/decisions/reglages-ecran.md § Sur une page, l'état passe devant l'explication
        if (!ready) {
            SettingsSteps(
                stringResource(R.string.settings_ppsspp_step1),
                stringResource(R.string.settings_ppsspp_step2),
                stringResource(R.string.settings_ppsspp_step3),
            )
        } else {
            store.rootLabel()?.let {
                BlockFact(stringResource(R.string.settings_library_fact_folder), it)
            }
            DetailNote(stringResource(R.string.settings_ppsspp_caveat))
        }

        if (!ready && rootUri != null) {
            BlockNotice(stringResource(R.string.settings_ppsspp_config_not_ready))
        }
        error?.let { BlockCaveat(stringResource(it)) }

        // Premier controle de la page : la manette y descend depuis l'en-tete.
        DetailActions {
            if (ready) {
                GhostButton(
                    label = stringResource(R.string.settings_ppsspp_config_change),
                    onClick = { picker.launch(rootUri) },
                    fillWidth = true,
                    modifier = Modifier.padEntry(),
                )
            } else {
                PrimaryButton(
                    label = stringResource(R.string.settings_ppsspp_config_choose),
                    onClick = { picker.launch(rootUri) },
                    modifier = Modifier.padEntry().fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
internal fun Ps2Block(
    ready: Boolean,
    profileName: String,
    onReadyChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var rootUri by remember { mutableStateOf(Ps2NetworkProfile.rootUri(context)) }
    var receipt by remember { mutableStateOf(Ps2NetworkProfile.receipt(context)) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val saveTitle = playerDisplayName(profileName)

    fun prepare(uri: Uri) {
        Ps2NetworkProfile.clearReady(context)
        onReadyChanged(false)
        busy = true
        error = null
        scope.launch {
            when (val outcome = withContext(Dispatchers.IO) {
                Ps2Armsx2Folder.prepare(context, uri, saveTitle)
            }) {
                is Ps2Armsx2Folder.Outcome.Success -> {
                    Ps2NetworkProfile.recordPrepared(context, outcome.prepared)
                    receipt = Ps2NetworkProfile.receipt(context)
                    onReadyChanged(true)
                }
                Ps2Armsx2Folder.Outcome.NotArmsx2Folder ->
                    error = context.getString(R.string.settings_ps2_profile_bad_folder)
                Ps2Armsx2Folder.Outcome.MissingWritePermission ->
                    error = context.getString(R.string.settings_ps2_profile_no_write)
                is Ps2Armsx2Folder.Outcome.InvalidMemoryCard ->
                    error = context.getString(R.string.settings_ps2_profile_invalid_card, outcome.name)
                is Ps2Armsx2Folder.Outcome.SourceChanged ->
                    error = context.getString(R.string.settings_ps2_profile_source_changed, outcome.name)
                is Ps2Armsx2Folder.Outcome.AmbiguousBios ->
                    error = context.getString(
                        R.string.settings_ps2_profile_ambiguous_bios,
                        outcome.candidates.joinToString(", "),
                    )
                is Ps2Armsx2Folder.Outcome.BiosUnavailable ->
                    error = context.getString(R.string.settings_ps2_profile_bios_unavailable, outcome.name)
                is Ps2Armsx2Folder.Outcome.BiosUnreadable ->
                    error = context.getString(R.string.settings_ps2_profile_bios_unreadable, outcome.name)
                is Ps2Armsx2Folder.Outcome.WriteFailed -> error = outcome.detail
            }
            busy = false
        }
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val granted = runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
                Ps2NetworkProfile.setRootUri(context, uri)
            }.getOrDefault(false)
            if (granted) {
                rootUri = uri
                receipt = null
                prepare(uri)
            } else error = context.getString(R.string.settings_ps2_profile_no_write)
        }
    }

    val current = receipt
    SettingsBlock(
        title = stringResource(R.string.settings_row_ps2_profile),
        mark = { EmulatorMark(Console.PS2) },
        state = BlockState(
            when {
                busy -> DetailTone.BUSY
                error != null -> DetailTone.BAD
                ready -> DetailTone.GOOD
                else -> DetailTone.WARN
            },
            stringResource(
                when {
                    busy -> R.string.settings_pill_working
                    error != null -> R.string.settings_pill_failed
                    ready -> R.string.settings_pill_ready
                    else -> R.string.settings_pill_todo
                }
            )
        )
    ) {
        // L'explication ne vaut que tant qu'elle apprend. Une fois la carte
        // preparee et affectee, ce que le joueur vient verifier est l'etat, et
        // trois etapes de methode au-dessus sont dans le chemin.
        if (!ready) {
            SettingsSteps(
                stringResource(R.string.settings_ps2_step1),
                stringResource(R.string.settings_ps2_step2),
                stringResource(R.string.settings_ps2_step3),
            )
        }

        if (current != null) {
            BlockFact(
                stringResource(R.string.settings_ps2_fact_card),
                current.cardName
            )
            BlockFact(
                stringResource(R.string.settings_ps2_fact_source),
                current.sourceCardName ?: stringResource(R.string.settings_ps2_profile_new_card)
            )
            BlockFact(
                stringResource(R.string.settings_ps2_fact_bios),
                current.biosName ?: stringResource(R.string.settings_ps2_profile_default_bios)
            )
            BlockFact(
                stringResource(R.string.settings_ps2_fact_console),
                current.consoleIdHex
            )
        }

        // Une carte dossier n'est ni une erreur ni un remplacement : c'est la
        // seule chose que le joueur doit absolument savoir, parce que c'est la
        // raison pour laquelle aucune de ses sauvegardes n'a ete clonee.
        val folderCardNote = current?.folderCardName?.let { name ->
            when {
                current.savesLeftBehind > 0 -> stringResource(
                    R.string.settings_ps2_profile_folder_partial,
                    name,
                    current.importedSaveCount,
                    current.savesLeftBehind,
                )
                current.importedSaveCount > 0 -> stringResource(
                    R.string.settings_ps2_profile_folder_imported,
                    name,
                    current.importedSaveCount,
                )
                else -> stringResource(R.string.settings_ps2_profile_folder_empty, name)
            }
        }
        // L'echec porte le rouge ; la carte dossier et les remplacements par jeu
        // sont des choses a savoir pendant que tout va bien, et prennent le
        // creux d'avertissement.
        // pourquoi : docs/decisions/reglages-ecran.md § Un avertissement n'est pas une erreur, et ne porte pas le rouge
        error?.let { BlockCaveat(it) }
        val notice = when {
            error != null -> null
            folderCardNote != null -> folderCardNote
            current != null && current.gameOverrideCount > 0 -> pluralStringResource(
                R.plurals.settings_ps2_profile_overrides,
                current.gameOverrideCount,
                current.gameOverrideCount,
            )
            else -> null
        }
        notice?.let { BlockNotice(it) }

        DetailActions {
            // Le seul bouton rempli : celui qui fait le travail. Une fois fait,
            // l'accent s'en va plutot que l'etiquette ne se transforme en
            // vantardise.
            // pourquoi : docs/decisions/reglages-ecran.md § Un bouton est nommé pour ce qu'il fait
            if (ready) {
                GhostButton(
                    label = stringResource(R.string.hint_ps2_profile_redo),
                    onClick = { rootUri?.let { prepare(it) } },
                    fillWidth = true
                )
            } else {
                PrimaryButton(
                    label = stringResource(
                        if (rootUri == null) R.string.hint_ps2_profile_choose_folder
                        else R.string.hint_ps2_profile_button
                    ),
                    onClick = {
                        val uri = rootUri
                        if (uri == null) folderPicker.launch(null) else prepare(uri)
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (rootUri != null) {
                GhostButton(
                    label = stringResource(R.string.hint_ps2_profile_change_folder),
                    onClick = { folderPicker.launch(rootUri) },
                    fillWidth = true,
                )
            }
        }
    }
}

/**
 * Le remplissage automatique, et la seule raison de ce bloc : **Android peut
 * l'eteindre** tout seul. Le bloc montre l'etat dans les deux cas, ce qui le
 * rend trouvable *avant* que quelque chose n'aille mal.
 * pourquoi : docs/decisions/reglages-ecran.md § Le remplissage automatique a sa rangée parce qu'Android peut l'éteindre
 */
@Composable
internal fun AutofillBlock(enabled: Boolean, onOpen: () -> Unit) {
    SettingsBlock(
        title = stringResource(R.string.settings_row_autofill),
        mark = { EmulatorMark(Console.THREE_DS) },
        state = BlockState(
            if (enabled) DetailTone.GOOD else DetailTone.WARN,
            stringResource(
                if (enabled) R.string.settings_value_autofill_on
                else R.string.settings_value_autofill_off
            )
        )
    ) {
        DetailNote(stringResource(R.string.settings_autofill_note))
        if (!enabled) BlockNotice(stringResource(R.string.settings_autofill_off))

        DetailActions {
            // Rempli tant qu'il est coupe : une mise a jour peut retirer la
            // permission, et c'est alors la seule chose entre le joueur et le
            // parametrage automatique.
            if (enabled) {
                GhostButton(
                    label = stringResource(R.string.settings_autofill_open),
                    onClick = onOpen,
                    fillWidth = true
                )
            } else {
                PrimaryButton(
                    label = stringResource(R.string.settings_autofill_open),
                    onClick = onOpen,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
