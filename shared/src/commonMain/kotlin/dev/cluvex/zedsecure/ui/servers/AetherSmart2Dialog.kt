package dev.cluvex.zedsecure.ui.servers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.cluvex.zedsecure.domain.config.AetherSmart2Profile
import dev.cluvex.zedsecure.domain.config.AetherSmartProbe
import dev.cluvex.zedsecure.platform.NetworkKind
import dev.cluvex.zedsecure.platform.detectNetworkKind
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Smart 2 dialog – fully network-driven.
 *
 * Only manual control exposed to the user: Protocol selection
 * (AUTOMATIC / MASQUE_H3 / MASQUE_H2 / WireGuard / Gool / MASQUE×2 / Hybrid).
 * All other fields (DNS, SNI, Noize, Fragment, ECH, Anti-DPI stack) are
 * derived from live probes of the user's current internet.
 */
@Composable
internal fun AetherSmart2Dialog(
    onDismiss: () -> Unit,
    onCreate: (AetherSmart2Profile) -> Unit,
) {
    var profile by remember { mutableStateOf<AetherSmart2Profile?>(null) }
    var summary by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(true) }
    var scanNonce by remember { mutableStateOf(0) }
    var protocolChoice by remember {
        mutableStateOf(AetherSmart2Profile.ProtocolChoice.AUTOMATIC)
    }

    LaunchedEffect(scanNonce, protocolChoice) {
        profile = null
        summary = ""
        scanning = true
        val network = detectNetworkKind()
        val infoJob = async {
            withTimeoutOrNull(5_000) {
                runCatching {
                    dev.cluvex.zedsecure.data.net.NetworkInfoRepository().fetchInfo(force = true)
                }.getOrNull()
            }
        }
        val probe = withTimeoutOrNull(25_000) {
            runCatching { AetherSmartProbe.run() }.getOrNull()
        }
        val info = infoJob.await()
        val ipv6 = !info?.ipv6.isNullOrBlank()
        val generated = AetherSmart2Profile.build(
            network = network,
            hasIpv6 = ipv6,
            probe = probe,
            protocolChoice = protocolChoice,
        )
        profile = generated
        val networkLabel = when (network) {
            NetworkKind.Cellular -> "Mobile"
            NetworkKind.Wifi -> "Wi-Fi"
            NetworkKind.Ethernet -> "Ethernet"
            NetworkKind.Unknown -> "Unknown"
        }
        summary = buildString {
            append(networkLabel)
            if (ipv6) append(" · IPv6")
            probe?.countryCode?.let { append(" · $it") }
            probe?.edgeMs?.let { append(" · edge ${it}ms") }
            append("\n")
            append(generated.antiDpi.summary)
            if (generated.preferredDns.isNotEmpty()) {
                append("\nDNS: ")
                append(generated.preferredDns.take(3).joinToString())
            }
            if (generated.preferredSnis.isNotEmpty()) {
                append("\nSNI: ")
                append(generated.preferredSnis.take(2).joinToString())
            }
        }
        scanning = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aether · Smart 2") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // ---------- Only manual control ----------
                Text(
                    "پروتکل (تنها انتخاب دستی)",
                    style = MaterialTheme.typography.labelMedium,
                )
                ProtocolChipRow(
                    selected = protocolChoice,
                    onSelect = { protocolChoice = it },
                )

                Spacer(Modifier.height(4.dp))

                if (scanning) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "در حال تشخیص شبکه، IPv6، DNS، SNI و سطح DPI…",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                if (profile != null) {
                    Text(profile!!.profileName, style = MaterialTheme.typography.titleSmall)
                    Text(summary, style = MaterialTheme.typography.bodySmall)

                    Text(
                        "Native Aether · ${profile!!.aetherProfile.protocol.uppercase()} · " +
                            "${profile!!.aetherProfile.scanMode} scan · ${profile!!.aetherProfile.noize} noize · " +
                            "${if (profile!!.aetherProfile.http2) "HTTP/2" else "native transport"} · " +
                            "${if (profile!!.aetherProfile.ech) "ECH auto" else "ECH off"} · " +
                            "${if (profile!!.aetherProfile.fragment) "fragmented ClientHello" else "normal ClientHello"} · " +
                            "${if (profile!!.aetherProfile.smartFailover) "Smart failover" else "single route"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    if (profile!!.antiDpi.mode == "dual") {
                        Text(
                            "⚔ Dual-Attack فعال: گلوله (Reality Punch) + خمپاره (جدیدترین + قدیمی‌ترین روش‌ها)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }

                    Text(
                        "Xray DNS/Routing · Iranian DNS priority · FakeDNS · SOCKS / HTTP / Mixed · IPv4/IPv6",
                        style = MaterialTheme.typography.bodySmall,
                    )

                    OutlinedTextField(
                        value = profile!!.commandPreview(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Native Aether command · generated automatically") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = profile != null && !scanning,
                onClick = { profile?.let(onCreate) },
            ) {
                Text("Create and activate")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { scanNonce++ }) {
                    Text("Scan again")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
    )
}

@Composable
private fun ProtocolChipRow(
    selected: AetherSmart2Profile.ProtocolChoice,
    onSelect: (AetherSmart2Profile.ProtocolChoice) -> Unit,
) {
    val options = listOf(
        AetherSmart2Profile.ProtocolChoice.AUTOMATIC to "خودکار",
        AetherSmart2Profile.ProtocolChoice.MASQUE_H3 to "MASQUE H3",
        AetherSmart2Profile.ProtocolChoice.MASQUE_H2 to "MASQUE H2",
        AetherSmart2Profile.ProtocolChoice.WIREGUARD to "WireGuard",
        AetherSmart2Profile.ProtocolChoice.GOOL to "Gool",
        AetherSmart2Profile.ProtocolChoice.MASQUE_X2 to "MASQUE×2",
        AetherSmart2Profile.ProtocolChoice.HYBRID_SMART to "Hybrid",
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { (value, label) ->
                    FilterChip(
                        selected = selected == value,
                        onClick = { onSelect(value) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }
    }
}
