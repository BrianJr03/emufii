package eu.emufii.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import eu.emufii.app.ui.controlRing
import eu.emufii.app.ui.components.cardSliceFill
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.socket
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.emufii.app.R
import eu.emufii.app.artwork.CocoonMedia
import eu.emufii.app.library.Rom
import eu.emufii.app.settings.SettingsStore
import eu.emufii.app.ui.components.ChevronRight
import eu.emufii.app.ui.components.DetailActions
import eu.emufii.app.ui.components.DetailNote
import eu.emufii.app.ui.components.DetailTone
import eu.emufii.app.ui.components.GhostButton
import eu.emufii.app.ui.components.PadTextField
import eu.emufii.app.ui.components.PrimaryButton
import eu.emufii.app.ui.components.FolderMark
import eu.emufii.app.ui.components.SteamGridDbMark
import eu.emufii.app.ui.components.padEntry
import eu.emufii.app.ui.tap

/**
 * Where the games come from, what illustrates them, what identifies them, and what was
 * hidden.
 * pourquoi : docs/decisions/reglages-ecran.md § On a page, the state comes before the explanation
 */
@Composable
internal fun LibraryPage(
    folder: String?,
    secondFolder: String?,
    scanning: Boolean,
    count: Int?,
    onPickFolder: () -> Unit,
    onPickSecondFolder: () -> Unit,
    onRemoveSecondFolder: () -> Unit,
    onRescan: () -> Unit,
    artworkKey: String,
    onArtworkKeyChange: (String) -> Unit,
    artworkSample: List<Rom>,
    hiddenCount: Int,
    onRestoreHidden: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsPage(
        title = stringResource(R.string.settings_page_library),
        onBack = onBack,
        modifier = modifier
    ) {
        // Order decides the columns: even left, odd right.
        val head = rememberBlockHeights()
        SettingsColumns(
            {
                FoldersBlock(
                    modifier = Modifier.sameHeightAs(head),
                    folder = folder,
                    secondFolder = secondFolder,
                    scanning = scanning,
                    count = count,
                    onPickFolder = onPickFolder,
                    onPickSecondFolder = onPickSecondFolder,
                    onRemoveSecondFolder = onRemoveSecondFolder,
                    onRescan = onRescan
                )
            },
            {
                ArtworkBlock(
                    modifier = Modifier.sameHeightAs(head),
                    sample = artworkSample,
                    onSourceChanged = onRescan
                )
            },
            {
                HiddenRomsBlock(count = hiddenCount, onRestore = onRestoreHidden)
            },
            {
                FallbackBlock(key = artworkKey, onKeyChange = onArtworkKeyChange)
            },
        )
    }
}

// ----------------------------------------------------------------- folders

/**
 * One or two slots and the rescan button. The slot is the button.
 * pourquoi : CLAUDE.md § Working rules, the "HOME MENU" world
 */
@Composable
private fun FoldersBlock(
    modifier: Modifier = Modifier,
    folder: String?,
    secondFolder: String?,
    scanning: Boolean,
    count: Int?,
    onPickFolder: () -> Unit,
    onPickSecondFolder: () -> Unit,
    onRemoveSecondFolder: () -> Unit,
    onRescan: () -> Unit
) {
    SettingsBlock(
        modifier = modifier,
        spread = true,
        title = stringResource(R.string.settings_row_folder),
        state = BlockState(
            when {
                scanning -> DetailTone.BUSY
                folder == null -> DetailTone.WARN
                else -> DetailTone.GOOD
            },
            when {
                scanning -> stringResource(R.string.settings_pill_scanning)
                folder == null -> stringResource(R.string.settings_pill_no_folder)
                count != null -> pluralStringResource(R.plurals.settings_pill_games, count, count)
                else -> stringResource(R.string.settings_pill_ready)
            }
        ),
        footer = {
            DetailActions {
                if (folder == null) {
                    PrimaryButton(
                        label = stringResource(R.string.lib_choose_folder),
                        onClick = onPickFolder,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton(
                            label = stringResource(R.string.settings_library_rescan),
                            onClick = onRescan,
                            fillWidth = true,
                            modifier = Modifier.weight(1f)
                        )
                        // Removal cannot live inside the slot: two nested clickables
                        // give two cursor stops.
                        // pourquoi : CLAUDE.md § Gamepad navigation
                        if (secondFolder != null) {
                            GhostButton(
                                label = stringResource(R.string.settings_library_remove_second),
                                onClick = onRemoveSecondFolder,
                                fillWidth = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    ) {
        FolderSlot(
            name = folder ?: stringResource(R.string.lib_no_folder_title),
            note = if (folder == null) stringResource(R.string.settings_library_note)
            else stringResource(R.string.settings_library_subfolders),
            onClick = onPickFolder,
            entry = true
        )

        // Offering a second folder to someone with none names a place that does not
        // exist.
        if (folder != null) {
            if (secondFolder != null) {
                FolderSlot(
                    name = secondFolder,
                    note = stringResource(R.string.settings_library_second_note),
                    onClick = onPickSecondFolder
                )
            } else {
                EmptyFolderSlot(
                    label = stringResource(R.string.settings_library_add_second),
                    onClick = onPickSecondFolder
                )
            }
        }
    }
}

/** Mark, name, what it does, and the chevron that says the whole row is the target. */
@Composable
private fun FolderSlot(
    name: String,
    note: String,
    onClick: () -> Unit,
    entry: Boolean = false
) {
    val dark = LocalEmufiiDarkTheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (entry) Modifier.padEntry() else Modifier)
            .controlRing(ROW_SHAPE)
            .socket(ROW_SHAPE, dark)
            .clip(ROW_SHAPE)
            .tap(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        FolderMark(size = 26.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ChevronRight(
            size = 18.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

/** It does not fill itself, so it is drawn as a place rather than a thing. */
@Composable
private fun EmptyFolderSlot(label: String, onClick: () -> Unit) {
    val outline = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .controlRing(ROW_SHAPE)
            .cardSliceFill(ROW_SHAPE)
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    color = outline,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(6.dp.toPx(), 6.dp.toPx())
                        )
                    )
                )
            }
            .clip(ROW_SHAPE)
            .tap(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "+",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

// ------------------------------------------------------------------ images

/**
 * From images already on the device. The block only speaks of Cocoon now.
 * pourquoi : docs/decisions/reglages-ecran.md § The pages' images come from the device, not from a stock library
 */
@Composable
private fun ArtworkBlock(
    modifier: Modifier = Modifier,
    sample: List<Rom>,
    onSourceChanged: () -> Unit,
) {
    val context = LocalContext.current
    val settingsStore = remember(context) { SettingsStore.get(context) }
    val cocoon by settingsStore.cocoonFolder.collectAsState()

    val cocoonPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // Read only: asking for write would ask for what we do not need.
            val granted = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                true
            }.getOrDefault(false)
            if (granted) {
                settingsStore.setCocoonFolder(uri.toString())
                CocoonMedia.forget()
                onSourceChanged()
            }
        }
    }

    SettingsBlock(
        modifier = modifier,
        // Two cards of one height whose buttons float at different levels read as
        // misaligned.
        spread = true,
        title = stringResource(R.string.settings_row_artwork),
        state = BlockState(
            if (cocoon.isNotBlank()) DetailTone.GOOD else DetailTone.WARN,
            stringResource(
                if (cocoon.isNotBlank()) R.string.settings_artwork_source_cocoon
                else R.string.settings_artwork_source_none
            )
        ),
        footer = {
            DetailActions {
                if (cocoon.isBlank()) {
                    PrimaryButton(
                        label = stringResource(R.string.settings_cocoon_choose),
                        // Straight to Cocoon's folder rather than wherever the picker
                        // was left.
                        onClick = { cocoonPicker.launch(COCOON_DEFAULT_FOLDER) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton(
                            label = stringResource(R.string.settings_cocoon_change),
                            onClick = { cocoonPicker.launch(COCOON_DEFAULT_FOLDER) },
                            fillWidth = true,
                            modifier = Modifier.weight(1f)
                        )
                        GhostButton(
                            label = stringResource(R.string.settings_cocoon_forget),
                            onClick = {
                                settingsStore.setCocoonFolder("")
                                // `forget` clears only Cocoon's index: thumbnails
                                // written during the scan stay on disk.
                                // pourquoi : docs/decisions/reglages-ecran.md § Giving up Cocoon needs a fresh walk
                                CocoonMedia.forget()
                                onSourceChanged()
                            },
                            fillWidth = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    ) {
        // The block spoke of images without showing one.
        ArtworkStrip(sample)
        DetailNote(stringResource(R.string.settings_cocoon_body))
    }
}

/** The fallback service: what dresses games no image on the device covers. */
@Composable
private fun FallbackBlock(key: String, onKeyChange: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    SettingsBlock(
        title = stringResource(R.string.settings_row_fallback),
        state = BlockState(
            if (key.isNotBlank()) DetailTone.GOOD else DetailTone.WARN,
            stringResource(
                if (key.isNotBlank()) R.string.settings_artwork_fallback_on
                else R.string.settings_artwork_fallback_off
            )
        ),
        onToggleExpanded = { expanded = !expanded },
        expanded = expanded
    ) {
        // The note stays visible folded: it is what says what the block is for.
        DetailNote(stringResource(R.string.settings_artwork_body))
        if (expanded) {
            SteamGridDbMark()
            // In the clear: this is not a password, and masking would hide only the
            // typo.
            PadTextField(
                value = key,
                onValueChange = onKeyChange,
                label = stringResource(R.string.settings_artwork_field),
                modifier = Modifier.fillMaxWidth()
            )
            DetailNote(stringResource(R.string.settings_artwork_where))
        }
    }
}

/**
 * All or nothing by design: a per-game list could not be crossed with a stick.
 * pourquoi : docs/decisions/reglages-ecran.md § Restoring hidden games is all or nothing
 */
@Composable
private fun HiddenRomsBlock(count: Int, onRestore: () -> Unit) {
    SettingsBlock(
        title = stringResource(R.string.settings_row_hidden),
        state = BlockState(
            if (count == 0) DetailTone.GOOD else DetailTone.BUSY,
            if (count == 0) stringResource(R.string.settings_pill_none)
            else pluralStringResource(R.plurals.settings_pill_hidden, count, count)
        )
    ) {
        DetailNote(stringResource(R.string.settings_hidden_body))
        if (count > 0) {
            GhostButton(
                label = stringResource(R.string.settings_hidden_restore),
                onClick = onRestore,
                fillWidth = true
            )
        }
    }
}

private val COCOON_DEFAULT_FOLDER: Uri = DocumentsContract.buildDocumentUri(
    "com.android.externalstorage.documents",
    "primary:Cocoonv2"
)
