package eu.emufii.app.ps2

import org.junit.Assert.assertEquals
import org.junit.Test

class Ps2FolderCardImportTest {

    private fun bytes(s: String) = s.toByteArray()

    /** The real index ARMSX2 wrote for a Midnight Club 3 save, Thor, 2026-08-23. */
    private val realIndex = """
        {${'$'}ROOT: {timeCreated: 1787559914,timeModified: 1787559916},
        file01: {order: 1,timeCreated: 1787559914,timeModified: 1787559914},
        iconview.ico: {order: 2,timeCreated: 1787559915,timeModified: 1787559915},
        BESLES-53717joww: {order: 3,timeCreated: 1787559915,timeModified: 1787559916},
        icon.sys: {order: 4,timeCreated: 1787559916,timeModified: 1787559916}}
    """.trimIndent()

    @Test
    fun `orders files the way the index says and drops the index itself`() {
        val files = mapOf(
            "icon.sys" to bytes("d"),
            "_pcsx2_index" to bytes(realIndex),
            "BESLES-53717joww" to bytes("c"),
            "file01" to bytes("a"),
            "iconview.ico" to bytes("b"),
        )
        val ordered = Ps2FolderCardImport.order(realIndex, files)
        assertEquals(
            listOf("file01", "iconview.ico", "BESLES-53717joww", "icon.sys"),
            ordered.map { it.first },
        )
    }

    @Test
    fun `a file the index forgot is kept, after the ordered ones, by name`() {
        val index = "{\$ROOT: {timeCreated: 1},b: {order: 1,timeCreated: 1}}"
        val files = mapOf(
            "z" to bytes("z"),
            "b" to bytes("b"),
            "a" to bytes("a"),
        )
        assertEquals(
            listOf("b", "a", "z"),
            Ps2FolderCardImport.order(index, files).map { it.first },
        )
    }

    @Test
    fun `no index at all still yields every file, in a stable order`() {
        val files = mapOf("b" to bytes("b"), "a" to bytes("a"))
        assertEquals(listOf("a", "b"), Ps2FolderCardImport.order(null, files).map { it.first })
    }

    @Test
    fun `the bookkeeping file never reaches the card`() {
        val files = mapOf("_pcsx2_index" to bytes("{}"), "save" to bytes("s"))
        val ordered = Ps2FolderCardImport.order("{}", files)
        assertEquals(listOf("save"), ordered.map { it.first })
    }

    @Test
    fun `file contents travel unchanged`() {
        val payload = ByteArray(300) { (it % 256).toByte() }
        val ordered = Ps2FolderCardImport.order(null, mapOf("file01" to payload))
        assertEquals(1, ordered.size)
        assertEquals(payload.toList(), ordered.single().second.toList())
    }
}
