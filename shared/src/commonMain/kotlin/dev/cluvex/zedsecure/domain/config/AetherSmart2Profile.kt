package dev.cluvex.zedsecure.domain.config

import dev.cluvex.zedsecure.platform.NetworkKind
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Aether Smart 2 – fully network-driven profile.
 *
 * Only manual control: protocol selection (including AUTOMATIC).
 * Everything else (DNS, SNI, Noize, Fragment, ECH, Scan, Anti-DPI stack,
 * Xray front) is derived from live probes of the user's current internet.
 *
 * Backward compatible with previous ZX_AETHER_SMART_2 marker readers.
 * Does not modify any non-Smart-2 code paths.
 */
@Serializable
data class AetherSmart2Profile(
    val marker: String = MARKER,
    val profileName: String = "Aether · Smart 2",
    val networkName: String = "Unknown",
    val hasIpv6: Boolean = false,
    val dnsPreference: String = "smartguard",
    val xrayConfigTemplate: String = "",
    /** Native Aether WARP/MASQUE/WireGuard profile; Xray remains the DNS/routing front. */
    val aetherProfile: AetherProfile = AetherProfile(
        profileName = "Aether · Smart 2",
        smartFailover = true,
    ),
    // ---------- New Smart-2 fields (optional for older decoders) ----------
    /** Only user-facing choice. AUTOMATIC lets the engine pick from live evidence. */
    val protocolChoice: String = ProtocolChoice.AUTOMATIC.name,
    val antiDpi: AntiDpiStack = AntiDpiStack(),
    val preferredDns: List<String> = emptyList(),
    val preferredSnis: List<String> = emptyList(),
    val networkFingerprintSummary: String = "",
) {
    fun xrayConfig(aetherPort: Int, upstreamPort: Int): String {
        require(aetherPort in 1024..65535 && upstreamPort in 1024..65535)
        require(aetherPort != upstreamPort)
        require(xrayConfigTemplate.isNotBlank()) { "Aether Smart 2 has no Xray template" }
        return xrayConfigTemplate
            .replace(AETHER_PORT_TOKEN, aetherPort.toString())
            .replace(UPSTREAM_PORT_TOKEN, upstreamPort.toString())
    }

    fun commandArgs(bindPort: Int, workingDirectory: String): List<String> =
        aetherProfile.toArgs(bindPort, workingDirectory)

    fun commandPreview(): String = aetherProfile.commandPreview()

    enum class ProtocolChoice {
        AUTOMATIC,
        MASQUE_H3,
        MASQUE_H2,
        WIREGUARD,
        GOOL,
        MASQUE_X2,
        HYBRID_SMART,
    }

    companion object {
        const val MARKER = "ZX_AETHER_SMART_2"
        private const val AETHER_PORT_TOKEN = "__ZX_AETHER2_SOCKS_PORT__"
        private const val UPSTREAM_PORT_TOKEN = "__ZX_AETHER2_UPSTREAM_PORT__"
        private val codec = Json { ignoreUnknownKeys = true; isLenient = true }

        /**
         * Full Smart-2 builder.
         *
         * @param protocolChoice only manual input; AUTOMATIC = evidence-based selection
         */
        fun build(
            network: NetworkKind,
            hasIpv6: Boolean,
            probe: AetherSmartProbe.Result?,
            protocolChoice: ProtocolChoice = ProtocolChoice.AUTOMATIC,
        ): AetherSmart2Profile {
            val networkName = when (network) {
                NetworkKind.Cellular -> "Mobile"
                NetworkKind.Wifi -> "Wi-Fi"
                NetworkKind.Ethernet -> "LAN"
                NetworkKind.Unknown -> "Unknown"
            }
            val country = probe?.countryCode
            val preferIran = country.equals("IR", ignoreCase = true) ||
                (probe?.ooniAnomalyPercent ?: 0) >= 40

            // ---------- Anti-DPI stack (11 methods + dual attack) ----------
            val antiDpi = AntiDpiStack.buildFromProbe(probe, country, preferIran)

            // ---------- Protocol decision (only manual or AUTOMATIC) ----------
            val resolvedProtocol = resolveProtocol(protocolChoice, probe, antiDpi)
            val useHttp2 = resolvedProtocol == "masque" && (
                protocolChoice == ProtocolChoice.MASQUE_H2 ||
                    (protocolChoice == ProtocolChoice.AUTOMATIC && probe?.udpReachable == false)
                )

            // ---------- Aether core from evidence ----------
            val smartAether = AetherSmartConfig.build(
                AetherSmartInput(
                    network = network,
                    hasIpv6 = hasIpv6,
                    pingMs = probe?.edgeMs,
                    edgeReachable = probe?.edgeLossPercent?.let { it < 100 },
                    edgeMs = probe?.edgeMs,
                    apiReachable = probe?.apiLossPercent?.let { it < 100 },
                    apiMs = probe?.apiMs,
                    edgeJitterMs = probe?.edgeJitterMs,
                    apiJitterMs = probe?.apiJitterMs,
                    udpReachable = probe?.udpReachable,
                    tcp443Reachable = probe?.tcp443Reachable,
                    tlsSniReachable = probe?.tlsSniReachable,
                    dnsSystemMatchesDoh = probe?.dnsSystemMatchesDoh,
                    countryCode = country,
                    ooniAnomalyPercent = probe?.ooniAnomalyPercent,
                ),
            ).copy(
                profileName = "Aether · Smart 2 · $networkName",
                smartFailover = true,
                protocol = resolvedProtocol,
                http2 = useHttp2,
                noize = antiDpi.noizeLevel,
                fragment = antiDpi.fragmentEnabled,
                fragmentSize = antiDpi.fragmentSize,
                fragmentDelay = antiDpi.fragmentDelay,
                ech = antiDpi.echEnabled,
                // Keep ECH resolver pinned; Iranian DNS is applied on Xray front
                echDns = "https://cloudflare-dns.com/dns-query@address=1.1.1.1@sni=cloudflare-dns.com",
            )

            // ---------- DNS preference ----------
            val preferCloudflare = probe?.dnsSystemMatchesDoh == false ||
                probe?.tcp443Reachable == false
            val dnsPref = when {
                preferIran -> "iran-first"
                preferCloudflare -> "cloudflare"
                else -> "smartguard"
            }

            val orderedDns = IranianAssets.orderedDns(preferIran)
            val dnsIps = orderedDns.flatMap { it.ips }.distinct().take(8)
            val snis = IranianAssets.orderedSnis(preferIran)

            val fingerprint = buildString {
                append(networkName)
                if (hasIpv6) append(" · IPv6")
                probe?.countryCode?.let { append(" · $it") }
                probe?.isp?.let { append(" · $it") }
                append(" · loss=${probe?.edgeLossPercent ?: "?"}%)")
                if (antiDpi.mode == "dual") append(" · Dual-Attack")
            }

            return AetherSmart2Profile(
                profileName = smartAether.profileName,
                networkName = networkName,
                hasIpv6 = hasIpv6,
                dnsPreference = dnsPref,
                xrayConfigTemplate = buildXrayConfig(
                    preferCloudflare = preferCloudflare,
                    hasIpv6 = hasIpv6,
                    preferIran = preferIran,
                    antiDpi = antiDpi,
                    dnsIps = dnsIps,
                ),
                aetherProfile = smartAether,
                protocolChoice = protocolChoice.name,
                antiDpi = antiDpi,
                preferredDns = dnsIps,
                preferredSnis = snis.take(8),
                networkFingerprintSummary = fingerprint,
            )
        }

        /**
         * Maps ProtocolChoice (+ live evidence) → native Aether protocol string.
         */
        private fun resolveProtocol(
            choice: ProtocolChoice,
            probe: AetherSmartProbe.Result?,
            antiDpi: AntiDpiStack,
        ): String {
            if (choice != ProtocolChoice.AUTOMATIC) {
                return when (choice) {
                    ProtocolChoice.MASQUE_H3, ProtocolChoice.MASQUE_H2 -> "masque"
                    ProtocolChoice.WIREGUARD -> "wg"
                    ProtocolChoice.GOOL -> "gool"
                    ProtocolChoice.MASQUE_X2 -> "mim"
                    ProtocolChoice.HYBRID_SMART -> "masque"
                    ProtocolChoice.AUTOMATIC -> "masque"
                }
            }

            // AUTOMATIC evidence ladder
            val udpOk = probe?.udpReachable != false
            val sniOk = probe?.tlsSniReachable != false
            val tcpOk = probe?.tcp443Reachable != false
            val hostile = antiDpi.mode == "dual" && antiDpi.mortarEnabled

            return when {
                hostile && !udpOk -> "gool"          // climb over the wall
                hostile -> "masque"                  // still try direct punch first
                !udpOk && tcpOk -> "masque"          // HTTP/2 path will be forced by http2 flag
                !sniOk -> "masque"
                else -> "masque"
            }
        }

        fun decode(raw: String): AetherSmart2Profile? = runCatching {
            codec.decodeFromString<AetherSmart2Profile>(raw)
        }.getOrNull()?.takeIf {
            it.marker == MARKER && it.xrayConfigTemplate.isNotBlank()
        }

        fun encode(profile: AetherSmart2Profile): String =
            Json { encodeDefaults = true }.encodeToString(profile.copy(marker = MARKER))

        private fun buildXrayConfig(
            preferCloudflare: Boolean,
            hasIpv6: Boolean,
            preferIran: Boolean,
            antiDpi: AntiDpiStack,
            dnsIps: List<String>,
        ): String {
            val queryStrategy = if (hasIpv6) "UseIP" else "UseIPv4"

            // Build DNS servers block: Iranian first when preferred, then global DoH
            val dnsServers = buildString {
                // FakeDNS first for local speed
                append("""{"address":"fakedns","domains":["geosite:cn","geosite:geolocation-cn","domain:ir","geosite:category-ir"]},""")
                // Iranian / preferred IPs as direct UDP/TCP
                dnsIps.take(4).forEach { ip ->
                    append("""{"address":"$ip","domains":["domain:ir","geosite:category-ir","geosite:private"],"expectIPs":["geoip:ir","geoip:private"]},""")
                }
                // DoH chain
                if (preferIran) {
                    append("""{"address":"https://free.shecan.ir/dns-query","domains":["geosite:geolocation-!cn"]},""")
                }
                if (preferCloudflare || !preferIran) {
                    append("""{"address":"https://cloudflare-dns.com/dns-query"},""")
                    append("""{"address":"https://dns.google/dns-query"},""")
                } else {
                    append("""{"address":"https://dns.smartguard.io/dns-query"},""")
                    append("""{"address":"https://cloudflare-dns.com/dns-query"},""")
                }
                // Final fallbacks
                append("""{"address":"1.1.1.1"},{"address":"8.8.8.8"}""")
            }

            // Hosts map includes Iranian DNS names
            val hostsExtra = if (preferIran) {
                """
                      "free.shecan.ir": ["178.22.122.100","185.51.200.2"],
                      "dns.shecan.ir": ["178.22.122.100","185.51.200.2"],
                """.trimIndent()
            } else ""

            return """
                {
                  "log": { "loglevel": "warning" },
                  "dns": {
                    "hosts": {
                      "geosite:category-ads-all": "127.0.0.1",
                      "domain:googleapis.cn": "googleapis.com",
                      "dns.alidns.com": ["223.5.5.5", "223.6.6.6", "2400:3200::1", "2400:3200:baba::1"],
                      "one.one.one.one": ["1.1.1.1", "1.0.0.1", "2606:4700:4700::1111", "2606:4700:4700::1001"],
                      "1dot1dot1dot1.cloudflare-dns.com": ["1.1.1.1", "1.0.0.1", "2606:4700:4700::1111", "2606:4700:4700::1001"],
                      "dns.cloudflare.com": ["162.159.61.8", "172.64.41.8"],
                      "cloudflare-dns.com": ["104.16.248.249", "104.16.249.249"],
                      "engage.cloudflareclient.com": ["162.159.192.1", "2606:4700:d0::a29f:c001"],
                      "dns.google": ["8.8.8.8", "8.8.4.4", "2001:4860:4860::8888", "2001:4860:4860::8844"],
                      "dns.quad9.net": ["9.9.9.9", "149.112.112.112"],
                      $hostsExtra
                      "dns.sb": ["45.11.45.11", "185.222.222.222"]
                    },
                    "servers": [ $dnsServers ],
                    "queryStrategy": "$queryStrategy",
                    "disableCache": false,
                    "disableFallback": false,
                    "disableFallbackIfMatch": true
                  },
                  "inbounds": [
                    {
                      "listen": "127.0.0.1",
                      "port": 10808,
                      "protocol": "mixed",
                      "settings": { "udp": true },
                      "sniffing": {
                        "enabled": true,
                        "destOverride": ["http", "tls", "quic", "fakedns"],
                        "routeOnly": false
                      },
                      "tag": "socks"
                    },
                    {
                      "listen": "127.0.0.1",
                      "port": __ZX_AETHER2_UPSTREAM_PORT__,
                      "protocol": "mixed",
                      "settings": { "udp": true },
                      "tag": "secondary-socks"
                    },
                    {
                      "listen": "127.0.0.1",
                      "port": 10853,
                      "protocol": "dokodemo-door",
                      "settings": {
                        "address": "1.1.1.1",
                        "network": "tcp,udp",
                        "port": 53
                      },
                      "tag": "dns-module"
                    }
                  ],
                  "outbounds": [
                    {
                      "mux": { "concurrency": -1, "enabled": false },
                      "protocol": "socks",
                      "settings": {
                        "servers": [
                          { "address": "127.0.0.1", "port": __ZX_AETHER2_SOCKS_PORT__ }
                        ]
                      },
                      "streamSettings": { "network": "tcp" },
                      "tag": "proxy"
                    },
                    { "protocol": "freedom", "settings": {}, "tag": "direct" },
                    { "protocol": "blackhole", "settings": {}, "tag": "block" },
                    { "protocol": "dns", "settings": { "userLevel": 12 }, "tag": "dns-out" },
                    { "protocol": "freedom", "settings": {}, "tag": "exit-node" }
                  ],
                  "policy": {
                    "levels": {
                      "0": { "downlinkOnly": 0, "uplinkOnly": 0 },
                      "12": { "connIdle": 12, "downlinkOnly": 0, "uplinkOnly": 0 }
                    }
                  },
                  "remarks": "Aether-Smart-2-${antiDpi.mode}",
                  "routing": {
                    "domainStrategy": "AsIs",
                    "rules": [
                      { "inboundTag": ["secondary-socks"], "outboundTag": "exit-node", "type": "field" },
                      { "inboundTag": ["socks"], "port": "53", "outboundTag": "dns-out", "type": "field" },
                      { "inboundTag": ["dns-module"], "outboundTag": "proxy", "type": "field" },
                      { "protocol": ["bittorrent"], "outboundTag": "direct", "type": "field" },
                      { "network": "udp", "port": "443", "outboundTag": "block", "type": "field" },
                      { "domain": ["geosite:category-ads-all"], "outboundTag": "block", "type": "field" },
                      { "ip": ["geoip:private"], "outboundTag": "direct", "type": "field" },
                      { "domain": ["geosite:private"], "outboundTag": "direct", "type": "field" },
                      { "domain": ["domain:ir", "geosite:category-ir"], "outboundTag": "direct", "type": "field" },
                      { "ip": ["geoip:ir"], "outboundTag": "direct", "type": "field" }
                    ]
                  }
                }
            """.trimIndent()
        }
    }
}
