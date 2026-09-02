package eu.emufii.app.wfc

/**
 * Answers the DNS queries Android sends into the tunnel, by asking Kaeru.
 *
 * The DS's WFC settings live in melonDS's private storage and its config screen is
 * inside the emulated console: Emufii cannot set the console's DNS, by file or by UI.
 * melonDS ships in "auto-obtain DNS" mode, where resolution goes through Android's
 * resolver (measured: with auto DNS the console reached Nintendo's dead ELB, only a DNS
 * pointed at Kaeru reached Kaeru). A tunnel that advertises a DNS server therefore
 * decides where the console ends up, with nothing for the player to configure.
 *
 * Parse, forward, wrap the answer; apart from the service so it tests without an emulator.
 */
class DnsRelay(
    private val sentinelAddress: ByteArray,
    private val upstream: Upstream
) {

    /** Whatever actually talks to Kaeru. A socket in production, a fake in tests. */
    fun interface Upstream {
        /** Null on timeout or failure; implementations must not throw. */
        fun exchange(query: ByteArray): ByteArray?
    }

    @Volatile var queriesRelayed: Long = 0
        private set

    @Volatile var queriesDropped: Long = 0
        private set

    @Volatile var upstreamFailures: Long = 0
        private set

    /**
     * The total above cannot tell "one timeout on a busy evening" from "the server is
     * gone", and only the second is worth showing. The run resets on the first answer
     * back, and packets that were never ours to answer do not touch it.
     */
    @Volatile var consecutiveUpstreamFailures: Int = 0
        private set

    /**
     * One raw IP packet in, the raw packet to write back out. Null covers three cases on
     * purpose, not ours, malformed, or upstream said nothing: the caller drops in all
     * three, which is the right default for a tunnel routing a single host.
     */
    fun handle(packet: ByteArray, length: Int = packet.size): ByteArray? {
        val datagram = Ipv4Udp.parse(packet, length)
        if (datagram == null) {
            queriesDropped++
            return null
        }

        val addressedToUs = datagram.destination.contentEquals(sentinelAddress) &&
            datagram.destinationPort == KaeruWfc.DNS_PORT
        if (!addressedToUs) {
            queriesDropped++
            return null
        }

        // A DNS message has a 12-byte header; past the EDNS ceiling it is not ours to relay.
        if (datagram.payload.size < 12 || datagram.payload.size > KaeruWfc.MAX_DNS_MESSAGE) {
            queriesDropped++
            return null
        }

        val answer = upstream.exchange(datagram.payload)
        if (answer == null || answer.size < 12) {
            upstreamFailures++
            consecutiveUpstreamFailures++
            return null
        }

        queriesRelayed++
        consecutiveUpstreamFailures = 0

        return Ipv4Udp.build(
            source = datagram.destination,
            destination = datagram.source,
            sourcePort = KaeruWfc.DNS_PORT,
            destinationPort = datagram.sourcePort,
            payload = answer,
            // The transaction id is inside the DNS payload; the IP id need not be unique
            // for a packet that is never fragmented.
            identification = Ipv4Udp.readShort(answer, 0)
        )
    }
}
