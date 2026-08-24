package eu.emufii.app.secondscreen

import eu.emufii.app.compat.CompatRating
import eu.emufii.app.library.Console
import eu.emufii.app.library.Rom
import eu.emufii.app.session.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What the Thor's second screen is showing, and the one place that decides it.
 *
 * The state lives here, at process scope, and **not** in the Compose tree that
 * draws the main screen. That is the whole design, and it is not tidiness: the
 * second screen's reason to exist is the moment the emulator owns the front
 * display and Emufii is nowhere to be seen. A model held inside a composition
 * dies with that composition, so the one host that matters later, a foreground
 * service outliving the activity, could never read it. Publishing here costs
 * nothing today and is what makes that host a new subscriber rather than a
 * rewrite.
 *
 * Both hosts render [SecondScreenContent] from this flow, so there is exactly
 * one description of the screen no matter who is holding the window.
 */
object SecondScreen {
    private val _model = MutableStateFlow<SecondScreenModel>(SecondScreenModel.Idle)
    val model: StateFlow<SecondScreenModel> = _model.asStateFlow()

    fun publish(model: SecondScreenModel) {
        _model.value = model
    }

    /** Back to the resting face. Called when the app leaves a session or stops. */
    fun clear() {
        _model.value = SecondScreenModel.Idle
    }
}

/**
 * The faces the panel can wear.
 *
 * Deliberately few. A second screen that tries to be a second app is a second
 * app to maintain; this one answers one question at a time, and the question
 * changes with what the player is doing on the front.
 */
sealed interface SecondScreenModel {

    /** Nothing going on: the app's mark, and the fact that the panel is alive. */
    data object Idle : SecondScreenModel

    /**
     * The game under the cursor on the front screen.
     *
     * The whole [Rom] travels rather than a handful of extracted fields, and the
     * panel resolves its own artwork from it. A tile on the front screen already
     * does exactly that, through the same `rememberTileArt`, so both screens
     * answer from one cache and one set of rules: the day a player picks a new
     * cover by hand, the back panel is not a second place that has to be told.
     */
    data class Browsing(
        val rom: Rom,
        /** Null while the compatibility list has not been fetched, or has nothing to say. */
        val rating: CompatRating? = null,
    ) : SecondScreenModel

    /** What the pad does right now, so the panel never claims a key that is inert. */
    val legend: PadLegend
        get() = when (this) {
            is Idle, is Browsing -> PadLegend.BROWSING
            is InSession -> PadLegend.IN_SESSION
        }

    /**
     * A session is up. The code is the payload.
     *
     * It is what gets read out loud to a friend, and until now it lived on a
     * screen the emulator covers the instant the game starts, which meant
     * reading it, launching, and hoping. Here it stays up while they play.
     */
    data class InSession(
        val code: String,
        val role: Session.Role,
        val console: Console?,
        val gameTitle: String?,
        /**
         * What the emulator's own dialog asks for.
         *
         * These are on the front screen too, and that is exactly the problem
         * they solve here: the moment you need them is the moment you are
         * inside ARMSX2 or Azahar typing them in, and the front screen is gone.
         * Copying them to the clipboard only carries one at a time, and the
         * dialog wants both. Null for a console whose path asks for neither.
         */
        val hostAddress: String? = null,
        val port: String? = null,
    ) : SecondScreenModel
}
