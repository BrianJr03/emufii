package eu.emufii.app.library

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

private const val TITLE_FORMAT = "v3"

class IconCache(context: Context) {
    private val dir: File = File(context.filesDir, "icons").apply { mkdirs() }

    fun fileFor(titleIdHex: String): File = File(dir, "$titleIdHex.png")
    /**
     * Titles are cached per language: the same cartridge has a different name in each.
     * [TITLE_FORMAT] goes up at every change in the way a title is read, failing which
     * libraries already scanned go on serving the old one; v2 shipped without the SMDH
     * line-break rule and wrote the truncated "The Legend of Zelda" into the cache.
     */
    private fun titleFileFor(titleIdHex: String, lang: String): File =
        File(dir, "$titleIdHex.title.$TITLE_FORMAT.$lang")
    private fun accentFileFor(titleIdHex: String): File = File(dir, "$titleIdHex.accent")

    fun writeIcon(titleIdHex: String, bitmap: Bitmap) {
        FileOutputStream(fileFor(titleIdHex)).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    fun writeTitle(titleIdHex: String, title: String, lang: String = TitleLanguage.tag) {
        titleFileFor(titleIdHex, lang).writeText(title, Charsets.UTF_8)
    }

    fun writeAccent(titleIdHex: String, argb: Int) {
        accentFileFor(titleIdHex).writeText(argb.toString(), Charsets.UTF_8)
    }

    fun readAccent(titleIdHex: String): Int? =
        accentFileFor(titleIdHex).takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull()

    fun readTitle(titleIdHex: String, lang: String = TitleLanguage.tag): String? {
        val f = titleFileFor(titleIdHex, lang)
        return if (f.exists()) f.readText(Charsets.UTF_8).ifBlank { null } else null
    }
}
