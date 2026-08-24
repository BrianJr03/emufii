package eu.emufii.app.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.math.abs

/**
 * Who you are to the other players. [id] is a stable random identifier that
 * doubles as the friend code, and is therefore **public by design** — which is
 * what lets adding a friend need no server-side directory.
 * pourquoi : docs/decisions/identite-et-dumps.md § Le code d'ami *est* l'identité, et il est public par conception
 */
data class Profile(
    val id: String,
    val name: String,
    val avatarFile: File? = null
) {
    /** True once the user has actually chosen a name rather than kept the default. */
    val isNamed: Boolean get() = name.isNotBlank() && name != DEFAULT_NAME

    /** The id as you'd show it to someone: `E7K2-9QM4-XR8T`. */
    val friendCode: String get() = FriendCode.format(id)

    companion object {
        /**
         * The pseudo of someone who never picked one: a **fixed sentinel**,
         * never a resource, translated only at the point of display.
         * pourquoi : docs/decisions/identite-et-dumps.md § Le pseudo est contraint là où il est saisi
         */
        const val DEFAULT_NAME = "Joueur"
        const val MAX_NAME_LENGTH = 20

        /**
         * Azahar's netplay form rejects a pseudo shorter than this. Enforced
         * where the name is *entered*, and **observed on the device** — the
         * validator lives in Azahar's DEX and its message omits the number.
         * pourquoi : docs/decisions/identite-et-dumps.md § Le pseudo est contraint là où il est saisi
         */
        const val MIN_NAME_LENGTH = 4
    }
}

/**
 * Local store: no account, no server-side profile, and the picture never leaves
 * the device. Durable but **device-bound** — a reinstall is a new person.
 * pourquoi : docs/decisions/identite-et-dumps.md § Le code d'ami *est* l'identité, et il est public par conception
 */
class ProfileStore(context: Context) {

    private val prefs = context.getSharedPreferences("emufii_profile", Context.MODE_PRIVATE)
    private val avatarTarget = File(context.filesDir, "avatar.png")
    private val appContext = context.applicationContext

    private val _profile = MutableStateFlow(load())
    val profile: StateFlow<Profile> = _profile.asStateFlow()

    private fun load(): Profile {
        // Early builds stored a UUID here. It was never shown to anyone and
        // nothing durable hangs off it, friends did not exist yet, so a
        // profile carrying one is simply reissued a shareable code.
        val stored = prefs.getString(KEY_ID, null)
        val id = stored?.takeIf { FriendCode.isValid(it) }
            ?: FriendCode.generate().also { prefs.edit { putString(KEY_ID, it) } }
        return Profile(
            id = id,
            name = prefs.getString(KEY_NAME, Profile.DEFAULT_NAME) ?: Profile.DEFAULT_NAME,
            avatarFile = avatarTarget.takeIf { it.exists() }
        )
    }

    /**
     * The backstop for callers that do not go through a form: nothing
     * downstream should wonder whether the stored pseudo is acceptable.
     * pourquoi : docs/decisions/identite-et-dumps.md § Le pseudo est contraint là où il est saisi
     */
    fun setName(name: String) {
        val trimmed = name.trim()
        val usable = if (trimmed.length < Profile.MIN_NAME_LENGTH) "" else trimmed
        val clean = usable.take(Profile.MAX_NAME_LENGTH).ifBlank { Profile.DEFAULT_NAME }
        prefs.edit { putString(KEY_NAME, clean) }
        _profile.value = _profile.value.copy(name = clean)
    }

    /**
     * Copies the picked image into our own storage, downscaled: the picker's
     * SAF grant is not persisted, and a phone photo is 50 megapixels.
     * pourquoi : docs/decisions/identite-et-dumps.md § L'avatar est recopié, jamais référencé
     */
    fun setAvatar(source: Uri): Result<Unit> = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        appContext.contentResolver.openInputStream(source).use {
            BitmapFactory.decodeStream(requireNotNull(it) { "image illisible" }, null, bounds)
        }

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= AVATAR_PX &&
            bounds.outHeight / (sample * 2) >= AVATAR_PX
        ) {
            sample *= 2
        }

        val decoded = appContext.contentResolver.openInputStream(source).use {
            BitmapFactory.decodeStream(
                requireNotNull(it) { "image illisible" },
                null,
                BitmapFactory.Options().apply { inSampleSize = sample }
            )
        } ?: error("format d'image non reconnu")

        avatarTarget.outputStream().use { out ->
            decoded.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        decoded.recycle()

        // New File instance so Compose sees a changed value even though the
        // path is identical, otherwise the picture updates only on restart.
        _profile.value = _profile.value.copy(avatarFile = File(avatarTarget.path))
    }

    fun clearAvatar() {
        avatarTarget.delete()
        _profile.value = _profile.value.copy(avatarFile = null)
    }

    /**
     * Erase this identity and start over. The new code is unrelated, which is
     * the point — and it also cuts you off from your own friends list.
     * pourquoi : docs/decisions/identite-et-dumps.md § Le code d'ami *est* l'identité, et il est public par conception
     */
    fun reset() {
        avatarTarget.delete()
        prefs.edit { clear() }
        _profile.value = load()
    }

    private companion object {
        const val KEY_ID = "id"
        const val KEY_NAME = "name"

        /** Generous for the largest place an avatar is drawn (104dp on a dense screen). */
        const val AVATAR_PX = 512
    }
}

/** Up to two letters standing in for a player with no picture. */
fun initialsFor(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(2).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}

/**
 * Index into a caller-supplied palette, stable for a given name so a player
 * keeps the same colour between sessions.
 */
fun avatarPaletteFor(name: String, paletteSize: Int): Int =
    if (paletteSize <= 0) 0 else abs(name.hashCode()) % paletteSize
