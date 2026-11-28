package dev.cluvex.zedsecure.core.ghost

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Live Ghost Status Dashboard manager.
 * DPI ladder, country net probe, DNS health, exit advice, stealth hints.
 * Complements GhostEngine — does not modify other features.
 */
object GhostStatusManager {

    private val detector = GhostDpiDetector()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null

    @Volatile private var status: GhostStatusAdvanced = GhostStatusAdvanced()
    @Volatile private var currentProfileId: String = "auto"
    @Volatile private var consecutiveFailures: Int = 0
    @Volatile private var currentLayer: GhostLayer = GhostLayer.SOFT
    @Volatile private var lastCountryReport: GhostCountryNetProbe.CountryReport? = null
    @Volatile private var lastDnsReport: GhostDnsHealth.Report? = null
    @Volatile private var lastExitAdvice: GhostExitAdvisor.Advice? = null

    fun current(): GhostStatusAdvanced = status
    fun isRunning(): Boolean = status.enabled
    fun lastCountryReport(): GhostCountryNetProbe.CountryReport? = lastCountryReport
    fun lastDnsReport(): GhostDnsHealth.Report? = lastDnsReport
    fun lastExitAdvice(): GhostExitAdvisor.Advice? = lastExitAdvice

    suspend fun onEnabled(profileId: String = "auto") {
        currentProfileId = profileId
        consecutiveFailures = 0
        currentLayer = GhostLayer.SOFT
        status = GhostStatusAdvanced(
            enabled = true,
            activeProfile = profileId,
            currentLayer = GhostLayer.SOFT,
            visibility = VisibilityState.ADAPTING,
            message = "Ghost Mode در حال راه‌اندازی…"
        )
        tick()
        startAutoTuneLoop()
    }

    fun onDisabled() {
        loopJob?.cancel()
        loopJob = null
        status = GhostStatusAdvanced(enabled = false, message = "Ghost Mode خاموش")
        consecutiveFailures = 0
        currentLayer = GhostLayer.SOFT
    }

    private fun startAutoTuneLoop() {
        loopJob?.cancel()
        val profile = GhostLayerProfiles.get(currentProfileId)
        val intervalMs = (profile.autoTuneIntervalSec.coerceIn(30, 60) * 1000L)
        loopJob = scope.launch {
            while (isActive && status.enabled) {
                delay(intervalMs)
                if (status.enabled) runCatching { tick() }
            }
        }
    }

    suspend fun tick(): GhostStatusAdvanced {
        if (!status.enabled) return status
        val detection = detector.detect()
        val now = System.currentTimeMillis()
        val iranTime = formatIranTime(now)
        val profile = GhostLayerProfiles.get(currentProfileId)

        val visibility = when {
            detection.level == DpiLevel.NONE -> VisibilityState.INVISIBLE
            detection.level <= DpiLevel.LOW -> VisibilityState.UNDER_PRESSURE
            else -> VisibilityState.ADAPTING
        }

        var action = "hold"
        if (detection.level >= DpiLevel.MEDIUM || consecutiveFailures >= 2) {
            val next = (currentLayer.level + 1).coerceAtMost(profile.maxLayer.level)
            currentLayer = GhostLayer.fromLevel(next)
            consecutiveFailures = 0
            action = "escalate_to_${currentLayer.name}"
        } else if (detection.level == DpiLevel.NONE) {
            consecutiveFailures = 0
            action = "stable"
        } else {
            consecutiveFailures++
            action = "retry_current"
        }

        val successRate = if (detection.probes.isEmpty()) 0
        else (detection.probes.count { it.success && !it.resetDetected } * 100 / detection.probes.size)

        status = GhostStatusAdvanced(
            enabled = true,
            currentLayer = currentLayer,
            dpiLevel = detection.level,
            dpiTypes = detection.types,
            lastCheckEpochMs = now,
            lastCheckIranTime = iranTime,
            visibility = visibility,
            activeProfile = currentProfileId,
            successRatePercent = successRate,
            message = buildMessage(detection, currentLayer, action),
            trafficBeforeBytes = status.trafficBeforeBytes,
            trafficAfterBytes = status.trafficAfterBytes
        )
        return status
    }

    suspend fun forceDetect(): GhostStatusAdvanced {
        val detection = detector.detect()
        val now = System.currentTimeMillis()
        val iranTime = formatIranTime(now)
        val visibility = when (detection.level) {
            DpiLevel.NONE -> VisibilityState.INVISIBLE
            DpiLevel.LOW -> VisibilityState.UNDER_PRESSURE
            else -> VisibilityState.ADAPTING
        }
        status = status.copy(
            dpiLevel = detection.level,
            dpiTypes = detection.types,
            lastCheckEpochMs = now,
            lastCheckIranTime = iranTime,
            visibility = visibility,
            successRatePercent = if (detection.probes.isEmpty()) 0
            else (detection.probes.count { it.success } * 100 / detection.probes.size),
            message = "تست دستی انجام شد • سطح DPI: ${detection.level.name}"
        )
        return status
    }

    suspend fun probeCountries(countryCodes: List<String> = emptyList()): GhostCountryNetProbe.CountryReport {
        val report = GhostCountryNetProbe.probe(countryCodes)
        lastCountryReport = report
        lastExitAdvice = GhostExitAdvisor.advise(report)
        if (status.enabled) {
            status = status.copy(
                message = "اسکن کشور: ${report.summaryFa}",
                lastCheckEpochMs = report.timestampMs,
                lastCheckIranTime = report.iranTime
            )
        }
        return report
    }

    suspend fun checkDns(): GhostDnsHealth.Report {
        val report = GhostDnsHealth.check()
        lastDnsReport = report
        if (status.enabled) {
            status = status.copy(message = report.summaryFa)
        }
        return report
    }

    fun stealthPlan(): GhostStealthKit.DecoyPlan {
        val underPressure = status.visibility == VisibilityState.UNDER_PRESSURE ||
            status.visibility == VisibilityState.ADAPTING ||
            status.dpiLevel >= DpiLevel.MEDIUM
        return GhostStealthKit.planForLayer(currentLayer, underPressure)
    }

    fun buildEngineHints(): Map<String, Any> {
        val profile = GhostLayerProfiles.get(currentProfileId)
        val hints = mutableMapOf<String, Any>()
        hints["current_layer"] = currentLayer.name
        hints["dpi_level"] = status.dpiLevel.name
        hints["profile"] = currentProfileId

        if (currentLayer.level >= GhostLayer.SOFT.level) {
            hints["reality"] = profile.useReality
            hints["utls"] = profile.useUTLS
            hints["utls_fingerprint"] = profile.uTLSFingerprint
            hints["xhttp"] = profile.useXHTTP
            hints["fragment"] = profile.fragmentClientHello
            hints["fragment_length"] = profile.fragmentLength
            hints["fragment_interval"] = profile.fragmentInterval
        }
        if (currentLayer.level >= GhostLayer.HARD.level) {
            hints["desync_mode"] = profile.desyncMode
            hints["fake_packets"] = profile.fakePackets
            hints["wrong_seq"] = profile.wrongSeq
            hints["wrong_chksum"] = profile.wrongChksum
            hints["ttl_trick"] = profile.ttlTrick
        }
        if (currentLayer.level >= GhostLayer.SHAPING.level) {
            hints["noise"] = profile.noiseEnabled
            hints["noise_bytes"] = profile.noiseBytes
            hints["padding"] = profile.paddingEnabled
            hints["traffic_shaping"] = profile.trafficShaping
        }
        lastCountryReport?.bestCountryCode?.let { hints["best_exit_country"] = it }
        lastExitAdvice?.preferredCountryCodes?.let { hints["preferred_exits"] = it }
        val stealth = stealthPlan()
        hints["decoy_hosts"] = stealth.hosts
        hints["decoy_parallel"] = stealth.parallel
        return hints
    }

    private fun buildMessage(
        detection: GhostDpiDetector.DetectionResult,
        layer: GhostLayer,
        action: String
    ): String = when {
        detection.level == DpiLevel.NONE -> "DPI تشخیص داده نشد • لایه ${layer.titleFa}"
        action.startsWith("escalate") -> "تطبیق سطح • رفتن به ${layer.titleFa}"
        else -> "وضعیت: ${detection.level.name} • لایه فعال: ${layer.titleFa}"
    }

    private fun formatIranTime(epochMs: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("Asia/Tehran") }
            .format(Date(epochMs)) + " (به وقت ایران)"
    }
}
