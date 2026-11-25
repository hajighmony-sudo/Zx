@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.cluvex.zedsecure.ui.servers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * DNS AUTO · Smart discovery (Ultimate – Zero Config)
 *
 * غول مرحله آخر — کاملاً بدون کانفیگ
 *
 * هدف:
 * - کار در شدیدترین شرایط فیلترینگ ایران (اینترنت تقریباً مرده، پهنای باند بسیار پایین، DPI سنگین)
 * - کشف خودکار رزالورهای سالم
 * - ساخت و فعال‌سازی پروفایل DNS AUTO
 * - آماده‌سازی برای تونل DNS (MasterDNS / VayDNS) بدون نیاز به تنظیم دستی کاربر
 *
 * فقط روی این قابلیت کار شده. هیچ قابلیت دیگری از اپ تغییر نکرده است.
 */
@Composable
fun DnsAutoSheet(
    onDismiss: () -> Unit,
    onConnect: (String) -> Unit,
    onCreateTunnelProfile: ((List<String>) -> Unit)? = null,
    ipv6Enabled: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    var results by remember { mutableStateOf<List<DnsAutoResult>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var completed by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf("آماده") }
    var bestResolvers by remember { mutableStateOf<List<String>>(emptyList()) }
    var ultraLowMode by remember { mutableStateOf(false) }

    // لیست بزرگ رزالورهای عمومی (تمرکز روی IPv4 برای پایداری در فیلترینگ شدید)
    val publicResolvers = remember {
        listOf(
            // Major public
            "8.8.8.8", "8.8.4.4",
            "1.1.1.1", "1.0.0.1",
            "9.9.9.9", "149.112.112.112",
            "208.67.222.222", "208.67.220.220",
            "94.140.14.14", "94.140.15.15",
            "185.228.168.9", "185.228.169.9",
            "76.76.2.0", "76.76.10.0",
            "8.26.56.26", "8.20.247.20",
            "64.6.64.6", "64.6.65.6",
            "84.200.69.80", "84.200.70.40",
            "1.1.1.2", "1.0.0.2",
            "9.9.9.10", "149.112.112.10",
            "208.67.222.123", "208.67.220.123",
            "185.228.168.10", "185.228.169.11",
            // Additional resilient
            "77.88.8.8", "77.88.8.1",
            "4.2.2.1", "4.2.2.2", "4.2.2.3", "4.2.2.4", "4.2.2.5", "4.2.2.6",
            "156.154.70.1", "156.154.71.1",
            "198.101.242.72", "23.253.163.53",
            "176.103.130.130", "176.103.130.131",
            "45.90.28.0", "45.90.30.0",
            "91.239.100.100", "89.233.43.71",
            "80.80.80.80", "80.80.81.81",
            "101.101.101.101", "101.102.103.104",
            "195.46.39.39", "195.46.39.40",
            "37.235.1.174", "37.235.1.177",
            "149.112.121.10", "149.112.122.10",
            "94.140.14.15", "94.140.15.16",
            "1.1.1.3", "1.0.0.3",
            // More options for heavy filtering
            "9.9.9.11", "149.112.112.11",
            "208.67.222.220", "208.67.220.222",
            "185.228.168.168", "185.228.169.168",
            "76.76.2.1", "76.76.10.1",
            "94.140.14.140", "94.140.15.141",
        ).distinct()
    }

    suspend fun probeResolver(address: String, timeoutMs: Int = 1800): DnsAutoResult {
        val udp = try {
            dnsQuery(address, 53, "example.com", DnsRecordType.A, DnsTransport.UDP, timeoutMs)
        } catch (c: CancellationException) {
            throw c
        } catch (_: Exception) {
            DnsResult.Failed(dev.cluvex.zedsecure.platform.DnsFailure.Unreachable)
        }

        val tcp = try {
            dnsQuery(address, 53, "example.com", DnsRecordType.A, DnsTransport.TCP, timeoutMs)
        } catch (c: CancellationException) {
            throw c
        } catch (_: Exception) {
            DnsResult.Failed(dev.cluvex.zedsecure.platform.DnsFailure.Unreachable)
        }

        return DnsAutoResult(address, udp, tcp)
    }

    suspend fun runSmartDiscovery() {
        if (scanning) return
        scanning = true
        completed = false
        results = emptyList()
        bestResolvers = emptyList()
        progress = 0f
        phase = "شناسایی"
        statusMessage = "در حال شناسایی DNS شبکه و رزالورهای عمومی…"

        try {
            // 1. Network resolvers
            val networkOnes = withContext(Dispatchers.IO) {
                currentNetworkDnsServers().distinct()
                    .filter { ipv6Enabled || !it.contains(':') }
            }

            // 2. Combined candidate list (network first, then public)
            val candidates = (networkOnes + publicResolvers)
                .distinct()
                .filter { ipv6Enabled || !it.contains(':') }
                .take(if (ultraLowMode) 24 else 48)

            if (candidates.isEmpty()) {
                statusMessage = "هیچ رزالوری پیدا نشد. اتصال اینترنت را بررسی کن."
                completed = true
                scanning = false
                return
            }

            phase = "اسکن موازی"
            statusMessage = "در حال تست ${candidates.size} رزالور (UDP + TCP)…"

            val semaphore = Semaphore(if (ultraLowMode) 6 else 12)
            val collected = coroutineScope {
                candidates.mapIndexed { index, resolver ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            val result = probeResolver(resolver, if (ultraLowMode) 2500 else 1800)
                            withContext(Dispatchers.Main.immediate) {
                                progress = (index + 1).toFloat() / candidates.size
                            }
                            result
                        }
                    }
                }.awaitAll()
            }

            // Rank: prefer valid UDP answers, then lowest latency
            val ranked = collected
                .sortedWith(
                    compareByDescending<DnsAutoResult> {
                        val reply = (it.udp as? DnsResult.Ok)?.reply
                        reply != null && reply.rcode == 0 && reply.answerCount > 0
                    }.thenBy {
                        (it.udp as? DnsResult.Ok)?.reply?.elapsedMs ?: 99999L
                    }.thenBy {
                        (it.tcp as? DnsResult.Ok)?.reply?.elapsedMs ?: 99999L
                    }
                )

            results = ranked
            progress = 1f
            phase = "پایان"

            val good = ranked.filter {
                val r = (it.udp as? DnsResult.Ok)?.reply
                r != null && r.rcode == 0 && r.answerCount > 0
            }
            bestResolvers = good.take(8).map { it.address }

            statusMessage = if (good.isNotEmpty()) {
                "${good.size} رزالور سالم پیدا شد. سریع‌ترین‌ها در بالا هستند."
            } else {
                "هیچ رزالور با پاسخ معتبر UDP پیدا نشد. شبکه بسیار محدود است."
            }
            completed = true
        } catch (c: CancellationException) {
            throw c
        } catch (e: Exception) {
            statusMessage = "خطا در اسکن: ${e.message ?: "ناشناخته"}"
            completed = true
        } finally {
            scanning = false
        }
    }

    LaunchedEffect(Unit) {
        runSmartDiscovery()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberAppSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .appVerticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "DNS AUTO · Smart discovery",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "غول مرحله آخر — کاملاً بدون کانفیگ\nبرای شرایطی که تقریباً همه چیز فیلتر شده و پهنای باند بسیار پایین است.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Ultra Low Bandwidth toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "حالت پهنای باند بسیار پایین",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "برای اینترنت داغون و فیلترینگ شدید",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    onClick = {
                        ultraLowMode = !ultraLowMode
                        if (!scanning) {
                            scope.launch { runSmartDiscovery() }
                        }
                    },
                    enabled = !scanning,
                ) {
                    Text(if (ultraLowMode) "فعال" else "غیرفعال")
                }
            }

            if (scanning) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                    Column {
                        Text(phase, fontWeight = FontWeight.Medium)
                        statusMessage?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                statusMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Results
            if (results.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "نتایج رتبه‌بندی‌شده (بهترین در بالا)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                results.take(12).forEach { item ->
                    val udpOk = (item.udp as? DnsResult.Ok)?.reply
                        ?.let { it.rcode == 0 && it.answerCount > 0 } == true
                    val color = if (udpOk) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            item.address,
                            color = color,
                            fontWeight = if (udpOk) FontWeight.Medium else FontWeight.Normal,
                        )
                        Text(
                            "UDP ${item.udp.latencyLabel()} · TCP ${item.tcp.latencyLabel()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "UDP: ${item.udp.label()}  |  TCP: ${item.tcp.label()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Action buttons
            val best = results.firstOrNull {
                val reply = (it.udp as? DnsResult.Ok)?.reply
                reply != null && reply.rcode == 0 && reply.answerCount > 0
            }

            if (best != null) {
                Button(
                    onClick = { onConnect(best.address) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !scanning,
                ) {
                    Text("فعال‌سازی DNS AUTO با ${best.address}")
                }

                if (bestResolvers.size >= 2 && onCreateTunnelProfile != null) {
                    OutlinedButton(
                        onClick = { onCreateTunnelProfile(bestResolvers) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !scanning,
                    ) {
                        Text("ساخت پروفایل تونل DNS با ${bestResolvers.size} رزالور برتر")
                    }
                }

                Text(
                    "پروفایل «DNS AUTO» با سریع‌ترین رزالور سالم ساخته و فعال می‌شود.\n" +
                        "این پروفایل درخواست‌های DNS را از مسیر انتخاب‌شده عبور می‌دهد.\n" +
                        "برای آوردن تلگرام و یوتیوب: بعد از فعال‌سازی، از per-app routing استفاده کن یا با دکمه پایین پروفایل تونل DNS بساز.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (completed && results.isNotEmpty()) {
                Text(
                    "هیچ رزالور با پاسخ معتبر پیدا نشد. شبکه بسیار محدود است.\n" +
                        "در این شرایط از تونل‌های DNS (MasterDNS / VayDNS) با دامنه شخصی یا قابلیت‌های GHOST MOD استفاده کن.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = {
                        scope.launch { runSmartDiscovery() }
                    },
                    enabled = !scanning,
                ) {
                    Text("اسکن دوباره")
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text("بستن")
                }
            }

            Text(
                "این نسخه Zero-Config است. رزالورها را خودکار پیدا می‌کند، رتبه‌بندی می‌کند و پروفایل می‌سازد.\n" +
                    "حالت پهنای باند بسیار پایین برای شرایط اینترنت داغون بهینه شده است.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class DnsAutoResult(
    val address: String,
    val udp: DnsResult,
    val tcp: DnsResult,
)

private fun DnsResult.label(): String = when (this) {
    is DnsResult.Ok -> if (reply.rcode == 0) "پاسخ معتبر" else "کد ${reply.rcode}"
    is DnsResult.Failed -> when (reason) {
        dev.cluvex.zedsecure.platform.DnsFailure.Timeout -> "تایم‌اوت"
        dev.cluvex.zedsecure.platform.DnsFailure.Refused -> "رد شد"
        dev.cluvex.zedsecure.platform.DnsFailure.Malformed -> "نامعتبر"
        dev.cluvex.zedsecure.platform.DnsFailure.Mismatch -> "نامنطبق"
        dev.cluvex.zedsecure.platform.DnsFailure.Unreachable -> "غیرقابل دسترس"
    }
}

private fun DnsResult.latencyLabel(): String = when (this) {
    is DnsResult.Ok -> "${reply.elapsedMs} ms"
    is DnsResult.Failed -> if (elapsedMs >= 0) "${elapsedMs} ms" else "—"
}
