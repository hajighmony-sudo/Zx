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
    fun recognizesOnlyNativeRegistrationCompletionLine() {
        assertTrue(AetherIdentityManager.isReadyLine("identities ready: all"))
        assertTrue(AetherIdentityManager.isReadyLine("[+] identities ready: masque"))
        assertFalse(AetherIdentityManager.isReadyLine("identity registration started"))
        assertFalse(AetherIdentityManager.isReadyLine("identities ready:"))
    }
}
