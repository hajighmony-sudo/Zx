@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.cluvex.zedsecure.ui.servers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.cluvex.zedsecure.domain.config.MasterDnsProfile
import dev.cluvex.zedsecure.platform.DnsRecordType
import dev.cluvex.zedsecure.platform.DnsResult
import dev.cluvex.zedsecure.platform.DnsTransport
import dev.cluvex.zedsecure.platform.currentNetworkDnsServers
import dev.cluvex.zedsecure.platform.dnsQuery
import dev.cluvex.zedsecure.ui.platform.appVerticalScroll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun DnsAutoSheet(
    availableProfiles: List<dev.cluvex.zedsecure.domain.config.VpnProfile>,
    onDismiss: () -> Unit,
    onSave: (name: String, MasterDnsProfile) -> Unit,
) {
    val eligibleProfiles = remember(availableProfiles) {
        availableProfiles.filterNot { it.isLocked }.mapNotNull { profile ->
            profile.masterDnsSettings()
                ?.takeIf { it.domains.isNotBlank() && it.encryptionKey.isNotBlank() }
                ?.let { profile to it }
        }
    }
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<Pair<dev.cluvex.zedsecure.domain.config.VpnProfile, MasterDnsProfile>?>(null) }
    var results by remember { mutableStateOf<List<DnsAutoResolver>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf<String?>(null) }

    suspend fun discoverAndSelect() {
        if (eligibleProfiles.isEmpty()) {
            selected = null
            results = emptyList()
            scanMessage = "No saved MasterDNS tunnel endpoint is available. DNS AUTO needs a valid tunnel domain and encryption key from an existing server profile; public DNS resolvers cannot generate those credentials."
            return
        }
        scanning = true
        selected = null
        results = emptyList()
        scanMessage = "Checking saved MasterDNS tunnel profiles and testing network/public resolvers…"
        try {
            val rankedProfiles = coroutineScope {
                eligibleProfiles.take(4).map { (profile, settings) ->
                    async(Dispatchers.Default) {
                        val found = DnsAutoDiscovery.scan(settings.domains)
                        if (found.isEmpty()) null else Triple(profile, settings, found)
                    }
                }.awaitAll().filterNotNull()
            }.sortedBy { it.third.first().latencyMs }

            val best = rankedProfiles.firstOrNull()
            if (best == null) {
                scanMessage = "No saved MasterDNS endpoint passed resolver discovery. Check the source profile or network and retry."
            } else {
                val tuned = best.second.copy(
                    resolvers = best.third.take(4).joinToString("\n") { it.address },
                    autoDisableDeadResolvers = true,
                )
                selected = best.first to tuned
                results = best.third
                scanMessage = "Selected ${best.first.name}; ${best.third.size} working resolvers found. The fastest candidates are configured automatically."
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            scanMessage = error.message?.take(180) ?: "DNS AUTO discovery failed."
        } finally {
            scanning = false
        }
    }

    LaunchedEffect(eligibleProfiles) { discoverAndSelect() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            SheetValue.Hidden,
            setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .appVerticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("DNS AUTO", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Automatic MasterDNS selection · resolver discovery · device-wide VPN routing",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "ZX checks saved MasterDNS tunnel endpoints, discovers DNS servers from the active network and its resolver catalogue, tests them, and selects the best responding endpoint/resolver combination. It then creates a DNS AUTO profile and starts the existing MasterDNS VPN engine.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (eligibleProfiles.isEmpty()) {
                Text(
                    "No ready MasterDNS endpoint was found in your saved servers. A DNS tunnel requires a real server domain and its matching encryption key; public DNS IPs alone cannot replace them. Add/import a valid MasterDNS endpoint once, then DNS AUTO can select and tune it automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Text(
                    "Tunnel endpoint: ${selected?.first?.name ?: if (scanning) "Searching saved profiles…" else "Not selected"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                selected?.second?.domains?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Button(
                onClick = { scope.launch { discoverAndSelect() } },
                enabled = !scanning && eligibleProfiles.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (scanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 10.dp),
                        strokeWidth = 2.dp,
                    )
                    Text("Discovering and testing…")
                } else {
                    Text("Run DNS AUTO scan")
                }
            }

            scanMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            if (results.isNotEmpty()) {
                Text("Automatically selected resolvers", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                results.take(4).forEach { item ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.address, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(item.source, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${item.latencyMs} ms", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Text(
                "Routing: all device apps through the VPN tunnel (ZX itself excluded) · Dead-resolver handling enabled · Network-change recovery handled by the VPN service",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = {
                    val chosen = selected ?: return@Button
                    onSave("DNS AUTO · ${chosen.first.name}", chosen.second)
                },
                enabled = selected != null && !scanning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save DNS AUTO & connect", fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Cancel")
            }
        }
    }
}

private data class DnsAutoResolver(
    val address: String,
    val source: String,
    val latencyMs: Long,
)

private object DnsAutoDiscovery {
    private data class Candidate(val address: String, val source: String)

    private val publicCandidates = listOf(
        Candidate("1.1.1.1", "Cloudflare"),
        Candidate("1.0.0.1", "Cloudflare"),
        Candidate("8.8.8.8", "Google Public DNS"),
        Candidate("8.8.4.4", "Google Public DNS"),
        Candidate("9.9.9.9", "Quad9"),
        Candidate("149.112.112.112", "Quad9"),
        Candidate("208.67.222.222", "OpenDNS"),
        Candidate("208.67.220.220", "OpenDNS"),
        Candidate("94.140.14.14", "AdGuard DNS"),
        Candidate("94.140.15.15", "AdGuard DNS"),
        Candidate("76.76.19.19", "Control D"),
        Candidate("76.76.2.0", "Control D"),
        Candidate("185.228.168.9", "CleanBrowsing"),
        Candidate("185.228.169.9", "CleanBrowsing"),
        Candidate("45.90.28.0", "NextDNS"),
        Candidate("45.90.30.0", "NextDNS"),
    )

    suspend fun scan(tunnelDomain: String): List<DnsAutoResolver> {
        val suffix = tunnelDomain.split(',', '\n').map { it.trim().trim('.') }
            .firstOrNull { it.isNotBlank() } ?: "example.com"
        val candidates = (currentNetworkDnsServers().map { Candidate(it, "Current network") } + publicCandidates)
            .distinctBy { it.address }
        return candidates.chunked(16).flatMap { group ->
            coroutineScope {
                group.map { candidate ->
                    async(Dispatchers.Default) {
                        val label = "zx-auto-${Random.nextInt(100000, 999999)}"
                        val result = try {
                            val udp = dnsQuery(
                                server = candidate.address,
                                port = 53,
                                name = "$label.$suffix",
                                type = DnsRecordType.A,
                                transport = DnsTransport.UDP,
                                timeoutMs = 1800,
                            )
                            if ((udp as? DnsResult.Ok)?.reply?.truncated == true) {
                                dnsQuery(
                                    server = candidate.address,
                                    port = 53,
                                    name = "$label.$suffix",
                                    type = DnsRecordType.A,
                                    transport = DnsTransport.TCP,
                                    timeoutMs = 1800,
                                )
                            } else udp
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Throwable) {
                            null
                        }
                        val reply = (result as? DnsResult.Ok)?.reply
                        if (reply != null && (reply.rcode == 0 || reply.rcode == 3)) {
                            DnsAutoResolver(candidate.address, candidate.source, reply.elapsedMs)
                        } else {
                            null
                        }
                    }
                }.awaitAll()
            }
        }.filterNotNull().sortedBy { it.latencyMs }
    }
}
