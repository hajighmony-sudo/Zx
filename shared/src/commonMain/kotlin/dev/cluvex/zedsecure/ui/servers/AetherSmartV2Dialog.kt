package dev.cluvex.zedsecure.ui.servers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.cluvex.zedsecure.domain.config.AetherProfile
import dev.cluvex.zedsecure.domain.config.AetherSmartConfigV2
import dev.cluvex.zedsecure.domain.config.AetherSmartInput
import dev.cluvex.zedsecure.domain.config.AetherSmartProbe
import dev.cluvex.zedsecure.data.net.NetworkInfoRepository
import dev.cluvex.zedsecure.platform.NetworkKind
import dev.cluvex.zedsecure.platform.detectLinkQuality
import dev.cluvex.zedsecure.platform.detectNetworkKind
import kotlinx.coroutines.withTimeoutOrNull

@Composable
internal fun AetherSmartV2Dialog(
    onDismiss: () -> Unit,
    onCreate: (AetherProfile) -> Unit,
) {
    var profile by remember { mutableStateOf<AetherProfile?>(null) }
    var network by remember { mutableStateOf(NetworkKind.Unknown) }
    var summary by remember { mutableStateOf("در حال بررسی شبکه و کیفیت مسیرها…") }
    var details by remember { mutableStateOf<List<String>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var scanKey by remember { mutableStateOf(0) }

    LaunchedEffect(scanKey) {
        profile = null
        error = null
        summary = "در حال بررسی شبکه و کیفیت مسیرها…"
        details = emptyList()
        runCatching {
            val kind = detectNetworkKind()
            network = kind
            val link = detectLinkQuality()
            val info = withTimeoutOrNull(5_000) {
                runCatching { NetworkInfoRepository().fetchInfo(force = true) }.getOrNull()
            }
            val probe = runCatching { AetherSmartProbe.run() }.getOrElse {
                AetherSmartProbe.Result(edgeMs = null, apiMs = null)
            }
            val input = AetherSmartInput(
                network = kind,
                hasIpv6 = !info?.ipv6.isNullOrBlank(),
                pingMs = info?.pingMs,
                countryCode = probe.countryCode ?: info?.countryCode,
                edgeReachable = probe.edgeMs != null,
                edgeMs = probe.edgeMs,
                apiReachable = probe.apiMs != null,
                apiMs = probe.apiMs,
                edgeJitterMs = probe.edgeJitterMs,
                apiJitterMs = probe.apiJitterMs,
                link = link,
                edgeLossPercent = probe.edgeLossPercent,
                apiLossPercent = probe.apiLossPercent,
                udpReachable = probe.udpReachable,
                tlsSniReachable = probe.tlsSniReachable,
                tcp443Reachable = probe.tcp443Reachable,
                dnsSystemMatchesDoh = probe.dnsSystemMatchesDoh,
                ooniAnomalyPercent = probe.ooniAnomalyPercent,
            )
            profile = AetherSmartConfigV2.build(input)
            summary = "بررسی انجام شد؛ تنظیمات بر اساس وضعیت فعلی اینترنت ساخته شدند."
            details = listOf(
                "شبکه: $kind",
                "پینگ شبکه: ${info?.pingMs?.let { "$it ms" } ?: "نامشخص"}",
                "Cloudflare Edge: ${probe.edgeMs?.let { "$it ms" } ?: "در دسترس نیست"}",
                "WARP API: ${probe.apiMs?.let { "$it ms" } ?: "در دسترس نیست"}",
                "Jitter مسیر: ${probe.edgeJitterMs?.let { "$it ms" } ?: "نامشخص"}",
                "Packet loss مسیر: ${probe.edgeLossPercent}%",
                "UDP: ${probe.udpReachable?.let { if (it) "در دسترس" else "ناموفق" } ?: "نامشخص"}",
                "TCP/443: ${probe.tcp443Reachable?.let { if (it) "در دسترس" else "ناموفق" } ?: "نامشخص"}",
                "TLS/SNI: ${probe.tlsSniReachable?.let { if (it) "در دسترس" else "ناموفق" } ?: "نامشخص"}",
                "DNS: ${probe.dnsSystemMatchesDoh?.let { if (it) "هماهنگ" else "عدم تطابق" } ?: "نامشخص"}",
                "IPv6: ${if (!info?.ipv6.isNullOrBlank()) "فعال" else "شناسایی نشد"}",
            )
        }.onFailure {
            error = it.message ?: "خطا در بررسی شبکه"
            summary = "بررسی کامل نشد؛ می‌توانید دوباره تلاش کنید."
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("کانفینگ هوشمند (Aether)2") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "نسخه مستقل و رایگان؛ وضعیت اینترنت، پایداری، دسترسی UDP/TCP، DNS و IPv6 را بررسی می‌کند و گزینه‌های موتور Aether را خودکار تنظیم می‌کند.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (profile == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(summary, style = MaterialTheme.typography.bodyMedium)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                details.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                profile?.let { p ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(p.profileName, fontWeight = FontWeight.Bold)
                            Text("پروتکل: ${p.protocol.uppercase()} · انتقال: ${if (p.http2) "HTTP/2" else "HTTP/3"}")
                            Text("اسکن: ${p.scanMode} · ضد فیلتر: ${p.noize}")
                            Text("IP: ${p.ipVersion} · Fragment: ${if (p.fragment) "فعال" else "خاموش"}")
                            Text("Smart fallback: ${if (p.smartFailover) "فعال" else "خاموش"}")
                            Text("API fallback: ${p.apiAddress.ifBlank { "خودکار" }}")
                        }
                    }
                    Text(
                        "پروفایل با اطلاعات همین اتصال ساخته شده است. پس از تغییر شبکه، دوباره اسکن کنید تا تنظیمات به‌روز شوند.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = profile != null, onClick = { profile?.let(onCreate) }) {
                Text("ساخت و فعال‌سازی")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = { scanKey++ }) { Text("اسکن مجدد") }
                TextButton(onClick = onDismiss) { Text("بستن") }
            }
        },
    )
}
