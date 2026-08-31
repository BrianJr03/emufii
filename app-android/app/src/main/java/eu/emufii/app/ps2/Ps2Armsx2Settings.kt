package eu.emufii.app.ps2

import org.json.JSONObject

/**
 * ARMSX2's settings, read out of the mirror it keeps in its data folder.
 *
 * ARMSX2 keeps its configuration in app-private preferences, which nothing
 * outside it can read, but it also writes a copy beside the memory cards,
 * `armsx2-settings.json` (`ConfigStore.BACKUP_FILENAME`, written on every
 * settings save, read back on reinstall). When the player's ARMSX2 data
 * folder is one another app can reach, that mirror says which files both
 * slots hold, which BIOS is selected, and which games override the global
 * card or BIOS: the facts needed to provision without silently missing a
 * configuration that a game would actually launch with.
 *
 * The `global` object carries flat camelCase keys; absent keys mean ARMSX2 is
 * running its defaults, which is what the fallbacks here reproduce.
 */
object Ps2Armsx2Settings {

    /** A sparse per-game override that can bypass global PS2 provisioning. */
    data class GameOverride(
        val serial: String,
        val slot1Enabled: Boolean? = null,
        val slot1Filename: String? = null,
        val biosFilename: String? = null,
    )

    data class Parsed(
        val slot1Enabled: Boolean = true,
        val slot1Filename: String = "mcd001.ps2",
        val biosFilename: String? = null,
        val slot2Enabled: Boolean = true,
        val slot2Filename: String = "mcd002.ps2",
        val gameOverrides: List<GameOverride> = emptyList(),
    ) {
        val hasRelevantGameOverrides: Boolean get() = gameOverrides.isNotEmpty()
    }

    fun parse(mirrorJson: String): Parsed {
        val root = runCatching { JSONObject(mirrorJson) }.getOrNull() ?: return Parsed()
        val global = root.optJSONObject("global") ?: JSONObject()
        val overrides = mutableListOf<GameOverride>()
        root.optJSONObject("games")?.let { games ->
            val serials = games.keys()
            while (serials.hasNext()) {
                val serial = serials.next()
                val game = games.optJSONObject(serial) ?: continue
                if (!game.has("memoryCardSlot1Enabled") &&
                    !game.has("memoryCardSlot1Filename") && !game.has("biosFilename")
                ) continue
                overrides += GameOverride(
                    serial = serial,
                    slot1Enabled = game.optBoolean("memoryCardSlot1Enabled")
                        .takeIf { game.has("memoryCardSlot1Enabled") },
                    slot1Filename = game.optString("memoryCardSlot1Filename")
                        .takeIf { game.has("memoryCardSlot1Filename") },
                    biosFilename = game.optString("biosFilename")
                        .takeIf { game.has("biosFilename") },
                )
            }
        }
        return Parsed(
            slot1Enabled = global.optBoolean("memoryCardSlot1Enabled", true),
            slot1Filename = global.optString("memoryCardSlot1Filename", "mcd001.ps2")
                .ifEmpty { "mcd001.ps2" },
            biosFilename = global.optString("biosFilename", "").ifEmpty { null },
            slot2Enabled = global.optBoolean("memoryCardSlot2Enabled", true),
            slot2Filename = global.optString("memoryCardSlot2Filename", "mcd002.ps2")
                .ifEmpty { "mcd002.ps2" },
            gameOverrides = overrides,
        )
    }

    /**
     * The native `PCSX2-Android.ini` ARMSX2 keeps at the data root, parsed for
     * the same global facts. Measured on a live install: builds exist that
     * write this file and no JSON mirror, so the folder flow reads whichever
     * of the two is there: the mirror when both are.
     */
    fun parseIni(ini: String): Parsed {
        val values = mutableMapOf<String, String>()
        var section = ""
        for (rawLine in ini.lineSequence()) {
            val line = rawLine.trim()
            if (line.startsWith('[') && line.endsWith(']')) {
                section = line.substring(1, line.length - 1)
                continue
            }
            val equals = line.indexOf('=')
            if (equals <= 0) continue
            val key = line.substring(0, equals).trim()
            val value = line.substring(equals + 1).trim()
            values["$section/$key"] = value
        }
        return Parsed(
            slot1Enabled = values["MemoryCards/Slot1_Enable"]?.lowercase() != "false",
            slot1Filename = values["MemoryCards/Slot1_Filename"]?.ifEmpty { null } ?: "mcd001.ps2",
            biosFilename = values["Filenames/BIOS"]?.ifEmpty { null },
            slot2Enabled = values["MemoryCards/Slot2_Enable"]?.lowercase() != "false",
            slot2Filename = values["MemoryCards/Slot2_Filename"]?.ifEmpty { null } ?: "mcd002.ps2",
        )
    }
}
