package eu.emufii.app.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.emufii.app.R
import eu.emufii.app.notify.Notifications
import eu.emufii.app.secondscreen.rememberPresentationDisplay
import eu.emufii.app.settings.AppLanguage
import eu.emufii.app.settings.SettingsStore
import eu.emufii.app.ui.components.DetailActions
import eu.emufii.app.ui.components.DetailNote
import eu.emufii.app.ui.components.DetailStatus
import eu.emufii.app.ui.components.DetailTone
import eu.emufii.app.ui.components.GhostButton
import eu.emufii.app.ui.components.SwitchRow

/**
 * What sets the app itself: the language, the alerts, the rear panel. The three blocks
 * left under Application once the emulators went home. They have nothing in common but
 * depending on no console.
 */
@Composable
internal fun GeneralPage(
    settingsStore: SettingsStore,
    language: AppLanguage,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notifyFriends by settingsStore.notifyFriends.collectAsState()
    val notifyUpdates by settingsStore.notifyUpdates.collectAsState()
    val secondScreenOn by settingsStore.secondScreen.collectAsState()

    SettingsPage(
        title = stringResource(R.string.settings_page_general),
        onBack = onBack,
        modifier = modifier
    ) {
        SettingsColumns(
            {
                SettingsBlock(
                    title = stringResource(R.string.settings_language),
                    state = BlockState(DetailTone.GOOD, stringResource(language.labelRes))
                ) {
                    // Tighter than the block's ordinary gap: three choices from one
                    // list are a single object, and the gap that separates two subjects
                    // made them float.
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppLanguage.entries.forEachIndexed { index, option ->
                        ChoiceRow(
                            label = stringResource(option.labelRes),
                            selected = option == language,
                            onClick = { settingsStore.setLanguage(option) },
                            entry = index == 0
                        )
                    }
                    }
                }
            },
            {
                NotificationsBlock(
                    friends = notifyFriends,
                    updates = notifyUpdates,
                    onSetFriends = settingsStore::setNotifyFriends,
                    onSetUpdates = settingsStore::setNotifyUpdates,
                )
            },
            {
                SecondScreenBlock(
                    enabled = secondScreenOn,
                    onSetEnabled = settingsStore::setSecondScreen,
                )
            },
        )
    }
}

/**
 * The alerts. Their status line carries what nobody would guess: off-store there is no
 * notification service, so an alert can arrive a quarter of an hour late, and that is
 * normal.
 * pourquoi : docs/decisions/reglages-ecran.md § The status lines, and what nobody would guess
 */
@Composable
private fun NotificationsBlock(
    friends: Boolean,
    updates: Boolean,
    onSetFriends: (Boolean) -> Unit,
    onSetUpdates: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    // Asked at composition rather than remembered: the player can leave for Android's
    // settings and come back, and a cached answer would still show the refusal they
    // just lifted.
    val allowed = Notifications.allowed(context)

    SettingsBlock(
        title = stringResource(R.string.settings_notifications),
        state = BlockState(
            when {
                !allowed -> DetailTone.WARN
                friends || updates -> DetailTone.GOOD
                else -> DetailTone.BUSY
            },
            stringResource(
                when {
                    !allowed -> R.string.settings_notify_blocked
                    friends || updates -> R.string.settings_value_autofill_on
                    else -> R.string.settings_notify_silent
                }
            )
        )
    ) {
        // Two switches, not four buttons with changing labels. A button reading
        // "Friends off" does not say whether it describes the state or the action it
        // triggers, which is the question asked half a second before pressing it.
        // pourquoi : docs/decisions/reglages-ecran.md § A setting with only two states is a switch
        SwitchRow(
            label = stringResource(R.string.settings_notify_friends),
            checked = friends,
            onCheckedChange = onSetFriends
        )
        SwitchRow(
            label = stringResource(R.string.settings_notify_updates),
            checked = updates,
            onCheckedChange = onSetUpdates
        )
        DetailNote(stringResource(R.string.settings_notifications_note))
        // The one case a switch here can do nothing about: Android is refusing, and the
        // remedy is three presses into a screen nobody finds by accident.
        if (!allowed) {
            BlockNotice(stringResource(R.string.settings_notify_blocked))
            DetailActions {
                GhostButton(
                    label = stringResource(R.string.settings_notify_open_android),
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            )
                        }
                    },
                    fillWidth = true
                )
            }
        }
    }
}

/**
 * The second screen. The status line is what earns its place: without it this is a
 * promise the player cannot check, since they turn it on, nothing happens, and they
 * cannot tell a broken feature from a one-screen device.
 * pourquoi : docs/decisions/reglages-ecran.md § The status lines, and what nobody would guess
 */
@Composable
private fun SecondScreenBlock(
    enabled: Boolean,
    onSetEnabled: (Boolean) -> Unit,
) {
    val display by rememberPresentationDisplay()
    val panel = display
    SettingsBlock(
        title = stringResource(R.string.settings_second_screen),
        state = BlockState(
            when {
                panel == null -> DetailTone.WARN
                enabled -> DetailTone.GOOD
                else -> DetailTone.BUSY
            },
            stringResource(
                if (enabled) R.string.settings_value_autofill_on
                else R.string.settings_value_autofill_off
            )
        )
    ) {
        SwitchRow(
            label = stringResource(R.string.settings_second_screen_switch),
            checked = enabled,
            onCheckedChange = onSetEnabled,
            // The display found, or its absence, under the switch: that is what the
            // player has to read to know whether turning it on does anything, and to
            // read it as they do.
            note = panel?.name ?: stringResource(R.string.settings_second_screen_absent)
        )
        DetailNote(stringResource(R.string.settings_second_screen_note))
    }
}

internal val AppLanguage.labelRes: Int
    get() = when (this) {
        AppLanguage.SYSTEM -> R.string.settings_language_system
        AppLanguage.FRENCH -> R.string.settings_language_fr
        AppLanguage.ENGLISH -> R.string.settings_language_en
    }
