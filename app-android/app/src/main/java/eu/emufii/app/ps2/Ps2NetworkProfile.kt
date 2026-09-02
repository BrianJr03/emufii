package eu.emufii.app.ps2

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Durable state of the generated card used by ARMSX2 per-game settings. */
object Ps2NetworkProfile {

    /** Cleared wherever the answer can change, so a cached yes never outlives its card. */
    @Volatile
    private var verified: Boolean? = null

    data class Receipt(
        val rootUri: String,
        val cardName: String,
        val sourceCardName: String?,
        val backupName: String?,
        val cardSha256: String,
        val consoleIdHex: String,
        val identitySource: Ps2Armsx2Folder.IdentitySource,
        val biosName: String?,
        val biosVersion: Int?,
        val gameOverrideCount: Int,
        val folderCardName: String?,
        val importedSaveCount: Int,
        val savesLeftBehind: Int,
        val slot2AlreadyPreserved: Boolean,
        val sourceCardForSlot2: String?,
        val assigned: Boolean,
    )

    fun rootUri(context: Context): Uri? = prefs(context).getString(KEY_ROOT, null)?.toUri()

    fun setRootUri(context: Context, uri: Uri): Boolean {
        val grant = context.contentResolver.persistedUriPermissions.firstOrNull { it.uri == uri }
        if (grant?.isReadPermission != true || grant.isWritePermission != true) return false
        prefs(context).edit {
            putString(KEY_ROOT, uri.toString())
            putBoolean(KEY_READY, false)
            putBoolean(KEY_ASSIGNED, false)
        }
        return true
    }

    fun recordPrepared(context: Context, prepared: Ps2Armsx2Folder.Prepared) {
        val json = JSONObject().apply {
            put("root", prepared.rootUri)
            put("card", prepared.cardName)
            put("source", prepared.sourceCardName)
            put("backup", prepared.backupName)
            put("sha256", prepared.cardSha256)
            put("console_id", prepared.consoleIdHex)
            put("identity_source", prepared.identitySource.name)
            put("bios", prepared.biosName)
            put("bios_version", prepared.biosVersion)
            put("override_count", prepared.gameOverrideCount)
            put("folder_card", prepared.folderCardName)
            put("imported_saves", prepared.importedSaveCount)
            put("saves_left", prepared.savesLeftBehind)
            put("slot2_preserved", prepared.slot2AlreadyPreserved)
            put("source_for_slot2", prepared.sourceCardForSlot2)
        }
        verified = null
        prefs(context).edit {
            putString(KEY_ROOT, prepared.rootUri)
            putString(KEY_RECEIPT, json.toString())
            // Publishing and reading the card back is the whole preparation: no second
            // accessibility pass for global Slot 1, each launch assigns the game instead.
            putBoolean(KEY_READY, true)
            putBoolean(KEY_ASSIGNED, false)
        }
    }

    fun receipt(context: Context): Receipt? {
        val raw = prefs(context).getString(KEY_RECEIPT, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            Receipt(
                rootUri = json.getString("root"),
                cardName = json.getString("card"),
                sourceCardName = json.optNullableString("source"),
                backupName = json.optNullableString("backup"),
                cardSha256 = json.getString("sha256"),
                consoleIdHex = json.getString("console_id"),
                identitySource = Ps2Armsx2Folder.IdentitySource.valueOf(json.getString("identity_source")),
                biosName = json.optNullableString("bios"),
                biosVersion = if (json.isNull("bios_version")) null else json.getInt("bios_version"),
                gameOverrideCount = json.optInt("override_count", 0),
                folderCardName = json.optNullableString("folder_card"),
                importedSaveCount = json.optInt("imported_saves", 0),
                savesLeftBehind = json.optInt("saves_left", 0),
                slot2AlreadyPreserved = json.optBoolean("slot2_preserved", false),
                sourceCardForSlot2 = json.optNullableString("source_for_slot2"),
                assigned = if (prefs(context).contains(KEY_ASSIGNED)) {
                    prefs(context).getBoolean(KEY_ASSIGNED, false)
                } else {
                    // Before version 45 KEY_READY meant accessibility had observed the
                    // global Slot 1 assignment; keep that evidence across the migration.
                    prefs(context).getBoolean(KEY_READY, false)
                },
            )
        }.getOrNull()
    }

    /** Retained for a setup started under the accessibility flow and still in flight. */
    fun markAssigned(context: Context, cardName: String, cardSha256: String): Boolean {
        val receipt = receipt(context) ?: return false
        if (receipt.cardName != cardName || !receipt.cardSha256.equals(cardSha256, ignoreCase = true)) {
            return false
        }
        verified = null
        prefs(context).edit {
            putBoolean(KEY_READY, true)
            putBoolean(KEY_ASSIGNED, true)
        }
        return true
    }

    /**
     * ~175 ms on the Thor: proving the profile is still on the card reads the whole 8 MB
     * image and the BIOS beside it. Never call it on the main thread, draw with
     * [isReadyQuick] and confirm with [verifyReady]. Kept synchronous for the gate before
     * joining a session, where a stale yes lands the guest in a tunnel whose game never
     * opens its local menu.
     */
    fun isReady(context: Context): Boolean {
        if (!prefs(context).getBoolean(KEY_READY, false)) return false
        val receipt = receipt(context) ?: return false
        return Ps2Armsx2Folder.isPreparedCardValid(
            context,
            receipt.rootUri.toUri(),
            receipt.cardName,
            receipt.consoleIdHex,
        ).also { verified = it }
    }

    /**
     * The launch card called [isReady] three times for a single opening: half a second of
     * blocked main thread on a popup meant to appear instantly. [verifyReady] refines this
     * a moment later if the card has moved since.
     */
    fun isReadyQuick(context: Context): Boolean =
        verified ?: prefs(context).getBoolean(KEY_READY, false)

    suspend fun verifyReady(context: Context): Boolean =
        withContext(Dispatchers.IO) { isReady(context) }

    fun clearReady(context: Context) {
        verified = null
        prefs(context).edit {
            putBoolean(KEY_READY, false)
            putBoolean(KEY_ASSIGNED, false)
        }
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun prefs(context: Context) = context.applicationContext
        .getSharedPreferences("ps2_network_profile", Context.MODE_PRIVATE)

    private const val KEY_ROOT = "armsx2_root"
    private const val KEY_RECEIPT = "prepared_receipt"
    private const val KEY_READY = "assigned_global_slot1"
    private const val KEY_ASSIGNED = "legacy_global_slot1_assigned"
}
