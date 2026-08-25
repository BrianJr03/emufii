package eu.emufii.app.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.emufii.app.library.Console
import eu.emufii.app.library.Rom
import eu.emufii.app.library.emulatorInfo
import eu.emufii.app.ui.components.ChevronRight
import eu.emufii.app.ui.components.RomArtwork
import eu.emufii.app.ui.components.DetailTone
import eu.emufii.app.ui.components.EmufiiScaffold
import eu.emufii.app.ui.components.SoftCard
import eu.emufii.app.ui.components.padEntry
import eu.emufii.app.ui.controlRing
import eu.emufii.app.ui.theme.ArtworkShape
import eu.emufii.app.ui.theme.CardShape
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.LocalEmufiiOledTheme
import eu.emufii.app.ui.theme.ShellRed
import eu.emufii.app.ui.theme.plateColors
import eu.emufii.app.ui.components.StateBead
import eu.emufii.app.ui.theme.socket

/**
 * Les briques communes aux reglages : la coquille d'une page, un bloc de
 * contenu, une entree du hub, une pastille d'etat, un choix.
 *
 * Ce fichier existe parce que les reglages ne sont plus un ecran mais huit :
 * sans un seul endroit qui dise a quoi ressemble une page de reglages, les
 * huit divergeraient au premier ajout.
 * pourquoi : docs/decisions/reglages-ecran.md § Un hub et sept pages, plus un accordéon
 */

/**
 * La largeur au-dela de laquelle une rangee de reglage cesse de se lire comme
 * une seule chose.
 * pourquoi : docs/decisions/reglages-ecran.md § Les trois constantes de forme d'une rangée
 */
internal val SETTINGS_MAX_WIDTH = 620.dp

/** Le retrait du texte par rapport au bord de sa carte. */
internal val ROW_INSET = 18.dp

/** La forme d'un choix pose dans un bloc. */
internal val ROW_SHAPE = RoundedCornerShape(14.dp)

/** Le rouge des gestes qu'on ne rattrape pas. */
internal val DANGER = ShellRed

/**
 * La largeur en dessous de laquelle une page reste sur une colonne.
 * pourquoi : docs/decisions/reglages-ecran.md § Deux colonnes, une fois l'accordéon parti
 */
private val TWO_COLUMN_FROM = 700.dp

/** Ce que la colonne unique ne depasse pas. */
private val ONE_COLUMN_MAX = SETTINGS_MAX_WIDTH

/** Ce que les deux colonnes ensemble ne depassent pas. */
private val TWO_COLUMN_MAX = 980.dp

/**
 * Une page de reglages : la coquille, et une colonne bornee — ou deux quand
 * l'ecran en porte deux.
 * pourquoi : docs/decisions/reglages-ecran.md § Deux colonnes, une fois l'accordéon parti
 */
@Composable
internal fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    EmufiiScaffold(title = title, onBack = onBack, modifier = modifier) { topPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = topPadding, bottom = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Borne a la largeur des **deux** colonnes, et non a celle d'une
            // seule : c'est [SettingsColumns] qui decide combien il y en a, et
            // il ne peut le decider que s'il voit la largeur reelle de la page.
            // Une page qui veut rester etroite se borne elle-meme.
            // pourquoi : docs/decisions/reglages-ecran.md § Deux colonnes, une fois l'accordéon parti
            Column(
                modifier = Modifier.widthIn(max = TWO_COLUMN_MAX).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) { content() }
        }
    }
}

/**
 * Les blocs d'une page, en deux colonnes des que l'ecran les porte.
 *
 * La colonne unique etait la bonne reponse tant que les rangees se depliaient :
 * une hauteur qui change rouvre un trou entre deux colonnes a chaque geste.
 * Une page ne change plus de hauteur, et le paysage de la Thor offre 850 dp
 * dont une seule colonne bornee a 620 n'utilisait que les trois quarts, en
 * faisant defiler trois ecrans la ou il en faut un.
 *
 * Les blocs sont distribues en alternance et non coupes au milieu : un bloc
 * appartient a une colonne entiere, sinon son etat et ses boutons finissent de
 * part et d'autre de la gouttiere.
 * pourquoi : docs/decisions/reglages-ecran.md § Deux colonnes, une fois l'accordéon parti
 */
@Composable
internal fun SettingsColumns(vararg blocks: @Composable () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < TWO_COLUMN_FROM || blocks.size < 2) {
            Column(
                modifier = Modifier.widthIn(max = ONE_COLUMN_MAX).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) { blocks.forEach { it() } }
        } else {
            Row(
                modifier = Modifier.widthIn(max = TWO_COLUMN_MAX).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                listOf(0, 1).forEach { side ->
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        blocks.forEachIndexed { index, block ->
                            if (index % 2 == side) block()
                        }
                    }
                }
            }
        }
    }
}

/** L'etat d'un bloc, tel que son en-tete l'affiche. */
internal data class BlockState(val tone: DetailTone, val label: String)

/**
 * Un bloc d'une page : un en-tete qui porte le nom **et l'etat**, puis le
 * contenu.
 *
 * L'ordre est le renversement de celui de l'accordeon, et c'est le coeur de
 * cette page. Une rangee depliee commencait par un paragraphe d'explication,
 * puis les boutons, puis l'etat tout en bas : on avait demande a ouvrir, donc
 * on voulait d'abord apprendre. Sur une page, tout est deja ouvert, et ce que
 * le joueur vient verifier c'est **ou ca en est** — quatre lignes de methode
 * posees devant lui sont un mur a traverser pour atteindre un mot.
 *
 * Donc : le nom et l'etat en en-tete, ce qu'on peut faire ensuite, et
 * l'explication en dernier — et seulement tant qu'elle apprend quelque chose.
 * pourquoi : docs/decisions/reglages-ecran.md § Sur une page, l'état passe devant l'explication
 */
@Composable
internal fun SettingsBlock(
    /**
     * Nul quand la page ne porte qu'un bloc : repeter le titre de la page dans
     * l'en-tete du seul bloc qu'elle contient le dit deux fois a 40 dp d'ecart.
     */
    title: String? = null,
    modifier: Modifier = Modifier,
    state: BlockState? = null,
    /** La marque du bloc : l'icone de l'emulateur concerne, quand il y en a un. */
    mark: (@Composable () -> Unit)? = null,
    /**
     * Vrai quand le bloc doit occuper toute la hauteur qu'on lui donne et
     * repartir son contenu dedans : son en-tete en haut, ses actions en bas.
     * Sert a aligner le pied d'une colonne d'un seul bloc sur celui d'une
     * colonne qui en compte deux.
     * pourquoi : docs/decisions/reglages-ecran.md § Deux colonnes, une fois l'accordéon parti
     */
    spread: Boolean = false,
    /**
     * Ce qui se colle au **pied** du bloc quand [spread] est vrai : les
     * actions, typiquement. Sans ce slot, repartir le contenu ecartait aussi le
     * texte de son titre, et le bloc avait un trou au milieu.
     */
    footer: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var bounds by remember { mutableStateOf(CardBounds(0f, 0f)) }
    SoftCard(modifier = modifier) {
        CompositionLocalProvider(LocalCardBounds provides bounds) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned {
                        bounds = CardBounds(
                            top = it.positionInRoot().y,
                            height = it.size.height.toFloat()
                        )
                    }
                    .then(if (spread) Modifier.fillMaxHeight() else Modifier)
                    .padding(ROW_INSET),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (title != null || state != null || mark != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        mark?.invoke()
                        Text(
                            title.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        state?.let { StatePill(it.tone, it.label) }
                    }
                }
                content()
                if (footer != null) {
                    // L'ecart va **la**, entre le contenu et le pied, et nulle
                    // part ailleurs : c'est ce qui aligne le pied d'une colonne
                    // d'un seul bloc sur celui d'une colonne qui en compte
                    // deux, sans decoller le texte de son titre.
                    if (spread) Spacer(Modifier.weight(1f))
                    footer()
                }
            }
        }
    }
}

/**
 * Une explication qui se donne en etapes, et jamais en paragraphe.
 *
 * Quatre phrases techniques d'affilee — le cas de PPSSPP, mesure a quatre
 * lignes pleine largeur — ne se lisent pas, elles se sautent. Les memes faits
 * numerotes se parcourent : on trouve l'etape ou l'on en est, on fait celle
 * d'apres. C'est aussi ce qui force a les ecrire courtes.
 * pourquoi : docs/decisions/reglages-ecran.md § Sur une page, l'état passe devant l'explication
 */
@Composable
internal fun SettingsSteps(vararg steps: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        steps.forEachIndexed { index, text -> SettingsStep(index + 1, text) }
    }
}

/** Une etape numerotee. */
@Composable
private fun SettingsStep(number: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Un fait du bloc : une etiquette a gauche, sa valeur a droite, sur une ligne.
 *
 * Sans le creux de [eu.emufii.app.ui.components.DetailStatus] autour. Le creux
 * dit « voila ce qui est » et le meritait quand l'etat arrivait en bas d'une
 * rangee depliee ; en en-tete de bloc, l'etat est deja dit par la pastille, et
 * un creux de plus par bloc empilait trois niveaux de conteneur.
 */
@Composable
internal fun BlockFact(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Ce qui a echoue. Rouge, et court.
 *
 * Le rouge coque n'apparait que deux fois dans toute l'app, et c'est pour ca
 * qu'il se lit quand il apparait. Une note de six lignes en rouge sous une
 * pastille verte fait relire la pastille pour savoir laquelle des deux ment :
 * ce qui n'est pas un echec passe par [BlockNotice].
 * pourquoi : docs/decisions/reglages-ecran.md § Un avertissement n'est pas une erreur, et ne porte pas le rouge
 */
@Composable
internal fun BlockCaveat(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error
    )
}

/**
 * La chose que le joueur doit savoir, alors que tout va bien.
 *
 * Le cas qui l'a fait naitre : la carte dossier de la PS2. Ce n'est ni une
 * erreur ni un remplacement — c'est la raison pour laquelle aucune sauvegarde
 * du joueur n'a ete clonee, donc la seule chose qu'il doit absolument lire, et
 * ca prend six lignes. En rouge, sous une pastille verte, ces six lignes
 * annulaient la pastille.
 *
 * Dans un creux, avec la perle d'avertissement : le creux dit « voila ce qui
 * est », la perle dit de quel poids, et le texte garde l'encre ordinaire.
 * pourquoi : docs/decisions/reglages-ecran.md § Un avertissement n'est pas une erreur, et ne porte pas le rouge
 */
@Composable
internal fun BlockNotice(text: String) {
    val dark = LocalEmufiiDarkTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .socket(ROW_SHAPE, dark)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        // En haut, pas au centre : une note de plusieurs lignes avec une perle
        // flottant au milieu cesse de se lire comme sa marque.
        verticalAlignment = Alignment.Top
    ) {
        StateBead(DetailTone.WARN, size = 12.dp)
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Une entree du hub : une marque, un nom, ce que la page contient, et l'etat.
 *
 * Chacune est sa **propre plaque** et non une rangee dans une carte commune.
 * C'est ce qui a fait disparaitre la machinerie de coins qui se morphaient et
 * de remplissage opaque tranche dans le degrade de la carte : une plaque est
 * deja opaque, et [SoftCard] pose deja l'anneau du curseur dans sa forme.
 *
 * Et ce n'est **pas** une tuile : la grille de tuiles est la grammaire de la
 * bibliotheque, ou le contenu est la jaquette. Une page de reglages n'a pas de
 * contenu a montrer, elle a des noms a lire, et une rangee se lit a la vitesse
 * ou on cherche un nom.
 * pourquoi : docs/decisions/reglages-ecran.md § Une entrée du hub est une plaque, pas une rangée
 */
@Composable
internal fun SettingsEntry(
    label: String,
    summary: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    /** Vrai pour la premiere entree de la page : la manette y descend, et y remonte. */
    entry: Boolean = false,
    state: EntryState? = null,
    icon: (@Composable (Color) -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    SoftCard(
        onClick = onOpen,
        modifier = modifier.then(if (entry) Modifier.padEntry() else Modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = ROW_INSET, vertical = 11.dp)
        ) {
            if (leading != null) leading()
            else if (icon != null) IconSocket(icon)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            state?.let { StatePill(it.tone, it.label) }
            ChevronRight(size = 18.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Ce qu'une entree du hub dit de sa page sans qu'on l'ouvre. */
internal data class EntryState(val tone: DetailTone, val label: String)

/**
 * La marque d'une page, dans son creux rond.
 *
 * Le creux tient a une chose : une icone posee a nu sur la plaque flotte, et
 * sept icones flottantes alignees se lisent comme de la decoration. Dans un
 * creux, chacune est un objet moule de plus.
 *
 * Elle est a l'encre attenuee et **jamais a l'accent** : l'accent ne veut dire
 * qu'une chose, « c'est ici », et sept marques cyan le lui retireraient.
 * pourquoi : docs/decisions/direction-visuelle.md § Trois sols, un accent, et rien d'autre n'a de teinte
 */
@Composable
private fun IconSocket(icon: @Composable (Color) -> Unit) {
    val dark = LocalEmufiiDarkTheme.current
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(34.dp)
            .socket(CircleShape, dark),
        contentAlignment = Alignment.Center
    ) { icon(ink) }
}

/**
 * L'etat d'une page, en une pastille.
 *
 * Le meme vocabulaire que la perle de [eu.emufii.app.ui.components.DetailStatus]
 * — memes quatre tons, memes quatre glyphes — parce qu'une application n'a le
 * droit de dire « c'est bon » que d'une seule facon. La pastille ajoute le mot
 * que la perle seule ne porte pas, parce qu'une rangee de hub doit se lire sans
 * qu'on l'ouvre.
 * pourquoi : docs/decisions/reglages-ecran.md § La pastille du hub reprend la perle, elle n'en invente pas une seconde
 */
@Composable
internal fun StatePill(tone: DetailTone, label: String) {
    val ink = when (tone) {
        DetailTone.GOOD -> Color(0xFF12A55C)
        DetailTone.BUSY -> Color(0xFF3C82C4)
        DetailTone.WARN -> Color(0xFFC78005)
        DetailTone.BAD -> Color(0xFFEB5D47)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(ink.copy(alpha = 0.14f))
            .border(1.dp, ink.copy(alpha = 0.35f), CircleShape)
            .padding(start = 6.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)
    ) {
        StateBead(tone, size = 11.dp)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = ink,
            maxLines = 1
        )
    }
}

/**
 * Une rangee qui se lit choisie ou non d'un coup d'oeil, sans bouton radio.
 * Le point plein et le fond teinte font le travail ; le radio de Material dans
 * une plaque moulee ressemble a un formulaire.
 */
@Composable
internal fun ChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    /** Vrai quand ce choix est le premier controle de sa page. */
    entry: Boolean = false
) {
    val emphasis by animateFloatAsState(if (selected) 1f else 0f, label = "choice-row")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (entry) Modifier.padEntry() else Modifier)
            .controlRing(ROW_SHAPE)
            // La teinte de selection est translucide par construction, donc a
            // elle seule elle n'a jamais rendu la rangee opaque et la lueur du
            // curseur passait au travers.
            .cardSliceFill(
                ROW_SHAPE,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f * emphasis)
            )
            .clip(ROW_SHAPE)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(
            Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(Color.White))
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Une rangee rouge : le geste qu'on ne fait qu'une fois dans la vie de l'app. */
@Composable
internal fun DangerRow(label: String, onClick: () -> Unit) {
    SoftCard(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ROW_INSET, vertical = 14.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = DANGER
            )
            Spacer(Modifier.weight(1f))
            ChevronRight(size = 18.dp, color = DANGER.copy(alpha = 0.6f))
        }
    }
}

/** Ou se trouve une carte de reglages et quelle hauteur elle fait, en coordonnees fenetre. */
internal data class CardBounds(val top: Float, val height: Float)

/**
 * La carte dans laquelle l'appelant dessine. Coordonnees **racine**, pas celles
 * d'un parent : ce qui en a besoin n'est pas a la meme profondeur.
 * pourquoi : docs/decisions/reglages-ecran.md § Le remplissage opaque existe pour le curseur, pas pour le look
 */
internal val LocalCardBounds = compositionLocalOf { CardBounds(0f, 0f) }

/**
 * Un remplissage opaque qui est, au pixel pres, ce que la carte peignait deja
 * ici. **Il existe pour le curseur, pas pour le look** : une lueur est une
 * ombre, et elle traverse tout ce qui n'est pas opaque.
 * pourquoi : docs/decisions/reglages-ecran.md § Le remplissage opaque existe pour le curseur, pas pour le look
 */
@Composable
internal fun Modifier.cardSliceFill(shape: Shape, tint: Color = Color.Transparent): Modifier {
    val card = LocalCardBounds.current
    val colors = plateColors(
        dark = LocalEmufiiDarkTheme.current,
        oled = LocalEmufiiOledTheme.current
    )
    var top by remember { mutableFloatStateOf(Float.NaN) }
    return this
        .onGloballyPositioned { top = it.positionInRoot().y }
        .background(
            brush = if (card.height <= 0f || top.isNaN()) SolidColor(colors.first())
            else Brush.verticalGradient(
                colors = colors,
                startY = card.top - top,
                endY = card.top - top + card.height
            ),
            shape = shape
        )
        .then(if (tint == Color.Transparent) Modifier else Modifier.background(tint, shape))
}

/**
 * La marque d'un bloc d'emulateur : l'icone de l'application installee.
 *
 * C'est la seule image que ces pages peuvent montrer honnetement, et elle
 * repond a une question que le texte posait mal : « PPSSPP » en titre ne dit
 * pas si PPSSPP est la. Son icone, oui — et le creux vide, quand elle manque,
 * le dit aussi bien.
 * pourquoi : docs/decisions/reglages-ecran.md § Les images des pages viennent de l'appareil, pas d'une banque
 */
@Composable
internal fun EmulatorMark(console: Console, size: Dp = 34.dp) {
    val context = LocalContext.current
    // Demandee une fois : une icone de lanceur est souvent un drawable
    // adaptatif, et en tramer une n'est pas gratuit.
    val info = remember(console) { emulatorInfo(context, console) }
    val dark = LocalEmufiiDarkTheme.current
    Box(
        modifier = Modifier.size(size).socket(ArtworkShape, dark),
        contentAlignment = Alignment.Center
    ) {
        val icon = info.icon
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(ArtworkShape)
            )
        } else {
            // L'abreviation de la console plutot qu'un point d'interrogation :
            // un emulateur absent est le cas ordinaire sur un appareil neuf.
            Text(
                console.shortLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Un echantillon de jaquettes reelles, pris dans la bibliotheque du joueur.
 *
 * Le bloc des icones de jeu parlait de deux sources d'images sans jamais en
 * montrer une seule : le joueur lisait « Cocoon est en vigueur » et devait
 * aller verifier dans la grille. Ici il voit **ce que sa grille affiche**, a
 * l'endroit ou il en change la source. C'est aussi la seule couleur de tout cet
 * ecran, et elle vient du contenu, comme le veut la direction.
 * pourquoi : docs/decisions/reglages-ecran.md § Les images des pages viennent de l'appareil, pas d'une banque
 */
@Composable
internal fun ArtworkStrip(roms: List<Rom>, modifier: Modifier = Modifier) {
    if (roms.isEmpty()) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        roms.forEach { rom ->
            RomArtwork(rom = rom, size = 56.dp)
        }
    }
}
