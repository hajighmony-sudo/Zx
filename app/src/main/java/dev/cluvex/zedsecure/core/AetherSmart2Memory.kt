package dev.cluvex.zedsecure.core

import android.content.Context
import dev.cluvex.zedsecure.domain.config.AetherProfile
import dev.cluvex.zedsecure.domain.config.AetherSmart2Profile
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale

/** Bounded Smart 2 memory: hashed network keys and tactic signatures only. */
internal object AetherSmart2Memory {
    private const val PREFS = "aether_smart2_memory"
    private const val KEY = "networks"
    private const val VERSION = 1
    const val PROVEN_TTL_MS = 14L * 24 * 60 * 60 * 1000
    const val FAILED_TTL_MS = 6L * 60 * 60 * 1000
    const val MAX_NETWORKS = 32

    data class Entry(val proven: String? = null, val provenAt: Long = 0L, val failedAt: Map<String, Long> = emptyMap()) {
        fun isProven(now: Long) = proven != null && now - provenAt in 0..PROVEN_TTL_MS
        fun failed(now: Long) = failedAt.filterValues { now - it in 0..FAILED_TTL_MS }.keys
    }

    fun networkKey(profile: AetherSmart2Profile): String {
        val material = listOf(profile.networkName.lowercase(Locale.ROOT), profile.networkAsn.orEmpty().uppercase(Locale.ROOT), profile.dnsPreference).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256").digest(material.toByteArray(Charsets.UTF_8))
        return digest.take(12).joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
    }

    fun order(context: Context, networkKey: String, profiles: List<AetherProfile>, now: Long = System.currentTimeMillis()): List<AetherProfile> {
        val entry = recall(context, networkKey) ?: return profiles
        val failed = entry.failed(now)
        val fresh = profiles.filterNot { AetherSmart2Brain.signature(it) in failed }
        val remembered = if (entry.isProven(now)) fresh.firstOrNull { AetherSmart2Brain.signature(it) == entry.proven } else null
        val tail = profiles.filter { AetherSmart2Brain.signature(it) in failed }
        return listOfNotNull(remembered) + fresh.filterNot { it == remembered } + tail
    }

    fun remember(context: Context, networkKey: String, profile: AetherProfile, now: Long = System.currentTimeMillis()) {
        edit(context) { map ->
            val old = map[networkKey]
            val signature = AetherSmart2Brain.signature(profile)
            map[networkKey] = Entry(signature, now, old?.failedAt.orEmpty() - signature)
        }
    }

    fun noteFailure(context: Context, networkKey: String, profile: AetherProfile, now: Long = System.currentTimeMillis()) {
        edit(context) { map ->
            val old = map[networkKey]
            val signature = AetherSmart2Brain.signature(profile)
            val proven = old?.proven?.takeIf { it != signature }
            map[networkKey] = Entry(proven, if (proven != null) old?.provenAt ?: 0L else 0L, old?.failedAt.orEmpty() + (signature to now))
        }
    }

    fun prune(map: Map<String, Entry>, now: Long, max: Int = MAX_NETWORKS): Map<String, Entry> {
        val clean = map.mapValues { (_, entry) -> entry.copy(failedAt = entry.failedAt.filterValues { now - it in 0..FAILED_TTL_MS }) }
            .filterValues { it.proven != null || it.failedAt.isNotEmpty() }
        if (clean.size <= max) return clean
        return clean.entries.sortedByDescending { maxOf(it.value.provenAt, it.value.failedAt.values.maxOrNull() ?: 0L) }
            .take(max).associate { it.key to it.value }
    }

    private fun recall(context: Context, networkKey: String): Entry? = read(context)[networkKey]

    private fun read(context: Context): Map<String, Entry> = runCatching {
        val raw = prefs(context).getString(KEY, null) ?: return emptyMap()
        val root = JSONObject(raw)
        if (root.optInt("v") != VERSION) return emptyMap()
        val networks = root.optJSONObject("n") ?: return emptyMap()
        buildMap {
            for (network in networks.keys()) {
                val item = networks.optJSONObject(network) ?: continue
                val failed = buildMap {
                    val f = item.optJSONObject("f")
                    if (f != null) for (id in f.keys()) put(id, f.optLong(id))
                }
                put(network, Entry(item.optString("t").takeIf { it.isNotBlank() }, item.optLong("p"), failed))
            }
        }
    }.getOrDefault(emptyMap())

    @Synchronized
    private fun edit(context: Context, change: (MutableMap<String, Entry>) -> Unit) {
        runCatching {
            val values = read(context).toMutableMap()
            change(values)
            val json = JSONObject()
            for ((network, entry) in prune(values, System.currentTimeMillis())) {
                val item = JSONObject().put("p", entry.provenAt)
                entry.proven?.let { item.put("t", it) }
                if (entry.failedAt.isNotEmpty()) {
                    val failed = JSONObject()
                    entry.failedAt.forEach { (id, at) -> failed.put(id, at) }
                    item.put("f", failed)
                }
                json.put(network, item)
            }
            prefs(context).edit().putString(KEY, JSONObject().put("v", VERSION).put("n", json).toString()).apply()
        }.onFailure {
            LogBus.append("W/Aether Smart 2: memory write failed (" + it.javaClass.simpleName + ")")
        }
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
