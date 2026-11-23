package dev.cluvex.zedsecure.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Reads DNS servers advertised by the active physical network.
 * VPN-provided DNS is deliberately ignored to avoid feeding a tunnel's own
 * virtual resolver back into DNS AUTO as an upstream.
 */
actual fun currentNetworkDnsServers(): List<String> = runCatching {
    val context = AndroidNetworkContext.appContext ?: return@runCatching emptyList()
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return@runCatching emptyList()
    val network = manager.activeNetwork ?: return@runCatching emptyList()
    val capabilities = manager.getNetworkCapabilities(network)
        ?: return@runCatching emptyList()
    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
        return@runCatching emptyList()
    }
    manager.getLinkProperties(network)?.dnsServers
        ?.mapNotNull { it.hostAddress?.substringBefore('%') }
        ?.filter { it.isNotBlank() && it != "0.0.0.0" && it != "::" }
        ?.distinct()
        .orEmpty()
}.getOrDefault(emptyList())
