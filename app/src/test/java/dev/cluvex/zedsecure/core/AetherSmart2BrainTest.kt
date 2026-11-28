package dev.cluvex.zedsecure.core

import dev.cluvex.zedsecure.domain.config.AetherProfile
import org.junit.Assert.*
import org.junit.Test

class AetherSmart2BrainTest {
    @Test fun recognizesUdpTimeoutAndTlsReset() {
        assertTrue(AetherSmart2Brain.signalsOf("WireGuard handshake timed out") and AetherSmart2Brain.S_UDP_SILENT != 0)
        assertTrue(AetherSmart2Brain.signalsOf("TLS connection reset by peer") and AetherSmart2Brain.S_TLS_RESET != 0)
    }
    @Test fun identityRecoveryIsNotASecondFailure() {
        assertTrue(AetherSmart2Brain.signalsOf("identity config parse failed") and AetherSmart2Brain.S_IDENTITY != 0)
        assertEquals(0, AetherSmart2Brain.signalsOf("provisioning a fresh identity"))
    }
    @Test fun udpFailureMovesTcpAheadAndHardensIt() {
        val udp = AetherProfile(protocol = "wg", noize = "off")
        val tcp = AetherProfile(protocol = "masque", http2 = true, noize = "off")
        val result = AetherSmart2Brain.adapt(listOf(udp, tcp), AetherSmart2Brain.Knowledge(udpStrikes = 1, udpWeakHint = true))
        assertEquals("masque", result.first().protocol)
        assertEquals("firewall", result.first().noize)
        assertTrue(result.first().fragment)
    }
    @Test fun earlyAbandonNeedsEvidenceAndNoCandidate() {
        val wg = AetherProfile(protocol = "wg")
        val knowledge = AetherSmart2Brain.Knowledge(udpStrikes = 1, udpWeakHint = true)
        assertTrue(AetherSmart2Brain.shouldAbandonUdp(wg, knowledge, 8_000, false))
        assertFalse(AetherSmart2Brain.shouldAbandonUdp(wg, knowledge, 8_000, true))
    }
}
