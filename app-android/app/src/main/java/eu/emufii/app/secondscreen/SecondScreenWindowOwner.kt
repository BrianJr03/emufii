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
 * Autonome des la premiere ligne : le service de premier plan qui garde le
 * panneau vivant pendant que l'emulateur possede l'ecran de face n'a aucune
 * activite a qui emprunter les siens.
 * pourquoi : docs/decisions/second-ecran.md § Les trois propriétaires sont autonomes dès la première ligne
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
