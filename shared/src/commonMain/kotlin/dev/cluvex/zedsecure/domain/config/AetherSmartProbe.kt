package dev.cluvex.zedsecure.domain.config

import dev.cluvex.zedsecure.platform.currentTimeMillis
import dev.cluvex.zedsecure.platform.httpGetResponse
import dev.cluvex.zedsecure.platform.probeAetherTransportFingerprint
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.booleanOrNull

/**
 * Evidence-based, direct-path diagnostics for Aether Smart.
 * IP intelligence is context only; blocking verdicts come from staged network probes.
 */
object AetherSmartProbe {
    data class Result(
        val edgeMs: Int?,
        val apiMs: Int?,
        val edgeJitterMs: Int? = null,
        val apiJitterMs: Int? = null,
        val edgeLossPercent: Int = 100,
        val apiLossPercent: Int = 100,
        val udpReachable: Boolean? = null,
        val tcp443Reachable: Boolean? = null,
        val tlsSniReachable: Boolean? = null,
        val dnsSystemMatchesDoh: Boolean? = null,
        val publicIp: String? = null,
        val isp: String? = null,
        val asn: String? = null,
        val countryCode: String? = null,
        val ooniAnomalyPercent: Int? = null,
        val ooniSamples: Int = 0,
    )

    suspend fun run(): Result = coroutineScope {
        val edge = async { sample(EDGE_URL, EDGE_FALLBACK_URL) }
        val api = async { sample(API_URL, API_FALLBACK_URL) }
        val transport = async { probeAetherTransportFingerprint() }
        val identity = async { fetchIpIdentity() }
        val edgeResult = edge.await()
        val apiResult = api.await()
        val transportResult = transport.await()
        val ip = identity.await()
        val dohAnswers = fetchDohAnswers()
        val systemAnswers = transportResult.systemDnsAnswers
        val dnsMatch = when {
            systemAnswers.isNullOrEmpty() || dohAnswers.isNullOrEmpty() -> null
            else -> systemAnswers.toSet().intersect(dohAnswers.toSet()).isNotEmpty()
        }
        val ooni = if (!ip.countryCode.isNullOrBlank() && !ip.asn.isNullOrBlank()) {
            fetchOoniSignal(ip.countryCode, ip.asn)
        } else null
        Result(
            edgeMs = edgeResult.medianMs,
            apiMs = apiResult.medianMs,
            edgeJitterMs = edgeResult.jitterMs,
            apiJitterMs = apiResult.jitterMs,
            edgeLossPercent = edgeResult.lossPercent,
            apiLossPercent = apiResult.lossPercent,
            udpReachable = transportResult.udpReachable,
            tcp443Reachable = transportResult.tcp443Reachable,
            tlsSniReachable = transportResult.tlsSniReachable,
            dnsSystemMatchesDoh = dnsMatch,
            publicIp = ip.ip,
            isp = ip.isp,
            asn = ip.asn,
            countryCode = ip.countryCode,
            ooniAnomalyPercent = ooni?.first,
            ooniSamples = ooni?.second ?: 0,
        )
    }

    private data class SampleSet(val medianMs: Int?, val jitterMs: Int?, val lossPercent: Int)
    private data class IpIdentity(
        val ip: String? = null,
        val isp: String? = null,
        val asn: String? = null,
        val countryCode: String? = null,
    )

    private suspend fun sample(primary: String, fallback: String): SampleSet = coroutineScope {
        val samples = (0 until SAMPLE_COUNT).map {
            async { time(primary) ?: time(fallback) }
        }.map { it.await() }
        val successful = samples.filterNotNull().sorted()
        SampleSet(
            medianMs = successful.takeIf { it.isNotEmpty() }?.let { it[it.size / 2] },
            jitterMs = successful.takeIf { it.size >= 3 }?.let { values ->
                val trim = if (values.size >= 5) 1 else 0
                values[values.lastIndex - trim] - values[trim]
            },
            lossPercent = ((SAMPLE_COUNT - successful.size) * 100) / SAMPLE_COUNT,
        )
    }

    private suspend fun time(url: String): Int? = withContext(Dispatchers.IO) {
        val start = currentTimeMillis()
        val answered = try {
            val response = httpGetResponse(url, USER_AGENT, null, TIMEOUT_MS, TIMEOUT_MS)
            response.code in 200..499
        } catch (_: Throwable) { false }
        if (answered) (currentTimeMillis() - start).toInt().coerceAtLeast(1) else null
    }

    private suspend fun fetchIpIdentity(): IpIdentity = withContext(Dispatchers.IO) {
        for (endpoint in IP_IDENTITY_ENDPOINTS) {
            val identity = runCatching {
                val response = httpGetResponse(endpoint, USER_AGENT, null, TIMEOUT_MS, TIMEOUT_MS)
                require(response.code in 200..299)
                val root = json.parseToJsonElement(response.body).jsonObject
                val asText = sequenceOf("as", "org")
                    .mapNotNull { key -> root[key]?.jsonPrimitive?.contentOrNull }
                    .firstOrNull { it.isNotBlank() }.orEmpty()
                val asn = Regex("(?i)AS\\d+").find(asText)?.value?.uppercase()
                val provider = sequenceOf("isp", "organization", "org")
                    .mapNotNull { key -> root[key]?.jsonPrimitive?.contentOrNull }
                    .firstOrNull { it.isNotBlank() }
                    ?: asText.substringAfter(asn ?: "\u0000").trim().takeIf { it.isNotBlank() }
                val country = sequenceOf("countryCode", "country_code", "country")
                    .mapNotNull { root[it]?.jsonPrimitive?.contentOrNull }
                    .firstOrNull { it.length == 2 }
                IpIdentity(
                    ip = sequenceOf("ip", "query").mapNotNull { root[it]?.jsonPrimitive?.contentOrNull }.firstOrNull(),
                    isp = provider,
                    asn = asn,
                    countryCode = country?.uppercase(),
                )
            }.getOrNull()
            if (identity != null && (identity.ip != null || identity.asn != null || identity.countryCode != null)) {
                return@withContext identity
            }
        }
        IpIdentity()
    }

    private suspend fun fetchDohAnswers(): List<String>? = withContext(Dispatchers.IO) {
        runCatching {
            val response = httpGetResponse(DOH_URL, USER_AGENT, null, TIMEOUT_MS, TIMEOUT_MS)
            require(response.code in 200..299)
            val answers = json.parseToJsonElement(response.body).jsonObject["Answer"]?.jsonArray ?: return@runCatching emptyList()
            answers.mapNotNull { item ->
                item.jsonObject["data"]?.jsonPrimitive?.contentOrNull
                    ?.takeIf { it.matches(Regex("^[0-9.]+$")) }
            }.distinct().sorted()
        }.getOrNull()
    }

    /**
     * OONI is network-level corroboration, not a live test of this handset.
     * A small recent sample is deliberately treated as context, never as proof.
     */
    private suspend fun fetchOoniSignal(country: String, asn: String): Pair<Int, Int>? =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "$OONI_URL?probe_cc=$country&probe_asn=$asn&test_name=web_connectivity&limit=20"
                val response = httpGetResponse(url, USER_AGENT, null, OONI_TIMEOUT_MS, OONI_TIMEOUT_MS)
                require(response.code in 200..299)
                val results = json.parseToJsonElement(response.body).jsonObject["results"]?.jsonArray
                    ?: return@runCatching null
                val flags = results.mapNotNull { item ->
                    item.jsonObject["anomaly"]?.jsonPrimitive?.booleanOrNull
                }
                if (flags.isEmpty()) null
                else (flags.count { it } * 100 / flags.size) to flags.size
            }.getOrNull()
        }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private const val SAMPLE_COUNT = 5
    private const val TIMEOUT_MS = 2_500
    private const val OONI_TIMEOUT_MS = 4_000
    private const val USER_AGENT = "ZX-Aether-Smart"
    private const val EDGE_URL = "https://cp.cloudflare.com/generate_204"
    private const val EDGE_FALLBACK_URL = "https://www.cloudflare.com/cdn-cgi/trace"
    private const val API_URL = "https://api.devices.cloudflare.com/"
    private const val API_FALLBACK_URL = "https://api.cloudflareclient.com/"
    private const val IPINFO_URL = "https://ipinfo.io/json"
    private const val IPMYP_URL = "https://api.ipmyp.ir/json"
    private val IP_IDENTITY_ENDPOINTS = listOf(IPINFO_URL, IPMYP_URL)
    private const val DOH_URL = "https://cloudflare-dns.com/dns-query?name=example.com&type=A"
    private const val OONI_URL = "https://api.ooni.io/api/v1/measurements"
}
