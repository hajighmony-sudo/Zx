package dev.cluvex.zedsecure.platform

/** Direct-path transport fingerprint. Null means the platform could not measure that signal. */
data class AetherTransportFingerprint(
    val udpReachable: Boolean? = null,
    val tlsSniReachable: Boolean? = null,
)

/** Tests the active physical network before the VPN tunnel is started. */
internal expect suspend fun probeAetherTransportFingerprint(): AetherTransportFingerprint
