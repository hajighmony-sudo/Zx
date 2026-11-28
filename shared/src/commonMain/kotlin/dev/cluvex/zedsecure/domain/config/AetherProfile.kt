package dev.cluvex.zedsecure.domain.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Settings for the isolated Aether WARP engine. */
@Serializable
data class AetherProfile(
    val marker: String = MARKER,
    val profileName: String = "Aether",
    val protocol: String = "masque",
    val scanMode: String = "balanced",
    val ipVersion: String = "v4",
    val noize: String = "firewall",
    val http2: Boolean = false,
    /** Sends the TLS ClientHello in pieces (anti-DPI); only meaningful with HTTP/2 transport. */
    val fragment: Boolean = false,
    /** Optional IP for the WARP registration API, for networks that block its default addresses. */
    val apiAddress: String = "",
    /** Enables the ordered WARP -> built-in Psiphon -> built-in Tor fallback used only by Smart profiles. */
    val smartFailover: Boolean = false,
) {
    fun toArgs(bindPort: Int, workingDirectory: String): List<String> {
        require(protocol in PROTOCOLS) { "Unsupported Aether protocol" }
        require(scanMode in SCAN_MODES) { "Unsupported Aether scan mode" }
        require(ipVersion in IP_VERSIONS) { "Unsupported Aether IP version" }
        require(noize in NOIZE_PROFILES) { "Unsupported Aether obfuscation profile" }
        require(!http2 || protocol != "wg") { "HTTP/2 transport is not available for WireGuard mode" }
        return buildList {
            add("--bind"); add("127.0.0.1:$bindPort")
            add("--protocol"); add(protocol)
            add("--scan"); add(scanMode)
            add("--ip"); add(ipVersion)
            add("--noize"); add(noize)
            if (http2) add("--h2")
            if (fragment && http2) add("--fragment")
            if (apiAddress.isNotBlank() && apiAddress.all { it.isLetterOrDigit() || it in ".:-[]" }) {
                add("--api-address"); add(apiAddress.trim())
            }
            // Keep the last known working gateway across transient reconnects and
            // emit native tunnel counters into the existing Aether log stream.
            add("--quick-reconnect")
            add("--stats"); add("--stats-secs"); add("15")
            add("--log-level"); add("info")
            add("--config"); add("$workingDirectory/aether.toml")
        }
    }

    /**
     * The native thorough/verified/ironclad scans have a 300-second internal
     * budget. The Android service must not kill the process before that scan
     * can finish and select a gateway.
     */
    /**
     * Human-readable preview of the exact native arguments generated for this profile.
     * The SOCKS port and working directory are runtime values selected by the service.
     * The preview is informational; the app launches the process with an argument list,
     * never by passing this string through a shell.
     */
    fun commandPreview(): String {
        val args = toArgs(PREVIEW_SOCKS_PORT, PREVIEW_HOME)
            .map { arg ->
                when (arg) {
                    "127.0.0.1:$PREVIEW_SOCKS_PORT" -> "127.0.0.1:<SOCKS_PORT>"
                    "$PREVIEW_HOME/aether.toml" -> "<AETHER_HOME>/aether.toml"
                    else -> arg
                }
            }
        return (listOf("libaether.so") + args).joinToString(" ")
    }

    fun startupBudgetMs(): Long = when (scanMode) {
        "turbo" -> 60_000L
        "balanced" -> 120_000L
        "thorough", "verified", "ironclad" -> 330_000L
        else -> 120_000L
    }

    /**
     * Ordered connection attempts: this profile first, then progressively more conservative
     * variants (other transport, stronger obfuscation, WireGuard, plain). The service tries each
     * until real traffic passes, so one unlucky setting never leaves the user with an error.
     */
    fun attempts(): List<AetherProfile> {
        // Manual profiles are explicit user choices. Only Smart profiles may
        // change transport or obfuscation automatically after a failed probe.
        if (!smartFailover) return listOf(this)

        return buildList {
            add(this@AetherProfile)
            if (protocol == "masque") add(copy(http2 = !http2))
            add(copy(protocol = "masque", http2 = true, fragment = true, noize = "gfw", scanMode = "thorough"))
            add(copy(protocol = "masque", http2 = true, fragment = true, noize = "gfw", scanMode = "thorough", apiAddress = "162.159.137.105"))

            // Native Aether supports both two-hop transports. Keep them as
            // conservative Smart fallbacks, not silent changes to manual profiles.
            add(copy(protocol = "gool", http2 = true, fragment = false, noize = "balanced", scanMode = "balanced"))
            add(copy(protocol = "mim", http2 = true, fragment = false, noize = "balanced", scanMode = "balanced"))
            add(copy(protocol = "wg", http2 = false, noize = "balanced", scanMode = "balanced"))
            add(copy(protocol = "masque", http2 = false, noize = "off", ipVersion = "v4", scanMode = "balanced"))
        }.distinct()
    }

    companion object {
        private const val PREVIEW_SOCKS_PORT = 10991
        private const val PREVIEW_HOME = "<AETHER_HOME>"
        const val MARKER = "ZX_AETHER"
        private val PROTOCOLS = setOf("masque", "wg", "gool", "mim")
        private val SCAN_MODES = setOf("turbo", "balanced", "thorough", "verified", "ironclad")
        private val IP_VERSIONS = setOf("v4", "v6", "both")
        private val NOIZE_PROFILES = setOf("off", "light", "firewall", "balanced", "gfw", "aggressive")

        fun decode(raw: String): AetherProfile? = runCatching {
            val json = Json { ignoreUnknownKeys = true }
            val obj = json.parseToJsonElement(raw) as? kotlinx.serialization.json.JsonObject
            val marked = obj?.get("marker")?.toString() == "\"ZX_AETHER\""
            // Profiles saved by an older build omitted the marker (defaults were not encoded), so
            // they were mistaken for Xray configs. Recognize them by their Aether-only keys; Xray
            // and sing-box configs never contain scanMode + noize + ipVersion together.
            val legacy = obj != null && "marker" !in obj && "scanMode" in obj && "noize" in obj &&
                "ipVersion" in obj && ((obj["protocol"]?.toString()?.trim('"') ?: "") in PROTOCOLS)
            if (marked || legacy) json.decodeFromString<AetherProfile>(raw) else null
        }.getOrNull()?.takeIf { it.marker == MARKER }?.let { decoded ->
            // Smart profiles saved before this field existed keep their intended failover behavior.
            if (decoded.profileName.startsWith("Aether · Smart")) decoded.copy(smartFailover = true) else decoded
        }

        fun encode(profile: AetherProfile): String =
            Json { encodeDefaults = true }.encodeToString(profile.copy(marker = MARKER))
    }
}
