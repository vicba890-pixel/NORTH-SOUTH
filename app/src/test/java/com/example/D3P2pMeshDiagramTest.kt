package com.example

import com.example.ui.game.D3MeshJsBridge
import com.example.ui.game.PeerMeshNode
import com.example.ui.game.defaultP2pMeshNodes
import com.example.ui.game.generateD3MeshHtml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class D3P2pMeshDiagramTest {

    @Test
    fun `test default mesh nodes contains NATS gateway and exactly 8 peers`() {
        val nodes = defaultP2pMeshNodes
        assertEquals("Total nodes must be 9 (1 Gateway + 8 Peers)", 9, nodes.size)

        val gateway = nodes.first { it.isGateway }
        assertEquals("nats_gateway", gateway.id)
        assertTrue("Gateway name must contain NATS", gateway.name.contains("NATS"))
        assertEquals(8, gateway.p2pConnectedCount)

        val peers = nodes.filter { !it.isGateway }
        assertEquals("There must be exactly 8 P2P peers", 8, peers.size)

        // Verify all 8 peers have unique IDs peer_1 through peer_8
        for (i in 1..8) {
            val peer = peers.find { it.id == "peer_$i" }
            assertNotNull("Peer $i must exist", peer)
            assertTrue("Peer latency must be positive", peer!!.latencyMs > 0)
            assertTrue("Peer truth hash must not be empty", peer.truthHash.startsWith("0x"))
            assertEquals("ONLINE", peer.status)
        }

        // Verify geographical diversity of the 8 peers
        val cities = peers.map { it.city }
        assertTrue(cities.any { it.contains("Dallas") })
        assertTrue(cities.any { it.contains("Tokyo") })
        assertTrue(cities.any { it.contains("Frankfurt") })
        assertTrue(cities.any { it.contains("Sydney") })
        assertTrue(cities.any { it.contains("São Paulo") })
        assertTrue(cities.any { it.contains("Singapore") })
        assertTrue(cities.any { it.contains("Zurich") })
        assertTrue(cities.any { it.contains("London") })
    }

    @Test
    fun `test generateD3MeshHtml contains D3 force simulation and P2P mesh topology`() {
        val html = generateD3MeshHtml()
        assertNotNull(html)
        assertTrue(html.isNotEmpty())

        // D3.js library presence & script
        assertTrue("HTML must reference D3 library", html.contains("d3.v7.min.js") || html.contains("d3"))
        assertTrue("HTML must contain forceSimulation", html.contains("d3.forceSimulation"))
        assertTrue("HTML must contain forceLink", html.contains("d3.forceLink"))
        assertTrue("HTML must contain forceManyBody", html.contains("d3.forceManyBody"))
        assertTrue("HTML must contain d3 drag handlers", html.contains("d3.drag()"))

        // Verification of 8 peers and NATS Gateway in D3 dataset
        assertTrue("HTML must declare nats_gateway node", html.contains("nats_gateway"))
        for (i in 1..8) {
            assertTrue("HTML must declare peer_$i", html.contains("peer_$i"))
        }

        // Verification of real-time particle animation and exposed window functions
        assertTrue("HTML must implement particle animations", html.contains("spawnParticle"))
        assertTrue("HTML must expose triggerGossipPulse", html.contains("window.triggerGossipPulse"))
        assertTrue("HTML must expose setMeshTopology", html.contains("window.setMeshTopology"))
        assertTrue("HTML must expose toggleNetworkPartition", html.contains("window.toggleNetworkPartition"))
        assertTrue("HTML must expose resetGraphCenter", html.contains("window.resetGraphCenter"))
        assertTrue("HTML must interface with AndroidBridge", html.contains("AndroidBridge"))
    }

    @Test
    fun `test D3MeshJsBridge safely deserializes clicked node and gossip events`() {
        var clickedPeer: PeerMeshNode? = null
        var gossipMessage: String? = null

        val bridge = D3MeshJsBridge(
            onNodeSelected = { node -> clickedPeer = node },
            onGossipStatus = { msg -> gossipMessage = msg }
        )

        val testNodeJson = """
            {
                "id": "peer_6",
                "name": "Peer #6 [Singapore]",
                "city": "Singapore, SGP",
                "region": "ASEAN-Hub (MCSFTA)",
                "latencyMs": 16,
                "truthHash": "0x11FF00DD55AA",
                "status": "ONLINE",
                "isGateway": false,
                "p2pConnectedCount": 7
            }
        """.trimIndent()

        bridge.onNodeClicked(testNodeJson)
        assertNotNull(clickedPeer)
        assertEquals("peer_6", clickedPeer?.id)
        assertEquals("Peer #6 [Singapore]", clickedPeer?.name)
        assertEquals(16, clickedPeer?.latencyMs)
        assertEquals("ONLINE", clickedPeer?.status)
        assertFalse(clickedPeer!!.isGateway)

        bridge.onGossipBroadcastEvent("TEST_GOSSIP_PULSE_FIRED")
        assertEquals("TEST_GOSSIP_PULSE_FIRED", gossipMessage)
    }
}
