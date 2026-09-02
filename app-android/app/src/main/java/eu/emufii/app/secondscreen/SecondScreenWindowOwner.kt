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
 * The three owners Compose insists on, standalone from the first line: the foreground
 * service holding the panel while the emulator owns the front screen has no activity to
 * borrow them from.
 * pourquoi : docs/decisions/second-ecran.md § The three owners are self-contained from the first line
 */
class SecondScreenWindowOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    /** The restore must precede CREATED; nothing is ever saved, the panel renders a flow. */
    fun attachTo(view: View) {
        savedStateController.performRestore(null)
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    /**
     * Both halves matter: without DESTROYED the composition keeps its collectors on
     * [SecondScreen] and the window leaks, without clearing the store it leaks one
     * indirection further away.
     */
    fun detach() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}
