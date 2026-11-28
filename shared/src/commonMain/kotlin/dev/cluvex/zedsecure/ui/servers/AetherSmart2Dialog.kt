package dev.cluvex.zedsecure.ui.servers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Modifier
import dev.cluvex.zedsecure.domain.config.AetherSmart2Profile
import dev.cluvex.zedsecure.domain.config.AetherSmartProbe
import dev.cluvex.zedsecure.platform.NetworkKind
import dev.cluvex.zedsecure.platform.detectNetworkKind
import dev.cluvex.zedsecure.shared.resources.Res
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AetherSmart2Dialog(
    onDismiss: () -> Unit,
    onCreate: (AetherSmart2Profile) -> Unit,
) {
    var profile by remember { mutableStateOf<AetherSmart2Profile?>(null) }
    var summary by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(true) }
    var scanNonce by remember { mutableStateOf(0) }

    LaunchedEffect(scanNonce) {
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
        val generated = AetherSmart2Profile.build(network, ipv6, probe)
        profile = generated
        val networkLabel = when (network) {
            NetworkKind.Cellular -> "Mobile"
            NetworkKind.Wifi -> "Wi-Fi"
            NetworkKind.Ethernet -> "Ethernet"
            NetworkKind.Unknown -> "Unknown"
        }
        summary = buildString {
            append(networkLabel)
            append(" · ")
            append(if (ipv6) "IPv4 + IPv6" else "IPv4")
            append(" · ")
            append(probe?.edgeMs?.let { "RTT $it ms" } ?: "RTT unavailable")
            append(" · DNS ")
            append(generated.dnsPreference)
            append(" · ")
            append(if (probe?.dnsSystemMatchesDoh == false) "resolver mismatch detected" else "resolver comparison inconclusive or consistent")
        }
        scanning = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.aether_smart2_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(Res.string.aether_smart2_body), style = MaterialTheme.typography.bodySmall)
                if (scanning) {
                    CircularProgressIndicator()
                    Text("Detecting network, IPv6, DNS and endpoint health…", style = MaterialTheme.typography.bodySmall)
                }
                if (profile != null) {
                    Text(profile!!.profileName, style = MaterialTheme.typography.titleSmall)
                    Text(summary, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Xray routing · DoH · FakeDNS · SOCKS / HTTP / Mixed · Psiphon CDN",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    OutlinedTextField(
                        value = profile!!.commandPreview(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Aether command · generated automatically") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = profile != null && !scanning, onClick = { profile?.let(onCreate) }) {
                Text(stringResource(Res.string.aether_smart2_create))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { scanNonce++ }) { Text(stringResource(Res.string.aether_smart_rescan)) }
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
            }
        },
    )
}
