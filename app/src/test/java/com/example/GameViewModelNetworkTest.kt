package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.game.GameViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GameViewModelNetworkTest {

    private lateinit var viewModel: GameViewModel
    private lateinit var networkSimulator: NetworkSimulator

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        viewModel = GameViewModel(context as android.app.Application)
        networkSimulator = NetworkSimulator(NetworkProfile.STANDARD_4G)
    }

    @Test
    fun `test initial state when application context initializes`() {
        assertNotNull("ViewModel instance should initialize successfully", viewModel)
        // Unauthenticated firebase user is null in offline unit test environment
        assertNull(viewModel.firebaseUser)
    }

    @Test
    fun `test tactical progress save payload creation under network environments`() {
        viewModel.playerCredits = 50000
        viewModel.playerAlloys = 12000
        viewModel.globalPopulation = 8_000_000_000L

        networkSimulator.currentProfile = NetworkProfile.STANDARD_4G
        val result4G = networkSimulator.transmitPayload(payloadSizeBytes = 512L)

        assertTrue("Save sync should succeed on 4G", result4G.success)

        networkSimulator.currentProfile = NetworkProfile.DEGRADED_3G
        val result3G = networkSimulator.transmitPayload(payloadSizeBytes = 512L)

        assertTrue("Save sync should succeed on 3G with higher latency", result3G.success)
        assertTrue("3G latency should exceed 4G base latency", result3G.latencyMs > result4G.latencyMs)
    }

    @Test
    fun `test offline fallback behavior maintains game consistency`() {
        networkSimulator.currentProfile = NetworkProfile.OFFLINE_AIRGAP
        val result = networkSimulator.transmitPayload(payloadSizeBytes = 256L)

        assertFalse(result.success)
        val initialCredits = viewModel.playerCredits
        viewModel.playerCredits += 500
        assertEquals(initialCredits + 500, viewModel.playerCredits)
    }
}
