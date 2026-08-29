package eu.emufii.app.ui.screens.settings

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.emufii.app.library.Console
import eu.emufii.app.library.Rom
import eu.emufii.app.library.emulatorInfo
import eu.emufii.app.ui.components.CardBounds
import eu.emufii.app.ui.components.ChevronRight
import eu.emufii.app.ui.components.LocalCardBounds
import eu.emufii.app.ui.components.cardSliceFill
import eu.emufii.app.ui.components.RomArtwork
import eu.emufii.app.ui.components.DetailTone
import eu.emufii.app.ui.components.EmufiiScaffold
import eu.emufii.app.ui.components.SoftCard
import eu.emufii.app.ui.components.padEntry
import eu.emufii.app.ui.controlRing
import eu.emufii.app.ui.theme.ArtworkShape
import eu.emufii.app.ui.theme.CardShape
import eu.emufii.app.ui.theme.Coral
import eu.emufii.app.ui.theme.ErrorDark
import eu.emufii.app.ui.theme.ErrorLight
import eu.emufii.app.ui.theme.GoodDark
import eu.emufii.app.ui.theme.GoodLight
import eu.emufii.app.ui.theme.InfoDark
import eu.emufii.app.ui.theme.InfoLight
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.LocalEmufiiOledTheme
import eu.emufii.app.ui.theme.Teal
import eu.emufii.app.ui.theme.WarnDark
import eu.emufii.app.ui.theme.WarnLight
import eu.emufii.app.ui.theme.plateColors
import eu.emufii.app.ui.components.StateBead
import eu.emufii.app.ui.theme.socket
import eu.emufii.app.ui.tap

/**
 * Les briques communes aux reglages : la coquille d'une page, un bloc de
 * contenu, une entree du hub, une pastille d'etat, un choix.
 *
 * Un seul endroit qui dise a quoi ressemble une page, sinon les huit divergent.
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

/**
 * Le rouge des gestes qu'on ne rattrape pas : l'erreur du theme, corail-tiree,
 * jamais un hex en dur. Coupe light ; la coupe dark se lit via [dangerInk].
 * pourquoi : docs/decisions/theme-duotone-shelves.md § Sémantique
 */
internal val DANGER = ErrorLight

/** La coupe theme-aware du meme rouge, pour les ecrans qui la dessinent. */
@Composable
internal fun dangerInk(): Color = if (LocalEmufiiDarkTheme.current) ErrorDark else ErrorLight

/**
 * Ce qu'une entree du hub appartient : systeme (turquoise) ou social (corail).
 * pourquoi : docs/decisions/theme-duotone-shelves.md § Réglages
 */
internal enum class EntryDomain { SYSTEM, SOCIAL }

/** La coupe lisible de l'axe sur la plaque : deep sur clair, dark bright sur sombre. */
@Composable
internal fun domainInk(domain: EntryDomain): Color {
    val dark = LocalEmufiiDarkTheme.current
    return when (domain) {
        EntryDomain.SYSTEM -> if (dark) Teal.darkBright else Teal.deep
        EntryDomain.SOCIAL -> if (dark) Coral.darkBright else Coral.deep
    }
}

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
    /**
     * Ce qui se pose au bout du titre : l'etat d'une page qui n'a qu'un sujet.
     * pourquoi : docs/decisions/reglages-ecran.md § Ce qui se pose au bout du titre d'une page
     */
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    EmufiiScaffold(title = title, onBack = onBack, trailing = trailing, modifier = modifier) { topPadding ->
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
 * Distribues en alternance et jamais coupes au milieu : un bloc appartient a une
 * colonne entiere, sinon son etat et ses boutons finissent de part et d'autre de
 * la gouttiere.
 * pourquoi : docs/decisions/reglages-ecran.md § Deux colonnes, une fois l'accordéon parti
 */
@Composable
internal fun SettingsColumns(vararg blocks: @Composable () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < TWO_COLUMN_FROM || blocks.size < 2) {
            // Sur une colonne, deux blocs ne sont plus cote a cote : les
            // appairer n'alignerait plus rien et ne ferait qu'ajouter du vide
            // sous le plus court.
            CompositionLocalProvider(LocalBlocksArePaired provides false) {
                Column(
                    modifier = Modifier.widthIn(max = ONE_COLUMN_MAX).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) { blocks.forEach { it() } }
            }
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

/**
 * Vrai quand les blocs sont dresses en deux colonnes, donc quand deux d'entre
 * eux se regardent en travers de la gouttiere. Faux sur une colonne.
 */
internal val LocalBlocksArePaired = compositionLocalOf { true }

/**
 * Deux blocs de colonnes differentes qui doivent finir a la meme hauteur.
 *
 * Ils vivent dans deux `Column` distinctes : rien ne les mesure ensemble, donc
 * ils se le disent. Le **maximum**, jamais la hauteur de l'un des deux.
 * pourquoi : docs/decisions/reglages-ecran.md § Aligner deux colonnes demande de mesurer, pas d'intrinsèque
 */
@Stable
internal class BlockHeights {
    var tallestPx by mutableIntStateOf(0)
        private set

    fun offer(heightPx: Int) {
        if (heightPx > tallestPx) tallestPx = heightPx
    }
}

@Composable
internal fun rememberBlockHeights(): BlockHeights = remember { BlockHeights() }

/**
 * Pose un bloc dans un groupe de hauteur egale. A mettre sur le `modifier` du
 * bloc, jamais dans son contenu : c'est la carte qui s'aligne, pas son texte.
 */
@Composable
internal fun Modifier.sameHeightAs(group: BlockHeights): Modifier {
    if (!LocalBlocksArePaired.current) return this
    val floor = with(LocalDensity.current) { group.tallestPx.toDp() }
    return this
        .heightIn(min = floor)
        // Apres `heightIn` : ce qui remonte est alors la hauteur tenue, qui est
        // deja le maximum du groupe — le plus court ne fait donc jamais grandir
        // le plancher, et la mesure se stabilise au premier passage.
        .onSizeChanged { group.offer(it.height) }
}

/** L'etat d'un bloc, tel que son en-tete l'affiche. */
internal data class BlockState(val tone: DetailTone, val label: String)

/**
 * Un bloc d'une page : un en-tete qui porte le nom **et l'etat**, puis le
 * contenu, puis l'explication — et seulement tant qu'elle apprend quelque chose.
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
     * Vrai quand le bloc doit occuper toute la hauteur qu'on lui donne : son
     * en-tete en haut, ses actions en bas.
     * pourquoi : docs/decisions/reglages-ecran.md § Deux colonnes, une fois l'accordéon parti
     */
    spread: Boolean = false,
    /**
     * Ce qui se colle au **pied** du bloc quand [spread] est vrai : les
     * actions, typiquement. Sans ce slot, repartir le contenu ecartait aussi le
     * texte de son titre, et le bloc avait un trou au milieu.
     */
    footer: (@Composable () -> Unit)? = null,
    /**
     * Non nul quand le bloc se replie. Reserve a ce qu'on regle une fois.
     * pourquoi : docs/decisions/reglages-ecran.md § Le repli est réservé à ce qu'on règle une fois
     */
    onToggleExpanded: (() -> Unit)? = null,
    /** Vrai quand le bloc repliable est ouvert. Ignore sans [onToggleExpanded]. */
    expanded: Boolean = true,
    content: @Composable () -> Unit
) {
    var bounds by remember { mutableStateOf(CardBounds(0f, 0f)) }
    SoftCard(modifier = modifier, onClick = onToggleExpanded) {
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
                    // Le repli change la hauteur de la carte : sans ca, la
                    // colonne d'a cote saute d'un coup au lieu de suivre.
                    .then(if (onToggleExpanded != null) Modifier.animateContentSize() else Modifier)
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
                        // **Sans poids, et c'est le point.** La pastille en
                        // portait un — `weight(1f, fill = false)` — pour qu'une
                        // etiquette longue ne laisse pas zero pixel au titre.
                        // Mais deux enfants ponderes se partagent la place
                        // restante **a parts egales** : la pastille se voyait
                        // allouer la moitie de la rangee et commencait donc au
                        // milieu de la carte, loin du bord droit ou elle doit
                        // etre. Le `fill = false` ne la retrecissait qu'apres
                        // coup, sans lui rendre sa place.
                        //
                        // Non ponderee, elle est mesuree la premiere, a sa
                        // largeur propre, et le titre prend tout le reste. La
                        // borne de largeur remplace le poids pour la protection
                        // qu'il apportait : une etiquette de deux mots tient
                        // largement dedans, et au-dela l'ellipse de [StatePill]
                        // fait le reste.
                        // pourquoi : docs/decisions/reglages-ecran.md § Une pastille d'état porte deux mots, jamais une phrase
                        state?.let {
                            Box(modifier = Modifier.widthIn(max = STATE_PILL_MAX)) {
                                StatePill(it.tone, it.label)
                            }
                        }
                        if (onToggleExpanded != null) {
                            // Vers le bas ferme, vers le haut ouvert : le
                            // chevron montre ou va le contenu, pas ou il est.
                            val turn by animateFloatAsState(
                                if (expanded) -90f else 90f,
                                label = "block-chevron"
                            )
                            ChevronRight(
                                size = 18.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.rotate(turn)
                            )
                        }
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
 * Une explication qui se donne en etapes, et jamais en paragraphe : quatre
 * phrases techniques d'affilee ne se lisent pas, elles se sautent.
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
 * Sans creux autour.
 * pourquoi : docs/decisions/reglages-ecran.md § Un fait de bloc n'a pas de creux autour
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
 * Ce qui a echoue. Rouge, et court : le rouge coque n'apparait que deux fois
 * dans toute l'app, et c'est pour ca qu'il se lit. Le reste passe par [BlockNotice].
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
 * La chose que le joueur doit savoir alors que tout va bien : un creux, la perle
 * d'avertissement, et l'encre ordinaire.
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
 * Chacune est sa **propre plaque** et non une rangee dans une carte commune, et
 * elles sont rangees en tableau plutot qu'en liste.
 * pourquoi : docs/decisions/reglages-ecran.md § Une entrée du hub est une plaque, pas une rangée
 * pourquoi : docs/decisions/reglages-ecran.md § Le hub est une grille, et le panneau montre la case visée
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
    /** Le domaine decide la teinte de l'encoche : turquoise systeme, corail social. */
    domain: EntryDomain = EntryDomain.SYSTEM,
    leading: (@Composable () -> Unit)? = null,
    /**
     * Appelee quand le curseur arrive sur l'entree ou la quitte. C'est par la
     * que le hub dit au second ecran quelle case est visee ; nulle part
     * ailleurs le focus ne traverse les deux fenetres.
     * pourquoi : docs/decisions/reglages-ecran.md § Le hub est une grille, et le panneau montre la case visée
     */
    onFocused: ((Boolean) -> Unit)? = null,
) {
    SoftCard(
        onClick = onOpen,
        modifier = modifier
            .then(if (entry) Modifier.padEntry() else Modifier)
            .then(
                if (onFocused != null) Modifier.onFocusEvent { onFocused(it.hasFocus) }
                else Modifier
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = ROW_INSET, vertical = 11.dp)
        ) {
            if (leading != null) leading()
            else if (icon != null) IconSocket(icon, domain)
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
 * La marque d'une page, dans son encoche ronde teintee par domaine : une icone
 * posee a nu flotte, et sept icones flottantes se lisent comme de la decoration.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § Réglages
 */
@Composable
private fun IconSocket(icon: @Composable (Color) -> Unit, domain: EntryDomain) {
    val ink = domainInk(domain)
    val axis: Color = if (domain == EntryDomain.SOCIAL) Coral.bright else Teal.bright
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(axis.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) { icon(ink) }
}

/**
 * Ce qu'une pastille d'etat peut prendre de la rangee du titre. Deux mots y
 * tiennent avec de la marge ; c'est un garde-fou, pas une mesure.
 */
private val STATE_PILL_MAX = 190.dp

/**
 * L'etat d'une page, en une pastille : le meme vocabulaire que la perle de
 * [eu.emufii.app.ui.components.DetailStatus], plus le mot qu'elle ne porte pas.
 * pourquoi : docs/decisions/reglages-ecran.md § La pastille du hub reprend la perle, elle n'en invente pas une seconde
 */
@Composable
internal fun StatePill(tone: DetailTone, label: String) {
    val dark = LocalEmufiiDarkTheme.current
    val ink = when (tone) {
        DetailTone.GOOD -> if (dark) GoodDark else GoodLight
        DetailTone.BUSY -> if (dark) InfoDark else InfoLight
        DetailTone.WARN -> if (dark) WarnDark else WarnLight
        DetailTone.BAD -> if (dark) ErrorDark else ErrorLight
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
            maxLines = 1,
            // Voir SessionFinderScreen : la coupe par defaut tranche le glyphe,
            // l'ellipse dit qu'il manque quelque chose.
            overflow = TextOverflow.Ellipsis
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
            .tap(onClick = onClick)
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
    val danger = dangerInk()
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
                color = danger
            )
            Spacer(Modifier.weight(1f))
            ChevronRight(size = 18.dp, color = danger.copy(alpha = 0.6f))
        }
    }
}

/**
 * La marque d'un bloc d'emulateur : l'icone de l'application installee, qui dit
 * si elle est la — ce que son nom en titre ne dit pas.
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
 * Un echantillon de jaquettes reelles, pris dans la bibliotheque du joueur : il
 * voit **ce que sa grille affiche**, la ou il en change la source.
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
