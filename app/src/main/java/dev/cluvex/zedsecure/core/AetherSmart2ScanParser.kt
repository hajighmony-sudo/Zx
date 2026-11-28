package dev.cluvex.zedsecure.core

/**
 * Parses the endpoint-selection lines emitted by the pinned native Aether core.
 * Candidate probes are deliberately not treated as a selected route.
 */
internal object AetherSmart2ScanParser {
    data class Result(
        val protocol: String,
        val endpoint: String,
        val innerEndpoint: String? = null,
    )

    private val singleEndpoint = listOf(
        "best gateway " to "MASQUE",
        "using cloudflare edge " to "MASQUE",
        "wg endpoint " to "WireGuard",
        "selected MASQUE gateway " to "MASQUE",
        "selected WireGuard endpoint " to "WireGuard",
    )
    private val pairedPatterns = listOf(
        Regex("using cloudflare edge\\s+(\\S+)\\s+\\(outer\\)\\s+and\\s+(\\S+)\\s+\\(inner\\)") to "GOOL",
        Regex("masque-in-masque ready:\\s*(\\S+)\\s+\\(outer\\)\\s+and\\s+(\\S+)\\s+\\(inner\\)") to "MIM",
        Regex("gool ready: masque\\s+(\\S+)\\s+carries wireguard\\s+(\\S+)") to "GOOL",
    )
    private val hostPort = Regex("^(?:\\[[0-9A-Fa-f:]+\\]|[A-Za-z0-9.-]+):([0-9]{1,5})$")

    fun parse(line: String): Result? {
        // Two-hop announcements must be checked before the single MASQUE phrase.
        for ((pattern, protocol) in pairedPatterns) {
            val match = pattern.find(line) ?: continue
            val outer = validEndpoint(match.groupValues[1]) ?: continue
            val inner = validEndpoint(match.groupValues[2]) ?: continue
            return Result(protocol, outer, inner)
        }
        for ((prefix, protocol) in singleEndpoint) {
            val index = line.indexOf(prefix)
            if (index >= 0) {
                val token = line.substring(index + prefix.length).trim().substringBefore(' ')
                validEndpoint(token)?.let { return Result(protocol, it) }
            }
        }
        return null
    }

    private fun validEndpoint(raw: String): String? {
        val value = raw.trim().trimEnd(',', ';', ')')
        val match = hostPort.matchEntire(value) ?: return null
        val port = match.groupValues[1].toIntOrNull() ?: return null
        return value.takeIf { port in 1..65535 }
    }
}
