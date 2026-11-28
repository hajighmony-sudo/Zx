package dev.cluvex.zedsecure.domain.config

import dev.cluvex.zedsecure.platform.LinkQuality
import dev.cluvex.zedsecure.platform.NetworkKind

/** What the user wants the Smart config to favor. */
enum class AetherSmartGoal { Auto, Speed, Stability, Stealth }

/** Privacy-preserving measurements collected from the active physical network. */
data class AetherSmartInput(
    val network: NetworkKind = NetworkKind.Unknown,
    val hasIpv6: Boolean = false,
    val pingMs: Int? = null,
    /** Kept for profile compatibility/diagnostics; geography alone never implies DPI filtering. */
    val countryCode: String? = null,
    val edgeReachable: Boolean? = null,
    val edgeMs: Int? = null,
    val apiReachable: Boolean? = null,
    /** Median latency of repeated requests to the WARP registration API. */
    val apiMs: Int? = null,
    val edgeJitterMs: Int? = null,
    val goal: AetherSmartGoal = AetherSmartGoal.Auto,
    /** Local link estimates; they are hints, not proof that a tunnel transport works. */
    val link: LinkQuality? = null,
    /** Packet-like loss estimate from repeated HTTPS reachability samples (0..100). */
    val edgeLossPercent: Int? = null,
    val apiLossPercent: Int? = null,
    /** Direct UDP/53 round-trip probe; null when the platform cannot test it. */
    val udpReachable: Boolean? = null,
    /** Full TLS handshake with Cloudflare SNI; null when unavailable. */
    val tlsSniReachable: Boolean? = null,
    /** TCP/443 reachability helps separate routing/port failure from TLS-stage interference. */
    val tcp443Reachable: Boolean? = null,
    /** Compare system-resolver A answers with independent DNS-over-HTTPS answers. */
    val dnsSystemMatchesDoh: Boolean? = null,
    /** Recent OONI anomaly ratio for the same country/ASN; contextual, not device proof. */
    val ooniAnomalyPercent: Int? = null,
)

/**
 * Generates a complete Aether profile from live measurements of the user's current link.
 *
 * Combined strengths from:
 * - CluvexStudio/Aether (MASQUE, ironclad/verified scans, noize profiles, fragment, gool concepts)
 * - QW-AI-Code/Aether (Android integration patterns, evidence-led Smart decisions)
 *
 * The decision is deliberately evidence-led: country is not used as a DPI verdict.
 * Repeated endpoint loss, latency variance, UDP reachability, TLS/SNI success and
 * DNS consistency are stronger signals than geography or advertised bandwidth alone.
 *
 * Smart remains an Aether-only profile; the service owns its separate
 * WARP -> built-in Psiphon -> built-in Tor failover policy.
 */
object AetherSmartConfig {

    fun build(input: AetherSmartInput): AetherProfile {
        val cellular = input.network == NetworkKind.Cellular
        val link = input.link
        val slow = (input.pingMs ?: 0) >= SLOW_PING_MS
        val edgeBlocked = input.edgeReachable == false
        val apiBlocked = input.apiReachable == false
        val fastEdge = input.edgeReachable == true && (input.edgeMs ?: Int.MAX_VALUE) <= FAST_EDGE_MS
        val edgeLoss = input.edgeLossPercent?.coerceIn(0, 100) ?: if (edgeBlocked) 100 else 0
        val apiLoss = input.apiLossPercent?.coerceIn(0, 100) ?: if (apiBlocked) 100 else 0
        val unstable = edgeLoss >= UNSTABLE_LOSS_PERCENT ||
            (input.edgeJitterMs ?: 0) >= UNSTABLE_JITTER_MS
        val apiSlow = (input.apiMs ?: 0) >= SLOW_API_MS
        val apiDegraded = apiLoss >= DEGRADED_API_LOSS_PERCENT || apiSlow

        val weak = link != null && (
            link.generation == "2G" || link.generation == "3G" ||
                link.downKbps in 1 until WEAK_DOWN_KBPS ||
                link.upKbps in 1..WEAK_UP_KBPS ||
                (link.signalDbm != null && link.signalDbm < WEAK_SIGNAL_DBM)
            )

        val strongWifi = link != null &&
            input.network == NetworkKind.Wifi &&
            !link.metered &&
            link.validated &&
            link.downKbps >= STRONG_DOWN_KBPS &&
            link.upKbps >= STRONG_UP_KBPS &&
            (link.signalDbm == null || link.signalDbm >= STRONG_SIGNAL_DBM)

        // Network type is only a fallback hint when Android cannot provide link estimates.
        // Advertised speed/signal never overrides measured reachability or loss.
        val udpFriendlyHint = when {
            link != null -> strongWifi || (
                input.network == NetworkKind.Ethernet && link.validated && !weak
                )
            else -> input.network == NetworkKind.Wifi || input.network == NetworkKind.Ethernet
        }

        val networkUnvalidated = link?.validated == false
        val udpThrottled = input.udpReachable == false
        val dnsTampering = input.dnsSystemMatchesDoh == false
        val tcp443Blocked = input.tcp443Reachable == false
        // A TLS failure is an SNI/TLS signal only when TCP/443 itself is reachable.
        val sniFiltered = input.tlsSniReachable == false && input.tcp443Reachable != false
        val hostile = (udpThrottled && sniFiltered) || (dnsTampering && tcp443Blocked)
        val communityCorroboration = (input.ooniAnomalyPercent ?: 0) >= OONI_HIGH_ANOMALY_PERCENT

        val speedOverride = input.goal == AetherSmartGoal.Speed &&
            !edgeBlocked && !unstable && !networkUnvalidated && !udpThrottled && !sniFiltered &&
            !dnsTampering && !tcp443Blocked && !communityCorroboration && !apiDegraded

        val strict = edgeBlocked || unstable || networkUnvalidated || sniFiltered || hostile ||
            dnsTampering || tcp443Blocked || communityCorroboration ||
            input.goal == AetherSmartGoal.Stealth

        // ---------- Noize (obfuscation) profile ----------
        // Inspired by CluvexStudio/Aether noize levels + QW-AI evidence signals
        val noize = when {
            hostile || edgeBlocked || unstable || networkUnvalidated || communityCorroboration ||
                input.goal == AetherSmartGoal.Stealth -> "gfw"
            cellular || weak || apiDegraded || udpThrottled || sniFiltered ||
                dnsTampering || tcp443Blocked -> "firewall"
            slow || (edgeLoss in 15 until UNSTABLE_LOSS_PERCENT) || input.goal == AetherSmartGoal.Stability -> "balanced"
            else -> "light"
        }

        // ---------- Scan mode ----------
        // turbo / balanced / thorough / verified / ironclad (from CluvexStudio)
        val scan = when {
            input.goal == AetherSmartGoal.Stealth -> "verified"
            edgeBlocked || unstable || networkUnvalidated || hostile || sniFiltered ||
                dnsTampering || tcp443Blocked || communityCorroboration || apiDegraded -> {
                // When conditions are very hostile, prefer ironclad (end-to-end data-plane validation)
                if (hostile || communityCorroboration || edgeLoss >= 50) "ironclad" else "thorough"
            }
            input.goal == AetherSmartGoal.Speed -> "turbo"
            input.goal == AetherSmartGoal.Stability -> if (weak || apiDegraded || cellular) "thorough" else "balanced"
            fastEdge && !apiDegraded && !weak -> "turbo"
            slow || weak -> "turbo"
            cellular -> "thorough"
            else -> "balanced"
        }

        // Prefer TCP-carried MASQUE (H2) where the physical link is metered, cellular,
        // weak, unstable, or not demonstrably friendly to the faster UDP path.
        val h2 = !speedOverride && (
            cellular || strict || weak || !udpFriendlyHint || apiDegraded || udpThrottled ||
                sniFiltered || dnsTampering || tcp443Blocked || communityCorroboration ||
                input.goal == AetherSmartGoal.Stability
            )

        // Fragment ClientHello only when using H2 and there are clear DPI signals
        val useFragment = h2 && (strict || udpThrottled || sniFiltered || dnsTampering || communityCorroboration)

        // API address fallback when Cloudflare API is blocked or degraded
        val apiAddress = when {
            apiBlocked || apiDegraded -> CLOUDFLARE_API_FALLBACK
            else -> ""
        }

        return AetherProfile(
            smartFailover = true,
            profileName = nameFor(input.network, input.goal),
            protocol = "masque",
            scanMode = scan,
            ipVersion = if (input.hasIpv6) "both" else "v4",
            noize = noize,
            http2 = h2,
            fragment = useFragment,
            apiAddress = apiAddress,
        )
    }

    fun nameFor(network: NetworkKind, goal: AetherSmartGoal = AetherSmartGoal.Auto): String {
        val base = when (network) {
            NetworkKind.Cellular -> "Aether · Smart · Mobile"
            NetworkKind.Wifi -> "Aether · Smart · Wi-Fi"
            NetworkKind.Ethernet -> "Aether · Smart · LAN"
            NetworkKind.Unknown -> "Aether · Smart"
        }
        return when (goal) {
            AetherSmartGoal.Speed -> "$base · Speed"
            AetherSmartGoal.Stability -> "$base · Stability"
            AetherSmartGoal.Stealth -> "$base · Stealth"
            AetherSmartGoal.Auto -> base
        }
    }

    // Thresholds tuned from real-world Iranian + global censorship measurements
    private const val SLOW_PING_MS = 350
    private const val FAST_EDGE_MS = 220
    private const val UNSTABLE_JITTER_MS = 180
    private const val UNSTABLE_LOSS_PERCENT = 34
    private const val DEGRADED_API_LOSS_PERCENT = 34
    private const val SLOW_API_MS = 900
    private const val STRONG_DOWN_KBPS = 15_000
    private const val STRONG_UP_KBPS = 3_000
    private const val STRONG_SIGNAL_DBM = -85
    private const val WEAK_DOWN_KBPS = 5_000
    private const val WEAK_UP_KBPS = 1_000
    private const val WEAK_SIGNAL_DBM = -105
    private const val OONI_HIGH_ANOMALY_PERCENT = 60
    private const val CLOUDFLARE_API_FALLBACK = "162.159.137.105"
}
