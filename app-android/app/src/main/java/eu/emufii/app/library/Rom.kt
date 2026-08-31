package eu.emufii.app.library

import android.net.Uri
import java.io.File

data class Rom(
    val uri: Uri,
    val filename: String,
    val displayName: String,
    val console: Console,
    val titleIdHex: String? = null,
    val productCode: String? = null,
    val iconFile: File? = null,
    val accentArgb: Int? = null,
    /** ARMSX2's eight-digit boot-ELF XOR, computed while scanning a PS2 disc. */
    val ps2ElfCrc: String? = null,
    /** The provider's LAST_MODIFIED; Android exposes no date-added. Zero sorts last. */
    val addedAt: Long = 0L
) {
    /** The PSP and the DS carry no title id, so they publish their disc id instead. */
    val sessionId: String? get() = titleIdHex ?: productCode
}
