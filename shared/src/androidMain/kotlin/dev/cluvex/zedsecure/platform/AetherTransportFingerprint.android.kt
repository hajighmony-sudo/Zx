package dev.cluvex.zedsecure.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

@Suppress("BlockingMethodInNonBlockingContext")
internal actual suspend fun probeAetherTransportFingerprint(): AetherTransportFingerprint =
    withContext(Dispatchers.IO) {
        coroutineScope {
            val udp = async {
                val first = udpDnsProbe("1.1.1.1")
                first || udpDnsProbe("8.8.8.8")
            }
            val tls = async { tlsSniProbe() }
            AetherTransportFingerprint(
                udpReachable = udp.await(),
                tlsSniReachable = tls.await(),
            )
        }
    }

private fun udpDnsProbe(server: String): Boolean = runCatching {
    DatagramSocket().use { socket ->
        socket.soTimeout = PROBE_TIMEOUT_MS
        val query = byteArrayOf(
            0x1A, 0x2B, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            7, 0x65, 0x78, 0x61, 0x6D, 0x70, 0x6C, 0x65, 3, 0x63, 0x6F, 0x6D, 0,
            0x00, 0x01, 0x00, 0x01,
        )
        socket.send(DatagramPacket(query, query.size, InetAddress.getByName(server), 53))
        val buffer = ByteArray(512)
        val response = DatagramPacket(buffer, buffer.size)
        socket.receive(response)
        val data = response.data
        response.length >= 12 &&
            data[0] == 0x1A.toByte() && data[1] == 0x2B.toByte() &&
            (data[2].toInt() and 0x80) != 0 &&
            (data[3].toInt() and 0x0F) == 0 &&
            ((data[6].toInt() and 0xFF) shl 8 or (data[7].toInt() and 0xFF)) > 0
    }
}.getOrDefault(false)

private fun tlsSniProbe(): Boolean = runCatching {
    Socket().use { raw ->
        raw.connect(InetSocketAddress("1.1.1.1", 443), TLS_TIMEOUT_MS)
        raw.soTimeout = TLS_TIMEOUT_MS
        val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
        (factory.createSocket(raw, "www.cloudflare.com", 443, true) as SSLSocket).use { ssl ->
            ssl.soTimeout = TLS_TIMEOUT_MS
            ssl.startHandshake()
            HttpsURLConnection.getDefaultHostnameVerifier()
                .verify("www.cloudflare.com", ssl.session)
        }
    }
}.getOrDefault(false)

private const val PROBE_TIMEOUT_MS = 2_500
private const val TLS_TIMEOUT_MS = 3_500
