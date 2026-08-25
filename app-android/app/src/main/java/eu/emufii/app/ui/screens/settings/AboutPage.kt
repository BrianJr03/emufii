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
 * Ce qu'est cette app, et ou la rejoindre. Deux cartes, cote a cote, et rien
 * d'autre.
 *
 * Une troisieme carte montrait les sept consoles servies. Elle a ete ecrite
 * puis retiree : la page « A propos » se visite pour connaitre une version ou
 * trouver un lien, et la liste des consoles est deja partout ailleurs — dans la
 * grille, dans la page Consoles, dans les tuiles de la bibliotheque. Une image
 * n'a sa place que si elle repond a la question que sa page pose.
 * pourquoi : docs/decisions/reglages-ecran.md § Les deux liens sortants, et leur ordre
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
                // Les deux cartes partent du meme bord haut et finissent au
                // meme niveau : on mesure les deux, et on impose le plus grand
                // des deux comme **minimum** aux deux.
                //
                // Deux pieges payes, dans cet ordre.
                // `Modifier.height(IntrinsicSize.Min)` etait la reponse
                // evidente et elle est fausse : la hauteur intrinseque minimale
                // d'un paragraphe est celle qu'il ferait a la largeur de son mot
                // le plus long, donc enorme — la rangee prenait deux ecrans et
                // demi de haut. Puis imposer la hauteur mesuree a gauche comme
                // **taille** a droite a ecrase le dernier bouton de la carte de
                // droite en un trait de trois pixels : une hauteur imposee
                // decoupe, un minimum laisse grandir.
                // pourquoi : docs/decisions/reglages-ecran.md § Aligner deux colonnes demande de mesurer, pas d'intrinsèque
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

/** Ce qu'est l'app, sa version, sa licence. */
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

/** Les deux seuls liens sortants de l'app. */
@Composable
private fun JoinBlock(open: (String) -> Unit, modifier: Modifier = Modifier) {
    SettingsBlock(
        title = stringResource(R.string.settings_about_join),
        modifier = modifier,
        spread = true,
        footer = {
            DetailActions {
                // Le Discord d'abord, et rempli : c'est le seul des deux qui
                // rende quelque chose au joueur. Le soutien est propose, jamais
                // mis en avant — une app qui demande de l'argent plus fort
                // qu'elle n'offre de l'aide se lit comme un guichet.
                // pourquoi : docs/decisions/reglages-ecran.md § Les deux liens sortants, et leur ordre
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
        // Aucun texte : deux boutons dont le libelle porte deja leur
        // destination n'ont rien a faire expliquer. Le paragraphe qui vivait
        // ici disait ce que le Discord sert et ou va l'argent — vrai, et
        // personne ne le lisait avant de presser le bouton qu'il coiffait.
        // pourquoi : docs/decisions/reglages-ecran.md § Les deux liens sortants, et leur ordre
    }
}

/** Le salon des joueurs. */
private const val DISCORD_URL = "https://discord.gg/tvWcb28vBZ"

/** Le pot commun. Jamais dans un dialogue, jamais au lancement : ici et nulle part ailleurs. */
private const val KOFI_URL = "https://ko-fi.com/emufii"

/**
 * La marque d'un service exterieur, a sa propre couleur.
 *
 * Non teintee par l'accent, et c'est voulu : elle designe un ailleurs, donc
 * c'est du contenu, comme l'icone d'une console ou une jaquette. La regle du
 * seul accent porte sur le chrome, pas sur ce que le chrome montre.
 * pourquoi : docs/decisions/direction-visuelle.md § Trois sols, un accent, et rien d'autre n'a de teinte
 */
@Composable
private fun BrandMark(res: Int) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = Modifier.size(20.dp)
    )
}
