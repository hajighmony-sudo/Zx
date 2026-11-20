@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package dev.cluvex.zedsecure.data.config

import kotlin.io.encoding.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Returns [link] with its display name replaced by [name], so a renamed config can be exported and
 * imported elsewhere under the new name. vmess:// keeps the name inside its base64 JSON ("ps"); every
 * other scheme carries it in the URL fragment. Unknown or broken links are returned unchanged.
 */
object LinkRename {
    private val lenient = Json { ignoreUnknownKeys = true; isLenient = true }
    private val urlSafe = Base64.UrlSafe.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)
    private val standard = Base64.Default.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)

    fun apply(link: String, name: String): String {
        val clean = name.trim()
        if (clean.isEmpty()) return link
        val trimmed = link.trim()
        return when {
            trimmed.startsWith("vmess://", ignoreCase = true) -> renameVmess(trimmed, clean) ?: trimmed
            trimmed.contains("://") -> trimmed.substringBefore('#') + "#" + encode(clean)
            else -> trimmed
        }
    }

    private fun renameVmess(link: String, name: String): String? = runCatching {
        val payload = link.substring("vmess://".length).substringBefore('#').trim()
        val decoded = runCatching { standard.decode(payload) }.getOrElse { urlSafe.decode(payload) }.decodeToString()
        val obj = lenient.parseToJsonElement(decoded).jsonObject
        val renamed = JsonObject(obj + ("ps" to JsonPrimitive(name)))
        "vmess://" + standard.encode(renamed.toString().encodeToByteArray())
    }.getOrNull()

    /** Percent-encodes everything except RFC 3986 unreserved characters, UTF-8 based. */
    fun encode(text: String): String = buildString {
        for (byte in text.encodeToByteArray()) {
            val c = byte.toInt() and 0xFF
            val ch = c.toChar()
            if (ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' || ch == '-' || ch == '_' || ch == '.' || ch == '~') {
                append(ch)
            } else {
                append('%')
                append("0123456789ABCDEF"[c shr 4])
                append("0123456789ABCDEF"[c and 15])
            }
        }
    }
}
