package eu.emufii.app.ps2

import android.content.Context
import android.util.Log
import org.json.JSONObject

/**
 * ARMSX2's labels, read from the 19 JSON files it keeps under `i18n`. All 19 languages,
 * not the device's: nothing says which language ARMSX2 runs in, and searching them all
 * costs one read at the first call. English stays in regardless, the Local Link labels
 * being hardcoded in ARMSX2's source rather than translated.
 */
class Ps2Labels(private val context: Context) {

    private val cache = HashMap<String, List<String>>()

    private val catalogs: List<JSONObject> by lazy { loadCatalogs() }

    /**
     * Every way ARMSX2 might write this label. [english] is always returned: a key absent
     * from the translations is the normal case for Local Link, not an error.
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
     * Opens ARMSX2's assets from inside Emufii, which only works because the package is
     * declared in `<queries>`: without it `createPackageContext` throws
     * `NameNotFoundException` at runtime. Failing falls back to English alone, so the
     * automation still bites on an English ARMSX2.
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
        Log.w(TAG, "assets i18n d'ARMSX2 illisibles, repli sur l'anglais seul", it)
        emptyList()
    }

    private fun installed(pkg: String): Boolean =
        runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess

    private companion object {
        const val TAG = "Ps2Labels"
    }
}
