package eu.emufii.app.ps2

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.security.MessageDigest

/**
 * Provisions a PS2 network profile inside the public ARMSX2 data folder.
 *
 * The active card is never edited. It is read, cloned in memory, patched, and
 * published under a new filename only after the complete result has been read
 * back and verified. The original is also copied into `emufii-backups`; this is
 * deliberately outside `memcards`, so ARMSX2 cannot mount the backup by mistake.
 */
object Ps2Armsx2Folder {

    data class Prepared(
        val rootUri: String,
        val cardName: String,
        val sourceCardName: String?,
        val backupName: String?,
        val cardSha256: String,
        val consoleIdHex: String,
        val identitySource: IdentitySource,
        val biosName: String?,
        val biosVersion: Int?,
        val gameOverrideCount: Int,
        /** Set when the player's card is a folder one, so nothing was cloned. */
        val folderCardName: String?,
        /** Saves copied off that folder card onto the published one. */
        val importedSaveCount: Int,
        /** Saves that did not fit; the card is published anyway. */
        val savesLeftBehind: Int,
        val slot2AlreadyPreserved: Boolean,
        val sourceCardForSlot2: String?,
    )

    enum class IdentitySource { ARMSX2_DEFAULT, NVM }

    sealed interface Outcome {
        data class Success(val prepared: Prepared) : Outcome
        data object NotArmsx2Folder : Outcome
        data object MissingWritePermission : Outcome
        data class InvalidMemoryCard(val name: String, val detail: String) : Outcome
        data class SourceChanged(val name: String) : Outcome
        data class AmbiguousBios(val candidates: List<String>) : Outcome
        data class BiosUnavailable(val name: String) : Outcome
        data class BiosUnreadable(val name: String) : Outcome
        data class WriteFailed(val detail: String) : Outcome
    }

    /**
     * La carte preparee est-elle toujours celle de l'emplacement 1, et porte-t-elle
     * toujours notre profil ?
     *
     * **Ne compare surtout pas la carte octet par octet** : une carte memoire est
     * un disque vivant, et une sauvegarde de jeu suffirait a declarer la
     * preparation perdue.
     * pourquoi : docs/decisions/ps2-carte-memoire.md § Une carte prête ne se vérifie pas octet par octet
     */
    fun isStillValid(
        context: Context,
        rootUri: Uri,
        cardName: String,
        expectedConsoleIdHex: String,
    ): Boolean = runCatching {
        val root = DocumentFile.fromTreeUri(context, rootUri)?.takeIf { it.isDirectory }
            ?: return false
        val loaded = loadSettings(context, root)
        if (!loaded.settings.slot1Enabled ||
            !loaded.settings.slot1Filename.equals(cardName, ignoreCase = true)
        ) return false
        isPreparedCardValid(context, rootUri, cardName, expectedConsoleIdHex)
    }.getOrDefault(false)

    /**
     * The generated card still exists and still carries the profile for this
     * BIOS. It no longer has to be the global Slot 1: EmuFii names it in the
     * per-game settings file immediately before ARMSX2 boots the selected game.
     */
    fun isPreparedCardValid(
        context: Context,
        rootUri: Uri,
        cardName: String,
        expectedConsoleIdHex: String,
    ): Boolean = runCatching {
        val root = DocumentFile.fromTreeUri(context, rootUri)?.takeIf { it.isDirectory }
            ?: return false
        val card = root.child("memcards")?.child(cardName)?.takeIf { it.isFile } ?: return false
        val onCard = Ps2CardPatch.recoverConsoleId(readBytes(context, card)) ?: return false
        if (!onCard.toHex().equals(expectedConsoleIdHex, ignoreCase = true)) return false
        val loaded = loadSettings(context, root)
        val identity = resolveIdentity(context, root, loaded.biosSetting) as? IdentityResult.Found ?: return false
        identity.consoleId.toHex().equals(expectedConsoleIdHex, ignoreCase = true)
    }.getOrDefault(false)

    fun prepare(context: Context, rootUri: Uri, saveTitle: String): Outcome = runCatching {
        val root = DocumentFile.fromTreeUri(context, rootUri)
            ?.takeIf { it.isDirectory } ?: return Outcome.NotArmsx2Folder
        val memcards = root.child("memcards")
            ?.takeIf { it.isDirectory } ?: return Outcome.NotArmsx2Folder
        if (!root.canWrite() || !memcards.canWrite()) return Outcome.MissingWritePermission

        val loaded = loadSettings(context, root)
        val settings = loaded.settings

        val identity = resolveIdentity(context, root, loaded.biosSetting)
        if (identity is IdentityResult.Error) return identity.outcome
        identity as IdentityResult.Found

        val slot1 = memcards.child(settings.slot1Filename)
        val slot2 = memcards.child(settings.slot2Filename)
        // Enabled slots first, then whatever the two name at all, so a card the
        // player has configured wins over one merely present. A folder memory
        // card cannot be cloned, but finding one does not end the search: the
        // other slot may hold a plain image, and stopping at the first slot
        // refused players a card that was sitting right there.
        val candidates = listOfNotNull(
            slot1?.takeIf { settings.slot1Enabled },
            slot2?.takeIf { settings.slot2Enabled },
            slot1,
            slot2,
        )
        // A card we published is not the player's card, and preferring theirs is
        // the whole reason the second run behaves like the first. Without this
        // order, the run after a successful setup finds our own generated card
        // sitting in slot 1, takes it as the source, clones it, and never looks
        // at the folder card in slot 2 — so the save import silently did
        // nothing at all. Measured on the Thor, 2026-08-23.
        //
        // Ours still comes last rather than never: when a player's own image was
        // cloned and then unassigned, that clone is the only place their saves
        // live, and dropping it would regenerate an empty card over them.
        val ours = { f: DocumentFile -> f.name?.startsWith(TARGET_STEM, ignoreCase = true) == true }
        val theirImage = candidates.firstOrNull { it.isFile && !ours(it) }
        val folderCard = if (theirImage == null) candidates.firstOrNull { it.isDirectory } else null
        val source = theirImage
            ?: if (folderCard == null) candidates.firstOrNull { it.isFile } else null
        val preserved = source ?: folderCard
        val sourceWasActiveSlot2 = preserved != null && settings.slot2Enabled &&
            preserved.name.equals(settings.slot2Filename, ignoreCase = true)
        val sourceBytes = source?.let { readBytes(context, it) }
        var patched = try {
            if (sourceBytes != null) {
                Ps2CardPatch.inject(
                    sourceBytes,
                    identity.consoleId,
                    epochSecond = PROFILE_EPOCH_SECOND,
                    saveTitle = saveTitle,
                )
            } else {
                Ps2MemoryCard.generate(saveTitle, identity.consoleId, PROFILE_EPOCH_SECOND)
            }
        } catch (e: Ps2CardPatch.CardFormatException) {
            return Outcome.InvalidMemoryCard(settings.slot1Filename, e.message.orEmpty())
        }

        // A folder card's saves are copied onto the card we publish, because a
        // folder card left in slot 2 is indexed with an empty filter and the
        // game sees nothing in it. See [Ps2FolderCardImport] for the measurement.
        // The player's folder is only ever read; it is not moved, emptied or
        // rewritten, so this stays a copy in one direction.
        var importedSaves = 0
        var savesLeftBehind = 0
        Log.d(TAG, "source=${source?.name} carteDossier=${folderCard?.name} " +
            "slot1=${settings.slot1Filename}(${settings.slot1Enabled}) " +
            "slot2=${settings.slot2Filename}(${settings.slot2Enabled})")
        if (folderCard != null) {
            for (save in readFolderCardSaves(context, folderCard)) {
                patched = try {
                    Ps2CardPatch.addSave(patched, save.directory, save.files, PROFILE_EPOCH_SECOND)
                } catch (e: Ps2CardPatch.CardFormatException) {
                    // A full card is not a failure of the whole preparation: the
                    // network profile is already on it and the player can still
                    // play. Count what did not fit and say so, rather than
                    // throwing away a card that works.
                    savesLeftBehind = 1
                    break
                }
                importedSaves++
            }
            Log.d(TAG, "$importedSaves sauvegarde(s) écrite(s) sur la carte")
            if (savesLeftBehind > 0) {
                savesLeftBehind = readFolderCardSaves(context, folderCard).size - importedSaves
            }
        }

        // A paused/running VM may still have the file open. Never publish a
        // snapshot if its source changed while we were reading and rebuilding
        // it: that could be a torn save even though the filesystem still parses.
        if (source != null && sourceBytes != null &&
            sha256(sourceBytes) != runCatching { sha256(readBytes(context, source)) }.getOrNull()
        ) {
            return Outcome.SourceChanged(source.name ?: settings.slot1Filename)
        }

        val digest = sha256(patched)
        val existing = memcards.listFiles().firstOrNull { candidate ->
            candidate.isFile && candidate.extensionLower() == "ps2" &&
                candidate.name?.startsWith(TARGET_STEM, ignoreCase = true) == true &&
                runCatching { sha256(readBytes(context, candidate)) == digest }.getOrDefault(false)
        }
        val target = existing ?: publishVerified(context, memcards, patched, identity.consoleId)
            ?: return Outcome.WriteFailed("ARMSX2 did not publish the verified temporary card")

        val backupName = if (source != null && sourceBytes != null) {
            backupSource(context, root, source.name ?: settings.slot1Filename, sourceBytes)
                ?: return Outcome.WriteFailed("the immutable source backup could not be written")
        } else null

        Outcome.Success(
            Prepared(
                rootUri = rootUri.toString(),
                cardName = target.name ?: return Outcome.WriteFailed("the published card has no filename"),
                sourceCardName = source?.name,
                backupName = backupName,
                cardSha256 = digest,
                consoleIdHex = identity.consoleId.toHex(),
                identitySource = identity.source,
                biosName = identity.biosName,
                biosVersion = identity.biosVersion,
                gameOverrideCount = settings.gameOverrides.size,
                folderCardName = folderCard?.name,
                importedSaveCount = importedSaves,
                savesLeftBehind = savesLeftBehind,
                slot2AlreadyPreserved = sourceWasActiveSlot2,
                sourceCardForSlot2 = preserved?.name?.takeIf { !settings.slot2Enabled },
            )
        )
    }.getOrElse { Outcome.WriteFailed(it.message ?: it.javaClass.simpleName) }

    private sealed interface IdentityResult {
        data class Found(
            val consoleId: ByteArray,
            val source: IdentitySource,
            val biosName: String?,
            val biosVersion: Int?,
        ) : IdentityResult
        data class Error(val outcome: Outcome) : IdentityResult
    }

    private data class LoadedSettings(
        val settings: Ps2Armsx2Settings.Parsed,
        val biosSetting: String?,
    )

    private fun loadSettings(context: Context, root: DocumentFile): LoadedSettings {
        val json = root.child("armsx2-settings.json")?.takeIf { it.isFile }
            ?.let { readText(context, it) }?.let(Ps2Armsx2Settings::parse)
        val ini = root.child("PCSX2-Android.ini")?.takeIf { it.isFile }
            ?.let { readText(context, it) }?.let(Ps2Armsx2Settings::parseIni)
        val settings = json ?: ini ?: Ps2Armsx2Settings.Parsed()
        return LoadedSettings(settings, settings.biosFilename ?: ini?.biosFilename)
    }

    private fun resolveIdentity(context: Context, root: DocumentFile, configuredBios: String?): IdentityResult {
        val files = root.child("bios")?.takeIf { it.isDirectory }?.listFiles()?.filter { it.isFile }.orEmpty()
        val configuredName = configuredBios?.let { File(it).name }?.takeIf { it.isNotBlank() }
        val biosFiles = files.filterNot { it.extensionLower() in COMPANION_EXTENSIONS }
        val nvmFiles = files.filter { it.extensionLower() == "nvm" }
        if (biosFiles.size > 1) {
            return IdentityResult.Error(Outcome.AmbiguousBios(biosFiles.mapNotNull { it.name }))
        }
        val bios = biosFiles.singleOrNull()
            ?: return IdentityResult.Error(Outcome.BiosUnavailable(configuredName ?: "active BIOS"))
        if (configuredName != null && !bios.name.equals(configuredName, ignoreCase = true)) {
            return IdentityResult.Error(Outcome.BiosUnavailable(configuredName))
        }
        val nvm = nvmFiles.firstOrNull { it.stem().equals(bios.stem(), ignoreCase = true) }
        if (nvm == null) {
            return IdentityResult.Found(
                Ps2NetcnfConfig.ARMSX2_CONSOLE_ID.copyOf(),
                IdentitySource.ARMSX2_DEFAULT,
                bios.name,
                runCatching { Ps2Bios.version(readBytes(context, bios)) }.getOrNull(),
            )
        }
        val biosBytes = runCatching { readBytes(context, bios) }.getOrNull()
            ?: return IdentityResult.Error(Outcome.BiosUnreadable(bios.name.orEmpty()))
        val version = Ps2Bios.version(biosBytes)
            ?: return IdentityResult.Error(Outcome.BiosUnreadable(bios.name.orEmpty()))
        val nvmBytes = runCatching { readBytes(context, nvm) }.getOrNull()
            ?: return IdentityResult.Error(Outcome.BiosUnreadable(nvm.name.orEmpty()))
        val consoleId = Ps2NetcnfConfig.effectiveIlinkIdFromNvm(
            nvmBytes,
            Ps2NetcnfConfig.BiosVersion(version ushr 8, version and 0xFF),
        )
        return IdentityResult.Found(
            consoleId,
            if (consoleId.contentEquals(Ps2NetcnfConfig.ARMSX2_CONSOLE_ID)) {
                IdentitySource.ARMSX2_DEFAULT
            } else IdentitySource.NVM,
            bios.name,
            version,
        )
    }

    private fun publishVerified(
        context: Context,
        memcards: DocumentFile,
        bytes: ByteArray,
        consoleId: ByteArray,
    ): DocumentFile? {
        val finalName = availableTargetName(memcards)
        val tempName = "$finalName.part"
        memcards.child(tempName)?.delete()
        val temp = memcards.createFile(MIME, tempName) ?: return null
        if (!writeBytes(context, temp, bytes)) {
            temp.delete()
            return null
        }
        val verified = runCatching {
            val reread = readBytes(context, temp)
            sha256(reread) == sha256(bytes) &&
                Ps2CardPatch.recoverConsoleId(reread)?.contentEquals(consoleId) == true
        }.getOrDefault(false)
        if (!verified || !temp.renameTo(finalName)) {
            temp.delete()
            return null
        }
        return memcards.child(finalName)
    }

    private fun backupSource(context: Context, root: DocumentFile, sourceName: String, bytes: ByteArray): String? {
        val folder = root.child(BACKUP_DIR) ?: root.createDirectory(BACKUP_DIR) ?: return null
        val safeStem = sourceName.substringBeforeLast('.').replace(Regex("[^A-Za-z0-9._-]"), "_")
        val name = "$safeStem-${sha256(bytes).take(12)}.ps2"
        if (folder.child(name) != null) return name
        val target = folder.createFile(MIME, name) ?: return null
        return if (writeBytes(context, target, bytes) &&
            runCatching { sha256(readBytes(context, target)) == sha256(bytes) }.getOrDefault(false)
        ) name else {
            target.delete()
            null
        }
    }

    private fun availableTargetName(memcards: DocumentFile): String {
        val names = memcards.listFiles().mapNotNull { it.name }.toSet()
        if (TARGET_NAME !in names) return TARGET_NAME
        var index = 2
        while ("$TARGET_STEM-$index.ps2" in names) index++
        return "$TARGET_STEM-$index.ps2"
    }

    private fun DocumentFile.child(name: String): DocumentFile? =
        listFiles().firstOrNull { it.name.equals(name, ignoreCase = true) }

    private fun DocumentFile.extensionLower(): String =
        name?.substringAfterLast('.', "")?.lowercase().orEmpty()

    private fun DocumentFile.stem(): String = name?.substringBeforeLast('.', name.orEmpty()).orEmpty()

    /**
     * Every save on a folder memory card, its files in card order.
     *
     * Anything unreadable is skipped rather than aborting: a folder card is the
     * player's own directory, and one odd entry in it must not cost them the
     * whole preparation.
     */
    private fun readFolderCardSaves(
        context: Context,
        card: DocumentFile,
    ): List<Ps2FolderCardImport.Save> {
        // Traced, and deliberately kept: this walk reads somebody else's
        // directory through SAF, every failure in it is recoverable by design,
        // and a silent empty result is indistinguishable from an empty card.
        // It cost a full afternoon of guessing on 2026-08-23.
        val entries = card.listFiles()
        Log.d(TAG, "carte dossier ${card.name}: ${entries.size} entrée(s) " +
            entries.joinToString { "${it.name}${if (it.isDirectory) "/" else ""}" })
        return entries
            .filter { it.isDirectory && it.name != null }
            .sortedBy { it.name }
            .mapNotNull { dir ->
                runCatching {
                    val files = dir.listFiles()
                        .filter { it.isFile && it.name != null }
                        .associate { it.name!! to readBytes(context, it) }
                    val index = files[Ps2FolderCardImport.INDEX]?.toString(Charsets.UTF_8)
                    val ordered = Ps2FolderCardImport.order(index, files)
                    Log.d(TAG, "sauvegarde ${dir.name}: ${files.size} fichier(s) lu(s), " +
                        "${ordered.size} retenu(s)")
                    ordered.takeIf { it.isNotEmpty() }
                        ?.let { Ps2FolderCardImport.Save(dir.name!!, it) }
                }.onFailure { Log.w(TAG, "sauvegarde ${dir.name} illisible, ignorée", it) }
                    .getOrNull()
            }
    }

    private fun readText(context: Context, file: DocumentFile): String =
        context.contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() }
            ?: error("cannot read ${file.name}")

    private fun readBytes(context: Context, file: DocumentFile): ByteArray =
        context.contentResolver.openInputStream(file.uri)?.use { it.readBytes() }
            ?: error("cannot read ${file.name}")

    private fun writeBytes(context: Context, file: DocumentFile, bytes: ByteArray): Boolean =
        runCatching {
            context.contentResolver.openOutputStream(file.uri, "wt")?.use { it.write(bytes) }
                ?: return false
            true
        }.getOrDefault(false)

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()
    private fun sha256(context: Context, file: DocumentFile): String {
        val digest = MessageDigest.getInstance("SHA-256")
        context.contentResolver.openInputStream(file.uri)?.use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        } ?: error("cannot read ${file.name}")
        return digest.digest().toHex()
    }
    private fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it.toInt() and 0xFF) }

    private const val TAG = "Ps2CardImport"
    private const val TARGET_STEM = "EmuFii-Network"
    private const val TARGET_NAME = "$TARGET_STEM.ps2"
    private const val BACKUP_DIR = "emufii-backups"
    private const val MIME = "application/octet-stream"
    private const val PROFILE_EPOCH_SECOND = 946684800L
    private val COMPANION_EXTENSIONS = setOf("nvm", "mec", "rom1", "rom2", "erom")
}
