package eu.emufii.app.library

import android.content.Context
import androidx.core.content.edit

/**
 * The file on disk is never touched: the ROM keeps its name, its saves stay paired with
 * it, and a third-party emulator that knows it by path sees nothing change. Last resort
 * for what automatic reading misses, the SMDH and banner titles being whatever the
 * publisher put there, sometimes truncated, in Japanese, or the tagline.
 */
class RomNames(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("rom_names", Context.MODE_PRIVATE)

    /**
     * On no account the displayed name: that changes at the first rename, and the key
     * would be lost with it, the chosen name gone by the second launch.
     */
    private fun key(rom: Rom): String = rom.sessionId ?: rom.filename

    fun nameFor(rom: Rom): String? =
        prefs.getString(key(rom), null)?.takeIf { it.isNotBlank() }

    fun setName(rom: Rom, name: String) {
        val cleaned = name.trim()
        prefs.edit {
            if (cleaned.isEmpty()) remove(key(rom)) else putString(key(rom), cleaned)
        }
    }

    fun apply(rom: Rom): Rom =
        nameFor(rom)?.let { rom.copy(displayName = it) } ?: rom
}
