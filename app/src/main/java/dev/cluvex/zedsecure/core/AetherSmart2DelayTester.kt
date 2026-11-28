package dev.cluvex.zedsecure.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.HttpURLConnection
import java.net.URL

/**
 * Measures a real HTTPS request through the active Aether SOCKS listener.
 * A failed probe returns null and does not turn a working tunnel into a failure.
 */
internal object AetherSmart2DelayTester {
    private val targets = listOf(
        "https://cp.cloudflare.com/generate_204",
        "https://www.cloudflare.com/cdn-cgi/trace",
    )

    suspend fun measure(socksPort: Int, timeoutMs: Long = 6_000L): Long? = withContext(Dispatchers.IO) {
        if (socksPort !in 1..65535 || timeoutMs <= 0) return@withContext null
        val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", socksPort))
        for (target in targets) {
            val connection = runCatching {
                (URL(target).openConnection(proxy) as HttpURLConnection).apply {
                    connectTimeout = timeoutMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    readTimeout = timeoutMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    instanceFollowRedirects = false
                    useCaches = false
                    requestMethod = "GET"
                }
            }.getOrNull() ?: continue
            try {
                val started = System.nanoTime()
                val code = connection.responseCode
                if (code in 200..299) {
                    runCatching { connection.inputStream.use { it.readBytes() } }
                    return@withContext ((System.nanoTime() - started) / 1_000_000L).coerceAtLeast(1L)
                }
            } catch (_: Exception) {
                // Try the next independent HTTPS endpoint.
            } finally {
                connection.disconnect()
            }
        }
        null
    }
}
