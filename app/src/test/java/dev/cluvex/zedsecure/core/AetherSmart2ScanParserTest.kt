package dev.cluvex.zedsecure.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AetherSmart2ScanParserTest {
    @Test
    fun parsesActualMasqueSelectionLog() {
        val result = AetherSmart2ScanParser.parse("[+] best gateway 162.159.192.1:2408 rtt=18ms")
        assertEquals("MASQUE", result?.protocol)
        assertEquals("162.159.192.1:2408", result?.endpoint)
    }

    @Test
    fun parsesActualWireGuardSelectionLog() {
        val result = AetherSmart2ScanParser.parse("[+] wg endpoint 162.159.192.1:2408 rtt=21ms")
        assertEquals("WireGuard", result?.protocol)
        assertEquals("162.159.192.1:2408", result?.endpoint)
    }

    @Test
    fun parsesTwoHopGoolSelection() {
        val result = AetherSmart2ScanParser.parse(
            "[+] using cloudflare edge 162.159.192.1:2408 (outer) and 188.114.96.1:2408 (inner)",
        )
        assertEquals("GOOL", result?.protocol)
        assertEquals("162.159.192.1:2408", result?.endpoint)
        assertEquals("188.114.96.1:2408", result?.innerEndpoint)
    }

    @Test
    fun parsesMasqueInMasqueSelection() {
        val result = AetherSmart2ScanParser.parse(
            "[+] masque-in-masque ready: 162.159.192.1:2408 (outer) and 188.114.96.1:2408 (inner)",
        )
        assertEquals("MIM", result?.protocol)
        assertEquals("188.114.96.1:2408", result?.innerEndpoint)
    }

    @Test
    fun rejectsMalformedPorts() {
        assertNull(AetherSmart2ScanParser.parse("[+] wg endpoint 192.0.2.1:99999 rtt=5ms"))
    }

    @Test
    fun ignoresCandidateUntilSelection() {
        assertNull(AetherSmart2ScanParser.parse("[+] candidate ok 192.0.2.1:2408 rtt=5ms"))
    }

    @Test
    fun ignoresUnrelatedOutput() {
        assertNull(AetherSmart2ScanParser.parse("[+] scan mode=balanced candidates=12"))
    }
}
