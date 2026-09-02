package eu.emufii.app.notify

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether anyone is looking at Emufii: a friend appearing on screen gets an in-app line,
 * elsewhere a system notification. Fed by the activity, not `ProcessLifecycleOwner`, and
 * kept outside the composition, which keeps running in the background, the case detected.
 */
object AppForeground {
    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    fun set(value: Boolean) {
        _visible.value = value
    }
}
