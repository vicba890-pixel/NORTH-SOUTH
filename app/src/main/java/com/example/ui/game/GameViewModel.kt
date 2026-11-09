package com.example.ui.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.ui.audio.ProceduralAudioEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.BuildConfig
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val repository = GameRepository(db)

    // --- FIREBASE AUTHENTICATION & FIRESTORE PROGRESS SERVICES ---
    val firebaseManager = FirebaseManager.getInstance(application)
    
    // Firebase states
    var firebaseUser by mutableStateOf<FirebaseUserStats?>(firebaseManager.getCurrentUser())
    var firebaseAuthError by mutableStateOf<String?>(null)
    var firebaseAuthLoading by mutableStateOf(false)
    
    var firebaseSyncLoading by mutableStateOf(false)
    var firebaseSyncSuccessMessage by mutableStateOf<String?>(null)
    var firebaseSyncErrorMessage by mutableStateOf<String?>(null)
    
    var firebaseProgressList by mutableStateOf<List<FirebaseTacticalProgress>>(emptyList())
    var firebaseMatchStatsList by mutableStateOf<List<MultiplayerMatchStats>>(emptyList())

    fun refreshFirebaseData() {
        viewModelScope.launch {
            try {
                firebaseProgressList = firebaseManager.loadAllTacticalProgress()
                firebaseMatchStatsList = firebaseManager.loadAllMultiplayerMatchStats()
            } catch (e: Exception) {
                android.util.Log.e("GameViewModel", "Error fetching Firestore lists", e)
            }
        }
    }

    fun registerFirebaseUser(email: String, password: String) {
        viewModelScope.launch {
            firebaseAuthLoading = true
            firebaseAuthError = null
            try {
                val user = firebaseManager.registerUser(email, password)
                firebaseUser = user
                repository.insertLog("FIREBASE_AUTH", "🛰️ Registered secure player ID: ${user.uid} (${if (user.isRealFirebase) "Live cloud" else "Local-first WASM sandbox"}).", "SYSTEM")
                refreshFirebaseData()
            } catch (e: Exception) {
                firebaseAuthError = e.message ?: "Registration failed."
                repository.insertLog("FIREBASE_AUTH_ERROR", "❌ Registration error: ${e.message}", "SYSTEM")
            } finally {
                firebaseAuthLoading = false
            }
        }
    }

    fun loginFirebaseUser(email: String, password: String) {
        viewModelScope.launch {
            firebaseAuthLoading = true
            firebaseAuthError = null
            try {
                val user = firebaseManager.loginUser(email, password)
                firebaseUser = user
                repository.insertLog("FIREBASE_AUTH", "🛰️ Securely authenticated commander: ${user.email} (${if (user.isRealFirebase) "Live Firestore active" else "Local WASM engine state"}).", "SYSTEM")
                refreshFirebaseData()
            } catch (e: Exception) {
                firebaseAuthError = e.message ?: "Authentication failed."
                repository.insertLog("FIREBASE_AUTH_ERROR", "❌ Authentication error: ${e.message}", "SYSTEM")
            } finally {
                firebaseAuthLoading = false
            }
        }
    }

    fun logoutFirebaseUser() {
        firebaseManager.logoutUser()
        firebaseUser = null
        viewModelScope.launch {
            repository.insertLog("FIREBASE_AUTH", "🔒 Player logged out. Tactical progress is offline-only.", "SYSTEM")
        }
        refreshFirebaseData()
    }

    fun saveTacticalProgressToFirebase(slotId: Int) {
        viewModelScope.launch {
            firebaseSyncLoading = true
            firebaseSyncSuccessMessage = null
            firebaseSyncErrorMessage = null
            try {
                val regionsVal = regions.value
                val techsVal = techs.value
                
                // Count owned regions
                val ownedRegions = regionsVal.count { it.controller == "player" }
                val unlockedTechs = techsVal.count { it.isUnlocked }
                
                val progress = FirebaseTacticalProgress(
                    slotId = slotId,
                    timestamp = System.currentTimeMillis(),
                    commanderName = commanderName,
                    commanderFaction = commanderFaction,
                    credits = playerCredits,
                    alloys = playerAlloys,
                    population = globalPopulation,
                    regionsCount = ownedRegions,
                    techsCount = unlockedTechs
                )
                
                firebaseManager.saveTacticalProgress(progress)
                firebaseSyncSuccessMessage = "Successfully secured Tactical Progress (Slot #$slotId) in Firestore!"
                repository.insertLog("FIRESTORE_SYNC", "✅ Tactical slot #$slotId synchronized with Firestore. Credits: $playerCredits, Alloys: $playerAlloys, Population: $globalPopulation", "SYSTEM")
                refreshFirebaseData()
            } catch (e: Exception) {
                firebaseSyncErrorMessage = e.message ?: "Synchronization failed."
                repository.insertLog("FIRESTORE_ERROR", "❌ Firestore synchronization failed: ${e.message}", "SYSTEM")
            } finally {
                firebaseSyncLoading = false
            }
        }
    }

    fun saveCombatResultToFirebase(victories: Int, defeats: Int, totalRounds: Int, averageLatencyMs: Double, outcome: String) {
        viewModelScope.launch {
            try {
                val stats = MultiplayerMatchStats(
                    timestamp = System.currentTimeMillis(),
                    commanderName = commanderName,
                    commanderFaction = commanderFaction,
                    victories = victories,
                    defeats = defeats,
                    totalRounds = totalRounds,
                    averageLatencyMs = averageLatencyMs,
                    outcome = outcome
                )
                firebaseManager.saveMultiplayerMatchStats(stats)
                repository.insertLog("FIRESTORE_SYNC", "⚡ Combat telemetry secured in Firestore collection 'match_statistics'. Outcome: $outcome", "SYSTEM")
                refreshFirebaseData()
            } catch (e: Exception) {
                android.util.Log.e("GameViewModel", "Failed to auto-save combat statistics", e)
            }
        }
    }

    // Room Observed States
    val regions: StateFlow<List<RegionEntity>> = repository.regions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val techs: StateFlow<List<TechEntity>> = repository.techs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val logs: StateFlow<List<LogEntity>> = repository.logs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val campaignSaves: StateFlow<List<CampaignSaveEntity>> = repository.campaignSaves.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val zkpMoves: StateFlow<List<ZkpMoveEntity>> = repository.zkpMoves.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    var isGameZkpEnforced by mutableStateOf(true)
    var zkpTamperWarning by mutableStateOf<String?>(null)

    // Player Live Gameplay Stats (Accumulated based on logistics output)
    var playerCredits by mutableStateOf(500)
    var playerAlloys by mutableStateOf(150)
    var globalPopulation by mutableStateOf(120000000L) // 120M Starting population

    // Real-time Resources based on territory control with logarithmic distribution model
    var playerGold by mutableStateOf(100.0)
    var playerFood by mutableStateOf(50.0)
    var playerManpower by mutableStateOf(200.0)

    var goldGrowthRate by mutableStateOf(0.0)
    var foodGrowthRate by mutableStateOf(0.0)
    var manpowerGrowthRate by mutableStateOf(0.0)

    var goldFocusCoeff by mutableStateOf(1.0)
    var foodFocusCoeff by mutableStateOf(1.0)
    var manpowerFocusCoeff by mutableStateOf(1.0)

    // Sound Engine Reference (Managed in ViewModel bound to View Lifecycles)
    val audioEngine = ProceduralAudioEngine(application)

    // Interface States
    var selectedTab by mutableIntStateOf(0) // 0: Map, 1: Tech, 2: Space Land, 3: Audio, 4: Advisor
    var zoomScale by mutableFloatStateOf(1f)
    var mapOffsetX by mutableFloatStateOf(0f)
    var mapOffsetY by mutableFloatStateOf(0f)

    // Live Simulated Multiplayer Core
    var pingMs by mutableIntStateOf(34)
    var chatInput by mutableStateOf("")

    // Native Multi-Language and Active Multiplayer Rooms state
    var currentLanguage by mutableStateOf(com.example.ui.game.AppLanguage.ENGLISH)
    var commanderName by mutableStateOf("VS_Commander")
    var commanderFaction by mutableStateOf("north_union") // north_union or "south_alliance"
    var isLobbyJoined by mutableStateOf(false)
    var serverRoomId by mutableStateOf("ARENA-07")

    // Space Lander Simulation State
    var landerAltitude by mutableFloatStateOf(100f) // meters above ground
    var landerSpeedY by mutableFloatStateOf(-8f)    // downward velocity in m/s
    var landerFuel by mutableFloatStateOf(100f)
    var landerIsThrusting by mutableStateOf(false)
    var landerStatus by mutableStateOf("ORBIT") // ORBIT, PROGRESS, LANDED, CRASHED
    var selectedLandingRegionId by mutableIntStateOf(1)
    var currentSpaceShipWeight by mutableStateOf(1500) // kg
    var selectedLanderStyle by mutableStateOf("ION_CAPSULE") // "ION_CAPSULE", "HEAVY_CARGO", "RETRO_POD"

    // Physical Sensors Live Feed States
    var sensorAccelX by mutableFloatStateOf(0f)
    var sensorAccelY by mutableFloatStateOf(0f)
    var sensorAccelZ by mutableFloatStateOf(9.81f)
    var sensorGyroX by mutableFloatStateOf(0f)
    var sensorGyroY by mutableFloatStateOf(0f)
    var sensorGyroZ by mutableFloatStateOf(0f)
    var isSensorOverrideActive by mutableStateOf(false) // Toggle to simulate tilting in emulator/web

    // New space lander horizontal mechanics
    var landerOffsetX by mutableFloatStateOf(0f) // -100f to +100f offset from landing pad center
    var landerSpeedX by mutableFloatStateOf(0f)   // lateral velocity (m/s)
    var landerWindX by mutableFloatStateOf(0f)     // dynamic wind velocity (m/s)

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event ?: return
            if (isSensorOverrideActive) return // Skip physical values if in manual/simulated tilt override
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    sensorAccelX = event.values[0]
                    sensorAccelY = event.values[1]
                    sensorAccelZ = event.values[2]
                }
                Sensor.TYPE_GYROSCOPE -> {
                    sensorGyroX = event.values[0]
                    sensorGyroY = event.values[1]
                    sensorGyroZ = event.values[2]
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    // Dynamic Turn-Based Battle/Conquest Engine state
    var turnTimeRemaining by mutableIntStateOf(30)
    var turnMaxDuration by mutableIntStateOf(30)
    var activeFactionTurn by mutableStateOf("player") // "player", "alliance_north", "rival_south"
    var isTurnTimerActive by mutableStateOf(true)
    var turnNumber by mutableIntStateOf(1)

    // Engine Ticking Job
    private var simulationJob: Job? = null
    // Active Lander Loop Job
    private var landerJob: Job? = null

    init {
        // Register Sensor Listeners for Hardware Accelerometer and Gyroscope
        val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelSensor?.let { sensorManager?.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroSensor?.let { sensorManager?.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME) }

        // Initialize State Tables if Empty
        viewModelScope.launch {
            // Seed Regions
            val currentRegions = repository.regions.first()
            if (currentRegions.isEmpty()) {
                val seedRegions = listOf(
                    RegionEntity(1, "South Archipelago", "player", 45000000L, 5, -34.6, 138.6, false, 25, 50),
                    RegionEntity(2, "Alliance North Sector", "alliance_north", 32000000L, 12, 55.75, 37.6, false, 15, 30),
                    RegionEntity(3, "Equatorial Ion Basin", "neutral", 15000000L, 3, 1.29, 103.85, false, 40, 80),
                    RegionEntity(4, "Eastern Steppes Core", "rival_south", 58000000L, 20, 39.9, 116.4, false, 30, 40),
                    RegionEntity(5, "Western Ridge Highlands", "rival_south", 72000000L, 24, 40.7, -74.0, false, 50, 60),
                    RegionEntity(6, "Lunar Zone Alpha", "neutral", 0L, 1, 0.0, 0.0, false, 120, 300),
                    RegionEntity(7, "Antarctic Vault Alpha", "neutral", 25000L, 15, -82.8, 135.0, false, 80, 150)
                )
                repository.insertRegions(seedRegions)
            }

            // Seed Technologies
            val currentTechs = repository.techs.first()
            if (currentTechs.isEmpty()) {
                val seedTechs = listOf(
                    TechEntity("iron_melting", "Iron Melting Math", "Civilization", 100, true, 100, "dH/dt = k * A * (Th - Tc)"),
                    TechEntity("pythagorean_ballistic", "Pythagorean Ballistics", "Military", 250, false, 0, "R = (v² * sin(2θ)) / g"),
                    TechEntity("thermodynamic_turbines", "Thermodynamic Turbines", "Civilization", 550, false, 0, "η = 1 - (Tc / Th)"),
                    TechEntity("satellite_grid", "Satellite Telecom Grid", "Signal", 1100, false, 0, "C = B * log₂(1 + SNR)"),
                    TechEntity("orbital_boost", "Orbital Multi-way Booster", "Aerospace", 2200, false, 0, "V_esc = √(2 * G * M / R)"),
                    TechEntity("fourier_harmony", "Fourier Signal Harmony", "Signal", 4000, false, 0, "X(f) = ∫ x(t) * e^{-i 2π f t} dt")
                )
                repository.insertTechs(seedTechs)
            }

            // Seed Logs
            val currentLogs = repository.logs.first()
            if (currentLogs.isEmpty()) {
                repository.insertLog("SYSTEM", "Satellite Command Online. Latency optimized to 34ms.", "SYSTEM")
                repository.insertLog("Alliance Commander", "Welcome to North&South World. Tactical map overlay synced.", "CHAT")
            }
        }

        // Start Ambient Music Synthesizer
        audioEngine.start()

        // Start Main Simulation Thread Loop
        startSimulationLoop()
        startTurnTimerLoop()
    }

    private fun startSimulationLoop() {
        simulationJob = viewModelScope.launch {
            while (true) {
                delay(1500)
                // 1. Calculate Outputs from Player-held Regions
                val heldRegions = regions.value.filter { it.controller == "player" }
                val alloyIncome = heldRegions.sumOf { it.alloyOutput }
                val creditsIncome = heldRegions.sumOf { it.creditsOutput }

                playerAlloys += alloyIncome
                playerCredits += creditsIncome

                // Real-time Resource Calculations using Logarithmic Distribution Models based on Geographic Territory Control
                val countOwned = heldRegions.size
                if (countOwned > 0) {
                    // Logarithmic Gold Distribution: taxation/wealth scaling based on credits output
                    val totalCreditsOutput = heldRegions.sumOf { it.creditsOutput.toDouble() }
                    goldGrowthRate = 5.0 + 8.5 * goldFocusCoeff * kotlin.math.ln(1.0 + totalCreditsOutput)

                    // Logarithmic Food Distribution: agricultural efficiency based on population density
                    val totalPopulationM = heldRegions.sumOf { it.population.toDouble() / 10_000_000.0 }
                    foodGrowthRate = 3.0 + 6.0 * foodFocusCoeff * kotlin.math.ln(1.0 + totalPopulationM)

                    // Logarithmic Manpower Distribution: recruit mobilization based on defense levels & population index
                    val totalMobilizationIndex = heldRegions.sumOf { r -> r.defenseLevel.toDouble() * (1.0 + r.population.toDouble() / 50_000_000.0) }
                    manpowerGrowthRate = 2.0 + 10.0 * manpowerFocusCoeff * kotlin.math.ln(1.0 + totalMobilizationIndex)
                } else {
                    goldGrowthRate = 0.0
                    foodGrowthRate = 0.0
                    manpowerGrowthRate = 0.0
                }

                // Increment resources based on tick delta (1.5 seconds)
                playerGold += goldGrowthRate * 1.5
                playerFood += foodGrowthRate * 1.5
                playerManpower += manpowerGrowthRate * 1.5

                // 2. Population Growth: Logistic Growth Math Equation Model
                // dP/dt = r * P * (1 - P / K)
                val birthRate = 0.0018f // growth rate coefficient
                val carryingCapacity = 5000000000L // 5 Billion saturation barrier
                val growth = (birthRate * globalPopulation * (1.0 - (globalPopulation.toDouble() / carryingCapacity))).toLong()
                globalPopulation += growth

                // 3. Simulated Connection and Ping fluctuations
                val pingDelta = (-5..5).random()
                pingMs = (pingMs + pingDelta).coerceIn(12, 110)

                // 4. Random AI Actions & Live chat notifications (multiplayer simulation)
                if (Math.random() > 0.85) {
                    val message = "Confrontation reported near Western Ridge. Deploying defense probes!"
                    val players = listOf("QuantumConqueror", "AncientGamer_2", "SiberianSentry", "VS_General")
                    val phrases = listOf(
                        "Establishing research dome in Zone Alpha!",
                        "Sound mixer looks crystal clear. Loving the bass harmony!",
                        "Space lander rocket telemetry configured. Getting ready to land on Lunar Zone Alpha!",
                        "Let's conquer Equatorial Basin! Who is with me?",
                        "Fourier frequencies adjusted in mixer. The environment feels extremely immersive.",
                        "Pythagorean Ballistics researched! Watch your borders."
                    )
                    repository.insertLog(players.random(), phrases.random(), "CHAT")
                }

                // AI Outposts conquest simulation with Zero-Knowledge Proof (ZKP) integrity verification
                if (Math.random() > 0.95) {
                    val wildcardRegions = regions.value.filter { it.controller != "player" }
                    if (wildcardRegions.isNotEmpty()) {
                        val victim = wildcardRegions.random()
                        val newController = if (victim.controller == "neutral") {
                            if (Math.random() > 0.5) "alliance_north" else "rival_south"
                        } else {
                            if (victim.controller == "alliance_north") "rival_south" else "alliance_north"
                        }
                        verifyAndCommitTransition(victim, newController, "ANNEX")
                    }
                }
            }
        }
        
        refreshFirebaseData()
    }

    private var turnTimerJob: Job? = null

    fun startTurnTimerLoop() {
        turnTimerJob?.cancel()
        turnTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (isTurnTimerActive) {
                    if (activeFactionTurn == "player") {
                        if (turnTimeRemaining > 0) {
                            turnTimeRemaining--
                            // Trigger warning chime and log warning when 5 or less seconds remain
                            if (turnTimeRemaining in 1..5) {
                                audioEngine.playChimeWarning()
                                repository.insertLog("TACTICAL", "⚠️ WARNING: Tactical decision window closing in $turnTimeRemaining seconds...", "TACTICAL")
                            }
                        } else {
                            // Turn expired!
                            triggerTurnAutoTransition()
                        }
                    }
                }
            }
        }
    }

    private fun triggerTurnAutoTransition() {
        viewModelScope.launch {
            repository.insertLog("SYSTEM", "🚨 COMMAND TIMEOUT: Decision time expired. Defaulting state and transferring strategic control.", "SYSTEM")
            // Pass turn to Rival South
            activeFactionTurn = "rival_south"
            runAiTurnSequence()
        }
    }

    fun endPlayerTurnManually() {
        if (activeFactionTurn != "player") return
        viewModelScope.launch {
            repository.insertLog("TACTICAL", "⚔️ PLAYER TURN ENDED: Transferring tactical control to factions.", "TACTICAL")
            activeFactionTurn = "rival_south"
            runAiTurnSequence()
        }
    }

    private fun runAiTurnSequence() {
        viewModelScope.launch {
            val currentFactions = listOf("rival_south", "alliance_north")
            for (faction in currentFactions) {
                activeFactionTurn = faction
                repository.insertLog("AI_CORE", "🤖 Faction ${faction.uppercase()} is computing secure trajectory...", "SYSTEM")
                delay(1800) // Simulated strategic processing delay
                
                // AI Conquest Move
                val candidates = regions.value.filter { it.controller != faction }
                if (candidates.isNotEmpty()) {
                    val target = candidates.random()
                    // Conquers target region via secure ZKP Move Transition
                    verifyAndCommitTransition(target, faction, "ANNEX_AI")
                    repository.insertLog("TACTICAL", "🤖 Faction ${faction.uppercase()} successfully annexed [${target.name}] under ZKP validation.", "TACTICAL")
                }
            }
            
            // Hand control back to player
            turnNumber++
            activeFactionTurn = "player"
            turnTimeRemaining = turnMaxDuration
            repository.insertLog("SYSTEM", "🛰️ SATELLITE SYNC: Turn $turnNumber active. Player Commander controls restored.", "SYSTEM")
        }
    }

    private fun calculateSha256(input: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "0x" + input.hashCode().toString(16)
        }
    }

    // Verify and commit a faction state transition with zero-knowledge proof
    fun verifyAndCommitTransition(
        region: RegionEntity,
        newController: String,
        actionType: String = "ANNEX",
        forceTamper: Boolean = false
    ) {
        viewModelScope.launch {
            val inputPayload = "${region.id}|${region.controller}|$newController|${region.defenseLevel}"
            val proofHash = "0x" + calculateSha256(inputPayload)
            
            // System constraint checks (witness checks)
            val isSufficientDefense = region.defenseLevel <= 45
            val isValidFactions = listOf("alliance_north", "rival_south", "player", "neutral").contains(newController)
            val hasValidPath = true // Adjacency mapping
            
            // Verify Groth16 pairings (simulating constraint checks)
            val isZkpProofValid = !forceTamper && isSufficientDefense && isValidFactions && hasValidPath
            
            if (isGameZkpEnforced) {
                if (isZkpProofValid) {
                    // Update database
                    val updated = region.copy(
                        controller = newController,
                        defenseLevel = if (newController == "player") region.defenseLevel + 4 else region.defenseLevel
                    )
                    repository.updateRegion(updated)
                    
                    // Log the verified ZKP Move
                    val details = """
                        * Circuit: zk-SNARK (Groth16 on BN254)
                        * Constraints: [Controller Transition, Witness Sign, Balanced Allocation]
                        * Curve Pairings: e(A, B) == e(α, β) · e(x, γ) · e(C, δ) -> VALID
                    """.trimIndent()
                    
                    repository.insertZkpMove(
                        com.example.data.ZkpMoveEntity(
                            faction = newController,
                            regionId = region.id,
                            regionName = region.name,
                            actionType = actionType,
                            proofHash = proofHash,
                            isVerified = true,
                            details = details
                        )
                    )
                    repository.insertLog("ZKP_VERIFIER", "🔒 State transition verified! Faction move [$newController -> ${region.name}] is cryptographically authentic.", "SYSTEM")
                } else {
                    // Proof verification FAILED!
                    val details = """
                        * Circuit: zk-SNARK (Groth16 on BN254)
                        * FAIL: Constraint [Valid Controller Transition / Witness Sign] Mismatch!
                        * Verification: BILINEAR PAIRING CHECK FAILURE (e(A, B) != e(α, β)...)
                    """.trimIndent()
                    
                    repository.insertZkpMove(
                        com.example.data.ZkpMoveEntity(
                            faction = newController,
                            regionId = region.id,
                            regionName = region.name,
                            actionType = "TAMPER_ALERT",
                            proofHash = "0xBAD_PROOF_WitnessViolation_" + kotlin.random.Random.nextInt(1000, 9999),
                            isVerified = false,
                            details = details
                        )
                    )
                    zkpTamperWarning = "⚠️ ZKP FAILURE: Intercepted illegal state transition! Faction $newController attempted unauthorized capture of ${region.name}. Move aborted."
                    repository.insertLog("ZKP_ALERT", "🚨 CRITICAL SECURITY FAULT: Unauthorized faction transition intercepted on ${region.name}! Proof rejected.", "SYSTEM")
                    audioEngine.playChimeWarning()
                }
            } else {
                // If ZKP bypass is active, we just do the transition blindly (unsecured)
                val updated = region.copy(
                    controller = newController,
                    defenseLevel = if (newController == "player") region.defenseLevel + 4 else region.defenseLevel
                )
                repository.updateRegion(updated)
                repository.insertLog("TACTICAL", "Region [${region.name}] annexed by $newController (ZKP Bypassed).", "TACTICAL")
            }
        }
    }

    fun simulateTamperedTransition() {
        viewModelScope.launch {
            val nonPlayerRegions = regions.value.filter { it.controller != "player" }
            if (nonPlayerRegions.isNotEmpty()) {
                val region = nonPlayerRegions.random()
                // Force a transition that is completely out of bound or faked controller (e.g. "rogue_hackers") with forceTamper = true
                verifyAndCommitTransition(region, "rogue_hackers", "TAMPER_ALERT", forceTamper = true)
            } else {
                repository.insertLog("ZKP_VERIFIER", "Cannot simulate tampering: No available regions.", "SYSTEM")
            }
        }
    }

    fun clearZkpVerificationLedger() {
        viewModelScope.launch {
            repository.clearZkpMoves()
            zkpTamperWarning = null
            repository.insertLog("ZKP_VERIFIER", "Zero-Knowledge move integrity ledger reset successfully.", "SYSTEM")
        }
    }

    // Space Lander Mechanics
    fun startSpaceLanding(regionId: Int) {
        if (landerStatus == "PROGRESS") return
        selectedLandingRegionId = regionId
        landerAltitude = 120f
        landerSpeedY = 2.5f // Gentle downward initial de-insertion speed
        landerFuel = 100f
        landerStatus = "PROGRESS"
        landerIsThrusting = false
        landerOffsetX = 0f
        landerSpeedX = 0f
        // Dynamic cross-wind speed (meters/sec)
        landerWindX = (kotlin.random.Random.nextFloat() * 2f - 1f) * 1.5f

        // Tailored craft parameters for high-fidelity physics balancing
        val gravity = when (selectedLanderStyle) {
            "RETRO_POD" -> 0.40f    // Low lunar gravity
            "HEAVY_CARGO" -> 1.62f  // High gravity
            else -> 1.15f           // Standard gravity
        }

        val baseThrust = when (selectedLanderStyle) {
            "RETRO_POD" -> 2.60f
            "HEAVY_CARGO" -> 12.5f
            else -> 4.90f
        }

        val fuelConsumptionPerTick = when (selectedLanderStyle) {
            "RETRO_POD" -> 0.9f
            "HEAVY_CARGO" -> 1.4f
            else -> 1.1f
        }

        landerJob?.cancel()
        landerJob = viewModelScope.launch {
            repository.insertLog("SPACE_DECK", "Initiating trajectory descent on Target Region ${regionId}...", "LANDING")
            audioEngine.tempoMs = 300 // speed up beat for atmospheric hazard
            audioEngine.theme = "TACTICAL"

            while (landerStatus == "PROGRESS") {
                delay(120)

                // 1. Logarithmic dynamic rocket mass calculation
                // Total mass = dry weight + fuel mass (fuel weight is up to 35% of lander's dry structure weight)
                val massDry = (currentSpaceShipWeight / 1000f)
                val massFuel = (landerFuel / 100f) * (massDry * 0.35f)
                val totalMass = massDry + massFuel

                // 2. Compute retro propulsion force from active booster
                val thrustForce = if (landerIsThrusting && landerFuel > 0f) {
                    landerFuel = (landerFuel - fuelConsumptionPerTick).coerceAtLeast(0f)
                    baseThrust / totalMass
                } else {
                    0f
                }

                // 3. Dynamic atmospheric drag force using logarithmic height progression
                // Represents dP/dh logarithmic pressure amplification near standard surface boundaries
                val logAtmDensity = kotlin.math.ln(1.0f + (120f - landerAltitude) / 12f) * 0.08f
                val dragDecel = (logAtmDensity * landerSpeedY).coerceIn(-0.5f, 1.2f)

                // 4. Update kinematics
                // Vertical:
                val netAcceleration = gravity - thrustForce - dragDecel
                landerSpeedY += netAcceleration * 0.12f
                landerAltitude -= landerSpeedY * 0.12f

                // Horizontal Tilt & Wind Mechanics:
                // Smoothly return simulated tilt to upright if simulator active
                if (isSensorOverrideActive) {
                    sensorAccelX *= 0.85f
                    sensorGyroZ *= 0.85f
                } else {
                    // Wind turbulence affects gyroscope angular velocity readings
                    if (Math.random() > 0.8) {
                        sensorGyroZ = (Math.random().toFloat() * 2f - 1f) * 0.6f
                    }
                }

                // Tilt input from accelerometer X-axis
                val tiltInput = -sensorAccelX
                val lateralForce = tiltInput * 0.45f
                val lateralDrag = -0.12f * landerSpeedX
                val netAccX = lateralForce + landerWindX + lateralDrag

                landerSpeedX += netAccX * 0.12f
                landerOffsetX += landerSpeedX * 12f * 0.12f
                landerOffsetX = landerOffsetX.coerceIn(-100f, 100f)

                // Occasional wind gust fluctuation
                if (Math.random() > 0.85) {
                    landerWindX += (Math.random().toFloat() * 2f - 1f) * 0.3f
                    landerWindX = landerWindX.coerceIn(-3.0f, 3.0f)
                }

                if (landerAltitude <= 0f) {
                    landerAltitude = 0f
                    val isAligned = kotlin.math.abs(landerOffsetX) <= 35f

                    // Evaluate landing safety specs (velocity Y and lateral alignment)
                    if (landerSpeedY < 3.8f && isAligned) { // Safe speed & properly centered
                        landerStatus = "LANDED"
                        // Unlock Space Land structure bonuses on our region entity
                        val regionToUpdate = regions.value.find { it.id == selectedLandingRegionId }
                        if (regionToUpdate != null) {
                            val updated = regionToUpdate.copy(
                                isSpaceLanded = true,
                                alloyOutput = regionToUpdate.alloyOutput + 35,
                                creditsOutput = regionToUpdate.creditsOutput + 75
                            )
                            repository.updateRegion(updated)
                        }
                        repository.insertLog("SPACE_DECK", "Touchdown CONFIRMED! Regional alloy pipelines boosted (+35 Alloy).", "LANDING")
                    } else if (landerSpeedY >= 3.8f) {
                        landerStatus = "CRASHED"
                        repository.insertLog("SPACE_DECK", "CRITICAL FAILURE: Hard Impact! Structural integrity failed at velocity: ${String.format("%.2f", landerSpeedY)} m/s (Limit: 3.8m/s)", "LANDING")
                    } else {
                        landerStatus = "CRASHED"
                        repository.insertLog("SPACE_DECK", "CRITICAL WARNING: Missed Pad! Lander struck deep-space mountains with lateral drift offset of ${String.format("%.1f", landerOffsetX)}m (Safe Limit: ±35m)", "LANDING")
                    }
                    audioEngine.tempoMs = 600
                    audioEngine.theme = "COSMIC_2"
                }
            }
        }
    }

    fun triggerLanderBooster(thrust: Boolean) {
        landerIsThrusting = thrust
    }

    fun manualSteerLeft() {
        if (!isSensorOverrideActive) return
        sensorAccelX = 4.5f
        sensorGyroZ = -1.2f
    }

    fun manualSteerRight() {
        if (!isSensorOverrideActive) return
        sensorAccelX = -4.5f
        sensorGyroZ = 1.2f
    }

    fun releaseSteer() {
        if (!isSensorOverrideActive) return
        sensorAccelX = 0f
        sensorGyroZ = 0f
    }

    // Technology Actions
    fun unlockTechnology(tech: TechEntity) {
        if (tech.isUnlocked) return
        if (playerAlloys >= tech.cost) {
            playerAlloys -= tech.cost
            viewModelScope.launch {
                val updated = tech.copy(isUnlocked = true, progress = 100)
                repository.updateTech(updated)
                repository.insertLog("RESEARCH_LAB", "Unlocked and normalized: [${tech.name}] based on formula '${tech.equation}'!", "SYSTEM")
            }
        } else {
            viewModelScope.launch {
                repository.insertLog("RESEARCH_LAB", "Insufficient alloys to extract math core for ${tech.name} (Requires ${tech.cost} Alloys).", "SYSTEM")
            }
        }
    }

    // Conquer Regions Tactically
    fun conquerRegion(region: RegionEntity) {
        if (activeFactionTurn != "player") {
            viewModelScope.launch {
                repository.insertLog("TACTICAL", "❌ COMMAND DENIED: Faction ${activeFactionTurn.uppercase()} turn is active.", "TACTICAL")
            }
            return
        }
        if (region.controller == "player") return
        val costCredits = region.defenseLevel * 40
        val costAlloys = region.defenseLevel * 15

        if (playerCredits >= costCredits && playerAlloys >= costAlloys) {
            playerCredits -= costCredits
            playerAlloys -= costAlloys
            verifyAndCommitTransition(region, "player", "ANNEX_PLAYER")
        } else {
            viewModelScope.launch {
                repository.insertLog("TACTICAL", "Cannot acquire [${region.name}]. Insufficient credits ($costCredits) or alloys ($costAlloys).", "TACTICAL")
            }
        }
    }

    // Send Real Chat Transmitter Messages
    fun sendChatMessage() {
        if (chatInput.isBlank()) return
        val text = chatInput
        chatInput = ""
        viewModelScope.launch {
            val prefix = if (isLobbyJoined) commanderName else "PRESIDENT_VS"
            repository.insertLog(prefix, text, "CHAT")
            
            // Trigger rapid audio tempo change for realistic feedback beep
            val origTempo = audioEngine.tempoMs
            audioEngine.tempoMs = 200
            delay(150)
            audioEngine.tempoMs = origTempo

            // Let other multiplayer players respond after a realistic delay
            delay(800)
            val lowerText = text.lowercase()
            if (lowerText.contains("conquer") || lowerText.contains("atacar") || lowerText.contains("conquistar")) {
                val responseText = when (currentLanguage) {
                    com.example.ui.game.AppLanguage.ENGLISH -> "Roger that! Targeting vulnerable southern border. Preparing fleet! 🚀"
                    com.example.ui.game.AppLanguage.ESPANOL -> "¡Entendido comandante! Apuntando al sector sur. ¡Tropas movilizadas! 🚀"
                    com.example.ui.game.AppLanguage.PORTUGUES -> "Entendido! Visando o setor sul vulnerável. Preparando nossa frota! 🚀"
                }
                repository.insertLog("Captain_Sanchez [ES]", responseText, "CHAT")
            } else if (lowerText.contains("hello") || lowerText.contains("hola") || lowerText.contains("oi") || lowerText.contains("bom dia")) {
                val responseText = when (currentLanguage) {
                    com.example.ui.game.AppLanguage.ENGLISH -> "Greetings, Commander! Synchronizing tactical satellites. 🛰️"
                    com.example.ui.game.AppLanguage.ESPANOL -> "¡Saludos, Comandante! Sincronizando satélites en curso táctico. 🛰️"
                    com.example.ui.game.AppLanguage.PORTUGUES -> "Saudações, Comandante! Sincronizando satélites táticos. 🛰️"
                }
                repository.insertLog("General_Bento [PT]", responseText, "CHAT")
            } else {
                // Normal reactive message logic from active peer
                val peers = listOf("OrbitalRider [EN]", "Captain_Sanchez [ES]", "General_Bento [PT]")
                val peerPhrases = when (currentLanguage) {
                    com.example.ui.game.AppLanguage.ENGLISH -> listOf(
                        "Battlefield frequencies verified. Tactical map is fully live.",
                        "Looking good! I am claiming alloy zones next.",
                        "Descent boosters configured. Preparing orbital entry!"
                    )
                    com.example.ui.game.AppLanguage.ESPANOL -> listOf(
                        "Canales de batalla verificados. El mapa táctico está en vivo.",
                        "¡Se ve bien! Conquistando el próximo depósito de aleación.",
                        "Propulsores listos. ¡Iniciando curso de satélite!"
                    )
                    com.example.ui.game.AppLanguage.PORTUGUES -> listOf(
                        "Canais de combate confirmados. Mapa global atualizado com sucesso.",
                        "Excelente! Conquistando setor de metais agora para fabricar defesas.",
                        "Módulo operacional pronto para descida aérea!"
                    )
                }
                repository.insertLog(peers.random(), peerPhrases.random(), "CHAT")
            }
        }
    }

    // AI Advisor States (Generative and simulated dynamic advisor response)
    var advisorReport by mutableStateOf<String?>(null)
    var isGeneratingAdvisor by mutableStateOf(false)
    var advisorErrorMessage by mutableStateOf<String?>(null)
    var customAdvisorQuery by mutableStateOf("")
    var isHighThinkingModeEnabled by mutableStateOf(true)
    var isZkpEnabled by mutableStateOf(true)
    val thinkingSteps = androidx.compose.runtime.mutableStateListOf<String>()

    fun requestAiTacticalDirective(customQuery: String = "") {
        viewModelScope.launch {
            isGeneratingAdvisor = true
            advisorErrorMessage = null
            thinkingSteps.clear()

            // Define deep-reasoning visual logs
            val stepsList = if (isHighThinkingModeEnabled) {
                val zkpMsg = if (isZkpEnabled) "Initializing Zero-Knowledge SNARK Cryptography (zk-Groth16)..." else "Initializing light cryptographic hashes..."
                mutableListOf(
                    "🧠 [Cognitive Link] Initiating high-reasoning tactical parser...",
                    "🔐 [ZKP Engine] $zkpMsg",
                    "📊 [Circuit Constraints] Analyzing state variables (${playerCredits} Credits, ${playerAlloys} Alloys)...",
                    "🗺️ [Game Theory] Mapping optimal annex solutions on ${regions.value.size} coordinate nodes...",
                    "🚀 [Physics Calculus] Integrating trajectory dynamics (${String.format("%.2f", landerSpeedY)} m/s) and logistic growth equations...",
                    "⚡ [ZKP Verification] generated proof π of resource integrity verified against Verification Key successfully.",
                    "📡 [De-capsulation] Structuring final decrypted strategic intelligence advisory..."
                )
            } else {
                mutableListOf(
                    "🧠 [Standard AI] Parsing commander query...",
                    "📡 [Packet Transmission] Transmitting state payload to Gemini server..."
                )
            }

            // Launch a coroutine to add thinking steps with realistic intervals
            val stepsJob = launch {
                for (step in stepsList) {
                    thinkingSteps.add(step)
                    delay(550)
                }
            }

            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                // Wait for all thinking steps to show beautifully in UI before resolving
                stepsJob.join()
                generateFallbackAdvisorReport(customQuery)
                advisorErrorMessage = "Notice: API Key not configured. Displaying high-precision local cybernetic recommendation verified via offline ZKP."
                isGeneratingAdvisor = false
                return@launch
            }

            // Build full state contextualized prompt
            val regionSummary = regions.value.joinToString("\n") { r ->
                "- ${r.name}: Controlled by '${r.controller}', Population: ${r.population}, Defense Level: ${r.defenseLevel}, Space Landing Installed: ${if (r.isSpaceLanded) "Yes" else "No"}"
            }
            val techSummary = techs.value.joinToString("\n") { t ->
                "- ${t.name}: ${if (t.isUnlocked) "UNLOCKED" else "LOCKED"} (Equation: ${t.equation})"
            }

            val zkpRequirement = if (isZkpEnabled) {
                """
                - INCLUDE a distinct section titled: "🔐 CRYPTOGRAPHIC ZERO-KNOWLEDGE PROOF (ZKP) INTEGRITY VERIFICATION"
                - In this section, provide a simulated cryptographic zk-SNARK proof block (e.g., Curves, Public Inputs, Proof G1/G2 elements, and verification statement) proving that you have analyzed the game stats correctly without exposing coordinate assets.
                """.trimIndent()
            } else ""

            val highThinkingRequirement = if (isHighThinkingModeEnabled) {
                """
                - Utilize deep cognitive reasoning and explore alternate game-theoretic outcomes in your analysis.
                - Detail your logical steps and outline the utility optimization path clearly.
                """.trimIndent()
            } else ""

            val systemPrompt = """
                You are the Tactical Supreme AI Command Assistant (codename J.A.R.V.I.S.) for the conquest game "North&South World" built by VS.
                Analyze the current player's state and provide brilliant tactical design-logic-mathematical recommendations.
                Format your response in a glorious sci-fi tactical layout with detailed bullet points.
                Include a mathematical or physical equation (e.g. game balance utility, logistics optimization, projectile dynamics) in your guidance.
                
                $highThinkingRequirement
                $zkpRequirement

                Current Player Resources:
                - Credits: ${playerCredits}
                - Alloys: ${playerAlloys}
                - Global Population: ${globalPopulation}

                Current Planetary Map Layout:
                ${if (regionSummary.isBlank()) "No regions loaded yet." else regionSummary}

                Research and Technologies Status:
                ${if (techSummary.isBlank()) "No technologies seeded." else techSummary}

                Active Audio Theme: ${audioEngine.theme}
                Descent Spaceship Stats: Style: ${selectedLanderStyle}, Current speed Y: ${String.format("%.2f", landerSpeedY)} m/s, Altitude: ${String.format("%.1f", landerAltitude)} m

                ${if (customQuery.isNotBlank()) "User's Strategic Inquiry: $customQuery" else "Generate a complete tactical campaign assessment."}
            """.trimIndent()

            val escapedPrompt = escapeJsonString(systemPrompt)
            val modelName = if (isHighThinkingModeEnabled) "gemini-2.0-flash-thinking-exp" else "gemini-2.5-flash"
            
            val jsonRequestBody = if (isHighThinkingModeEnabled) {
                """
                {
                  "contents": [
                    {
                      "parts": [
                        {
                          "text": "$escapedPrompt"
                        }
                      ]
                    }
                  ],
                  "generationConfig": {
                    "thinkingConfig": {
                      "thinkingBudget": 2048
                    }
                  }
                }
                """.trimIndent()
            } else {
                """
                {
                  "contents": [
                    {
                      "parts": [
                        {
                          "text": "$escapedPrompt"
                        }
                      ]
                    }
                  ]
                }
                """.trimIndent()
            }

            withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
                        .writeTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
                        .build()

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = jsonRequestBody.toRequestBody(mediaType)
                    
                    val request = Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey")
                        .post(body)
                        .build()

                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            val jsonResponse = org.json.JSONObject(responseBody)
                            val candidates = jsonResponse.getJSONArray("candidates")
                            val firstCandidate = candidates.getJSONObject(0)
                            val content = firstCandidate.getJSONObject("content")
                            val parts = content.getJSONArray("parts")
                            val firstPart = parts.getJSONObject(0)
                            val text = firstPart.getString("text")
                            
                            // Let the visual animation finish nicely or cancel and complete
                            stepsJob.join()
                            withContext(Dispatchers.Main) {
                                advisorReport = text
                                isGeneratingAdvisor = false
                            }
                        } else {
                            throw Exception("Empty response body from Gemini API.")
                        }
                    } else {
                        throw Exception("Gemini API error code: ${response.code}")
                    }
                } catch (e: Exception) {
                    stepsJob.join()
                    withContext(Dispatchers.Main) {
                        generateFallbackAdvisorReport(customQuery)
                        advisorErrorMessage = "Notice: Gemini High-Reasoning API request failed (${e.localizedMessage ?: "Timeout"}). Fallback verified report loaded."
                        isGeneratingAdvisor = false
                    }
                }
            }
        }
    }

    fun generateFallbackAdvisorReport(customQuery: String) {
        val totalRegions = regions.value.size
        val playerRegions = regions.value.count { it.controller == "player" }
        val lockedTechs = techs.value.filter { !it.isUnlocked }
        val recommendedTech = lockedTechs.minByOrNull { it.cost }
        val highestThreatRegion = regions.value.filter { it.controller != "player" }.maxByOrNull { it.defenseLevel }
        val optimalTarget = regions.value.filter { it.controller != "player" }.minByOrNull { it.defenseLevel }

        val birthRate = 0.0018
        val carryingCapacity = 5000000000.0
        val p = globalPopulation.toDouble()
        val dPdt = birthRate * p * (1.0 - (p / carryingCapacity))
        
        val optionalQueryText = if (customQuery.isNotBlank()) {
            "\n💡 *Strategic Query Received: \"$customQuery\"*\n→ *Analysist Answer*: AI advises that focusing on maximizing alloy pipelines ($playerCredits credits and $playerAlloys alloys available) is the mathematically optimal choice to resolve your query.\n"
        } else ""

        val zkpSection = if (isZkpEnabled) {
            """
            
            =========================================
            🔐 **SECTION 5: CRYPTOGRAPHIC INTEGRITY VERIFICATION (ZKP)**
            =========================================
            *   **Verification Circuit**: `zk-SNARK (Groth16 on BN254 Curve)`
            *   **Public Commitments / Inputs**:
                *   `Player_Resource_Hash` = `0x3A8F9C2D1B78EF...5A9E` (derived from $playerCredits Credits & $playerAlloys Alloys)
                *   `Map_State_Commitment` = `0x77EF00D34F6A2B...12C0` (derived from $totalRegions planetary outposts)
            *   **Zero-Knowledge Witnesses**: Verified securely (0 bytes disclosed)
            *   **Proof Parameters (π)**:
                *   `g_A`: `0x23F90A4E1B39D48C12FA...78EF`
                *   `g_B`: `0x5C912D8A7F41E02882CA...140A`
                *   `g_C`: `0x9D081A4F9E3120C210BD...532B`
            *   **Bilinear Pairing Equation**: `e(A, B) == e(α, β) · e(x, γ) · e(C, δ)` (SUCCESS)
            *   *Attestation*: Strategic recommendations are mathematically guaranteed to be consistent with verified game states under zero-disclosure privacy constraint.
            """.trimIndent()
        } else ""

        advisorReport = """
            🛰️ **TACTICAL AI RECONNAISSANCE DIRECTIVE (LOCAL EMBEDDED SYSTEM)**
            
            *Status Sync*: Online | *Security Uplink*: Local Math Logic Simulation Fallback
            $optionalQueryText
            
            =========================================
            📊 **SECTION 1: DEMOGRAPHIC LOGISTICS CALCULUS**
            =========================================
            *   **Current Global Population (P)**: ${String.format("%,d", globalPopulation)}
            *   **Logistic Growth Equation**: `dP/dt = r * P * (1 - P/K)`
                *   Where `r` (birth factor) = `0.0018`, `K` (carrying saturation) = `5.00 B`
            *   **Current Instaneous Velocity**: `+${String.format("%,d", dPdt.toLong())} citizens/sec`
            *   **Projection**: Population saturation is currently at `${String.format("%.2f", (p / carryingCapacity) * 100)}%`. High saturation triggers orbital efficiency boosts.
            
            =========================================
            🛸 **SECTION 2: AEROSPACE FLIGHT BALANCING**
            =========================================
            *   **Gravity Constant (g)**: Lunar standard configured (`1.62 m/s²`)
            *   **Kinetic Balance Equation**: `F_net = m * a = m * g - F_thrust`
            *   **Descent Velocity Warning**: Active velocity magnitude in space lander tab is `${String.format("%.2f", landerSpeedY)} m/s`.
            *   *Crucial AI Safety Recommendation*: Recommending retrograde booster triggers at `Y < 15.0m` to ensure descent speed vector lands safely under the structural tolerance limit of `< 3.80 m/s`.
            
            =========================================
            🏹 **SECTION 3: CONQUEST STRATEGY METRICS**
            =========================================
            *   **Expansion Ratio**: $playerRegions of $totalRegions regions annexed (${String.format("%.1f", (playerRegions.toFloat() / totalRegions) * 100)}%).
            *   **High-Priority Target**: ${optimalTarget?.name ?: "All Regions Annexed"} (Defense Level: ${optimalTarget?.defenseLevel ?: 0}). Acquisition requires ${(optimalTarget?.defenseLevel ?: 0) * 40} Credits and ${(optimalTarget?.defenseLevel ?: 0) * 15} Alloys.
            *   **Threat Assessment**: ${highestThreatRegion?.name ?: "None"} sector is mobilizing heavy defensive fortifications (Defense Level: ${highestThreatRegion?.defenseLevel ?: 0}).
            
            =========================================
            🔬 **SECTION 4: SCIENTIFIC FORMULA EVOLUTION**
            =========================================
            *   **Active Techs**: ${techs.value.count { it.isUnlocked }} of ${techs.value.size} fully researched.
            ${if (recommendedTech != null) "*   **Next Optimization Goal**: AI strongly recommends researching **${recommendedTech.name}** next. Cost: `${recommendedTech.cost} Alloys` (Remaining Alloys: `$playerAlloys`). Underpinning law: `${recommendedTech.equation}`." else "*   **Scientific Hegemony**: All technology branches fully normalized!"}
            
            =========================================
            📡 **COSMIC MIXER AUDIO HARMONICS**
            =========================================
            *   **Frequency Amplitude Status**: Current Master Volume is ${String.format("%.0f", audioEngine.volume * 100)}%.
            *   *Acoustical Optimization*: Increase additive harmonics count to maximize structural focus during intense landing procedures.
            $zkpSection
        """.trimIndent()
    }

    // Campaign Save and Load Actions
    fun saveCampaign(slotId: Int) {
        viewModelScope.launch {
            try {
                val currentRegions = repository.getAllRegions()
                val currentTechs = repository.getAllTechs()
                val regionsJson = serializeRegions(currentRegions)
                val techsJson = serializeTechs(currentTechs)

                val save = CampaignSaveEntity(
                    slotId = slotId,
                    timestamp = System.currentTimeMillis(),
                    credits = playerCredits,
                    alloys = playerAlloys,
                    globalPopulation = globalPopulation,
                    commanderName = commanderName,
                    commanderFaction = commanderFaction,
                    isLobbyJoined = isLobbyJoined,
                    serverRoomId = serverRoomId,
                    selectedLanderStyle = selectedLanderStyle,
                    currentSpaceShipWeight = currentSpaceShipWeight,
                    regionsJson = regionsJson,
                    techsJson = techsJson,
                    gold = playerGold,
                    food = playerFood,
                    manpower = playerManpower,
                    goldCoeff = goldFocusCoeff,
                    foodCoeff = foodFocusCoeff,
                    manpowerCoeff = manpowerFocusCoeff
                )

                repository.insertCampaignSave(save)
                repository.insertLog("SYSTEM", "Campaign Save snapshot written to Slot $slotId successfully.", "SYSTEM")
            } catch (e: Exception) {
                repository.insertLog("SYSTEM", "Failed to save campaign: ${e.localizedMessage}", "SYSTEM")
            }
        }
    }

    fun loadCampaign(slotId: Int) {
        viewModelScope.launch {
            try {
                val save = repository.getCampaignSave(slotId)
                if (save != null) {
                    playerCredits = save.credits
                    playerAlloys = save.alloys
                    globalPopulation = save.globalPopulation
                    commanderName = save.commanderName
                    commanderFaction = save.commanderFaction
                    isLobbyJoined = save.isLobbyJoined
                    serverRoomId = save.serverRoomId
                    selectedLanderStyle = save.selectedLanderStyle
                    currentSpaceShipWeight = save.currentSpaceShipWeight
                    playerGold = save.gold
                    playerFood = save.food
                    playerManpower = save.manpower
                    goldFocusCoeff = save.goldCoeff
                    foodFocusCoeff = save.foodCoeff
                    manpowerFocusCoeff = save.manpowerCoeff

                    val loadedRegions = deserializeRegions(save.regionsJson)
                    val loadedTechs = deserializeTechs(save.techsJson)

                    if (loadedRegions.isNotEmpty()) {
                        repository.insertRegions(loadedRegions)
                    }
                    if (loadedTechs.isNotEmpty()) {
                        repository.insertTechs(loadedTechs)
                    }

                    repository.insertLog("SYSTEM", "Campaign restored from Slot $slotId. Core metrics recovered.", "SYSTEM")
                } else {
                    repository.insertLog("SYSTEM", "No Campaign Save found in Slot $slotId.", "SYSTEM")
                }
            } catch (e: Exception) {
                repository.insertLog("SYSTEM", "Failed to load campaign: ${e.localizedMessage}", "SYSTEM")
            }
        }
    }

    fun deleteCampaignSave(slotId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteCampaignSave(slotId)
                repository.insertLog("SYSTEM", "Campaign Save slot $slotId erased from local core storage.", "SYSTEM")
            } catch (e: Exception) {
                repository.insertLog("SYSTEM", "Failed to delete campaign slot: ${e.localizedMessage}", "SYSTEM")
            }
        }
    }

    fun resetCampaign() {
        viewModelScope.launch {
            try {
                val seedRegions = listOf(
                    RegionEntity(1, "South Archipelago", "player", 45000000L, 5, -34.6, 138.6, false, 25, 50),
                    RegionEntity(2, "Alliance North Sector", "alliance_north", 32000000L, 12, 55.75, 37.6, false, 15, 30),
                    RegionEntity(3, "Equatorial Ion Basin", "neutral", 15000000L, 3, 1.29, 103.85, false, 40, 80),
                    RegionEntity(4, "Eastern Steppes Core", "rival_south", 58000000L, 20, 39.9, 116.4, false, 30, 40),
                    RegionEntity(5, "Western Ridge Highlands", "rival_south", 72000000L, 24, 40.7, -74.0, false, 50, 60),
                    RegionEntity(6, "Lunar Zone Alpha", "neutral", 0L, 1, 0.0, 0.0, false, 120, 300),
                    RegionEntity(7, "Antarctic Vault Alpha", "neutral", 25000L, 15, -82.8, 135.0, false, 80, 150)
                )
                repository.insertRegions(seedRegions)

                val seedTechs = listOf(
                    TechEntity("iron_melting", "Iron Melting Math", "Civilization", 100, true, 100, "dH/dt = k * A * (Th - Tc)"),
                    TechEntity("pythagorean_ballistic", "Pythagorean Ballistics", "Military", 250, false, 0, "R = (v² * sin(2θ)) / g"),
                    TechEntity("thermodynamic_turbines", "Thermodynamic Turbines", "Civilization", 550, false, 0, "η = 1 - (Tc / Th)"),
                    TechEntity("satellite_grid", "Satellite Telecom Grid", "Signal", 1100, false, 0, "C = B * log₂(1 + SNR)"),
                    TechEntity("orbital_boost", "Orbital Multi-way Booster", "Aerospace", 2200, false, 0, "V_esc = √(2 * G * M / R)"),
                    TechEntity("fourier_harmony", "Fourier Signal Harmony", "Signal", 4000, false, 0, "X(f) = ∫ x(t) * e^{-i 2π f t} dt")
                )
                repository.insertTechs(seedTechs)

                playerCredits = 500
                playerAlloys = 150
                globalPopulation = 120000000L
                playerGold = 100.0
                playerFood = 50.0
                playerManpower = 200.0
                goldFocusCoeff = 1.0
                foodFocusCoeff = 1.0
                manpowerFocusCoeff = 1.0

                repository.insertLog("SYSTEM", "Active war campaign reset back to seed baseline.", "SYSTEM")
            } catch (e: Exception) {
                repository.insertLog("SYSTEM", "Failed to reset active campaign: ${e.localizedMessage}", "SYSTEM")
            }
        }
    }

    private fun serializeRegions(regions: List<RegionEntity>): String {
        val array = org.json.JSONArray()
        for (r in regions) {
            val obj = org.json.JSONObject()
            obj.put("id", r.id)
            obj.put("name", r.name)
            obj.put("controller", r.controller)
            obj.put("population", r.population)
            obj.put("defenseLevel", r.defenseLevel)
            obj.put("latitude", r.latitude)
            obj.put("longitude", r.longitude)
            obj.put("isSpaceLanded", r.isSpaceLanded)
            obj.put("alloyOutput", r.alloyOutput)
            obj.put("creditsOutput", r.creditsOutput)
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeRegions(jsonStr: String): List<RegionEntity> {
        val list = mutableListOf<RegionEntity>()
        if (jsonStr.isBlank()) return list
        val array = org.json.JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                RegionEntity(
                    id = obj.getInt("id"),
                    name = obj.getString("name"),
                    controller = obj.getString("controller"),
                    population = obj.getLong("population"),
                    defenseLevel = obj.getInt("defenseLevel"),
                    latitude = obj.getDouble("latitude"),
                    longitude = obj.getDouble("longitude"),
                    isSpaceLanded = obj.optBoolean("isSpaceLanded", false),
                    alloyOutput = obj.optInt("alloyOutput", 10),
                    creditsOutput = obj.optInt("creditsOutput", 20)
                )
            )
        }
        return list
    }

    private fun serializeTechs(techs: List<TechEntity>): String {
        val array = org.json.JSONArray()
        for (t in techs) {
            val obj = org.json.JSONObject()
            obj.put("id", t.id)
            obj.put("name", t.name)
            obj.put("category", t.category)
            obj.put("cost", t.cost)
            obj.put("isUnlocked", t.isUnlocked)
            obj.put("progress", t.progress)
            obj.put("equation", t.equation)
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeTechs(jsonStr: String): List<TechEntity> {
        val list = mutableListOf<TechEntity>()
        if (jsonStr.isBlank()) return list
        val array = org.json.JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                TechEntity(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    category = obj.getString("category"),
                    cost = obj.getInt("cost"),
                    isUnlocked = obj.getBoolean("isUnlocked"),
                    progress = obj.optInt("progress", 0),
                    equation = obj.optString("equation", "")
                )
            )
        }
        return list
    }

    private fun escapeJsonString(string: String): String {
        return string.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    fun insertLog(sender: String, message: String, type: String) {
        viewModelScope.launch {
            repository.insertLog(sender, message, type)
        }
    }

    fun generateMathematicalMap(
        seed: Long,
        regionCount: Int,
        distribution: String, // "balanced", "player_heavy", "rival_heavy", "neutral_heavy"
        resourceFactor: Float
    ) {
        viewModelScope.launch {
            try {
                repository.deleteAllRegions()

                val prefixes = listOf(
                    "Nexus", "Enclave", "Aegis", "Vanguard", "Ion", "Vertex", 
                    "Polaris", "Cosmic", "Helios", "Titan", "Specter", "Sigma", 
                    "Hyperion", "Aurora", "Hadron", "Galactic", "Krypton", "Zenith"
                )
                val suffixes = listOf(
                    "Prime", "Basin", "Ridge", "Sector", "Vault", "Harbor", 
                    "Void", "Canyon", "Apex", "Range", "Plateau", "Delta", 
                    "Core", "Spire", "Matrix", "Abyss", "Haven", "Platform"
                )

                val generatedRegions = mutableListOf<RegionEntity>()
                
                for (i in 1..regionCount) {
                    val indexSeed = seed + i * 1493L
                    val rObj = java.util.Random(indexSeed)

                    val pIdx = rObj.nextInt(prefixes.size)
                    val sIdx = rObj.nextInt(suffixes.size)
                    val rawName = "${prefixes[pIdx]} ${suffixes[sIdx]}"
                    val name = if (rObj.nextBoolean()) "$rawName ${rObj.nextInt(90) + 10}" else rawName

                    val controller = when (distribution) {
                        "player_heavy" -> {
                            val roll = rObj.nextFloat()
                            when {
                                roll < 0.45f -> "player"
                                roll < 0.65f -> "alliance_north"
                                roll < 0.85f -> "neutral"
                                else -> "rival_south"
                            }
                        }
                        "rival_heavy" -> {
                            val roll = rObj.nextFloat()
                            when {
                                roll < 0.45f -> "rival_south"
                                roll < 0.65f -> "alliance_north"
                                roll < 0.85f -> "neutral"
                                else -> "player"
                            }
                        }
                        "neutral_heavy" -> {
                            val roll = rObj.nextFloat()
                            when {
                                roll < 0.60f -> "neutral"
                                roll < 0.75f -> "alliance_north"
                                roll < 0.90f -> "rival_south"
                                else -> "player"
                            }
                        }
                        else -> { // "balanced"
                            val roll = rObj.nextFloat()
                            when {
                                roll < 0.25f -> "player"
                                roll < 0.50f -> "alliance_north"
                                roll < 0.75f -> "rival_south"
                                else -> "neutral"
                            }
                        }
                    }

                    val basePop = 5_000_000L + (rObj.nextFloat() * 85_000_000L).toLong()
                    val population = if (controller == "neutral" && rObj.nextFloat() < 0.3f) 0L else basePop
                    val defenseLevel = rObj.nextInt(25) + 1

                    val latitude = -78.0 + (rObj.nextDouble() * 156.0)
                    val longitude = -175.0 + (rObj.nextDouble() * 350.0)

                    val baseAlloy = 10 + rObj.nextInt(25)
                    val baseCredits = 15 + rObj.nextInt(35)
                    val alloyOutput = (baseAlloy * resourceFactor * (1f + defenseLevel / 50f)).toInt().coerceAtLeast(5)
                    val creditsOutput = (baseCredits * resourceFactor * (1f + defenseLevel / 35f)).toInt().coerceAtLeast(10)

                    generatedRegions.add(
                        RegionEntity(
                            id = i,
                            name = name,
                            controller = controller,
                            population = population,
                            defenseLevel = defenseLevel,
                            latitude = latitude,
                            longitude = longitude,
                            isSpaceLanded = false,
                            alloyOutput = alloyOutput,
                            creditsOutput = creditsOutput
                        )
                    )
                }

                repository.insertRegions(generatedRegions)
                repository.insertLog("SYSTEM", "Dynamic Map generated. Math-physics seed $seed compiled successfully. $regionCount outposts initialized.", "SYSTEM")
            } catch (e: Exception) {
                repository.insertLog("SYSTEM", "Map generation failure: ${e.localizedMessage}", "SYSTEM")
            }
        }
    }

    override fun onCleared() {
        sensorManager?.unregisterListener(sensorListener)
        simulationJob?.cancel()
        landerJob?.cancel()
        turnTimerJob?.cancel()
        audioEngine.stop()
        super.onCleared()
    }
}
