package dev.cluvex.zedsecure.domain.config

import dev.cluvex.zedsecure.domain.model.DnsResolverProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LockedSharePayloadTest {
    @Test
    fun linkProfileRoundTripsWithItsFullMetadata() {
        val profile = VpnProfile(
            id = "profile-1",
            name = "Primary",
            protocol = "VLESS",
            address = "example.com",
            port = 443,
            transportLabel = "VLESS · TLS",
            source = ProfileSource.Link(
                "vless://00000000-0000-0000-0000-000000000000@example.com:443?security=tls&type=tcp#Primary",
            ),
            addedAt = 1234L,
            subscriptionId = "subscription-1",
            note = "portable note",
        )

        val payload = profile.lockedSharePayload()
        val restored = VpnProfile.fromLockedSharePayload(payload)

        assertNotNull(restored)
        assertEquals(profile, restored)
        assertEquals(profile.source, VpnProfile.sourceOfPayload(payload))
    }

    @Test
    fun dnsResolverProfileDoesNotCollapseIntoAnUnsupportedUri() {
        val profile = VpnProfile(
            id = "dns-1",
            name = "DNS profile",
            protocol = "DNS-DOH",
            address = "1.1.1.1",
            port = 443,
            transportLabel = "DNS · DOH",
            source = ProfileSource.DnsResolver(
                DnsResolverProfile(
                    id = "dns-1",
                    name = "DNS profile",
                    protocol = "doh",
                    remoteEndpoint = "https://dns.example/dns-query",
                    directAddress = "1.1.1.1",
                ),
            ),
            addedAt = 5678L,
        )

        val payload = profile.lockedSharePayload()
        val restored = VpnProfile.fromLockedSharePayload(payload)

        assertEquals(profile, restored)
        assertEquals(profile.source, VpnProfile.sourceOfPayload(payload))
    }
}
