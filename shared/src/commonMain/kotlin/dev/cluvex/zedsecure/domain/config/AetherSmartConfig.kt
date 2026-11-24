package dev.cluvex.zedsecure.domain.config

import dev.cluvex.zedsecure.platform.LinkQuality
import dev.cluvex.zedsecure.platform.NetworkKind

/** What the user wants the Smart config to favor. */
enum class AetherSmartGoal { Auto, Speed, Stealth }

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
)

/**
 * Generates a complete Aether profile from live measurements of the user's current link.
 *
 * The decision is deliberately evidence-led: country is not used as a DPI verdict. Repeated
 * endpoint loss and latency variance are stronger signals than geography or the advertised
 * bandwidth alone. Smart remains an Aether-only profile; the service owns its separate
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
        val apiDegraded = apiLoss >= DEGRADED_API_LOSS_PERCENT
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
        val sniFiltered = input.tlsSniReachable == false
        val hostile = udpThrottled && sniFiltered
        val speedOverride = input.goal == AetherSmartGoal.Speed &&
            !edgeBlocked && !unstable && !networkUnvalidated && !udpThrottled && !sniFiltered
        val strict = edgeBlocked || unstable || networkUnvalidated || sniFiltered || hostile ||
            input.goal == AetherSmartGoal.Stealth

        val noize = when {
            hostile || edgeBlocked || unstable || networkUnvalidated || input.goal == AetherSmartGoal.Stealth -> "gfw"
            cellular || weak || apiDegraded || udpThrottled || sniFiltered -> "firewall"
            else -> "balanced"
        }
        val scan = when {
            input.goal == AetherSmartGoal.Stealth -> "verified"
            edgeBlocked || unstable || networkUnvalidated || hostile || sniFiltered -> "thorough"
            input.goal == AetherSmartGoal.Speed -> "turbo"
            fastEdge && !apiDegraded -> "turbo"
            slow || weak -> "turbo"
            cellular -> "thorough"
            else -> "balanced"
        }
        // Prefer TCP-carried MASQUE where the physical link is metered, cellular,
        // weak, unstable, or not demonstrably friendly to the faster UDP path.
        val h2 = !speedOverride && (
            cellular || strict || weak || !udpFriendlyHint || apiDegraded || udpThrottled || sniFiltered
            )

        return AetherProfile(
            smartFailover = true,
            profileName = nameFor(input.network),
            protocol = "masque",
            scanMode = scan,
            ipVersion = if (input.hasIpv6) "both" else "v4",
            noize = noize,
            http2 = h2,
            fragment = h2 && (strict || udpThrottled || sniFiltered),
            apiAddress = if (apiBlocked || apiDegraded) CLOUDFLARE_API_FALLBACK else "",
        )
    }

    fun nameFor(network: NetworkKind): String = when (network) {
        NetworkKind.Cellular -> "Aether · Smart · Mobile"
        NetworkKind.Wifi -> "Aether · Smart · Wi-Fi"
        NetworkKind.Ethernet -> "Aether · Smart · LAN"
        NetworkKind.Unknown -> "Aether · Smart"
    }

    private const val SLOW_PING_MS = 350
    private const val FAST_EDGE_MS = 220
    private const val UNSTABLE_JITTER_MS = 180
    private const val UNSTABLE_LOSS_PERCENT = 34
    private const val DEGRADED_API_LOSS_PERCENT = 34
    private const val STRONG_DOWN_KBPS = 15_000
    private const val STRONG_UP_KBPS = 3_000
    private const val STRONG_SIGNAL_DBM = -85
    private const val WEAK_DOWN_KBPS = 5_000
    private const val WEAK_UP_KBPS = 1_000
    private const val WEAK_SIGNAL_DBM = -105
    private const val CLOUDFLARE_API_FALLBACK = "162.159.137.105"
}
