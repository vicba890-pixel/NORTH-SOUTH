package com.example.data

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class NetworkProfile(
    val profileName: String,
    val baseLatencyMs: Long,
    val jitterMs: Long,
    val packetLossPercentage: Double,
    val bandwidthKbps: Long,
    val isConnected: Boolean
) {
    ULTRA_5G("5G Ultra-Wideband", baseLatencyMs = 12L, jitterMs = 3L, packetLossPercentage = 0.0, bandwidthKbps = 500_000L, isConnected = true),
    STANDARD_4G("4G LTE Global", baseLatencyMs = 45L, jitterMs = 10L, packetLossPercentage = 0.1, bandwidthKbps = 50_000L, isConnected = true),
    DEGRADED_3G("3G HSPA Mobile", baseLatencyMs = 180L, jitterMs = 40L, packetLossPercentage = 1.5, bandwidthKbps = 3_600L, isConnected = true),
    CONGESTED_2G("2G EDGE Constrained", baseLatencyMs = 650L, jitterMs = 150L, packetLossPercentage = 5.0, bandwidthKbps = 128L, isConnected = true),
    LEO_SATELLITE("LEO Satellite Constellation", baseLatencyMs = 280L, jitterMs = 60L, packetLossPercentage = 2.0, bandwidthKbps = 25_000L, isConnected = true),
    OFFLINE_AIRGAP("Air-Gapped Offline Sandbox", baseLatencyMs = 999_999L, jitterMs = 0L, packetLossPercentage = 100.0, bandwidthKbps = 0L, isConnected = false)
}

data class NetworkTransmissionResult(
    val success: Boolean,
    val latencyMs: Long,
    val attempts: Int,
    val bytesTransferred: Long,
    val errorMessage: String? = null
)

class NetworkSimulator(
    var currentProfile: NetworkProfile = NetworkProfile.STANDARD_4G,
    private val randomSupplier: () -> Double = { Random.nextDouble() }
) {

    fun calculateEffectiveLatency(payloadSizeBytes: Long = 1024L): Long {
        if (!currentProfile.isConnected) return 999_999L
        val jitterVariation = ((randomSupplier() * 2 - 1) * currentProfile.jitterMs).toLong()
        val transferTimeMs = if (currentProfile.bandwidthKbps > 0) {
            (payloadSizeBytes * 8L * 1000L) / (currentProfile.bandwidthKbps * 1024L)
        } else 0L
        return max(5L, currentProfile.baseLatencyMs + jitterVariation + transferTimeMs)
    }

    fun simulatePacketDrop(): Boolean {
        if (!currentProfile.isConnected) return true
        return (randomSupplier() * 100.0) < currentProfile.packetLossPercentage
    }

    fun transmitPayload(
        payloadSizeBytes: Long,
        maxRetries: Int = 3
    ): NetworkTransmissionResult {
        if (!currentProfile.isConnected) {
            return NetworkTransmissionResult(
                success = false,
                latencyMs = 0L,
                attempts = 1,
                bytesTransferred = 0L,
                errorMessage = "Network Disconnected: Offline Local WASM Sandbox Active"
            )
        }

        var totalLatency = 0L
        var attempts = 0

        while (attempts < maxRetries) {
            attempts++
            val latency = calculateEffectiveLatency(payloadSizeBytes)
            totalLatency += latency

            val isDropped = simulatePacketDrop()
            if (!isDropped) {
                return NetworkTransmissionResult(
                    success = true,
                    latencyMs = totalLatency,
                    attempts = attempts,
                    bytesTransferred = payloadSizeBytes,
                    errorMessage = null
                )
            }
            // Exponential backoff penalty for retries
            totalLatency += (50L * (1 shl (attempts - 1)))
        }

        return NetworkTransmissionResult(
            success = false,
            latencyMs = totalLatency,
            attempts = attempts,
            bytesTransferred = 0L,
            errorMessage = "Packet loss threshold exceeded after $attempts attempts under ${currentProfile.profileName}"
        )
    }
}
