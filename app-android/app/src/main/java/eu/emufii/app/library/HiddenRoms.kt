package eu.emufii.app.library

import android.content.Context
import androidx.core.content.edit

/** Nothing is deleted: the file stays with its saves, so another emulator that knows it by path sees no change. */
class HiddenRoms(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("rom_hidden", Context.MODE_PRIVATE)

    /** Keyed like a rename, and must stay so. Never the displayed name. */
    private fun key(rom: Rom): String = rom.sessionId ?: rom.filename

    fun isHidden(rom: Rom): Boolean = prefs.contains(key(rom))

    fun hide(rom: Rom) {
        prefs.edit { putBoolean(key(rom), true) }
    }

    fun count(): Int = prefs.all.size

    fun clear() {
        prefs.edit { clear() }
    }
}
