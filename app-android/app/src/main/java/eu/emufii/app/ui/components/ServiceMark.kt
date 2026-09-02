package eu.emufii.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.emufii.app.R

/**
 * SteamGridDB's mark, shown at the two places that ask for its key: the onboarding step
 * and the settings card.
 *
 * Nominative use: the logo names the service, neither modified nor recoloured, and nothing
 * here suggests SteamGridDB endorses Emufii. Hence the original dark background kept inside
 * its own pill rather than being cut out to match the theme.
 */
@Composable
fun SteamGridDbMark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.steamgriddb_logo),
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(11.dp))
        )
        Column {
            Text(
                stringResource(R.string.artwork_service_name),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.artwork_service_host),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
