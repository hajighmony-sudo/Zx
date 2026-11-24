package dev.cluvex.zedsecure.data.net

import dev.cluvex.zedsecure.domain.config.LocalProxy
import dev.cluvex.zedsecure.platform.httpGetViaSocks
import dev.cluvex.zedsecure.platform.currentTimeMillis
import dev.cluvex.zedsecure.platform.resolveHostAddress
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Everything the IP database knows about a server's address. */
data class GeoDetails(
    val ip: String,
    val countryCode: String?,
    val country: String?,
    val region: String?,
    val city: String?,
    val isp: String?,
    val asn: String?,
    val latitude: Double?,
    val longitude: Double?,
)

object GeoLookup {
    private val detailCache = HashMap<String, GeoDetails>()

    /** Full IP intelligence for a host (country, city, ISP/ASN, coordinates); cached per host. */
    suspend fun detailsOf(host: String): GeoDetails? {
        if (host.isBlank() || host == "-") return null
        cacheLock.withLock { detailCache[host]?.let { return it } }
        val ip = withTimeoutOrNull(RESOLVE_TIMEOUT_MS) { resolveHostAddress(host) } ?: return null
        val url = "https://api.ip.sb/geoip/$ip"
        val body = runCatching { httpGetViaSocks(url, socksPort = null, connectTimeoutMs = 6_000, readTimeoutMs = 6_000) }.getOrNull()
            ?: runCatching {
                httpGetViaSocks(url, socksPort = dev.cluvex.zedsecure.core.VpnManager.activeSocksPort ?: LocalProxy.SOCKS_PORT, connectTimeoutMs = 6_000, readTimeoutMs = 6_000)
            }.getOrNull()
            ?: return GeoDetails(ip, null, null, null, null, null, null, null, null)
        val details = runCatching {
            val obj = json.parseToJsonElement(body).jsonObject
            fun text(vararg keys: String): String? =
                keys.firstNotNullOfOrNull { key -> obj[key]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() && it != "null" } }
            GeoDetails(
                ip = ip,
                countryCode = text("country_code", "countryCode")?.takeIf { it.length == 2 }?.uppercase(),
                country = text("country"),
                region = text("region", "region_name"),
                city = text("city"),
                isp = text("organization", "isp", "org"),
                asn = text("asn", "as"),
                latitude = text("latitude", "lat")?.toDoubleOrNull(),
                longitude = text("longitude", "lon")?.toDoubleOrNull(),
            )
        }.getOrNull() ?: return GeoDetails(ip, null, null, null, null, null, null, null, null)
        if (details.countryCode != null) cacheLock.withLock { detailCache[host] = details; cache[host] = details.countryCode }
        return details
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val cacheLock = Mutex()
    private val cache = HashMap<String, String>()

    private const val FAILURE_TTL_MS = 10 * 60_000L
    private val failures = HashMap<String, Long>()

    private const val RESOLVE_TIMEOUT_MS = 3_000L

    suspend fun countryOf(host: String): String? {
        if (host.isBlank() || host == "-") return null
        val now = currentTimeMillis()
        cacheLock.withLock {
            cache[host]?.let { return it }
            failures[host]?.let { if (now - it < FAILURE_TTL_MS) return null }
        }

        val ip = withTimeoutOrNull(RESOLVE_TIMEOUT_MS) { resolveHostAddress(host) }
            ?: return null.also { rememberFailure(host) }

        val body = runCatching { httpGetViaSocks("https://api.ip.sb/geoip/$ip", socksPort = null, connectTimeoutMs = 6_000, readTimeoutMs = 6_000) }.getOrNull()
            ?: runCatching { httpGetViaSocks("https://api.ip.sb/geoip/$ip", socksPort = dev.cluvex.zedsecure.core.VpnManager.activeSocksPort ?: LocalProxy.SOCKS_PORT, connectTimeoutMs = 6_000, readTimeoutMs = 6_000) }.getOrNull()
            ?: return null.also { rememberFailure(host) }

        val code = runCatching {
            val obj = json.parseToJsonElement(body).jsonObject
            obj["country_code"]?.jsonPrimitive?.content
                ?: obj["countryCode"]?.jsonPrimitive?.content
        }.getOrNull()?.takeIf { it.length == 2 }?.uppercase()

        if (code != null) cacheLock.withLock { cache[host] = code } else rememberFailure(host)
        return code
    }

    private suspend fun rememberFailure(host: String) {
        cacheLock.withLock { failures[host] = currentTimeMillis() }
    }
}
