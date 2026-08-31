package eu.emufii.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.emufii.app.BuildConfig
import eu.emufii.app.R
import eu.emufii.app.ui.components.DetailActions
import eu.emufii.app.ui.components.DetailNote
import eu.emufii.app.ui.components.DetailTone
import eu.emufii.app.ui.components.GhostButton
import eu.emufii.app.ui.components.PrimaryButton
import eu.emufii.app.ui.components.padEntry

/**
 * What the app is, and where to join it. Two cards side by side and nothing else. A
 * third listed the seven consoles served; it was written then removed, since this page
 * is visited for a version or a link, and the console list is everywhere else already.
 * pourquoi : docs/decisions/reglages-ecran.md § The two outbound links, and their order
 */
@Composable
internal fun AboutPage(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val open = { url: String ->
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        Unit
    }

    SettingsPage(
        title = stringResource(R.string.settings_page_about),
        onBack = onBack,
        modifier = modifier
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= 700.dp) {
                // Both cards start at the same top edge and end level: measure both,
                // then impose the taller as a minimum on both.
                // `Modifier.height(IntrinsicSize.Min)` was the obvious answer and is
                // wrong, the minimum intrinsic height being the shorter of the two.
                // pourquoi : docs/decisions/reglages-ecran.md § Aligning two columns means measuring, not intrinsics
                var leftHeight by remember { mutableIntStateOf(0) }
                var rightHeight by remember { mutableIntStateOf(0) }
                val density = LocalDensity.current
                val floor = with(density) { maxOf(leftHeight, rightHeight).toDp() }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .onSizeChanged { leftHeight = it.height }
                    ) { IdentityBlock(modifier = Modifier.heightIn(min = floor)) }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .onSizeChanged { rightHeight = it.height }
                    ) { JoinBlock(open = open, modifier = Modifier.heightIn(min = floor)) }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    IdentityBlock()
                    JoinBlock(open = open)
                }
            }
        }
    }
}

/** What the app is, its version, its licence. */
@Composable
private fun IdentityBlock(modifier: Modifier = Modifier) {
    SettingsBlock(
        title = stringResource(R.string.app_name),
        modifier = modifier,
        state = BlockState(DetailTone.GOOD, BuildConfig.VERSION_NAME)
    ) {
        DetailNote(stringResource(R.string.settings_about_body))
        BlockFact(
            stringResource(R.string.settings_about_fact_build),
            BuildConfig.VERSION_CODE.toString()
        )
        BlockFact(
            stringResource(R.string.settings_about_fact_licence),
            stringResource(R.string.settings_about_licence_value)
        )
    }
}

/** The app's only two outgoing links. */
@Composable
private fun JoinBlock(open: (String) -> Unit, modifier: Modifier = Modifier) {
    SettingsBlock(
        title = stringResource(R.string.settings_about_join),
        modifier = modifier,
        spread = true,
        footer = {
            DetailActions {
                // Discord first, and filled: it is the only one of the two that gives
                // the player anything. Support is offered, never pushed: an app that
                // asks for money louder than it offers help reads as a till.
                // pourquoi : docs/decisions/reglages-ecran.md § The two outbound links, and their order
                PrimaryButton(
                    label = stringResource(R.string.settings_about_discord),
                    onClick = { open(DISCORD_URL) },
                    modifier = Modifier.padEntry().fillMaxWidth(),
                    leading = { BrandMark(R.drawable.ic_discord) }
                )
                GhostButton(
                    label = stringResource(R.string.settings_about_kofi),
                    onClick = { open(KOFI_URL) },
                    fillWidth = true,
                    leading = { BrandMark(R.drawable.ic_kofi) }
                )
            }
        }
    ) {
        // No text: two buttons whose labels already carry their destination explain
        // nothing. The paragraph that lived here said what Discord is for and where the
        // money goes, and nobody read it before pressing the button it capped.
        // pourquoi : docs/decisions/reglages-ecran.md § The two outbound links, and their order
    }
}

/** The players' room. */
private const val DISCORD_URL = "https://discord.gg/tvWcb28vBZ"

/** The tip jar. Never in a dialog, never at launch: here and nowhere else. */
private const val KOFI_URL = "https://ko-fi.com/emufii"

/**
 * An outside service's mark, in its own colour. Untinted by the accent deliberately: it
 * names an elsewhere, so it is content, like a console icon or cover art. The
 * single-accent rule covers the chrome, not what the chrome shows.
 * pourquoi : docs/decisions/direction-visuelle.md § Three floors, one accent, and nothing else has a hue
 */
@Composable
private fun BrandMark(res: Int) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = Modifier.size(20.dp)
    )
}
