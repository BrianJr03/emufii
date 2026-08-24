package eu.emufii.app.notify

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether anyone is actually looking at Emufii right now.
 *
 * The distinction matters for exactly one decision: a friend appearing while the
 * app is on screen deserves a line inside the app, and the same friend appearing
 * while the player is in an emulator, or on their home screen, deserves a system
 * notification. Getting it wrong is not a small thing either way: an in-app card
 * nobody can see is a lost alert, and a notification for something already
 * written on the screen is noise.
 *
 * Fed by the activity rather than by `ProcessLifecycleOwner`, which would mean a
 * dependency for one boolean this app can observe itself. It lives outside the
 * composition because the process keeps composing while it is in the background,
 * which is precisely the case being detected.
 */
object AppForeground {
    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    fun set(value: Boolean) {
        _visible.value = value
    }
}
