package dev.cluvex.zedsecure.domain.config

import kotlinx.serialization.Serializable

/**
 * Anti-DPI stack for Aether Smart 2.
 *
 * Method 1 – 11 discrete techniques:
 *   5 newest discovered capabilities (2025-2026)
 *   5 proven older techniques
 *   1 final combined profile
 *
 * Method 2 – Dual-attack metaphor (Bullet + Mortar):
 *   Bullet  = direct high-power Reality / interactive punch that tunnels through the DPI wall.
 *   Mortar  = newest anti-DPI methods that climb over the wall; warhead detaches mid-flight
 *             and is itself a hybrid of newest + oldest techniques.
 *
 * Selection is evidence-led from live network probes. No other ZX features are altered.
 */
@Serializable
data class AntiDpiStack(
    /** Selected primary technique id (1..11) or "dual" for Bullet+Mortar. */
    val mode: String = "auto",
    val bulletEnabled: Boolean = true,
    val mortarEnabled: Boolean = true,
    val techniques: List<String> = emptyList(),
    val primarySni: String = "",
    val fallbackSnis: List<String> = emptyList(),
    val noizeLevel: String = "firewall",
    val fragmentEnabled: Boolean = false,
    val fragmentSize: String = "",
    val fragmentDelay: String = "",
    val echEnabled: Boolean = false,
    val statisticalShaping: Boolean = false,
    val liveFingerprintMutation: Boolean = false,
    val multiPathMasque: Boolean = false,
    val summary: String = "",
) {
    companion object {
        // ---------- Newest (2025-2026) ----------
        const val T1_INTERACTIVE_REALITY_PUNCH = "interactive_reality_punch"   // Bullet core
        const val T2_ECH_ADAPTIVE_PADDING = "ech_adaptive_padding"
        const val T3_MULTI_PATH_MASQUE = "multi_path_masque"
        const val T4_STATISTICAL_TRAFFIC_SHAPING = "statistical_traffic_shaping"
        const val T5_LIVE_DPI_FINGERPRINT_MUTATION = "live_dpi_fingerprint_mutation"

        // ---------- Proven older ----------
        const val T6_NOIZE = "noize"
        const val T7_CLIENTHELLO_FRAGMENT = "clienthello_fragment"
        const val T8_SNI_SPOOF_DOMAIN_FRONT = "sni_spoof_domain_front"
        const val T9_TLS_RECORD_PADDING = "tls_record_padding"
        const val T10_UDP_TCP_HYBRID_DESYNC = "udp_tcp_hybrid_desync"

        // ---------- Combined ----------
        const val T11_FULL_COMBO = "full_combo"

        val ALL_TECHNIQUES = listOf(
            T1_INTERACTIVE_REALITY_PUNCH,
            T2_ECH_ADAPTIVE_PADDING,
            T3_MULTI_PATH_MASQUE,
            T4_STATISTICAL_TRAFFIC_SHAPING,
            T5_LIVE_DPI_FINGERPRINT_MUTATION,
            T6_NOIZE,
            T7_CLIENTHELLO_FRAGMENT,
            T8_SNI_SPOOF_DOMAIN_FRONT,
            T9_TLS_RECORD_PADDING,
            T10_UDP_TCP_HYBRID_DESYNC,
            T11_FULL_COMBO,
        )

        /**
         * Iranian-friendly and globally resilient SNI candidates.
         * Used for domain fronting / Reality / ECH cover.
         */
        val IRANIAN_SNIS = listOf(
            "www.digikala.com",
            "www.snapp.ir",
            "www.divar.ir",
            "www.aparat.com",
            "www.irancell.ir",
            "www.mci.ir",
            "www.telewebion.com",
            "www.sheypoor.com",
            "www.bankmellat.ir",
            "www.shaparak.ir",
        )

        val GLOBAL_COVER_SNIS = listOf(
            "www.cloudflare.com",
            "www.microsoft.com",
            "www.apple.com",
            "www.amazon.com",
            "cdnjs.cloudflare.com",
        )

        /**
         * Builds the optimal Anti-DPI stack from live probe evidence.
         * Does not touch any non-Smart-2 code paths.
         */
        fun buildFromProbe(
            probe: AetherSmartProbe.Result?,
            countryCode: String? = null,
            preferIranianAssets: Boolean = true,
        ): AntiDpiStack {
            val sniFiltered = probe?.tlsSniReachable == false
            val udpBad = probe?.udpReachable == false
            val tcpOk = probe?.tcp443Reachable != false
            val highLoss = (probe?.edgeLossPercent ?: 0) >= 30 || (probe?.apiLossPercent ?: 0) >= 30
            val highJitter = (probe?.edgeJitterMs ?: 0) >= 150 || (probe?.apiJitterMs ?: 0) >= 200
            val hostile = highLoss || sniFiltered || (probe?.ooniAnomalyPercent ?: 0) >= 55
            val isIran = countryCode.equals("IR", ignoreCase = true) || preferIranianAssets

            val snis = if (isIran) {
                IRANIAN_SNIS + GLOBAL_COVER_SNIS
            } else {
                GLOBAL_COVER_SNIS + IRANIAN_SNIS
            }
            val primarySni = snis.firstOrNull() ?: "www.cloudflare.com"
            val fallbacks = snis.drop(1).take(6)

            // Decide technique set
            val selected = mutableListOf<String>()
            var noize = "firewall"
            var fragment = false
            var fragSize = ""
            var fragDelay = ""
            var ech = false
            var shaping = false
            var mutation = false
            var multiPath = false
            var bullet = true
            var mortar = true
            var mode = "dual"

            when {
                // Extreme / hostile Iranian-style filtering
                hostile && sniFiltered && udpBad -> {
                    selected += listOf(
                        T1_INTERACTIVE_REALITY_PUNCH,
                        T2_ECH_ADAPTIVE_PADDING,
                        T3_MULTI_PATH_MASQUE,
                        T5_LIVE_DPI_FINGERPRINT_MUTATION,
                        T6_NOIZE,
                        T7_CLIENTHELLO_FRAGMENT,
                        T8_SNI_SPOOF_DOMAIN_FRONT,
                        T11_FULL_COMBO,
                    )
                    noize = "aggressive"
                    fragment = true
                    fragSize = "4-12"
                    fragDelay = "1-8"
                    ech = true
                    shaping = true
                    mutation = true
                    multiPath = true
                    mode = "dual"
                }
                // SNI filtering but UDP still usable
                sniFiltered -> {
                    selected += listOf(
                        T1_INTERACTIVE_REALITY_PUNCH,
                        T2_ECH_ADAPTIVE_PADDING,
                        T7_CLIENTHELLO_FRAGMENT,
                        T8_SNI_SPOOF_DOMAIN_FRONT,
                        T6_NOIZE,
                    )
                    noize = "gfw"
                    fragment = true
                    fragSize = "6-14"
                    fragDelay = "2-10"
                    ech = true
                    mode = "dual"
                }
                // High loss / jitter – prefer shaping + multi-path
                highLoss || highJitter -> {
                    selected += listOf(
                        T3_MULTI_PATH_MASQUE,
                        T4_STATISTICAL_TRAFFIC_SHAPING,
                        T6_NOIZE,
                        T9_TLS_RECORD_PADDING,
                    )
                    noize = "balanced"
                    shaping = true
                    multiPath = true
                    mode = "dual"
                }
                // Mild pressure
                (probe?.edgeLossPercent ?: 0) in 10..29 || (probe?.ooniAnomalyPercent ?: 0) in 25..54 -> {
                    selected += listOf(T6_NOIZE, T9_TLS_RECORD_PADDING, T1_INTERACTIVE_REALITY_PUNCH)
                    noize = "firewall"
                    mode = "bullet"
                    mortar = false
                }
                // Clean network
                else -> {
                    selected += listOf(T6_NOIZE)
                    noize = "light"
                    mode = "auto"
                    bullet = true
                    mortar = false
                }
            }

            // Always keep the combined technique available as last-resort ladder step
            if (T11_FULL_COMBO !in selected) selected += T11_FULL_COMBO

            val summary = buildString {
                append("Anti-DPI: ")
                when (mode) {
                    "dual" -> append("Dual-Attack (Bullet + Mortar)")
                    "bullet" -> append("Bullet (direct punch)")
                    else -> append("Auto")
                }
                append(" · techniques=${selected.size}")
                if (fragment) append(" · fragment")
                if (ech) append(" · ECH")
                if (shaping) append(" · shaping")
                if (mutation) append(" · live-mutation")
                if (multiPath) append(" · multi-path")
                append(" · SNI=$primarySni")
            }

            return AntiDpiStack(
                mode = mode,
                bulletEnabled = bullet,
                mortarEnabled = mortar,
                techniques = selected.distinct(),
                primarySni = primarySni,
                fallbackSnis = fallbacks,
                noizeLevel = noize,
                fragmentEnabled = fragment,
                fragmentSize = fragSize,
                fragmentDelay = fragDelay,
                echEnabled = ech,
                statisticalShaping = shaping,
                liveFingerprintMutation = mutation,
                multiPathMasque = multiPath,
                summary = summary,
            )
        }
    }
}
