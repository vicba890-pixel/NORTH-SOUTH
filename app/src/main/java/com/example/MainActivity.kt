package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.game.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    GameCockpitScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun GameCockpitScreen(
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = viewModel()
) {
    // Read States from ViewModel
    val regions by viewModel.regions.collectAsState()
    val techs by viewModel.techs.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val campaignSaves by viewModel.campaignSaves.collectAsState()
    val zkpMoves by viewModel.zkpMoves.collectAsState()

    var selectedRegion: com.example.data.RegionEntity? by remember { mutableStateOf(null) }

    // Synchronize selectedRegion back to any updates from the DB list
    val activeSelectedRegion = regions.find { it.id == selectedRegion?.id } ?: regions.firstOrNull()

    // Selection state for Tech Detail panel
    var selectedTech by remember { mutableStateOf<com.example.data.TechEntity?>(null) }
    if (selectedTech == null && techs.isNotEmpty()) {
        selectedTech = techs.firstOrNull()
    }
    val activeSelectedTech = techs.find { it.id == selectedTech?.id } ?: techs.firstOrNull()

    // Admiral mode toggle (for desktop/expanded screen sizes)
    var isAdmiralMode by remember { mutableStateOf(true) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030712)) // Pure deep space background
    ) {
        val width = maxWidth
        val isCompact = width < 600.dp
        val isMedium = width >= 600.dp && width < 1000.dp
        val isExpanded = width >= 1000.dp

        if (isCompact) {
            // COMPACT (MOBILE) LAYOUT PHONE VIEWPORT
            Column(modifier = Modifier.fillMaxSize()) {
                // Compact top telemetry header
                TopTelemetryHeader(
                    credits = viewModel.playerCredits,
                    alloys = viewModel.playerAlloys,
                    population = viewModel.globalPopulation,
                    pingMs = viewModel.pingMs,
                    currentLanguage = viewModel.currentLanguage,
                    onLanguageChange = { viewModel.currentLanguage = it },
                    isLobbyJoined = viewModel.isLobbyJoined,
                    roomId = viewModel.serverRoomId,
                    gold = viewModel.playerGold,
                    food = viewModel.playerFood,
                    manpower = viewModel.playerManpower,
                    isCompact = true
                )

                // Tab display content
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)) {
                    AnimatedContent(
                        targetState = viewModel.selectedTab,
                        transitionSpec = {
                            fadeIn(tween(180)) togetherWith fadeOut(tween(160))
                        },
                        label = "MobileTabTransition"
                    ) { activeTab ->
                        when (activeTab) {
                            0 -> {
                                // Maps and chat logs stacked vertically
                                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    WorldMapAndZoom(
                                        regions = regions,
                                        selectedRegion = activeSelectedRegion,
                                        onRegionSelect = { selectedRegion = it },
                                        zoomScale = viewModel.zoomScale,
                                        onZoomScaleChange = { viewModel.zoomScale = it },
                                        mapOffsetX = viewModel.mapOffsetX,
                                        mapOffsetY = viewModel.mapOffsetY,
                                        onOffsetChange = { x, y ->
                                            viewModel.mapOffsetX = x
                                            viewModel.mapOffsetY = y
                                        },
                                        turnTimeRemaining = viewModel.turnTimeRemaining,
                                        turnMaxDuration = viewModel.turnMaxDuration,
                                        activeFactionTurn = viewModel.activeFactionTurn,
                                        isTurnTimerActive = viewModel.isTurnTimerActive,
                                        turnNumber = viewModel.turnNumber,
                                        onEndTurnManually = { viewModel.endPlayerTurnManually() },
                                        onToggleTurnTimer = { viewModel.isTurnTimerActive = it },
                                        currentLanguage = viewModel.currentLanguage,
                                        accelX = viewModel.sensorAccelX,
                                        accelY = viewModel.sensorAccelY,
                                        gyroZ = viewModel.sensorGyroZ,
                                        isSensorOverrideActive = viewModel.isSensorOverrideActive,
                                        onToggleSensorOverride = { viewModel.isSensorOverrideActive = it },
                                        onSaveMatchStats = { victories, defeats, totalRounds, averageLatencyMs, outcome ->
                                            viewModel.saveCombatResultToFirebase(victories, defeats, totalRounds, averageLatencyMs, outcome)
                                        },
                                        modifier = Modifier.fillMaxWidth().weight(1.2f)
                                    )

                                    ChatAndConquestTabs(
                                        regions = regions,
                                        logs = logs,
                                        playerCredits = viewModel.playerCredits,
                                        playerAlloys = viewModel.playerAlloys,
                                        chatInput = viewModel.chatInput,
                                        onChatInputChange = { viewModel.chatInput = it },
                                        onSendMessage = { viewModel.sendChatMessage() },
                                        onConquerRegion = { viewModel.conquerRegion(it) },
                                        onSelectRegionOnMap = {
                                            selectedRegion = it
                                            viewModel.mapOffsetX = -(it.longitude.toFloat() * 0.8f * viewModel.zoomScale)
                                            viewModel.mapOffsetY = (it.latitude.toFloat() * 0.8f * viewModel.zoomScale)
                                        },
                                        isLobbyJoined = viewModel.isLobbyJoined,
                                        commanderName = viewModel.commanderName,
                                        commanderFaction = viewModel.commanderFaction,
                                        serverRoomId = viewModel.serverRoomId,
                                        onJoinLobby = { name, faction, roomId ->
                                            viewModel.commanderName = name
                                            viewModel.commanderFaction = faction
                                            viewModel.serverRoomId = roomId
                                            viewModel.isLobbyJoined = true
                                        },
                                        onLeaveLobby = {
                                            viewModel.isLobbyJoined = false
                                        },
                                        currentLanguage = viewModel.currentLanguage,
                                        playerGold = viewModel.playerGold,
                                        playerFood = viewModel.playerFood,
                                        playerManpower = viewModel.playerManpower,
                                        goldGrowthRate = viewModel.goldGrowthRate,
                                        foodGrowthRate = viewModel.foodGrowthRate,
                                        manpowerGrowthRate = viewModel.manpowerGrowthRate,
                                        goldFocusCoeff = viewModel.goldFocusCoeff,
                                        foodFocusCoeff = viewModel.foodFocusCoeff,
                                        manpowerFocusCoeff = viewModel.manpowerFocusCoeff,
                                        onGoldCoeffChange = { viewModel.goldFocusCoeff = it },
                                        onFoodCoeffChange = { viewModel.foodFocusCoeff = it },
                                        onManpowerCoeffChange = { viewModel.manpowerFocusCoeff = it },
                                        onGenerateMap = { seed, count, dist, factor -> viewModel.generateMathematicalMap(seed, count, dist, factor) },
                                        modifier = Modifier.fillMaxWidth().weight(1f)
                                    )
                                }
                            }
                            1 -> {
                                ResearchLabTab(
                                    techs = techs,
                                    playerAlloys = viewModel.playerAlloys,
                                    onUnlockTech = { viewModel.unlockTechnology(it) },
                                    currentLanguage = viewModel.currentLanguage,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            2 -> {
                                SpaceLanderSimulator(
                                    altitude = viewModel.landerAltitude,
                                    speedY = viewModel.landerSpeedY,
                                    fuel = viewModel.landerFuel,
                                    isThrusting = viewModel.landerIsThrusting,
                                    status = viewModel.landerStatus,
                                    selectedLanderStyle = viewModel.selectedLanderStyle,
                                    onLanderStyleChange = { viewModel.selectedLanderStyle = it },
                                    shipWeight = viewModel.currentSpaceShipWeight,
                                    onShipWeightChange = { viewModel.currentSpaceShipWeight = it },
                                    targetRegion = activeSelectedRegion ?: regions.firstOrNull(),
                                    onStartLanding = {
                                        viewModel.startSpaceLanding(activeSelectedRegion?.id ?: 1)
                                    },
                                    onThrustChange = { viewModel.triggerLanderBooster(it) },
                                    currentLanguage = viewModel.currentLanguage,
                                    accelX = viewModel.sensorAccelX,
                                    accelY = viewModel.sensorAccelY,
                                    gyroX = viewModel.sensorGyroX,
                                    gyroY = viewModel.sensorGyroY,
                                    gyroZ = viewModel.sensorGyroZ,
                                    landerOffsetX = viewModel.landerOffsetX,
                                    landerSpeedX = viewModel.landerSpeedX,
                                    landerWindX = viewModel.landerWindX,
                                    isSensorOverrideActive = viewModel.isSensorOverrideActive,
                                    onToggleSensorOverride = { viewModel.isSensorOverrideActive = it },
                                    onManualSteerLeft = { viewModel.manualSteerLeft() },
                                    onManualSteerRight = { viewModel.manualSteerRight() },
                                    onReleaseSteer = { viewModel.releaseSteer() },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            3 -> {
                                SoundMixerTab(
                                    volume = viewModel.audioEngine.volume,
                                    onVolumeChange = { viewModel.audioEngine.volume = it },
                                    tempoMs = viewModel.audioEngine.tempoMs,
                                    onTempoChange = { viewModel.audioEngine.tempoMs = it },
                                    activeHarmonics = viewModel.audioEngine.activeHarmonics,
                                    onHarmonicsChange = { viewModel.audioEngine.activeHarmonics = it },
                                    theme = viewModel.audioEngine.theme,
                                    onThemeChange = { viewModel.audioEngine.theme = it },
                                    currentLanguage = viewModel.currentLanguage,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            4 -> {
                                LogicalMathAdvisor(
                                    globalPopulation = viewModel.globalPopulation,
                                    landerVelocityY = viewModel.landerSpeedY,
                                    activeVolume = viewModel.audioEngine.volume,
                                    playerCredits = viewModel.playerCredits,
                                    playerAlloys = viewModel.playerAlloys,
                                    regions = regions,
                                    techs = techs,
                                    advisorReport = viewModel.advisorReport,
                                    isGeneratingAdvisor = viewModel.isGeneratingAdvisor,
                                    advisorErrorMessage = viewModel.advisorErrorMessage,
                                    customAdvisorQuery = viewModel.customAdvisorQuery,
                                    onQueryChange = { viewModel.customAdvisorQuery = it },
                                    onGenerateReport = { viewModel.requestAiTacticalDirective(it) },
                                    currentLanguage = viewModel.currentLanguage,
                                    isHighThinkingModeEnabled = viewModel.isHighThinkingModeEnabled,
                                    onHighThinkingModeChange = { viewModel.isHighThinkingModeEnabled = it },
                                    isZkpEnabled = viewModel.isZkpEnabled,
                                    onZkpChange = { viewModel.isZkpEnabled = it },
                                    thinkingSteps = viewModel.thinkingSteps,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            5 -> {
                                com.example.ui.game.CampaignTab(
                                    campaignSaves = campaignSaves,
                                    onSave = { viewModel.saveCampaign(it) },
                                    onLoad = { viewModel.loadCampaign(it) },
                                    onDelete = { viewModel.deleteCampaignSave(it) },
                                    onResetCampaign = { viewModel.resetCampaign() },
                                    currentLanguage = viewModel.currentLanguage,
                                    firebaseUser = viewModel.firebaseUser,
                                    firebaseAuthError = viewModel.firebaseAuthError,
                                    firebaseAuthLoading = viewModel.firebaseAuthLoading,
                                    firebaseSyncLoading = viewModel.firebaseSyncLoading,
                                    firebaseSyncSuccessMessage = viewModel.firebaseSyncSuccessMessage,
                                    firebaseSyncErrorMessage = viewModel.firebaseSyncErrorMessage,
                                    firebaseProgressList = viewModel.firebaseProgressList,
                                    firebaseMatchStatsList = viewModel.firebaseMatchStatsList,
                                    isRealFirebaseActive = viewModel.firebaseManager.isRealFirebaseActive(),
                                    onRegisterUser = { email, pass -> viewModel.registerFirebaseUser(email, pass) },
                                    onLoginUser = { email, pass -> viewModel.loginFirebaseUser(email, pass) },
                                    onLogoutUser = { viewModel.logoutFirebaseUser() },
                                    onSaveToFirestore = { slot -> viewModel.saveTacticalProgressToFirebase(slot) },
                                    onRefreshFirebase = { viewModel.refreshFirebaseData() },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            6 -> {
                                com.example.ui.game.SecureCoreTab(
                                    currentLanguage = viewModel.currentLanguage,
                                    playerCredits = viewModel.playerCredits,
                                    playerAlloys = viewModel.playerAlloys,
                                    onAdjustCredits = { viewModel.playerCredits += it },
                                    onAddLog = { sender, msg, type -> viewModel.insertLog(sender, msg, type) },
                                    audioEngine = viewModel.audioEngine,
                                    isGameZkpEnforced = viewModel.isGameZkpEnforced,
                                    onGameZkpEnforcedChange = { viewModel.isGameZkpEnforced = it },
                                    zkpMoves = zkpMoves,
                                    zkpTamperWarning = viewModel.zkpTamperWarning,
                                    onDismissTamperWarning = { viewModel.zkpTamperWarning = null },
                                    onSimulateTamper = { viewModel.simulateTamperedTransition() },
                                    onClearZkpMoves = { viewModel.clearZkpVerificationLedger() },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            7 -> {
                                com.example.ui.game.PwaApkTab(
                                    currentLanguage = viewModel.currentLanguage,
                                    playerCredits = viewModel.playerCredits,
                                    playerAlloys = viewModel.playerAlloys,
                                    audioEngine = viewModel.audioEngine,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // Phone M3 Nav Bar
                PhoneNavigationBar(
                    selectedTab = viewModel.selectedTab,
                    onTabSelect = { viewModel.selectedTab = it },
                    currentLanguage = viewModel.currentLanguage,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // SIDE NAVIGATION ROW (FOR TABLETS AND DESKTOPS)
            Row(modifier = Modifier.fillMaxSize()) {
                if (isExpanded) {
                    DesktopSidebar(
                        selectedTab = viewModel.selectedTab,
                        onTabSelect = {
                            viewModel.selectedTab = it
                            isAdmiralMode = false
                        },
                        isAdmiralMode = isAdmiralMode,
                        onAdmiralModeChange = { isAdmiralMode = it },
                        currentLanguage = viewModel.currentLanguage
                    )
                } else {
                    TabletNavigationRail(
                        selectedTab = viewModel.selectedTab,
                        onTabSelect = { viewModel.selectedTab = it },
                        currentLanguage = viewModel.currentLanguage
                    )
                }

                // Main Content Workspace Column next to Navigation
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                ) {
                    // Top Telemetry Metrics
                    TopTelemetryHeader(
                        credits = viewModel.playerCredits,
                        alloys = viewModel.playerAlloys,
                        population = viewModel.globalPopulation,
                        pingMs = viewModel.pingMs,
                        currentLanguage = viewModel.currentLanguage,
                        onLanguageChange = { viewModel.currentLanguage = it },
                        isLobbyJoined = viewModel.isLobbyJoined,
                        roomId = viewModel.serverRoomId,
                        gold = viewModel.playerGold,
                        food = viewModel.playerFood,
                        manpower = viewModel.playerManpower,
                        isCompact = false
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Dashboard Area
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (isExpanded && isAdmiralMode) {
                            // Desktop Multi-Display Cockpit Console (Active)
                            UnifiedAdmiralCockpit(
                                regions = regions,
                                techs = techs,
                                logs = logs,
                                activeSelectedRegion = activeSelectedRegion,
                                onRegionSelect = { selectedRegion = it },
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // FOCUSED DASHBOARD WITH ADAPTIVE MULTI-PANE LAYOUTS
                            AnimatedContent(
                                targetState = viewModel.selectedTab,
                                transitionSpec = {
                                    fadeIn(tween(200)) togetherWith fadeOut(tween(180))
                                },
                                label = "DesktopTabTransition"
                            ) { activeTab ->
                                when (activeTab) {
                                    0 -> {
                                        // Tactical Map Screen (Dual pane map and telemetry card)
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            WorldMapAndZoom(
                                                regions = regions,
                                                selectedRegion = activeSelectedRegion,
                                                onRegionSelect = { selectedRegion = it },
                                                zoomScale = viewModel.zoomScale,
                                                onZoomScaleChange = { viewModel.zoomScale = it },
                                                mapOffsetX = viewModel.mapOffsetX,
                                                mapOffsetY = viewModel.mapOffsetY,
                                                onOffsetChange = { x, y ->
                                                    viewModel.mapOffsetX = x
                                                    viewModel.mapOffsetY = y
                                                },
                                                turnTimeRemaining = viewModel.turnTimeRemaining,
                                                turnMaxDuration = viewModel.turnMaxDuration,
                                                activeFactionTurn = viewModel.activeFactionTurn,
                                                isTurnTimerActive = viewModel.isTurnTimerActive,
                                                turnNumber = viewModel.turnNumber,
                                                onEndTurnManually = { viewModel.endPlayerTurnManually() },
                                                onToggleTurnTimer = { viewModel.isTurnTimerActive = it },
                                                currentLanguage = viewModel.currentLanguage,
                                                accelX = viewModel.sensorAccelX,
                                                accelY = viewModel.sensorAccelY,
                                                gyroZ = viewModel.sensorGyroZ,
                                                isSensorOverrideActive = viewModel.isSensorOverrideActive,
                                                onToggleSensorOverride = { viewModel.isSensorOverrideActive = it },
                                                onSaveMatchStats = { victories, defeats, totalRounds, averageLatencyMs, outcome ->
                                                    viewModel.saveCombatResultToFirebase(victories, defeats, totalRounds, averageLatencyMs, outcome)
                                                },
                                                modifier = Modifier
                                                    .weight(1.3f)
                                                    .fillMaxHeight()
                                            )

                                            ChatAndConquestTabs(
                                                regions = regions,
                                                logs = logs,
                                                playerCredits = viewModel.playerCredits,
                                                playerAlloys = viewModel.playerAlloys,
                                                chatInput = viewModel.chatInput,
                                                onChatInputChange = { viewModel.chatInput = it },
                                                onSendMessage = { viewModel.sendChatMessage() },
                                                onConquerRegion = { viewModel.conquerRegion(it) },
                                                onSelectRegionOnMap = {
                                                    selectedRegion = it
                                                    viewModel.mapOffsetX = -(it.longitude.toFloat() * 1.5f * viewModel.zoomScale)
                                                    viewModel.mapOffsetY = (it.latitude.toFloat() * 1.5f * viewModel.zoomScale)
                                                },
                                                isLobbyJoined = viewModel.isLobbyJoined,
                                                commanderName = viewModel.commanderName,
                                                commanderFaction = viewModel.commanderFaction,
                                                serverRoomId = viewModel.serverRoomId,
                                                onJoinLobby = { name, faction, roomId ->
                                                    viewModel.commanderName = name
                                                    viewModel.commanderFaction = faction
                                                    viewModel.serverRoomId = roomId
                                                    viewModel.isLobbyJoined = true
                                                },
                                                onLeaveLobby = {
                                                    viewModel.isLobbyJoined = false
                                                },
                                                currentLanguage = viewModel.currentLanguage,
                                                playerGold = viewModel.playerGold,
                                                playerFood = viewModel.playerFood,
                                                playerManpower = viewModel.playerManpower,
                                                goldGrowthRate = viewModel.goldGrowthRate,
                                                foodGrowthRate = viewModel.foodGrowthRate,
                                                manpowerGrowthRate = viewModel.manpowerGrowthRate,
                                                goldFocusCoeff = viewModel.goldFocusCoeff,
                                                foodFocusCoeff = viewModel.foodFocusCoeff,
                                                manpowerFocusCoeff = viewModel.manpowerFocusCoeff,
                                                onGoldCoeffChange = { viewModel.goldFocusCoeff = it },
                                                onFoodCoeffChange = { viewModel.foodFocusCoeff = it },
                                                onManpowerCoeffChange = { viewModel.manpowerFocusCoeff = it },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                            )
                                        }
                                    }
                                    1 -> {
                                        // Adaptive multi-pane Science Upgrade Tree
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(modifier = Modifier.weight(1.1f).fillMaxHeight()) {
                                                ResearchLabTab(
                                                    techs = techs,
                                                    playerAlloys = viewModel.playerAlloys,
                                                    onUnlockTech = { viewModel.unlockTechnology(it) },
                                                    currentLanguage = viewModel.currentLanguage,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }

                                            AdaptiveResearchPanel(
                                                selectedTech = activeSelectedTech,
                                                playerAlloys = viewModel.playerAlloys,
                                                onUnlockTech = { viewModel.unlockTechnology(it) },
                                                currentLanguage = viewModel.currentLanguage,
                                                modifier = Modifier.weight(0.9f).fillMaxHeight()
                                            )
                                        }
                                    }
                                    2 -> {
                                        // Adaptive Dual-Pane Lander Simulation
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(modifier = Modifier.weight(1.2f).fillMaxHeight()) {
                                                SpaceLanderSimulator(
                                                    altitude = viewModel.landerAltitude,
                                                    speedY = viewModel.landerSpeedY,
                                                    fuel = viewModel.landerFuel,
                                                    isThrusting = viewModel.landerIsThrusting,
                                                    status = viewModel.landerStatus,
                                                    selectedLanderStyle = viewModel.selectedLanderStyle,
                                                    onLanderStyleChange = { viewModel.selectedLanderStyle = it },
                                                    shipWeight = viewModel.currentSpaceShipWeight,
                                                    onShipWeightChange = { viewModel.currentSpaceShipWeight = it },
                                                    targetRegion = activeSelectedRegion ?: regions.firstOrNull(),
                                                    onStartLanding = {
                                                        viewModel.startSpaceLanding(activeSelectedRegion?.id ?: 1)
                                                    },
                                                    onThrustChange = { viewModel.triggerLanderBooster(it) },
                                                    currentLanguage = viewModel.currentLanguage,
                                                    accelX = viewModel.sensorAccelX,
                                                    accelY = viewModel.sensorAccelY,
                                                    gyroX = viewModel.sensorGyroX,
                                                    gyroY = viewModel.sensorGyroY,
                                                    gyroZ = viewModel.sensorGyroZ,
                                                    landerOffsetX = viewModel.landerOffsetX,
                                                    landerSpeedX = viewModel.landerSpeedX,
                                                    landerWindX = viewModel.landerWindX,
                                                    isSensorOverrideActive = viewModel.isSensorOverrideActive,
                                                    onToggleSensorOverride = { viewModel.isSensorOverrideActive = it },
                                                    onManualSteerLeft = { viewModel.manualSteerLeft() },
                                                    onManualSteerRight = { viewModel.manualSteerRight() },
                                                    onReleaseSteer = { viewModel.releaseSteer() },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }

                                            AdaptiveSpaceLanderTelemetryPanel(
                                                altitude = viewModel.landerAltitude,
                                                speedY = viewModel.landerSpeedY,
                                                fuel = viewModel.landerFuel,
                                                isThrusting = viewModel.landerIsThrusting,
                                                status = viewModel.landerStatus,
                                                selectedLanderStyle = viewModel.selectedLanderStyle,
                                                onLanderStyleChange = { viewModel.selectedLanderStyle = it },
                                                shipWeight = viewModel.currentSpaceShipWeight,
                                                onShipWeightChange = { viewModel.currentSpaceShipWeight = it },
                                                targetRegion = activeSelectedRegion ?: regions.firstOrNull(),
                                                onStartLanding = {
                                                    viewModel.startSpaceLanding(activeSelectedRegion?.id ?: 1)
                                                },
                                                onThrustChange = { viewModel.triggerLanderBooster(it) },
                                                currentLanguage = viewModel.currentLanguage,
                                                modifier = Modifier.weight(0.8f).fillMaxHeight()
                                            )
                                        }
                                    }
                                    3 -> {
                                        SoundMixerTab(
                                            volume = viewModel.audioEngine.volume,
                                            onVolumeChange = { viewModel.audioEngine.volume = it },
                                            tempoMs = viewModel.audioEngine.tempoMs,
                                            onTempoChange = { viewModel.audioEngine.tempoMs = it },
                                            activeHarmonics = viewModel.audioEngine.activeHarmonics,
                                            onHarmonicsChange = { viewModel.audioEngine.activeHarmonics = it },
                                            theme = viewModel.audioEngine.theme,
                                            onThemeChange = { viewModel.audioEngine.theme = it },
                                            currentLanguage = viewModel.currentLanguage,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    4 -> {
                                        LogicalMathAdvisor(
                                            globalPopulation = viewModel.globalPopulation,
                                            landerVelocityY = viewModel.landerSpeedY,
                                            activeVolume = viewModel.audioEngine.volume,
                                            playerCredits = viewModel.playerCredits,
                                            playerAlloys = viewModel.playerAlloys,
                                            regions = regions,
                                            techs = techs,
                                            advisorReport = viewModel.advisorReport,
                                            isGeneratingAdvisor = viewModel.isGeneratingAdvisor,
                                            advisorErrorMessage = viewModel.advisorErrorMessage,
                                            customAdvisorQuery = viewModel.customAdvisorQuery,
                                            onQueryChange = { viewModel.customAdvisorQuery = it },
                                            onGenerateReport = { viewModel.requestAiTacticalDirective(it) },
                                            currentLanguage = viewModel.currentLanguage,
                                            isHighThinkingModeEnabled = viewModel.isHighThinkingModeEnabled,
                                            onHighThinkingModeChange = { viewModel.isHighThinkingModeEnabled = it },
                                            isZkpEnabled = viewModel.isZkpEnabled,
                                            onZkpChange = { viewModel.isZkpEnabled = it },
                                            thinkingSteps = viewModel.thinkingSteps,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    5 -> {
                                        com.example.ui.game.CampaignTab(
                                            campaignSaves = campaignSaves,
                                            onSave = { viewModel.saveCampaign(it) },
                                            onLoad = { viewModel.loadCampaign(it) },
                                            onDelete = { viewModel.deleteCampaignSave(it) },
                                            onResetCampaign = { viewModel.resetCampaign() },
                                            currentLanguage = viewModel.currentLanguage,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    6 -> {
                                        com.example.ui.game.SecureCoreTab(
                                            currentLanguage = viewModel.currentLanguage,
                                            playerCredits = viewModel.playerCredits,
                                            playerAlloys = viewModel.playerAlloys,
                                            onAdjustCredits = { viewModel.playerCredits += it },
                                            onAddLog = { sender, msg, type -> viewModel.insertLog(sender, msg, type) },
                                            audioEngine = viewModel.audioEngine,
                                            isGameZkpEnforced = viewModel.isGameZkpEnforced,
                                            onGameZkpEnforcedChange = { viewModel.isGameZkpEnforced = it },
                                            zkpMoves = zkpMoves,
                                            zkpTamperWarning = viewModel.zkpTamperWarning,
                                            onDismissTamperWarning = { viewModel.zkpTamperWarning = null },
                                            onSimulateTamper = { viewModel.simulateTamperedTransition() },
                                            onClearZkpMoves = { viewModel.clearZkpVerificationLedger() },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    7 -> {
                                        com.example.ui.game.PwaApkTab(
                                            currentLanguage = viewModel.currentLanguage,
                                            playerCredits = viewModel.playerCredits,
                                            playerAlloys = viewModel.playerAlloys,
                                            audioEngine = viewModel.audioEngine,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhoneNavigationBar(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    currentLanguage: com.example.ui.game.AppLanguage,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.height(64.dp),
        containerColor = Color(0xFF0F172A),
        contentColor = Color(0xFF00E5FF)
    ) {
        val items = listOf(
            0 to "🗺️",
            1 to "🔬",
            2 to "🚀",
            3 to "📻",
            4 to "🧮",
            5 to "💾",
            6 to "🛡️",
            7 to "🌐"
        )
        items.forEach { (index, icon) ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelect(index) },
                icon = { Text(text = icon, fontSize = 16.sp) },
                label = {
                    val labelText = when (index) {
                        0 -> com.example.ui.game.LanguageUtils.translate("tab_map", currentLanguage)
                        1 -> com.example.ui.game.LanguageUtils.translate("tab_research", currentLanguage)
                        2 -> com.example.ui.game.LanguageUtils.translate("tab_space", currentLanguage)
                        3 -> com.example.ui.game.LanguageUtils.translate("tab_sound", currentLanguage)
                        4 -> com.example.ui.game.LanguageUtils.translate("tab_math", currentLanguage)
                        5 -> com.example.ui.game.LanguageUtils.translate("tab_campaign", currentLanguage)
                        6 -> com.example.ui.game.LanguageUtils.translate("tab_secure_core", currentLanguage)
                        else -> com.example.ui.game.LanguageUtils.translate("tab_pwa_apk", currentLanguage)
                    }
                    Text(text = labelText, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color(0xFF94A3B8),
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color(0xFF94A3B8),
                    indicatorColor = Color(0x3300E5FF)
                )
            )
        }
    }
}

@Composable
fun TabletNavigationRail(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    currentLanguage: com.example.ui.game.AppLanguage,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = Color(0xFF0F172A),
        contentColor = Color(0xFF00E5FF)
    ) {
        val items = listOf(
            0 to "🗺️",
            1 to "🔬",
            2 to "🚀",
            3 to "📻",
            4 to "🧮",
            5 to "💾",
            6 to "🛡️",
            7 to "🌐"
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        items.forEach { (index, icon) ->
            NavigationRailItem(
                selected = selectedTab == index,
                onClick = { onTabSelect(index) },
                icon = { Text(text = icon, fontSize = 18.sp) },
                label = {
                    val labelText = when (index) {
                        0 -> com.example.ui.game.LanguageUtils.translate("tab_map", currentLanguage)
                        1 -> com.example.ui.game.LanguageUtils.translate("tab_research", currentLanguage)
                        2 -> com.example.ui.game.LanguageUtils.translate("tab_space", currentLanguage)
                        3 -> com.example.ui.game.LanguageUtils.translate("tab_sound", currentLanguage)
                        4 -> com.example.ui.game.LanguageUtils.translate("tab_math", currentLanguage)
                        5 -> com.example.ui.game.LanguageUtils.translate("tab_campaign", currentLanguage)
                        6 -> com.example.ui.game.LanguageUtils.translate("tab_secure_core", currentLanguage)
                        else -> com.example.ui.game.LanguageUtils.translate("tab_pwa_apk", currentLanguage)
                    }
                    Text(text = labelText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color(0xFF94A3B8),
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color(0xFF94A3B8),
                    indicatorColor = Color(0x3300E5FF)
                )
            )
        }
    }
}

@Composable
fun DesktopSidebar(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    isAdmiralMode: Boolean,
    onAdmiralModeChange: (Boolean) -> Unit,
    currentLanguage: com.example.ui.game.AppLanguage,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(240.dp)
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        border = BorderStroke(1.dp, Color(0x3300E5FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Faction Badge Header
                Column {
                    Text(
                        text = "🛰️ COCKPIT CONSOLE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "VS MULTI-GRID SYS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00E5FF),
                        fontFamily = FontFamily.Monospace
                    )
                }

                Divider(color = Color(0x1F94A3B8))

                // Vertical Menu Items
                val menuItems = listOf(
                    0 to ("🗺️ " + com.example.ui.game.LanguageUtils.translate("tab_map", currentLanguage)),
                    1 to ("🔬 " + com.example.ui.game.LanguageUtils.translate("tab_research", currentLanguage)),
                    2 to ("🚀 " + com.example.ui.game.LanguageUtils.translate("tab_space", currentLanguage)),
                    3 to ("📻 " + com.example.ui.game.LanguageUtils.translate("tab_sound", currentLanguage)),
                    4 to ("🧮 " + com.example.ui.game.LanguageUtils.translate("tab_math", currentLanguage)),
                    5 to ("💾 " + com.example.ui.game.LanguageUtils.translate("tab_campaign", currentLanguage)),
                    6 to ("🛡️ " + com.example.ui.game.LanguageUtils.translate("tab_secure_core", currentLanguage)),
                    7 to ("🌐 " + com.example.ui.game.LanguageUtils.translate("tab_pwa_apk", currentLanguage))
                )

                menuItems.forEach { (index, label) ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0x2200E5FF) else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF00E5FF) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onTabSelect(index) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Bottom Admiral Mode Switcher
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF020617), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0x1F94A3B8), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "COMMAND DESK MODE",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (!isAdmiralMode) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (!isAdmiralMode) Color(0xFF00E5FF) else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { onAdmiralModeChange(false) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "FOCUSED",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isAdmiralMode) Color.White else Color(0xFF94A3B8)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isAdmiralMode) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (isAdmiralMode) Color(0xFF00E5FF) else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { onAdmiralModeChange(true) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ADMIRAL",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAdmiralMode) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdaptiveResearchPanel(
    selectedTech: com.example.data.TechEntity?,
    playerAlloys: Int,
    onUnlockTech: (com.example.data.TechEntity) -> Unit,
    currentLanguage: com.example.ui.game.AppLanguage,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0x2200E5FF))
    ) {
        if (selectedTech != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "⚙️ FORMULA DETAILS",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
                    border = BorderStroke(1.dp, Color(0x2200E5FF))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = selectedTech.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0x1100E5FF), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = selectedTech.category.uppercase(),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = selectedTech.equation,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700),
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Scientific Formula Matrix Equation",
                            fontSize = 8.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Functional Application", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    val capabilityText = when (selectedTech.id) {
                        "iron_melting" -> "Axiomatizes alloy fusion matrices, unlocking a persistent +35% regional efficiency multiplier for steel production lines across annexed fields."
                        "pythagorean_ballistic" -> "Modulates ballistic projectiles via right-triangle coordinate curves, establishing projectile speed margins and enhancing planetary orbital grid safety lines."
                        "thermodynamic_turbines" -> "Reduces energy loss coefficients by optimizing Tc/Th heat quotients ratios inside atmospheric combustion triggers."
                        "satellite_grid" -> "Broadens communication signals under Shannon-Hartley capacity laws, stabilizing space telemetric links under interference bounds."
                        "orbital_boost" -> "Increases gravity acceleration escape coordinates, allowing rapid transport vessels to enter space modules under minimal fuel capacities."
                        "fourier_harmony" -> "Synthesizes multi-dimensional wave components on-the-fly, locking environmental acoustic frequency structures into pleasant acoustic signals."
                        else -> "Accelerates scientific and demographic multiplier vectors across colonization zones."
                    }
                    Text(
                        text = capabilityText,
                        fontSize = 11.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }

                Divider(color = Color(0x1F94A3B8))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "DEVELOPMENT INDEX", fontSize = 8.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        Text(
                            text = if (selectedTech.isUnlocked) "100% ONLINE" else "0% ARCHIVED",
                            fontSize = 11.sp,
                            color = if (selectedTech.isUnlocked) Color(0xFF00FF88) else Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    Button(
                        onClick = { onUnlockTech(selectedTech) },
                        enabled = !selectedTech.isUnlocked && playerAlloys >= selectedTech.cost,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        val btnLabel = if (selectedTech.isUnlocked) "STABILIZED" else "STABILIZE"
                        Text(text = btnLabel, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NO TECHNOLOGY FORMULA SELECTED",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun AdaptiveSpaceLanderTelemetryPanel(
    altitude: Float,
    speedY: Float,
    fuel: Float,
    isThrusting: Boolean,
    status: String,
    selectedLanderStyle: String,
    onLanderStyleChange: (String) -> Unit,
    shipWeight: Int,
    onShipWeightChange: (Int) -> Unit,
    targetRegion: com.example.data.RegionEntity?,
    onStartLanding: () -> Unit,
    onThrustChange: (Boolean) -> Unit,
    currentLanguage: com.example.ui.game.AppLanguage,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0x2200E5FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "📊 FLIGHT CONFIGURATIONS",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            // Flight Model
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "SELECT CHASSIS MODEL", fontSize = 8.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val shapes = listOf("ION_CAPSULE" to "Ion Capsule", "HEAVY_CARGO" to "Heavy Cargo", "RETRO_POD" to "Retro Pod")
                    shapes.forEach { (style, label) ->
                        val selected = selectedLanderStyle == style
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) Color(0x1A00E5FF) else Color(0xFF020617))
                                .border(
                                    width = 1.dp,
                                    color = if (selected) Color(0xFF00E5FF) else Color(0x3394A3B8),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { onLanderStyleChange(style) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Mass configuration slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "INTEGRATED SYSTEM MASS (kg)", fontSize = 8.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(text = "$shipWeight kg", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = shipWeight.toFloat(),
                    onValueChange = { onShipWeightChange(it.toInt()) },
                    valueRange = 800f..3000f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }

            Divider(color = Color(0x1F94A3B8))

            // Live metrics
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
                border = BorderStroke(1.dp, Color(0x1A94A3B8))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Descent Altitude Meter:", fontSize = 9.sp, color = Color(0xFF64748B))
                        Text(text = "${String.format("%.1f", altitude)} m", fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Gravitational Velocity Y:", fontSize = 9.sp, color = Color(0xFF64748B))
                        Text(
                            text = "${String.format("%.2f", speedY)} m/s",
                            fontSize = 10.sp,
                            color = if (speedY > 3.8f) Color(0xFFF87171) else Color(0xFF4ADE80),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Liquid Propellant Fuel:", fontSize = 9.sp, color = Color(0xFF64748B))
                        Text(
                            text = "${fuel.toInt()}%",
                            fontSize = 10.sp,
                            color = if (fuel < 25f) Color(0xFFF87171) else Color(0xFF4ADE80),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Big physical ignite booster instructions info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF020617), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0x1A94A3B8), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "FLIGHT COMPUTER INSTRUCTIONS",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Use the interactive Flight Canvas on the left to activate descent retrograde jets. Hold the joystick/thrust trigger to decelerate safely below 3.8 m/s prior to surface impact. Excessive ship mass increases gravity acceleration coefficient.",
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

@Composable
fun UnifiedAdmiralCockpit(
    regions: List<com.example.data.RegionEntity>,
    techs: List<com.example.data.TechEntity>,
    logs: List<com.example.data.LogEntity>,
    activeSelectedRegion: com.example.data.RegionEntity?,
    onRegionSelect: (com.example.data.RegionEntity?) -> Unit,
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // COLUMN 1: TACTICAL CONTROL CENTER & SPACE FLIGHT LANDER
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Tactical map (takes 55% of height)
            Box(modifier = Modifier.weight(1.3f).fillMaxWidth()) {
                WorldMapAndZoom(
                    regions = regions,
                    selectedRegion = activeSelectedRegion,
                    onRegionSelect = onRegionSelect,
                    zoomScale = viewModel.zoomScale,
                    onZoomScaleChange = { viewModel.zoomScale = it },
                    mapOffsetX = viewModel.mapOffsetX,
                    mapOffsetY = viewModel.mapOffsetY,
                    onOffsetChange = { x, y ->
                        viewModel.mapOffsetX = x
                        viewModel.mapOffsetY = y
                    },
                    turnTimeRemaining = viewModel.turnTimeRemaining,
                    turnMaxDuration = viewModel.turnMaxDuration,
                    activeFactionTurn = viewModel.activeFactionTurn,
                    isTurnTimerActive = viewModel.isTurnTimerActive,
                    turnNumber = viewModel.turnNumber,
                    onEndTurnManually = { viewModel.endPlayerTurnManually() },
                    onToggleTurnTimer = { viewModel.isTurnTimerActive = it },
                    currentLanguage = viewModel.currentLanguage,
                    accelX = viewModel.sensorAccelX,
                    accelY = viewModel.sensorAccelY,
                    gyroZ = viewModel.sensorGyroZ,
                    isSensorOverrideActive = viewModel.isSensorOverrideActive,
                    onToggleSensorOverride = { viewModel.isSensorOverrideActive = it },
                    onSaveMatchStats = { victories, defeats, totalRounds, averageLatencyMs, outcome ->
                        viewModel.saveCombatResultToFirebase(victories, defeats, totalRounds, averageLatencyMs, outcome)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Descent Space Lander (takes 45% of height)
            Box(modifier = Modifier.weight(1.0f).fillMaxWidth()) {
                SpaceLanderSimulator(
                    altitude = viewModel.landerAltitude,
                    speedY = viewModel.landerSpeedY,
                    fuel = viewModel.landerFuel,
                    isThrusting = viewModel.landerIsThrusting,
                    status = viewModel.landerStatus,
                    selectedLanderStyle = viewModel.selectedLanderStyle,
                    onLanderStyleChange = { viewModel.selectedLanderStyle = it },
                    shipWeight = viewModel.currentSpaceShipWeight,
                    onShipWeightChange = { viewModel.currentSpaceShipWeight = it },
                    targetRegion = activeSelectedRegion ?: regions.firstOrNull(),
                    onStartLanding = {
                        viewModel.startSpaceLanding(activeSelectedRegion?.id ?: 1)
                    },
                    onThrustChange = { viewModel.triggerLanderBooster(it) },
                    currentLanguage = viewModel.currentLanguage,
                    accelX = viewModel.sensorAccelX,
                    accelY = viewModel.sensorAccelY,
                    gyroX = viewModel.sensorGyroX,
                    gyroY = viewModel.sensorGyroY,
                    gyroZ = viewModel.sensorGyroZ,
                    landerOffsetX = viewModel.landerOffsetX,
                    landerSpeedX = viewModel.landerSpeedX,
                    landerWindX = viewModel.landerWindX,
                    isSensorOverrideActive = viewModel.isSensorOverrideActive,
                    onToggleSensorOverride = { viewModel.isSensorOverrideActive = it },
                    onManualSteerLeft = { viewModel.manualSteerLeft() },
                    onManualSteerRight = { viewModel.manualSteerRight() },
                    onReleaseSteer = { viewModel.releaseSteer() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // COLUMN 2: SCIENCE UPGRADES, J.A.R.V.I.S INTELLIGENCE AND MULTIPLAYER LOG FEED
        Column(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Selected auxiliary dashboard: tabbed between Science Lab & live Chat/Conquest logs
            var auxTab by remember { mutableIntStateOf(0) } // 0: Chat Logs, 1: Science Upgrades
            
            TabRow(
                selectedTabIndex = auxTab,
                containerColor = Color(0xFF0F172A),
                contentColor = Color(0xFF00E5FF),
                modifier = Modifier.fillMaxWidth().height(36.dp),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[auxTab]),
                        color = Color(0xFF00E5FF)
                    )
                }
            ) {
                Tab(
                    selected = auxTab == 0,
                    onClick = { auxTab = 0 },
                    text = { Text("📡 COMMAND TRANSMITTER FEED", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = if (auxTab == 0) Color.White else Color(0xFF94A3B8)) }
                )
                Tab(
                    selected = auxTab == 1,
                    onClick = { auxTab = 1 },
                    text = { Text("🔬 SCIENCE TECH TREE", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = if (auxTab == 1) Color.White else Color(0xFF94A3B8)) }
                )
                Tab(
                    selected = auxTab == 2,
                    onClick = { auxTab = 2 },
                    text = { Text("🛡️ SECURE CORE vmsb©", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = if (auxTab == 2) Color(0xFF00FFCC) else Color(0xFF94A3B8)) }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxWidth()
            ) {
                when (auxTab) {
                    0 -> {
                        ChatAndConquestTabs(
                            regions = regions,
                            logs = logs,
                            playerCredits = viewModel.playerCredits,
                            playerAlloys = viewModel.playerAlloys,
                            chatInput = viewModel.chatInput,
                            onChatInputChange = { viewModel.chatInput = it },
                            onSendMessage = { viewModel.sendChatMessage() },
                            onConquerRegion = { viewModel.conquerRegion(it) },
                            onSelectRegionOnMap = { region ->
                                onRegionSelect(region)
                                viewModel.mapOffsetX = -(region.longitude.toFloat() * 1.2f * viewModel.zoomScale)
                                viewModel.mapOffsetY = (region.latitude.toFloat() * 1.2f * viewModel.zoomScale)
                            },
                            isLobbyJoined = viewModel.isLobbyJoined,
                            commanderName = viewModel.commanderName,
                            commanderFaction = viewModel.commanderFaction,
                            serverRoomId = viewModel.serverRoomId,
                            onJoinLobby = { name, faction, roomId ->
                                viewModel.commanderName = name
                                viewModel.commanderFaction = faction
                                viewModel.serverRoomId = roomId
                                viewModel.isLobbyJoined = true
                            },
                            onLeaveLobby = {
                                viewModel.isLobbyJoined = false
                            },
                            currentLanguage = viewModel.currentLanguage,
                            playerGold = viewModel.playerGold,
                            playerFood = viewModel.playerFood,
                            playerManpower = viewModel.playerManpower,
                            goldGrowthRate = viewModel.goldGrowthRate,
                            foodGrowthRate = viewModel.foodGrowthRate,
                            manpowerGrowthRate = viewModel.manpowerGrowthRate,
                            goldFocusCoeff = viewModel.goldFocusCoeff,
                            foodFocusCoeff = viewModel.foodFocusCoeff,
                            manpowerFocusCoeff = viewModel.manpowerFocusCoeff,
                            onGoldCoeffChange = { viewModel.goldFocusCoeff = it },
                            onFoodCoeffChange = { viewModel.foodFocusCoeff = it },
                            onManpowerCoeffChange = { viewModel.manpowerFocusCoeff = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    1 -> {
                        ResearchLabTab(
                            techs = techs,
                            playerAlloys = viewModel.playerAlloys,
                            onUnlockTech = { viewModel.unlockTechnology(it) },
                            currentLanguage = viewModel.currentLanguage,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        val zkpMoves by viewModel.zkpMoves.collectAsState()
                        com.example.ui.game.SecureCoreTab(
                            currentLanguage = viewModel.currentLanguage,
                            playerCredits = viewModel.playerCredits,
                            playerAlloys = viewModel.playerAlloys,
                            onAdjustCredits = { viewModel.playerCredits += it },
                            onAddLog = { sender, msg, type -> viewModel.insertLog(sender, msg, type) },
                            audioEngine = viewModel.audioEngine,
                            isGameZkpEnforced = viewModel.isGameZkpEnforced,
                            onGameZkpEnforcedChange = { viewModel.isGameZkpEnforced = it },
                            zkpMoves = zkpMoves,
                            zkpTamperWarning = viewModel.zkpTamperWarning,
                            onDismissTamperWarning = { viewModel.zkpTamperWarning = null },
                            onSimulateTamper = { viewModel.simulateTamperedTransition() },
                            onClearZkpMoves = { viewModel.clearZkpVerificationLedger() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Lower display: J.A.R.V.I.S. Cognitive Strategic Uplink Terminal
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth()
            ) {
                LogicalMathAdvisor(
                    globalPopulation = viewModel.globalPopulation,
                    landerVelocityY = viewModel.landerSpeedY,
                    activeVolume = viewModel.audioEngine.volume,
                    playerCredits = viewModel.playerCredits,
                    playerAlloys = viewModel.playerAlloys,
                    regions = regions,
                    techs = techs,
                    advisorReport = viewModel.advisorReport,
                    isGeneratingAdvisor = viewModel.isGeneratingAdvisor,
                    advisorErrorMessage = viewModel.advisorErrorMessage,
                    customAdvisorQuery = viewModel.customAdvisorQuery,
                    onQueryChange = { viewModel.customAdvisorQuery = it },
                    onGenerateReport = { viewModel.requestAiTacticalDirective(it) },
                    currentLanguage = viewModel.currentLanguage,
                    isHighThinkingModeEnabled = viewModel.isHighThinkingModeEnabled,
                    onHighThinkingModeChange = { viewModel.isHighThinkingModeEnabled = it },
                    isZkpEnabled = viewModel.isZkpEnabled,
                    onZkpChange = { viewModel.isZkpEnabled = it },
                    thinkingSteps = viewModel.thinkingSteps,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun TopTelemetryHeader(
    credits: Int,
    alloys: Int,
    population: Long,
    pingMs: Int,
    currentLanguage: com.example.ui.game.AppLanguage,
    onLanguageChange: (com.example.ui.game.AppLanguage) -> Unit,
    isLobbyJoined: Boolean,
    roomId: String,
    gold: Double,
    food: Double,
    manpower: Double,
    isCompact: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isCompact) 4.dp else 8.dp, vertical = if (isCompact) 2.dp else 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0x3300E5FF))
    ) {
        Column(modifier = Modifier.padding(if (isCompact) 6.dp else 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Game title (Localized)
                Column {
                    Text(
                        text = com.example.ui.game.LanguageUtils.translate("app_title", currentLanguage),
                        style = if (isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = com.example.ui.game.LanguageUtils.translate("app_subtitle", currentLanguage),
                        fontSize = if (isCompact) 8.sp else 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Inline flag selection controls + ping
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 6.dp)
                ) {
                    com.example.ui.game.AppLanguage.values().forEach { lang ->
                        val selected = lang == currentLanguage
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (selected) Color(0xFF00E5FF) else Color(0x3394A3B8),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .background(if (selected) Color(0x2200E5FF) else Color.Transparent)
                                .clickable { onLanguageChange(lang) }
                                .padding(horizontal = if (isCompact) 4.dp else 6.dp, vertical = 2.dp)
                        ) {
                            val flag = when (lang) {
                                com.example.ui.game.AppLanguage.ENGLISH -> if (isCompact) "EN" else "🇺🇸 EN"
                                com.example.ui.game.AppLanguage.ESPANOL -> if (isCompact) "ES" else "🇪🇸 ES"
                                com.example.ui.game.AppLanguage.PORTUGUES -> if (isCompact) "PT" else "🇧🇷 PT"
                            }
                            Text(
                                text = flag,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(if (isCompact) 4.dp else 6.dp)
                            .background(if (isLobbyJoined) Color(0xFF00FF88) else Color(0xFFFFD700), shape = CircleShape)
                    )
                    Text(
                        text = if (isLobbyJoined) (if (isCompact) roomId else "ROOM $roomId") else "OFF",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))

            // Resource Metrics Display Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 6.dp)
            ) {
                // Credits Metric
                ResourceMetricCell(
                    icon = "💳",
                    label = com.example.ui.game.LanguageUtils.translate("credits", currentLanguage),
                    value = "$credits",
                    tint = Color(0xFF00E5FF),
                    isCompact = isCompact,
                    modifier = Modifier.weight(1f).testTag("header_credits_cell")
                )

                // Alloys Metric
                ResourceMetricCell(
                    icon = "⚙️",
                    label = com.example.ui.game.LanguageUtils.translate("alloys", currentLanguage),
                    value = "$alloys t",
                    tint = Color(0xFFECEFF1),
                    isCompact = isCompact,
                    modifier = Modifier.weight(1f).testTag("header_alloys_cell")
                )

                // Population Metric
                ResourceMetricCell(
                    icon = "👥",
                    label = com.example.ui.game.LanguageUtils.translate("population", currentLanguage),
                    value = formatPopulation(population),
                    tint = Color(0xFF00FF88),
                    isCompact = isCompact,
                    modifier = Modifier.weight(if (isCompact) 1.1f else 1.3f).testTag("header_population_cell")
                )
            }

            Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 6.dp))

            // Real-time Logistics Resources Display Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 6.dp)
            ) {
                // Gold Metric
                ResourceMetricCell(
                    icon = "🪙",
                    label = "GOLD",
                    value = String.format("%.0f", gold),
                    tint = Color(0xFFFFD700),
                    isCompact = isCompact,
                    modifier = Modifier.weight(1f).testTag("header_gold_cell")
                )

                // Food Metric
                ResourceMetricCell(
                    icon = "🌾",
                    label = "FOOD",
                    value = String.format("%.0f", food),
                    tint = Color(0xFFFF8A65),
                    isCompact = isCompact,
                    modifier = Modifier.weight(1f).testTag("header_food_cell")
                )

                // Manpower Metric
                ResourceMetricCell(
                    icon = "⚔️",
                    label = "MANPOWER",
                    value = String.format("%.0f", manpower),
                    tint = Color(0xFF00FF88),
                    isCompact = isCompact,
                    modifier = Modifier.weight(if (isCompact) 1.1f else 1.3f).testTag("header_manpower_cell")
                )
            }
        }
    }
}

@Composable
fun ResourceMetricCell(
    icon: String,
    label: String,
    value: String,
    tint: Color,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF020617), shape = RoundedCornerShape(6.dp))
            .border(1.dp, Color(0x1F94A3B8), shape = RoundedCornerShape(6.dp))
            .padding(if (isCompact) 4.dp else 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 6.dp)
        ) {
            Text(text = icon, fontSize = if (isCompact) 11.sp else 14.sp)
            Column {
                Text(text = label, fontSize = 7.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                Text(
                    text = value,
                    fontSize = if (isCompact) 10.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = tint,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun MainNavigationTabs(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    currentLanguage: com.example.ui.game.AppLanguage
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color(0xFF060B15),
        contentColor = Color(0xFF00E5FF),
        edgePadding = 8.dp,
        indicator = { tabPositions ->
            if (selectedTab < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFF00E5FF)
                )
            }
        }
    ) {
        val tabDefinitions = listOf(
            0 to com.example.ui.game.LanguageUtils.translate("tab_map", currentLanguage),
            1 to com.example.ui.game.LanguageUtils.translate("tab_research", currentLanguage),
            2 to com.example.ui.game.LanguageUtils.translate("tab_space", currentLanguage),
            3 to com.example.ui.game.LanguageUtils.translate("tab_sound", currentLanguage),
            4 to com.example.ui.game.LanguageUtils.translate("tab_math", currentLanguage)
        )

        tabDefinitions.forEach { (index, label) ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelect(index) },
                modifier = Modifier.testTag("nav_tab_index_$index"),
                text = {
                    Text(
                        text = label,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp,
                        color = if (selectedTab == index) Color.White else Color(0x99ECEFF1)
                    )
                }
            )
        }
    }
}

// Format raw population values to human-readable strings (e.g. 120M, 4.3B)
fun formatPopulation(num: Long): String {
    return when {
        num >= 1_000_000_000 -> String.format("%.2f B", num.toDouble() / 1_000_000_000.0)
        num >= 1_000_000 -> String.format("%.1f M", num.toDouble() / 1_000_000.0)
        num >= 1_000 -> String.format("%.1f k", num.toDouble() / 1_000.0)
        else -> "$num"
    }
}
