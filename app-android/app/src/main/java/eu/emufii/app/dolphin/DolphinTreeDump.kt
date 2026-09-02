package eu.emufii.app.dolphin

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import eu.emufii.app.BuildConfig
import eu.emufii.app.netplay.NetplayLabels
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The only test bench is the Thor, and a remote player with no PC has no logcat to
 * send: two hypotheses were already spent guessing on their behalf, the build and the
 * node ceiling, both wrong. The file goes into the public Downloads rather than the
 * app's private storage, `getExternalFilesDir` having been invisible since Android 11.
 * Off unless [BuildConfig.TREE_DUMP]: the dump names the games in the grid.
 */
object DolphinTreeDump {

    /**
     * One dump per plan: the driver passes over a stuck screen several times a second,
     * and would otherwise fill Downloads with copies of the same tree.
     */
    private var written = false

    fun reset() {
        written = false
    }

    /** [reason] says at which fork the driver gave up, and leads the file. */
    fun capture(context: Context, pkg: String, nodes: List<Node>, reason: String) {
        if (!BuildConfig.TREE_DUMP || written) return
        written = true

        val stamp = SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US).format(Date())
        val name = "emufii-arbre-dolphin-$stamp.txt"
        val body = render(context, pkg, nodes, reason)

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val ok = runCatching {
            val uri = context.contentResolver
                .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return@runCatching false
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(body.toByteArray())
            } ?: return@runCatching false
            true
        }.getOrElse {
            Log.w(TAG, "dump impossible", it)
            false
        }

        // The player does not read logs, that being this file's premise: unless told
        // on screen they never know they have something to send.
        val message =
            if (ok) "Emufii: diagnostic written to Downloads/$name"
            else "Emufii: the diagnostic could not be written"
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
        Log.i(TAG, "$message (${nodes.size} nodes, reason=$reason)")
    }

    private fun render(
        context: Context,
        pkg: String,
        nodes: List<Node>,
        reason: String
    ): String = buildString {
        appendLine("Emufii: Dolphin accessibility tree")
        appendLine("reason       : $reason")
        appendLine("date         : ${Date()}")
        appendLine("emufii       : ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("target pkg   : $pkg ${versionOf(context, pkg)}")
        appendLine("device       : ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("locales      : ${context.resources.configuration.locales.toLanguageTags()}")
        appendLine("nodes        : ${nodes.size}")
        appendLine()

        // One unresolved label explains a silent driver by itself, half the cases.
        // Zero translations for a name means the string does not exist in this Dolphin
        // build; a full list means the tree is what does not contain it.
        appendLine("--- labels resolved in the resources of $pkg ---")
        for (name in LABELS) {
            val values = NetplayLabels.of(context, pkg, name)
            appendLine("$name (${values.size}) : ${values.joinToString(" | ")}")
        }
        appendLine()

        appendLine("--- nœuds ---")
        nodes.forEachIndexed { i, n ->
            appendLine(
                "[$i] ${n.className}" +
                    " texte=${n.text.quote()}" +
                    " desc=${n.description.quote()}" +
                    " id=${n.viewId.quote()}" +
                    " clic=${n.clickable}" +
                    " bornes=[${n.bounds.left},${n.bounds.top}][${n.bounds.right},${n.bounds.bottom}]"
            )
        }
    }

    private fun String.quote(): String = if (isEmpty()) "-" else "\"$this\""

    private fun versionOf(context: Context, pkg: String): String = runCatching {
        val info = context.packageManager.getPackageInfo(pkg, 0)
        "${info.versionName} (${info.longVersionCode})"
    }.getOrDefault("version inconnue")

    private val LABELS = listOf(
        DolphinTarget.LABEL_MENU_NETPLAY,
        DolphinTarget.LABEL_NICKNAME,
        DolphinTarget.LABEL_IP_ADDRESS,
        DolphinTarget.LABEL_PORT,
        DolphinTarget.LABEL_CONNECTION_TYPE,
        DolphinTarget.LABEL_DIRECT_CONNECTION,
        DolphinTarget.LABEL_TRAVERSAL_SERVER,
        DolphinTarget.LABEL_ROLE_CONNECT,
        DolphinTarget.LABEL_ROLE_HOST
    )

    private const val TAG = "DolphinNetplay"
}
