package dev.cluvex.zedsecure.platform

/** Direct-path signals collected before a VPN tunnel is started. */
data class AetherTransportFingerprint(
    val udpReachable: Boolean? = null,
    val tcp443Reachable: Boolean? = null,
    val tlsSniReachable: Boolean? = null,
    val systemDnsAnswers: List<String>? = null,
)

/** Tests the active physical network before the VPN tunnel is started. */
internal expect suspend fun probeAetherTransportFingerprint(): AetherTransportFingerprint
