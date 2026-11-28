package dev.cluvex.zedsecure.core.ghost

import kotlinx.serialization.Serializable

/**
 * Advanced live status model for Ghost Mode (DPI detection + layers).
 * Complements existing GhostEngine.DebugStatus without replacing it.
 * Only Ghost Mode related — does not touch other app features.
 */
@Serializable
data class GhostStatusAdvanced(
    val enabled: Boolean = false,
    val currentLayer: GhostLayer = GhostLayer.SOFT,
    val dpiLevel: DpiLevel = DpiLevel.NONE,
    val dpiTypes: List<DpiType> = emptyList(),
    val lastCheckEpochMs: Long = 0L,
    val lastCheckIranTime: String = "",
    val visibility: VisibilityState = VisibilityState.UNKNOWN,
    val activeProfile: String = "auto",
    val successRatePercent: Int = 0,
    val message: String = "",
    val trafficBeforeBytes: Long = 0L,
    val trafficAfterBytes: Long = 0L
) {
    fun isHealthy(): Boolean = enabled && visibility == VisibilityState.INVISIBLE
}

@Serializable
enum class DpiLevel {
    NONE,       // هیچ DPI تشخیص داده نشد
    LOW,        // ضعیف
    MEDIUM,     // متوسط
    HIGH,       // سنگین
    EXTREME     // فوق‌سنگین
}

@Serializable
enum class DpiType {
    SNI_BLOCKING,
    TLS_FINGERPRINT,
    ACTIVE_PROBING,
    STATISTICAL_ML,
    UDP_THROTTLING,
    PROTOCOL_SIGNATURE,
    UNKNOWN
}

@Serializable
enum class VisibilityState {
    INVISIBLE,      // سبز — نامرئی
    UNDER_PRESSURE, // زرد — تحت فشار
    ADAPTING,       // قرمز — در حال تطبیق
    FAILED,         // قرمز — شکست
    UNKNOWN
}

@Serializable
enum class GhostLayer(val level: Int, val titleFa: String, val titleEn: String) {
    DETECTION(0, "تشخیص و وضعیت", "Detection & Status"),
    SOFT(1, "استتار نرم", "Soft Camouflage"),
    HARD(2, "دسینک سخت", "Hard Desync"),
    SHAPING(3, "شکل‌دهی آماری", "Statistical Shaping"),
    LADDER(4, "نردبان تطبیقی", "Adaptive Ladder"),
    FALLBACK(5, "مسیر جایگزین", "Fallback & Multi-Path");

    companion object {
        fun fromLevel(level: Int): GhostLayer =
            entries.firstOrNull { it.level == level } ?: SOFT
    }
}
