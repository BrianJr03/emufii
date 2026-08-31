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
 * What has to be prepared inside the emulators before playing. These three blocks lived
 * under Application, between the language and the theme, for want of anywhere else.
 * They have nothing to do with the app's look: they are rituals outside Emufii.
 * pourquoi : docs/decisions/onboarding.md § The emulator rituals are the settings blocks, not copies
 * pourquoi : docs/decisions/reglages-ecran.md § Emulators are not an application setting
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
        // The method shows only while it teaches something. Once the folder is chosen,
        // what the player comes to check is the state, and three steps above it are in
        // the way.
        // pourquoi : docs/decisions/reglages-ecran.md § On a page, the state comes before the explanation
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

        // The page's first control: the pad comes down to it from the header.
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
        // The explanation is worth having only while it teaches. Once the card is
        // prepared and assigned, what the player comes to check is the state.
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

        // A folder card is neither an error nor an override: it is the one thing the
        // player has to know, being the reason none of their saves was cloned.
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
        // Failure carries red; a folder card and per-game overrides are things to know
        // while all is well, and take the warning hollow.
        // pourquoi : docs/decisions/reglages-ecran.md § A warning is not an error, and does not carry the red
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
            // The only filled button: the one that does the work. Once done, the accent
            // leaves rather than the label turning into a boast.
            // pourquoi : docs/decisions/reglages-ecran.md § A button is named for what it does
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
 * Automatic filling, and this block's only reason: Android can switch it off on its
 * own. The block shows the state either way, which makes it findable before something
 * goes wrong.
 * pourquoi : docs/decisions/reglages-ecran.md § Autofill has its own row because Android can switch it off
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
            // Filled while it is off: an update can withdraw the permission, and this
            // is then the only thing between the player and automatic setup.
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
