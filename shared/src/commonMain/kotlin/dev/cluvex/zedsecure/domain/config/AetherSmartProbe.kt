package dev.cluvex.zedsecure.domain.config

import dev.cluvex.zedsecure.platform.currentTimeMillis
import dev.cluvex.zedsecure.platform.httpGetResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * Repeated, independent reachability samples for the active physical network.
 *
 * Each target is sampled in parallel to keep setup time bounded. A failed sample is
 * counted as loss; a single lucky response therefore cannot make a filtered or unstable
 * connection look healthy.
 */
object AetherSmartProbe {
    data class Result(
        val edgeMs: Int?,
        val apiMs: Int?,
        val edgeJitterMs: Int? = null,
        val edgeLossPercent: Int = 100,
        val apiLossPercent: Int = 100,
    )

    suspend fun run(): Result = coroutineScope {
        val edge = async {
            sample(
                primary = EDGE_URL,
                fallback = EDGE_FALLBACK_URL,
            )
        }
        val api = async {
            sample(
                primary = API_URL,
                fallback = API_FALLBACK_URL,
            )
        }
        val edgeResult = edge.await()
        val apiResult = api.await()
        Result(
            edgeMs = edgeResult.medianMs,
            apiMs = apiResult.medianMs,
            edgeJitterMs = edgeResult.jitterMs,
            edgeLossPercent = edgeResult.lossPercent,
            apiLossPercent = apiResult.lossPercent,
        )
    }

    private data class SampleSet(
        val medianMs: Int?,
        val jitterMs: Int?,
        val lossPercent: Int,
    )

    private suspend fun sample(primary: String, fallback: String): SampleSet = coroutineScope {
        val samples = (0 until SAMPLE_COUNT).map {
            async {
                time(primary) ?: time(fallback)
            }
        }.map { it.await() }
        val successful = samples.filterNotNull().sorted()
        val median = successful.takeIf { it.isNotEmpty() }?.let { it[it.size / 2] }
        val jitter = successful.takeIf { it.size >= 2 }?.let { it.last() - it.first() }
        val lost = SAMPLE_COUNT - successful.size
        SampleSet(
            medianMs = median,
            jitterMs = jitter,
            lossPercent = (lost * 100) / SAMPLE_COUNT,
        )
    }

    private suspend fun time(url: String): Int? = withContext(Dispatchers.IO) {
        val start = currentTimeMillis()
        val answered = try {
            httpGetResponse(url, "ZX-Aether-Smart", null, TIMEOUT_MS, TIMEOUT_MS)
            true
        } catch (e: Throwable) {
            // An HTTP error still proves that the TCP/TLS path reached the remote endpoint.
            e.message?.startsWith("HTTP ") == true
        }
        if (answered) (currentTimeMillis() - start).toInt().coerceAtLeast(1) else null
    }

    private const val SAMPLE_COUNT = 3
    private const val TIMEOUT_MS = 2_500
    private const val EDGE_URL = "https://cp.cloudflare.com/generate_204"
    private const val EDGE_FALLBACK_URL = "https://www.cloudflare.com/cdn-cgi/trace"
    private const val API_URL = "https://api.devices.cloudflare.com/"
    private const val API_FALLBACK_URL = "https://api.cloudflareclient.com/"
}
