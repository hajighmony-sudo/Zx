package dev.cluvex.zedsecure.domain.config

import dev.cluvex.zedsecure.platform.NetworkKind

/**
 * Independent second-generation Aether profile builder.
 * It uses live link and reachability evidence; geography is never treated as proof of filtering.
 */
object AetherSmartConfigV2 {
    fun build(input: AetherSmartInput): AetherProfile {
        val cellular = input.network == NetworkKind.Cellular
        val link = input.link
        val weakLink = link != null && (
            link.generation == "2G" || link.generation == "3G" ||
                (link.downKbps in 1 until 5_000) ||
                (link.upKbps in 1..1_000) ||
                (link.signalDbm != null && link.signalDbm < -105)
            )
        val edgeLoss = input.edgeLossPercent ?: if (input.edgeReachable == false) 100 else 0
        val apiLoss = input.apiLossPercent ?: if (input.apiReachable == false) 100 else 0
        val unstable = edgeLoss >= 30 || (input.edgeJitterMs ?: 0) >= 160 ||
            (input.apiJitterMs ?: 0) >= 220
        val apiDegraded = input.apiReachable == false || apiLoss >= 30 ||
            (input.apiMs ?: 0) >= 900
        val tcpBlocked = input.tcp443Reachable == false
        val tlsInterference = input.tlsSniReachable == false && !tcpBlocked
        val dnsMismatch = input.dnsSystemMatchesDoh == false
        val udpUnavailable = input.udpReachable == false
        val communitySignal = (input.ooniAnomalyPercent ?: 0) >= 60
        val strongFilteringEvidence = tcpBlocked || tlsInterference || dnsMismatch ||
            (udpUnavailable && tlsInterference) || communitySignal
        val unvalidated = link?.validated == false
        val fastStable = !cellular && !weakLink && !unstable && !apiDegraded &&
            !strongFilteringEvidence && !unvalidated

        // HTTP/2 carries MASQUE over TCP on links where UDP or link quality is uncertain.
        val useHttp2 = !fastStable || udpUnavailable || cellular
        val useFragment = useHttp2 && (strongFilteringEvidence || dnsMismatch || tlsInterference)
        val scan = when {
            strongFilteringEvidence || unvalidated || edgeLoss >= 60 -> "ironclad"
            unstable || apiDegraded || weakLink -> "thorough"
            fastStable -> "turbo"
            else -> "balanced"
        }
        val noize = when {
            strongFilteringEvidence || communitySignal -> "gfw"
            cellular || weakLink || apiDegraded || unvalidated -> "firewall"
            unstable -> "balanced"
            else -> "light"
        }
        val apiAddress = if (!input.apiReachable.orDefaultTrue() || apiDegraded) {
            "162.159.137.105"
        } else {
            ""
        }
        val networkLabel = when (input.network) {
            NetworkKind.Cellular -> "Mobile"
            NetworkKind.Wifi -> "Wi-Fi"
            NetworkKind.Ethernet -> "LAN"
            NetworkKind.Unknown -> "Auto"
        }
        return AetherProfile(
            profileName = "Aether 2 · Smart · $networkLabel",
            protocol = "masque",
            scanMode = scan,
            ipVersion = if (input.hasIpv6) "both" else "v4",
            noize = noize,
            http2 = useHttp2,
            fragment = useFragment,
            apiAddress = apiAddress,
            smartFailover = true,
        )
    }

    private fun Boolean?.orDefaultTrue(): Boolean = this ?: true
}
