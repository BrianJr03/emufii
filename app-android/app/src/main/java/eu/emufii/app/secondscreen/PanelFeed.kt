package eu.emufii.app.secondscreen

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The news, on its way to the rear panel.
 *
 * A friend coming online, a version being out: the app already says both, and
 * this does not take that away from anyone. **The front screen keeps every
 * alert it had** — a player with one screen loses nothing, which is the rule
 * this feature works under (`CLAUDE.md`, « le mono-écran reste la mise en page
 * principale »). The panel *mirrors* them.
 *
 * What it adds is the case the front screen cannot serve at all: while a game
 * is running, Emufii is behind the emulator and its own alert has nowhere to
 * appear. Today that news arrives as an Android notification, which pulls a
 * shade over the game the player is in the middle of. On the back of the
 * machine it costs them nothing and interrupts nothing.
 *
 * Process-scoped, like [SecondScreen] and for the same reason: the host that
 * will outlive the activity has to read the same thing, and a feed held in a
 * composition would go silent exactly when the emulator takes the front screen.
 */
object PanelFeed {

    /**
     * One piece of news.
     *
     * [id] exists so a note can be retired by the coroutine that showed it
     * without retiring the one that replaced it in the meantime — the race is
     * real: two friends coming online a second apart.
     */
    data class Note(
        val text: String,
        val kind: Kind,
        val id: Long = nextId(),
    )

    /** What the note is about, which is all the panel needs to tint the dot. */
    enum class Kind { FRIEND, UPDATE, INFO }

    private val _note = MutableStateFlow<Note?>(null)
    val note: StateFlow<Note?> = _note.asStateFlow()

    fun post(text: String, kind: Kind = Kind.INFO) {
        if (text.isBlank()) return
        _note.value = Note(text = text, kind = kind)
    }

    /** Retires [id], and only [id]: a newer note has already taken the strip. */
    fun dismiss(id: Long) {
        if (_note.value?.id == id) _note.value = null
    }

    fun clear() {
        _note.value = null
    }

    private var counter = 0L
    @Synchronized private fun nextId(): Long = ++counter
}
