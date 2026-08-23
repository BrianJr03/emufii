package eu.emufii.app.ps2

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import eu.emufii.app.azahar.NetplayPlan
import eu.emufii.app.library.Ps2DiscIdentity
import eu.emufii.app.session.RomRef
import eu.emufii.app.wg.WgConfig

/** The ARMSX2 per-game layer EmuFii can configure without private app access. */
object Ps2GameSettings {

    sealed interface Outcome {
        data class Success(val filename: String) : Outcome
        data object MissingFolderGrant : Outcome
        data object MissingPreparedCard : Outcome
        data object UnknownDiscIdentity : Outcome
        data class WriteFailed(val detail: String) : Outcome
    }

    fun canConfigure(context: Context, rom: RomRef): Boolean =
        identity(rom) != null &&
            Ps2NetworkProfile.rootUri(context) != null &&
            Ps2NetworkProfile.receipt(context) != null

    /**
     * Merges only EmuFii's keys and leaves every graphics, speedhack and patch
     * override untouched. ARMSX2 loads this layer after its global preferences,
     * so both network and Slot 1 apply to this boot without changing any other
     * game or navigating its UI.
     */
    fun apply(context: Context, rom: RomRef, plan: NetplayPlan): Outcome = runCatching {
        val identity = identity(rom) ?: return Outcome.UnknownDiscIdentity
        val rootUri = Ps2NetworkProfile.rootUri(context) ?: return Outcome.MissingFolderGrant
        val receipt = Ps2NetworkProfile.receipt(context) ?: return Outcome.MissingPreparedCard
        val root = DocumentFile.fromTreeUri(context, rootUri)?.takeIf { it.isDirectory && it.canWrite() }
            ?: return Outcome.MissingFolderGrant
        val memcards = root.child("memcards") ?: return Outcome.MissingPreparedCard
        if (memcards.child(receipt.cardName)?.isFile != true) return Outcome.MissingPreparedCard
        val settings = root.child("gamesettings")
            ?: root.createDirectory("gamesettings")
            ?: return Outcome.WriteFailed("ARMSX2 did not create gamesettings")
        if (!settings.canWrite()) return Outcome.MissingFolderGrant

        val filename = identity.settingsFilename
        val target = settings.child(filename)
        val original = target?.let { readText(context, it) }.orEmpty()
        val room = plan.password.orEmpty().filter { it.isLetterOrDigit() && it.code < 128 }
            .take(Ps2Target.ROOM_CODE_LENGTH.last)
            .takeIf { it.length >= Ps2Target.ROOM_CODE_LENGTH.first }
            ?: return Outcome.WriteFailed("the ARMSX2 room code is invalid")
        val host = plan.role == NetplayPlan.Role.Host
        val merged = merge(
            original,
            linkedMapOf(
                "DEV9/Eth" to linkedMapOf(
                    "EthEnable" to "true",
                    "EthApi" to "Local Link",
                    "LocalLinkHost" to host.toString(),
                    // ARMSX2 ignores this in host mode. Removing a value left by
                    // a previous guest keeps the file an exact description.
                    "LocalLinkAddress" to if (host) null else WgConfig.PS2_HOST_NAME,
                    "LocalLinkPort" to plan.port.toString(),
                    "LocalLinkRoomCode" to room,
                ),
                "MemoryCards" to linkedMapOf(
                    "Slot1_Enable" to "true",
                    "Slot1_Filename" to receipt.cardName,
                ),
            ),
        )

        // Verify a staging document before touching an existing game override;
        // some providers acknowledge a write and then publish a short file.
        val tempName = ".emufii-${identity.serial}-${identity.elfCrc}.tmp"
        settings.child(tempName)?.delete()
        val temp = settings.createFile("text/plain", tempName)
            ?: return Outcome.WriteFailed("ARMSX2 did not create the staging settings file")
        if (!writeAndVerify(context, temp, merged)) {
            temp.delete()
            return Outcome.WriteFailed("the staging settings file did not verify")
        }

        val published = target ?: settings.createFile("text/plain", filename)
            ?: run {
                temp.delete()
                return Outcome.WriteFailed("ARMSX2 did not create $filename")
            }
        val verified = writeAndVerify(context, published, merged)
        temp.delete()
        if (!verified) return Outcome.WriteFailed("$filename did not verify after writing")
        Outcome.Success(filename)
    }.getOrElse { Outcome.WriteFailed(it.message ?: it.javaClass.simpleName) }

    internal fun identity(rom: RomRef): Ps2DiscIdentity? = identity(rom.productCode, rom.ps2ElfCrc)

    internal fun identity(productCode: String?, elfCrc: String?): Ps2DiscIdentity? {
        val serial = productCode?.uppercase()?.takeIf { it.matches(SERIAL) } ?: return null
        val crc = elfCrc?.uppercase()?.takeIf { it.matches(CRC) } ?: return null
        return Ps2DiscIdentity(serial, crc)
    }

    /**
     * Sparse INI merge. A null value removes a key EmuFii owns; unknown lines,
     * comments, blank lines, sections and their order are preserved verbatim.
     */
    internal fun merge(
        original: String,
        changes: LinkedHashMap<String, LinkedHashMap<String, String?>>,
    ): String {
        val newline = if (original.contains("\r\n")) "\r\n" else "\n"
        val lines = original.replace("\r\n", "\n").split('\n').toMutableList()
        if (lines.size == 1 && lines[0].isEmpty()) lines.clear()

        for ((section, wanted) in changes) {
            var start = lines.indexOfFirst { sectionName(it)?.equals(section, true) == true }
            if (start < 0) {
                if (lines.isNotEmpty() && lines.last().isNotBlank()) lines += ""
                lines += "[$section]"
                start = lines.lastIndex
            }
            var end = (start + 1 until lines.size).firstOrNull { sectionName(lines[it]) != null }
                ?: lines.size

            for ((key, value) in wanted) {
                val existing = (start + 1 until end).firstOrNull { lineKey(lines[it])?.equals(key, true) == true }
                if (existing != null) {
                    if (value == null) {
                        lines.removeAt(existing)
                        end--
                    } else {
                        lines[existing] = "$key = $value"
                    }
                } else if (value != null) {
                    lines.add(end, "$key = $value")
                    end++
                }
            }
        }
        return lines.joinToString(newline).trimEnd() + newline
    }

    private fun sectionName(line: String): String? {
        val trimmed = line.trim()
        return trimmed.takeIf { it.length >= 2 && it.first() == '[' && it.last() == ']' }
            ?.substring(1, trimmed.length - 1)?.trim()
    }

    private fun lineKey(line: String): String? {
        val trimmed = line.trimStart()
        if (trimmed.startsWith(';') || trimmed.startsWith('#')) return null
        val equals = trimmed.indexOf('=')
        if (equals <= 0) return null
        return trimmed.substring(0, equals).trim().takeIf { it.isNotEmpty() }
    }

    private fun DocumentFile.child(name: String): DocumentFile? =
        listFiles().firstOrNull { it.name.equals(name, ignoreCase = true) }

    private fun readText(context: Context, file: DocumentFile): String =
        context.contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() }.orEmpty()

    private fun writeAndVerify(context: Context, file: DocumentFile, text: String): Boolean {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val resolver = context.contentResolver
        resolver.openOutputStream(file.uri, "wt")?.use { it.write(bytes) } ?: return false
        val readBack = resolver.openInputStream(file.uri)?.use { it.readBytes() } ?: return false
        return readBack.contentEquals(bytes)
    }

    private val SERIAL = Regex("^[A-Z0-9]{4}-[A-Z0-9]{5}$")
    private val CRC = Regex("^[0-9A-F]{8}$")
}
