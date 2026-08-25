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
    private val _model = MutableStateFlow<SecondScreenModel>(SecondScreenModel.Idle)
    val model: StateFlow<SecondScreenModel> = _model.asStateFlow()

    /**
     * Which page of the browsing face is showing. Held here because the button
     * that turns it is on the *front* screen, and it resets on a new game.
     * pourquoi : docs/decisions/second-ecran.md § L'état du panneau vit à portée de processus, pas dans la composition
     */
    private val _page = MutableStateFlow(0)
    val page: StateFlow<Int> = _page.asStateFlow()

    fun publish(model: SecondScreenModel) {
        if (!sameGame(_model.value, model)) _page.value = 0
        _model.value = model
    }

    /** The one control the panel has, pressed from the front screen. */
    fun flipPage() {
        if (_model.value is SecondScreenModel.Browsing) _page.value = 1 - _page.value
    }

    /**
     * Les etapes de la session, telles que le panneau peut les presser.
     *
     * Elles voyagent **deja resolues** — libelle, etat, action — et non sous
     * forme d'ingredients a recalculer au dos. Deux raisons : l'etat qui decide
     * du libelle vit dans la composition de l'ecran de session, et la fenetre
     * du panneau a son propre contexte d'affichage, qui a deja fait parler
     * l'app en francais dans une interface en anglais une fois.
     * pourquoi : docs/decisions/second-ecran.md § Le panneau prend les etapes, parce qu'il est tactile
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
    }

    /** Back to the resting face. Called when the app leaves a session or stops. */
    fun clear() {
        _model.value = SecondScreenModel.Idle
        _page.value = 0
        _steps.value = emptyList()
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
 * Un ami, tel que le panneau le rapporte.
 *
 * Deja resolu — le libelle d'etat est traduit par l'ecran de face, jamais au
 * dos, ou le contexte d'affichage a sa propre langue.
 * pourquoi : docs/decisions/second-ecran.md § La liste d'amis descend au dos, les deux cartes restent devant
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
 * Une etape de session, telle que le panneau la presse.
 *
 * Le panneau est tactile : la Thor arbitre le focus entre ses deux ecrans, une
 * pression donne le focus a l'ecran presse. Les etapes descendent donc au dos,
 * ou elles sont sous les pouces, et l'ecran de face rend la hauteur qu'elles
 * prenaient a l'explication.
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

    /** What the pad does right now, so the panel never claims a key that is inert. */
    val legend: PadLegend
        get() = when (this) {
            is Idle, is Browsing -> PadLegend.BROWSING
            is ConsoleFolder -> PadLegend.FOLDER
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
