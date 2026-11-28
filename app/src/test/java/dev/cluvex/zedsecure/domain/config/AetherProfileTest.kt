package dev.cluvex.zedsecure.domain.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AetherProfileTest {
    @Test
    fun encodedProfileRoundTripsAndRejectsOtherConfigTypes() {
        val profile = AetherProfile(
            profileName = "Primary",
            protocol = "masque",
            http2 = true,
            fragment = true,
            apiAddress = "162.159.137.105",
        )

        val decoded = AetherProfile.decode(AetherProfile.encode(profile))

        assertEquals(profile, decoded)
        assertNull(AetherProfile.decode("""{"protocol":"vless"}"""))
    }

    @Test
    fun attemptsKeepUserSelectionFirstAndRemoveDuplicates() {
        val profile = AetherProfile(protocol = "masque", http2 = false, smartFailover = true)

        val attempts = profile.attempts()

        assertEquals(profile, attempts.first())
        assertEquals(attempts.size, attempts.distinct().size)
        assertTrue(attempts.any { it.protocol == "wg" })
        assertTrue(attempts.any { it.protocol == "gool" && it.http2 })
        assertTrue(attempts.any { it.protocol == "mim" && it.http2 })
        assertTrue(attempts.any { it.protocol == "masque" && it.http2 && it.fragment })
    }

    @Test
    fun manualProfilesNeverSilentlySwitchTransport() {
        val manual = AetherProfile(protocol = "masque", http2 = false, smartFailover = false)
        assertEquals(listOf(manual), manual.attempts())
    }

    @Test
    fun smartProfilesCanTryAllNativeTransportFamilies() {
        val smart = AetherProfile(protocol = "masque", smartFailover = true)
        assertTrue(smart.attempts().map { it.protocol }.containsAll(listOf("masque", "wg", "gool", "mim")))
    }

    @Test
    fun argumentsIncludeLoopbackBindingAndDiagnostics() {
        val args = AetherProfile().toArgs(10991, "/data/user/0/app/files/aether")

        assertTrue(args.containsAll(listOf("--bind", "127.0.0.1:10991")))
        assertTrue(args.containsAll(listOf("--stats", "--stats-secs", "15")))
        assertTrue(args.contains("--quick-reconnect"))
        assertTrue(args.containsAll(listOf("--config", "/data/user/0/app/files/aether/aether.toml")))
        assertFalse(args.contains("--fragment"))
    }


    @Test
    fun smartProfileProducesCompleteNativeCommandFromDetectedSettings() {
        val profile = AetherSmartConfig.build(
            AetherSmartInput(
                network = dev.cluvex.zedsecure.platform.NetworkKind.Cellular,
                hasIpv6 = true,
                edgeReachable = true,
                apiReachable = false,
                udpReachable = false,
                tcp443Reachable = true,
                tlsSniReachable = false,
                dnsSystemMatchesDoh = false,
                edgeLossPercent = 0,
                apiLossPercent = 100,
            ),
        )

        val command = profile.commandPreview()

        assertTrue(command.contains("--protocol masque"))
        assertTrue(command.contains("--scan ironclad"))
        assertTrue(command.contains("--ip both"))
        assertTrue(command.contains("--noize gfw"))
        assertTrue(command.contains("--h2"))
        assertTrue(command.contains("--fragment"))
        assertTrue(command.contains("--api-address 162.159.137.105"))
        assertTrue(command.contains("127.0.0.1:<SOCKS_PORT>"))
        assertTrue(command.contains("<AETHER_HOME>/aether.toml"))
        assertTrue(profile.smartFailover)
    }

    @Test
    fun generatedNativeCommandMatchesManualArgumentBuilder() {
        val profile = AetherProfile(
            protocol = "masque",
            scanMode = "verified",
            ipVersion = "v4",
            noize = "firewall",
            http2 = true,
            fragment = true,
            apiAddress = "162.159.137.105",
        )

        val preview = profile.commandPreview()
        val args = profile.toArgs(10991, "<AETHER_HOME>")

        assertTrue(preview.startsWith("libaether.so "))
        assertTrue(preview.contains("--quick-reconnect"))
        assertTrue(preview.contains("--stats --stats-secs 15"))
        assertTrue(preview.contains("--config <AETHER_HOME>/aether.toml"))
        assertTrue(args.containsAll(listOf("--h2", "--fragment", "--api-address", "162.159.137.105")))
    }

    @Test
    fun startupBudgetAllowsNativeScanToFinish() {
        assertEquals(60_000L, AetherProfile(scanMode = "turbo").startupBudgetMs())
        assertEquals(120_000L, AetherProfile(scanMode = "balanced").startupBudgetMs())
        assertEquals(330_000L, AetherProfile(scanMode = "thorough").startupBudgetMs())
        assertEquals(330_000L, AetherProfile(scanMode = "verified").startupBudgetMs())
        assertEquals(330_000L, AetherProfile(scanMode = "ironclad").startupBudgetMs())
    }

    @Test
    fun invalidProtocolIsRejectedBeforeProcessLaunch() {
        val failure = runCatching {
            AetherProfile(protocol = "unknown").toArgs(10991, "/tmp/aether")
        }.exceptionOrNull()

        assertNotNull(failure)
        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun legacyProfileWithoutMarkerIsStillRecognizedAsAether() {
        val legacy = """{"profileName":"Aether · Smart · Mobile","protocol":"masque","scanMode":"thorough","ipVersion":"v4","noize":"firewall","http2":true}"""
        val decoded = AetherProfile.decode(legacy)
        assertNotNull(decoded)
        assertTrue(decoded!!.http2)
        assertNull(AetherProfile.decode("""{"outbounds":[],"inbounds":[],"log":{}}"""))
    }
    @Test
    fun smartProfilesOptIntoFailoverWhileManualProfilesStayIsolated() {
        assertFalse(AetherProfile().smartFailover)
        assertTrue(AetherSmartConfig.build(AetherSmartInput()).smartFailover)
    }

    @Test
    fun smartFailoverFlagSurvivesProfileEncoding() {
        val profile = AetherSmartConfig.build(AetherSmartInput())
        assertEquals(profile, AetherProfile.decode(AetherProfile.encode(profile)))
    }

    @Test
    fun smartUsesMeasuredLossToChooseConservativeTransport() {
        val profile = AetherSmartConfig.build(
            AetherSmartInput(
                network = dev.cluvex.zedsecure.platform.NetworkKind.Wifi,
                edgeReachable = true,
                edgeMs = 180,
                apiReachable = true,
                edgeJitterMs = 240,
                edgeLossPercent = 34,
                apiLossPercent = 0,
            ),
        )

        assertEquals("thorough", profile.scanMode)
        assertEquals("gfw", profile.noize)
        assertTrue(profile.http2)
        assertTrue(profile.fragment)
    }

    @Test
    fun smartDoesNotTreatCountryAsProofOfDpiFiltering() {
        val profile = AetherSmartConfig.build(
            AetherSmartInput(
                network = dev.cluvex.zedsecure.platform.NetworkKind.Wifi,
                countryCode = "RU",
                edgeReachable = true,
                edgeMs = 90,
                apiReachable = true,
                edgeJitterMs = 10,
                edgeLossPercent = 0,
                apiLossPercent = 0,
            ),
        )

        assertEquals("light", profile.noize)
        assertFalse(profile.http2)
    }

    @Test
    fun smartUsesHttp2WhenUdpIsThrottledButTlsSniWorks() {
        val profile = AetherSmartConfig.build(
            AetherSmartInput(
                network = dev.cluvex.zedsecure.platform.NetworkKind.Wifi,
                edgeReachable = true,
                apiReachable = true,
                udpReachable = false,
                tlsSniReachable = true,
                edgeLossPercent = 0,
                apiLossPercent = 0,
            ),
        )

        assertTrue(profile.http2)
        assertTrue(profile.fragment)
        assertEquals("firewall", profile.noize)
    }

    @Test
    fun smartSpeedGoalDoesNotForceUdpWhenFingerprintShowsThrottling() {
        val profile = AetherSmartConfig.build(
            AetherSmartInput(
                network = dev.cluvex.zedsecure.platform.NetworkKind.Wifi,
                goal = AetherSmartGoal.Speed,
                edgeReachable = true,
                edgeMs = 80,
                apiReachable = true,
                udpReachable = false,
                tlsSniReachable = true,
                edgeLossPercent = 0,
                apiLossPercent = 0,
            ),
        )

        assertTrue(profile.http2)
        assertTrue(profile.fragment)
    }

    @Test
    fun smartUsesStrongObfuscationWhenUdpAndSniBothFail() {
        val profile = AetherSmartConfig.build(
            AetherSmartInput(
                network = dev.cluvex.zedsecure.platform.NetworkKind.Wifi,
                edgeReachable = true,
                apiReachable = true,
                udpReachable = false,
                tlsSniReachable = false,
                edgeLossPercent = 0,
                apiLossPercent = 0,
            ),
        )

        assertTrue(profile.http2)
        assertTrue(profile.fragment)
        assertEquals("gfw", profile.noize)
        assertEquals("ironclad", profile.scanMode)
    }


    @Test
    fun smartTreatsDnsMismatchAsPotentialTamperingAndUsesConservativeTransport() {
        val profile = AetherSmartConfig.build(
            AetherSmartInput(
                network = dev.cluvex.zedsecure.platform.NetworkKind.Wifi,
                edgeReachable = true,
                apiReachable = true,
                udpReachable = true,
                tcp443Reachable = true,
                tlsSniReachable = true,
                dnsSystemMatchesDoh = false,
            ),
        )

        assertTrue(profile.http2)
        assertTrue(profile.fragment)
        assertEquals("firewall", profile.noize)
        assertEquals("thorough", profile.scanMode)
    }

    @Test
    fun smartDistinguishesTcp443FailureFromTlsStageInterference() {
        val profile = AetherSmartConfig.build(
            AetherSmartInput(
                network = dev.cluvex.zedsecure.platform.NetworkKind.Wifi,
                edgeReachable = true,
                apiReachable = true,
                tcp443Reachable = false,
                tlsSniReachable = false,
                udpReachable = true,
            ),
        )

        assertTrue(profile.http2)
        assertEquals("firewall", profile.noize)
    }

    @Test
    fun recentOoniAsnAnomaliesCorroborateButCountryAloneDoesNot() {
        val profile = AetherSmartConfig.build(
            AetherSmartInput(
                network = dev.cluvex.zedsecure.platform.NetworkKind.Wifi,
                countryCode = "XX",
                edgeReachable = true,
                apiReachable = true,
                tcp443Reachable = true,
                tlsSniReachable = true,
                udpReachable = true,
                ooniAnomalyPercent = 75,
            ),
        )

        assertTrue(profile.http2)
        assertEquals("gfw", profile.noize)
        assertEquals("ironclad", profile.scanMode)
    }


    @Test
    fun smartSpeedGoalCannotOverrideDnsOrCommunityFilteringEvidence() {
        val profile = AetherSmartConfig.build(
            AetherSmartInput(
                network = dev.cluvex.zedsecure.platform.NetworkKind.Wifi,
                goal = AetherSmartGoal.Speed,
                edgeReachable = true,
                apiReachable = true,
                udpReachable = true,
                tcp443Reachable = true,
                tlsSniReachable = true,
                dnsSystemMatchesDoh = false,
                ooniAnomalyPercent = 80,
            ),
        )

        assertTrue(profile.http2)
        assertTrue(profile.fragment)
        assertEquals("gfw", profile.noize)
        assertEquals("ironclad", profile.scanMode)
    }

    @Test
    fun smartUsesApiFallbackWhenRepeatedApiSamplesAreDegraded() {
        val profile = AetherSmartConfig.build(
            AetherSmartInput(
                network = dev.cluvex.zedsecure.platform.NetworkKind.Wifi,
                edgeReachable = true,
                apiReachable = true,
                edgeLossPercent = 0,
                apiLossPercent = 67,
            ),
        )

        assertEquals("162.159.137.105", profile.apiAddress)
        assertTrue(profile.http2)
    }

    @Test
    fun smartUsesApiLatencyAsADegradationSignalEvenWhenReachable() {
        val profile = AetherSmartConfig.build(
            AetherSmartInput(
                network = dev.cluvex.zedsecure.platform.NetworkKind.Wifi,
                edgeReachable = true,
                edgeMs = 85,
                apiReachable = true,
                apiMs = 1_250,
                edgeLossPercent = 0,
                apiLossPercent = 0,
                udpReachable = true,
                tcp443Reachable = true,
                tlsSniReachable = true,
                dnsSystemMatchesDoh = true,
            ),
        )

        assertEquals("162.159.137.105", profile.apiAddress)
        assertTrue(profile.http2)
        assertEquals("firewall", profile.noize)
        assertEquals("thorough", profile.scanMode)
    }

    @Test
    fun stabilityGoalPrefersPredictableTransportWithoutUnneededFragmentation() {
        val profile = AetherSmartConfig.build(
            AetherSmartInput(
                network = dev.cluvex.zedsecure.platform.NetworkKind.Wifi,
                edgeReachable = true,
                edgeMs = 90,
                apiReachable = true,
                apiMs = 110,
                edgeLossPercent = 0,
                apiLossPercent = 0,
                udpReachable = true,
                tcp443Reachable = true,
                tlsSniReachable = true,
                dnsSystemMatchesDoh = true,
                goal = AetherSmartGoal.Stability,
            ),
        )

        assertEquals("balanced", profile.scanMode)
        assertEquals("balanced", profile.noize)
        assertTrue(profile.http2)
        assertFalse(profile.fragment)
        assertEquals("", profile.apiAddress)
    }
    @Test
    fun smartUsesApiJitterAsAStabilitySignalWithoutAssumingDpiInterference() {
        val profile = AetherSmartConfig.build(
            AetherSmartInput(
                network = dev.cluvex.zedsecure.platform.NetworkKind.Wifi,
                edgeReachable = true,
                edgeMs = 80,
                apiReachable = true,
                apiMs = 120,
                edgeJitterMs = 12,
                apiJitterMs = 300,
                edgeLossPercent = 0,
                apiLossPercent = 0,
                udpReachable = true,
                tcp443Reachable = true,
                tlsSniReachable = true,
                dnsSystemMatchesDoh = true,
            ),
        )

        assertEquals("thorough", profile.scanMode)
        assertEquals("firewall", profile.noize)
        assertTrue(profile.http2)
        assertFalse(profile.fragment)
        assertEquals("", profile.apiAddress)
    }

}
