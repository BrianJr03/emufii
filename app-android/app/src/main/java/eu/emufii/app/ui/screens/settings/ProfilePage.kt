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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
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
import eu.emufii.app.ui.theme.GoodDark
import eu.emufii.app.ui.theme.GoodLight
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.socket
import eu.emufii.app.ui.focusRing
import eu.emufii.app.ui.controlRing
import eu.emufii.app.ui.tap

/**
 * The photo, the nickname, and what others see of them. The reset lives here and
 * nowhere else; it had its own red-zone section on the single screen, and once that was
 * split, a whole section for a row pressed once was too much.
 * pourquoi : docs/decisions/reglages-ecran.md § The reset lives on the page it erases
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
                            // One cursor stop, and the pencil pill is declared after
                            // the ring: `border` draws over the content of the node
                            // carrying it.
                            // pourquoi : docs/decisions/reglages-ecran.md § The ring and the pencil badge are siblings, not parent and child
                            var photoFocused by remember { mutableStateOf(false) }
                            Box(
                                contentAlignment = Alignment.BottomEnd,
                                modifier = Modifier
                                    .padEntry()
                                    .controlRing(CircleShape, enabled = false)
                                    .onFocusEvent { photoFocused = it.hasFocus }
                                    .tap(onClick = onPickPhoto)
                            ) {
                                Box(modifier = Modifier.focusRing(photoFocused, CircleShape)) {
                                    Avatar(
                                        name = playerDisplayName(
                                            name.ifBlank { Profile.DEFAULT_NAME }
                                        ),
                                        imageFile = profile.avatarFile,
                                        size = 78.dp
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    // Coral: the profile is the social domain.
                                    color = domainInk(EntryDomain.SOCIAL),
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
                                    tint = dangerInk()
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
                                // The same floor as the onboarding: the rule has to
                                // hold everywhere the name is edited, or a nickname
                                // shortened here comes back in a form the emulator
                                // refuses.
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
                    // Show rather than describe. "Your photo and nickname are visible
                    // to your friends" asked the player to imagine the result; the row
                    // below is the result, drawn as the friends list draws it.
                    // pourquoi : docs/decisions/reglages-ecran.md § The pages' images come from the device, not from a stock library
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
 * The player as others see them, without their photo. That is this preview's whole
 * point, and it nearly was its flaw: the first version showed the avatar from the local
 * file, giving exactly the opposite of what the sentence below explains.
 * pourquoi : docs/decisions/reglages-ecran.md § The pages' images come from the device, not from a stock library
 */
@Composable
private fun SeenByOthers(name: String) {
    val dark = LocalEmufiiDarkTheme.current
    // The online green, taken from the theme: Good leans teal.
    val online = if (dark) GoodDark else GoodLight
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
                color = online
            )
        }
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(online)
        )
    }
}
