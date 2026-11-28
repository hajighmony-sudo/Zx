package dev.cluvex.zedsecure.core

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
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
    fun registrationUsesNativeCoreKindsForEachProtocolFamily() {
        val directory = java.io.File("/tmp/aether")
        assertEquals("all", AetherIdentityManager.registrationArguments(directory).let {
            it[it.indexOf("--register") + 1]
        })
        assertEquals("gool-classic", AetherIdentityManager.registrationArguments(
            directory,
            registerKind = "gool-classic",
        ).let { it[it.indexOf("--register") + 1] })
        assertEquals("gool", AetherIdentityManager.registrationArguments(
            directory,
            registerKind = "gool",
        ).let { it[it.indexOf("--register") + 1] })
        assertEquals("mim", AetherIdentityManager.registrationArguments(
            directory,
            registerKind = "mim",
        ).let { it[it.indexOf("--register") + 1] })
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

    @Test
    fun failedRenewalLeavesActiveIdentityUntouched() = runBlocking {
        val root = Files.createTempDirectory("zx-aether-renew").toFile()
        val active = java.io.File(root, "active")
        val staged = java.io.File(root, "staged")
        active.mkdirs()
        val activeKey = java.io.File(active, "aether-masque.toml")
        activeKey.writeText("device_id = \"old-device\"\n")
        try {
            val renewed = AetherIdentityManager.renewAtomically(
                active,
                staged,
                listOf("aether-masque.toml"),
            ) { directory ->
                java.io.File(directory, "aether-masque.toml").writeText("device_id = \"new-device\"\n")
                false
            }
            assertFalse(renewed)
            assertEquals("old-device", AetherIdentityManager.parseIdentity(activeKey.readText())?.deviceId)
            assertFalse(staged.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun successfulRenewalPromotesOnlyAfterAllRequestedKeysAreValid() = runBlocking {
        val root = Files.createTempDirectory("zx-aether-renew").toFile()
        val active = java.io.File(root, "active")
        val staged = java.io.File(root, "staged")
        active.mkdirs()
        val activeKey = java.io.File(active, "aether-masque.toml")
        val unrelatedKey = java.io.File(active, "aether-wg.toml")
        activeKey.writeText("device_id = \"old-masque\"\n")
        unrelatedKey.writeText("device_id = \"keep-wg\"\n")
        try {
            val renewed = AetherIdentityManager.renewAtomically(
                active,
                staged,
                listOf("aether-masque.toml"),
            ) { directory ->
                java.io.File(directory, "aether-masque.toml").writeText("device_id = \"new-masque\"\n")
                true
            }
            assertTrue(renewed)
            assertEquals("new-masque", AetherIdentityManager.parseIdentity(activeKey.readText())?.deviceId)
            assertEquals("keep-wg", AetherIdentityManager.parseIdentity(unrelatedKey.readText())?.deviceId)
            assertFalse(staged.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun interruptedCommittedRenewalCanBeSettledOnNextUse() {
        val root = Files.createTempDirectory("zx-aether-recover").toFile()
        val active = java.io.File(root, "active")
        val staged = java.io.File(root, "staged")
        active.mkdirs()
        val activeKey = java.io.File(active, "aether-masque.toml")
        activeKey.writeText("device_id = \"old-device\"\n")
        try {
            java.io.File(staged, "aether-masque.toml").parentFile?.mkdirs()
            java.io.File(staged, "aether-masque.toml").writeText("device_id = \"committed-device\"\n")
            java.io.File(staged, ".aether-renewal-ready").createNewFile()
            assertTrue(AetherIdentityManager.settleRenewal(active, staged))
            assertEquals("committed-device", AetherIdentityManager.parseIdentity(activeKey.readText())?.deviceId)
            assertFalse(staged.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun incompleteRenewalNeverReplacesActiveIdentity() = runBlocking {
        val root = Files.createTempDirectory("zx-aether-incomplete").toFile()
        val active = java.io.File(root, "active")
        val staged = java.io.File(root, "staged")
        active.mkdirs()
        val activeKey = java.io.File(active, "aether-masque.toml")
        activeKey.writeText("device_id = \"old-device\"\n")
        try {
            val renewed = AetherIdentityManager.renewAtomically(
                active,
                staged,
                listOf("aether-masque.toml", "aether-masque-secondary.toml"),
            ) { directory ->
                java.io.File(directory, "aether-masque.toml").writeText("device_id = \"new-device\"\n")
                true
            }
            assertFalse(renewed)
            assertEquals("old-device", AetherIdentityManager.parseIdentity(activeKey.readText())?.deviceId)
        } finally {
            root.deleteRecursively()
        }
    }

}
