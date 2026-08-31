package eu.emufii.app.ps2

import android.content.Context
import android.util.Log
import org.json.JSONObject

/**
 * ARMSX2's labels, read from the 19 JSON files it keeps under `i18n`. Same
 * principle as `eu.emufii.app.netplay.NetplayLabels`, different container.
 *
 * All 19 languages, not the device's: nothing says which language the app
 * opposite runs in, and searching them all costs one read at the first call.
 * English stays in regardless, since the Local Link labels are in no translation
 * file at all, hardcoded in ARMSX2's source.
 */
class Ps2Labels(private val context: Context) {

    /** Key to every known translation, English included. Read once. */
    private val cache = HashMap<String, List<String>>()

    /** All of ARMSX2's language files, loaded once. */
    private val catalogs: List<JSONObject> by lazy { loadCatalogs() }

    /**
     * Every way ARMSX2 might write this label.
     *
     * [english] is what we fall back on, and it is always returned: a key missing
     * from the translations is not an error, it is the normal case for Local
     * Link.
     */
    fun of(key: String?, english: String): List<String> = cache.getOrPut(key ?: english) {
        val out = LinkedHashSet<String>()
        out += english
        if (key != null) {
            for (catalog in catalogs) {
                catalog.optString(key).takeIf { it.isNotBlank() }?.let { out += it }
            }
        }
        out.toList()
    }

    /**
     * Opens ARMSX2's assets from inside Emufii.
     *
     * Possible because the package is declared in `<queries>`: without that,
     * `createPackageContext` throws `NameNotFoundException` and we would only
     * find out at runtime. The failure is not fatal, we fall back to English, and
     * the automation will only work on an English ARMSX2, which is a legible
     * degraded mode rather than a silent breakdown.
     */
    private fun loadCatalogs(): List<JSONObject> = runCatching {
        val pkg = Ps2Target.packages.first { installed(it) }
        val assets = context.createPackageContext(pkg, 0).assets
        val files = assets.list(Ps2Target.I18n.DIRECTORY).orEmpty()
        files.filter { it.endsWith(".json") }.mapNotNull { name ->
            runCatching {
                val text = assets.open("${Ps2Target.I18n.DIRECTORY}/$name")
                    .bufferedReader()
                    .use { it.readText() }
                JSONObject(text)
            }.getOrNull()
        }.also { Log.d(TAG, "ARMSX2 labels: ${it.size} languages read") }
    }.getOrElse {
        // Said once, loudly: this is the difference between "the automation does
        // not bite" and "the automation does not bite in French".
        Log.w(TAG, "assets i18n d'ARMSX2 illisibles, repli sur l'anglais seul", it)
        emptyList()
    }

    private fun installed(pkg: String): Boolean =
        runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess

    private companion object {
        const val TAG = "Ps2Labels"
    }
}
