package dev.cluvex.zedsecure.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AetherSmart2MemoryTest {
    @Test fun pruneExpiresFailuresAndBoundsNetworkCount() {
        val now = 1_000_000L
        val entries = (0..40).associate { index ->
            "net$index" to AetherSmart2Memory.Entry("route$index", now - index,
                mapOf("failed" to now - AetherSmart2Memory.FAILED_TTL_MS - 1))
        }
        val pruned = AetherSmart2Memory.prune(entries, now)
        assertEquals(AetherSmart2Memory.MAX_NETWORKS, pruned.size)
        assertTrue(pruned.values.all { it.failedAt.isEmpty() })
    }
}
