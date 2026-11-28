package dev.cluvex.zedsecure.core.ghost

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * DPI presence & type detector for Ghost Mode status dashboard.
 * Parallel HTTPS probes to Reality-style donors; maps timeouts / failures
 * into DpiLevel + DpiType for the adaptive ladder.
 *
 * Only Ghost Mode related — does not touch other features.
 */
class GhostDpiDetector {

    data class ProbeResult(
        val target: String,
        val success: Boolean,
        val latencyMs: Long,
        val resetDetected: Boolean,
        val sniBlocked: Boolean,
        val fingerprintRejected: Boolean
    )

    data class DetectionResult(
        val level: DpiLevel,
        val types: List<DpiType>,
        val probes: List<ProbeResult>,
        val timestampMs: Long
    )

    private val probeTargets = listOf(
        "www.microsoft.com",
        "www.apple.com",
        "cloudflare.com",
        "www.nvidia.com",
        "www.samsung.com"
    )

    suspend fun detect(): DetectionResult = withContext(Dispatchers.IO) {
        val probes = coroutineScope {
            probeTargets.map { target ->
                async { probeOne(target) }
            }.awaitAll()
        }

        val failed = probes.count { !it.success || it.resetDetected || it.sniBlocked || it.fingerprintRejected }
        val level = when {
            failed == 0 -> DpiLevel.NONE
            failed == 1 -> DpiLevel.LOW
            failed == 2 -> DpiLevel.MEDIUM
            failed == 3 -> DpiLevel.HIGH
            else -> DpiLevel.EXTREME
        }

        val types = mutableListOf<DpiType>()
        if (probes.any { it.sniBlocked }) types += DpiType.SNI_BLOCKING
        if (probes.any { it.fingerprintRejected }) types += DpiType.TLS_FINGERPRINT
        if (probes.any { it.resetDetected }) types += DpiType.ACTIVE_PROBING
        if (probes.count { !it.success } >= 3) types += DpiType.PROTOCOL_SIGNATURE

        DetectionResult(
            level = level,
            types = types.ifEmpty { if (failed > 0) listOf(DpiType.UNKNOWN) else emptyList() },
            probes = probes,
            timestampMs = System.currentTimeMillis()
        )
    }

    private fun probeOne(host: String): ProbeResult {
        return try {
            val t0 = System.nanoTime()
            val conn = (URL("https://$host/").openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 3500
                readTimeout = 3500
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "ZX-GhostDpiDetector/1.0")
            }
            val code = conn.responseCode
            val latency = ((System.nanoTime() - t0) / 1_000_000L).coerceAtLeast(1L)
            conn.disconnect()
            val ok = code in 200..399 || code == 405
            ProbeResult(
                target = host,
                success = ok,
                latencyMs = latency,
                resetDetected = !ok && latency < 200,
                sniBlocked = !ok && latency in 200..1500,
                fingerprintRejected = false
            )
        } catch (_: Throwable) {
            ProbeResult(
                target = host,
                success = false,
                latencyMs = 3500,
                resetDetected = true,
                sniBlocked = false,
                fingerprintRejected = false
            )
        }
    }
}
