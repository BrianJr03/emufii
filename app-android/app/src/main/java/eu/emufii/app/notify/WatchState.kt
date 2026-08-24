package eu.emufii.app.notify

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject

/**
 * What the last look at the coordinator found, kept so the next one can tell a
 * change from a repetition.
 *
 * Shared by the app and the background job on purpose: they take turns looking,
 * and if each kept its own memory the job would re-announce, half an hour later,
 * the friend the player already saw arrive on screen.
 *
 * Small enough for shared preferences, a handful of friends and two integers,
 * and it survives the process being killed, which is exactly the moment the
 * memory has to hold.
 */
class WatchState(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun seen(): Map<String, SeenFriend> = runCatching {
        val raw = prefs.getString(KEY_SEEN, null) ?: return emptyMap()
        val o = JSONObject(raw)
        o.keys().asSequence().mapNotNull { code ->
            val entry = o.optJSONObject(code) ?: return@mapNotNull null
            code to SeenFriend(
                online = entry.optBoolean(FIELD_ONLINE, false),
                game = entry.optString(FIELD_GAME).takeIf { it.isNotBlank() }
            )
        }.toMap()
    }.getOrDefault(emptyMap())

    fun setSeen(seen: Map<String, SeenFriend>) {
        val o = JSONObject()
        seen.forEach { (code, s) ->
            o.put(
                code,
                JSONObject().apply {
                    put(FIELD_ONLINE, s.online)
                    s.game?.let { put(FIELD_GAME, it) }
                }
            )
        }
        prefs.edit { putString(KEY_SEEN, o.toString()) }
    }

    /**
     * The highest version we have already announced.
     *
     * Separate from [eu.emufii.app.update.UpdateDismissals]: dismissing the
     * banner means "stop showing me this card", not "never mention this version
     * again". A player who swipes the notification away and never opens the app
     * would otherwise be told about the same version every fifteen minutes.
     */
    fun notifiedVersion(): Int = prefs.getInt(KEY_VERSION, 0)

    fun setNotifiedVersion(versionCode: Int) {
        prefs.edit { putInt(KEY_VERSION, versionCode) }
    }

    private companion object {
        const val PREFS = "emufii_watch"
        const val KEY_SEEN = "seen_friends"
        const val KEY_VERSION = "notified_version_code"
        const val FIELD_ONLINE = "online"
        const val FIELD_GAME = "game"
    }
}
