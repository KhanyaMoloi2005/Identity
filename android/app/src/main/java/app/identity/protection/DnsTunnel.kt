package app.identity.protection

import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import app.identity.filter.DnsDecision
import app.identity.filter.DnsFilter
import app.identity.filter.IpPackets
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/**
 * Handles the DNS queries that arrive through the tunnel: blocked names get "no such domain"
 * straight away; search engines are redirected to their safe mode (D-027); the rest are forwarded
 * to the upstream resolver (D-003) and the answer is passed back.
 *
 * One thread reads the tunnel and sleeps (no polling timer) until a packet or [stop] arrives.
 * Forwarding happens on a small thread pool so one slow answer doesn't hold up the others.
 * Nothing here logs domain names.
 */
internal class DnsTunnel(
    private val tunnel: ParcelFileDescriptor,
    /** Builds the filter on the reader thread, because loading the blocklist takes a moment. */
    private val createFilter: () -> DnsFilter,
    /** Marks a socket so its traffic skips our own VPN ([android.net.VpnService.protect]). */
    private val protect: (DatagramSocket) -> Boolean,
) {
    private val output = FileOutputStream(tunnel.fileDescriptor)
    private val forwarders = Executors.newFixedThreadPool(FORWARD_THREADS)

    // Writing a byte into this pipe wakes the reader thread so it can finish.
    private val wakeUp = Os.pipe()
    private val reader = Thread(::readLoop, "identity-dns")

    // Replaced as a whole when the user adds or removes a rule; read by several threads.
    @Volatile private var filter: DnsFilter? = null

    fun start() = reader.start()

    /** Rebuilds the filter after the user's rules change, in the background. Until then the old rules apply. */
    fun reload() = inBackground {
        try {
            filter = createFilter()
        } catch (e: Exception) {
            // Keep the rules already in use.
        }
    }

    fun stop() {
        try {
            Os.write(wakeUp[1], byteArrayOf(1), 0, 1)
        } catch (e: Exception) {
            // The reader has already finished.
        }
        reader.join(STOP_WAIT_MS)
        forwarders.shutdownNow()
        try {
            Os.close(wakeUp[0])
            Os.close(wakeUp[1])
        } catch (e: ErrnoException) {
            // Already closed; nothing to clean up.
        }
    }

    private fun readLoop() {
        // Queries that arrive meanwhile wait in the tunnel and are answered once the list is ready.
        filter = try {
            createFilter()
        } catch (e: Exception) {
            return // no filter: stop answering, so DNS fails closed (D-023)
        }
        val input = FileInputStream(tunnel.fileDescriptor)
        val buffer = ByteArray(MAX_PACKET)
        val tunnelPoll = StructPollfd().apply {
            fd = tunnel.fileDescriptor
            events = OsConstants.POLLIN.toShort()
        }
        val wakePoll = StructPollfd().apply {
            fd = wakeUp[0]
            events = OsConstants.POLLIN.toShort()
        }
        while (true) {
            try {
                tunnelPoll.revents = 0
                wakePoll.revents = 0
                Os.poll(arrayOf(tunnelPoll, wakePoll), -1) // sleeps until something happens
                if (wakePoll.revents.toInt() != 0) return
                val events = tunnelPoll.revents.toInt()
                if ((events and OsConstants.POLLIN) != 0) {
                    val length = input.read(buffer)
                    if (length > 0) handle(buffer, length)
                } else if (events != 0) {
                    return // the tunnel was closed or failed
                }
            } catch (e: ErrnoException) {
                if (e.errno != OsConstants.EINTR) return // EINTR: interrupted by the system; just wait again
            } catch (e: IOException) {
                return
            }
        }
    }

    private fun handle(buffer: ByteArray, length: Int) {
        val query = IpPackets.parseUdp(buffer, length) ?: return
        if (query.destinationPort != DNS_PORT) return
        val current = filter ?: return
        when (val decision = current.decide(query.payload)) {
            is DnsDecision.Answer -> write(query.reply(decision.response))
            DnsDecision.Forward -> inBackground {
                askUpstream(query.payload)?.let { write(query.reply(it)) }
            }
            is DnsDecision.Redirect -> inBackground {
                askUpstream(decision.query)?.let(decision::answer)?.let { write(query.reply(it)) }
            }
            DnsDecision.Drop -> Unit
        }
    }

    private fun inBackground(work: () -> Unit) {
        try {
            forwarders.execute(work)
        } catch (e: RejectedExecutionException) {
            // Stopping; drop it.
        }
    }

    /**
     * Asks each upstream server in turn and returns the first answer. Null if none answers in time;
     * the app that asked then retries by itself.
     */
    private fun askUpstream(message: ByteArray): ByteArray? {
        for (server in UPSTREAM_SERVERS) {
            try {
                DatagramSocket().use { socket ->
                    if (!protect(socket)) return null
                    socket.soTimeout = UPSTREAM_TIMEOUT_MS
                    socket.connect(server) // only accept answers from this server
                    socket.send(DatagramPacket(message, message.size))
                    val answer = DatagramPacket(ByteArray(IpPackets.MAX_UDP_PAYLOAD), IpPackets.MAX_UDP_PAYLOAD)
                    socket.receive(answer)
                    return answer.data.copyOf(answer.length)
                }
            } catch (e: IOException) {
                // Timed out or no network: try the next server.
            }
        }
        return null
    }

    private fun write(packet: ByteArray) {
        synchronized(output) {
            try {
                output.write(packet)
            } catch (e: IOException) {
                // The tunnel closed while the answer was on its way.
            }
        }
    }

    private companion object {
        const val DNS_PORT = 53
        const val MAX_PACKET = 32_767
        const val FORWARD_THREADS = 4
        const val UPSTREAM_TIMEOUT_MS = 5_000
        const val STOP_WAIT_MS = 1_000L

        /** Cloudflare for Families (D-003): a free second filtering layer. Literal addresses, no lookup needed. */
        val UPSTREAM_SERVERS = listOf(
            InetSocketAddress(InetAddress.getByAddress(byteArrayOf(1, 1, 1, 3)), DNS_PORT),
            InetSocketAddress(InetAddress.getByAddress(byteArrayOf(1, 0, 0, 3)), DNS_PORT),
        )
    }
}
