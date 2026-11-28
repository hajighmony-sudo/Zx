package dev.cluvex.zedsecure.domain.config

import dev.cluvex.zedsecure.platform.NetworkKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AetherSmart2ProfileTest {
    @Test
    fun profileRoundTripsAndKeepsSecondEngineSeparate() {
        val profile = AetherSmart2Profile.build(NetworkKind.Wifi, true, null)
        val decoded = AetherSmart2Profile.decode(AetherSmart2Profile.encode(profile))
        assertEquals(profile, decoded)
        assertTrue(profile.profileName.contains("Smart 2"))
        assertEquals(true, profile.aetherProfile.smartFailover)
        assertTrue(profile.aetherProfile.protocol in setOf("masque", "wg", "gool", "mim"))
        assertTrue(profile.aetherProfile.attempts().isNotEmpty())
    }

    @Test
    fun commandUsesNativeAetherAndDynamicLoopbackPort() {
        val profile = AetherSmart2Profile.build(NetworkKind.Cellular, false, null)
        val args = profile.commandArgs(10919, "/tmp/aether-smart2")
        assertTrue(args.containsAll(listOf(
            "--bind", "127.0.0.1:10919",
            "--protocol", profile.aetherProfile.protocol,
            "--scan", profile.aetherProfile.scanMode,
            "--ip", profile.aetherProfile.ipVersion,
            "--noize", profile.aetherProfile.noize,
            "--config", "/tmp/aether-smart2/aether.toml",
        )))
        assertTrue("--psiphon-only" !in args)
    }

    @Test
    fun xrayTemplateContainsRoutingDnsAndAllThreeLocalInbounds() {
        val profile = AetherSmart2Profile.build(NetworkKind.Wifi, true, null)
        val json = profile.xrayConfig(10919, 10922)
        assertTrue(json.contains("\"port\": 10919"))
        assertTrue(json.contains("\"port\": 10922"))
        assertTrue(json.contains("\"tag\": \"secondary-socks\""))
        assertTrue(json.contains("\"tag\": \"dns-module\""))
        assertTrue(json.contains("\"address\": \"fakedns\""))
        assertTrue(json.contains("dns.smartguard.io"))
        assertTrue(json.contains("\"geoip:ir\""))
        assertTrue(json.contains("\"finalQuery\": true"))
        assertTrue(json.contains("\"domestic-dns_1_0\""))
        assertTrue(json.contains("\"domestic-dns_2_0\""))
        assertTrue(json.contains("\"inboundTag\": [\"domestic-dns_1_0\", \"domestic-dns_2_0\"]"))
    }
}
