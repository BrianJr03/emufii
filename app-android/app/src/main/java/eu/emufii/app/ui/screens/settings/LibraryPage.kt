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
 * La bibliotheque : d'ou viennent les jeux, ce qui les illustre, ce qui les
 * identifie, et ce qui a ete retire de la grille.
 *
 * **Quatre blocs, un sujet chacun.** Le bloc des icones en portait deux — la
 * source des images, et la cle du service de secours — et il avait fini a neuf
 * elements empiles : une bande, un paragraphe, deux boutons, un fait, une
 * marque, une note, un champ, une note. Le second dossier de ROMs, ajoute le
 * meme jour, avait pousse le bloc des dossiers a quatre boutons dont deux se
 * ressemblaient (« Changer de dossier » / « Changer le 2e dossier »).
 *
 * Les deux desordres avaient la meme cause : **des actions posees a cote de
 * l'objet au lieu d'etre posees dessus.** Un dossier se change en touchant le
 * dossier ; la cle de secours est un sujet, donc un bloc.
 * pourquoi : docs/decisions/reglages-ecran.md § Sur une page, l'état passe devant l'explication
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
    /** Quelques jaquettes reelles, pour que le bloc des icones montre au lieu de nommer. */
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
        // L'ordre decide des colonnes : les pairs a gauche, les impairs a
        // droite. A gauche, ce qui remplit la grille — les dossiers, puis ce
        // qui en a ete retire ; a droite, ce qui l'habille, puis le service qui
        // prend le relais quand rien sur l'appareil ne convient.
        // Les deux blocs de tete se regardent en travers de la gouttiere, et
        // c'est la premiere chose qu'on voit de la page : ils finissent a la
        // meme hauteur, le plus grand des deux donnant la mesure.
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

// --------------------------------------------------------------- les dossiers

/**
 * Ou sont les ROMs. Un ou deux emplacements, et le bouton pour tout reparcourir.
 *
 * **L'emplacement est le bouton.** Toucher un dossier le change, et le second
 * emplacement, tant qu'il est vide, est ce qui invite a l'ajouter : c'est la
 * grille de la bibliotheque qui a donne cette forme, ou une case vide dit ce
 * qui pourrait la remplir sans qu'un bouton ait a le nommer.
 * pourquoi : CLAUDE.md § UI : le monde « HOME MENU »
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
                    // Le seul moment ou cette page n'a qu'une chose a faire.
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
                        // Le retrait ne peut pas vivre dans l'emplacement : deux
                        // zones cliquables imbriquees donnent deux arrets de
                        // curseur au meme endroit, dont un invisible.
                        // pourquoi : CLAUDE.md § Navigation à la manette
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

        // Le second emplacement n'apparait pas avant le premier : proposer un
        // « 2e dossier » a qui n'en a aucun nomme une place dans une liste vide.
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

/**
 * Un emplacement rempli : la marque, le nom, ce qu'on en fait — et le chevron,
 * qui est ce qui dit que la rangee entiere se touche.
 */
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

/**
 * L'emplacement vide : un contour pointille et une croix. Il ne se remplit pas
 * tout seul, donc il se dessine comme une place, pas comme une carte ratee.
 */
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

// ----------------------------------------------------------------- les images

/**
 * D'ou viennent les icones de la grille : des images deja sur l'appareil.
 *
 * Le bloc ne parle plus que de Cocoon. Le service de secours a pris son propre
 * bloc — ils repondaient a deux questions differentes (« qu'est-ce qui habille
 * mes jeux ? » et « ou chercher quand rien ne les habille ? ») dans une seule
 * pile de neuf elements.
 * pourquoi : docs/decisions/reglages-ecran.md § Les images des pages viennent de l'appareil, pas d'une banque
 */
@Composable
private fun ArtworkBlock(
    modifier: Modifier = Modifier,
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
        modifier = modifier,
        // Le pied colle au bas de la carte : deux cartes de meme hauteur dont
        // les boutons flottent a des niveaux differents se lisent comme deux
        // hauteurs quand meme.
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
                        // Ouvert droit sur le dossier de Cocoon plutot que la ou
                        // le selecteur en etait reste : naviguer un selecteur
                        // vers un dossier dont on n'a pas choisi le nom est
                        // exactement le genre de petite tache qu'on abandonne.
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
                                // `forget` ne vide que l'index de Cocoon : les
                                // vignettes ecrites pendant le scan restent sur
                                // le disque, donc la bande et la grille
                                // continuaient d'afficher les images de Cocoon
                                // apres y avoir renonce. Il faut reparcourir
                                // pour que la source change vraiment.
                                // pourquoi : docs/decisions/reglages-ecran.md § Renoncer à Cocoon demande un nouveau parcours
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
        // Le bloc parlait d'images sans jamais en montrer une. Ici, ce sont les
        // jaquettes de la bibliotheque du joueur, telles qu'elles sont a
        // l'instant : changer de source change la bande.
        ArtworkStrip(sample)
        DetailNote(stringResource(R.string.settings_cocoon_body))
    }
}

/**
 * Le service de secours, et rien d'autre : ce qui habille les jeux dont aucune
 * image n'existe sur l'appareil.
 *
 * Bloc a part depuis le 2026-08-28. La cle vivait en bas du bloc des icones,
 * derriere une bande d'images, un paragraphe et deux boutons — un champ de
 * saisie au huitieme rang d'une pile, ou personne ne va le chercher.
 *
 * **Replie par defaut**, et c'est le seul bloc de la page qui l'est : une cle
 * de service se saisit une fois dans la vie de l'app, et rien de ce qu'il cache
 * n'est un etat qu'on vient verifier — la pastille dit deja s'il y en a une.
 * Referme, il ne montre que ce que montre « Jeux retires du menu », son voisin
 * de rangee : un titre, une pastille, trois lignes. Les deux cartes font alors
 * la meme hauteur, ce qui etait la demande.
 */
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
        // La note reste visible replie : elle est ce qui dit a quoi sert le
        // bloc, donc ce qui donne envie de l'ouvrir. Sans elle, la carte fermee
        // ne serait qu'un titre et un mot.
        DetailNote(stringResource(R.string.settings_artwork_body))
        if (expanded) {
            SteamGridDbMark()
            // **En clair, pas masquee** : ce n'est pas un mot de passe, et la
            // masquer ne cacherait que la faute de frappe, qui est l'incident
            // probable.
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
