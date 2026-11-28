package dev.cluvex.zedsecure.domain.config

import dev.cluvex.zedsecure.platform.NetworkKind
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A separate Xray + Aether/Psiphon profile. Xray owns DNS/routing; Aether
 * supplies the Psiphon CDN route and uses Xray's secondary mixed inbound as
 * its upstream so the two engines do not create a proxy loop.
 */
@Serializable
data class AetherSmart2Profile(
    val marker: String = MARKER,
    val profileName: String = "Aether · Smart 2",
    val networkName: String = "Unknown",
    val hasIpv6: Boolean = false,
    val dnsPreference: String = "smartguard",
    val xrayConfigTemplate: String = "",
) {
    fun xrayConfig(aetherPort: Int, upstreamPort: Int): String {
        require(aetherPort in 1024..65535 && upstreamPort in 1024..65535)
        require(aetherPort != upstreamPort)
        require(xrayConfigTemplate.isNotBlank()) { "Aether Smart 2 has no Xray template" }
        return xrayConfigTemplate
            .replace(AETHER_PORT_TOKEN, aetherPort.toString())
            .replace(UPSTREAM_PORT_TOKEN, upstreamPort.toString())
    }

    fun commandArgs(bindPort: Int, upstreamPort: Int): List<String> {
        require(bindPort in 1024..65535 && upstreamPort in 1024..65535)
        require(bindPort != upstreamPort)
        return listOf(
            "--bind", "127.0.0.1:$bindPort",
            "--psiphon-only",
            "--psiphon-mode", "cdn",
            "--psiphon-server-entries", "shipped-list",
            "--upstream", "socks5://127.0.0.1:$upstreamPort",
        )
    }

    fun commandPreview(): String =
        "aether --bind 127.0.0.1:<AETHER_PORT> --psiphon-only " +
            "--psiphon-mode cdn --psiphon-server-entries shipped-list " +
            "--upstream socks5://127.0.0.1:<MIXED_PORT>"

    companion object {
        const val MARKER = "ZX_AETHER_SMART_2"
        private const val AETHER_PORT_TOKEN = "__ZX_AETHER2_SOCKS_PORT__"
        private const val UPSTREAM_PORT_TOKEN = "__ZX_AETHER2_UPSTREAM_PORT__"
        private val codec = Json { ignoreUnknownKeys = true; isLenient = true }

        fun build(
            network: NetworkKind,
            hasIpv6: Boolean,
            probe: AetherSmartProbe.Result?,
        ): AetherSmart2Profile {
            val networkName = when (network) {
                NetworkKind.Cellular -> "Mobile"
                NetworkKind.Wifi -> "Wi-Fi"
                NetworkKind.Ethernet -> "LAN"
                NetworkKind.Unknown -> "Unknown"
            }
            // Prefer independent DoH when the system resolver disagrees with it.
            val preferCloudflare = probe?.dnsSystemMatchesDoh == false ||
                probe?.tcp443Reachable == false
            return AetherSmart2Profile(
                profileName = "Aether · Smart 2 · $networkName",
                networkName = networkName,
                hasIpv6 = hasIpv6,
                dnsPreference = if (preferCloudflare) "cloudflare" else "smartguard",
                xrayConfigTemplate = buildXrayConfig(preferCloudflare),
            )
        }

        fun decode(raw: String): AetherSmart2Profile? = runCatching {
            codec.decodeFromString<AetherSmart2Profile>(raw)
        }.getOrNull()?.takeIf {
            it.marker == MARKER && it.xrayConfigTemplate.isNotBlank()
        }

        fun encode(profile: AetherSmart2Profile): String =
            Json { encodeDefaults = true }.encodeToString(profile.copy(marker = MARKER))

        private fun buildXrayConfig(preferCloudflare: Boolean): String {
            val firstDoh = if (preferCloudflare) {
                "\"https://cloudflare-dns.com/dns-query\""
            } else {
                "\"https://dns.smartguard.io/dns-query\""
            }
            val secondDoh = if (preferCloudflare) {
                "\"https://dns.smartguard.io/dns-query\""
            } else {
                "\"https://cloudflare-dns.com/dns-query\""
            }
            return """
                {
                  "log": { "loglevel": "warning" },
                  "dns": {
                    "hosts": {
                      "geosite:category-ads-all": "127.0.0.1",
                      "domain:googleapis.cn": "googleapis.com",
                      "dns.alidns.com": ["223.5.5.5", "223.6.6.6", "2400:3200::1", "2400:3200:baba::1"],
                      "dns.sse.cisco.com": ["208.67.220.220", "208.67.222.222", "2620:119:35::35", "2620:119:53::53"],
                      "dns.umbrella.com": ["208.67.220.220", "208.67.222.222", "2620:119:35::35", "2620:119:53::53"],
                      "one.one.one.one": ["1.1.1.1", "1.0.0.1", "2606:4700:4700::1111", "2606:4700:4700::1001"],
                      "1dot1dot1dot1.cloudflare-dns.com": ["1.1.1.1", "1.0.0.1", "2606:4700:4700::1111", "2606:4700:4700::1001"],
                      "dns.cloudflare.com": ["162.159.61.8", "172.64.41.8", "2a06:98c1:52::8", "2803:f800:53::8"],
                      "cloudflare-dns.com": ["104.16.248.249", "104.16.249.249", "2606:4700::6810:f8f9", "2606:4700::6810:f9f9"],
                      "engage.cloudflareclient.com": ["162.159.192.1", "2606:4700:d0::a29f:c001"],
                      "doh.pub": ["1.12.12.12", "120.53.53.53"],
                      "dot.pub": ["1.12.12.12", "120.53.53.53"],
                      "dns.google": ["8.8.8.8", "8.8.4.4", "2001:4860:4860::8888", "2001:4860:4860::8844"],
                      "dns.quad9.net": ["9.9.9.9", "149.112.112.112", "2620:fe::fe", "2620:fe::9"],
                      "dns.sb": ["45.11.45.11", "185.222.222.222", "2a09::", "2a11::"],
                      "common.dot.dns.yandex.net": ["77.88.8.8", "77.88.8.1", "2a02:6b8::feed:0ff", "2a02:6b8:0:1::feed:0ff"]
                    },
                    "servers": [
                      {
                        "address": "fakedns",
                        "domains": ["geosite:cn", "geosite:private", "domain:ir", "geosite:category-ir"]
                      },
                      $firstDoh,
                      $secondDoh,
                      {
                        "address": "https://dns.alidns.com/dns-query",
                        "domains": ["domain:ir", "geosite:category-ir"],
                        "skipFallback": true,
                        "tag": "domestic-dns_2_0"
                      },
                      {
                        "address": "localhost",
                        "domains": ["geosite:private"],
                        "skipFallback": true,
                        "tag": "domestic-dns_1_0"
                      }
                    ],
                    "tag": "dns-module"
                  },
                  "fakedns": [
                    { "ipPool": "198.18.0.0/15", "poolSize": 65535 }
                  ],
                  "inbounds": [
                    {
                      "listen": "127.0.0.1",
                      "port": 10808,
                      "protocol": "socks",
                      "settings": { "auth": "noauth", "udp": true },
                      "sniffing": {
                        "destOverride": ["http", "tls", "quic", "fakedns"],
                        "enabled": true,
                        "routeOnly": false
                      },
                      "tag": "socks"
                    },
                    {
                      "listen": "127.0.0.1",
                      "port": 10809,
                      "protocol": "http",
                      "settings": {},
                      "sniffing": {
                        "destOverride": ["http", "tls", "quic", "fakedns"],
                        "enabled": true,
                        "routeOnly": false
                      },
                      "tag": "http"
                    },
                    {
                      "listen": "127.0.0.1",
                      "port": __ZX_AETHER2_UPSTREAM_PORT__,
                      "protocol": "mixed",
                      "settings": { "udp": true },
                      "tag": "secondary-socks"
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
                  "remarks": "AI_CON",
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
