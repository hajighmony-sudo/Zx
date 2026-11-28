package dev.cluvex.zedsecure.core

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Keeps the WARP identity set used by MASQUE, WireGuard, GOOL and MIM complete.
 *
 * Registration is an explicit native Aether operation (--register all). Existing
 * identities are never deleted or replaced by this preflight. The native engine
 * writes missing identities into the same app-private working directory that is
 * encrypted by AetherIdentityVault whenever the process stops.
 */
internal data class AetherIdentityInfo(
    val deviceId: String,
    val ipv4: String,
    val ipv6: String,
)

internal data class AetherIdentityKey(
    val file: String,
    val identity: AetherIdentityInfo?,
)

internal data class AetherIdentityStatus(
    val protocol: String,
    val keys: List<AetherIdentityKey>,
)

internal object AetherIdentityManager {
    internal val requiredFiles = listOf(
        "aether-wg.toml",
        "aether-wg-secondary.toml",
        "aether-masque.toml",
        "aether-masque-secondary.toml",
        "aether-masque-gool.toml",
    )

    internal fun missingFiles(directory: File): List<String> =
        requiredFiles.filter { name ->
            val file = File(directory, name)
            !file.isFile || !isValidIdentityFile(file)
        }

    internal fun isReadyLine(line: String): Boolean =
        READY_LINE.containsMatchIn(line)

    internal fun registrationArguments(workingDirectory: File, apiAddress: String = ""): List<String> =
        buildList {
            addAll(listOf("--register", "all"))
            addAll(listOf("--config", File(workingDirectory, "aether.toml").absolutePath))
            addAll(listOf("--wg-config", File(workingDirectory, "aether-wg.toml").absolutePath))
            addAll(listOf("--masque-config", File(workingDirectory, "aether-masque.toml").absolutePath))
            if (apiAddress.isNotBlank() && apiAddress.all { it.isLetterOrDigit() || it in ".:-[]" }) {
                addAll(listOf("--api-address", apiAddress))
            }
            addAll(listOf("--log-level", "info"))
        }

    internal fun parseIdentity(text: String): AetherIdentityInfo? {
        val fields = mutableMapOf<String, String>()
        var insideMultiline = false
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (!insideMultiline) {
                IDENTITY_FIELD.matchEntire(line)?.destructured?.let { (key, value) ->
                    fields.putIfAbsent(key, value)
                }
            }
    private val MULTILINE_DELIMITER = Regex("""(?:"{3}|'{3})""")
                insideMultiline = !insideMultiline
            }
        }
        val deviceId = fields["device_id"]?.takeIf { it.isNotBlank() } ?: return null
        return AetherIdentityInfo(deviceId, fields["ipv4"].orEmpty(), fields["ipv6"].orEmpty())
    }

    internal fun isValidIdentityFile(file: File): Boolean =
        file.isFile && file.length() > 0L &&
            runCatching { parseIdentity(file.readText()) != null }.getOrDefault(false)

    /** Read native identity metadata without exposing private-key fields to the UI. */
    internal fun keys(directory: File, files: List<String> = requiredFiles): List<AetherIdentityKey> =
        files.map { name ->
            val file = File(directory, name)
            val identity = runCatching {
                if (file.isFile) parseIdentity(file.readText()) else null
            }.getOrNull()
            AetherIdentityKey(name, identity)
        }

    /** Identity files required by each native transport family. */
    internal fun filesOf(protocol: String): List<String> = when (protocol.lowercase().replace('-', '_')) {
        "masque" -> listOf("aether-masque.toml")
        "wg", "wireguard" -> listOf("aether-wg.toml")
        "gool" -> listOf("aether-wg.toml", "aether-wg-secondary.toml")
        "mim" -> listOf("aether-masque.toml", "aether-masque-secondary.toml")
        "wg_over_masque", "wireguard_over_masque", "wgmasque" ->
            listOf("aether-masque.toml", "aether-masque-gool.toml")
        else -> emptyList()
    }

    internal fun status(directory: File, protocol: String): AetherIdentityStatus =
        AetherIdentityStatus(protocol, keys(directory, filesOf(protocol)))

    /**
     * Returns true when every native identity already exists or Aether registered
     * the missing set successfully. Failure is non-fatal: the caller may still
     * try the normal connection path and report the native engine's own error.
     */
    suspend fun ensureAll(
        nativeLibraryDir: File,
        workingDirectory: File,
        apiAddress: String = "",
        timeoutMs: Long = REGISTRATION_TIMEOUT_MS,
    ): Boolean = withContext(Dispatchers.IO) {
        require(timeoutMs > 0)
        if (!workingDirectory.exists() && !workingDirectory.mkdirs()) return@withContext false

        AetherIdentityVault.unsealInto(workingDirectory)
        if (missingFiles(workingDirectory).isEmpty()) {
            // Keep credentials encrypted at rest between the preflight and the tunnel process start.
            AetherIdentityVault.sealAndShred(workingDirectory)
            return@withContext true
        }

        val engine = AetherEngineProcess(nativeLibraryDir, workingDirectory)
        try {
            engine.start(registrationArguments(workingDirectory, apiAddress))
            val deadline = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < deadline) {
                if (engine.registrationReady) break
                if (!engine.isAlive()) {
                    // Let the stdout reader consume the final buffered line.
                    Thread.sleep(150)
                    if (engine.registrationReady) break
                    return@withContext false
                }
                Thread.sleep(POLL_MS)
            }
            engine.registrationReady && missingFiles(workingDirectory).isEmpty()
        } catch (failure: Exception) {
            LogBus.append("W/Aether: identity registration preflight failed (" + failure.javaClass.simpleName + ")")
            false
        } finally {
            engine.stop()
        }
    }

    private val READY_LINE = Regex("""identities ready:\s*\S""", RegexOption.IGNORE_CASE)
    private val IDENTITY_FIELD = Regex("""^(device_id|ipv4|ipv6)\s*=\s*"([^"]*)"$""")
    private val MULTILINE_DELIMITER = Regex("""\"\"\"|'''""")
    private const val POLL_MS = 200L
    private const val REGISTRATION_TIMEOUT_MS = 4 * 60_000L
}
