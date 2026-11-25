package dev.cluvex.zedsecure.core.ghost

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdultBlocklistTest {
    @Test
    fun acceptsFullUrlsAndNormalizesToRegistrableHost() {
        val hosts = AdultBlocklist.normalizeHosts(
            "https://www.example.com/watch?id=123\nhttp://sub.example.org:8443/path",
        )

        assertEquals(listOf("example.com", "sub.example.org"), hosts)
    }

    @Test
    fun ignoresInvalidHostsAndIpLiterals() {
        val hosts = AdultBlocklist.normalizeHosts(
            "localhost\n127.0.0.1\nnot-a-domain\nhttps://example.com/path",
        )

        assertEquals(listOf("example.com"), hosts)
    }

    @Test
    fun userEntriesBecomeDomainRulesForSubdomains() {
        val rules = AdultBlocklist.userRules("https://www.example.com/path")

        assertEquals(listOf("domain:example.com"), rules)
    }

    @Test
    fun builtInPolicyIncludesGeoCategoryAndEncryptedDnsEndpoints() {
        assertTrue(AdultBlocklist.rules.contains("geosite:category-porn"))
        assertTrue(AdultBlocklist.rules.contains("domain:dns.google"))
        assertFalse(AdultBlocklist.rules.isEmpty())
    }
}
