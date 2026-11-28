package dev.cluvex.zedsecure.core.ghost

/**
 * Pre-defined strong anti-DPI profiles for different networks (2026).
 * Used by the adaptive ladder and status dashboard.
 * Only Ghost Mode related — does not alter other engines or UI pages.
 */
object GhostLayerProfiles {

    val all = listOf("auto", "iran_mobile", "iran_home", "russia", "china", "custom")

    data class LayerConfig(
        val profileId: String,
        val maxLayer: GhostLayer,
        val autoTuneIntervalSec: Int,
        val useReality: Boolean = true,
        val useUTLS: Boolean = true,
        val uTLSFingerprint: String = "chrome",
        val useXHTTP: Boolean = true,
        val fragmentClientHello: Boolean = true,
        val fragmentLength: String = "50-100",
        val fragmentInterval: String = "10-20",
        val desyncMode: String = "fake,multisplit",
        val fakePackets: Int = 4,
        val wrongSeq: Boolean = true,
        val wrongChksum: Boolean = false,
        val ttlTrick: Boolean = true,
        val noiseEnabled: Boolean = true,
        val noiseBytes: Int = 4,
        val paddingEnabled: Boolean = true,
        val trafficShaping: Boolean = true
    )

    fun get(profileId: String): LayerConfig = when (profileId) {
        "iran_mobile" -> iranMobile()
        "iran_home"   -> iranHome()
        "russia"      -> russia()
        "china"       -> china()
        "custom"      -> LayerConfig(profileId = "custom", maxLayer = GhostLayer.FALLBACK, autoTuneIntervalSec = 45)
        else          -> auto()
    }

    fun auto() = LayerConfig(
        profileId = "auto",
        maxLayer = GhostLayer.FALLBACK,
        autoTuneIntervalSec = 45,
        desyncMode = "fake,multisplit,disorder",
        fakePackets = 6
    )

    fun iranMobile() = LayerConfig(
        profileId = "iran_mobile",
        maxLayer = GhostLayer.FALLBACK,
        autoTuneIntervalSec = 35,
        fragmentLength = "40-80",
        fragmentInterval = "8-15",
        desyncMode = "fake,multisplit,disorder",
        fakePackets = 6,
        ttlTrick = true
    )

    fun iranHome() = LayerConfig(
        profileId = "iran_home",
        maxLayer = GhostLayer.LADDER,
        autoTuneIntervalSec = 50,
        fragmentLength = "50-120",
        fragmentInterval = "10-25",
        desyncMode = "fake,multisplit",
        fakePackets = 4,
        ttlTrick = false
    )

    fun russia() = LayerConfig(
        profileId = "russia",
        maxLayer = GhostLayer.FALLBACK,
        autoTuneIntervalSec = 40,
        desyncMode = "fake,multisplit,disorder",
        fakePackets = 8,
        wrongChksum = true,
        ttlTrick = true
    )

    fun china() = LayerConfig(
        profileId = "china",
        maxLayer = GhostLayer.FALLBACK,
        autoTuneIntervalSec = 30,
        fragmentLength = "30-70",
        desyncMode = "fake,multisplit,hostfakesplit",
        fakePackets = 10,
        ttlTrick = true
    )
}
