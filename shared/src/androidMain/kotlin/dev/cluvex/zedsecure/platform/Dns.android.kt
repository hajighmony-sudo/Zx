package dev.cluvex.zedsecure.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Reads DNS servers from available physical networks.
 * VPN-provided resolvers are excluded to avoid selecting the tunnel's own DNS.
 */
actual fun currentNetworkDnsServers(): List<String> = runCatching {
    val context = AndroidNetworkContext.appContext ?: return@runCatching emptyList()
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return@runCatching emptyList()

    val candidates = manager.allNetworks.mapNotNull { network ->
        val capabilities = manager.getNetworkCapabilities(network) ?: return@mapNotNull null
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return@mapNotNull null
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return@mapNotNull null
        }

        val dns = manager.getLinkProperties(network)?.dnsServers
            ?.mapNotNull { it.hostAddress?.substringBefore('%') }
            ?.filter { it.isNotBlank() && it != "0.0.0.0" && it != "::" }
            ?.distinct()
            .orEmpty()

        if (dns.isEmpty()) return@mapNotNull null
        val validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        validated to dns
    }

    candidates.sortedByDescending { it.first }.flatMap { it.second }.distinct()
}.getOrDefault(emptyList())
