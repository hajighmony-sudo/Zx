package dev.cluvex.zedsecure.core.ghost

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Ghost Mode exclusive feature: probe internet reachability, latency and
 * rough bandwidth toward representative endpoints in selected countries.
 *
 * Inspired by Cloudflare speed-test style measurements (latency + small
 * download sample). Dependency-free; Ghost Mode only.
 */
object GhostCountryNetProbe {

    data class CountryEndpoint(
        val countryCode: String,
        val countryFa: String,
        val countryEn: String,
        val host: String,
        val path: String = "/",
        val note: String = ""
    )

    data class CountrySample(
        val countryCode: String,
        val countryFa: String,
        val countryEn: String,
        val host: String,
        val reachable: Boolean,
        val latencyMs: Long,
        val downloadKbps: Double,
        val httpCode: Int,
        val error: String? = null
    )

    data class CountryReport(
        val samples: List<CountrySample>,
        val bestCountryCode: String?,
        val bestLatencyMs: Long?,
        val averageLatencyMs: Long,
        val reachableCount: Int,
        val timestampMs: Long,
        val iranTime: String,
        val summaryFa: String
    )

    val catalog: List<CountryEndpoint> = listOf(
        CountryEndpoint("DE", "آلمان", "Germany", "www.hetzner.com", "/", "EU hub"),
        CountryEndpoint("NL", "هلند", "Netherlands", "www.digitalocean.com", "/", "EU hub"),
        CountryEndpoint("GB", "بریتانیا", "United Kingdom", "www.bbc.co.uk", "/", "EU/UK"),
        CountryEndpoint("FR", "فرانسه", "France", "www.ovh.com", "/", "EU"),
        CountryEndpoint("US", "آمریکا", "United States", "www.cloudflare.com", "/", "Global edge"),
        CountryEndpoint("US", "آمریکا (غربی)", "United States West", "www.apple.com", "/", "West coast"),
        CountryEndpoint("CA", "کانادا", "Canada", "www.shopify.com", "/", "NA"),
        CountryEndpoint("TR", "ترکیه", "Turkey", "www.trendyol.com", "/", "Near region"),
        CountryEndpoint("AE", "امارات", "UAE", "www.etisalat.ae", "/", "Near region"),
        CountryEndpoint("SG", "سنگاپور", "Singapore", "www.singaporeair.com", "/", "Asia hub"),
        CountryEndpoint("JP", "ژاپن", "Japan", "www.sony.com", "/", "APAC"),
        CountryEndpoint("KR", "کره جنوبی", "South Korea", "www.samsung.com", "/", "APAC"),
        CountryEndpoint("IN", "هند", "India", "www.flipkart.com", "/", "South Asia"),
        CountryEndpoint("RU", "روسیه", "Russia", "www.yandex.ru", "/", "CIS"),
        CountryEndpoint("CN", "چین", "China", "www.baidu.com", "/", "CN"),
        CountryEndpoint("IR", "ایران", "Iran", "www.digikala.com", "/", "Local baseline"),
    )

    fun countries(): List<Pair<String, String>> =
        catalog.map { it.countryCode to it.countryFa }.distinctBy { it.first }

    suspend fun probe(
        countryCodes: List<String> = emptyList(),
        timeoutMs: Int = 4000,
        sampleBytes: Int = 64 * 1024
    ): CountryReport = withContext(Dispatchers.IO) {
        val targets = if (countryCodes.isEmpty()) catalog
        else catalog.filter { it.countryCode in countryCodes.map { c -> c.uppercase() } }

        val samples = coroutineScope {
            targets.map { ep ->
                async { sampleOne(ep, timeoutMs, sampleBytes) }
            }.awaitAll()
        }

        val reachable = samples.filter { it.reachable }
        val best = reachable.minByOrNull { it.latencyMs }
        val avg = if (reachable.isEmpty()) 0L
        else reachable.map { it.latencyMs }.average().toLong()

        val now = System.currentTimeMillis()
        val iran = formatIranTime(now)
        val summary = buildString {
            append("کشورهای قابل‌دسترس: ")
            append(reachable.size)
            append("/")
            append(samples.size)
            if (best != null) {
                append(" • بهترین: ")
                append(best.countryFa)
                append(" (")
                append(best.latencyMs)
                append(" ms")
                if (best.downloadKbps > 0) {
                    append(", ~")
                    append(best.downloadKbps.toLong())
                    append(" kbps")
                }
                append(")")
            }
            append(" • میانگین تأخیر: ")
            append(avg)
            append(" ms")
        }

        CountryReport(
            samples = samples.sortedBy { if (it.reachable) it.latencyMs else Long.MAX_VALUE },
            bestCountryCode = best?.countryCode,
            bestLatencyMs = best?.latencyMs,
            averageLatencyMs = avg,
            reachableCount = reachable.size,
            timestampMs = now,
            iranTime = iran,
            summaryFa = summary
        )
    }

    private fun sampleOne(ep: CountryEndpoint, timeoutMs: Int, sampleBytes: Int): CountrySample {
        val url = "https://${ep.host}${ep.path}"
        return try {
            val t0 = System.nanoTime()
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "ZX-GhostCountryProbe/1.0")
                setRequestProperty("Accept", "*/*")
            }
            val code = conn.responseCode
            val latency = ((System.nanoTime() - t0) / 1_000_000L).coerceAtLeast(1L)

            var kbps = 0.0
            if (code in 200..399) {
                try {
                    val stream = conn.inputStream
                    val buf = ByteArray(minOf(sampleBytes, 32 * 1024))
                    val t1 = System.nanoTime()
                    var total = 0
                    while (total < sampleBytes) {
                        val n = stream.read(buf)
                        if (n <= 0) break
                        total += n
                    }
                    val elapsedSec = ((System.nanoTime() - t1) / 1_000_000_000.0).coerceAtLeast(0.001)
                    if (total > 0) kbps = (total * 8.0 / 1000.0) / elapsedSec
                    stream.close()
                } catch (_: Throwable) {
                }
            }
            conn.disconnect()

            CountrySample(
                countryCode = ep.countryCode,
                countryFa = ep.countryFa,
                countryEn = ep.countryEn,
                host = ep.host,
                reachable = code in 200..399,
                latencyMs = latency,
                downloadKbps = kbps,
                httpCode = code
            )
        } catch (t: Throwable) {
            CountrySample(
                countryCode = ep.countryCode,
                countryFa = ep.countryFa,
                countryEn = ep.countryEn,
                host = ep.host,
                reachable = false,
                latencyMs = timeoutMs.toLong(),
                downloadKbps = 0.0,
                httpCode = -1,
                error = t.message ?: t::class.simpleName
            )
        }
    }

    private fun formatIranTime(epochMs: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("Asia/Tehran") }
            .format(Date(epochMs)) + " (به وقت ایران)"
    }
}
