package eu.emufii.app.secondscreen

import eu.emufii.app.compat.CompatRating
import eu.emufii.app.library.Console
import eu.emufii.app.library.Rom
import eu.emufii.app.library.RomTags
import eu.emufii.app.meta.GameMeta
import eu.emufii.app.session.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What the second screen is showing, and the one place that decides it.
 *
 * At **process scope**, never inside the Compose tree: the panel's reason to
 * exist is the moment the emulator owns the front display, and a model held in
 * a composition dies with it.
 * pourquoi : docs/decisions/second-ecran.md § L'état du panneau vit à portée de processus, pas dans la composition
 */
object SecondScreen {
    /** Ce que les publieurs de fond ont regle : grille, hub, session. */
    private val _base = MutableStateFlow<SecondScreenModel>(SecondScreenModel.Idle)

    /**
     * Ce que les couches posees par-dessus ont mis, **empilees**.
     *
     * Une vraie pile, depuis le 2026-08-30. C'etait une case unique avec un
     * jeton : le dernier qui ecrivait gagnait, et le proprietaire precedent
     * perdait sa face sans le savoir — son `takeBack` ne reconnaissait plus le
     * jeton et **vidait la case pour tout le monde**. Vu en vrai : la
     * bibliotheque est composee sous le logo de demarrage, sa barre du haut
     * posait sa propre face, et le panneau repassait au jeu survole pendant que
     * l'ecran de face en etait encore a son logo.
     *
     * Empilees, les couches n'ont plus a se connaitre : chacune pose et retire
     * la sienne, la derniere posee est celle qu'on voit, et celle du dessous
     * revient d'elle-meme. C'est ce que le KDoc de cette classe promettait deja.
     * pourquoi : docs/decisions/second-ecran.md § Une pile plutôt qu'une publication de plus
     */
    private val asides = mutableListOf<Pair<Any, SecondScreenModel>>()

    private val _aside = MutableStateFlow<SecondScreenModel?>(null)

    /**
     * La face posee par-dessus, s'il y en a une. Publiee pour distinguer les
     * **deux repos** : celui qu'on laisse et celui qu'on pose.
     * pourquoi : docs/decisions/second-ecran.md § Une pile plutôt qu'une publication de plus
     */
    val aside: StateFlow<SecondScreenModel?> = _aside.asStateFlow()

    /**
     * Le fond, ou ce qui le masque. Recalcule a chaque ecriture et non derive
     * par `combine`, qui demanderait une portee sur `Dispatchers.Main`.
     * pourquoi : docs/decisions/second-ecran.md § Une pile plutôt qu'une publication de plus
     */
    private val _model = MutableStateFlow<SecondScreenModel>(SecondScreenModel.Idle)
    val model: StateFlow<SecondScreenModel> = _model.asStateFlow()

    /** Le sommet de la pile, ou le fond quand elle est vide. */
    private fun refresh() {
        _aside.value = asides.lastOrNull()?.second
        _model.value = _aside.value ?: _base.value
    }

    /**
     * Pose une face par-dessus le fond, et rend un jeton pour la retirer : elle
     * ne retire que la sienne, ou rien.
     * pourquoi : docs/decisions/second-ecran.md § Une pile plutôt qu'une publication de plus
     */
    @Synchronized
    fun putAside(model: SecondScreenModel): Any {
        val token = Any()
        asides += token to model
        refresh()
        return token
    }

    /** Retire la face de [token], ou qu'elle soit dans la pile. */
    @Synchronized
    fun takeBack(token: Any) {
        if (asides.removeAll { it.first === token }) refresh()
    }

    /**
     * Met a jour une face sans changer sa place dans la pile : une couche qui
     * change de contenu ne doit pas repasser devant celles posees depuis.
     */
    @Synchronized
    fun updateAside(token: Any, model: SecondScreenModel) {
        val at = asides.indexOfFirst { it.first === token }
        if (at >= 0) {
            asides[at] = token to model
            refresh()
        }
    }

    /**
     * Which page of the browsing face is showing. Held here because the button
     * that turns it is on the *front* screen, and it resets on a new game.
     * pourquoi : docs/decisions/second-ecran.md § L'état du panneau vit à portée de processus, pas dans la composition
     */
    private val _page = MutableStateFlow(0)
    val page: StateFlow<Int> = _page.asStateFlow()

    fun publish(model: SecondScreenModel) {
        if (!sameGame(_base.value, model)) _page.value = 0
        _base.value = model
        refresh()
    }

    /** The one control the panel has, pressed from the front screen. */
    fun flipPage() {
        if (_base.value is SecondScreenModel.Browsing) _page.value = 1 - _page.value
    }

    /**
     * Les etapes de la session, telles que le panneau peut les presser.
     *
     * Elles voyagent **deja resolues** : la fenetre du panneau a son propre
     * contexte d'affichage.
     * pourquoi : docs/decisions/second-ecran.md § Ce qui voyage au panneau voyage déjà résolu
     */
    private val _steps = MutableStateFlow<List<PanelStep>>(emptyList())
    val steps: StateFlow<List<PanelStep>> = _steps.asStateFlow()

    /**
     * Publie les etapes, ou les retire. Les lambdas appartiennent a une
     * composition : l'ecran qui les pose **doit** les retirer en partant, sinon
     * le panneau garde une session morte sous le doigt.
     */
    fun publishSteps(steps: List<PanelStep>) {
        _steps.value = steps
        _stepCursor.value = _stepCursor.value?.coerceIn(0, (steps.lastIndex).coerceAtLeast(0))
    }

    /**
     * Quelle etape la manette designe, ou null quand le curseur est sur
     * l'ecran de face. Le focus ne traverse pas les fenetres : c'est un curseur
     * virtuel, publie ici, que chaque ecran lit de son cote — le meme parti pris
     * que [flipPage].
     */
    private val _stepCursor = MutableStateFlow<Int?>(null)
    val stepCursor: StateFlow<Int?> = _stepCursor.asStateFlow()

    /**
     * Le curseur ne s'arrete que sur une etape pressable : une etape
     * verrouillee reste affichee, mais cesse d'etre un arret.
     * pourquoi : docs/decisions/second-ecran.md § Le curseur ne s'arrête que sur une étape pressable
     */
    fun selectStep(index: Int) {
        val steps = _steps.value
        if (steps.isEmpty()) return
        val from = _stepCursor.value
        val wanted = index.coerceIn(0, steps.lastIndex)
        if (steps[wanted].enabled) {
            _stepCursor.value = wanted
            return
        }
        // Le sens de la recherche est celui du mouvement demande ; a l'arrivee
        // (pas de position de depart), on cherche vers l'avant.
        val step = if (from != null && wanted < from) -1 else 1
        var i = wanted + step
        while (i in steps.indices) {
            if (steps[i].enabled) {
                _stepCursor.value = i
                return
            }
            i += step
        }
        // Rien d'ouvert de ce cote : on ne bouge pas, sauf a n'etre nulle part,
        // auquel cas la premiere etape ouverte de la liste fait l'affaire.
        if (from == null) {
            steps.indexOfFirst { it.enabled }.takeIf { it >= 0 }?.let { _stepCursor.value = it }
        }
    }

    fun moveStep(delta: Int) {
        val index = _stepCursor.value ?: return
        selectStep(index + delta)
    }

    fun clearStepCursor() {
        _stepCursor.value = null
    }

    /**
     * Back to the resting face. Called when the app leaves a session or stops.
     *
     * **Ne vide pas la pile des faces posees**, et c'est le point : cet appel
     * vient d'un publieur de fond qui s'en va (la bibliotheque quitte l'ecran),
     * et les couches posees par-dessus ne lui appartiennent pas. Les vider
     * ferait exactement ce que la pile existe pour empecher — retirer la face
     * d'autrui. Chaque couche retire la sienne en se defaisant.
     * pourquoi : docs/decisions/second-ecran.md § Une pile plutôt qu'une publication de plus
     */
    @Synchronized
    fun clear() {
        _base.value = SecondScreenModel.Idle
        refresh()
        _page.value = 0
        _steps.value = emptyList()
        _stepCursor.value = null
    }

    /**
     * Whether two models are about the same game — not the same as being equal:
     * late facts must not snap an open second page shut.
     * pourquoi : docs/decisions/second-ecran.md § L'état du panneau vit à portée de processus, pas dans la composition
     */
    private fun sameGame(before: SecondScreenModel, after: SecondScreenModel): Boolean =
        before is SecondScreenModel.Browsing && after is SecondScreenModel.Browsing &&
            before.rom.uri == after.rom.uri
}

/**
 * Un ami, tel que le panneau le rapporte. Deja resolu.
 * pourquoi : docs/decisions/second-ecran.md § Ce qui voyage au panneau voyage déjà résolu
 */
data class PanelFriend(
    val name: String,
    /** Ce qu'il fait, en toutes lettres : « joue a X », « en ligne », « hors ligne ». */
    val line: String,
    val online: Boolean,
    val inSession: Boolean,
    /**
     * Retirer cet ami, pour de bon. Le panneau demande avant, chez lui : la
     * question se pose la ou le doigt vient de presser, pas de l'autre cote de
     * la machine.
     * pourquoi : docs/decisions/second-ecran.md § La liste d'amis descend au dos, les deux cartes restent devant
     */
    val onRemove: () -> Unit = {},
)

/**
 * Quelle marque le panneau dessine pour une entree des reglages : un **nom**,
 * pas un composable, qui retiendrait l'arbre qui l'a cree.
 * pourquoi : docs/decisions/second-ecran.md § Ce qui voyage au panneau voyage déjà résolu
 */
enum class PanelMark {
    PROFILE, LIBRARY, CONSOLES, EMULATORS, APPEARANCE, GENERAL, ABOUT,

    // Les pastilles de la barre du haut, depuis le 2026-08-29. Elles empruntent
    // les marques deja dessinees plutot que d'en ajouter : celle du chercheur
    // est un signal, celle des amis la meme silhouette que le profil — c'est le
    // titre et le resume qui les separent, et sur le panneau ils sont en grand.
    SEARCH, LAYOUT, SORT, SESSIONS, FRIENDS,
}

/**
 * Une etape de session, telle que le panneau la presse : elles descendent au
 * dos parce qu'il est tactile, et l'ecran de face rend leur hauteur.
 * pourquoi : docs/decisions/second-ecran.md § Le panneau prend les etapes, parce qu'il est tactile
 */
data class PanelStep(
    /** Deja traduit par l'ecran de face, jamais resolu au dos. */
    val label: String,
    /** Vert, avec sa coche : l'etape est faite. */
    val done: Boolean,
    val enabled: Boolean,
    val onPress: () -> Unit,
)

/**
 * The faces the panel can wear. Deliberately few: a second screen that tries to
 * be a second app is a second app to maintain.
 * pourquoi : docs/decisions/second-ecran.md § Ce qui voyage jusqu'au panneau
 */
sealed interface SecondScreenModel {

    /** Nothing going on: the app's mark, and the fact that the panel is alive. */
    data object Idle : SecondScreenModel

    /**
     * The game under the cursor. The **whole** [Rom] travels, so both screens
     * resolve artwork from one cache and one set of rules.
     * pourquoi : docs/decisions/second-ecran.md § Ce qui voyage jusqu'au panneau
     */
    data class Browsing(
        val rom: Rom,
        /** Null while the compatibility list has not been fetched, or has nothing to say. */
        val rating: CompatRating? = null,
        /**
         * Region and revision, **passed** rather than computed: the panel never
         * touches a file, and a cursor moves ten times a second.
         * pourquoi : docs/decisions/second-ecran.md § Ce qui voyage jusqu'au panneau
         */
        val tags: RomTags = RomTags(),
        /** What the served catalogue says about the game, for the second page. Usually null. */
        val meta: GameMeta? = null,
    ) : SecondScreenModel

    /**
     * The cursor is on a console's folder: the one place a player is thinking
     * about the *machine*, and every machine plays together differently.
     * pourquoi : docs/decisions/second-ecran.md § Ce qui voyage jusqu'au panneau
     */
    data class ConsoleFolder(val console: Console) : SecondScreenModel

    /**
     * Le curseur est sur une entree du hub des reglages : le panneau montre en
     * grand ce que la tuile dit en petit. Il ne **delegue** rien.
     * pourquoi : docs/decisions/reglages-ecran.md § Le hub est une grille, et le panneau montre la case visée
     */
    data class SettingsEntry(
        /** Deja traduits par l'ecran de face : le panneau a son propre contexte d'affichage. */
        val title: String,
        val summary: String,
        /** La racine du chemin, « Paramètres », traduite du meme cote. */
        val root: String,
        val mark: PanelMark,
        /** Corail quand l'entree est sociale, turquoise sinon. */
        val social: Boolean = false,
    ) : SecondScreenModel

    /**
     * L'ecran de face pose une question, et le panneau doit s'en apercevoir :
     * cette face sert a **cesser de montrer quelque chose de faux**, pas a
     * montrer quelque chose de plus. Elle porte la question posee devant, jamais
     * un resume.
     * pourquoi : docs/decisions/second-ecran.md § Un panneau qui affirme le faux est une panne
     */
    data class Asking(
        val title: String,
        val detail: String,
        /** Corail quand la question est sociale (une session, un ami). */
        val social: Boolean = false,
    ) : SecondScreenModel

    /** What the pad does right now, so the panel never claims a key that is inert. */
    val legend: PadLegend
        get() = when (this) {
            // Rien sous le curseur, donc rien a ouvrir : la legende de la grille
            // y annoncait « Ouvrir » et « Menu du jeu » au-dessus d'un ecran
            // vide, ce qui est precisement la ligne qu'elle existe pour eviter.
            is Idle -> PadLegend()
            is Browsing -> PadLegend.BROWSING
            is ConsoleFolder, is SettingsEntry, is Asking -> PadLegend.FOLDER
            is Friends -> PadLegend()
            is InSession -> PadLegend.IN_SESSION
        }

    /**
     * A session is up, and the code is the payload — it stays up while they
     * play, where the front screen is covered by the emulator.
     * pourquoi : docs/decisions/second-ecran.md § Le code de session ne porte pas d'étiquette
     */
    /**
     * La liste d'amis, pendant qu'elle est a l'ecran.
     *
     * Le panneau la porte en entier ; l'ecran de face garde les deux cartes qui
     * demandent quelque chose — ton code, et le champ pour en ajouter un.
     * pourquoi : docs/decisions/second-ecran.md § La liste d'amis descend au dos, les deux cartes restent devant
     */
    data class Friends(
        val entries: List<PanelFriend>,
    ) : SecondScreenModel

    data class InSession(
        val code: String,
        val role: Session.Role,
        val console: Console?,
        val gameTitle: String?,
        /**
         * What the emulator's own dialog asks for. The clipboard carries one at
         * a time and the dialog wants both. Null where neither is needed.
         * pourquoi : docs/decisions/second-ecran.md § Ce qui voyage jusqu'au panneau
         */
        val hostAddress: String? = null,
        val port: String? = null,
    ) : SecondScreenModel
}
