package eu.emufii.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.socket
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.emufii.app.R
import eu.emufii.app.artwork.CocoonMedia
import eu.emufii.app.library.Console
import eu.emufii.app.library.Rom
import eu.emufii.app.settings.SettingsStore
import eu.emufii.app.ui.components.DetailActions
import eu.emufii.app.ui.components.DetailFact
import eu.emufii.app.ui.components.DetailNote
import eu.emufii.app.ui.components.DetailStatus
import eu.emufii.app.ui.components.DetailTone
import eu.emufii.app.ui.components.GhostButton
import eu.emufii.app.ui.components.PadTextField
import eu.emufii.app.ui.components.PrimaryButton
import eu.emufii.app.ui.components.FolderMark
import eu.emufii.app.ui.components.SteamGridDbMark
import eu.emufii.app.ui.components.padEntry

/**
 * La bibliotheque : d'ou viennent les jeux, ce qui les illustre, ce qui les
 * identifie, et ce qui a ete retire de la grille.
 *
 * Quatre blocs sur une page au lieu de quatre rangees qui se depliaient a
 * quatre endroits d'un ecran de quatorze. Ils partagent un sujet — ce que la
 * grille montre — et c'est le seul critere de rangement que cette page suit.
 */
@Composable
internal fun LibraryPage(
    folder: String?,
    scanning: Boolean,
    count: Int?,
    onPickFolder: () -> Unit,
    onRescan: () -> Unit,
    artworkKey: String,
    onArtworkKeyChange: (String) -> Unit,
    /** Quelques jaquettes reelles, pour que le bloc des icones montre au lieu de nommer. */
    artworkSample: List<Rom>,
    hasKeys: Boolean,
    keysRejected: Boolean,
    onPickKeys: () -> Unit,
    onForgetKeys: () -> Unit,
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
        // L'ordre decide des colonnes : les pairs a gauche, les impairs a
        // droite. A gauche, ce qui remplit la grille — le dossier, puis ce qui
        // en a ete retire ; a droite, ce qui l'habille et ce qui l'identifie.
        SettingsColumns(
            {
                FolderBlock(
                    folder = folder,
                    scanning = scanning,
                    count = count,
                    onPickFolder = onPickFolder,
                    onRescan = onRescan
                )
            },
            {
                ArtworkBlock(
                    key = artworkKey,
                    onKeyChange = onArtworkKeyChange,
                    sample = artworkSample,
                    onSourceChanged = onRescan
                )
            },
            {
                HiddenRomsBlock(count = hiddenCount, onRestore = onRestoreHidden)
            },
            {
                KeysBlock(
                    hasKeys = hasKeys,
                    rejected = keysRejected,
                    onPick = onPickKeys,
                    onForget = onForgetKeys
                )
            },
        )
    }
}

/**
 * Ou sont les ROMs, et le bouton pour les reparcourir. La plomberie qu'on regle
 * une fois appartient aux reglages, pas au dock de la bibliotheque.
 * pourquoi : docs/decisions/reglages-ecran.md § Ce que les réglages disent de ce qu'Emufii ne fait pas
 */
@Composable
private fun FolderBlock(
    folder: String?,
    scanning: Boolean,
    count: Int?,
    onPickFolder: () -> Unit,
    onRescan: () -> Unit
) {
    val dark = LocalEmufiiDarkTheme.current
    SettingsBlock(
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
        )
    ) {
        // Le dossier est ce que ce bloc contient de plus important, et il tenait
        // sur une ligne d'etiquette-valeur perdue en travers d'une demi-colonne
        // vide. Ici il est l'objet du bloc : la marque de dossier, le nom que
        // le joueur a choisi, et sous lui ce que le scan y a trouve.
        // pourquoi : docs/decisions/reglages-ecran.md § La carte du dossier montre le dossier
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .socket(ROW_SHAPE, dark)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            FolderMark(size = 26.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    folder ?: stringResource(R.string.lib_no_folder_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (folder == null) stringResource(R.string.settings_library_note)
                    else stringResource(R.string.settings_library_subfolders),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DetailActions {
            // Rempli seulement tant qu'il n'y a pas de dossier, parce que c'est
            // le seul moment ou cette page n'a qu'une chose a faire. Une fois la
            // bibliotheque la, changer de dossier et reparcourir sont deux
            // courses ordinaires, et aucune ne merite l'accent.
            if (folder == null) {
                PrimaryButton(
                    label = stringResource(R.string.lib_choose_folder),
                    onClick = onPickFolder,
                    modifier = Modifier.padEntry().fillMaxWidth()
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton(
                        label = stringResource(R.string.settings_library_change),
                        onClick = onPickFolder,
                        fillWidth = true,
                        modifier = Modifier.weight(1f).padEntry()
                    )
                    // Rien a parcourir tant qu'aucun dossier n'est choisi, et en
                    // choisir un lance deja un scan.
                    GhostButton(
                        label = stringResource(R.string.settings_library_rescan),
                        onClick = onRescan,
                        fillWidth = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * D'ou viennent les cles de console du joueur. Dit deliberement ce qu'Emufii ne
 * fait **pas** : demander un fichier de cles sans explication fait desinstaller
 * une app.
 * pourquoi : docs/decisions/reglages-ecran.md § Ce que les réglages disent de ce qu'Emufii ne fait pas
 */
@Composable
private fun KeysBlock(
    hasKeys: Boolean,
    rejected: Boolean,
    onPick: () -> Unit,
    onForget: () -> Unit
) {
    SettingsBlock(
        title = stringResource(R.string.settings_row_keys),
        mark = { EmulatorMark(Console.SWITCH) },
        state = BlockState(
            if (hasKeys) DetailTone.GOOD else DetailTone.WARN,
            stringResource(
                if (hasKeys) R.string.settings_keys_state_ok
                else R.string.settings_keys_state_none
            )
        )
    ) {
        BlockFact(
            stringResource(R.string.settings_keys_fact_effect),
            stringResource(if (hasKeys) R.string.settings_keys_ok else R.string.settings_keys_none)
        )
        DetailNote(stringResource(R.string.settings_keys_note))
        // Un fichier refuse est une reserve sur l'etat, pas un etat en soi : le
        // joueur a toujours ce qu'il avait avant.
        if (rejected) BlockNotice(stringResource(R.string.settings_keys_bad))

        DetailActions {
            if (hasKeys) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton(
                        label = stringResource(R.string.settings_keys_replace),
                        onClick = onPick
                    )
                    GhostButton(
                        label = stringResource(R.string.settings_keys_forget),
                        onClick = onForget
                    )
                }
            } else {
                PrimaryButton(
                    label = stringResource(R.string.settings_keys_pick),
                    onClick = onPick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * La cle SteamGridDB du joueur. **En clair, pas masquee** : ce n'est pas un mot
 * de passe, et la masquer ne cacherait que la faute de frappe, qui est
 * l'incident probable.
 * pourquoi : docs/decisions/reglages-ecran.md § Ce que les réglages disent de ce qu'Emufii ne fait pas
 */
@Composable
private fun ArtworkBlock(
    key: String,
    onKeyChange: (String) -> Unit,
    sample: List<Rom>,
    /** Change de source = les vignettes deja ecrites sont perimees. */
    onSourceChanged: () -> Unit,
) {
    val context = LocalContext.current
    val settingsStore = remember(context) { SettingsStore.get(context) }
    val cocoon by settingsStore.cocoonFolder.collectAsState()

    val cocoonPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // Lecture seule. On regarde les images de Cocoon et on n'y touche
            // jamais : demander l'ecriture serait demander ce dont on n'a aucun
            // usage.
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
        title = stringResource(R.string.settings_row_artwork),
        state = BlockState(
            if (cocoon.isNotBlank() || key.isNotBlank()) DetailTone.GOOD else DetailTone.WARN,
            stringResource(
                when {
                    cocoon.isNotBlank() -> R.string.settings_artwork_source_cocoon
                    key.isNotBlank() -> R.string.settings_artwork_source_catalogue
                    else -> R.string.settings_artwork_source_none
                }
            )
        )
    ) {
        // Le bloc parlait de deux sources d'images sans jamais en montrer une.
        // Ici, ce sont les jaquettes de la bibliotheque du joueur, telles
        // qu'elles sont a l'instant : changer de source change la bande.
        // pourquoi : docs/decisions/reglages-ecran.md § Les images des pages viennent de l'appareil, pas d'une banque
        ArtworkStrip(sample)

        // Deux sources, et ce ne sont pas des pairs : Cocoon, ce sont des
        // jaquettes deja sur l'appareil, choisies pour ces fichiers-la ; le
        // catalogue est une supposition faite depuis un nom de fichier.
        DetailNote(stringResource(R.string.settings_cocoon_body))

        DetailActions {
            if (cocoon.isBlank()) {
                PrimaryButton(
                    label = stringResource(R.string.settings_cocoon_choose),
                    // Ouvert droit sur le dossier de Cocoon plutot que la ou le
                    // selecteur en etait reste : naviguer un selecteur vers un
                    // dossier dont on n'a pas choisi le nom est exactement le
                    // genre de petite tache qu'on abandonne.
                    onClick = { cocoonPicker.launch(COCOON_DEFAULT_FOLDER) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton(
                        label = stringResource(R.string.settings_cocoon_change),
                        onClick = { cocoonPicker.launch(COCOON_DEFAULT_FOLDER) }
                    )
                    GhostButton(
                        label = stringResource(R.string.settings_cocoon_forget),
                        onClick = {
                            settingsStore.setCocoonFolder("")
                            // `forget` ne vide que l'index de Cocoon : les
                            // vignettes ecrites pendant le scan restent sur le
                            // disque, donc la bande et la grille continuaient
                            // d'afficher les images de Cocoon apres y avoir
                            // renonce. Il faut reparcourir pour que la source
                            // change vraiment.
                            // pourquoi : docs/decisions/reglages-ecran.md § Renoncer à Cocoon demande un nouveau parcours
                            CocoonMedia.forget()
                            onSourceChanged()
                        }
                    )
                }
            }
        }

        // La cle vit sous ce qu'elle explique : c'est le reglage du repli, et
        // elle se lisait comme l'affaire principale du bloc.
        BlockFact(
            stringResource(R.string.settings_artwork_fact_fallback),
            stringResource(
                if (key.isNotBlank()) R.string.settings_artwork_fallback_on
                else R.string.settings_artwork_fallback_off
            )
        )
        SteamGridDbMark()
        DetailNote(stringResource(R.string.settings_artwork_body))
        PadTextField(
            value = key,
            onValueChange = onKeyChange,
            label = stringResource(R.string.settings_artwork_field),
            modifier = Modifier.fillMaxWidth()
        )
        DetailNote(stringResource(R.string.settings_artwork_where))
    }
}

/**
 * Le chemin du retour pour un jeu retire dans un menu long-presse. Tout ou rien
 * a dessein : une liste par jeu ne pourrait montrer que des chemins.
 * pourquoi : docs/decisions/reglages-ecran.md § Restaurer les jeux retirés est tout ou rien
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

/** La ou Cocoon se range d'habitude : une supposition, et rien de plus. */
private val COCOON_DEFAULT_FOLDER: Uri = DocumentsContract.buildDocumentUri(
    "com.android.externalstorage.documents",
    "primary:Cocoonv2"
)
