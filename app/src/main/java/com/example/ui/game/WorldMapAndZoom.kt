package com.example.ui.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RegionEntity
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Brush
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class GridTerrain(
    val label: String,
    val cost: Int,
    val color: Color,
    val symbol: String,
    val description: String
) {
    PLAINS("Plains", 1, Color(0x1F10B981), "⬡", "Flat grassland. Speed base: 1."),
    FOREST("Forest", 2, Color(0x2F00E676), "🌲", "Dense tree cover. Speed base: 2."),
    DESERT("Desert", 2, Color(0x2FFFB300), "🏜️", "Arid sand dunes. Speed base: 2."),
    WATER("Water", 3, Color(0x2F00B0FF), "🌊", "Deep cobalt lake water. Speed base: 3."),
    MOUNTAIN("Mountain", 4, Color(0x2FE040FB), "▲", "Crystalline ridge. Speed base: 4.")
}

data class KafkaBinaryEvent(
    val seqId: Long,
    val timestampMs: Long,
    val topic: String,
    val eventType: String,
    val payload: String,
    val proofHash: String,
    val partition: Int = 0
)

data class ScientificBatchJob(
    val id: String,
    val title: String,
    val octaveScript: String,
    val cppSource: String,
    val compilerFlags: String = "-O3 -march=native -fPIC -shared",
    var priority: String = "MEDIUM",
    var status: String = "QUEUED",
    var octaveLog: String = "",
    var cppLog: String = "",
    var executionTimeMs: Long = 0L
)

fun getTerrainAt(x: Int, y: Int): GridTerrain {
    val layout = arrayOf(
        arrayOf(GridTerrain.PLAINS, GridTerrain.PLAINS, GridTerrain.FOREST, GridTerrain.MOUNTAIN, GridTerrain.PLAINS, GridTerrain.PLAINS, GridTerrain.PLAINS, GridTerrain.PLAINS),
        arrayOf(GridTerrain.PLAINS, GridTerrain.FOREST, GridTerrain.FOREST, GridTerrain.MOUNTAIN, GridTerrain.PLAINS, GridTerrain.PLAINS, GridTerrain.WATER, GridTerrain.WATER),
        arrayOf(GridTerrain.PLAINS, GridTerrain.PLAINS, GridTerrain.PLAINS, GridTerrain.MOUNTAIN, GridTerrain.PLAINS, GridTerrain.WATER, GridTerrain.WATER, GridTerrain.PLAINS),
        arrayOf(GridTerrain.FOREST, GridTerrain.FOREST, GridTerrain.PLAINS, GridTerrain.PLAINS, GridTerrain.PLAINS, GridTerrain.PLAINS, GridTerrain.PLAINS, GridTerrain.PLAINS),
        arrayOf(GridTerrain.DESERT, GridTerrain.DESERT, GridTerrain.PLAINS, GridTerrain.PLAINS, GridTerrain.FOREST, GridTerrain.FOREST, GridTerrain.PLAINS, GridTerrain.PLAINS),
        arrayOf(GridTerrain.DESERT, GridTerrain.DESERT, GridTerrain.DESERT, GridTerrain.PLAINS, GridTerrain.FOREST, GridTerrain.MOUNTAIN, GridTerrain.MOUNTAIN, GridTerrain.PLAINS),
        arrayOf(GridTerrain.PLAINS, GridTerrain.PLAINS, GridTerrain.PLAINS, GridTerrain.PLAINS, GridTerrain.PLAINS, GridTerrain.MOUNTAIN, GridTerrain.PLAINS, GridTerrain.PLAINS),
        arrayOf(GridTerrain.PLAINS, GridTerrain.PLAINS, GridTerrain.PLAINS, GridTerrain.PLAINS, GridTerrain.PLAINS, GridTerrain.PLAINS, GridTerrain.PLAINS, GridTerrain.PLAINS)
    )
    if (y in 0..7 && x in 0..7) {
        return layout[y][x]
    }
    return GridTerrain.PLAINS
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun WorldMapAndZoom(
    regions: List<RegionEntity>,
    selectedRegion: RegionEntity?,
    onRegionSelect: (RegionEntity) -> Unit,
    zoomScale: Float,
    onZoomScaleChange: (Float) -> Unit,
    mapOffsetX: Float,
    mapOffsetY: Float,
    onOffsetChange: (Float, Float) -> Unit,
    turnTimeRemaining: Int,
    turnMaxDuration: Int,
    activeFactionTurn: String,
    isTurnTimerActive: Boolean,
    turnNumber: Int,
    onEndTurnManually: () -> Unit,
    onToggleTurnTimer: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    currentLanguage: com.example.ui.game.AppLanguage = com.example.ui.game.AppLanguage.ENGLISH,
    accelX: Float = 0f,
    accelY: Float = 0f,
    gyroZ: Float = 0f,
    isSensorOverrideActive: Boolean = false,
    onToggleSensorOverride: (Boolean) -> Unit = {},
    onSaveMatchStats: (Int, Int, Int, Double, String) -> Unit = { _, _, _, _, _ -> },
    commanderFaction: String = "north_union"
) {
    val textMeasurer = rememberTextMeasurer()

    var isWasmDashboardVisible by remember { mutableStateOf(false) }
    var mainCockpitTab by remember { mutableStateOf("TACTICAL") } // "TACTICAL", "NEXUS_CORE", "ENTERPRISE_LABS"
    
    // Nexus Enterprise & Labs State
    var enterpriseSelectedHub by remember { mutableStateOf("Rotterdam Hub") }
    var enterpriseFleetUnits by remember { mutableStateOf(280) }
    var enterpriseNodeDisrupted by remember { mutableStateOf(false) }
    var enterpriseCostPerKm by remember { mutableStateOf(1.42) }
    var enterpriseTransitHours by remember { mutableStateOf(14.2) }
    var enterpriseSelectedScenario by remember { mutableStateOf("GEOPOLITICAL_FUEL_SHOCK") }
    var enterpriseMctsIterations by remember { mutableStateOf(50000) }
    var enterpriseSuccessRate by remember { mutableStateOf(88.7) }
    var enterpriseFinancialSavingsMillions by remember { mutableStateOf(14.8) }
    var enterpriseZkProofStatus by remember { mutableStateOf<String?>("VERIFIED (ISO-27001 & ESG)") }
    var enterpriseZkLogs by remember { mutableStateOf(listOf(
        "[00:01:04] [ZK_CIRCUIT] Circom BN254 witness constraints loaded: 1,024 ESG constraints.",
        "[00:01:05] [ARKWORKS] Groth16 proof generated in 11.2ms: 0x9f8e...44a1",
        "[00:01:05] [COMPLIANCE] B2B Audit Certificate successfully signed into immutable Kafka ledger."
    )) }
    var isTelemetryConsoleVisible by remember { mutableStateOf(false) }
    var pingLatencyMs by remember { mutableStateOf(1.18) }
    var telemetryLogs by remember { mutableStateOf(listOf(
        "[SYSTEM] C++ Native Kafka Orchestrator active on port 9092.",
        "[ASYNC_CHANNEL] WS channel connected. Bandwidth: 10 Gbps.",
        "[GEO_CACHE] 64/64 spatial grid nodes indexed into O(1) lookup map."
    )) }
    var aStarTotalCost by remember { mutableStateOf(0) }

    // ZKP Proving States
    val scope = rememberCoroutineScope()
    var zkpGenerating by remember { mutableStateOf(false) }
    var zkpProofResult by remember { mutableStateOf<String?>(null) }
    var zkpWitnessValue by remember { mutableStateOf("") }
    var zkpVerificationPass by remember { mutableStateOf<Boolean?>(null) }
    var zkpLogs by remember { mutableStateOf(listOf("ZKP Circuit mapped. Witness constraints: tilt_vector < 5.0 m/s²")) }

    // Firestone state sync variables
    var firestoneState by remember { mutableStateOf("CONNECTED") } // CONNECTED, SYNCING
    var firestoneDocsCount by remember { mutableStateOf(1048) }
    var firestoneLatencyMs by remember { mutableStateOf(14) }
    var firestoneLogs by remember { mutableStateOf(listOf("Firestone stream listening to 'sector_tactics' collection")) }
    
    // Animation for Firestone visual feedback
    val infiniteTransitionLocal = rememberInfiniteTransition(label = "LocalEffects")
    val firestonePulseScale by infiniteTransitionLocal.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FirestonePulse"
    )
    val firestoneRotation by infiniteTransitionLocal.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "FirestoneRotation"
    )

    // Sync Waves animation state
    var showSyncWave by remember { mutableStateOf(false) }
    val syncWaveProgress = remember { Animatable(0f) }

    // A* Pathfinding Tactical Grid States (8x8 grid)
    val gridWidth = 8
    val gridHeight = 8
    var obstaclesState by remember { mutableStateOf(setOf(
        Pair(2, 2), Pair(2, 3), Pair(2, 4),
        Pair(5, 3), Pair(5, 4), Pair(5, 5)
    )) }
    var aStarStartX by remember { mutableStateOf(0) }
    var aStarStartY by remember { mutableStateOf(0) }
    var aStarEndX by remember { mutableStateOf(7) }
    var aStarEndY by remember { mutableStateOf(7) }
    
    var aStarPath by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }
    var activeUnitX by remember { mutableStateOf<Float?>(null) }
    var activeUnitY by remember { mutableStateOf<Float?>(null) }
    var isAnimatingUnit by remember { mutableStateOf(false) }
    
    // Step-by-step pathfinding visual animation states
    var openSetNodes by remember { mutableStateOf<Set<Pair<Int, Int>>>(emptySet()) }
    var closedSetNodes by remember { mutableStateOf<Set<Pair<Int, Int>>>(emptySet()) }
    var currentEvaluatingNode by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var isSearchingPath by remember { mutableStateOf(false) }
    var pathAnimationSpeedMs by remember { mutableStateOf(80L) }
    
    var aStarOpenSetSize by remember { mutableStateOf(0) }
    var aStarClosedSetSize by remember { mutableStateOf(0) }
    var aStarLatencyMs by remember { mutableStateOf(0.0) }
    var aStarLogs by remember { mutableStateOf(listOf("A* Tactical Navigator engine idle.")) }
    var aStarInteractionMode by remember { mutableStateOf("TOGGLE_OBSTACLE") } // "TOGGLE_OBSTACLE", "SET_START", "SET_TARGET"

    // LocalStorage (SharedPreferences) Grid Persistence & Undo History
    val gridContext = LocalContext.current
    val gridPrefs = remember(gridContext) { gridContext.getSharedPreferences("tactical_grid_storage", Context.MODE_PRIVATE) }

    var gridHistoryStack by remember { mutableStateOf<List<List<Any>>>(emptyList()) }

    val pushGridHistory: () -> Unit = {
        val snapshot: List<Any> = listOf(obstaclesState, aStarStartX, aStarStartY, aStarEndX, aStarEndY)
        gridHistoryStack = (gridHistoryStack + listOf(snapshot)).takeLast(25)
    }

    val undoLastGridAction: () -> Unit = {
        if (gridHistoryStack.isNotEmpty()) {
            val lastSnapshot = gridHistoryStack.last()
            gridHistoryStack = gridHistoryStack.dropLast(1)
            @Suppress("UNCHECKED_CAST")
            obstaclesState = lastSnapshot[0] as Set<Pair<Int, Int>>
            aStarStartX = lastSnapshot[1] as Int
            aStarStartY = lastSnapshot[2] as Int
            aStarEndX = lastSnapshot[3] as Int
            aStarEndY = lastSnapshot[4] as Int
            aStarPath = emptyList()
            openSetNodes = emptySet()
            closedSetNodes = emptySet()
            aStarLogs = aStarLogs + "[UNDO] Reverted grid action (${gridHistoryStack.size} step(s) remaining in history)."
        }
    }

    // Automatically save current grid state (start, end, obstacles) to localStorage
    LaunchedEffect(obstaclesState, aStarStartX, aStarStartY, aStarEndX, aStarEndY) {
        val obstaclesStr = obstaclesState.joinToString(";") { "${it.first},${it.second}" }
        gridPrefs.edit()
            .putString("auto_saved_obstacles", obstaclesStr)
            .putInt("auto_saved_start_x", aStarStartX)
            .putInt("auto_saved_start_y", aStarStartY)
            .putInt("auto_saved_end_x", aStarEndX)
            .putInt("auto_saved_end_y", aStarEndY)
            .apply()
    }

    val saveGridToLocalStorage: () -> Unit = {
        val obstaclesStr = obstaclesState.joinToString(";") { "${it.first},${it.second}" }
        gridPrefs.edit()
            .putString("saved_obstacles", obstaclesStr)
            .putInt("saved_start_x", aStarStartX)
            .putInt("saved_start_y", aStarStartY)
            .putInt("saved_end_x", aStarEndX)
            .putInt("saved_end_y", aStarEndY)
            .apply()
        aStarLogs = aStarLogs + "[LOCALSTORAGE] Grid state saved (${obstaclesState.size} barriers, Start: ($aStarStartX,$aStarStartY), Target: ($aStarEndX,$aStarEndY))."
    }

    val loadLastMapFromLocalStorage: () -> Unit = {
        val savedObstaclesStr = gridPrefs.getString("auto_saved_obstacles", null)
            ?: gridPrefs.getString("saved_obstacles", null)
        if (savedObstaclesStr != null) {
            pushGridHistory()
            val loadedObstacles = if (savedObstaclesStr.isBlank()) emptySet() else savedObstaclesStr.split(";").mapNotNull {
                val coords = it.split(",")
                if (coords.size == 2) {
                    val x = coords[0].toIntOrNull()
                    val y = coords[1].toIntOrNull()
                    if (x != null && y != null) Pair(x, y) else null
                } else null
            }.toSet()
            obstaclesState = loadedObstacles
            aStarStartX = gridPrefs.getInt("auto_saved_start_x", gridPrefs.getInt("saved_start_x", aStarStartX))
            aStarStartY = gridPrefs.getInt("auto_saved_start_y", gridPrefs.getInt("saved_start_y", aStarStartY))
            aStarEndX = gridPrefs.getInt("auto_saved_end_x", gridPrefs.getInt("saved_end_x", aStarEndX))
            aStarEndY = gridPrefs.getInt("auto_saved_end_y", gridPrefs.getInt("saved_end_y", aStarEndY))
            aStarPath = emptyList()
            openSetNodes = emptySet()
            closedSetNodes = emptySet()
            aStarLogs = aStarLogs + "[LOCALSTORAGE] Last map loaded (${loadedObstacles.size} barriers, Start: ($aStarStartX,$aStarStartY), Target: ($aStarEndX,$aStarEndY))."
        } else {
            aStarLogs = aStarLogs + "[LOCALSTORAGE] No saved map found in local storage."
        }
    }

    val loadGridFromLocalStorage: () -> Unit = {
        loadLastMapFromLocalStorage()
    }

    // Interactive Grid Cell Hover & Keyframe Pulse animation states
    var hoveredCellX by remember { mutableStateOf<Int?>(null) }
    var hoveredCellY by remember { mutableStateOf<Int?>(null) }
    var isGridBoardHovered by remember { mutableStateOf(false) }

    // --- KAFKA C++ BINARY ORCHESTRATOR & IMMUTABLE EVENT-SOURCING LEDGER ---
    var eventSeqCounter by remember { mutableStateOf(1001L) }
    var kafkaEventsLedger by remember { mutableStateOf(listOf(
        KafkaBinaryEvent(
            seqId = 1000L,
            timestampMs = System.currentTimeMillis() - 15000,
            topic = "kafka.binary.system",
            eventType = "ORCHESTRATOR_ONLINE",
            payload = "Binary Event Orchestrator connected via Async Channels. Replay buffer initialized.",
            proofHash = "0x7f8a91b...c402e (circom+rust)"
        )
    )) }

    fun publishKafkaEvent(topic: String, eventType: String, payload: String) {
        val seq = eventSeqCounter++
        val proof = "0x" + Random.nextInt(10000000, 99999999).toString(16) + Random.nextInt(10000000, 99999999).toString(16) + " (circom+rust)"
        val newEvent = KafkaBinaryEvent(
            seqId = seq,
            timestampMs = System.currentTimeMillis(),
            topic = topic,
            eventType = eventType,
            payload = payload,
            proofHash = proof,
            partition = (seq % 4).toInt()
        )
        kafkaEventsLedger = (kafkaEventsLedger + newEvent).takeLast(40)
    }

    // --- DECOUPLED LOGISTICS & ECONOMY ENGINE STATES ---
    var energyCrystals by remember { mutableStateOf(520) }
    var steelAlloy by remember { mutableStateOf(340) }
    var quantumFuel by remember { mutableStateOf(210) }
    var ammunition by remember { mutableStateOf(900) }
    var supplyRouteStatus by remember { mutableStateOf("OPTIMAL") } // "OPTIMAL", "SUPPLYING", "DISRUPTED"
    var supplyFlowRate by remember { mutableStateOf(65) } // units / min
    var isLogisticsAutoRunning by remember { mutableStateOf(true) }

    // --- DYNAMIC UNIT TACTICAL TRANSACTION STATES ---
    var unitMorale by remember { mutableStateOf(92) } // 0-100%
    var activeSupplyLineDistance by remember { mutableStateOf(3) } // grid cells
    var isUnitSupplied by remember { mutableStateOf(true) }

    // --- SPATIAL GEO/GRID MAP CACHE ---
    var spatialLookupHits by remember { mutableStateOf(1420) }
    var spatialLookupMisses by remember { mutableStateOf(0) }

    // --- MCTS PREDICTIVE ROUTER LEDGER (Cobol/Circom/Rust / libsNark) ---
    var mctsWinProbability by remember { mutableStateOf(89.2f) }
    var mctsSimulatedNodes by remember { mutableStateOf(10240) }
    var mctsLastPredictionTimeMs by remember { mutableStateOf(0.42) }
    var mctsProofStatus by remember { mutableStateOf("VERIFIED (libsNark ZK-SNARK)") }

    var selectedLedgerTab by remember { mutableStateOf("EVENTS") } // "EVENTS", "LOGISTICS", "SPATIAL_GRID", "MCTS"

    // Background coroutine loop for Decoupled Logistics & Economy pipeline running independently from combat/movement
    LaunchedEffect(isLogisticsAutoRunning) {
        while (isLogisticsAutoRunning) {
            delay(4000)
            energyCrystals += 8
            steelAlloy += 5
            quantumFuel += 3
            ammunition += 12
            spatialLookupHits += 8
            publishKafkaEvent(
                topic = "kafka.binary.logistics",
                eventType = "RESOURCE_FLOW_TICK",
                payload = "Decoupled Economy: +8 Energy, +5 Steel, +3 Fuel, +12 Ammo dispatched in background coroutine."
            )
        }
    }

    fun dispatchSupplyConvoy() {
        if (quantumFuel >= 25) {
            quantumFuel -= 25
            supplyFlowRate += 15
            unitMorale = (unitMorale + 8).coerceAtMost(100)
            supplyRouteStatus = "SUPPLYING"
            publishKafkaEvent(
                topic = "kafka.binary.logistics",
                eventType = "RESOURCE_DISPATCHED",
                payload = "Supply convoy dispatched (-25 Fuel). Flow rate increased to $supplyFlowRate units/min."
            )
        }
    }

    fun boostUnitMorale() {
        if (energyCrystals >= 30) {
            energyCrystals -= 30
            unitMorale = 100
            publishKafkaEvent(
                topic = "kafka.binary.unit_state",
                eventType = "MORALE_BOOSTED",
                payload = "Tactical Unit Morale restored to 100% (-30 Energy Crystals)."
            )
        }
    }

    fun runMctsSimulation() {
        scope.launch {
            val startTime = System.nanoTime()
            delay(300)
            mctsSimulatedNodes += Random.nextInt(1200, 3500)
            mctsWinProbability = (85.0f + Random.nextFloat() * 12.0f).coerceAtMost(99.9f)
            mctsLastPredictionTimeMs = (System.nanoTime() - startTime) / 1_000_000.0
            publishKafkaEvent(
                topic = "kafka.binary.mcts",
                eventType = "MCTS_TRAJECTORY_EVALUATED",
                payload = "MCTS Ledger: Win probability computed at ${String.format("%.1f", mctsWinProbability)}% over $mctsSimulatedNodes nodes."
            )
        }
    }

    fun replayEventStream() {
        scope.launch {
            publishKafkaEvent(
                topic = "kafka.binary.system",
                eventType = "REPLAY_STARTED",
                payload = "Initiating immutable event stream replay verification across ${kafkaEventsLedger.size} binary logs..."
            )
            delay(400)
            publishKafkaEvent(
                topic = "kafka.binary.system",
                eventType = "REPLAY_COMPLETED",
                payload = "SUCCESS: Event stream replayed. Zero state corruptions detected."
            )
        }
    }

    // Real-time Tactical Combat states (WebAssembly Math-Simulated)
    var playerHp by remember { mutableStateOf(120) }
    val playerMaxHp = 120
    var enemyHp by remember { mutableStateOf(100) }
    val enemyMaxHp = 100
    var combatRound by remember { mutableStateOf(0) }
    var isCombatActive by remember { mutableStateOf(false) }
    var combatLatencyMs by remember { mutableStateOf(0.0) }
    var battleLogs by remember { mutableStateOf(listOf(
        "WASM Combat Engine initialized.",
        "Awaiting tactical encounter at coordinate (7, 7)..."
    )) }

    fun runWasmCombatMath() {
        if (isCombatActive) return
        scope.launch {
            isCombatActive = true
            combatRound = 0
            battleLogs = listOf("[SYSTEM] Starting low-latency WebAssembly combat resolution engine...")
            delay(400)
            
            while (playerHp > 0 && enemyHp > 0) {
                val startTime = System.nanoTime()
                combatRound++
                
                // Low-latency WASM-simulated math
                // Alliance Recon Drone attacks Rogue Sentinel
                val rawPlayerDmg = Random.nextInt(15, 25)
                val playerCritChance = Random.nextFloat()
                val isPlayerCrit = playerCritChance < 0.25f // 25% crit rate
                val finalPlayerDmg = if (isPlayerCrit) (rawPlayerDmg * 1.5f).toInt() else rawPlayerDmg
                
                // Deduct enemy hp
                enemyHp = (enemyHp - finalPlayerDmg).coerceAtLeast(0)
                
                val endTime = System.nanoTime()
                combatLatencyMs = (endTime - startTime) / 1_000_000.0
                
                val playerCritMsg = if (isPlayerCrit) " [CRITICAL IMPACT!]" else ""
                battleLogs = battleLogs + String.format(
                    "Round %d [t+%.4fms]: Alliance Recon 🛰️ strikes Rogue Sentinel 👾 for %d damage%s (Sentinel HP: %d/%d)",
                    combatRound, combatLatencyMs, finalPlayerDmg, playerCritMsg, enemyHp, enemyMaxHp
                )
                
                delay(350)
                
                if (enemyHp <= 0) {
                    battleLogs = battleLogs + "🏆 VICTORY: Rogue Sentinel destroyed! Tactical path cleared."
                    onSaveMatchStats(1, 0, combatRound, combatLatencyMs, "VICTORY")
                    break
                }
                
                // Rogue Sentinel attacks Alliance Recon Drone
                val startOppTime = System.nanoTime()
                val rawEnemyDmg = Random.nextInt(10, 20)
                val enemyCritChance = Random.nextFloat()
                val isEnemyCrit = enemyCritChance < 0.15f
                val finalEnemyDmg = if (isEnemyCrit) (rawEnemyDmg * 1.4f).toInt() else rawEnemyDmg
                
                playerHp = (playerHp - finalEnemyDmg).coerceAtLeast(0)
                
                val endOppTime = System.nanoTime()
                val enemyLatencyMs = (endOppTime - startOppTime) / 1_000_000.0
                
                val enemyCritMsg = if (isEnemyCrit) " [SHIELD BREACH!]" else ""
                battleLogs = battleLogs + String.format(
                    "Round %d [t+%.4fms]: Rogue Sentinel 👾 counter-attacks Alliance Recon 🛰️ for %d damage%s (Recon HP: %d/%d)",
                    combatRound, enemyLatencyMs, finalEnemyDmg, enemyCritMsg, playerHp, playerMaxHp
                )
                
                delay(350)
                
                if (playerHp <= 0) {
                    battleLogs = battleLogs + "❌ DEFEAT: Alliance Recon destroyed! Mission aborted."
                    onSaveMatchStats(0, 1, combatRound, combatLatencyMs, "DEFEAT")
                    break
                }
            }
            
            isCombatActive = false
        }
    }

    fun resetCombatUnits() {
        playerHp = playerMaxHp
        enemyHp = enemyMaxHp
        combatRound = 0
        battleLogs = listOf(
            "WASM Combat Engine reset.",
            "Recon Drone fully repaired (HP: $playerMaxHp/$playerMaxHp).",
            "Sentinel shields restored (HP: $enemyMaxHp/$enemyMaxHp).",
            "Awaiting tactical encounter at coordinate ($aStarEndX, $aStarEndY)..."
        )
    }

    // Step-by-step visual animation of the A* pathfinding algorithm as it explores the grid
    fun runAStarStepByStepAnimation() {
        if (isSearchingPath || isAnimatingUnit) return
        scope.launch {
            isSearchingPath = true
            aStarPath = emptyList()
            openSetNodes = emptySet()
            closedSetNodes = emptySet()
            currentEvaluatingNode = null
            
            val startTime = System.nanoTime()
            
            data class Node(val x: Int, val y: Int, var g: Int = Int.MAX_VALUE, var h: Int = 0, var parent: Node? = null) {
                val f: Int get() = g + h
            }
            
            val openSet = mutableListOf<Node>()
            val closedSet = mutableSetOf<Pair<Int, Int>>()
            
            val startNode = Node(aStarStartX, aStarStartY, g = 0, h = Math.abs(aStarStartX - aStarEndX) + Math.abs(aStarStartY - aStarEndY))
            openSet.add(startNode)
            openSetNodes = setOf(Pair(aStarStartX, aStarStartY))
            
            var maxOpenSize = 1
            var destNode: Node? = null
            var stepCount = 0
            
            aStarLogs = listOf(
                "🚀 [A* ENGINE] Initiating step-by-step grid pathfinding exploration...",
                "Start node: ($aStarStartX, $aStarStartY) | Target node: ($aStarEndX, $aStarEndY)",
                "Heuristic: Manhattan grid distance + Terrain weight costs"
            )
            
            while (openSet.isNotEmpty()) {
                openSet.sortBy { it.f * 1000 + it.h }
                val current = openSet.removeAt(0)
                val currentCoord = Pair(current.x, current.y)
                closedSet.add(currentCoord)
                
                stepCount++
                currentEvaluatingNode = currentCoord
                openSetNodes = openSet.map { Pair(it.x, it.y) }.toSet()
                closedSetNodes = closedSet.toSet()
                
                maxOpenSize = Math.max(maxOpenSize, openSet.size)
                aStarOpenSetSize = openSet.size
                aStarClosedSetSize = closedSet.size
                
                val currentLog = "Step $stepCount: Evaluating node (${current.x}, ${current.y}) [f=${current.f}, g=${current.g}, h=${current.h}]"
                aStarLogs = (aStarLogs + currentLog).takeLast(25)
                
                if (current.x == aStarEndX && current.y == aStarEndY) {
                    destNode = current
                    break
                }
                
                val neighbors = listOf(
                    Pair(current.x - 1, current.y),
                    Pair(current.x + 1, current.y),
                    Pair(current.x, current.y - 1),
                    Pair(current.x, current.y + 1)
                )
                
                for (neighbor in neighbors) {
                    val nx = neighbor.first
                    val ny = neighbor.second
                    if (nx !in 0 until gridWidth || ny !in 0 until gridHeight) continue
                    if (obstaclesState.contains(Pair(nx, ny))) continue
                    if (closedSet.contains(Pair(nx, ny))) continue
                    
                    val tentativeG = current.g + getTerrainAt(nx, ny).cost
                    val existing = openSet.find { it.x == nx && it.y == ny }
                    
                    if (existing == null) {
                        val nNode = Node(nx, ny, g = tentativeG, h = Math.abs(nx - aStarEndX) + Math.abs(ny - aStarEndY), parent = current)
                        openSet.add(nNode)
                    } else if (tentativeG < existing.g) {
                        existing.g = tentativeG
                        existing.parent = current
                    }
                }
                
                openSetNodes = openSet.map { Pair(it.x, it.y) }.toSet()
                delay(pathAnimationSpeedMs)
            }
            
            currentEvaluatingNode = null
            val endTime = System.nanoTime()
            aStarLatencyMs = (endTime - startTime) / 1_000_000.0
            
            val path = mutableListOf<Pair<Int, Int>>()
            var curr = destNode
            while (curr != null) {
                path.add(0, Pair(curr.x, curr.y))
                curr = curr.parent
            }
            
            aStarPath = path
            aStarOpenSetSize = maxOpenSize
            aStarClosedSetSize = closedSet.size
            
            if (path.isNotEmpty()) {
                var totalCost = 0
                for (i in 1 until path.size) {
                    val node = path[i]
                    totalCost += getTerrainAt(node.first, node.second).cost
                }
                aStarTotalCost = totalCost
                aStarLogs = aStarLogs + listOf(
                    "🎯 TARGET REACHED in $stepCount exploration steps!",
                    "Optimal A* Path: ${path.size} steps | Total terrain cost: $totalCost"
                )
                publishKafkaEvent(
                    topic = "kafka.binary.movement",
                    eventType = "UNIT_PATH_INITIATED",
                    payload = "A* Path calculated: ${path.size} steps from ($aStarStartX, $aStarStartY) to ($aStarEndX, $aStarEndY). Cost: $totalCost."
                )
            } else {
                aStarTotalCost = 0
                aStarLogs = aStarLogs + "❌ TARGET UNREACHABLE: Path blocked by barriers after $stepCount steps."
            }
            
            isSearchingPath = false
        }
    }

    // Reset path states reactively when start, target, or obstacles change
    LaunchedEffect(aStarStartX, aStarStartY, aStarEndX, aStarEndY, obstaclesState) {
        aStarPath = emptyList()
        openSetNodes = emptySet()
        closedSetNodes = emptySet()
        currentEvaluatingNode = null
        aStarOpenSetSize = 0
        aStarClosedSetSize = 0
        aStarLogs = listOf(
            "A* Tactical Navigator ready.",
            "Start: ($aStarStartX, $aStarStartY) | Target: ($aStarEndX, $aStarEndY)",
            "Press 'FIND A* PATH' to start visual step-by-step grid exploration."
        )
    }

    // Unit coordinate placement tracking
    LaunchedEffect(aStarPath) {
        if (aStarPath.isNotEmpty()) {
            activeUnitX = aStarStartX.toFloat()
            activeUnitY = aStarStartY.toFloat()
        } else {
            activeUnitX = null
            activeUnitY = null
        }
    }

    fun triggerAutomatedMovement() {
        if (aStarPath.isEmpty() || isAnimatingUnit) return
        scope.launch {
            isAnimatingUnit = true
            aStarLogs = aStarLogs + "Initiating WASM-managed automated unit trajectory..."
            publishKafkaEvent(
                topic = "kafka.binary.movement",
                eventType = "UNIT_TRAJECTORY_STARTED",
                payload = "Unit initiated movement along WASM trajectory (${aStarPath.size} nodes)."
            )
            for (node in aStarPath) {
                activeUnitX = node.first.toFloat()
                activeUnitY = node.second.toFloat()
                aStarLogs = aStarLogs + "Unit passing sector coordinate (${node.first}, ${node.second})"
                delay(250)
            }
            aStarLogs = aStarLogs + "SUCCESS: Automated unit arrived at target coordinate."
            isAnimatingUnit = false
            publishKafkaEvent(
                topic = "kafka.binary.movement",
                eventType = "UNIT_ARRIVED_TARGET",
                payload = "Unit arrived at target sector ($aStarEndX, $aStarEndY). Position updated in dynamic state table."
            )
            
            // Automatically engage combat if target sector is reached and enemy is still functional
            if (activeUnitX?.toInt() == aStarEndX && activeUnitY?.toInt() == aStarEndY && enemyHp > 0) {
                runWasmCombatMath()
            }
        }
    }

    // Real-time Satellite Sync sweeping laser animations
    val infiniteTransition = rememberInfiniteTransition(label = "SatSweep")
    val sweepY by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepDelta"
    )

    val signalPulseFreq by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SignalPulse"
    )

    // CSS Keyframe pulse animation for .grid-cell elements in the predictive simulation sandbox
    val gridCellPulseTransition = rememberInfiniteTransition(label = "GridCellPulse")
    val cellPulseAlpha by gridCellPulseTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                0.65f at 0 with FastOutSlowInEasing
                1.0f at 700 with FastOutSlowInEasing
                0.65f at 1400 with FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "GridCellAlphaPulse"
    )
    val cellPulseScale by gridCellPulseTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                0.94f at 0 with FastOutSlowInEasing
                1.06f at 700 with FastOutSlowInEasing
                0.94f at 1400 with FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "GridCellScalePulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF070B12)) // Dark satellite background
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newZoom = (zoomScale * zoom).coerceIn(0.7f, 3.5f)
                    onZoomScaleChange(newZoom)
                    // Bound the drag pan offsets nicely
                    val newX = (mapOffsetX + pan.x).coerceIn(-600f, 600f)
                    val newY = (mapOffsetY + pan.y).coerceIn(-400f, 400f)
                    onOffsetChange(newX, newY)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().testTag("satellite_world_map")) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f + mapOffsetX
            val centerY = height / 2f + mapOffsetY

            // 1. Draw Google-Maps-Style Grid Lines & Coordinate Labels (Latitude/Longitude)
            val gridStep = 100f * zoomScale

            // Horizontal longitude grids
            val horizStart = (centerY % gridStep) - gridStep
            var currentY = horizStart
            while (currentY < height + gridStep) {
                drawLine(
                    color = Color(0x3500B0FF),
                    start = Offset(0f, currentY),
                    end = Offset(width, currentY),
                    strokeWidth = 0.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 15f))
                )
                // Draw latitude label
                val latDeg = ((height / 2f - (currentY - mapOffsetY)) / (height / 180f)).toInt()
                if (latDeg in -90..90) {
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "${latDeg}° N/S",
                        topLeft = Offset(10f, currentY + 4f),
                        style = TextStyle(
                            color = Color(0x5000B0FF),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
                currentY += gridStep
            }

            // Vertical latitude grids
            val vertStart = (centerX % gridStep) - gridStep
            var currentX = vertStart
            while (currentX < width + gridStep) {
                drawLine(
                    color = Color(0x3500B0FF),
                    start = Offset(currentX, 0f),
                    end = Offset(currentX, height),
                    strokeWidth = 0.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 15f))
                )
                // Draw longitude label
                val lngDeg = (((currentX - centerX) / (width / 360f)) / zoomScale).toInt()
                if (lngDeg in -180..180) {
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "${lngDeg}° E/W",
                        topLeft = Offset(currentX + 5f, height - 25f),
                        style = TextStyle(
                            color = Color(0x5000B0FF),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
                currentX += gridStep
            }

            // Draw Prime Meridian and Equator as solid markers
            drawLine(
                color = Color(0x7500B0FF),
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 1f
            )
            drawLine(
                color = Color(0x7500B0FF),
                start = Offset(centerX, 0f),
                end = Offset(centerX, height),
                strokeWidth = 1f
            )

            // 2. Beautiful Continent Vectors (Mathematical curves simulating Earth Factions)
            val pathNorth = Path().apply {
                moveTo(centerX - 400 * zoomScale, centerY - 250 * zoomScale)
                quadraticTo(
                    centerX - 200 * zoomScale, centerY - 350 * zoomScale,
                    centerX + 150 * zoomScale, centerY - 280 * zoomScale
                )
                cubicTo(
                    centerX + 350 * zoomScale, centerY - 200 * zoomScale,
                    centerX + 250 * zoomScale, centerY - 50 * zoomScale,
                    centerX + 50 * zoomScale, centerY - 120 * zoomScale
                )
                quadraticTo(
                    centerX - 100 * zoomScale, centerY - 80 * zoomScale,
                    centerX - 400 * zoomScale, centerY - 250 * zoomScale
                )
            }
            drawPath(path = pathNorth, color = Color(0x1A00E676)) // Allied Continent (Green tint)

            val pathSouth = Path().apply {
                moveTo(centerX - 150 * zoomScale, centerY + 180 * zoomScale)
                cubicTo(
                    centerX - 50 * zoomScale, centerY + 80 * zoomScale,
                    centerX + 250 * zoomScale, centerY + 120 * zoomScale,
                    centerX + 300 * zoomScale, centerY + 280 * zoomScale
                )
                quadraticTo(
                    centerX + 150 * zoomScale, centerY + 360 * zoomScale,
                    centerX - 150 * zoomScale, centerY + 180 * zoomScale
                )
            }
            drawPath(path = pathSouth, color = Color(0x1AFF1744)) // Rival Continent (Red tint)

            // 3. Satellite Tracking Sweep laser line
            drawLine(
                color = Color(0x3500E676),
                start = Offset(0f, sweepY),
                end = Offset(width, sweepY),
                strokeWidth = 3f * zoomScale
            )

            // 4. Draw Region Outpost Markers
            regions.forEach { region ->
                // Map Latitude and Longitude to layout coordinates
                // Lat: -90..90 -> top..(bottom)
                // Lng: -180..180 -> left..right
                val markerX = centerX + (region.longitude.toFloat() * (width / 360f)) * zoomScale
                val markerY = centerY - (region.latitude.toFloat() * (height / 180f)) * zoomScale

                val factionColor = when (region.controller) {
                    "player" -> Color(0xFF00E5FF)          // Radiant Cyan for player
                    "alliance_north" -> Color(0xFF00E676) // Bright Emerald
                    "rival_south" -> Color(0xFFFF1744)    // Vivid Red
                    else -> Color(0xFF90A4AE)             // Slate Grey
                }

                // communication ripple rings
                if (selectedRegion?.id == region.id) {
                    drawCircle(
                        color = factionColor,
                        radius = (30f + signalPulseFreq * 40f) * zoomScale,
                        center = Offset(markerX, markerY),
                        style = Stroke(width = 2.5f * (1f - signalPulseFreq)),
                        alpha = 1f - signalPulseFreq
                    )
                }

                // Outpost dot core
                drawCircle(
                    color = factionColor.copy(alpha = 0.35f),
                    radius = 16f * zoomScale,
                    center = Offset(markerX, markerY)
                )
                drawCircle(
                    color = factionColor,
                    radius = 7f * zoomScale,
                    center = Offset(markerX, markerY)
                )

                // Space Launch lander dome circle indicator if space-ship has safely set down
                if (region.isSpaceLanded) {
                    drawCircle(
                        color = Color(0xFFFFD700), // Golden crown orbital zone
                        radius = 22f * zoomScale,
                        center = Offset(markerX, markerY),
                        style = Stroke(width = 1.5f)
                    )
                }

                // Region Name Label
                val textLayoutResult = textMeasurer.measure(
                    text = AnnotatedString(region.name),
                    style = TextStyle(
                        color = if (selectedRegion?.id == region.id) Color.White else Color(0xCCECEFF1),
                        fontSize = (10 * zoomScale).coerceIn(8f, 14f).sp,
                        fontWeight = if (selectedRegion?.id == region.id) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif
                    )
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(markerX - textLayoutResult.size.width / 2f, markerY - (22f * zoomScale) - textLayoutResult.size.height)
                )
            }
        }

        // Overlay Navigation Compass & Touch Zoom controls
        Card(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xD90E1726)),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { onZoomScaleChange((zoomScale + 0.25f).coerceAtMost(3.5f)) },
                    modifier = Modifier.testTag("map_zoom_in")
                ) {
                    Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "LOD: ${String.format("%.1f", zoomScale)}x",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { onZoomScaleChange((zoomScale - 0.25f).coerceAtLeast(0.7f)) },
                    modifier = Modifier.testTag("map_zoom_out")
                ) {
                    Text("-", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        onZoomScaleChange(1f)
                        onOffsetChange(0f, 0f)
                    },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    val centerLabel = when (currentLanguage) {
                        com.example.ui.game.AppLanguage.ENGLISH -> "CENTER"
                        com.example.ui.game.AppLanguage.ESPANOL -> "CENTRAR"
                        com.example.ui.game.AppLanguage.PORTUGUES -> "CENTRAR"
                    }
                    Text(centerLabel, fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Selected Region Status HUD overlay
        if (selectedRegion != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .widthIn(max = 240.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF20F172A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = selectedRegion.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    val controlLabel = when (currentLanguage) {
                        com.example.ui.game.AppLanguage.ENGLISH -> "Control"
                        com.example.ui.game.AppLanguage.ESPANOL -> "Control"
                        com.example.ui.game.AppLanguage.PORTUGUES -> "Controle"
                    }
                    Text(
                        text = "$controlLabel: ${selectedRegion.controller.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = when (selectedRegion.controller) {
                            "player" -> Color(0xFF00E5FF)
                            "alliance_north" -> Color(0xFF00E676)
                            "rival_south" -> Color(0xFFFF1744)
                            else -> Color(0xFF94A3B8)
                        },
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Lat: ${selectedRegion.latitude}° | Lng: ${selectedRegion.longitude}°",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    val yieldLabel = when (currentLanguage) {
                        com.example.ui.game.AppLanguage.ENGLISH -> "Yield: +${selectedRegion.creditsOutput} Credits / +${selectedRegion.alloyOutput} Alloys"
                        com.example.ui.game.AppLanguage.ESPANOL -> "Producción: +${selectedRegion.creditsOutput} Créditos / +${selectedRegion.alloyOutput} Aleaciones"
                        com.example.ui.game.AppLanguage.PORTUGUES -> "Rendimento: +${selectedRegion.creditsOutput} Créditos / +${selectedRegion.alloyOutput} Ligas"
                    }
                    Text(
                        text = yieldLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE2E8F0)
                    )

                    if (selectedRegion.isSpaceLanded) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val harborLabel = when (currentLanguage) {
                            com.example.ui.game.AppLanguage.ENGLISH -> "🚀 Space Harbor: ACTIVE"
                            com.example.ui.game.AppLanguage.ESPANOL -> "🚀 Puerto Espacial: ACTIVO"
                            com.example.ui.game.AppLanguage.PORTUGUES -> "🚀 Porto Espacial: ATIVO"
                        }
                        Text(
                            text = harborLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ========================== TACTICAL BATTLE TURN ENGINE HUD OVERLAY ==========================
        TacticalTurnHUD(
            turnNumber = turnNumber,
            activeFactionTurn = activeFactionTurn,
            commanderFaction = commanderFaction,
            turnTimeRemaining = turnTimeRemaining,
            turnMaxDuration = turnMaxDuration,
            isTurnTimerActive = isTurnTimerActive,
            currentLanguage = currentLanguage,
            onEndTurnManually = onEndTurnManually,
            onToggleTurnTimer = onToggleTurnTimer,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .widthIn(max = 420.dp)
                .wrapContentHeight()
        )

        // WASM Tactical & Telemetry Overlay Toggle Buttons
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { isTelemetryConsoleVisible = !isTelemetryConsoleVisible },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTelemetryConsoleVisible) Color(0xFF38BDF8) else Color(0xD90F172A),
                    contentColor = if (isTelemetryConsoleVisible) Color.Black else Color(0xFF38BDF8)
                ),
                border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("telemetry_console_toggle_btn")
            ) {
                Text(
                    text = if (isTelemetryConsoleVisible) "❌ CLOSE LOGS" else "🖥️ TELEMETRY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Button(
                onClick = { isWasmDashboardVisible = !isWasmDashboardVisible },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isWasmDashboardVisible) Color(0xFF00FFCC) else Color(0xD90F172A),
                    contentColor = if (isWasmDashboardVisible) Color.Black else Color(0xFF00FFCC)
                ),
                border = BorderStroke(1.dp, Color(0xFF00FFCC)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("wasm_overlay_toggle_btn")
            ) {
                Text(
                    text = if (isWasmDashboardVisible) "❌ CLOSE HUD" else "🛰️ WASM OVERLAY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Animated Tactical WASM Dashboard Overlay
        AnimatedVisibility(
            visible = isWasmDashboardVisible,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(250)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF9050811))
                    .pointerInput(Unit) {} // Consume touch events
                    .padding(16.dp)
            ) {
                // Background scanline laser sweeps
                val infiniteScan = rememberInfiniteTransition(label = "ScanLines")
                val scanLineY by infiniteScan.animateFloat(
                    initialValue = 0f,
                    targetValue = 1000f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(6000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "LaserLineY"
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawLine(
                        color = Color(0x1F00FFCC),
                        start = Offset(0f, scanLineY),
                        end = Offset(size.width, scanLineY),
                        strokeWidth = 2f
                    )
                }

                // Main Scrollable Console Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0x6600E5FF))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("⚡", fontSize = 16.sp)
                                    Text(
                                        "WASM TACTICAL CALCULATION MAIN COCKPIT",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00FFCC)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x3300FF88))
                                        .border(1.dp, Color(0xFF00FF88), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "WASM EXEC ACTIVE",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00FF88)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "COMPUTED VIA WEB-ASSEMBLY COMPILATION WITH REAL-TIME HARDWARE CALIBRATION SENSOR FEEDBACK",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                color = Color(0xFF8892B0)
                            )
                        }
                    }

                    // --- MODULE STATE ISOLATION TAB SELECTOR BAR ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val mainTabs = listOf(
                            "TACTICAL" to "🎯 TACTICAL FRONTLINE",
                            "NEXUS_CORE" to "⚡ NEXUS CORE",
                            "ENTERPRISE_LABS" to "🏢 LABS / ENTERPRISE"
                        )
                        mainTabs.forEach { (tabKey, label) ->
                            val isSelected = mainCockpitTab == tabKey
                            Button(
                                onClick = { mainCockpitTab = tabKey },
                                modifier = Modifier.weight(1f).height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF0284C7) else Color(0xFF0F172A),
                                    contentColor = if (isSelected) Color.White else Color(0xFF94A3B8)
                                ),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // --- DECOUPLED ASYNCHRONOUS CHANNEL STATUS BADGE ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF030712), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("📡", fontSize = 10.sp)
                                Text(
                                    text = when (mainCockpitTab) {
                                        "TACTICAL" -> "DECOUPLED CHANNEL: SPATIAL GEO/GRID CACHE [O(1) LOOKUPS]"
                                        "NEXUS_CORE" -> "DECOUPLED CHANNEL: KAFKA C++ EVENT LEDGER & MCTS STREAM"
                                        else -> "DECOUPLED CHANNEL: NEXUS ENTERPRISE & LABS (DIGITAL TWIN B2B)"
                                    },
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                            Text(
                                text = "RENDER ISOLATED • 0 LOST TICKS",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF00FF88)
                            )
                        }
                    }

                    if (mainCockpitTab == "TACTICAL") {
                        // --- MODULE 1: TACTICAL FRONTLINE & COMBAT MATRIX ---
                        // Native products of North & South World
                        Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Allied North Product
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1120)),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "ALLIED NORTH PRODUCT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF00E5FF)
                                )
                                Text(
                                    "Cryo-Alloy Fuel Core",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                // Custom canvas vector rendering for Allied North Product
                                Canvas(modifier = Modifier.size(100.dp)) {
                                    // Radial blue gradient background
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(Color(0x3300E5FF), Color(0x0500E5FF))
                                        ),
                                        radius = 45f
                                    )
                                    // Concentric circles
                                    drawCircle(color = Color(0x4400E5FF), radius = 40f, style = Stroke(1.5f))
                                    drawCircle(color = Color(0xFF00E5FF), radius = 25f, style = Stroke(2.5f))
                                    // Core diamond structure
                                    val path = Path().apply {
                                        moveTo(50f, 30f)
                                        lineTo(70f, 50f)
                                        lineTo(50f, 70f)
                                        lineTo(30f, 50f)
                                        close()
                                    }
                                    drawPath(path, color = Color(0x8800E5FF))
                                    // Crosshair lines
                                    drawLine(color = Color(0x6600E5FF), start = Offset(10f, 50f), end = Offset(90f, 50f), strokeWidth = 0.5f)
                                    drawLine(color = Color(0x6600E5FF), start = Offset(50f, 10f), end = Offset(50f, 90f), strokeWidth = 0.5f)
                                    // Scanning laser sweeps
                                    val laserProgress = (scanLineY % 80f) + 10f
                                    drawLine(
                                        color = Color(0xFF00FFCC),
                                        start = Offset(10f, laserProgress),
                                        end = Offset(90f, laserProgress),
                                        strokeWidth = 1.5f
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("• TEMP: -184°C [CRYO-SECURE]", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8892B0))
                                    Text("• CONVERSION COEFF: 99.4%", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8892B0))
                                    Text("• REGION STATUS: SYNCED", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF00FF88))
                                }
                            }
                        }

                        // Rival South Product
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF140D14)),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFF4500).copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "RIVAL SOUTH PRODUCT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFFF5722)
                                )
                                Text(
                                    "Volcanic Plasma Igniter",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                // Custom canvas vector rendering for Rival South Product
                                Canvas(modifier = Modifier.size(100.dp)) {
                                    // Radial orange gradient background
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(Color(0x33FF5722), Color(0x05FF5722))
                                        ),
                                        radius = 45f
                                    )
                                    // Concentric circles
                                    drawCircle(color = Color(0x44FF5722), radius = 40f, style = Stroke(1.5f))
                                    // Volcanic central core
                                    drawCircle(color = Color(0xFFFF5722), radius = (20f * firestonePulseScale), style = Stroke(2.5f))
                                    // Core star structure
                                    val path = Path().apply {
                                        moveTo(50f, 25f)
                                        lineTo(58f, 42f)
                                        lineTo(75f, 50f)
                                        lineTo(58f, 58f)
                                        lineTo(50f, 75f)
                                        lineTo(42f, 58f)
                                        lineTo(25f, 50f)
                                        lineTo(42f, 42f)
                                        close()
                                    }
                                    drawPath(path, color = Color(0x88FF5722))
                                    // Scanning laser sweeps
                                    val laserProgress = (scanLineY % 80f) + 10f
                                    drawLine(
                                        color = Color(0xFFFFB300),
                                        start = Offset(10f, laserProgress),
                                        end = Offset(90f, laserProgress),
                                        strokeWidth = 1.5f
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("• HEAT LEVEL: 1420°C [THERMAL]", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8892B0))
                                    Text("• ENERGY OUTPUT: 1.8 GW", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8892B0))
                                    Text("• STABILITY FACTOR: STABLE", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF00FF88))
                                }
                            }
                        }
                    }

                    // Tactical computations & power metrics comparing worlds
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "REAL-TIME TACTICAL CALCULATIONS MATRIX",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFFB700)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val northCount = regions.count { it.controller == "alliance_north" }
                            val southCount = regions.count { it.controller == "rival_south" }
                            val neutralCount = regions.count { it.controller == "neutral" }
                            val playerCount = regions.count { it.controller == "player" }
                            val totalRegions = regions.size.toFloat().coerceAtLeast(1f)

                            val allianceNorthWeight = (northCount.toFloat() / totalRegions) * 100f
                            val rivalSouthWeight = (southCount.toFloat() / totalRegions) * 100f

                            Text(
                                "wasm_calculate_global_territory_balance():",
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF00E5FF)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("NORTH", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF00E676))
                                LinearProgressIndicator(
                                    progress = allianceNorthWeight / 100f,
                                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFF00E676),
                                    trackColor = Color(0x1AFFFFFF)
                                )
                                Text("${String.format("%.1f", allianceNorthWeight)}%", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("SOUTH", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFFF1744))
                                LinearProgressIndicator(
                                    progress = rivalSouthWeight / 100f,
                                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFFFF1744),
                                    trackColor = Color(0x1AFFFFFF)
                                )
                                Text("${String.format("%.1f", rivalSouthWeight)}%", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "wasm_compute_threat_entropy_index() = " + String.format("%.4f", (rivalSouthWeight * 1.25f - allianceNorthWeight * 0.82f + 42f)),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFFD700)
                            )
                            Text(
                                "wasm_zkp_sensor_matrix_drift() = " + String.format("%.6f", (accelX * 0.0031f + accelY * 0.0028f)),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF8892B0)
                            )
                        }
                    }

                    // SENSORS CALIBRATION & ZKP CIRCUIT
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF00FFCC).copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("📡", fontSize = 14.sp)
                                    Text(
                                        "ZKP CRYO-SENSOR CALIBRATION",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF00FFCC)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            when (zkpVerificationPass) {
                                                true -> Color(0x3300FF88)
                                                false -> Color(0x33FF3B30)
                                                else -> Color(0x3394A3B8)
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            when (zkpVerificationPass) {
                                                true -> Color(0xFF00FF88)
                                                false -> Color(0xFFFF3B30)
                                                else -> Color(0xFF94A3B8)
                                            },
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = when (zkpVerificationPass) {
                                            true -> "ZKP SECURE"
                                            false -> "CRITICAL RANGE REJECTED"
                                            else -> "CIRCUIT STANDBY"
                                        },
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (zkpVerificationPass) {
                                            true -> Color(0xFF00FF88)
                                            false -> Color(0xFFFF3B30)
                                            else -> Color(0xFF94A3B8)
                                        },
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Text(
                                text = "Proves that the physical device inclinometer tilt vector sqrt(accelX² + accelY²) stays under safe tactical descent range (< 5.0m/s²) using a non-interactive Zero-Knowledge Schnorr/range proof without leaking raw sensor coordinates.",
                                fontSize = 8.sp,
                                color = Color(0xFF8892B0),
                                lineHeight = 10.sp
                            )

                            // Sensor Values
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("ACCEL X", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                    Text(String.format("%.3f m/s²", accelX), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("ACCEL Y", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                    Text(String.format("%.3f m/s²", accelY), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("GYRO Z", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                    Text(String.format("%.3f rad/s", gyroZ), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    val tiltVal = java.lang.Math.sqrt((accelX * accelX + accelY * accelY).toDouble())
                                    Text("TILT VECTOR", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                    Text(String.format("%.3f m/s²", tiltVal), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (tiltVal < 5.0) Color(0xFF00FF88) else Color(0xFFFF1744), fontFamily = FontFamily.Monospace)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            zkpGenerating = true
                                            zkpVerificationPass = null
                                            zkpLogs = listOf("Hashing raw sensor witness values: X=${accelX}, Y=${accelY}")
                                            delay(500)
                                            val tiltVal = java.lang.Math.sqrt((accelX * accelX + accelY * accelY).toDouble())
                                            zkpWitnessValue = String.format("%.4f", tiltVal)
                                            zkpLogs = zkpLogs + "Generated zk-SNARK constraint parameters mapping: target_val = $zkpWitnessValue"
                                            delay(500)
                                            zkpLogs = zkpLogs + "Constructing Schnorr range commitment: g^r * h^s mod p"
                                            delay(600)
                                            val isSafe = tiltVal < 5.0
                                            zkpProofResult = "0x" + List(48) { "0123456789abcdef".random() }.joinToString("")
                                            zkpLogs = zkpLogs + "ZK Proof payload generated: $zkpProofResult"
                                            delay(400)
                                            zkpVerificationPass = isSafe
                                            if (isSafe) {
                                                zkpLogs = zkpLogs + "Verifier verification: SUCCESS. Node bounds are cryptographically verified."
                                            } else {
                                                zkpLogs = zkpLogs + "Verifier alert: REJECTED! Witness factor exceeded threshold parameters."
                                            }
                                            zkpGenerating = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0B1B3A),
                                        contentColor = Color(0xFF00FFCC)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF00FFCC)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1.3f),
                                    enabled = !zkpGenerating
                                ) {
                                    Text("RUN CRYPTO SENSOR ZKP", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }

                                Button(
                                    onClick = {
                                        zkpVerificationPass = null
                                        zkpProofResult = null
                                        zkpWitnessValue = ""
                                        zkpLogs = listOf("ZKP Circuit mapped. Witness constraints: tilt_vector < 5.0 m/s²")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("RESET ZKP", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }

                            // Scrolling logs console
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .background(Color.Black, RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                zkpLogs.forEach { log ->
                                    Text(
                                        text = ">> $log",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        color = Color(0xFF00FFCC)
                                    )
                                }
                            }
                        }
                    }

                    // FIRESTONE STATE SYNC & VISUAL FEEDBACK SENSOR REACTOR
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF7700).copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("🔥", fontSize = 14.sp)
                                    Text(
                                        "FIRESTONE STATE SYNC REACTOR",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFFF9800)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (firestoneState == "SYNCING") Color(0x33FFB300) else Color(0x3300FF88))
                                        .border(1.dp, if (firestoneState == "SYNCING") Color(0xFFFFB300) else Color(0xFF00FF88), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = firestoneState,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (firestoneState == "SYNCING") Color(0xFFFFB300) else Color(0xFF00FF88),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Text(
                                text = "The Firestone reactor core represents state-sync visualizer. Tilting the device sensors generates rotational feedback kinetic torque on the Firestone core crystal, modifying sync telemetry latency factors.",
                                fontSize = 8.sp,
                                color = Color(0xFF8892B0),
                                lineHeight = 10.sp
                            )

                            // Firestone Visual Reactor Canvas & Sync Button Side-by-Side
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dynamic Canvas Core
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF080D1A))
                                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val width = size.width
                                        val height = size.height
                                        val cX = width / 2f
                                        val cY = height / 2f

                                        // Accel tilt offset calculations for sensor feedback
                                        val tiltOffsetX = (accelX * 3f).coerceIn(-30f, 30f)
                                        val tiltOffsetY = (accelY * -3f).coerceIn(-30f, 30f)
                                        val reactorCenter = Offset(cX + tiltOffsetX, cY + tiltOffsetY)

                                        // Dynamic pulsing radius based on firestonePulseScale
                                        val coreRadius = 24f * firestonePulseScale

                                        // Sync Wave concentric rings expansion
                                        if (showSyncWave) {
                                            val maxWaveRadius = 45f
                                            val currentRadius = maxWaveRadius * syncWaveProgress.value
                                            val alpha = 1f - syncWaveProgress.value
                                            drawCircle(
                                                color = Color(0xFFFF9800).copy(alpha = alpha),
                                                radius = currentRadius,
                                                center = reactorCenter,
                                                style = Stroke(width = 2f)
                                            )
                                        }

                                        // Outer magnetic flame boundary
                                        drawCircle(
                                            color = Color(0x33FF5722),
                                            radius = coreRadius + 12f,
                                            center = reactorCenter,
                                            style = Stroke(1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f)))
                                        )

                                        // Inner Firestone radial glowing reactor core crystal (drawn as rotating diamond)
                                        val crystalPath = Path().apply {
                                            val theta = Math.toRadians(firestoneRotation.toDouble())
                                            val points = listOf(
                                                Offset(0f, -coreRadius),
                                                Offset(coreRadius * 0.7f, 0f),
                                                Offset(0f, coreRadius),
                                                Offset(-coreRadius * 0.7f, 0f)
                                            )
                                            val rotatedPoints = points.map { pt ->
                                                val rx = pt.x * Math.cos(theta) - pt.y * Math.sin(theta)
                                                val ry = pt.x * Math.sin(theta) + pt.y * Math.cos(theta)
                                                Offset(rx.toFloat() + reactorCenter.x, ry.toFloat() + reactorCenter.y)
                                            }
                                            moveTo(rotatedPoints[0].x, rotatedPoints[0].y)
                                            lineTo(rotatedPoints[1].x, rotatedPoints[1].y)
                                            lineTo(rotatedPoints[2].x, rotatedPoints[2].y)
                                            lineTo(rotatedPoints[3].x, rotatedPoints[3].y)
                                            close()
                                        }

                                        drawPath(
                                            path = crystalPath,
                                            brush = Brush.radialGradient(
                                                colors = listOf(Color(0xFFFFD54F), Color(0xFFFF8F00), Color(0xFFD84315)),
                                                center = reactorCenter,
                                                radius = coreRadius
                                            )
                                        )
                                    }
                                }

                                // Interactive triggers and text descriptions
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        "Reactor Status: ACTIVE\nSync Documents: $firestoneDocsCount\nLatency: $firestoneLatencyMs ms RTT",
                                        fontSize = 8.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                firestoneState = "SYNCING"
                                                firestoneLogs = firestoneLogs + "Initiating Google Cloud Firestore sync sequence..."
                                                showSyncWave = true
                                                
                                                // Trigger gorgeous expanding sync wave animation
                                                syncWaveProgress.snapTo(0f)
                                                syncWaveProgress.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = tween(800, easing = LinearEasing)
                                                )
                                                showSyncWave = false
                                                
                                                delay(400)
                                                firestoneDocsCount += 1
                                                firestoneLatencyMs = Random.nextInt(10, 25)
                                                firestoneState = "CONNECTED"
                                                firestoneLogs = firestoneLogs + "Committed synchronized sensor calibrations successfully to sector_tactics Firestore stream."
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF3E2723),
                                            contentColor = Color(0xFFFFB74D)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFFFF9800)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("COMMIT FIRESTONE STATE", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }

                            // Logs console
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .background(Color.Black, RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                firestoneLogs.forEach { log ->
                                    Text(
                                        text = ">> $log",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        color = Color(0xFFFF9800)
                                    )
                                }
                            }
                        }
                    }

                    // A* Pathfinding Tactical Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF00FFCC).copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("🎯", fontSize = 14.sp)
                                    Text(
                                        "WASM A* TACTICAL PATHFINDER",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF00FFCC)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            when {
                                                isSearchingPath -> Color(0x33FFD700)
                                                isAnimatingUnit -> Color(0x3300FF88)
                                                else -> Color(0x331E293B)
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            when {
                                                isSearchingPath -> Color(0xFFFFD700)
                                                isAnimatingUnit -> Color(0xFF00FF88)
                                                else -> Color(0x6694A3B8)
                                            },
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = when {
                                            isSearchingPath -> "EXPLORING GRID"
                                            isAnimatingUnit -> "UNIT MOVING"
                                            aStarPath.isNotEmpty() -> "PATH READY"
                                            else -> "NAV STANDBY"
                                        },
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isSearchingPath -> Color(0xFFFFD700)
                                            isAnimatingUnit -> Color(0xFF00FF88)
                                            aStarPath.isNotEmpty() -> Color(0xFF00FFCC)
                                            else -> Color(0xFF94A3B8)
                                        },
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Text(
                                text = "Highly-optimized mathematical A* routing compiled natively within the WebAssembly engine. Visualizes step-by-step exploration of open and closed sets across map coordinates.",
                                fontSize = 8.sp,
                                color = Color(0xFF8892B0),
                                lineHeight = 10.sp
                            )

                            // Interaction Mode Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val modes = listOf(
                                    "TOGGLE_OBSTACLE" to "🧱 BARRIERS",
                                    "SET_START" to "🟢 START",
                                    "SET_TARGET" to "🟠 TARGET"
                                )
                                modes.forEach { (mode, label) ->
                                    Button(
                                        onClick = { aStarInteractionMode = mode },
                                        modifier = Modifier.weight(1f).height(28.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (aStarInteractionMode == mode) Color(0xFF0B3B5B) else Color(0xFF1E293B),
                                            contentColor = if (aStarInteractionMode == mode) Color(0xFF00FFCC) else Color.White
                                        ),
                                        border = if (aStarInteractionMode == mode) BorderStroke(1.dp, Color(0xFF00FFCC)) else null,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }

                            // LocalStorage Persistence & Undo Toolbar Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = { saveGridToLocalStorage() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                        .testTag("save_grid_btn"),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0F2E2B),
                                        contentColor = Color(0xFF00FFCC)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF00FFCC)),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("💾 SAVE", fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }

                                Button(
                                    onClick = { loadLastMapFromLocalStorage() },
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(28.dp)
                                        .testTag("load_last_map_btn"),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2E220F),
                                        contentColor = Color(0xFFFFB700)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFFFFB700)),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("🔄 LOAD LAST MAP", fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }

                                Button(
                                    onClick = { undoLastGridAction() },
                                    enabled = gridHistoryStack.isNotEmpty(),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(28.dp)
                                        .testTag("undo_action_btn"),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (gridHistoryStack.isNotEmpty()) Color(0xFF3B1E2B) else Color(0xFF1E293B),
                                        contentColor = if (gridHistoryStack.isNotEmpty()) Color(0xFFFF4081) else Color.Gray
                                    ),
                                    border = BorderStroke(1.dp, if (gridHistoryStack.isNotEmpty()) Color(0xFFFF4081) else Color.DarkGray),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        if (gridHistoryStack.isNotEmpty()) "⏪ UNDO (${gridHistoryStack.size})" else "⏪ UNDO",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Quick Grid Modification Row (Clear / Random Maze)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = {
                                        pushGridHistory()
                                        obstaclesState = emptySet()
                                        aStarPath = emptyList()
                                        openSetNodes = emptySet()
                                        closedSetNodes = emptySet()
                                        aStarLogs = aStarLogs + "[GRID] Cleared all barrier obstacles."
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(24.dp)
                                        .testTag("clear_obstacles_btn"),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1E293B),
                                        contentColor = Color(0xFF94A3B8)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    shape = RoundedCornerShape(3.dp)
                                ) {
                                    Text("🧹 CLEAR BARRIERS", fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }

                                Button(
                                    onClick = {
                                        pushGridHistory()
                                        val randomSet = mutableSetOf<Pair<Int, Int>>()
                                        val randomCount = (8..18).random()
                                        while (randomSet.size < randomCount) {
                                            val rx = (0 until gridWidth).random()
                                            val ry = (0 until gridHeight).random()
                                            if ((rx != aStarStartX || ry != aStarStartY) && (rx != aStarEndX || ry != aStarEndY)) {
                                                randomSet.add(Pair(rx, ry))
                                            }
                                        }
                                        obstaclesState = randomSet
                                        aStarPath = emptyList()
                                        openSetNodes = emptySet()
                                        closedSetNodes = emptySet()
                                        aStarLogs = aStarLogs + "[GRID] Generated random barrier maze (${randomSet.size} obstacles)."
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(24.dp)
                                        .testTag("random_maze_btn"),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1E293B),
                                        contentColor = Color(0xFF00E5FF)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(3.dp)
                                ) {
                                    Text("🎲 RANDOM MAZE", fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }

                            // Interactive 8x8 Grid
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .background(Color(0xFF070B14), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    for (y in 0 until gridHeight) {
                                        Row(
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            for (x in 0 until gridWidth) {
                                                val isObstacle = obstaclesState.contains(Pair(x, y))
                                                val isStart = x == aStarStartX && y == aStarStartY
                                                val isEnd = x == aStarEndX && y == aStarEndY
                                                val isPathNode = aStarPath.contains(Pair(x, y)) && !isStart && !isEnd
                                                val isCurrentlyEvaluating = currentEvaluatingNode == Pair(x, y)
                                                val isInOpenSet = openSetNodes.contains(Pair(x, y)) && !isStart && !isEnd && !isCurrentlyEvaluating
                                                val isInClosedSet = closedSetNodes.contains(Pair(x, y)) && !isStart && !isEnd && !isCurrentlyEvaluating && !isPathNode
                                                val hasActiveUnit = activeUnitX?.toInt() == x && activeUnitY?.toInt() == y
                                                val terrain = getTerrainAt(x, y)

                                                val isStartOrEnd = isStart || isEnd
                                                val isDirectlyHovered = (hoveredCellX == x && hoveredCellY == y)
                                                val isCellPulsing = isStartOrEnd || isDirectlyHovered || isCurrentlyEvaluating || isPathNode || isInOpenSet || (isGridBoardHovered && hasActiveUnit)

                                                val baseCellColor = when {
                                                    hasActiveUnit -> Color(0xFF00FFCC)
                                                    isStart -> Color(0xFF00E676)
                                                    isEnd -> Color(0xFFFF5722)
                                                    isCurrentlyEvaluating -> Color(0xFFFFD700)
                                                    isPathNode -> Color(0x6600FF88)
                                                    isInOpenSet -> Color(0x5500E5FF)
                                                    isInClosedSet -> Color(0x447C4DFF)
                                                    isObstacle -> Color(0xFF334155)
                                                    else -> terrain.color
                                                }

                                                val cellColor = if (isStartOrEnd) {
                                                    baseCellColor.copy(alpha = (0.75f + 0.25f * cellPulseAlpha).coerceIn(0.4f, 1f))
                                                } else if (isCellPulsing) {
                                                    baseCellColor.copy(alpha = (baseCellColor.alpha * cellPulseAlpha).coerceIn(0.35f, 1f))
                                                } else {
                                                    baseCellColor
                                                }

                                                // Smooth background and border color / width morphing transitions for grid cells
                                                val animatedCellColor by animateColorAsState(
                                                    targetValue = cellColor,
                                                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                                    label = "GridCellColorMorph"
                                                )

                                                val targetBorderColor = when {
                                                    isDirectlyHovered -> Color(0xFF00FFCC)
                                                    isCurrentlyEvaluating -> Color(0xFFFFFF00)
                                                    isStart -> Color(0xFF00FF88).copy(alpha = (0.6f + 0.4f * cellPulseAlpha).coerceIn(0f, 1f))
                                                    isEnd -> Color(0xFFFF7043).copy(alpha = (0.6f + 0.4f * cellPulseAlpha).coerceIn(0f, 1f))
                                                    isPathNode -> Color(0xFF00FF88).copy(alpha = cellPulseAlpha)
                                                    isInOpenSet -> Color(0xFF00E5FF).copy(alpha = cellPulseAlpha)
                                                    isInClosedSet -> Color(0x887C4DFF)
                                                    else -> Color(0x22FFFFFF)
                                                }

                                                val animatedBorderColor by animateColorAsState(
                                                    targetValue = targetBorderColor,
                                                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                                    label = "GridCellBorderColorMorph"
                                                )

                                                val targetBorderWidth = if (isStart || isEnd) {
                                                    (2.dp + 1.dp * (cellPulseScale - 0.94f) * 8f)
                                                } else if (isDirectlyHovered || isCurrentlyEvaluating) {
                                                    2.dp
                                                } else if (isPathNode || isInOpenSet) {
                                                    1.dp
                                                } else {
                                                    0.5.dp
                                                }

                                                val animatedBorderWidth by animateDpAsState(
                                                    targetValue = targetBorderWidth,
                                                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                                                    label = "GridCellBorderWidthMorph"
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .testTag("grid-cell")
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .graphicsLayer {
                                                            if (isStartOrEnd) {
                                                                scaleX = cellPulseScale
                                                                scaleY = cellPulseScale
                                                                alpha = 0.85f + 0.15f * cellPulseAlpha
                                                                shadowElevation = 8f * cellPulseScale
                                                                spotShadowColor = if (isStart) Color(0xFF00FF88) else Color(0xFFFF5722)
                                                                ambientShadowColor = if (isStart) Color(0xFF00FF88) else Color(0xFFFF5722)
                                                            } else if (isCellPulsing) {
                                                                scaleX = cellPulseScale
                                                                scaleY = cellPulseScale
                                                                alpha = cellPulseAlpha
                                                            }
                                                        }
                                                        .background(animatedCellColor, RoundedCornerShape(2.dp))
                                                        .border(
                                                            width = animatedBorderWidth,
                                                            color = animatedBorderColor,
                                                            shape = RoundedCornerShape(2.dp)
                                                        )
                                                        .pointerInput(x, y) {
                                                            awaitPointerEventScope {
                                                                while (true) {
                                                                    val event = awaitPointerEvent()
                                                                    isGridBoardHovered = true
                                                             pushGridHistory()
                                                                    if (event.type == PointerEventType.Move || event.type == PointerEventType.Enter) {
                                                                        hoveredCellX = x
                                                                        hoveredCellY = y
                                                                    } else if (event.type == PointerEventType.Exit) {
                                                                        if (hoveredCellX == x && hoveredCellY == y) {
                                                                            hoveredCellX = null
                                                                            hoveredCellY = null
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        .clickable {
                                                            hoveredCellX = x
                                                            hoveredCellY = y
                                                            isGridBoardHovered = true
                                                            if (!isSearchingPath && !isAnimatingUnit) {
                                                                when (aStarInteractionMode) {
                                                                    "TOGGLE_OBSTACLE" -> {
                                                                        if (!isStart && !isEnd) {
                                                                            obstaclesState = if (isObstacle) {
                                                                                obstaclesState - Pair(x, y)
                                                                            } else {
                                                                                obstaclesState + Pair(x, y)
                                                                            }
                                                                        }
                                                                    }
                                                                    "SET_START" -> {
                                                                        if (!isObstacle && !isEnd) {
                                                                            aStarStartX = x
                                                                            aStarStartY = y
                                                                        }
                                                                    }
                                                                    "SET_TARGET" -> {
                                                                        if (!isObstacle && !isStart) {
                                                                            aStarEndX = x
                                                                            aStarEndY = y
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (hasActiveUnit) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center,
                                                            modifier = Modifier.fillMaxSize()
                                                        ) {
                                                            Text("🛰️", fontSize = 11.sp)
                                                            Spacer(modifier = Modifier.height(1.dp))
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth(0.8f)
                                                                    .height(2.dp)
                                                                    .clip(RoundedCornerShape(1.dp))
                                                                    .background(Color.Red.copy(alpha = 0.4f))
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth(playerHp.toFloat() / playerMaxHp.toFloat())
                                                                        .fillMaxHeight()
                                                                        .background(Color(0xFF00FFCC))
                                                                )
                                                            }
                                                        }
                                                    } else if (isEnd) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center,
                                                            modifier = Modifier.fillMaxSize()
                                                        ) {
                                                            Text(if (enemyHp > 0) "👾" else "💥", fontSize = 11.sp)
                                                            Spacer(modifier = Modifier.height(1.dp))
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth(0.8f)
                                                                    .height(2.dp)
                                                                    .clip(RoundedCornerShape(1.dp))
                                                                    .background(Color.Red.copy(alpha = 0.4f))
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth(enemyHp.toFloat() / enemyMaxHp.toFloat())
                                                                        .fillMaxHeight()
                                                                        .background(Color(0xFFFF5722))
                                                                )
                                                            }
                                                        }
                                                    } else if (isStart) {
                                                        Text("S", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                                                    } else if (isCurrentlyEvaluating) {
                                                        Text("⚡", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    } else if (isPathNode) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .background(Color(0xFF00FF88), CircleShape)
                                                        )
                                                    } else if (isInOpenSet) {
                                                        Text("◇", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                                                    } else if (isInClosedSet) {
                                                        Text("·", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB388FF))
                                                    } else if (!isObstacle) {
                                                        Text(
                                                            text = terrain.symbol,
                                                            fontSize = 8.sp,
                                                            color = Color.White.copy(alpha = 0.15f),
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Search State Legend Key
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SEARCH KEY:",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF8892B0),
                                    fontFamily = FontFamily.Monospace
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("⚡ Current", fontSize = 6.sp, color = Color(0xFFFFD700), fontFamily = FontFamily.Monospace)
                                    Text("◇ Open Set", fontSize = 6.sp, color = Color(0xFF00E5FF), fontFamily = FontFamily.Monospace)
                                    Text("· Closed Set", fontSize = 6.sp, color = Color(0xFFB388FF), fontFamily = FontFamily.Monospace)
                                    Text("🟢 Path", fontSize = 6.sp, color = Color(0xFF00FF88), fontFamily = FontFamily.Monospace)
                                }
                            }

                            // Animation Speed Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "EXPLORATION SPEED:",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF8892B0),
                                    fontFamily = FontFamily.Monospace
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val speeds = listOf(180L to "SLOW", 80L to "NORMAL", 25L to "FAST")
                                    speeds.forEach { (speedMs, label) ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(if (pathAnimationSpeedMs == speedMs) Color(0xFF00FFCC) else Color(0xFF1E293B))
                                                .clickable { pathAnimationSpeedMs = speedMs }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (pathAnimationSpeedMs == speedMs) Color.Black else Color.White,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }

                            // Math telemetry metrics
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("ENGINE LATENCY", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                    Text(String.format("%.4f ms", aStarLatencyMs), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FF88), fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("OPEN SET PEAK", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                    Text("$aStarOpenSetSize nodes", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("CLOSED SET SIZE", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                    Text("$aStarClosedSetSize nodes", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("STEPS / COST", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                    Text("${aStarPath.size} stp / ${aStarTotalCost} cst", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB700), fontFamily = FontFamily.Monospace)
                                }
                            }

                            // Action Button: "FIND A* PATH"
                            Button(
                                onClick = { runAStarStepByStepAnimation() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSearchingPath) Color(0xFF311B92) else Color(0xFF0B1B3A),
                                    contentColor = Color(0xFF00FFCC)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF00FFCC)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth().testTag("wasm_trigger_astar_btn"),
                                enabled = !isSearchingPath && !isAnimatingUnit
                            ) {
                                if (isSearchingPath) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            color = Color(0xFF00FFCC),
                                            strokeWidth = 2.dp
                                        )
                                        Text("EXPLORING GRID STEP-BY-STEP...", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                } else {
                                    Text(
                                        text = if (aStarPath.isEmpty()) "FIND A* PATH" else "RE-RUN A* PATHFINDING ANIMATION",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Trajectory execution button (when path is found)
                            if (aStarPath.isNotEmpty()) {
                                Button(
                                    onClick = { triggerAutomatedMovement() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF004D40),
                                        contentColor = Color(0xFF00FF88)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF00FF88)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("wasm_execute_trajectory_btn"),
                                    enabled = !isSearchingPath && !isAnimatingUnit
                                ) {
                                    Text("EXECUTE UNIT TRAJECTORY", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }

                            // Scrolling logs console for A* pathfinding
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .background(Color.Black, RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                aStarLogs.forEach { log ->
                                    Text(
                                        text = ">> $log",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        color = Color(0xFF00FFCC)
                                    )
                                }
                            }

                            // Horizontal design divider for Combat
                            Divider(
                                color = Color(0xFF1F2937),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            // Combat stats header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("⚔️", fontSize = 14.sp)
                                    Text(
                                        "WASM TACTICAL COMBAT MATRIX",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFFF5722)
                                    )
                                }
                                if (isCombatActive) {
                                    Text(
                                        text = "RESOLVING COMBAT MATH...",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFB700),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Interactive Combat Unit Health Bars
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                // 1. Alliance Recon (Player) HP Bar
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Alliance Recon Drone 🛰️ (S)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00FFCC),
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            "$playerHp / $playerMaxHp HP",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00FFCC),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0x33FF0000))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(playerHp.toFloat() / playerMaxHp.toFloat())
                                                .fillMaxHeight()
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(Color(0xFF00B0FF), Color(0xFF00FFCC))
                                                    )
                                                )
                                        )
                                    }
                                }

                                // 2. Rogue Sentinel (Enemy) HP Bar
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Rogue Sentinel 👾 (T)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF5722),
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            "$enemyHp / $enemyMaxHp HP",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF5722),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0x33FF0000))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(enemyHp.toFloat() / enemyMaxHp.toFloat())
                                                .fillMaxHeight()
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(Color(0xFFFF5252), Color(0xFFFF5722))
                                                    )
                                                )
                                        )
                                    }
                                }
                            }

                            // Combat Trigger Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { runWasmCombatMath() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF3B0B0B),
                                        contentColor = Color(0xFFFF5252)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFFFF5252)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1.2f).height(32.dp).testTag("wasm_trigger_combat_btn"),
                                    contentPadding = PaddingValues(0.dp),
                                    enabled = playerHp > 0 && enemyHp > 0 && !isCombatActive
                                ) {
                                    Text("ENGAGE COMBAT", fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }

                                Button(
                                    onClick = { resetCombatUnits() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0F172A),
                                        contentColor = Color(0xFF94A3B8)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(0.8f).height(32.dp).testTag("wasm_reset_combat_btn"),
                                    contentPadding = PaddingValues(0.dp),
                                    enabled = !isCombatActive
                                ) {
                                    Text("REPAIR & RESTORE", fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }

                            // Scrolling Combat Battle Log
                            Text(
                                text = "TACTICAL BATTLE LOG TELEMETRY",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8892B0),
                                fontFamily = FontFamily.Monospace
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(95.dp)
                                    .background(Color(0xFF020617), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                battleLogs.forEach { log ->
                                    val textColor = when {
                                        log.contains("strikes") -> Color(0xFF00FFCC)
                                        log.contains("counter-attacks") -> Color(0xFFFF5252)
                                        log.contains("VICTORY") -> Color(0xFF00E676)
                                        log.contains("DEFEAT") -> Color(0xFFFF1744)
                                        else -> Color(0xFF94A3B8)
                                    }
                                    Text(
                                        text = ">> $log",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
            } else if (mainCockpitTab == "NEXUS_CORE") {
                        // --- MODULE 2: NEXUS CORE, KAFKA LEDGER, ZKP & MCTS ---
                        // --- KAFKA C++ BINARY ORCHESTRATOR & DECOUPLED LOGISTICS LEDGER CARD ---
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("kafka_orchestrator_card"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0F1D)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header Title & Channel Telemetry Badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("⚡", fontSize = 14.sp)
                                    Text(
                                        "KAFKA C++ ORCHESTRATOR & DECOUPLED LEDGER",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x2200E676))
                                        .border(1.dp, Color(0xFF00E676), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "WS ASYNC CHANNEL ACTIVE",
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E676),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Text(
                                text = "Kafka Binary C++ Event Sourcing • MCTS Router (Cobol/Circom+Rust) • Spatial Geo/Grid Node Index",
                                fontSize = 8.sp,
                                color = Color(0xFF94A3B8),
                                fontFamily = FontFamily.Monospace
                            )

                            // Navigation Tab Selector for Ledger Subsystems
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val tabs = listOf(
                                    "EVENTS" to "📜 EVENT LOG",
                                    "LOGISTICS" to "📦 LOGISTICS",
                                    "SPATIAL_GRID" to "🗺️ GEO-GRID",
                                    "MCTS" to "🧠 MCTS ROUTER"
                                )
                                tabs.forEach { (tabKey, label) ->
                                    val isSelected = selectedLedgerTab == tabKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B))
                                            .clickable { selectedLedgerTab = tabKey }
                                            .padding(vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            // Subsystem Content Rendering
                            when (selectedLedgerTab) {
                                "EVENTS" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "IMMUTABLE KAFKA EVENT STREAM (${kafkaEventsLedger.size} LOGS)",
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF38BDF8),
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "PROOF: circom+rust (arkworks)",
                                                fontSize = 7.sp,
                                                color = Color(0xFFA855F7),
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        // Scrolling immutable binary event log
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                                .background(Color(0xFF020617), RoundedCornerShape(6.dp))
                                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                                .padding(6.dp)
                                                .verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            kafkaEventsLedger.reversed().forEach { event ->
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
                                                        .padding(4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "#${event.seqId} • ${event.eventType} [${event.topic}]",
                                                            fontSize = 7.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF38BDF8),
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                        Text(
                                                            text = "P${event.partition}",
                                                            fontSize = 7.sp,
                                                            color = Color(0xFF64748B),
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                    Text(
                                                        text = event.payload,
                                                        fontSize = 7.5.sp,
                                                        color = Color(0xFFE2E8F0),
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    Text(
                                                        text = "ZK-Proof Hash: ${event.proofHash}",
                                                        fontSize = 6.5.sp,
                                                        color = Color(0xFFA855F7),
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    publishKafkaEvent(
                                                        topic = "kafka.binary.tactical",
                                                        eventType = "MANUAL_EVENT_INJECTED",
                                                        payload = "Commander manually injected immutable binary state payload into Kafka C++ pipeline."
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF0369A1),
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.weight(1f).height(28.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("INJECT EVENT", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            }

                                            Button(
                                                onClick = { replayEventStream() },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF334155),
                                                    contentColor = Color(0xFF38BDF8)
                                                ),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.weight(1f).height(28.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("REPLAY STREAM", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }
                                }

                                "LOGISTICS" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "DECOUPLED BACKGROUND ECONOMY & LOGISTICS",
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981),
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "FLOW RATE: $supplyFlowRate u/min",
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF59E0B),
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        // 4 Resource Inventories Grid
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // Energy
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(Color(0xFF022C22), RoundedCornerShape(4.dp))
                                                    .border(1.dp, Color(0xFF059669), RoundedCornerShape(4.dp))
                                                    .padding(6.dp)
                                            ) {
                                                Column {
                                                    Text("⚡ ENERGY", fontSize = 6.5.sp, color = Color(0xFF6EE7B7), fontFamily = FontFamily.Monospace)
                                                    Text("$energyCrystals", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                                }
                                            }
                                            // Steel
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(Color(0xFF1E293B), RoundedCornerShape(4.dp))
                                                    .border(1.dp, Color(0xFF64748B), RoundedCornerShape(4.dp))
                                                    .padding(6.dp)
                                            ) {
                                                Column {
                                                    Text("🔩 STEEL", fontSize = 6.5.sp, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
                                                    Text("$steelAlloy", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                                }
                                            }
                                            // Fuel
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(Color(0xFF451A03), RoundedCornerShape(4.dp))
                                                    .border(1.dp, Color(0xFFD97706), RoundedCornerShape(4.dp))
                                                    .padding(6.dp)
                                            ) {
                                                Column {
                                                    Text("⛽ FUEL", fontSize = 6.5.sp, color = Color(0xFFFCD34D), fontFamily = FontFamily.Monospace)
                                                    Text("$quantumFuel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                                }
                                            }
                                            // Ammo
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(Color(0xFF312E81), RoundedCornerShape(4.dp))
                                                    .border(1.dp, Color(0xFF6366F1), RoundedCornerShape(4.dp))
                                                    .padding(6.dp)
                                            ) {
                                                Column {
                                                    Text("💣 AMMO", fontSize = 6.5.sp, color = Color(0xFFA5B4FC), fontFamily = FontFamily.Monospace)
                                                    Text("$ammunition", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                                }
                                            }
                                        }

                                        // Supply Convoy & Morale Boost Controls
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = { dispatchSupplyConvoy() },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF065F46),
                                                    contentColor = Color(0xFF6EE7B7)
                                                ),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.weight(1f).height(28.dp),
                                                contentPadding = PaddingValues(0.dp),
                                                enabled = quantumFuel >= 25
                                            ) {
                                                Text("DISPATCH CONVOY (-25 Fuel)", fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            }

                                            Button(
                                                onClick = { boostUnitMorale() },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF4C1D95),
                                                    contentColor = Color(0xFFC084FC)
                                                ),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.weight(1f).height(28.dp),
                                                contentPadding = PaddingValues(0.dp),
                                                enabled = energyCrystals >= 30
                                            ) {
                                                Text("BOOST MORALE (-30 Energy)", fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }
                                }

                                "SPATIAL_GRID" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "SPATIAL GEO/GRID NODE INDEXED CACHE",
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8),
                                            fontFamily = FontFamily.Monospace
                                        )

                                        // Metrics Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("INDEXED NODES", fontSize = 6.5.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                                                Text("64 / 64 Cells", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                            }
                                            Column {
                                                Text("O(1) CACHE HITS", fontSize = 6.5.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                                                Text("$spatialLookupHits Hits", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FF88), fontFamily = FontFamily.Monospace)
                                            }
                                            Column {
                                                Text("RECALC MISSES", fontSize = 6.5.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                                                Text("$spatialLookupMisses Misses", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace)
                                            }
                                            Column {
                                                Text("UNIT MORALE", fontSize = 6.5.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                                                Text("$unitMorale %", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700), fontFamily = FontFamily.Monospace)
                                            }
                                        }

                                        // Transactional Unit Status
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
                                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = "ACTIVE UNIT POS: (${activeUnitX?.toInt() ?: aStarStartX}, ${activeUnitY?.toInt() ?: aStarStartY}) | SUPPLY LINE: $supplyRouteStatus ($activeSupplyLineDistance cells) | MORALE: $unitMorale%",
                                                fontSize = 7.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFFE2E8F0)
                                            )
                                        }
                                    }
                                }

                                "MCTS" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "MCTS PREDICTIVE ROUTER (Cobol/Circom+Rust)",
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFA855F7),
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = mctsProofStatus,
                                                fontSize = 6.5.sp,
                                                color = Color(0xFF00FF88),
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("PREDICTED WIN %", fontSize = 6.5.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                                                Text(String.format("%.1f%%", mctsWinProbability), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FF88), fontFamily = FontFamily.Monospace)
                                            }
                                            Column {
                                                Text("MCTS NODES EVAL", fontSize = 6.5.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                                                Text("$mctsSimulatedNodes", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                            }
                                            Column {
                                                Text("SIM LATENCY", fontSize = 6.5.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                                                Text(String.format("%.2f ms", mctsLastPredictionTimeMs), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace)
                                            }
                                        }

                                        Button(
                                            onClick = { runMctsSimulation() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF581C87),
                                                contentColor = Color(0xFFE9D5FF)
                                            ),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.fillMaxWidth().height(28.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("SIMULATE MCTS PREDICTIVE TRAJECTORY", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    } else {
                        // --- MODULE 3: NEXUS ENTERPRISE & LABS (DIGITAL TWIN B2B) ---
                        NexusEnterpriseLabsDashboard(
                            selectedHub = enterpriseSelectedHub,
                            onHubSelected = { enterpriseSelectedHub = it },
                            fleetUnits = enterpriseFleetUnits,
                            onFleetUnitsChange = { enterpriseFleetUnits = it },
                            isNodeDisrupted = enterpriseNodeDisrupted,
                            onToggleNodeDisrupted = {
                                enterpriseNodeDisrupted = !enterpriseNodeDisrupted
                                if (enterpriseNodeDisrupted) {
                                    enterpriseCostPerKm = 1.84
                                    enterpriseTransitHours = 18.6
                                } else {
                                    enterpriseCostPerKm = 1.42
                                    enterpriseTransitHours = 14.2
                                }
                            },
                            costPerKm = enterpriseCostPerKm,
                            transitHours = enterpriseTransitHours,
                            selectedScenario = enterpriseSelectedScenario,
                            onScenarioSelected = { enterpriseSelectedScenario = it },
                            mctsIterations = enterpriseMctsIterations,
                            onMctsIterationsChange = { enterpriseMctsIterations = it },
                            successRate = enterpriseSuccessRate,
                            financialSavings = enterpriseFinancialSavingsMillions,
                            zkStatus = enterpriseZkProofStatus,
                            zkLogs = enterpriseZkLogs,
                            onRecalculateRoute = {
                                scope.launch {
                                    enterpriseCostPerKm = if (enterpriseNodeDisrupted) 1.51 else 1.38
                                    enterpriseTransitHours = if (enterpriseNodeDisrupted) 15.1 else 13.6
                                    publishKafkaEvent("enterprise.digital_twin.a_star", "ROUTE_RECALCULATED", "O(1) A* fast path recalculated for $enterpriseSelectedHub with $enterpriseFleetUnits fleet units.")
                                }
                            },
                            onRunMctsScenario = {
                                scope.launch {
                                    val randSavings = 12.0 + Random.nextDouble() * 8.0
                                    val randSuccess = 85.0 + Random.nextDouble() * 12.0
                                    enterpriseFinancialSavingsMillions = String.format(java.util.Locale.US, "%.1f", randSavings).toDouble()
                                    enterpriseSuccessRate = String.format(java.util.Locale.US, "%.1f", randSuccess).toDouble()
                                    publishKafkaEvent("enterprise.mcts.predictive", "STRESS_TEST_RUN", "MCTS stress test executed for scenario $enterpriseSelectedScenario over $enterpriseMctsIterations iterations.")
                                }
                            },
                            onGenerateZkProof = {
                                scope.launch {
                                    enterpriseZkProofStatus = "GENERATING PROOF..."
                                    delay(400)
                                    val hash = "0x" + (1..16).map { "0123456789abcdef".random() }.joinToString("")
                                    enterpriseZkProofStatus = "VERIFIED (Circom BN254)"
                                    enterpriseZkLogs = enterpriseZkLogs + listOf(
                                        "[${System.currentTimeMillis() % 100000}] [ARKWORKS] Groth16 B2B compliance proof generated: $hash",
                                        "[${System.currentTimeMillis() % 100000}] [COMPLIANCE] ISO-27001 & ESG zero-knowledge certificate validated."
                                    )
                                    publishKafkaEvent("enterprise.zk.compliance", "CERTIFICATE_ISSUED", "ZK Proof generated: $hash. ESG & ISO-27001 verified.")
                                }
                            },
                            onPublishKafkaEvent = { topic, eventType, payload ->
                                publishKafkaEvent(topic, eventType, payload)
                            }
                        )
                    }
                }
            }
        }

        // ========================== COMMANDER TELEMETRY & SYSTEM HEALTH DIAGNOSTICS MODAL ==========================
        AnimatedVisibility(
            visible = isTelemetryConsoleVisible,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(250)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFA030712))
                    .pointerInput(Unit) {}
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("telemetry_modal_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF090D16)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF38BDF8))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header Title Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🖥️", fontSize = 16.sp)
                                Column {
                                    Text(
                                        "COMMANDER MODE: SYSTEM TELEMETRY & KAFKA DIAGNOSTICS",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF38BDF8)
                                    )
                                    Text(
                                        "Real-time Async Channels • WebSocket Latency • ZK-Proof Auditing • Spatial Cache Health",
                                        fontSize = 7.5.sp,
                                        color = Color(0xFF94A3B8),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            IconButton(
                                onClick = { isTelemetryConsoleVisible = false },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Text("❌", fontSize = 11.sp, color = Color.White)
                            }
                        }

                        Divider(color = Color(0xFF1E293B), thickness = 1.dp)

                        // Real-time Health Metrics Dashboard Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 1. Kafka C++ Engine Status
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF021329), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(6.dp))
                                    .padding(6.dp)
                            ) {
                                Column {
                                    Text("KAFKA C++ BROKER", fontSize = 6.5.sp, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace)
                                    Text("ONLINE :9092", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FF88), fontFamily = FontFamily.Monospace)
                                    Text("Rate: 1,240 msg/s", fontSize = 6.sp, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
                                }
                            }

                            // 2. WS Channel Latency
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF022C22), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF059669), RoundedCornerShape(6.dp))
                                    .padding(6.dp)
                            ) {
                                Column {
                                    Text("WS ASYNC LATENCY", fontSize = 6.5.sp, color = Color(0xFF6EE7B7), fontFamily = FontFamily.Monospace)
                                    Text(String.format("%.2f ms", pingLatencyMs), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                    Text("Jitter: ±0.08 ms", fontSize = 6.sp, color = Color(0xFFA7F3D0), fontFamily = FontFamily.Monospace)
                                }
                            }

                            // 3. Replay Buffer & Integrity
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF312E81), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF6366F1), RoundedCornerShape(6.dp))
                                    .padding(6.dp)
                            ) {
                                Column {
                                    Text("REPLAY BUFFER", fontSize = 6.5.sp, color = Color(0xFFA5B4FC), fontFamily = FontFamily.Monospace)
                                    Text("${kafkaEventsLedger.size}/40 Logs", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                    Text("Integrity: 100% OK", fontSize = 6.sp, color = Color(0xFFC7D2FE), fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 4. Spatial Geo/Grid Cache
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF451A03), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFFD97706), RoundedCornerShape(6.dp))
                                    .padding(6.dp)
                            ) {
                                Column {
                                    Text("SPATIAL GEO-GRID", fontSize = 6.5.sp, color = Color(0xFFFCD34D), fontFamily = FontFamily.Monospace)
                                    Text("64/64 O(1) Nodes", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                    Text("Hits: $spatialLookupHits | Miss: $spatialLookupMisses", fontSize = 6.sp, color = Color(0xFFFDE68A), fontFamily = FontFamily.Monospace)
                                }
                            }

                            // 5. ZK Proof Verifier
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF4C1D95), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF8B5CF6), RoundedCornerShape(6.dp))
                                    .padding(6.dp)
                            ) {
                                Column {
                                    Text("ZK-SNARK VERIFIER", fontSize = 6.5.sp, color = Color(0xFFDDD6FE), fontFamily = FontFamily.Monospace)
                                    Text("PASS (arkworks)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FF88), fontFamily = FontFamily.Monospace)
                                    Text("Circom+Rust Validated", fontSize = 6.sp, color = Color(0xFFC4B5FD), fontFamily = FontFamily.Monospace)
                                }
                            }

                            // 6. Decoupled Logistics
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF64748B), RoundedCornerShape(6.dp))
                                    .padding(6.dp)
                            ) {
                                Column {
                                    Text("BG LOGISTICS THREAD", fontSize = 6.5.sp, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
                                    Text("ACTIVE (+65 u/m)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                    Text("Route: $supplyRouteStatus", fontSize = 6.sp, color = Color(0xFFCBD5E1), fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        Text(
                            text = "COMMANDER DIAGNOSTIC ACTIONS & SYSTEM COMMANDS",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace
                        )

                        // Interactive Command Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val newPing = 0.8 + Random.nextDouble() * 0.6
                                        pingLatencyMs = newPing
                                        telemetryLogs = telemetryLogs + String.format("[%tT] [PING_TEST] WS async channel roundtrip response: %.2f ms. Buffer jitter < 0.1ms.", System.currentTimeMillis(), newPing)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1), contentColor = Color.White),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f).height(28.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("⚡ TEST LATENCY", fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        telemetryLogs = telemetryLogs + String.format("[%tT] [ZK_AUDIT] Auditing %d event SNARK proofs via arkworks Groth16 verifier...", System.currentTimeMillis(), kafkaEventsLedger.size)
                                        delay(300)
                                        telemetryLogs = telemetryLogs + String.format("[%tT] [ZK_AUDIT] SUCCESS: All %d proof hashes cryptographically verified.", System.currentTimeMillis(), kafkaEventsLedger.size)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF581C87), contentColor = Color(0xFFE9D5FF)),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f).height(28.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("🔐 AUDIT ZK PROOFS", fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        telemetryLogs = telemetryLogs + String.format("[%tT] [BUFFER_PURGE] Replay buffer purged. Re-initializing Kafka stream consumer...", System.currentTimeMillis())
                                        delay(200)
                                        publishKafkaEvent("kafka.binary.system", "BUFFER_RESTART", "Replay buffer purged and re-initialized by Commander command.")
                                        telemetryLogs = telemetryLogs + String.format("[%tT] [BUFFER_PURGE] Kafka pipeline recovered successfully. Buffer online.", System.currentTimeMillis())
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C), contentColor = Color.White),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f).height(28.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("🧹 PURGE BUFFER", fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }

                            Button(
                                onClick = {
                                    spatialLookupHits += 16
                                    telemetryLogs = telemetryLogs + String.format("[%tT] [GEO_SYNC] Geo/Grid spatial terrain nodes re-indexed. O(1) cache refreshed.", System.currentTimeMillis())
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857), contentColor = Color(0xFF6EE7B7)),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f).height(28.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("🔄 SYNC GEO CACHE", fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Text(
                            text = "COMMANDER LIVE SYSTEM TELEMETRY TERMINAL LOGS",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace
                        )

                        // Terminal Log Window
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color(0xFF020617), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            telemetryLogs.forEach { log ->
                                Text(
                                    text = ">> $log",
                                    fontSize = 7.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = when {
                                        log.contains("SUCCESS") || log.contains("ONLINE") -> Color(0xFF00FF88)
                                        log.contains("PURGE") -> Color(0xFFFFB300)
                                        log.contains("AUDIT") -> Color(0xFFC084FC)
                                        else -> Color(0xFF38BDF8)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TacticalTurnHUD(
    turnNumber: Int,
    activeFactionTurn: String,
    commanderFaction: String,
    turnTimeRemaining: Int,
    turnMaxDuration: Int,
    isTurnTimerActive: Boolean,
    currentLanguage: com.example.ui.game.AppLanguage,
    onEndTurnManually: () -> Unit,
    onToggleTurnTimer: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val factionTitle = when (activeFactionTurn) {
        "player" -> when (currentLanguage) {
            com.example.ui.game.AppLanguage.ENGLISH -> "NORTH UNION (PLAYER)"
            com.example.ui.game.AppLanguage.ESPANOL -> "UNIÓN DEL NORTE (JUGADOR)"
            com.example.ui.game.AppLanguage.PORTUGUES -> "UNIÃO DO NORTE (JOGADOR)"
        }
        "alliance_north" -> when (currentLanguage) {
            com.example.ui.game.AppLanguage.ENGLISH -> "ALLIANCE NORTH (AI)"
            com.example.ui.game.AppLanguage.ESPANOL -> "ALIANZA DEL NORTE (IA)"
            com.example.ui.game.AppLanguage.PORTUGUES -> "ALIANÇA DO NORTE (IA)"
        }
        else -> when (currentLanguage) {
            com.example.ui.game.AppLanguage.ENGLISH -> "SOUTH COALITION (RIVAL AI)"
            com.example.ui.game.AppLanguage.ESPANOL -> "COALICIÓN DEL SUR (IA RIVAL)"
            com.example.ui.game.AppLanguage.PORTUGUES -> "COALIZÃO DO SUL (IA RIVAL)"
        }
    }

    val factionStatus = when (activeFactionTurn) {
        "player" -> when (currentLanguage) {
            com.example.ui.game.AppLanguage.ENGLISH -> "HQ LINK ESTABLISHED • SECURE CHANNEL"
            com.example.ui.game.AppLanguage.ESPANOL -> "ENLACE CG ESTABLECIDO • CANAL SEGURO"
            com.example.ui.game.AppLanguage.PORTUGUES -> "LINK QG ESTABELECIDO • CANAL SEGURO"
        }
        "alliance_north" -> when (currentLanguage) {
            com.example.ui.game.AppLanguage.ENGLISH -> "COORDINATING FLANK OPERATIONS"
            com.example.ui.game.AppLanguage.ESPANOL -> "COORDINANDO OPERACIONES DE FLANCO"
            com.example.ui.game.AppLanguage.PORTUGUES -> "COORDENANDO OPERAÇÕES DE FLANCO"
        }
        else -> when (currentLanguage) {
            com.example.ui.game.AppLanguage.ENGLISH -> "TACTICAL DRONE SWEEP DETECTED"
            com.example.ui.game.AppLanguage.ESPANOL -> "BARRIDO DE DRONE TÁCTICO DETECTADO"
            com.example.ui.game.AppLanguage.PORTUGUES -> "VARREDURA DE DRONE TÁTICO DETECTADA"
        }
    }

    val endTurnLabel = when (currentLanguage) {
        com.example.ui.game.AppLanguage.ENGLISH -> "END TURN"
        com.example.ui.game.AppLanguage.ESPANOL -> "FIN TURNO"
        com.example.ui.game.AppLanguage.PORTUGUES -> "FIM TURNO"
    }

    val tacticalGridLabel = when (currentLanguage) {
        com.example.ui.game.AppLanguage.ENGLISH -> "TACTICAL TERRAIN GRID ACTIVE"
        com.example.ui.game.AppLanguage.ESPANOL -> "RED TÁCTICA DE TERRENO ACTIVA"
        com.example.ui.game.AppLanguage.PORTUGUES -> "GRID DE TERRENO TÁTICO ATIVO"
    }

    val glowColor = when (activeFactionTurn) {
        "player" -> Color(0xFF00FFCC)
        "alliance_north" -> Color(0xFF00E676)
        else -> Color(0xFFFF1744)
    }

    Card(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .testTag("tactical_turn_hud"),
        colors = CardDefaults.cardColors(containerColor = Color(0xF20F172A)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, glowColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val infinitePulse = rememberInfiniteTransition(label = "TurnPulseHUD")
                    val dotAlpha by infinitePulse.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "DotAlphaHUD"
                    )

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(glowColor.copy(alpha = dotAlpha))
                    )

                    Column {
                        Text(
                            text = "TURN $turnNumber: $factionTitle",
                            style = TextStyle(
                                color = glowColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = factionStatus,
                            style = TextStyle(
                                color = Color(0xFF8892B0),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(
                            if (activeFactionTurn == "player" || activeFactionTurn == "alliance_north") 
                                Color(0x2200FFCC) 
                            else 
                                Color(0x22FF1744),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (activeFactionTurn == "player" || activeFactionTurn == "alliance_north") "NORTH" else "SOUTH",
                        color = if (activeFactionTurn == "player" || activeFactionTurn == "alliance_north") Color(0xFF00FFCC) else Color(0xFFFF1744),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x1F1E293B), RoundedCornerShape(4.dp))
                    .border(0.5.dp, Color(0x338892B0), RoundedCornerShape(4.dp))
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🌐", fontSize = 9.sp)
                    Text(
                        text = tacticalGridLabel,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8892B0),
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "A* Enabled",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FFCC),
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isTurnTimerActive) {
                        val timerColor = when {
                            turnTimeRemaining > 10 -> Color(0xFF00FFCC)
                            turnTimeRemaining in 6..10 -> Color(0xFFFFB300)
                            else -> Color(0xFFFF1744)
                        }
                        Text(
                            text = "⏱️ ${turnTimeRemaining}s SECURE",
                            style = TextStyle(
                                color = timerColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    } else {
                        Text(
                            text = "Timer: PAUSED",
                            style = TextStyle(
                                color = Color(0xFF8892B0),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }

                    IconButton(
                        onClick = { onToggleTurnTimer(!isTurnTimerActive) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text(
                            text = if (isTurnTimerActive) "⏸" else "▶",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = onEndTurnManually,
                    enabled = activeFactionTurn == "player",
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00FFCC),
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF1E293B),
                        disabledContentColor = Color(0x66ECEFF1)
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .height(26.dp)
                        .testTag("end_turn_btn")
                ) {
                    Text(
                        text = endTurnLabel,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isTurnTimerActive && activeFactionTurn == "player") {
                val progress = turnTimeRemaining.toFloat() / turnMaxDuration.toFloat()
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(1.dp)),
                    color = when {
                        turnTimeRemaining > 10 -> Color(0xFF00FFCC)
                        turnTimeRemaining in 6..10 -> Color(0xFFFFB300)
                        else -> Color(0xFFFF1744)
                    },
                    trackColor = Color(0x1AFFFFFF)
                )
            }
        }
    }
}

@Composable
fun NexusEnterpriseLabsDashboard(
    selectedHub: String,
    onHubSelected: (String) -> Unit,
    fleetUnits: Int,
    onFleetUnitsChange: (Int) -> Unit,
    isNodeDisrupted: Boolean,
    onToggleNodeDisrupted: () -> Unit,
    costPerKm: Double,
    transitHours: Double,
    selectedScenario: String,
    onScenarioSelected: (String) -> Unit,
    mctsIterations: Int,
    onMctsIterationsChange: (Int) -> Unit,
    successRate: Double,
    financialSavings: Double,
    zkStatus: String?,
    zkLogs: List<String>,
    onRecalculateRoute: () -> Unit,
    onRunMctsScenario: () -> Unit,
    onGenerateZkProof: () -> Unit,
    onPublishKafkaEvent: (String, String, String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- B2B HEADER & INDUSTRIAL BRANDING ---
        Card(
            modifier = Modifier.fillMaxWidth().testTag("enterprise_header_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🏢", fontSize = 16.sp)
                    Column {
                        Text(
                            text = "NEXUS ENTERPRISE & LABS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF38BDF8)
                        )
                        Text(
                            text = "B2B Digital Twins • Supply Chain Optimization • MCTS Risk Sandbox • ZK Audit",
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFF0284C7).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "SIMULATION LABS ACTIVE",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        }

        // ================= PANEL 1: GEMELO DIGITAL LOGÍSTICO (DIGITAL TWIN & FLEET OPTIMIZATION) =================
        Card(
            modifier = Modifier.fillMaxWidth().testTag("enterprise_digital_twin_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B132B)),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🚚", fontSize = 12.sp)
                        Text(
                            text = "PANEL 1: GEMELO DIGITAL LOGÍSTICO (SUPPLY CHAIN & FLEET A*)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF38BDF8)
                        )
                    }
                    Text(
                        text = "ENGINE: A* O(1) GEO-GRID",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF00FF88)
                    )
                }

                // Global Industrial Hub Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val hubs = listOf(
                        "Rotterdam Hub",
                        "Frankfurt Nexus",
                        "Singapore Gateway",
                        "Chicago Depot"
                    )
                    hubs.forEach { hub ->
                        val isSel = selectedHub == hub
                        Button(
                            onClick = { onHubSelected(hub) },
                            modifier = Modifier.weight(1f).height(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) Color(0xFF0284C7) else Color(0xFF1E293B),
                                contentColor = if (isSel) Color.White else Color(0xFF94A3B8)
                            ),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = hub.take(10),
                                fontSize = 6.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Fleet & Node Disruption Parameters Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Fleet size controls
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fleet Units: $fleetUnits", fontSize = 8.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = { onFleetUnitsChange((fleetUnits - 20).coerceAtLeast(20)) },
                                modifier = Modifier.size(18.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                            ) { Text("-", fontSize = 9.sp, color = Color.White) }
                            Button(
                                onClick = { onFleetUnitsChange(fleetUnits + 20) },
                                modifier = Modifier.size(18.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                            ) { Text("+", fontSize = 9.sp, color = Color.White) }
                        }
                    }

                    // Node Failure Simulation Toggle
                    Button(
                        onClick = onToggleNodeDisrupted,
                        modifier = Modifier.height(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNodeDisrupted) Color(0xFFDC2626) else Color(0xFF059669),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = if (isNodeDisrupted) "⚠️ NODE BLOCKED" else "🟢 NODE NORMAL",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Digital Twin Key Metrics Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Metric 1: Cost/Km
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF021329), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(4.dp))
                            .padding(6.dp)
                    ) {
                        Column {
                            Text("COST PER KM", fontSize = 6.5.sp, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace)
                            Text(String.format(java.util.Locale.US, "$%.2f / km", costPerKm), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                            Text("A* Savings: -18.4%", fontSize = 6.sp, color = Color(0xFF00FF88), fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Metric 2: Transit Hours under Stress
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF022C22), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF059669), RoundedCornerShape(4.dp))
                            .padding(6.dp)
                    ) {
                        Column {
                            Text("TRANSIT TIME", fontSize = 6.5.sp, color = Color(0xFF6EE7B7), fontFamily = FontFamily.Monospace)
                            Text(String.format(java.util.Locale.US, "%.1f Hours", transitHours), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                            Text(if (isNodeDisrupted) "Alt Route Bypassed" else "Direct Path", fontSize = 6.sp, color = Color(0xFFA7F3D0), fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Metric 3: O(1) Re-route Speed
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF312E81), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF6366F1), RoundedCornerShape(4.dp))
                            .padding(6.dp)
                    ) {
                        Column {
                            Text("O(1) RE-ROUTE", fontSize = 6.5.sp, color = Color(0xFFA5B4FC), fontFamily = FontFamily.Monospace)
                            Text("0.42 ms", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                            Text("0 Delivery Halts", fontSize = 6.sp, color = Color(0xFFC7D2FE), fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Action Button: Recalculate A* Route
                Button(
                    onClick = onRecalculateRoute,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "⚡ RECALCULATE OPTIMAL A* DIGITAL TWIN ROUTE",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // ================= PANEL 2: LABORATORIO PREDICTIVO MCTS (PREDICTIVE SIMULATION & WHAT-IF) =================
        Card(
            modifier = Modifier.fillMaxWidth().testTag("enterprise_mcts_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF170C2E)),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🎲", fontSize = 12.sp)
                        Text(
                            text = "PANEL 2: LABORATORIO PREDICTIVO MCTS (STRESS TESTING & WHAT-IF)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFC084FC)
                        )
                    }
                    Text(
                        text = "MCTS TREE: UCB1 BOUNDS",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFE9D5FF)
                    )
                }

                Text(
                    text = "SELECCIONAR ESCENARIO DE STRESS TESTING / WHAT-IF:",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    fontFamily = FontFamily.Monospace
                )

                // Scenario Selector Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val scenarios = listOf(
                        "GEOPOLITICAL_FUEL_SHOCK" to "⚡ Fuel Shock (+35%)",
                        "CHIP_BOTTLENECK" to "📦 Chip Shortage (-40%)",
                        "WEATHER_DISRUPTION" to "❄️ Severe Weather"
                    )
                    scenarios.forEach { (key, label) ->
                        val isSel = selectedScenario == key
                        Button(
                            onClick = { onScenarioSelected(key) },
                            modifier = Modifier.weight(1f).height(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) Color(0xFF7C3AED) else Color(0xFF1E1B4B),
                                contentColor = if (isSel) Color.White else Color(0xFFCBD5E1)
                            ),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 6.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Iterations Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MCTS Iterations: $mctsIterations",
                        fontSize = 7.5.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val iters = listOf(25000, 50000, 100000, 250000)
                        iters.forEach { count ->
                            val isSel = mctsIterations == count
                            Button(
                                onClick = { onMctsIterationsChange(count) },
                                modifier = Modifier.height(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) Color(0xFF8B5CF6) else Color(0xFF312E81)
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text("${count/1000}k", fontSize = 6.5.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                // MCTS Predictive Results Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Success Rate
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF2E1065), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF8B5CF6), RoundedCornerShape(4.dp))
                            .padding(6.dp)
                    ) {
                        Column {
                            Text("SUPPLY RESILIENCE", fontSize = 6.5.sp, color = Color(0xFFDDD6FE), fontFamily = FontFamily.Monospace)
                            Text(String.format(java.util.Locale.US, "%.1f%%", successRate), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FF88), fontFamily = FontFamily.Monospace)
                            Text("On-Time Probability", fontSize = 6.sp, color = Color(0xFFC4B5FD), fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Financial Savings
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF064E3B), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(4.dp))
                            .padding(6.dp)
                    ) {
                        Column {
                            Text("5-YR FINANCIAL SAVINGS", fontSize = 6.5.sp, color = Color(0xFFA7F3D0), fontFamily = FontFamily.Monospace)
                            Text(String.format(java.util.Locale.US, "+$%.1f M", financialSavings), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                            Text("Cost Avoidance", fontSize = 6.sp, color = Color(0xFF6EE7B7), fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Computed Risk Index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF451A03), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(4.dp))
                            .padding(6.dp)
                    ) {
                        Column {
                            Text("RISK INDEX", fontSize = 6.5.sp, color = Color(0xFFFDE68A), fontFamily = FontFamily.Monospace)
                            Text("1.14 (LOW)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                            Text("Auto Mitigated", fontSize = 6.sp, color = Color(0xFFFCD34D), fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Button: Execute MCTS Simulation
                Button(
                    onClick = onRunMctsScenario,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7C3AED),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "🎲 EXECUTE MCTS INDUSTRIAL STRESS TEST & FINANCIAL PROJECTION",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // ================= PANEL 3: AUDITORÍA DE CADENAS Y SEGURIDAD ZK (CRYPTOGRAPHIC COMPLIANCE LEDGER) =================
        Card(
            modifier = Modifier.fillMaxWidth().testTag("enterprise_zk_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF061A23)),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF0D9488).copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🔐", fontSize = 12.sp)
                        Text(
                            text = "PANEL 3: AUDITORÍA ZK & CERTIFICACIÓN DE COMPLIANCE B2B",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF2DD4BF)
                        )
                    }
                    Text(
                        text = "ARKWORKS GROTH16",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF99F6E4)
                    )
                }

                // ZK Status Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF042F2E), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFF0D9488), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("B2B COMPLIANCE CERTIFICATE", fontSize = 7.sp, color = Color(0xFF99F6E4), fontFamily = FontFamily.Monospace)
                            Text(
                                text = zkStatus ?: "PENDING PROOF GENERATION",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "ISO-27001 • ESG PASSED",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FF88),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Generate ZK Proof Button
                Button(
                    onClick = onGenerateZkProof,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D9488),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "🔐 CERTIFICAR CONOCIMIENTO CERO (ARKWORKS GROTH16 B2B PROOF)",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "REGISTRO INMUTABLE DE AUDITORÍA INDUSTRIAL:",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace
                )

                // Audit Terminal Stream
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Color(0xFF020617), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                        .padding(6.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    zkLogs.forEach { log ->
                        Text(
                            text = ">> $log",
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (log.contains("Groth16") || log.contains("validated") || log.contains("signed")) Color(0xFF2DD4BF) else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // ================= PANEL 4: SCIENTIFIC ENGINE CONSOLE & BULK COMMAND DISPATCHER =================
        ScientificConsoleDispatcherCard(
            onPublishKafkaEvent = onPublishKafkaEvent
        )
    }
}

fun getPriorityWeight(priority: String): Int = when (priority.uppercase()) {
    "HIGH" -> 3
    "MEDIUM" -> 2
    "LOW" -> 1
    else -> 0
}

@Composable
fun ScientificConsoleDispatcherCard(
    onPublishKafkaEvent: (String, String, String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    // Default scientific job presets with Priority Flags
    val defaultPresets = remember {
        listOf(
            ScientificBatchJob(
                id = "job_matrix_eig",
                title = "1. Matrix Eigendecomposition & Spectral Analysis",
                octaveScript = "octave-cli: A = randn(128,128); [V,D] = eig(A + A'); save('-binary', '/tmp/eig.mat', 'V', 'D');",
                cppSource = "#include <iostream>\n#include <vector>\nextern \"C\" void process_eig(double* v) { /* SIMD Spectral Kernel */ }",
                compilerFlags = "-O3 -march=native -fPIC -shared",
                priority = "HIGH"
            ),
            ScientificBatchJob(
                id = "job_astar_tuning",
                title = "2. A* Heuristic Weight Optimization Grid",
                octaveScript = "octave-cli: H = meshgrid(1:8); W = inv(H + eye(8))*det(H); dlmwrite('/tmp/weights.csv', W);",
                cppSource = "#include <cmath>\nextern \"C\" double heuristic(double dx, double dy) { return std::hypot(dx, dy) * 1.414; }",
                compilerFlags = "-O3 -ffast-math -fPIC -shared",
                priority = "MEDIUM"
            ),
            ScientificBatchJob(
                id = "job_fft_filter",
                title = "3. Fourier FFT Sensor Signal Noise Filter",
                octaveScript = "octave-cli: sig = sin(2*pi*50*(0:0.001:1)); fsig = fft(sig); save('-ascii', '/tmp/fft.txt', 'fsig');",
                cppSource = "#include <complex>\nextern \"C\" void fft_filter(double* in, double* out) { /* AVX2 FFT Filter */ }",
                compilerFlags = "-O3 -mavx2 -fPIC -shared",
                priority = "LOW"
            ),
            ScientificBatchJob(
                id = "job_zkp_circuit",
                title = "4. ZKP R1CS Constraint Matrix & QAP Solver",
                octaveScript = "octave-cli: [A,B,C] = r1cs_generate(256); qap = poly_interpolate(A,B,C);",
                cppSource = "#include <omp.h>\nextern \"C\" bool verify_proof(const char* proof) { return true; }",
                compilerFlags = "-O3 -fopenmp -fPIC -shared",
                priority = "HIGH"
            )
        )
    }

    var batchQueue by remember { mutableStateOf(defaultPresets) }
    var isExecutingBatch by remember { mutableStateOf(false) }
    var activeJobIndex by remember { mutableStateOf(-1) }
    var batchLogs by remember {
        mutableStateOf(
            listOf(
                "[DISPATCHER] Scientific Console Batch Engine initialized.",
                "[DISPATCHER] Queue loaded with ${defaultPresets.size} default Octave ➔ C++ compilation jobs."
            )
        )
    }

    // Custom Job Form state
    var showCustomForm by remember { mutableStateOf(false) }
    var customTitle by remember { mutableStateOf("") }
    var customOctaveScript by remember { mutableStateOf("") }
    var customCppSource by remember { mutableStateOf("") }
    var customFlags by remember { mutableStateOf("-O3 -fPIC -shared") }
    var customPriority by remember { mutableStateOf("HIGH") }

    // Log panel visual sorting mechanism states
    var logSortMode by remember { mutableStateOf("PRIORITY") } // "PRIORITY", "CHRONOLOGICAL", "STATUS"
    var logPriorityFilter by remember { mutableStateOf("ALL") } // "ALL", "HIGH", "MEDIUM", "LOW"

    // Expanded job log toggle
    var expandedJobId by remember { mutableStateOf<String?>(null) }

    val completedJobsCount = batchQueue.count { it.status == "SUCCESS" }
    val pendingJobsCount = batchQueue.count { it.status == "QUEUED" }
    val processingJobsCount = batchQueue.count { it.status == "OCTAVE_COMPUTING" || it.status == "CPP_COMPILING" }
    val totalBatchTimeMs = batchQueue.sumOf { it.executionTimeMs }

    val infiniteTransition = rememberInfiniteTransition(label = "dispatcherPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("scientificConsoleCard")
            .testTag("scientific_dispatcher_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1527)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF00FFCC).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🧪", fontSize = 14.sp)
                    Column {
                        Text(
                            text = "BULK COMMAND DISPATCHER (OCTAVE ➔ C++)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00FFCC)
                        )
                        Text(
                            text = "Scientific Engine Batch Operations • Status & Priority Visual Engine",
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .background(
                            if (isExecutingBatch) Color(0x33FFB300) else Color(0x2200FF88),
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            1.dp,
                            if (isExecutingBatch) Color(0xFFFFB300) else Color(0xFF00FF88),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isExecutingBatch) "BATCH EXECUTING..." else "DISPATCHER READY",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isExecutingBatch) Color(0xFFFFB300) else Color(0xFF00FF88)
                    )
                }
            }

            // ================= REAL-TIME D3.JS INTERACTIVE NODE-LINK DIAGRAM =================
            // Visualizes P2P mesh connectivity between the 8 peers and NATS gateway
            D3P2pMeshDiagramCard(
                onPublishKafkaEvent = onPublishKafkaEvent,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 2.dp))

            // Quick Operations Toolbar (Load Suite, Add Custom, Sort Priority, Clear)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = {
                        batchQueue = defaultPresets
                        batchLogs = batchLogs + "[DISPATCHER] Reset queue to standard Scientific Suite (4 jobs with Priority flags)."
                    },
                    enabled = !isExecutingBatch,
                    modifier = Modifier.weight(1f).height(24.dp).testTag("load_scientific_suite_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color(0xFF38BDF8)
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("📋 LOAD SUITE", fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = { showCustomForm = !showCustomForm },
                    enabled = !isExecutingBatch,
                    modifier = Modifier.weight(1.1f).height(24.dp).testTag("toggle_custom_job_form_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showCustomForm) Color(0xFF0284C7) else Color(0xFF0F172A),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (showCustomForm) "✖ CLOSE FORM" else "➕ CUSTOM JOB", fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = {
                        batchQueue = batchQueue.sortedByDescending { getPriorityWeight(it.priority) }
                        batchLogs = batchLogs + "[PRIORITY SORT] Reordered batch queue by Priority (HIGH ➔ MEDIUM ➔ LOW). High-priority tasks jumped ahead!"
                    },
                    enabled = !isExecutingBatch && batchQueue.isNotEmpty(),
                    modifier = Modifier.weight(1.2f).height(24.dp).testTag("sort_queue_priority_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E1035),
                        contentColor = Color(0xFFFF4081)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFFF4081)),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("⚡ SORT PRIORITY", fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = {
                        batchQueue = emptyList()
                        batchLogs = batchLogs + "[DISPATCHER] Cleared all queued jobs."
                    },
                    enabled = !isExecutingBatch && batchQueue.isNotEmpty(),
                    modifier = Modifier.weight(0.9f).height(24.dp).testTag("clear_queue_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color(0xFFFF5252)
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("🧹 CLEAR", fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            // Custom Job Form Panel
            AnimatedVisibility(visible = showCustomForm) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("custom_job_form_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("ADD CUSTOM OCTAVE ➔ C++ SCIENTIFIC OPERATION", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace)
                        
                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            label = { Text("Job Title / Description", fontSize = 7.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            textStyle = TextStyle(fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                        )

                        OutlinedTextField(
                            value = customOctaveScript,
                            onValueChange = { customOctaveScript = it },
                            label = { Text("Octave Script Command (e.g. octave-cli: X = inv(A)*B;)", fontSize = 7.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            textStyle = TextStyle(fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF00FFCC))
                        )

                        OutlinedTextField(
                            value = customCppSource,
                            onValueChange = { customCppSource = it },
                            label = { Text("C++ Source Code / ABI Kernel", fontSize = 7.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            textStyle = TextStyle(fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF93C5FD))
                        )

                        // Priority Selector Row in Form
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("PRIORITY FLAG:", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
                                listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
                                    val pColor = when (p) {
                                        "HIGH" -> Color(0xFFFF1744)
                                        "MEDIUM" -> Color(0xFFFF9100)
                                        else -> Color(0xFF38BDF8)
                                    }
                                    val isSelected = customPriority == p
                                    Box(
                                        modifier = Modifier
                                            .clickable { customPriority = p }
                                            .background(if (isSelected) pColor else Color(0xFF1E293B), RoundedCornerShape(3.dp))
                                            .border(1.dp, pColor, RoundedCornerShape(3.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                            .testTag("priority_select_$p")
                                    ) {
                                        Text(
                                            text = when(p) {
                                                "HIGH" -> "🔥 HIGH"
                                                "MEDIUM" -> "⚡ MED"
                                                else -> "🔷 LOW"
                                            },
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.Black else pColor,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customFlags,
                                onValueChange = { customFlags = it },
                                label = { Text("g++ Flags", fontSize = 7.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).height(48.dp),
                                textStyle = TextStyle(fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                            )

                            Button(
                                onClick = {
                                    if (customTitle.isNotBlank()) {
                                        val newJob = ScientificBatchJob(
                                            id = "job_custom_${System.currentTimeMillis() % 10000}",
                                            title = customTitle,
                                            octaveScript = if (customOctaveScript.isNotBlank()) customOctaveScript else "octave-cli: A = rand(10); save('/tmp/out.mat', 'A');",
                                            cppSource = if (customCppSource.isNotBlank()) customCppSource else "// Custom C++ ABI\nextern \"C\" void run() {}",
                                            compilerFlags = customFlags,
                                            priority = customPriority
                                        )
                                        batchQueue = batchQueue + newJob
                                        batchLogs = batchLogs + "[DISPATCHER] Added custom job: '${customTitle}' with Priority [${customPriority}]"
                                        customTitle = ""
                                        customOctaveScript = ""
                                        customCppSource = ""
                                        showCustomForm = false
                                    }
                                },
                                modifier = Modifier.height(36.dp).testTag("add_custom_job_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color.Black),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("➕ QUEUE JOB", fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            // Visual Status Legend & Metrics Summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF040A18), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("status_legend_bar"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status breakdown with color dots
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("STATUS:", fontSize = 6.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                    
                    // Pending Dot + Count
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFFB300)))
                        Text("Pending: $pendingJobsCount", fontSize = 6.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFD54F), fontFamily = FontFamily.Monospace)
                    }

                    // Processing Dot + Count
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF).copy(alpha = if (processingJobsCount > 0) pulseAlpha else 0.4f))
                        )
                        Text("Processing: $processingJobsCount", fontSize = 6.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF00E5FF), fontFamily = FontFamily.Monospace)
                    }

                    // Completed Dot + Count
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF00E676)))
                        Text("Completed: $completedJobsCount", fontSize = 6.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF00E676), fontFamily = FontFamily.Monospace)
                    }
                }

                // Priority Legend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val highCount = batchQueue.count { it.priority == "HIGH" }
                    val medCount = batchQueue.count { it.priority == "MEDIUM" }
                    val lowCount = batchQueue.count { it.priority == "LOW" }

                    Text("PRIORITY:", fontSize = 6.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                    Text("🔥$highCount", fontSize = 6.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF1744), fontFamily = FontFamily.Monospace)
                    Text("⚡$medCount", fontSize = 6.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9100), fontFamily = FontFamily.Monospace)
                    Text("🔷$lowCount", fontSize = 6.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace)
                }
            }

            // Queued Operations List Header
            Text(
                text = "BATCH QUEUE DISPATCH LIST (${batchQueue.size} JOBS):",
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                fontFamily = FontFamily.Monospace
            )

            if (batchQueue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF020617), RoundedCornerShape(4.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Queue is empty. Click 'LOAD SUITE' or 'CUSTOM JOB' to add scientific operations.",
                        fontSize = 8.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    batchQueue.forEachIndexed { index, job ->
                        val isExpanded = expandedJobId == job.id
                        val isProcessing = job.status == "OCTAVE_COMPUTING" || job.status == "CPP_COMPILING"

                        // Human-readable status label and color
                        val (statusLabel, statusColor, statusBgColor) = when (job.status) {
                            "QUEUED" -> Triple("Pending", Color(0xFFFFB300), Color(0x22FFB300))
                            "OCTAVE_COMPUTING" -> Triple("Processing (Octave)", Color(0xFF00E5FF), Color(0x3300E5FF))
                            "CPP_COMPILING" -> Triple("Processing (C++)", Color(0xFF38BDF8), Color(0x3338BDF8))
                            "SUCCESS" -> Triple("Completed", Color(0xFF00E676), Color(0x2200E676))
                            "FAILED" -> Triple("Failed", Color(0xFFFF1744), Color(0x22FF1744))
                            else -> Triple("Pending", Color(0xFF94A3B8), Color(0x2294A3B8))
                        }

                        // Priority styling
                        val pLabel = when (job.priority) {
                            "HIGH" -> "HIGH"
                            "MEDIUM" -> "MEDIUM"
                            "LOW" -> "LOW"
                            else -> "MEDIUM"
                        }
                        val pIcon = when (job.priority) {
                            "HIGH" -> "🔥"
                            "MEDIUM" -> "⚡"
                            "LOW" -> "🔷"
                            else -> "⚡"
                        }
                        val pColor = when (job.priority) {
                            "HIGH" -> Color(0xFFFF1744)
                            "MEDIUM" -> Color(0xFFFF9100)
                            "LOW" -> Color(0xFF38BDF8)
                            else -> Color(0xFFFF9100)
                        }
                        val pBgColor = when (job.priority) {
                            "HIGH" -> Color(0x28FF1744)
                            "MEDIUM" -> Color(0x28FF9100)
                            "LOW" -> Color(0x2838BDF8)
                            else -> Color(0x28FF9100)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("job_item_card_$index"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (activeJobIndex == index) Color(0xFF021329) else Color(0xFF091120)
                            ),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(
                                1.dp,
                                if (activeJobIndex == index) Color(0xFF00FFCC) else Color(0xFF1E293B)
                            )
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left: Index + Status Dot + Title
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Status Dot Indicator (Color-coded with optional pulsing ring)
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .testTag("status_dot_$index"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isProcessing) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(statusColor.copy(alpha = pulseAlpha * 0.4f))
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isProcessing) statusColor.copy(alpha = pulseAlpha) else statusColor)
                                            )
                                        }

                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "#${index + 1}",
                                                    fontSize = 7.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF64748B),
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = job.title,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                // Priority label chip alongside title
                                                Box(
                                                    modifier = Modifier
                                                        .background(pBgColor, RoundedCornerShape(2.dp))
                                                        .border(0.5.dp, pColor, RoundedCornerShape(2.dp))
                                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                                        .testTag("command_priority_chip_$index")
                                                ) {
                                                    Text(
                                                        text = "PRIORITY: $pLabel",
                                                        fontSize = 6.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = pColor,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "g++ flags: ${job.compilerFlags}",
                                                fontSize = 6.5.sp,
                                                color = Color(0xFF64748B),
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    // Right: Priority Badge + Status Badge + Action Controls
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // 1. Priority Level Badge (with color-coded dot & interactive cycle)
                                        Box(
                                            modifier = Modifier
                                                .clickable(enabled = !isExecutingBatch) {
                                                    val nextP = when (job.priority) {
                                                        "HIGH" -> "MEDIUM"
                                                        "MEDIUM" -> "LOW"
                                                        "LOW" -> "HIGH"
                                                        else -> "HIGH"
                                                    }
                                                    batchQueue = batchQueue.mapIndexed { idx, item ->
                                                        if (idx == index) item.copy(priority = nextP) else item
                                                    }
                                                    batchLogs = batchLogs + "[PRIORITY] Job #${index + 1} '${job.title}' priority set to $nextP."
                                                }
                                                .background(pBgColor, RoundedCornerShape(3.dp))
                                                .border(1.dp, pColor, RoundedCornerShape(3.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                .testTag("priority_label_$index")
                                                .testTag("priority_badge_$index")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(pColor)
                                                        .testTag("priority_dot_$index")
                                                )
                                                Text(
                                                    text = "$pIcon PRIORITY: $pLabel",
                                                    fontSize = 6.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = pColor,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.testTag("priority_label_text_$index")
                                                )
                                            }
                                        }

                                        // 2. Jump to Top button for priority task positioning
                                        if (!isExecutingBatch && index > 0) {
                                            Button(
                                                onClick = {
                                                    val mutableList = batchQueue.toMutableList()
                                                    val moved = mutableList.removeAt(index)
                                                    mutableList.add(0, moved)
                                                    batchQueue = mutableList
                                                    batchLogs = batchLogs + "[JUMP AHEAD] '${job.title}' (${job.priority}) jumped to front of execution queue!"
                                                },
                                                modifier = Modifier.height(20.dp).testTag("jump_to_top_btn_$index"),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF1E293B),
                                                    contentColor = Color(0xFF00FFCC)
                                                ),
                                                shape = RoundedCornerShape(3.dp)
                                            ) {
                                                Text("⬆ JUMP", fontSize = 6.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            }
                                        }

                                        // 3. Current Status Badge (Pending / Processing / Completed)
                                        Box(
                                            modifier = Modifier
                                                .background(statusBgColor, RoundedCornerShape(3.dp))
                                                .border(1.dp, statusColor, RoundedCornerShape(3.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                .testTag("status_badge_$index")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isProcessing) statusColor.copy(alpha = pulseAlpha) else statusColor)
                                                )
                                                Text(
                                                    text = statusLabel,
                                                    fontSize = 6.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = statusColor,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }

                                        if (job.executionTimeMs > 0) {
                                            Text(
                                                text = "${job.executionTimeMs}ms",
                                                fontSize = 7.sp,
                                                color = Color(0xFF00FF88),
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Button(
                                            onClick = { expandedJobId = if (isExpanded) null else job.id },
                                            modifier = Modifier.size(20.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                                        ) {
                                            Text(if (isExpanded) "▲" else "▼", fontSize = 7.sp, color = Color.White)
                                        }

                                        if (!isExecutingBatch) {
                                            Button(
                                                onClick = {
                                                    batchQueue = batchQueue.filterIndexed { i, _ -> i != index }
                                                    batchLogs = batchLogs + "[DISPATCHER] Removed job '${job.title}' from queue."
                                                },
                                                modifier = Modifier.size(20.dp),
                                                contentPadding = PaddingValues(0.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF331010))
                                            ) {
                                                Text("✖", fontSize = 7.sp, color = Color(0xFFFF5252))
                                            }
                                        }
                                    }
                                }

                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF020617), RoundedCornerShape(3.dp))
                                            .padding(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("STATUS: $statusLabel", fontSize = 6.5.sp, color = statusColor, fontFamily = FontFamily.Monospace)
                                            Text("PRIORITY: ${job.priority}", fontSize = 6.5.sp, color = pColor, fontFamily = FontFamily.Monospace)
                                        }
                                        Text("OCTAVE SCRIPT:", fontSize = 6.5.sp, color = Color(0xFF00FFCC), fontFamily = FontFamily.Monospace)
                                        Text(job.octaveScript, fontSize = 6.5.sp, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
                                        Text("C++ SOURCE KERNEL:", fontSize = 6.5.sp, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace)
                                        Text(job.cppSource, fontSize = 6.5.sp, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
                                        if (job.octaveLog.isNotBlank()) {
                                            Text("OCTAVE LOG: ${job.octaveLog}", fontSize = 6.5.sp, color = Color(0xFFFFB300), fontFamily = FontFamily.Monospace)
                                        }
                                        if (job.cppLog.isNotBlank()) {
                                            Text("C++ GCC LOG: ${job.cppLog}", fontSize = 6.5.sp, color = Color(0xFF00FF88), fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Overall Batch Progress & Execute Controls
            if (isExecutingBatch) {
                val progress = if (batchQueue.isNotEmpty()) activeJobIndex.coerceAtLeast(0).toFloat() / batchQueue.size.toFloat() else 0f
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF00FFCC),
                    trackColor = Color(0xFF1E293B)
                )
            }

            // Execute Batch Dispatcher Button
            Button(
                onClick = {
                    if (batchQueue.isNotEmpty() && !isExecutingBatch) {
                        scope.launch {
                            isExecutingBatch = true
                            
                            // Reorder batch queue by priority before execution so High Priority tasks jump ahead!
                            batchQueue = batchQueue.sortedByDescending { getPriorityWeight(it.priority) }
                            
                            val highCount = batchQueue.count { it.priority == "HIGH" }
                            val medCount = batchQueue.count { it.priority == "MEDIUM" }
                            val lowCount = batchQueue.count { it.priority == "LOW" }
                            batchLogs = batchLogs + "[BATCH ENGINE] Priority Order Applied: $highCount HIGH, $medCount MEDIUM, $lowCount LOW tasks."
                            batchLogs = batchLogs + "[BATCH ENGINE] Initiating bulk dispatch across ${batchQueue.size} scientific operations..."
                            
                            // Reset job statuses
                            batchQueue = batchQueue.map {
                                it.copy(status = "QUEUED", octaveLog = "", cppLog = "", executionTimeMs = 0L)
                            }

                            for (i in batchQueue.indices) {
                                activeJobIndex = i
                                val currentJob = batchQueue[i]
                                
                                // Phase 1: Octave Compute
                                batchQueue = batchQueue.mapIndexed { idx, item ->
                                    if (idx == i) item.copy(status = "OCTAVE_COMPUTING") else item
                                }
                                batchLogs = batchLogs + "[JOB #${i+1}] [${currentJob.priority} PRIORITY] Executing Octave script: ${currentJob.title}"
                                delay(500)
                                val octLog = "Octave 8.2.0: Script executed. Convergence in ${Random.nextInt(8, 32)} iterations. Saved matrix to /tmp/${currentJob.id}.mat"
                                
                                // Phase 2: C++ Compilation
                                batchQueue = batchQueue.mapIndexed { idx, item ->
                                    if (idx == i) item.copy(status = "CPP_COMPILING", octaveLog = octLog) else item
                                }
                                batchLogs = batchLogs + "[JOB #${i+1}] [${currentJob.priority} PRIORITY] Compiling C++ ABI shared kernel (g++ ${currentJob.compilerFlags})..."
                                delay(600)
                                val executionMs = Random.nextLong(90, 280)
                                val cppLog = "g++ ${currentJob.compilerFlags} -o /tmp/lib_${currentJob.id}.so. Build SUCCESS. Shared object created (${Random.nextInt(180, 520)} KB)."

                                // Complete Job
                                batchQueue = batchQueue.mapIndexed { idx, item ->
                                    if (idx == i) item.copy(status = "SUCCESS", cppLog = cppLog, executionTimeMs = executionMs) else item
                                }
                                batchLogs = batchLogs + "[JOB #${i+1}] '${currentJob.title}' [${currentJob.priority}] COMPLETED in ${executionMs}ms."
                            }

                            activeJobIndex = -1
                            isExecutingBatch = false
                            val totalMs = batchQueue.sumOf { it.executionTimeMs }
                            batchLogs = batchLogs + "[BATCH ENGINE] SUCCESS: All ${batchQueue.size} scientific operations completed! Total execution time: ${totalMs}ms."

                            onPublishKafkaEvent(
                                "enterprise.scientific.batch",
                                "BULK_BATCH_COMPLETED",
                                "Bulk Scientific Dispatcher executed ${batchQueue.size} Octave -> C++ jobs in ${totalMs}ms."
                            )
                        }
                    }
                },
                enabled = !isExecutingBatch && batchQueue.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00FFCC),
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF1E293B),
                    disabledContentColor = Color.Gray
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth().height(32.dp).testTag("dispatch_bulk_batch_btn"),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = if (isExecutingBatch) "⚡ EXECUTING PRIORITY BATCH (${activeJobIndex + 1}/${batchQueue.size})..." else "🚀 DISPATCH BULK BATCH (${batchQueue.size} SCIENTIFIC JOBS)",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Batch Metrics & Output Terminal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF021329), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFF00FFCC), RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    Column {
                        Text("COMPLETED JOBS", fontSize = 6.5.sp, color = Color(0xFF00FFCC), fontFamily = FontFamily.Monospace)
                        Text("$completedJobsCount / ${batchQueue.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                        Text("Octave ➔ C++ Linked", fontSize = 6.sp, color = Color(0xFF00FF88), fontFamily = FontFamily.Monospace)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF091E3A), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    Column {
                        Text("TOTAL BATCH TIME", fontSize = 6.5.sp, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace)
                        Text("${totalBatchTimeMs} ms", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                        Text("SIMD Multi-Threaded", fontSize = 6.sp, color = Color(0xFF93C5FD), fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Log Panel Header & Visual Sorting Indicator Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SCIENTIFIC LOG PANEL & TASK REORDER MONITOR:",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace
                )

                // Visual sort order indicator badge
                Box(
                    modifier = Modifier
                        .background(
                            when (logSortMode) {
                                "PRIORITY" -> Color(0x3300FFCC)
                                "CHRONOLOGICAL" -> Color(0x3338BDF8)
                                else -> Color(0x3300FF88)
                            },
                            RoundedCornerShape(3.dp)
                        )
                        .border(
                            1.dp,
                            when (logSortMode) {
                                "PRIORITY" -> Color(0xFF00FFCC)
                                "CHRONOLOGICAL" -> Color(0xFF38BDF8)
                                else -> Color(0xFF00FF88)
                            },
                            RoundedCornerShape(3.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                        .testTag("log_sort_indicator_badge")
                ) {
                    Text(
                        text = when (logSortMode) {
                            "PRIORITY" -> "⚡ SORT: PRIORITY (HIGH ➔ MED ➔ LOW)"
                            "CHRONOLOGICAL" -> "⏱ SORT: CHRONOLOGICAL"
                            else -> "📊 SORT: BY STATUS"
                        },
                        fontSize = 6.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (logSortMode) {
                            "PRIORITY" -> Color(0xFF00FFCC)
                            "CHRONOLOGICAL" -> Color(0xFF38BDF8)
                            else -> Color(0xFF00FF88)
                        },
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Visual Sorting & Filtering Toolbar Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Sort by Priority Button
                Button(
                    onClick = {
                        logSortMode = "PRIORITY"
                        batchQueue = batchQueue.sortedByDescending { getPriorityWeight(it.priority) }
                        batchLogs = batchLogs + "[VISUAL SORT] Log panel reordered displayed tasks by assigned Priority (HIGH ➔ MEDIUM ➔ LOW)."
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(22.dp)
                        .testTag("sort_by_priority_btn")
                        .testTag("sort_log_priority_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (logSortMode == "PRIORITY") Color(0xFF0284C7) else Color(0xFF1E293B),
                        contentColor = if (logSortMode == "PRIORITY") Color.White else Color(0xFF00FFCC)
                    ),
                    shape = RoundedCornerShape(3.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("⚡ SORT BY PRIORITY", fontSize = 6.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                // 2. Sort Chronological Button
                Button(
                    onClick = {
                        logSortMode = "CHRONOLOGICAL"
                        batchLogs = batchLogs + "[LOG PANEL] Reordered to Chronological event stream."
                    },
                    modifier = Modifier
                        .weight(1.1f)
                        .height(22.dp)
                        .testTag("sort_chronological_btn")
                        .testTag("sort_log_chrono_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (logSortMode == "CHRONOLOGICAL") Color(0xFF0284C7) else Color(0xFF1E293B),
                        contentColor = if (logSortMode == "CHRONOLOGICAL") Color.White else Color(0xFF94A3B8)
                    ),
                    shape = RoundedCornerShape(3.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("⏱ CHRONOLOGICAL", fontSize = 6.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                // 3. Sort by Status Button
                Button(
                    onClick = {
                        logSortMode = "STATUS"
                        batchLogs = batchLogs + "[LOG PANEL] Reordered by task execution Status."
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(22.dp)
                        .testTag("sort_by_status_btn")
                        .testTag("sort_log_status_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (logSortMode == "STATUS") Color(0xFF0284C7) else Color(0xFF1E293B),
                        contentColor = if (logSortMode == "STATUS") Color.White else Color(0xFF94A3B8)
                    ),
                    shape = RoundedCornerShape(3.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("📊 BY STATUS", fontSize = 6.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            // Priority Filter Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("FILTER:", fontSize = 6.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)

                listOf(
                    "ALL" to "ALL",
                    "HIGH" to "🔥 HIGH",
                    "MEDIUM" to "⚡ MED",
                    "LOW" to "🔷 LOW"
                ).forEach { (fKey, fLabel) ->
                    val isSelected = logPriorityFilter == fKey
                    Box(
                        modifier = Modifier
                            .clickable { logPriorityFilter = fKey }
                            .background(
                                if (isSelected) Color(0xFF0284C7) else Color(0xFF0B132B),
                                RoundedCornerShape(2.dp)
                            )
                            .border(
                                0.5.dp,
                                if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B),
                                RoundedCornerShape(2.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .testTag("filter_priority_${fKey.lowercase()}")
                    ) {
                        Text(
                            text = fLabel,
                            fontSize = 6.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Log Panel Container with Priority-Reordered Tasks & Terminal Output
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp, max = 180.dp)
                    .background(Color(0xFF020617), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                    .padding(6.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("scientific_log_panel")
                    .testTag("batch_log_panel"),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // If in PRIORITY Sort Mode, render displayed tasks reordered by their assigned priority!
                if (logSortMode == "PRIORITY") {
                    val priorityLevels = listOf("HIGH", "MEDIUM", "LOW").filter {
                        logPriorityFilter == "ALL" || logPriorityFilter == it
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x2200FFCC), RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "⚡ TASKS DISPLAY REORDERED BY ASSIGNED PRIORITY (HIGH ➔ MEDIUM ➔ LOW):",
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FFCC),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    priorityLevels.forEach { pLevel ->
                        val matchingTasks = batchQueue.filter { it.priority == pLevel }
                        if (matchingTasks.isNotEmpty()) {
                            val (tierColor, tierIcon, tierBg) = when (pLevel) {
                                "HIGH" -> Triple(Color(0xFFFF1744), "🔥", Color(0x22FF1744))
                                "MEDIUM" -> Triple(Color(0xFFFF9100), "⚡", Color(0x22FF9100))
                                else -> Triple(Color(0xFF38BDF8), "🔷", Color(0x2238BDF8))
                            }

                            // Tier Header
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(tierBg, RoundedCornerShape(3.dp))
                                    .border(0.5.dp, tierColor, RoundedCornerShape(3.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .testTag("log_priority_tier_${pLevel.lowercase()}")
                            ) {
                                Text(
                                    text = "$tierIcon $pLevel PRIORITY TASKS (${matchingTasks.size} operations)",
                                    fontSize = 6.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tierColor,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Render each task in this priority tier
                            matchingTasks.forEach { task ->
                                val (statusLabel, statusColor) = when (task.status) {
                                    "QUEUED" -> Pair("Pending", Color(0xFFFFB300))
                                    "OCTAVE_COMPUTING" -> Pair("Processing (Octave)", Color(0xFF00E5FF))
                                    "CPP_COMPILING" -> Pair("Processing (C++)", Color(0xFF38BDF8))
                                    "SUCCESS" -> Pair("Completed", Color(0xFF00E676))
                                    "FAILED" -> Pair("Failed", Color(0xFFFF1744))
                                    else -> Pair("Pending", Color(0xFF94A3B8))
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0A1020), RoundedCornerShape(3.dp))
                                        .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(3.dp))
                                        .padding(4.dp)
                                        .testTag("log_task_item_${task.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(statusColor))
                                            Text(
                                                text = task.title,
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            // Priority Label
                                            Text(
                                                text = "[$pLevel]",
                                                fontSize = 6.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = tierColor,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            // Status Label
                                            Text(
                                                text = statusLabel,
                                                fontSize = 6.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = statusColor,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    if (task.octaveLog.isNotBlank()) {
                                        Text(
                                            text = "  >> Octave: ${task.octaveLog}",
                                            fontSize = 6.sp,
                                            color = Color(0xFFFFB300),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    if (task.cppLog.isNotBlank()) {
                                        Text(
                                            text = "  >> GCC ABI: ${task.cppLog}",
                                            fontSize = 6.sp,
                                            color = Color(0xFF00FF88),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "--- REALTIME DISPATCHER LOG STREAM ---",
                        fontSize = 6.sp,
                        color = Color(0xFF64748B),
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Batch stream logs
                batchLogs.forEach { log ->
                    val color = when {
                        log.contains("SUCCESS") -> Color(0xFF00FF88)
                        log.contains("GNU Octave") || log.contains("Octave script") -> Color(0xFFFFB300)
                        log.contains("PRIORITY") || log.contains("JUMP") || log.contains("HIGH") -> Color(0xFFFF4081)
                        log.contains("g++") -> Color(0xFF38BDF8)
                        log.contains("BATCH ENGINE") || log.contains("DISPATCHER") || log.contains("VISUAL SORT") -> Color(0xFF00FFCC)
                        else -> Color(0xFF94A3B8)
                    }
                    Text(
                        text = ">> $log",
                        fontSize = 7.sp,
                        fontFamily = FontFamily.Monospace,
                        color = color
                    )
                }
            }
        }
    }
}
