package dev.cluvex.zedsecure.core

import dev.cluvex.zedsecure.domain.config.AetherProfile
import java.util.Locale

/** Evidence-driven retry planner for Aether Smart 2. Manual profiles never enter this planner. */
internal object AetherSmart2Brain {
    const val S_UDP_SILENT = 1
    const val S_TLS_RESET = 1 shl 1
    const val S_SCAN_EMPTY = 1 shl 2
    const val S_API_BLOCKED = 1 shl 3
    const val S_IDENTITY = 1 shl 4
    const val S_ECH = 1 shl 5
    const val S_TCP_BLOCKED = 1 shl 6
    const val EARLY_UDP_ABANDON_MS = 7_000L
    private const val FAST_DEATH_MS = 3_000L

    enum class Verdict { IDENTITY_DAMAGED, ECH_UNAVAILABLE, API_BLOCKED, ENGINE_FAULT, UDP_BLOCKED, TLS_INTERFERENCE, GATEWAYS_FILTERED, TOO_SLOW, NONE }

    data class Knowledge(
        val udpStrikes: Int = 0,
        val tlsStrikes: Int = 0,
        val filteredStrikes: Int = 0,
        val apiBlocked: Boolean = false,
        val udpWeakHint: Boolean = false,
    ) {
        val udpBlocked get() = udpStrikes >= 2 || (udpStrikes >= 1 && udpWeakHint)
        val tlsInterference get() = tlsStrikes > 0
        val gatewaysFiltered get() = filteredStrikes > 0
        fun learn(v: Verdict) = when (v) {
            Verdict.UDP_BLOCKED -> copy(udpStrikes = udpStrikes + 1)
            Verdict.TLS_INTERFERENCE -> copy(tlsStrikes = tlsStrikes + 1)
            Verdict.GATEWAYS_FILTERED -> copy(filteredStrikes = filteredStrikes + 1)
            Verdict.API_BLOCKED -> copy(apiBlocked = true)
            else -> this
        }
    }

    fun initial(udpReachable: Boolean?, tlsSniReachable: Boolean?, tcp443Reachable: Boolean?, edgeLossPercent: Int?) =
        Knowledge(
            udpStrikes = if (udpReachable == false) 1 else 0,
            tlsStrikes = if (tlsSniReachable == false && tcp443Reachable != false) 1 else 0,
            filteredStrikes = if (tcp443Reachable == false || edgeLossPercent == 100) 1 else 0,
            udpWeakHint = udpReachable == false,
        )

    fun signalsOf(line: String): Int {
        val value = line.lowercase(Locale.ROOT)
        var bits = 0
        val recovered = "provisioning a fresh identity" in value
        if (!recovered && ("valid utf-8" in value || "config parse" in value ||
                "damaged file was moved" in value || "fresh identity can be provisioned" in value)) bits = bits or S_IDENTITY
        if ("ech is on but" in value || "no ech key" in value) bits = bits or S_ECH
        if ("could not reach the account api" in value ||
            (("register" in value || "enrol" in value || "provision" in value) && ("failed" in value || "error" in value)) ||
            ("api.cloudflareclient.com" in value && ("timed out" in value || "error" in value || "refused" in value))) bits = bits or S_API_BLOCKED
        if ("scan deadline reached with no" in value || ("no usable" in value && "found" in value) ||
            "found no data-plane endpoint" in value) bits = bits or S_SCAN_EMPTY
        if ("connection reset" in value || "reset by peer" in value || "broken pipe" in value || "unexpected eof" in value ||
            ("tls" in value && ("alert" in value || "handshake failure" in value || "handshake eof" in value)) ||
            ("handshake" in value && "reset" in value)) bits = bits or S_TLS_RESET
        if (("handshake" in value && ("timed out" in value || "timeout" in value)) || "idle timeout" in value ||
            ("no response" in value && ("udp" in value || "quic" in value || "wireguard" in value || "endpoint" in value))) bits = bits or S_UDP_SILENT
        if ("network is unreachable" in value || "connection refused" in value ||
            ("tcp" in value && "connect" in value && ("timed out" in value || "timeout" in value))) bits = bits or S_TCP_BLOCKED
        return bits
    }

    fun diagnose(signals: Int, died: Boolean, aliveMs: Long, ridesUdp: Boolean, fatal: String?): Verdict {
        fun has(bit: Int) = signals and bit != 0
        return when {
            has(S_IDENTITY) -> Verdict.IDENTITY_DAMAGED
            has(S_ECH) -> Verdict.ECH_UNAVAILABLE
            has(S_API_BLOCKED) -> Verdict.API_BLOCKED
            died && aliveMs < FAST_DEATH_MS && !fatal.isNullOrBlank() -> Verdict.ENGINE_FAULT
            has(S_TLS_RESET) && !ridesUdp -> Verdict.TLS_INTERFERENCE
            ridesUdp && (has(S_UDP_SILENT) || has(S_SCAN_EMPTY) || !died) -> Verdict.UDP_BLOCKED
            has(S_SCAN_EMPTY) || has(S_TCP_BLOCKED) -> Verdict.GATEWAYS_FILTERED
            has(S_TLS_RESET) -> Verdict.TLS_INTERFERENCE
            !died -> Verdict.TOO_SLOW
            else -> Verdict.NONE
        }
    }

    fun ridesUdp(p: AetherProfile) = when (p.protocol) {
        "wg" -> true
        "masque", "mim", "gool" -> !p.http2
        else -> false
    }

    fun signature(p: AetherProfile) =
        listOf(p.protocol, p.http2, p.noize, p.fragment, p.ech, p.scanMode, p.apiAddress).joinToString("|")

    fun adapt(remaining: List<AetherProfile>, k: Knowledge): List<AetherProfile> {
        val boosted = remaining.map { p ->
            var noize = p.noize
            var fragment = p.fragment
            var http2 = p.http2
            var scan = p.scanMode
            if (!ridesUdp(p)) {
                if (k.tlsInterference || k.gatewaysFiltered) {
                    fragment = true
                    noize = maxNoize(noize, "firewall")
                }
                if (k.udpBlocked && p.protocol != "wg") http2 = true
            } else if (k.gatewaysFiltered || k.udpBlocked) {
                noize = maxNoize(noize, "gfw")
            }
            if (k.gatewaysFiltered && scan == "turbo") scan = "thorough"
            p.copy(noize = noize, fragment = fragment, http2 = http2, scanMode = scan)
        }
        val ordered = if (k.udpBlocked) boosted.filterNot(::ridesUdp) + boosted.filter(::ridesUdp) else boosted
        return ordered.distinctBy(::signature)
    }

    fun shouldAbandonUdp(p: AetherProfile, k: Knowledge, elapsedMs: Long, candidateSeen: Boolean) =
        ridesUdp(p) && k.udpBlocked && !candidateSeen && elapsedMs >= EARLY_UDP_ABANDON_MS

    private fun maxNoize(current: String, minimum: String): String {
        val rank = mapOf("off" to 0, "light" to 1, "firewall" to 2, "balanced" to 3, "gfw" to 4, "aggressive" to 5)
        return if ((rank[current] ?: 0) >= (rank[minimum] ?: 0)) current else minimum
    }
}
