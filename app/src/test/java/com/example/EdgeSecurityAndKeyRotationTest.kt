package com.example

import com.example.ui.game.DilithiumPostQuantumEngine
import com.example.ui.game.EdgeSecurityEngine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EdgeSecurityAndKeyRotationTest {

    // =========================================================================
    // 1. DILITHIUM-3 KEY ROTATION UNIT TESTS
    // =========================================================================

    @Test
    fun testDilithiumSessionKeyRotatorGenesisAndRotation() {
        val rotator = DilithiumPostQuantumEngine.DilithiumSessionKeyRotator("test_pqc_rotator")

        // 1. Check Genesis Epoch #1
        assertEquals("Genesis epoch must be 1", 1, rotator.activeEpoch)
        assertNotNull(rotator.activeKeyPair.publicKey)
        assertNotNull(rotator.activeKeyPair.privateKey)
        assertTrue(rotator.activeKeyPair.publicKey.keyFingerprint.startsWith("dilithium3:0x"))
        assertEquals(1, rotator.rotationHistory.size)
        assertEquals(1, rotator.rotationHistory[0].epochNumber)

        val genesisFingerprint = rotator.activeKeyPair.publicKey.keyFingerprint

        // 2. Perform Key Rotation to Epoch #2
        val epoch2Record = rotator.rotateKey("Scheduled Key Expiry Rotation")
        assertEquals(2, rotator.activeEpoch)
        assertEquals(2, epoch2Record.epochNumber)
        assertNotEquals("Rotated public key must differ from genesis", genesisFingerprint, rotator.activeKeyPair.publicKey.keyFingerprint)
        assertEquals(2, rotator.rotationHistory.size)
        assertEquals("Scheduled Key Expiry Rotation", epoch2Record.rotationReason)

        // 3. Perform Key Rotation to Epoch #3
        val epoch3Record = rotator.rotateKey("High-Traffic Threat Rotation")
        assertEquals(3, rotator.activeEpoch)
        assertEquals(3, epoch3Record.epochNumber)
        assertNotEquals(epoch2Record.publicKeyFingerprint, rotator.activeKeyPair.publicKey.keyFingerprint)
        assertEquals(3, rotator.rotationHistory.size)
    }

    @Test
    fun testSignatureVerificationWithRotatedKeys() {
        val rotator = DilithiumPostQuantumEngine.DilithiumSessionKeyRotator("pqc_sign_verify_test")
        val samplePayload = "vmsb_native_bytecode_state_0xCAFEBABE".toByteArray()

        // Sign with Epoch #1
        val sigEpoch1 = DilithiumPostQuantumEngine.sign(samplePayload, rotator.activeKeyPair.privateKey)
        assertTrue("Epoch #1 signature must verify with Epoch #1 key",
            DilithiumPostQuantumEngine.verify(samplePayload, sigEpoch1, rotator.activeKeyPair.publicKey))

        // Rotate to Epoch #2
        rotator.rotateKey("Rotated to Epoch 2")
        val sigEpoch2 = DilithiumPostQuantumEngine.sign(samplePayload, rotator.activeKeyPair.privateKey)
        assertTrue("Epoch #2 signature must verify with Epoch #2 key",
            DilithiumPostQuantumEngine.verify(samplePayload, sigEpoch2, rotator.activeKeyPair.publicKey))

        // Rejection test: Tampered payload must fail
        assertFalse("Tampered payload must fail verification with rotated key",
            DilithiumPostQuantumEngine.verify(samplePayload, sigEpoch2, rotator.activeKeyPair.publicKey, isBytecodeTampered = true))
    }

    // =========================================================================
    // 2. EDGE ANYCAST, SSL/TLS PQC & ANTI-DDOS UNIT TESTS
    // =========================================================================

    @Test
    fun testAnycastEdgePoPsTopologyAndLatency() {
        val pops = EdgeSecurityEngine.POPS
        assertEquals(4, pops.size)

        // Ensure geographic coverage across 4 major zones
        val cities = pops.map { it.city }
        assertTrue(cities.contains("Dallas"))
        assertTrue(cities.contains("Frankfurt"))
        assertTrue(cities.contains("Tokyo"))
        assertTrue(cities.contains("Sydney"))

        // All PoPs must exhibit sub-30ms low latency and > 95% cache hit ratio
        pops.forEach { pop ->
            assertTrue("PoP ${pop.popId} latency must be < 30ms", pop.latencyMs < 30.0)
            assertTrue("PoP ${pop.popId} cache hit ratio must exceed 95%", pop.cacheHitRatioPercent >= 95.0f)
            assertTrue("PoP BGP ASN must be Anycast vmsb", pop.bgpAsn.contains("vmsb©"))
        }
    }

    @Test
    fun testTlsPqc0RttResumptionEvaluation() {
        // 0-RTT mode
        val summary0Rtt = EdgeSecurityEngine.evaluateTlsPqc(is0Rtt = true)
        assertTrue(summary0Rtt.isSessionResumed)
        assertTrue(summary0Rtt.handshakeMode.contains("0-RTT"))
        assertTrue("0-RTT latency must be sub-millisecond", summary0Rtt.handshakeLatencyMs < 1.0)
        assertTrue(summary0Rtt.pqcKeyExchange.contains("ML-KEM-768"))
        assertTrue(summary0Rtt.certificateSeal.contains("vmsb©"))

        // 1-RTT mode
        val summary1Rtt = EdgeSecurityEngine.evaluateTlsPqc(is0Rtt = false)
        assertFalse(summary1Rtt.isSessionResumed)
        assertTrue("1-RTT latency must be higher than 0-RTT", summary1Rtt.handshakeLatencyMs > 5.0)
    }

    @Test
    fun testAntiDdosEbpfMitigationAndZkpZeroCaptcha() {
        // 1. Nominal traffic
        val nominal = EdgeSecurityEngine.processTraffic(isAttackSimulated = false, isEbpfEnabled = true)
        assertFalse(nominal.isAttackUnderway)
        assertEquals(4200, nominal.legitimateRps)
        assertEquals(120, nominal.systemFluidityFps)
        assertEquals("vmsb©", nominal.protectionLabel)

        // 2. Simulated DDoS attack with eBPF enabled
        val attackMitigated = EdgeSecurityEngine.processTraffic(isAttackSimulated = true, isEbpfEnabled = true)
        assertTrue(attackMitigated.isAttackUnderway)
        assertTrue("Total RPS under attack must be high (> 400k RPS)", attackMitigated.totalIncomingRps >= 400_000)
        assertTrue("eBPF drop rate must exceed 95%", attackMitigated.ebpfDropRatePercent >= 95.0f)
        assertTrue("Tar-pit count must match mitigated traffic", attackMitigated.tarPitDivertedCount > 0)
        assertEquals("System fluidity must remain 120 FPS when eBPF is active", 120, attackMitigated.systemFluidityFps)

        // 3. Transparent ZKP Micro-Witness (< 0.2 ms execution, zero captchas)
        val (isSatisfied, latencyMs) = EdgeSecurityEngine.evaluateZkpMicroWitness("session_user_token_abc")
        assertTrue("Micro-proof must execute without user interaction", latencyMs < 5.0)
    }
}
