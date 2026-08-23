package eu.emufii.app.ps2

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import org.json.JSONObject

/** Durable state of the generated card and of its verified global assignment. */
object Ps2NetworkProfile {

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
        val slot2AlreadyPreserved: Boolean,
        val sourceCardForSlot2: String?,
        val assigned: Boolean,
    )

    fun rootUri(context: Context): Uri? = prefs(context).getString(KEY_ROOT, null)?.toUri()

    /** Store a new tree only once Android confirms persistent read and write grants. */
    fun setRootUri(context: Context, uri: Uri): Boolean {
        val grant = context.contentResolver.persistedUriPermissions.firstOrNull { it.uri == uri }
        if (grant?.isReadPermission != true || grant.isWritePermission != true) return false
        prefs(context).edit {
            putString(KEY_ROOT, uri.toString())
            putBoolean(KEY_READY, false)
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
            put("slot2_preserved", prepared.slot2AlreadyPreserved)
            put("source_for_slot2", prepared.sourceCardForSlot2)
        }
        prefs(context).edit {
            putString(KEY_ROOT, prepared.rootUri)
            putString(KEY_RECEIPT, json.toString())
            putBoolean(KEY_READY, false)
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
                slot2AlreadyPreserved = json.optBoolean("slot2_preserved", false),
                sourceCardForSlot2 = json.optNullableString("source_for_slot2"),
                assigned = prefs(context).getBoolean(KEY_READY, false),
            )
        }.getOrNull()
    }

    /** Called only after the driver observes the target card's `✓ Slot 1` chip. */
    fun markAssigned(context: Context, cardName: String, cardSha256: String): Boolean {
        val receipt = receipt(context) ?: return false
        if (receipt.cardName != cardName || !receipt.cardSha256.equals(cardSha256, ignoreCase = true)) {
            return false
        }
        prefs(context).edit { putBoolean(KEY_READY, true) }
        return true
    }

    fun isReady(context: Context): Boolean {
        if (!prefs(context).getBoolean(KEY_READY, false)) return false
        val receipt = receipt(context) ?: return false
        return Ps2Armsx2Folder.isStillValid(
            context,
            receipt.rootUri.toUri(),
            receipt.cardName,
            receipt.consoleIdHex,
        )
    }

    fun clearReady(context: Context) {
        prefs(context).edit { putBoolean(KEY_READY, false) }
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun prefs(context: Context) = context.applicationContext
        .getSharedPreferences("ps2_network_profile", Context.MODE_PRIVATE)

    private const val KEY_ROOT = "armsx2_root"
    private const val KEY_RECEIPT = "prepared_receipt"
    private const val KEY_READY = "assigned_global_slot1"
}
