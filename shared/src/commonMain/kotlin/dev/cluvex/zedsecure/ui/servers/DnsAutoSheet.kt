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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import dev.cluvex.zedsecure.platform.DnsRecordType
import dev.cluvex.zedsecure.platform.DnsResult
import dev.cluvex.zedsecure.platform.DnsTransport
import dev.cluvex.zedsecure.platform.currentNetworkDnsServers
import dev.cluvex.zedsecure.platform.dnsQuery
import dev.cluvex.zedsecure.ui.platform.appVerticalScroll
import dev.cluvex.zedsecure.ui.platform.rememberAppSheetState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun DnsAutoSheet(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val networkResolvers = remember { currentNetworkDnsServers().distinct() }
    var results by remember { mutableStateOf<List<DnsAutoResult>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var completed by remember { mutableStateOf(false) }

    suspend fun diagnose() {
        if (scanning) return
        if (networkResolvers.isEmpty()) {
            results = emptyList()
            completed = true
            message = "سیستم‌عامل DNS فعالی برای شبکهٔ فعلی اعلام نکرده است."
            return
        }
        scanning = true
        completed = false
        results = emptyList()
        message = "در حال بررسی پاسخ‌گویی DNS شبکهٔ فعلی…"
        try {
            results = coroutineScope {
                networkResolvers.take(6).map { resolver ->
                    async(Dispatchers.IO) {
                        val udp = runCatching {
                            dnsQuery(
                                server = resolver,
                                port = 53,
                                name = "example.com",
                                type = DnsRecordType.A,
                                transport = DnsTransport.UDP,
                                timeoutMs = 2200,
                            )
                        }.getOrElse { DnsResult.Failed(dev.cluvex.zedsecure.platform.DnsFailure.Unreachable) }

                        val tcp = runCatching {
                            dnsQuery(
                                server = resolver,
                                port = 53,
                                name = "example.com",
                                type = DnsRecordType.A,
                                transport = DnsTransport.TCP,
                                timeoutMs = 2200,
                            )
                        }.getOrElse { DnsResult.Failed(dev.cluvex.zedsecure.platform.DnsFailure.Unreachable) }

                        DnsAutoResult(address = resolver, udp = udp, tcp = tcp)
                    }
                }.awaitAll()
            }
            val working = results.count { it.udp.responded || it.tcp.responded }
            message = "بررسی تمام شد؛ $working از ${results.size} سرور DNS حداقل از یک روش پاسخ دادند."
            completed = true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            message = error.message?.take(160) ?: "بررسی DNS کامل نشد."
            completed = true
        } finally {
            scanning = false
        }
    }

    LaunchedEffect(networkResolvers) { diagnose() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberAppSheetState(),
    ) {
        Column(
            Modifier.fillMaxWidth().appVerticalScroll(rememberScrollState()).imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("DNS AUTO", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("تشخیص خودکار DNS شبکه", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "این بخش DNSهای اعلام‌شده از سوی شبکهٔ فعلی را شناسایی می‌کند و پاسخ‌گویی UDP و TCP آن‌ها را می‌سنجد. هیچ پروفایل VPN یا تونلی ایجاد یا فعال نمی‌شود.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("DNSهای شناسایی‌شده: ${networkResolvers.size}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (networkResolvers.isEmpty()) {
                Text("فهرست DNS شبکه خالی است؛ وضعیت اتصال شبکه را بررسی و دوباره تلاش کن.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            } else {
                networkResolvers.take(6).forEach { resolver -> Text(resolver, style = MaterialTheme.typography.bodyMedium) }
            }
            Button(
                onClick = { scope.launch { diagnose() } },
                enabled = !scanning && networkResolvers.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (scanning) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 10.dp), strokeWidth = 2.dp)
                    Text("در حال بررسی…")
                } else Text("بررسی دوباره")
            }
            message?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (completed && results.isEmpty() && networkResolvers.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            if (results.isNotEmpty()) {
                Text("نتیجهٔ بررسی", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                results.forEach { item ->
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.address, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("UDP · ${item.udp.label()}", style = MaterialTheme.typography.labelMedium)
                            Text(item.udp.latencyLabel(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("TCP · ${item.tcp.label()}", style = MaterialTheme.typography.labelMedium)
                            Text(item.tcp.latencyLabel(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Text(
                "DoH و DoT در این صفحه فعلاً آزمایش نمی‌شوند؛ برای آزمایش معتبر آن‌ها باید نشانی سرویس و نام میزبان TLS به‌صورت جداگانه تعریف شود.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "تغییر DNS فقط روی درخواست‌های DNS اثر دارد و به‌تنهایی تمام ترافیک دستگاه را وارد VPN یا تونل نمی‌کند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("بستن") }
        }
    }
}

private data class DnsAutoResult(val address: String, val udp: DnsResult, val tcp: DnsResult)

private fun DnsResult.label(): String = when (this) {
    is DnsResult.Ok -> if (reply.rcode == 0) "پاسخ معتبر" else "پاسخ DNS با کد ${reply.rcode}"
    is DnsResult.Failed -> when (reason) {
        dev.cluvex.zedsecure.platform.DnsFailure.Timeout -> "مهلت تمام شد"
        dev.cluvex.zedsecure.platform.DnsFailure.Refused -> "درخواست رد شد"
        dev.cluvex.zedsecure.platform.DnsFailure.Malformed -> "پاسخ نامعتبر"
        dev.cluvex.zedsecure.platform.DnsFailure.Mismatch -> "پاسخ نامنطبق"
        dev.cluvex.zedsecure.platform.DnsFailure.Unreachable -> "دردسترس نیست"
    }
}

private fun DnsResult.latencyLabel(): String = when (this) {
    is DnsResult.Ok -> "${reply.elapsedMs} ms"
    is DnsResult.Failed -> elapsedMs.takeIf { it >= 0 }?.let { "${it} ms" } ?: "—"
}
