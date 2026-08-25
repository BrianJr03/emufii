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
 * Ce qui regle l'app elle-meme : la langue, les alertes, le panneau arriere.
 *
 * Les trois blocs qui restaient dans « Application » une fois les emulateurs
 * partis chez eux. Ils n'ont rien en commun sinon de ne dependre d'aucune
 * console, et c'est exactement ce que dit le titre de la page.
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
                    // Plus serre que l'ecart ordinaire du bloc : trois choix
                    // d'une meme liste sont un seul objet, et l'ecart qui separe
                    // deux sujets les faisait flotter.
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
 * Les alertes. Leur ligne d'etat porte ce que personne ne devinerait : hors
 * boutique, il n'y a pas de service de notification, donc une alerte peut
 * arriver avec un quart d'heure de retard et c'est normal.
 * pourquoi : docs/decisions/reglages-ecran.md § Les lignes d'état, et ce que personne ne devinerait
 */
@Composable
private fun NotificationsBlock(
    friends: Boolean,
    updates: Boolean,
    onSetFriends: (Boolean) -> Unit,
    onSetUpdates: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    // Demande a la composition plutot que retenu : le joueur peut partir dans
    // les reglages d'Android et revenir, et une reponse en cache montrerait
    // encore le refus qu'il vient de lever.
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
        // Deux interrupteurs, et non quatre boutons dont le libelle change.
        // Un bouton « Amis coupes » ne dit pas s'il decrit l'etat ou l'action
        // qu'il declenche : c'est la question qu'on se pose une demi-seconde
        // avant de le presser, et un interrupteur ne la pose jamais.
        // pourquoi : docs/decisions/reglages-ecran.md § Un réglage qui n'a que deux états est un interrupteur
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
        // Le seul cas ou un interrupteur d'ici ne peut rien : c'est Android qui
        // refuse, et le remede est a trois pressions dans un ecran que personne
        // ne trouve par hasard.
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
 * Le second ecran. La ligne d'etat est ce qui lui vaut sa place : sans elle,
 * c'est une promesse que le joueur ne peut pas verifier — il l'active, rien ne
 * se passe, et il ne peut pas savoir si la fonction est cassee ou si son
 * appareil n'a qu'un ecran.
 * pourquoi : docs/decisions/reglages-ecran.md § Les lignes d'état, et ce que personne ne devinerait
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
            // L'appareil trouve, ou son absence, sous l'interrupteur : c'est ce
            // que le joueur doit lire pour savoir si activer sert a quelque
            // chose, et le lire au moment ou il actionne.
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
