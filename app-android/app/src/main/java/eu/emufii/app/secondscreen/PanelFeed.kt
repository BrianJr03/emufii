package eu.emufii.app.secondscreen

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The front screen keeps every alert it had; this adds the case it cannot serve, a game
 * running with Emufii behind the emulator. Process-scoped like [SecondScreen]: a feed
 * held in a composition would go silent exactly when the emulator takes the front screen.
 */
object PanelFeed {

    /**
     * [id] lets the coroutine that showed a note retire it without retiring the one that
     * replaced it meanwhile; the race is two friends coming online a second apart.
     */
    data class Note(
        val text: String,
        val kind: Kind,
        val id: Long = nextId(),
    )

    enum class Kind { FRIEND, UPDATE, INFO }

    private val _note = MutableStateFlow<Note?>(null)
    val note: StateFlow<Note?> = _note.asStateFlow()

    fun post(text: String, kind: Kind = Kind.INFO) {
        if (text.isBlank()) return
        _note.value = Note(text = text, kind = kind)
    }

    fun dismiss(id: Long) {
        if (_note.value?.id == id) _note.value = null
    }

    fun clear() {
        _note.value = null
    }

    private var counter = 0L
    @Synchronized private fun nextId(): Long = ++counter
}
