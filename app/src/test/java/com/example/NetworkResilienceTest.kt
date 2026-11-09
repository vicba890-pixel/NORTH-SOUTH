package com.example.data

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NetworkResilienceTest {

    private lateinit var networkSimulator: NetworkSimulator

    @Before
    fun setUp() {
        networkSimulator = NetworkSimulator(NetworkProfile.STANDARD_4G)
    }

    @Test
    fun `test 5G ultra profile transmission succeeds with low latency`() {
        networkSimulator.currentProfile = NetworkProfile.ULTRA_5G
        val result = networkSimulator.transmitPayload(payloadSizeBytes = 2048L)

        assertTrue("5G transmission should succeed", result.success)
        assertTrue("5G latency should be under 100ms", result.latencyMs < 100L)
        assertEquals(1, result.attempts)
        assertNull(result.errorMessage)
    }

    @Test
    fun `test offline airgap fails gracefully with offline sandbox error`() {
        networkSimulator.currentProfile = NetworkProfile.OFFLINE_AIRGAP
        val result = networkSimulator.transmitPayload(payloadSizeBytes = 1024L)

        assertFalse("Offline mode should fail network call", result.success)
        assertEquals(0L, result.bytesTransferred)
        assertTrue(result.errorMessage?.contains("Offline Local WASM Sandbox") == true)
    }

    @Test
    fun `test 2G EDGE transmission handles high latency and packet retry`() {
        var callCount = 0
        val mockRandom = {
            callCount++
            if (callCount <= 2) 0.001 else 0.99
        }
        val simulator = NetworkSimulator(NetworkProfile.CONGESTED_2G, mockRandom)

        val result = simulator.transmitPayload(payloadSizeBytes = 512L, maxRetries = 3)

        assertTrue("2G retry strategy should eventually succeed", result.success)
        assertEquals(2, result.attempts)
        assertTrue("Latency should include retry backoff", result.latencyMs > 600L)
    }

    @Test
    fun `test LEO satellite profile calculates expected latency range`() {
        networkSimulator.currentProfile = NetworkProfile.LEO_SATELLITE
        val latency = networkSimulator.calculateEffectiveLatency(payloadSizeBytes = 4096L)

        assertTrue("Satellite latency should be around 200-500ms", latency in 200L..500L)
    }

    @Test
    fun `test packet loss exceeding max retries yields error result`() {
        val alwaysDropSimulator = NetworkSimulator(NetworkProfile.CONGESTED_2G) { 0.001 }
        val result = alwaysDropSimulator.transmitPayload(payloadSizeBytes = 1024L, maxRetries = 3)

        assertFalse("Repeated packet drops should fail", result.success)
        assertEquals(3, result.attempts)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("Packet loss threshold exceeded"))
    }
}
