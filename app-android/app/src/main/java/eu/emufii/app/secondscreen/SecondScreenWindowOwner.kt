package eu.emufii.app.secondscreen

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.view.View

/**
 * The three owners Compose insists on, owned by nobody in particular.
 *
 * A `ComposeView` refuses to compose unless its view tree carries a
 * [LifecycleOwner], a [ViewModelStoreOwner] and a [SavedStateRegistryOwner].
 * On the main screen the activity supplies all three and it is invisible work.
 * On a window that is not the activity's, nothing supplies them, and the
 * failure is a runtime crash on first frame rather than anything a compiler
 * catches.
 *
 * The obvious shortcut is to lend the activity's owners to the second screen's
 * window. It compiles, it runs, and it is a dead end: the host that this whole
 * feature is for, a foreground service that keeps the panel alive **while the
 * emulator owns the front display**, has no activity to borrow from. Writing
 * the shortcut now would mean writing this class later anyway, after the
 * feature had already shipped on top of it.
 *
 * So the owner is standalone from the first line. Both hosts attach one of
 * these to their own window, and the second host is a subscriber rather than a
 * rewrite.
 */
class SecondScreenWindowOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    /**
     * Hand the owners to a view and bring it to life.
     *
     * The restore has to happen before the lifecycle passes CREATED, which is
     * the ordering the platform documents and the one that fails loudly if it
     * is wrong. Nothing is ever saved into this registry: the panel holds no
     * state of its own, it renders a flow. It exists because Compose asks for
     * one, and an empty registry is the honest answer.
     */
    fun attachTo(view: View) {
        savedStateController.performRestore(null)
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    /**
     * Tear the window down.
     *
     * Both halves matter. Without DESTROYED the composition keeps its
     * collectors on [SecondScreen] and the window leaks; without clearing the
     * store the same leak happens one indirection further away. A panel that is
     * unplugged and replugged twenty times in an evening has to leave nothing
     * behind each time.
     */
    fun detach() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}
