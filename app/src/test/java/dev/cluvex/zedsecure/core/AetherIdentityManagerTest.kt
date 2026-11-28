package dev.cluvex.zedsecure.core

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AetherIdentityManagerTest {
    @Test
    fun reportsAllFiveNativeIdentityFilesAsMissingInitially() {
        val directory = Files.createTempDirectory("zx-aether-identities").toFile()
        try {
            assertEquals(AetherIdentityManager.requiredFiles, AetherIdentityManager.missingFiles(directory))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun considersIdentitySetCompleteOnlyWhenEveryFileIsNonEmpty() {
        val directory = Files.createTempDirectory("zx-aether-identities").toFile()
        try {
            AetherIdentityManager.requiredFiles.forEach { name ->
                java.io.File(directory, name).writeText("device_id = \"test\"\n")
            }
            assertTrue(AetherIdentityManager.missingFiles(directory).isEmpty())
            java.io.File(directory, AetherIdentityManager.requiredFiles.first()).writeText("")
            assertFalse(AetherIdentityManager.missingFiles(directory).isEmpty())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun rejectsNonEmptyFilesThatDoNotContainAnIdentityDeviceId() {
        val directory = Files.createTempDirectory("zx-aether-identities").toFile()
        try {
            AetherIdentityManager.requiredFiles.forEach { name ->
                java.io.File(directory, name).writeText("device_id = \"test\"\n")
            }
            val corrupt = java.io.File(directory, AetherIdentityManager.requiredFiles.first())
            corrupt.writeText("not a valid identity")
            assertTrue(AetherIdentityManager.missingFiles(directory).contains(corrupt.name))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun registrationUsesSameExplicitIdentityPathsAsTunnelRuns() {
        val directory = java.io.File("/data/user/0/example/files/aether")
        val args = AetherIdentityManager.registrationArguments(directory, "162.159.137.105")
        assertTrue(args.containsAll(listOf(
            "--register", "all",
            "--config", "/data/user/0/example/files/aether/aether.toml",
            "--wg-config", "/data/user/0/example/files/aether/aether-wg.toml",
            "--masque-config", "/data/user/0/example/files/aether/aether-masque.toml",
            "--api-address", "162.159.137.105",
            "--log-level", "info",
        )))
    }

    @Test
    fun registrationDoesNotPassUnsafeApiAddressAsAnArgument() {
        val args = AetherIdentityManager.registrationArguments(java.io.File("/tmp/aether"), "--bad value")
        assertFalse(args.contains("--api-address"))
    }

    @Test
    fun recognizesOnlyNativeRegistrationCompletionLine() {
        assertTrue(AetherIdentityManager.isReadyLine("identities ready: all"))
        assertTrue(AetherIdentityManager.isReadyLine("[+] identities ready: masque"))
        assertFalse(AetherIdentityManager.isReadyLine("identity registration started"))
        assertFalse(AetherIdentityManager.isReadyLine("identities ready:"))
    }
    @Test
    fun parsesIdentityMetadataWithoutReadingPrivateKeyBlocksAsFields() {
        val source = listOf(
            "device_id = \"device-123\"",
            "private_key = \"\"\"",
            "ipv4 = \"192.0.2.10\"",
            "ipv6 = \"2001:db8::10\"",
            "\"\"\""
        ).joinToString("\n")
        val parsed = AetherIdentityManager.parseIdentity(source)
        assertEquals("device-123", parsed?.deviceId)
        assertEquals("", parsed?.ipv4)
        assertEquals("", parsed?.ipv6)
    }
    @Test
    fun mapsProtocolToOnlyItsRequiredIdentityFiles() {
        assertEquals(listOf("aether-masque.toml"), AetherIdentityManager.filesOf("masque"))
        assertEquals(listOf("aether-wg.toml"), AetherIdentityManager.filesOf("wg"))
        assertEquals(
            listOf("aether-wg.toml", "aether-wg-secondary.toml"),
            AetherIdentityManager.filesOf("gool"),
        )
        assertEquals(
            listOf("aether-masque.toml", "aether-masque-secondary.toml"),
            AetherIdentityManager.filesOf("mim"),
        )
    }

    @Test
    fun mapsWireGuardOverMasqueToItsDedicatedInnerIdentity() {
        assertEquals(
            listOf("aether-masque.toml", "aether-masque-gool.toml"),
            AetherIdentityManager.filesOf("wg-over-masque"),
        )
        assertEquals(
            listOf("aether-masque.toml", "aether-masque-gool.toml"),
            AetherIdentityManager.filesOf("wireguard_over_masque"),
        )
    }

    @Test
    fun parsesIdentityFieldsOutsideTripleQuotedPrivateKey() {
        val source = listOf(
            "device_id = \"device-123\"",
            "private_key = \"\"\"",
            "ipv4 = \"192.0.2.10\"",
            "ipv6 = \"2001:db8::10\"",
            "\"\"\"",
        ).joinToString("\n")
        val parsed = AetherIdentityManager.parseIdentity(source)
        assertEquals("device-123", parsed?.deviceId)
        assertEquals("", parsed?.ipv4)
        assertEquals("", parsed?.ipv6)
    }

    @Test
    fun statusNeverReturnsPrivateKeyContents() {
        val directory = Files.createTempDirectory("zx-aether-status").toFile()
        try {
            java.io.File(directory, "aether-masque.toml").writeText(
                "device_id = \"safe-id\"\nipv4 = \"192.0.2.2\"\nprivate_key = \"secret\"\n",
            )
            val status = AetherIdentityManager.status(directory, "masque")
            assertEquals("safe-id", status.keys.single().identity?.deviceId)
            assertFalse(status.toString().contains("secret"))
        } finally {
            directory.deleteRecursively()
        }
    }

}
