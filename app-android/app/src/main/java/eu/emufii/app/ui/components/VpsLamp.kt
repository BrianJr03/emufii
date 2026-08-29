package eu.emufii.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.emufii.app.R
import eu.emufii.app.secondscreen.VpsState
import eu.emufii.app.secondscreen.VpsStatus
import eu.emufii.app.ui.theme.ErrorDark
import eu.emufii.app.ui.theme.ErrorLight
import eu.emufii.app.ui.theme.GoodDark
import eu.emufii.app.ui.theme.GoodLight
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme

/**
 * La lumiere de service : un point allume et deux mots.
 *
 * Sa propre couleur, jamais l'accent de l'app.
 *
 * Sortie du panneau arriere le 2026-08-28 pour vivre ici, parce qu'elle est
 * demandee sur l'ecran principal aussi, et que le panneau ne delegue rien : les
 * deux la dessinent, a partir du meme [VpsStatus].
 * pourquoi : docs/decisions/second-ecran.md § La lumière de service a sa propre couleur
 */
@Composable
fun VpsLamp(modifier: Modifier = Modifier, dotSize: Dp = 15.dp) {
    // Le sondage vit avec la lampe : elle est le seul consommateur de cet etat,
    // et [VpsStatus.keepPolling] garantit une boucle unique quand les deux
    // ecrans la dessinent ensemble.
    LaunchedEffect(Unit) { VpsStatus.keepPolling() }

    val state by VpsStatus.state.collectAsState()
    val dark = LocalEmufiiDarkTheme.current

    val tone = when (state) {
        VpsState.ONLINE -> if (dark) GoodDark else GoodLight
        VpsState.OFFLINE -> if (dark) ErrorDark else ErrorLight
        // Gris tant que rien n'est su. Ecrire « en panne » parce qu'un handheld
        // est dans un tunnel accuserait notre machine du train.
        VpsState.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                // Une lampe allumee, pas un rond imprime : elle porte sa propre
                // lueur comme les plaques portent leur ombre.
                .shadow(
                    elevation = if (state == VpsState.UNKNOWN) 0.dp else 12.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = tone,
                    spotColor = tone
                )
                .clip(CircleShape)
                .background(tone)
        )
        Column {
            Text(
                stringResource(R.string.panel_vps),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(
                    when (state) {
                        VpsState.ONLINE -> R.string.panel_vps_online
                        VpsState.OFFLINE -> R.string.panel_vps_offline
                        VpsState.UNKNOWN -> R.string.panel_vps_unknown
                    }
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
