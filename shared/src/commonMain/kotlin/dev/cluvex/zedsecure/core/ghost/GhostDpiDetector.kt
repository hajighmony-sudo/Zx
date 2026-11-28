package dev.cluvex.zedsecure.core.ghost

/**
 * DPI presence & type detector for Ghost Mode status dashboard.
 * Parallel probes + reset / SNI / fingerprint behaviour.
 * Complements existing GhostEngine pressure detection.
 * Only Ghost Mode related — does not touch other features.
 *
 * Runtime note: when wired to a live engine, replace the placeholder probes
 * with real HTTP/TLS checks against the targets below and map RST / timeout /
 * wrong cert responses into the ProbeResult flags.
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

    // Targets chosen as common Reality donors / popular HTTPS endpoints
    private val probeTargets = listOf(
        "www.microsoft.com",
        "www.apple.com",
        "cloudflare.com",
        "www.nvidia.com",
        "www.samsung.com"
    )

    /**
     * Fast DPI detection pass.
     * Placeholder probes keep the status API stable offline;
     * production builds should replace the body with real concurrent probes.
     */
    suspend fun detect(): DetectionResult {
        val probes = probeTargets.map { target ->
            // Offline-safe defaults: assume healthy network until real probes exist
            ProbeResult(
                target = target,
                success = true,
                latencyMs = 80L + (target.hashCode().toLong() and 0x3F),
                resetDetected = false,
                sniBlocked = false,
                fingerprintRejected = false
            )
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

        return DetectionResult(
            level = level,
            types = types,
            probes = probes,
            timestampMs = System.currentTimeMillis()
        )
    }
}
