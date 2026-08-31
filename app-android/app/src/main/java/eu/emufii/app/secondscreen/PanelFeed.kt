package eu.emufii.app.secondscreen

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The news, mirrored to the rear panel. The front screen keeps every alert it
 * had; what this adds is the case it cannot serve, a game running with Emufii
 * behind the emulator, where the only other route is a notification pulled over
 * the game. Process-scoped like [SecondScreen]: a feed held in a composition
 * would go silent exactly when the emulator takes the front screen.
 */
object PanelFeed {

    /**
     * One piece of news.
     *
     * [id] exists so a note can be retired by the coroutine that showed it
     * without retiring the one that replaced it in the meantime: the race is
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
