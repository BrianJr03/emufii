package eu.emufii.app.ps2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Ps2GameSettingsTest {

    @Test
    fun `merge preserves unknown overrides and replaces only owned keys`() {
        val original = """
            ; tuned by the player
            [EmuCore/GS]
            VsyncEnable = 1

            [DEV9/Eth]
            EthEnable = false
            LocalLinkPeerId = 31337
            LocalLinkAddress = old-host

            [MemoryCards]
            Slot1_Filename = personal.ps2
            Slot2_Filename = second.ps2
        """.trimIndent() + "\n"

        val merged = Ps2GameSettings.merge(
            original,
            linkedMapOf(
                "DEV9/Eth" to linkedMapOf(
                    "EthEnable" to "true",
                    "EthApi" to "Local Link",
                    "LocalLinkAddress" to null,
                    "LocalLinkPort" to "19072",
                ),
                "MemoryCards" to linkedMapOf(
                    "Slot1_Filename" to "EmuFii_Network.ps2",
                ),
            ),
        )

        assertTrue(merged.contains("VsyncEnable = 1"))
        assertTrue(merged.contains("LocalLinkPeerId = 31337"))
        assertTrue(merged.contains("Slot2_Filename = second.ps2"))
        assertTrue(merged.contains("EthEnable = true"))
        assertTrue(merged.contains("EthApi = Local Link"))
        assertTrue(merged.contains("LocalLinkPort = 19072"))
        assertTrue(merged.contains("Slot1_Filename = EmuFii_Network.ps2"))
        assertFalse(merged.contains("LocalLinkAddress"))
    }

    @Test
    fun `missing sections are appended once and merge is idempotent`() {
        val changes: LinkedHashMap<String, LinkedHashMap<String, String?>> = linkedMapOf(
            "DEV9/Eth" to linkedMapOf("EthEnable" to "true"),
            "MemoryCards" to linkedMapOf("Slot1_Enable" to "true"),
        )
        val once = Ps2GameSettings.merge("[UI]\nTheme = dark\n", changes)
        val twice = Ps2GameSettings.merge(once, changes)
        assertEquals(once, twice)
        assertEquals(1, Regex("\\[DEV9/Eth]").findAll(twice).count())
        assertEquals(1, Regex("\\[MemoryCards]").findAll(twice).count())
    }

    @Test
    fun `identity refuses absent or malformed CRC`() {
        assertEquals(null, Ps2GameSettings.identity("SCUS-97481", null))
        assertEquals(null, Ps2GameSettings.identity("SCUS-97481", "1234"))
        assertEquals(
            "SCUS-97481_2F123FD8.ini",
            Ps2GameSettings.identity("SCUS-97481", "2f123fd8")?.settingsFilename,
        )
    }
}
