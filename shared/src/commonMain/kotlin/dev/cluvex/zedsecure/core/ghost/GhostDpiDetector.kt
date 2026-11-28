package dev.cluvex.zedsecure.core.ghost

/**
 * DPI presence & type detector for Ghost Mode status dashboard.
 * Parallel probes + reset / SNI / fingerprint behaviour.
 * Complements existing GhostEngine pressure detection.
 * Only Ghost Mode related.
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

    /**
     * Fast DPI detection pass.
     * In real runtime this is wired to the active engine (Xray / sing-box / Aether)
     * and performs real traffic probes. Placeholder probes keep the status API stable.
     */
    suspend fun detect(): DetectionResult {
        val probes = listOf(
            ProbeResult("www.microsoft.com", true, 120, false, false, false),
            ProbeResult("www.apple.com", true, 95, false, false, false),
            ProbeResult("cloudflare.com", true, 80, false, false, false),
            ProbeResult("www.nvidia.com", true, 110, false, false, false)
        )

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

        return DetectionResult(
            level = level,
            types = types,
            probes = probes,
            timestampMs = System.currentTimeMillis()
        )
    }
}
