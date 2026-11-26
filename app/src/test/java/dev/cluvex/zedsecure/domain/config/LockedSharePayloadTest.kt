package dev.cluvex.zedsecure.domain.config

import dev.cluvex.zedsecure.domain.model.DnsResolverProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LockedSharePayloadTest {
    @Test
    fun linkProfilePreservesPortableDataButNotSenderLocalState() {
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
            subscriptionId = "sender-subscription",
            lastPingMs = 42,
            bytesDown = 1000L,
            bytesUp = 2000L,
            note = "sender-only note",
            dnsAutoManaged = true,
        )

        val payload = profile.lockedSharePayload()
        val restored = VpnProfile.fromLockedSharePayload(payload)

        assertNotNull(restored)
        assertEquals(profile.id, restored!!.id)
        assertEquals(profile.name, restored.name)
        assertEquals(profile.source, restored.source)
        assertEquals("", restored.subscriptionId)
        assertEquals(null, restored.lastPingMs)
        assertEquals(0L, restored.bytesDown)
        assertEquals(0L, restored.bytesUp)
        assertEquals(null, restored.note)
        assertEquals(false, restored.dnsAutoManaged)
        assertEquals(profile.source, VpnProfile.sourceOfPayload(payload))
    }

    @Test
    fun dnsResolverProfilesRemainTransferableWithoutAnExportableUri() {
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

        assertNotNull(restored)
        assertEquals(profile.source, restored!!.source)
        assertEquals(profile.source, VpnProfile.sourceOfPayload(payload))
    }
}
