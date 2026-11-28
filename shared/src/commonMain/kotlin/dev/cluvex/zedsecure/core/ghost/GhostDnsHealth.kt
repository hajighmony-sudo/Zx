package dev.cluvex.zedsecure.core.ghost

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetAddress

/**
 * Lightweight DNS health check for Ghost Mode.
 * Resolves ordinary hostnames via system resolver — no hard dependency on
 * Cloudflare 1.1.1.1 or any single DoH provider.
 * Ghost Mode only.
 */
object GhostDnsHealth {

    data class ResolveSample(
        val host: String,
        val ok: Boolean,
        val latencyMs: Long,
        val addressCount: Int,
        val error: String? = null
    )

    data class Report(
        val samples: List<ResolveSample>,
        val successRatePercent: Int,
        val averageLatencyMs: Long,
        val healthy: Boolean,
        val summaryFa: String
    )

    private val hosts = listOf(
        "www.microsoft.com",
        "www.apple.com",
        "www.samsung.com",
        "www.hetzner.com",
        "www.digikala.com"
    )

    suspend fun check(): Report = withContext(Dispatchers.IO) {
        val samples = coroutineScope {
            hosts.map { h -> async { resolveOne(h) } }.awaitAll()
        }
        val ok = samples.count { it.ok }
        val rate = if (samples.isEmpty()) 0 else (ok * 100 / samples.size)
        val avg = samples.filter { it.ok }.map { it.latencyMs }.let {
            if (it.isEmpty()) 0L else it.average().toLong()
        }
        val healthy = rate >= 60
        Report(
            samples = samples,
            successRatePercent = rate,
            averageLatencyMs = avg,
            healthy = healthy,
            summaryFa = if (healthy) "DNS سالم ($rate٪ • میانگین ${avg} ms)"
            else "DNS ضعیف ($rate٪ • میانگین ${avg} ms)"
        )
    }

    private fun resolveOne(host: String): ResolveSample {
        return try {
            val t0 = System.nanoTime()
            val addrs = InetAddress.getAllByName(host)
            val ms = ((System.nanoTime() - t0) / 1_000_000L).coerceAtLeast(1L)
            ResolveSample(host, addrs.isNotEmpty(), ms, addrs.size)
        } catch (t: Throwable) {
            ResolveSample(host, false, 0L, 0, t.message ?: t::class.simpleName)
        }
    }
}
