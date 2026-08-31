package eu.emufii.app.update

import android.content.Context
import androidx.core.content.edit
import eu.emufii.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * The note cannot come from the app's resources, since it describes a version
 * the installed app knows nothing about; it travels in both languages instead.
 */
data class LatestVersion(
    val versionCode: Int,
    val versionName: String,
    val url: String?,
    val notes: String?,
    val notesEn: String? = null
) {

    /** French stays the fallback: the only one old `latest.json` files carry. */
    fun notesFor(locale: java.util.Locale): String? =
        if (locale.language == "fr") notes ?: notesEn else notesEn ?: notes
}

/**
 * Tells the player a version exists and where to get it; downloads and installs
 * nothing, since updating from a URL read off the network is a code execution
 * path. Silence is the default answer: unreachable server, missing file or
 * broken JSON all display nothing, because "we do not know" must never reach the
 * screen as "you are behind".
 * pourquoi : docs/SECURITY_REVIEW.md § S5
 */
object UpdateCheck {

    /** Null when the server announces none or does not answer; never throws. */
    suspend fun fetch(baseUrl: String = BuildConfig.COORDINATOR_BASE_URL): LatestVersion? =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL("$baseUrl/latest").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4000
                    readTimeout = 4000
                }
                try {
                    // 204 = nothing published. Everything else outside 2xx is a
                    // failure, and a failure is silence.
                    if (conn.responseCode != 200) return@runCatching null
                    val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    LatestVersion(
                        versionCode = json.getInt("version_code"),
                        versionName = json.optString("version_name"),
                        url = json.optString("url").takeIf { it.isNotBlank() && it != "null" },
                        notes = json.optString("notes").takeIf { it.isNotBlank() && it != "null" },
                        notesEn = json.optString("notes_en").takeIf { it.isNotBlank() && it != "null" }
                    )
                } finally {
                    conn.disconnect()
                }
            }.getOrNull()
        }

    fun isNewer(latest: LatestVersion): Boolean = latest.versionCode > BuildConfig.VERSION_CODE
}

/**
 * What the player has already dismissed.
 *
 * Remembered by version number and not by a boolean: dismissing the announcement
 * for 12 must say nothing about 13. A "seen it" flag would have silenced every
 * later version, which amounts to removing the feature at the first refusal.
 */
class UpdateDismissals(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isDismissed(versionCode: Int): Boolean = prefs.getInt(KEY, 0) >= versionCode

    fun dismiss(versionCode: Int) {
        prefs.edit { putInt(KEY, versionCode) }
    }

    private companion object {
        const val PREFS = "emufii_updates"
        const val KEY = "dismissed_version_code"
    }
}
