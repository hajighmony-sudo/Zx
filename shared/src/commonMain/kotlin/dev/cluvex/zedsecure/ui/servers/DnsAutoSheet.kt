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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.cluvex.zedsecure.domain.config.MasterDnsProfile
import dev.cluvex.zedsecure.platform.DnsRecordType
import dev.cluvex.zedsecure.platform.DnsResult
import dev.cluvex.zedsecure.platform.DnsTransport
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
    initialProfile: MasterDnsProfile? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, MasterDnsProfile) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember(initialProfile) { mutableStateOf("DNS AUTO · MasterDNS") }
    var domain by remember(initialProfile) { mutableStateOf(initialProfile?.domains.orEmpty()) }
    var key by remember(initialProfile) { mutableStateOf(initialProfile?.encryptionKey.orEmpty()) }
    var method by remember(initialProfile) { mutableStateOf(initialProfile?.encryptionMethod ?: MasterDnsProfile.ENC_XOR) }
    var resolvers by remember(initialProfile) { mutableStateOf(initialProfile?.resolvers.orEmpty()) }
    var results by remember { mutableStateOf<List<DnsAutoResolver>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf<String?>(null) }

    val invalid = MasterDnsProfile.invalidResolvers(resolvers)
    val valid = domain.isNotBlank() && key.isNotBlank() &&
        MasterDnsProfile.splitResolvers(resolvers).isNotEmpty() && invalid.isEmpty()

    suspend fun scanResolvers() {
        scanning = true
        scanMessage = "Checking the current network and public DNS resolvers…"
        results = emptyList()
        try {
            var found = DnsAutoDiscovery.scan(domain)
            if (found.isEmpty()) {
                scanMessage = "No DNS resolver responded; retrying once…"
                found = DnsAutoDiscovery.scan(domain)
            }
            results = found
            if (found.isNotEmpty()) {
                resolvers = found.take(4).joinToString("\n") { it.address }
                scanMessage = "Found ${found.size} responding resolvers. The fastest responders are selected."
            } else {
                scanMessage = "No resolver passed the test. You can retry or enter a trusted numeric resolver manually."
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            scanMessage = error.message?.take(180) ?: "Resolver scan failed."
        } finally {
            scanning = false
        }
    }

    // Discovery starts automatically when the DNS AUTO panel opens.
    LaunchedEffect(Unit) { scanResolvers() }

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
                "Built-in public resolver discovery with automatic MasterDNS-first setup.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "The first version scans ZX's bundled public-resolver catalogue. Network Checker and SlipNet are reference projects, not live feeds here. A working resolver is not a tunnel server: enter the domain and encryption key supplied by your DNS-tunnel provider.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Profile name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = domain,
                onValueChange = { domain = it },
                label = { Text("MasterDNS tunnel domain") },
                placeholder = { Text("tunnel.example.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text("MasterDNS encryption key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Encryption", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    MasterDnsProfile.ENC_XOR to "XOR",
                    MasterDnsProfile.ENC_CHACHA20 to "ChaCha20",
                    MasterDnsProfile.ENC_AES128 to "AES-128",
                    MasterDnsProfile.ENC_AES256 to "AES-256",
                ).forEach { (value, label) ->
                    TextButton(onClick = { method = value }) {
                        Text(
                            if (method == value) "✓ $label" else label,
                            color = if (method == value) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Button(
                onClick = { scope.launch { scanResolvers() } },
                enabled = !scanning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (scanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 10.dp),
                        strokeWidth = 2.dp,
                    )
                    Text("Scanning all resolver sources…")
                } else {
                    Text("Discover & test DNS resolvers")
                }
            }

            scanMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            if (results.isNotEmpty()) {
                Text("Verified resolver candidates", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                results.forEach { item ->
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

            OutlinedTextField(
                value = resolvers,
                onValueChange = { resolvers = it },
                label = { Text("Resolver IPs") },
                placeholder = { Text("1.1.1.1\n8.8.8.8") },
                minLines = 2,
                isError = invalid.isNotEmpty(),
                supportingText = {
                    Text(
                        if (invalid.isNotEmpty()) "MasterDNS requires numeric IPv4/IPv6 resolver addresses."
                        else "You can edit the discovered list or enter numeric IP addresses manually.",
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                "Engine: Auto → MasterDNS priority    •    Routing: all device apps (ZX itself excluded)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "DoH URLs and Cloudflare clean IPs are not inserted as MasterDNS resolvers. DoH belongs to the separate DNSTT/VayDNS profile flow; clean IPs are not DNS resolvers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = {
                    onSave(
                        name.trim().ifBlank { "DNS AUTO · MasterDNS" },
                        MasterDnsProfile(
                            domains = domain.trim(),
                            encryptionKey = key,
                            encryptionMethod = method,
                            resolvers = resolvers.trim(),
                            listenPort = 18000,
                            balancingStrategy = 3,
                            packetDuplication = 3,
                            compression = 0,
                            autoDisableDeadResolvers = true,
                        ),
                    )
                },
                enabled = valid && !scanning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save & connect automatically", fontWeight = FontWeight.SemiBold)
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

    private val candidates = listOf(
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
        return candidates.chunked(8).flatMap { group ->
            coroutineScope {
                group.map { candidate ->
                    async(Dispatchers.Default) {
                        val label = "zx-auto-${Random.nextInt(100000, 999999)}"
                        val result = try {
                            dnsQuery(
                                server = candidate.address,
                                port = 53,
                                name = "$label.$suffix",
                                type = DnsRecordType.A,
                                transport = DnsTransport.UDP,
                                timeoutMs = 1800,
                            )
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
