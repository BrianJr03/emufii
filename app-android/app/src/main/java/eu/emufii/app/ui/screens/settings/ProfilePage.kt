package eu.emufii.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.emufii.app.R
import eu.emufii.app.profile.Profile
import eu.emufii.app.profile.playerDisplayName
import eu.emufii.app.ui.components.Avatar
import eu.emufii.app.ui.components.GhostButton
import eu.emufii.app.ui.components.PadTextField
import eu.emufii.app.ui.components.padEntry
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import eu.emufii.app.ui.components.DetailNote
import eu.emufii.app.ui.components.PencilMark
import eu.emufii.app.ui.theme.AccentGreen
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.socket
import eu.emufii.app.ui.controlRing

/**
 * La page du profil : la photo, le pseudo, et ce que les autres en voient.
 *
 * La remise a zero vit ici et nulle part ailleurs. Elle avait sa propre
 * section « zone rouge » sur l'ecran unique ; une fois l'ecran decoupe, une
 * section entiere pour une rangee qu'on presse une fois dans la vie de l'app
 * n'avait plus lieu d'etre, et le geste appartient a l'identite qu'il efface.
 * pourquoi : docs/decisions/reglages-ecran.md § La remise à zéro vit sur la page qu'elle efface
 */
@Composable
internal fun ProfilePage(
    profile: Profile,
    name: String,
    onNameChange: (String) -> Unit,
    photoError: String?,
    onPickPhoto: () -> Unit,
    onClearPhoto: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsPage(
        title = stringResource(R.string.settings_page_profile),
        onBack = onBack,
        modifier = modifier
    ) {
        SettingsColumns(
            {
                SettingsBlock(title = stringResource(R.string.settings_row_identity)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Un seul arret de curseur, pas deux : la photo et le
                            // crayon declenchent la meme chose, et deux noeuds
                            // focalisables pour un geste faisaient deux pressions
                            // de direction sans que rien ne bouge a l'ecran.
                            Box(
                                contentAlignment = Alignment.BottomEnd,
                                modifier = Modifier
                                    .padEntry()
                                    .controlRing(CircleShape)
                                    .clickable(onClick = onPickPhoto)
                            ) {
                                Avatar(
                                    name = playerDisplayName(name.ifBlank { Profile.DEFAULT_NAME }),
                                    imageFile = profile.avatarFile,
                                    size = 78.dp
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        PencilMark(size = 14.dp, color = Color.White)
                                    }
                                }
                            }
                            if (profile.avatarFile != null) {
                                GhostButton(
                                    label = stringResource(R.string.profile_remove_photo),
                                    onClick = onClearPhoto,
                                    tint = DANGER
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PadTextField(
                                value = name,
                                onValueChange = {
                                    if (it.length <= Profile.MAX_NAME_LENGTH) onNameChange(it)
                                },
                                placeholder = stringResource(R.string.profile_default_name),
                                label = stringResource(R.string.profile_name_label),
                                // Le meme plancher qu'a l'accueil : la regle doit
                                // tenir partout ou le nom s'edite, sinon le pseudo
                                // raccourci ici retombe dans le formulaire de
                                // l'emulateur sous une forme qu'il refuse.
                                isError = name.trim().length < Profile.MIN_NAME_LENGTH,
                                supportingText = {
                                    Text(
                                        if (name.trim().length < Profile.MIN_NAME_LENGTH) {
                                            stringResource(
                                                R.string.onb_name_too_short,
                                                Profile.MIN_NAME_LENGTH
                                            )
                                        } else {
                                            stringResource(R.string.profile_name_hint)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (name.trim().length < Profile.MIN_NAME_LENGTH) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    photoError?.let { BlockCaveat(it) }
                }
            },
            {
                SettingsBlock(title = stringResource(R.string.settings_profile_seen_by)) {
                    // Montrer plutot que decrire. « Ta photo et ton pseudo sont
                    // visibles par tes amis » demandait au joueur d'imaginer le
                    // resultat ; la rangee ci-dessous **est** le resultat, dessinee
                    // comme la liste d'amis la dessine.
                    // pourquoi : docs/decisions/reglages-ecran.md § Les images des pages viennent de l'appareil, pas d'une banque
                    SeenByOthers(name = name)
                    DetailNote(stringResource(R.string.profile_photo_note))
                }
            },
        )

        DangerRow(
            label = stringResource(R.string.profile_reset),
            onClick = onReset
        )
    }
}

/**
 * Le joueur tel que les autres le voient — **sans sa photo**.
 *
 * C'est tout l'interet de cet apercu, et ca a failli etre son defaut : la
 * premiere version affichait l'avatar avec le fichier local, et donnait donc
 * exactement le contraire de ce que la phrase en dessous explique. La photo
 * ne quitte pas l'appareil ; les autres recoivent les initiales et la couleur.
 * Montrer ce qu'ils recoivent apprend en une rangee ce qu'un paragraphe
 * demandait d'imaginer.
 * pourquoi : docs/decisions/reglages-ecran.md § Les images des pages viennent de l'appareil, pas d'une banque
 */
@Composable
private fun SeenByOthers(name: String) {
    val dark = LocalEmufiiDarkTheme.current
    val displayName = playerDisplayName(name.ifBlank { Profile.DEFAULT_NAME })
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .socket(ROW_SHAPE, dark)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Avatar(name = displayName, imageFile = null, size = 40.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(R.string.friends_online),
                style = MaterialTheme.typography.bodySmall,
                color = AccentGreen
            )
        }
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(AccentGreen)
        )
    }
}
