package dev.cluvex.zedsecure.core

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private const val RENEWAL_READY_MARK = ".aether-renewal-ready"
    private val renewalMutex = Mutex()
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
            if (MULTILINE_DELIMITER.findAll(line).count() % 2 == 1) {
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
     * Replace only the requested identity files after every replacement has been registered
     * and validated in a separate directory. Existing active keys remain untouched on failure.
     *
     * The callback must run native Aether with its config/output paths directed at [renewalDirectory].
     * It must return true only after the engine reports that registration completed.
     */
    internal suspend fun renewAtomically(
        workingDirectory: File,
        renewalDirectory: File,
        files: List<String>,
        register: suspend (File) -> Boolean,
    ): Boolean = renewalMutex.withLock {
        require(files.isNotEmpty() && files.all { it in requiredFiles }) {
            "Only known Aether identity files can be renewed"
        }
        val prepared = withContext(Dispatchers.IO) {
            AetherIdentityVault.unsealInto(workingDirectory)
            settleRenewal(workingDirectory, renewalDirectory) &&
                renewalDirectory.deleteRecursively() &&
                renewalDirectory.mkdirs()
        }
        if (!prepared) return@withLock false

        val ready = AtomicBoolean(false)
        try {
            if (register(renewalDirectory)) {
                withContext(Dispatchers.IO) {
                    // Ignore unrelated files the native registration may have emitted.
                    (requiredFiles - files.toSet()).forEach { File(renewalDirectory, it).delete() }
                    ready.set(
                        files.all { isValidIdentityFile(File(renewalDirectory, it)) } &&
                            runCatching { File(renewalDirectory, RENEWAL_READY_MARK).createNewFile() }
                                .getOrDefault(false),
                    )
                }
            }
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                if (ready.get()) {
                    settleRenewal(workingDirectory, renewalDirectory)
                } else {
                    renewalDirectory.deleteRecursively()
                }
                AetherIdentityVault.sealAndShred(workingDirectory)
            }
        }
        ready.get()
    }

    /**
     * Finish a previously committed renewal after interruption. Without the ready marker,
     * the staged files are never promoted and the active identity set remains unchanged.
     */
    internal fun settleRenewal(workingDirectory: File, renewalDirectory: File): Boolean {
        if (!File(renewalDirectory, RENEWAL_READY_MARK).isFile) return true
        if (!workingDirectory.exists() && !workingDirectory.mkdirs()) return false
        for (name in requiredFiles) {
            val staged = File(renewalDirectory, name)
            if (staged.isFile && !moveIdentityOver(staged, File(workingDirectory, name))) return false
        }
        return renewalDirectory.deleteRecursively()
    }

    private fun moveIdentityOver(source: File, target: File): Boolean = try {
        Files.move(
            source.toPath(),
            target.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
        true
    } catch (_: IOException) {
        // Another recovery pass may already have moved this file.
        !source.isFile
    }

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
        val renewalDirectory = File(workingDirectory.parentFile ?: workingDirectory, "aether-renewal")
        if (!settleRenewal(workingDirectory, renewalDirectory)) {
            AetherIdentityVault.sealAndShred(workingDirectory)
            return@withContext false
        }
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
    private val MULTILINE_DELIMITER = Regex("""(?:"{3}|'{3})""")
    private const val POLL_MS = 200L
    private const val REGISTRATION_TIMEOUT_MS = 4 * 60_000L
}
