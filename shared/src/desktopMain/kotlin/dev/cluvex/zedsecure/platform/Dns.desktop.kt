package dev.cluvex.zedsecure.platform

/** Desktop DNS discovery is not exposed by the shared networking abstraction. */
actual fun currentNetworkDnsServers(): List<String> = emptyList()
