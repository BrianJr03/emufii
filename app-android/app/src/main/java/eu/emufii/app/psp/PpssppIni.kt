package eu.emufii.app.psp

import java.util.Locale

/** The four PPSSPP values owned by Emufii while a private session is launched. */
internal val PRIVATE_NETWORK = linkedMapOf(
    "EnableWlan" to "True",
    "EnableAdhocServer" to "True",
    "proAdhocServer" to HOST_SENTINEL,
    // PPSSPP: Auto = 0, AlwaysOn = 1, AlwaysOff = 2. VPN play needs P2P mode.
    "AdhocServerRelayMode" to "2",
)

internal data class PpssppIniValue(val present: Boolean, val value: String = "")

internal data class PpssppNetworkSnapshot(
    val fileExisted: Boolean,
    val values: Map<String, PpssppIniValue>,
)

/**
 * The part of PPSSPP's INI format Emufii needs.
 *
 * It is deliberately a line-preserving editor rather than a general INI
 * serializer. PPSSPP owns these files: comments, unknown settings, section
 * order and the player's line endings must survive our four-key change.
 */
internal object PpssppIni {
    private val section = Regex("^\\s*\\[([^]]+)]\\s*(?:[;#].*)?$")
    private val assignment = Regex("^\\s*([^;#][^=]*?)\\s*=\\s*(.*)$")
    private val discId = Regex("(?i)(?<![A-Z0-9])([A-Z]{4}[0-9]{5})(?![A-Z0-9])")

    fun privateConfig(source: String): String = rewrite(
        source,
        PRIVATE_NETWORK.mapValues { (_, value) -> value },
    )

    fun snapshot(source: String, fileExisted: Boolean): PpssppNetworkSnapshot {
        val found = readNetwork(source)
        return PpssppNetworkSnapshot(
            fileExisted = fileExisted,
            values = PRIVATE_NETWORK.keys.associateWith { key ->
                found[key.lowercase(Locale.ROOT)]?.let { PpssppIniValue(true, it) }
                    ?: PpssppIniValue(false)
            },
        )
    }

    fun restore(source: String, original: PpssppNetworkSnapshot): String = rewrite(
        source,
        PRIVATE_NETWORK.keys.associateWith { key ->
            original.values[key]?.takeIf(PpssppIniValue::present)?.value
        },
    )

    /** True when deleting an Emufii-created file cannot discard another setting. */
    fun hasNoAssignments(source: String): Boolean = source
        .lineSequence()
        .none { assignment.matchEntire(it) != null }

    /**
     * PPSSPP names a game config from DISC_ID, for example ULUS10277.
     *
     * ISO and PBP metadata gives Emufii `PSP-<DISC_ID>`. Compressed containers
     * currently do not, so accept the conventional ID in the filename too, but
     * never invent one from an unrelated run of letters and digits.
     */
    fun resolveDiscId(productCode: String?, filename: String?, displayName: String?): String? {
        val fromProduct = productCode
            ?.trim()
            ?.uppercase(Locale.ROOT)
            ?.removePrefix("PSP-")
            ?.takeIf { discId.matches(it) }
        return fromProduct
            ?: sequenceOf(filename, displayName)
                .filterNotNull()
                .mapNotNull {
                    discId.find(it)?.groupValues?.get(1)?.uppercase(Locale.ROOT)
                }
                .firstOrNull()
    }

    private fun readNetwork(source: String): Map<String, String> {
        val lines = source.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        var inNetwork = false
        val out = linkedMapOf<String, String>()
        for (line in lines) {
            section.matchEntire(line)?.let { header ->
                inNetwork = header.groupValues[1].trim().equals("Network", ignoreCase = true)
                continue
            }
            if (!inNetwork) continue
            val match = assignment.matchEntire(line) ?: continue
            val key = match.groupValues[1].trim().lowercase(Locale.ROOT)
            if (PRIVATE_NETWORK.keys.any { it.equals(key, ignoreCase = true) }) {
                // PPSSPP's parser resolves duplicates in file order. Keeping the
                // last value here mirrors the value the emulator sees.
                out[key] = match.groupValues[2]
            }
        }
        return out
    }

    /** Null values remove a key; non-null values set exactly one canonical line. */
    private fun rewrite(source: String, desired: Map<String, String?>): String {
        val newline = if (source.contains("\r\n")) "\r\n" else "\n"
        val trailingNewline = source.endsWith('\n') || source.endsWith('\r')
        val normalized = source.replace("\r\n", "\n").replace('\r', '\n')
        val body = normalized.trimEnd('\n')
        val lines = if (body.isEmpty()) mutableListOf() else body.split('\n').toMutableList()

        val networkStarts = lines.indices.filter { index ->
            val line = lines[index]
            section.matchEntire(line)?.groupValues?.get(1)?.trim()
                ?.equals("Network", ignoreCase = true) == true
        }
        if (networkStarts.isEmpty()) {
            val additions = desired.mapNotNull { (key, value) -> value?.let { "$key = $it" } }
            if (additions.isEmpty()) return source
            if (lines.isNotEmpty() && lines.last().isNotBlank()) lines += ""
            lines += "[Network]"
            lines += additions
            return lines.joinToString(newline) + if (trailingNewline) newline else ""
        }

        // An ordinary PPSSPP file has one [Network] section. A hand-edited file
        // can have more, and a later section can otherwise override values we
        // wrote into the first. Remove owned keys from every earlier section and
        // write one effective copy into the last section.
        val finalNetworkStart = networkStarts.last()
        val finalNetworkEnd = (finalNetworkStart + 1 until lines.size).firstOrNull { index ->
            section.matchEntire(lines[index]) != null
        } ?: lines.size

        val canonical = desired.keys.associateBy { it.lowercase(Locale.ROOT) }
        val written = mutableSetOf<String>()
        val out = ArrayList<String>(lines.size + desired.size)
        var inNetwork = false
        var inFinalNetwork = false
        for (index in lines.indices) {
            if (index == finalNetworkEnd) {
                appendMissing(out, desired, written)
            }
            val line = lines[index]
            section.matchEntire(line)?.let { header ->
                inNetwork = header.groupValues[1].trim().equals("Network", ignoreCase = true)
                inFinalNetwork = index == finalNetworkStart
            }
            if (inNetwork && index !in networkStarts) {
                val match = assignment.matchEntire(line)
                val lowered = match?.groupValues?.get(1)?.trim()?.lowercase(Locale.ROOT)
                val key = lowered?.let(canonical::get)
                if (key != null) {
                    // One deterministic line even when a hand-edited file had
                    // duplicates. Earlier [Network] sections cannot override
                    // it, and a null desired value removes every occurrence.
                    if (inFinalNetwork && written.add(key)) {
                        desired[key]?.let { out += "$key = $it" }
                    }
                    continue
                }
            }
            out += line
        }
        if (finalNetworkEnd == lines.size) appendMissing(out, desired, written)
        return out.joinToString(newline) + if (trailingNewline) newline else ""
    }

    private fun appendMissing(
        out: MutableList<String>,
        desired: Map<String, String?>,
        written: Set<String>,
    ) {
        desired.forEach { (key, value) ->
            if (key !in written && value != null) out += "$key = $value"
        }
    }
}
