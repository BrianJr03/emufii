package eu.emufii.app.profile

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * [name] is the last pseudo the coordinator reported, kept so the list still reads as
 * names once everyone is offline. It starts null: a friend who has not opened the app
 * has no name to look up.
 */
data class Friend(
    val code: String,
    val name: String?,
    val addedAt: Long
) {
    /** `E7K2-9QM4-XR8T`, the code as it is shown and shared. */
    val displayCode: String get() = FriendCode.format(code)
}

data class FriendStatus(
    val online: Boolean,
    val sessionCode: String? = null,
    val romTitle: String? = null,
    val romTitleId: String? = null,
    val players: Int = 0,
    val ready: Boolean = false
) {
    val inSession: Boolean get() = sessionCode != null

    companion object {
        val Offline = FriendStatus(online = false)
    }
}

sealed interface AddFriendResult {
    data class Added(val friend: Friend) : AddFriendResult

    /** Wrong length, stray characters, or a typo the checksum caught. */
    data object Invalid : AddFriendResult

    data object AlreadyAdded : AddFriendResult

    /** Their own code: harmless, but it would sit in the list showing them their own game. */
    data object Self : AddFriendResult
}

/**
 * On this device and nowhere else. There is no server-side social graph: the coordinator
 * is only ever asked "which of these codes is online", and answers from a table it forgets
 * every couple of minutes. The consequence: this list does not follow the user to a new
 * phone.
 */
class FriendStore private constructor(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _friends = MutableStateFlow(load())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private fun load(): List<Friend> = runCatching {
        val raw = prefs.getString(KEY_LIST, null) ?: return emptyList()
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { i ->
            val o = array.getJSONObject(i)
            val code = o.optString(FIELD_CODE).takeIf { FriendCode.isValid(it) } ?: return@mapNotNull null
            Friend(
                code = code,
                name = o.optString(FIELD_NAME).takeIf { it.isNotBlank() },
                addedAt = o.optLong(FIELD_ADDED_AT, 0L)
            )
        }
    }.getOrDefault(emptyList())

    private fun persist(list: List<Friend>) {
        val array = JSONArray()
        for (f in list) {
            array.put(
                JSONObject().apply {
                    put(FIELD_CODE, f.code)
                    if (f.name != null) put(FIELD_NAME, f.name)
                    put(FIELD_ADDED_AT, f.addedAt)
                }
            )
        }
        prefs.edit { putString(KEY_LIST, array.toString()) }
        _friends.value = list
    }

    fun add(input: String, selfCode: String, now: Long = System.currentTimeMillis()): AddFriendResult {
        val code = FriendCode.normalize(input) ?: return AddFriendResult.Invalid
        if (code == selfCode) return AddFriendResult.Self
        if (_friends.value.any { it.code == code }) return AddFriendResult.AlreadyAdded
        val friend = Friend(code = code, name = null, addedAt = now)
        persist(_friends.value + friend)
        return AddFriendResult.Added(friend)
    }

    fun remove(code: String) {
        persist(_friends.value.filterNot { it.code == code })
    }

    fun noteNames(names: Map<String, String>) {
        if (names.isEmpty()) return
        val updated = _friends.value.map { f ->
            val fresh = names[f.code]
            if (fresh != null && fresh != f.name) f.copy(name = fresh) else f
        }
        if (updated != _friends.value) persist(updated)
    }

    fun clear() = persist(emptyList())

    companion object {
        /**
         * One instance for the process: shared preferences are seen by every instance,
         * the `StateFlow` in front of them is not. A second store built by the presence
         * watcher wrote a freshly learnt name to disk and the screen went on showing the
         * code until restart. On the Thor, 24 August: the notification said "Testeur"
         * while the list still said `EMVF-11TE-ST0S`.
         */
        @Volatile
        private var instance: FriendStore? = null

        fun get(context: Context): FriendStore =
            instance ?: synchronized(this) {
                instance ?: FriendStore(context.applicationContext).also { instance = it }
            }

        @VisibleForTesting
        fun reload(context: Context): FriendStore = FriendStore(context.applicationContext)

        private const val PREFS = "emufii_friends"
        private const val KEY_LIST = "list"
        private const val FIELD_CODE = "code"
        private const val FIELD_NAME = "name"
        private const val FIELD_ADDED_AT = "added_at"
    }
}
