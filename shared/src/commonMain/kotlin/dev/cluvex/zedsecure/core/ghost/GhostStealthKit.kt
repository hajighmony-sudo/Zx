package dev.cluvex.zedsecure.core.ghost

import kotlin.random.Random

/**
 * Soft stealth helpers for Ghost Mode (application-layer hints only).
 * Builds decoy request patterns, padding ranges and User-Agent rotation
 * without relying on any single CDN (no Cloudflare-specific endpoints).
 * Ghost Mode only — does not change VPN cores or other features.
 */
object GhostStealthKit {

    data class DecoyPlan(
        val hosts: List<String>,
        val userAgents: List<String>,
        val paddingBytes: IntRange,
        val interRequestJitterMs: IntRange,
        val parallel: Int,
        val noteFa: String
    )

    data class FragmentHint(
        val enabled: Boolean,
        val lengthRange: String,
        val intervalRange: String,
        val packets: Int
    )

    private val decoyHosts = listOf(
        "www.digikala.com",
        "www.aparat.com",
        "www.varzesh3.com",
        "www.microsoft.com",
        "www.apple.com",
        "www.samsung.com",
        "www.sony.com",
        "www.hetzner.com",
        "www.bbc.co.uk",
        "www.yandex.ru"
    )

    private val userAgents = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3 Safari/605.1.15",
        "Mozilla/5.0 (X11; Linux x86_64; rv:122.0) Gecko/20100101 Firefox/122.0",
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3 Mobile/15E148 Safari/604.1"
    )

    fun planForLayer(layer: GhostLayer, underPressure: Boolean): DecoyPlan {
        val count = when {
            underPressure || layer.level >= GhostLayer.HARD.level -> 6
            layer.level >= GhostLayer.SOFT.level -> 4
            else -> 2
        }
        val hosts = decoyHosts.shuffled().take(count)
        val uas = userAgents.shuffled().take(3)
        return DecoyPlan(
            hosts = hosts,
            userAgents = uas,
            paddingBytes = if (underPressure) 32..256 else 8..96,
            interRequestJitterMs = if (underPressure) 15..80 else 40..160,
            parallel = if (underPressure) 3 else 2,
            noteFa = if (underPressure) "نویز سنگین چندفروشنده" else "نویز سبک چندفروشنده"
        )
    }

    fun fragmentHint(profile: GhostLayerProfiles.LayerConfig, layer: GhostLayer): FragmentHint {
        val on = profile.fragmentClientHello && layer.level >= GhostLayer.SOFT.level
        return FragmentHint(
            enabled = on,
            lengthRange = profile.fragmentLength,
            intervalRange = profile.fragmentInterval,
            packets = if (layer.level >= GhostLayer.HARD.level) profile.fakePackets else (profile.fakePackets / 2).coerceAtLeast(1)
        )
    }

    fun randomUserAgent(): String = userAgents[Random.nextInt(userAgents.size)]
}
