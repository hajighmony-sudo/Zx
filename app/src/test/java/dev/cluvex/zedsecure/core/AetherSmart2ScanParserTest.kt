package dev.cluvex.zedsecure.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AetherSmart2ScanParserTest {
    @Test
    fun parsesMasqueGateway() {
        val result = AetherSmart2ScanParser.parse("[+] selected MASQUE gateway 162.159.192.1:2408")
        assertEquals("MASQUE", result?.protocol)
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
    fun rejectsMalformedPorts() {
        assertNull(AetherSmart2ScanParser.parse("[+] selected WireGuard endpoint 192.0.2.1:99999"))
    }

    @Test
    fun ignoresUnrelatedOutput() {
        assertNull(AetherSmart2ScanParser.parse("[+] candidate ok but scan continues"))
    }
}
