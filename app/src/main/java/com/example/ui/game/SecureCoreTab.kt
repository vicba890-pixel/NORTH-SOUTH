package com.example.ui.game

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun SecureCoreTab(
    currentLanguage: AppLanguage,
    playerCredits: Int,
    playerAlloys: Int,
    onAdjustCredits: (Int) -> Unit,
    onAddLog: (String, String, String) -> Unit,
    audioEngine: com.example.ui.audio.ProceduralAudioEngine,
    isGameZkpEnforced: Boolean = true,
    onGameZkpEnforcedChange: (Boolean) -> Unit = {},
    zkpMoves: List<com.example.data.ZkpMoveEntity> = emptyList(),
    zkpTamperWarning: String? = null,
    onDismissTamperWarning: () -> Unit = {},
    onSimulateTamper: () -> Unit = {},
    onClearZkpMoves: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // --- SMART CONTRACTS STATES ---
    var scCounterparty by remember { mutableStateOf("NASA - Jet Propulsion Laboratory") }
    var scMicroTariff by remember { mutableStateOf(15.0f) }
    var scContractType by remember { mutableStateOf("Non-Exclusive Distribution & Academic As-Is License") }
    var scIsDeploying by remember { mutableStateOf(false) }
    var scDeploySuccess by remember { mutableStateOf(false) }
    var scContractLogs by remember { mutableStateOf(listOf("Contract Engine online. Awaiting parameter entry.")) }

    // --- AreIA 21ST-CENTURY SMART CONTRACT & AUDIT ENGINE STATES ---
    var areiaSelectedClauses by remember { mutableStateOf(setOf("ZKP_PRIVACY", "SMART_ESCROW", "CYBER_ARBITRATION")) }
    var areiaSlaPerformance by remember { mutableStateOf(99.98f) }
    var areiaEscrowCreditsLocked by remember { mutableStateOf(500) }
    var areiaEscrowAlloysLocked by remember { mutableStateOf(25) }
    var areiaFeedbackLog by remember { mutableStateOf(listOf("AreIA AI Auditor online. Multi-tenant secure compliance socket connected.", "No contract deviations detected. SLA is currently nominal.")) }
    var areiaSelectedPersona by remember { mutableStateOf("CyberArbitrator") } // "CyberArbitrator", "TaxSentinel", "QuantumAudit"
    var areiaCustomQuery by remember { mutableStateOf("") }
    var areiaAiResponse by remember { mutableStateOf("") }
    var isAreiaAnalyzing by remember { mutableStateOf(false) }
    var isSlaBreached by remember { mutableStateOf(false) }
    var areiaGeneratedAgreementHash by remember { mutableStateOf("") }

    // --- AreIA vmsb 21ST-CENTURY SMART CONTRACT LAUNCH DECK ---
    var areiaBasePrice by remember { mutableStateOf(450.0f) }
    var areiaRetentionFeeX by remember { mutableStateOf(1500.0f) } // "Valor X" direct retention fee
    var areiaIsLaunchAgreementSigned by remember { mutableStateOf(false) }
    var areiaLaunchSignedHash by remember { mutableStateOf("") }
    var areiaLaunchLog by remember { mutableStateOf(listOf("vmsb launch protocol initiated. Awaiting parameters & executive sign.")) }
    var areiaStudentWallets by remember {
        mutableStateOf(
            listOf(
                Triple("Ana Silva (U. Physics)", "vmsb-std-0x8a92b0", 250.0f),
                Triple("Carlos Santos (Quantum CS)", "vmsb-std-0x7c15ef", 180.0f),
                Triple("Mariana Souza (ZKP Dev)", "vmsb-std-0x9f31bd", 300.0f)
            )
        )
    }
    var areiaTotalDisbursedToStudents by remember { mutableStateOf(0.0f) }
    var areiaAutoOptimizerEnabled by remember { mutableStateOf(true) }
    var areiaIsHardThinking by remember { mutableStateOf(false) }
    var areiaHardThinkSolution by remember { mutableStateOf("") }
    var areiaSelectedQuestionIndex by remember { mutableStateOf(0) }

    // --- INTEROPERABLE LEDGER EXPORT STATES ---
    var selectedExportFormat by remember { mutableStateOf("SAP_ERP") } // "QUICKBOOKS", "SAP_ERP", "XERO", "IFRS_9"
    var showExportConfirmDialog by remember { mutableStateOf(false) }
    var isSigningLedger by remember { mutableStateOf(false) }
    var exportSuccessMessage by remember { mutableStateOf("") }
    var generatedLedgerSeal by remember { mutableStateOf("") }

    // --- BRAZIL TAX COMPLIANCE BOT STATES ---
    var isNubankConnected by remember { mutableStateOf(false) }
    var nubankUserEmail by remember { mutableStateOf("vicba890@gmail.com") }
    var nubankUserName by remember { mutableStateOf("Victor Barreto") }
    var nubankUserCpf by remember { mutableStateOf("88035689053") }
    var nubankAccountNum by remember { mutableStateOf("Agência 0001 • Conta 194820-4") }
    var isDetailsVisible by remember { mutableStateOf(false) } // Securely obfuscated/hidden by default

    // Helper to format/mask CPF
    val getMaskedCpf: (String, Boolean) -> String = { cpf, visible ->
        if (cpf.length == 11) {
            if (visible) {
                "${cpf.substring(0, 3)}.${cpf.substring(3, 6)}.${cpf.substring(6, 9)}-${cpf.substring(9)}"
            } else {
                "${cpf.substring(0, 3)}.***.***-${cpf.substring(9)}"
            }
        } else {
            cpf
        }
    }

    // Dynamic calculation of BRL revenues from player's simulated resources (Credits & Alloys)
    val taxableRevenueBrl = remember(playerCredits, playerAlloys) {
        (playerCredits * 12.5f) + (playerAlloys * 85.0f)
    }
    val taxRateBr = 0.30f // Fixed 30% Brasil Tax Rate
    val calculatedTaxBrl = taxableRevenueBrl * taxRateBr

    var isZkpRunning by remember { mutableStateOf(false) }
    var zkpProofGenerated by remember { mutableStateOf(false) }
    var zkpProofHash by remember { mutableStateOf("") }
    var zkpVerificationLogs by remember { mutableStateOf(listOf("ZKP Core Idle. Waiting for Nubank state linkage.")) }

    var isTransmittingTaxes by remember { mutableStateOf(false) }
    var transmissionSuccess by remember { mutableStateOf(false) }
    var transmissionLogs by remember { mutableStateOf(listOf("Compliance channel idle. e-CAC and BACEN web-sockets online.")) }

    var darfModalOpen by remember { mutableStateOf(false) }
    var darfPeriodCode by remember { mutableStateOf("0211") } // Código de Receita para IR
    var darfPeriodApuration by remember { mutableStateOf("30/06/2026") }
    var darfVencimento by remember { mutableStateOf("31/07/2026") }
    var darfReference by remember { mutableStateOf("85820000001-5 20000300102-4 19384820104-2 00019482019-3") }

    // Helper SHA-256 calculator
    val calculateSha256: (String) -> String = { input ->
        try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "0x" + input.hashCode().toString(16)
        }
    }

    // 1. Authentication State
    var isAuthed by remember { mutableStateOf(false) }
    var privateKey by remember { mutableStateOf("0xbf67da84de01e23a6bbd92a0011c2e4785fac9") }
    var isGeneratingKey by remember { mutableStateOf(false) }
    var isVerifyingAuth by remember { mutableStateOf(false) }
    var authTerminalLogs by remember { mutableStateOf(listOf("System idle. Secure enclave initialized.")) }

    // 2. Active-Active Node Failure & Scaling States
    var nodeNaActive by remember { mutableStateOf(true) }
    var nodeEuActive by remember { mutableStateOf(true) }
    var nodeApacActive by remember { mutableStateOf(true) }
    var nodeAuActive by remember { mutableStateOf(true) }

    // Gossip, Merkle & Attestation states
    var isGossipActive by remember { mutableStateOf(true) }
    var gossipLogs by remember { mutableStateOf(listOf("NATS channel open. Waiting for Kyber-1024 encryption handshake...")) }
    var attestationLogs by remember { mutableStateOf(listOf("Cross-Block Attestation Ledger online. System idling.")) }
    var isAttestationSigning by remember { mutableStateOf(false) }

    // Global Service Auditing and Exclusivity state
    var isGlobalAuditRunning by remember { mutableStateOf(false) }
    var globalAuditLogs by remember { mutableStateOf(listOf("Global registry verified. System is ready for live audit.")) }
    var expandedAreaDetail by remember { mutableStateOf<String?>(null) }

    // --- EDUCATION, FRACTAL TV & CRISIS MANAGEMENT STATES ---
    var educationTaxAllocationPercent by remember { mutableStateOf(50f) } // default 50% of the 30% Brasil Tax allocated to Education
    var selectedEducationNode by remember { mutableStateOf("PRIMARY") } // PRIMARY, UNIVERSITY, PHYSICS_LAB
    var isFractalTvActive by remember { mutableStateOf(true) }
    var fractalJuliaReal by remember { mutableStateOf(-0.7f) }
    var fractalJuliaImag by remember { mutableStateOf(0.27015f) }
    var selectedCrisisType by remember { mutableStateOf("ECONOMIC_IMPACT") } // ECONOMIC_IMPACT, CLIMATE_AND_ENERGY, INFRASTRUCTURE_LOAD
    var simulatedCrisisLogs by remember { mutableStateOf(listOf("System operational. Ready to run dynamic social & educational impact analysis.")) }
    var isSimulationRunning by remember { mutableStateOf(false) }
    
    // --- NORTH-SOUTH ECONOMIC MODEL & DASHBOARD STATES ---
    var globalNorthIndex by remember { mutableStateOf(85f) }
    var globalSouthIndex by remember { mutableStateOf(65f) }
    var selectedDashboardTab by remember { mutableStateOf("LIQUIDITY") } // "LIQUIDITY" or "VALUATION"
    var selectedTimelinePoint by remember { mutableStateOf(3) } // index of active point on Recharts-like dashboard
    
    
    // Calculates dynamic operational parameters
    val activeNodesCount = (if (nodeNaActive) 1 else 0) + (if (nodeEuActive) 1 else 0) + (if (nodeApacActive) 1 else 0) + (if (nodeAuActive) 1 else 0)
    val baseNodeFrequency = 3.15f // GHz * 4 = 12.60 GHz
    val requiredTotalPower = 12.6f // GHz aggregated capacity needed for 100% throughput
    
    val targetNodeFrequency = when (activeNodesCount) {
        4 -> baseNodeFrequency // 3.15 GHz each
        3 -> 4.2f // Overclock to 4.2 GHz each
        2 -> 6.3f // Overclock to 6.3 GHz each
        1 -> 12.6f // Max boost to 12.6 GHz single core
        else -> 0.0f // System down!
    }

    val dynamicEnclaveState = when (activeNodesCount) {
        4 -> "QUADRUPLE REDUNDANCY OPTIMAL"
        3 -> "TRIPLE REDUNDANCY STABLE"
        2 -> "DUAL CORE HOT-FAILOVER ACTIVE"
        1 -> "SINGLE NODE CRITICAL BOOST MODE"
        else -> "SYSTEM EMERGENCY OFFLINE"
    }

    // 3. The Three Gates State & Parameters
    var selectedGateIndex by remember { mutableStateOf(0) } // 0: Enterprise, 1: Elite Academic, 2: Mass Volume (Cult/Students)
    
    // Gate 1: Lyapunov control parameters
    var lyapunovAlpha by remember { mutableStateOf(1.2f) }
    var lyapunovBeta by remember { mutableStateOf(0.8f) }
    
    // Gate 2: Elite Academic Wave Parameters
    var waveFrequency by remember { mutableStateOf(3.0f) }
    var waveAmplitude by remember { mutableStateOf(40.0f) }
    var wavePhase by remember { mutableStateOf(0.0f) }
    
    // Gate 3: Mass Volume Proof rate
    var simulationTicks by remember { mutableStateOf(0) }
    var microTasksCompleted by remember { mutableStateOf(1420) }

    // --- CRISIS MONITORING & COMPUTE METRICS STATES ---
    var simulatedTrafficLoad by remember { mutableStateOf(4200f) } // Requests/sec (RPS)
    var activeCrisisScenario by remember { mutableStateOf("NONE") } // "NONE", "DDOS_NA", "POWER_EU", "SEISMIC_AP"
    var computeEfficiencyHistory by remember { mutableStateOf(listOf(95f, 96f, 94f, 95f, 97f, 98f, 95f, 96f, 94f, 95f, 97f, 98f, 95f)) }
    var crisisLogs by remember { mutableStateOf(listOf(
        "SysAudit [UTC]: Triple active-active balanced load-balancer operational.",
        "SysAudit [UTC]: Enclave nodes NA-1, EU-2, AP-4 status verified stable.",
        "SysAudit [UTC]: Logical throughput threshold within optimal parameters."
    )) }

    // State listeners to audit node changes
    LaunchedEffect(activeNodesCount) {
        val timestamp = "SysAudit [UTC]"
        val newLog = when (activeNodesCount) {
            4 -> "$timestamp: Active-active balance fully restored. Enclaves in Dallas, Frankfurt, Tokyo, and Sydney back in high-availability sync."
            3 -> "$timestamp: 3 nodes active. Redundant enclaves running stable."
            2 -> "$timestamp: Failure in Node redundancy. Dynamic overclocking initiated to 6.3 GHz to preserve logical throughput."
            1 -> "$timestamp: ALERT: Only single node active. Emergency overclock to 12.6 GHz to prevent compute saturation!"
            else -> "$timestamp: EMERGENCY: All physical active-active enclaves OFFLINE. Loss of quadruple-redundant math engine capacity."
        }
        crisisLogs = (crisisLogs + newLog).takeLast(25)
    }

    // Gossip beacon live simulation loop
    LaunchedEffect(isGossipActive, nodeNaActive, nodeEuActive, nodeApacActive, nodeAuActive) {
        if (isGossipActive) {
            while (true) {
                delay(3000)
                val activeList = mutableListOf<String>()
                if (nodeNaActive) activeList.add("Dallas (NA-1)")
                if (nodeEuActive) activeList.add("Frankfurt (EU-2)")
                if (nodeApacActive) activeList.add("Tokyo (AP-4)")
                if (nodeAuActive) activeList.add("Sydney (AU-5)")

                if (activeList.isNotEmpty()) {
                    val sender = activeList.random()
                    val otherActive = activeList.filter { it != sender }
                    val logEntry = if (otherActive.isNotEmpty()) {
                        val recipient = otherActive.random()
                        val truthHash = "0x" + List(12) { "0123456789abcdef".random() }.joinToString("")
                        "NATS Bus [Kyber-1024 PQC]: $sender gossiped Truth Hash (GDR-TE: $truthHash) to $recipient"
                    } else {
                        "$sender waiting for peer nodes on NATS channel..."
                    }
                    gossipLogs = (listOf(logEntry) + gossipLogs).take(30)
                } else {
                    gossipLogs = (listOf("⚠️ NATS Bus Alert: All nodes offline. Gossip protocol halted.") + gossipLogs).take(30)
                }
            }
        }
    }

    // Tick simulation to make waves and micro-tasks dynamic!
    LaunchedEffect(Unit) {
        var tickCounter = 0
        while (true) {
            delay(100)
            wavePhase += 0.2f
            if (activeNodesCount > 0) {
                simulationTicks++
                if (selectedGateIndex == 2) {
                    microTasksCompleted += Random.nextInt(5, 18)
                }
            }
            // Update performance history every 10 ticks (1.0 second)
            tickCounter++
            if (tickCounter >= 10) {
                tickCounter = 0
                val currentEff = when (activeNodesCount) {
                    3 -> 94f + Random.nextFloat() * 4f
                    2 -> 85f + Random.nextFloat() * 5f
                    1 -> 60f + Random.nextFloat() * 8f
                    else -> 0f
                }
                computeEfficiencyHistory = (computeEfficiencyHistory + currentEff).takeLast(15)
            }
        }
    }

    // 4. Payment Gateway & Smart License State
    var selectedLicenseType by remember { mutableStateOf("AI_COCKPIT") }
    var licensePeriodMonths by remember { mutableStateOf(6f) }
    var showZkpCheckout by remember { mutableStateOf(false) }
    var isCheckingOut by remember { mutableStateOf(false) }
    var checkoutSuccess by remember { mutableStateOf(false) }
    var checkoutLogs by remember { mutableStateOf(listOf<String>()) }

    // --- REVERSED BUSINESS MODEL: QUALITY-ADJUSTED EXCLUSIVITY MARKET VALUE ---
    // The price is not amortized; it represents a dynamically adjusted, scalable, competent market value
    // based on real-time service quality (active enclaves, processing frequency, connections)
    // and scaled dynamically by the North & South world economic model.
    val baseLicenseCost = when (selectedLicenseType) {
        "AI_COCKPIT" -> 3500f
        "CONQUEST_NODE" -> 7000f
        "QUANTUM_GRID" -> 14000f
        else -> 5000f
    }

    // Dynamic factor representing Quality of Service (QoS) & Exclusivity (active enclaves + connection stats)
    val baseServiceQualityFactor = 0.60f + (activeNodesCount * 0.15f) + (if (isNubankConnected) 0.15f else 0.0f)
    
    // North-South model factor (North Index drives premium scaling, South Index improves balance and efficiency)
    val northSouthAdjustmentFactor = (globalNorthIndex / 85f) * (1.2f - (globalSouthIndex / 150f))
    val totalExclusivityFactor = baseServiceQualityFactor * northSouthAdjustmentFactor

    // Dynamic market value adjustment (not static amortization; competent and scalable)
    val dynamicLicenseValue = baseLicenseCost * totalExclusivityFactor * (licensePeriodMonths / 12f)
    
    // Local taxes (30% Brazil Tax deducted from day 1)
    val brazilTax30 = dynamicLicenseValue * 0.30f
    
    // Collateral Stake Deposit is structured to guarantee immediate Day 1 liquid differential
    val collateralDeposited = dynamicLicenseValue * 3.5f

    val baseStakeYieldRate = when (selectedLicenseType) {
        "AI_COCKPIT" -> 0.18f
        "CONQUEST_NODE" -> 0.24f
        "QUANTUM_GRID" -> 0.32f
        else -> 0.20f
    }
    
    // Dynamic combined compound yield rate enhanced by quality of service enclaves and South efficiency gains
    val southYieldAdjustment = 0.8f + (globalSouthIndex / 100f) * 0.3f
    val compoundYieldRate = baseStakeYieldRate * totalExclusivityFactor * southYieldAdjustment

    // Value left over time horizons - showing positive net liquid differential from Day 1
    val yield1Day = collateralDeposited * (compoundYieldRate / 365f)
    val cost1Day = dynamicLicenseValue / (licensePeriodMonths * 30f)
    // Positive liquid differential from Day 1, with Brazil local tax subtracted immediately
    val netLiquid1Day = collateralDeposited - brazilTax30 + yield1Day - cost1Day

    val yield1Week = collateralDeposited * (compoundYieldRate / 52f)
    val cost1Week = dynamicLicenseValue / (licensePeriodMonths * 4.2f)
    val netLiquid1Week = collateralDeposited - brazilTax30 + yield1Week - cost1Week

    val yield6Months = collateralDeposited * (compoundYieldRate * 0.5f)
    val cost6Months = dynamicLicenseValue * 0.5f
    val netLiquid6Months = collateralDeposited - brazilTax30 + yield6Months - cost6Months

    val netLiquidToday = collateralDeposited - brazilTax30 // Initial value with local taxes cleared immediately
    
    val yield2Years = collateralDeposited * (compoundYieldRate * 2.0f)
    val cost2Years = dynamicLicenseValue
    val netLiquid2Years = collateralDeposited - brazilTax30 + yield2Years - cost2Years

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B19))
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High-Tech Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10172D)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, Color(0xFF00FFCC))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Security Core",
                    tint = Color(0xFF00FFCC),
                    modifier = Modifier.size(36.dp)
                )
                Column {
                    Text(
                        text = if (currentLanguage == AppLanguage.ENGLISH) "SECURE KERNEL & TRIPLE REDUNDANCY" 
                               else if (currentLanguage == AppLanguage.ESPANOL) "NÚCLEO SEGURO Y TRIPLE REDUNDANCIA" 
                               else "NÚCLEO SEGURO E TRÍPLICE REDUNDÂNCIA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (currentLanguage == AppLanguage.ENGLISH) "Active-Active Autonomous Load Balancer & Zero-Knowledge Secure Enclave"
                               else if (currentLanguage == AppLanguage.ESPANOL) "Balanceador Autónomo Activo-Activo y Enclave Seguro de Conocimiento Cero"
                               else "Balanceador Autônomo Ativo-Ativo e Enclave Seguro de Conhecimento Zero",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8892B0)
                    )
                }
            }
        }

        // ========================== COMPREHENSIVE SYSTEM AUDIT & ZKP CERTIFICATION SUITE ==========================
        SystemAuditAndCertificationSuite(
            currentLanguage = currentLanguage,
            onAddLog = onAddLog,
            modifier = Modifier.fillMaxWidth()
        )

        // ========================== UNIFIED ZKP • DILITHIUM • VMSB-NATIVO TRIAD VISUALIZER ==========================
        ZkpDilithiumVmsbVisualizerCard(
            modifier = Modifier.fillMaxWidth()
        )

        // ========================== ANTI-REA VMSB-NATIVE DILITHIUM REVERSE ENGINEERING PROTECTION ==========================
        AntiReaVmsbDilithiumCard(
            currentLanguage = currentLanguage,
            onAddLog = onAddLog,
            modifier = Modifier.fillMaxWidth()
        )

        // ========================== EDGE INFRASTRUCTURE • SSL • CDN • ANTI-DDOS (vmsb©) ==========================
        EdgeNetworkSecurityCard(
            onAddLog = onAddLog,
            modifier = Modifier.fillMaxWidth()
        )

        // ========================== ZERO-KNOWLEDGE PROOF (ZKP) MOVE INTEGRITY CARD ==========================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("zkp_move_integrity_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1020)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, if (isGameZkpEnforced) Color(0xFF00FF88) else Color(0xFFFF3B30))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title and State Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🛡️", fontSize = 18.sp)
                        Column {
                            Text(
                                text = "FACTION MOVE INTEGRITY CORE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Zero-Knowledge State Transition Circuit (zk-SNARK)",
                                fontSize = 8.sp,
                                color = Color(0xFF94A3B8),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    
                    // Pulsing / Active Status Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isGameZkpEnforced) Color(0x2200FF88) else Color(0x22FF3B30))
                            .border(1.dp, if (isGameZkpEnforced) Color(0xFF00FF88) else Color(0xFFFF3B30), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isGameZkpEnforced) "ACTIVE ENFORCEMENT" else "BYPASSED / UNSECURED",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGameZkpEnforced) Color(0xFF00FF88) else Color(0xFFFF3B30),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Divider(color = Color(0x1F94A3B8))

                Text(
                    text = "Protects the state boundaries between the Alliance North and Rival South factions. Generates non-interactive cryptographic commitments to verify move constraints before updating database sectors.",
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 12.sp
                )

                // Warning of Simulated Tamper/Hack Attacks
                if (zkpTamperWarning != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x33FF3B30), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFFF3B30), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🚨 CORRUPT STATE TRANSITION DETECTED!",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF3B30),
                                fontFamily = FontFamily.Monospace
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss Warning",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onDismissTamperWarning() }
                            )
                        }
                        Text(
                            text = zkpTamperWarning ?: "",
                            fontSize = 8.sp,
                            color = Color.White,
                            lineHeight = 11.sp
                        )
                        Text(
                            text = "COMPLIANCE ACTION: Invalid witness aborted. Regional database rolled back successfully.",
                            fontSize = 7.sp,
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Controls: Toggle enforcement, Simulate Tamper, Reset Audit Ledger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Toggle Switch Button
                    Button(
                        onClick = {
                            audioEngine.playKeyTap()
                            onGameZkpEnforcedChange(!isGameZkpEnforced)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isGameZkpEnforced) Color(0x3300E5FF) else Color(0x33FF3B30)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isGameZkpEnforced) Color(0xFF00E5FF) else Color(0xFFFF3B30)),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("toggle_zkp_enforce_btn"),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(
                            text = if (isGameZkpEnforced) "🛡️ ACTIVE: ZKP ON" else "⚠️ INACTIVE: ZKP OFF",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isGameZkpEnforced) Color(0xFF00E5FF) else Color(0xFFFF3B30)
                        )
                    }

                    // Simulate Hack Attack / Tamper Button
                    Button(
                        onClick = {
                            audioEngine.playKeyTap()
                            onSimulateTamper()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A1525)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF5252)),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("simulate_zkp_tamper_btn"),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "⚡ SIMULATE HACK",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFF5252)
                        )
                    }

                    // Reset Audit Ledger Button
                    Button(
                        onClick = {
                            audioEngine.playKeyTap()
                            onClearZkpMoves()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("clear_zkp_ledger_btn"),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "🧹 CLEAR LOGS",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Verified Ledger Display Flow
                Text(
                    text = "VERIFIED CONQUEST TRANSITIONS LEDGER (BLOCKCHAIN STYLE)",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FF88),
                    fontFamily = FontFamily.Monospace
                )

                if (zkpMoves.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Color(0xFF060B15), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0x3394A3B8), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recorded moves. Conquest zones are idle.\nWaiting for Alliance North/South movements...",
                            fontSize = 8.sp,
                            color = Color(0xFF64748B),
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        zkpMoves.forEach { move ->
                            var isItemExpanded by remember { mutableStateOf(false) }
                            
                            val factionColor = when (move.faction) {
                                "alliance_north" -> Color(0xFF00E676)
                                "rival_south" -> Color(0xFFFF1744)
                                "player" -> Color(0xFF00E5FF)
                                else -> Color(0xFFFF9100)
                            }
                            
                            val factionLabel = when (move.faction) {
                                "alliance_north" -> "ALLIANCE NORTH"
                                "rival_south" -> "RIVAL SOUTH"
                                "player" -> "PLAYER GENERAL"
                                else -> "ROGUE STATE / TAMPER"
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF060B15), RoundedCornerShape(6.dp))
                                    .border(
                                        1.dp,
                                        if (move.isVerified) Color(0x3300FF88) else Color(0x66FF3B30),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { isItemExpanded = !isItemExpanded }
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(factionColor)
                                        )
                                        Text(
                                            text = factionLabel,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = factionColor,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    // Status Badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (move.isVerified) Color(0x1F00FF88) else Color(0x1FFF3B30))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = if (move.isVerified) "ZKP SECURE" else "REJECTED (BAD WITNESS)",
                                            fontSize = 6.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (move.isVerified) Color(0xFF00FF88) else Color(0xFFFF3B30),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Sector: ${move.regionName} (ID: ${move.regionId})",
                                        fontSize = 8.sp,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Type: ${move.actionType}",
                                        fontSize = 7.sp,
                                        color = Color(0xFF64748B),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Text(
                                    text = "Proof Hash: ${move.proofHash}",
                                    fontSize = 7.sp,
                                    color = Color(0xFF64748B),
                                    fontFamily = FontFamily.Monospace
                                )

                                if (isItemExpanded) {
                                    Divider(color = Color(0x1394A3B8), modifier = Modifier.padding(vertical = 4.dp))
                                    Text(
                                        text = move.details,
                                        fontSize = 7.sp,
                                        color = Color(0xFF94A3B8),
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ========================== REAL-TIME STRUCTURAL CRISIS MONITORING DASHBOARD ==========================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("crisis_dashboard_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F162E)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, when (activeNodesCount) {
                3 -> Color(0xFF00FFCC)
                2 -> Color(0xFFFFD600)
                1 -> Color(0xFFFF3D00)
                else -> Color.Red
            })
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Crisis Monitor",
                            tint = when (activeNodesCount) {
                                3 -> Color(0xFF00FFCC)
                                2 -> Color(0xFFFFD600)
                                else -> Color(0xFFFF3D00)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = when (currentLanguage) {
                                AppLanguage.ENGLISH -> "REAL-TIME CRISIS & REDUNDANCY MONITOR"
                                AppLanguage.ESPANOL -> "MONITOREO DE CRISIS Y REDUNDANCIA"
                                else -> "MONITOR DE CRISE E REDUNDÂNCIA"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Badge(
                        containerColor = when (activeNodesCount) {
                            3 -> Color(0x2600FFCC)
                            2 -> Color(0x26FFD600)
                            1 -> Color(0x33FF3D00)
                            else -> Color(0x33FF0000)
                        },
                        contentColor = when (activeNodesCount) {
                            3 -> Color(0xFF00FFCC)
                            2 -> Color(0xFFFFD600)
                            1 -> Color(0xFFFF5252)
                            else -> Color.Red
                        }
                    ) {
                        Text(
                            text = when (activeNodesCount) {
                                3 -> "TRIPLE ACTIVE"
                                2 -> "HOT FAILOVER"
                                1 -> "SINGLE CRITICAL"
                                else -> "SYSTEM DOWN"
                            },
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Dynamic Alert Banner depending on activeNodesCount
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = when (activeNodesCount) {
                                3 -> Color(0x1000FFCC)
                                2 -> Color(0x15FFD600)
                                1 -> Color(0x20FF3D00)
                                else -> Color(0x33FF0000)
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = when (activeNodesCount) {
                                3 -> Color(0x4000FFCC)
                                2 -> Color(0x40FFD600)
                                1 -> Color(0x60FF3D00)
                                else -> Color(0x90FF0000)
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = when (activeNodesCount) {
                                3 -> "🟢 STATUS: TRIPLE ACTIVE-ACTIVE BALANCED"
                                2 -> "🟡 WARNING: REDUNDANCY DEGRADED"
                                1 -> "⚠️ CRITICAL: UNDER-ALLOCATION THRESHOLD"
                                else -> "🚨 EMERGENCY: COMPLETE COMPUTE BREAKDOWN"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (activeNodesCount) {
                                3 -> Color(0xFF00FFCC)
                                2 -> Color(0xFFFFD600)
                                1 -> Color(0xFFFF5252)
                                else -> Color.Red
                            },
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = when (activeNodesCount) {
                                3 -> "All three geographical secure enclaves are in active-active lockstep sync. Lyapunov control is converging at 100% capacity."
                                2 -> "Operating at 2/3 active nodes. Remaining clusters automatic overclocking to 6.3 GHz in order to sustain mathematical throughput."
                                1 -> "Imminent logical collapse danger! Single core operating at peak boosted thermal limit of 12.6 GHz. Active data corruption and lag threat high."
                                else -> "Zero math-logical execution nodes online. Automatic backup buffer holding queue active but database replication suspended."
                            },
                            fontSize = 9.sp,
                            color = Color(0xFF8892B0),
                            lineHeight = 12.sp
                        )
                    }
                }

                // Mathematical Compute Performance Metric readout & Live-updating Canvas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "MATH-LOGICAL COMPUTE EFFICIENCY (η)",
                            fontSize = 9.sp,
                            color = Color(0xFF5BC0BE),
                            fontFamily = FontFamily.Monospace
                        )
                        val lastEff = computeEfficiencyHistory.lastOrNull() ?: 0f
                        Text(
                            text = "${String.format("%.1f", lastEff)}%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "CAPACITY: ${String.format("%.1f", targetNodeFrequency * activeNodesCount)} GHz / 12.6 GHz",
                            fontSize = 10.sp,
                            color = Color(0xFF8892B0),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "THROUGHPUT: ${String.format("%.0f", if (activeNodesCount > 0) simulatedTrafficLoad else 0f)} RPS",
                            fontSize = 10.sp,
                            color = Color(0xFF00E5FF),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // The scrolling real-time canvas chart
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(Color(0xFF070B19), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF1B233F), RoundedCornerShape(6.dp))
                ) {
                    val width = size.width
                    val height = size.height

                    // Draw grid reference lines (75%, 50%, 25%)
                    val percentages = listOf(0.25f, 0.50f, 0.75f, 1.0f)
                    percentages.forEach { percentage ->
                        val y = height * (1f - percentage)
                        drawLine(
                            color = Color(0x1200FFCC),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    if (computeEfficiencyHistory.isNotEmpty()) {
                        val maxPoints = 15
                        val xStep = width / (maxPoints - 1).coerceAtLeast(1)
                        val path = Path()

                        computeEfficiencyHistory.forEachIndexed { index, value ->
                            val normalizedY = height * (1f - (value / 100f).coerceIn(0f, 1f))
                            val x = index * xStep
                            if (index == 0) {
                                path.moveTo(x, normalizedY)
                            } else {
                                path.lineTo(x, normalizedY)
                            }
                        }

                        drawPath(
                            path = path,
                            color = when (activeNodesCount) {
                                3 -> Color(0xFF00FFCC)
                                2 -> Color(0xFFFFD600)
                                1 -> Color(0xFFFF3D00)
                                else -> Color.Red
                            },
                            style = Stroke(width = 4f)
                        )
                    }
                }

                // WORKLOAD DIVIDER & METRIC ANALYSIS
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SIMULATED TRAFFIC WORKLOAD",
                            fontSize = 9.sp,
                            color = Color(0xFF5BC0BE),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${simulatedTrafficLoad.toInt()} RPS (Total Limit: 15k)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Slider(
                        value = simulatedTrafficLoad,
                        onValueChange = { simulatedTrafficLoad = it },
                        valueRange = 1000f..15000f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00FFCC),
                            activeTrackColor = Color(0xFF00FFCC),
                            inactiveTrackColor = Color(0xFF1B233F)
                        ),
                        modifier = Modifier.testTag("traffic_load_slider")
                    )

                    // Individual load divider bars (Dallas, Frankfurt, Tokyo)
                    val loadPerNode = if (activeNodesCount > 0) simulatedTrafficLoad / activeNodesCount else 0f
                    val maxCapacityPerNode = 5000f

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        @Composable
                        fun NodeLoadBar(nodeName: String, isActive: Boolean, load: Float) {
                            val utilization = (load / maxCapacityPerNode).coerceAtMost(1f)
                            val isOverloaded = utilization > 0.85f

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$nodeName: ${if (isActive) "${load.toInt()} RPS" else "OFFLINE"}",
                                    fontSize = 10.sp,
                                    color = if (isActive) Color.White else Color(0xFF555D75),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (isActive) "${(utilization * 100).toInt()}% UTIL" else "0% CAP",
                                    fontSize = 9.sp,
                                    color = if (!isActive) Color(0xFF555D75) else if (isOverloaded) Color(0xFFFF5252) else Color(0xFF00FFCC),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            LinearProgressIndicator(
                                progress = { if (isActive) utilization else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (isOverloaded) Color(0xFFFF3D00) else Color(0xFF00FFCC),
                                trackColor = Color(0xFF0B1123)
                            )
                        }

                        NodeLoadBar("DALLAS (NA-1)", nodeNaActive, loadPerNode)
                        NodeLoadBar("FRANKFURT (EU-2)", nodeEuActive, loadPerNode)
                        NodeLoadBar("TOKYO (AP-4)", nodeApacActive, loadPerNode)
                        NodeLoadBar("SYDNEY (AU-5)", nodeAuActive, loadPerNode)
                    }
                }

                // CRISIS INJECTOR ACTION CONTROLS
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "CRISIS INJECTOR CONTROL TERMINAL",
                        fontSize = 9.sp,
                        color = Color(0xFF5BC0BE),
                        fontFamily = FontFamily.Monospace
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { nodeNaActive = !nodeNaActive },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("toggle_na_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (nodeNaActive) Color(0x1F00FFCC) else Color(0x33FF3D00),
                                    contentColor = if (nodeNaActive) Color(0xFF00FFCC) else Color(0xFFFF5252)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (nodeNaActive) Color(0xFF00FFCC) else Color(0xFFFF3D00))
                            ) {
                                Text("DALLAS ${if (nodeNaActive) "ON" else "OFF"}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { nodeEuActive = !nodeEuActive },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("toggle_eu_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (nodeEuActive) Color(0x1F00FFCC) else Color(0x33FF3D00),
                                    contentColor = if (nodeEuActive) Color(0xFF00FFCC) else Color(0xFFFF5252)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (nodeEuActive) Color(0xFF00FFCC) else Color(0xFFFF3D00))
                            ) {
                                Text("FRANKFURT ${if (nodeEuActive) "ON" else "OFF"}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { nodeApacActive = !nodeApacActive },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("toggle_apac_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (nodeApacActive) Color(0x1F00FFCC) else Color(0x33FF3D00),
                                    contentColor = if (nodeApacActive) Color(0xFF00FFCC) else Color(0xFFFF5252)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (nodeApacActive) Color(0xFF00FFCC) else Color(0xFFFF3D00))
                            ) {
                                Text("TOKYO ${if (nodeApacActive) "ON" else "OFF"}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { nodeAuActive = !nodeAuActive },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("toggle_au_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (nodeAuActive) Color(0x1F00FFCC) else Color(0x33FF3D00),
                                    contentColor = if (nodeAuActive) Color(0xFF00FFCC) else Color(0xFFFF5252)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (nodeAuActive) Color(0xFF00FFCC) else Color(0xFFFF3D00))
                            ) {
                                Text("SYDNEY ${if (nodeAuActive) "ON" else "OFF"}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            nodeNaActive = true
                            nodeEuActive = true
                            nodeApacActive = true
                            nodeAuActive = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("reset_nodes_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00FFCC),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Recover", modifier = Modifier.size(14.dp))
                            Text("EXECUTE KERNEL HOT-FAILOVER RESET", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Interactive Audit Logger Console
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF070B19), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF1B233F), RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "AUDIT LOGGER REAL-TIME EVENTS",
                        fontSize = 8.sp,
                        color = Color(0xFF5BC0BE),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    crisisLogs.takeLast(4).forEach { log ->
                        Text(
                            text = ">> $log",
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (log.contains("ALERT") || log.contains("EMERGENCY")) Color(0xFFFF5252) else if (log.contains("restored")) Color(0xFF00FFCC) else Color(0xFF8892B0),
                            lineHeight = 11.sp
                        )
                    }
                }
            }
        }

        // Row containing Authentication Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AUTHENTICATION MODULE
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B35)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isAuthed) Color(0xFF00FFCC) else Color(0x3300FFCC))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.ENGLISH) "🔑 ZKP SECURE AUTHENTICATION"
                                   else if (currentLanguage == AppLanguage.ESPANOL) "🔑 AUTENTICACIÓN ZKP SEGURA"
                                   else "🔑 AUTENTICAÇÃO ZKP SEGURA",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )

                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isAuthed) Color(0xFF00FFCC) else Color(0xFFFF4949))
                        )
                    }

                    Text(
                        text = if (currentLanguage == AppLanguage.ENGLISH) "Verify identity securely using zero-knowledge Schnorr commitments. Your private key never leaves the device."
                               else if (currentLanguage == AppLanguage.ESPANOL) "Verifique su identidad de forma segura usando compromisos Schnorr de conocimiento cero. Su clave privada nunca sale del dispositivo."
                               else "Verifique sua identidade com segurança usando compromissos Schnorr de conhecimento zero. Sua chave privada nunca sai do dispositivo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8892B0),
                        lineHeight = 16.sp
                    )

                    // Displays current Private key
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0B1123), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MEMBER SECRET SEED",
                                fontSize = 10.sp,
                                color = Color(0xFF5BC0BE),
                                fontFamily = FontFamily.Monospace
                            )
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Secret",
                                tint = Color(0xFF5BC0BE),
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable {
                                        scope.launch {
                                            isGeneratingKey = true
                                            delay(800)
                                            val hex = "0x" + List(40) { "0123456789abcdef".random() }.joinToString("")
                                            privateKey = hex
                                            authTerminalLogs = authTerminalLogs + "Generated new unique deterministic seed $hex"
                                            isGeneratingKey = false
                                        }
                                    }
                            )
                        }
                        
                        Text(
                            text = if (isGeneratingKey) "GENERATING CRITICAL PARAMS..." else privateKey,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Simulated Terminal Output logs for Auth verification
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Color.Black, RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF222C4E), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        authTerminalLogs.forEach { log ->
                            Text(
                                text = "> $log",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = if (log.contains("Success") || log.contains("Authenticated")) Color(0xFF00FFCC) else Color(0xFF8892B0)
                            )
                        }
                    }

                    // Interactive verification button
                    Button(
                        onClick = {
                            scope.launch {
                                isVerifyingAuth = true
                                authTerminalLogs = authTerminalLogs + "Initializing ZK Proof commitment..."
                                delay(600)
                                authTerminalLogs = authTerminalLogs + "Challenge generated from server: C = Hash(G, Y, T)"
                                delay(600)
                                authTerminalLogs = authTerminalLogs + "Proof response: S = V - C*X (mod Q) calculated."
                                delay(500)
                                authTerminalLogs = authTerminalLogs + "Success: G^S * Y^C matches T. Secure authorization GRANTED!"
                                isAuthed = true
                                isVerifyingAuth = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAuthed) Color(0x3300FFCC) else Color(0xFF00FFCC),
                            contentColor = if (isAuthed) Color(0xFF00FFCC) else Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verify_zkp_auth_btn"),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isVerifyingAuth
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isAuthed) Icons.Default.CheckCircle else Icons.Default.Done,
                                contentDescription = "Verify Auth"
                            )
                            Text(
                                text = if (isVerifyingAuth) "COMPUTING..." 
                                       else if (isAuthed) "ZKP SECURED" 
                                       else "GENERATE & VERIFY ZKP AUTH",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // ========================== TRIPLE ACTIVE-ACTIVE REDUNDANCY MODULE ==========================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F162E)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, Color(0xFF5BC0BE))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section Title with physical metadata formula
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (currentLanguage == AppLanguage.ENGLISH) "🌐 TRIPLE ACTIVE-ACTIVE REDUNDANT KERNEL"
                                   else if (currentLanguage == AppLanguage.ESPANOL) "🌐 NÚCLEO CON TRIPLE REDUNDANCIA ACTIVA-ACTIVA"
                                   else "🌐 NÚCLEO COM TRÍPLICE REDUNDÂNCIA ATIVA-ATIVA",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Geographic Load Balancing Rule: Sum(f_i) >= 12.6 GHz | Latency L_i = f(d_i) / S_i",
                            fontSize = 10.sp,
                            color = Color(0xFF00FFCC),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Badge(
                        containerColor = if (activeNodesCount == 3) Color(0x3300FFCC) else if (activeNodesCount > 0) Color(0x33FFB703) else Color(0x33FF4949),
                        contentColor = if (activeNodesCount == 3) Color(0xFF00FFCC) else if (activeNodesCount > 0) Color(0xFFFFB703) else Color(0xFFFF4949)
                    ) {
                        Text(
                            text = dynamicEnclaveState,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Text(
                    text = if (currentLanguage == AppLanguage.ENGLISH) "Simulate node interruptions. When any node falls offline, the remaining active-active server clusters automatically overclock their processors dynamically to sustain full mathematical computing throughput with zero packet loss or delay."
                           else if (currentLanguage == AppLanguage.ESPANOL) "Simule interrupciones de nodos. Cuando un servidor cae, los clusters restantes se aceleran dinámicamente de forma automática para sostener el cómputo total sin pérdida de paquetes ni demoras."
                           else "Simule interrupções de nós. Quando qualquer servidor falha, os clusters restantes aceleram dinamicamente seus processadores para manter o processamento total com zero de atraso.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8892B0)
                )

                // Interactive Cluster Toggles & Displays
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Node 1: North America
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { nodeNaActive = !nodeNaActive },
                            colors = CardDefaults.cardColors(
                                containerColor = if (nodeNaActive) Color(0xFF131D3F) else Color(0x22FF4949)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (nodeNaActive) Color(0xFF00FFCC) else Color(0xFFFF4949)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (nodeNaActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = "Node NA",
                                    tint = if (nodeNaActive) Color(0xFF00FFCC) else Color(0xFFFF4949),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("NODE NA-1", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                    Text("Dallas Enclave", fontSize = 8.sp, color = Color(0xFF8892B0))
                                }
                                
                                Divider(color = Color(0xFF222C4E))

                                Text(
                                    text = if (nodeNaActive) "${String.format("%.2f", targetNodeFrequency)} GHz" else "0.0 GHz (OFFLINE)",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (nodeNaActive) Color(0xFF00FFCC) else Color(0xFFFF4949),
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    text = if (nodeNaActive) "ACTIVE-ACTIVE" else "DISRUPTED",
                                    fontSize = 7.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (nodeNaActive) Color(0xFF8892B0) else Color(0xFFFF4949)
                                )
                            }
                        }

                        // Node 2: Europe
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { nodeEuActive = !nodeEuActive },
                            colors = CardDefaults.cardColors(
                                containerColor = if (nodeEuActive) Color(0xFF131D3F) else Color(0x22FF4949)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (nodeEuActive) Color(0xFF00FFCC) else Color(0xFFFF4949)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (nodeEuActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = "Node EU",
                                    tint = if (nodeEuActive) Color(0xFF00FFCC) else Color(0xFFFF4949),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("NODE EU-2", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                    Text("Frankfurt Enclave", fontSize = 8.sp, color = Color(0xFF8892B0))
                                }
                                
                                Divider(color = Color(0xFF222C4E))

                                Text(
                                    text = if (nodeEuActive) "${String.format("%.2f", targetNodeFrequency)} GHz" else "0.0 GHz (OFFLINE)",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (nodeEuActive) Color(0xFF00FFCC) else Color(0xFFFF4949),
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    text = if (nodeEuActive) "ACTIVE-ACTIVE" else "DISRUPTED",
                                    fontSize = 7.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (nodeEuActive) Color(0xFF8892B0) else Color(0xFFFF4949)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Node 3: Asia-Pacific
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { nodeApacActive = !nodeApacActive },
                            colors = CardDefaults.cardColors(
                                containerColor = if (nodeApacActive) Color(0xFF131D3F) else Color(0x22FF4949)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (nodeApacActive) Color(0xFF00FFCC) else Color(0xFFFF4949)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (nodeApacActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = "Node APAC",
                                    tint = if (nodeApacActive) Color(0xFF00FFCC) else Color(0xFFFF4949),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("NODE AP-4", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                    Text("Tokyo Enclave", fontSize = 8.sp, color = Color(0xFF8892B0))
                                }
                                
                                Divider(color = Color(0xFF222C4E))

                                Text(
                                    text = if (nodeApacActive) "${String.format("%.2f", targetNodeFrequency)} GHz" else "0.0 GHz (OFFLINE)",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (nodeApacActive) Color(0xFF00FFCC) else Color(0xFFFF4949),
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    text = if (nodeApacActive) "ACTIVE-ACTIVE" else "DISRUPTED",
                                    fontSize = 7.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (nodeApacActive) Color(0xFF8892B0) else Color(0xFFFF4949)
                                )
                            }
                        }

                        // Node 4: Australia
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { nodeAuActive = !nodeAuActive },
                            colors = CardDefaults.cardColors(
                                containerColor = if (nodeAuActive) Color(0xFF131D3F) else Color(0x22FF4949)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (nodeAuActive) Color(0xFF00FFCC) else Color(0xFFFF4949)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (nodeAuActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = "Node AU",
                                    tint = if (nodeAuActive) Color(0xFF00FFCC) else Color(0xFFFF4949),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("NODE AU-5", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                    Text("Sydney Enclave", fontSize = 8.sp, color = Color(0xFF8892B0))
                                }
                                
                                Divider(color = Color(0xFF222C4E))

                                Text(
                                    text = if (nodeAuActive) "${String.format("%.2f", targetNodeFrequency)} GHz" else "0.0 GHz (OFFLINE)",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (nodeAuActive) Color(0xFF00FFCC) else Color(0xFFFF4949),
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    text = if (nodeAuActive) "ACTIVE-ACTIVE" else "DISRUPTED",
                                    fontSize = 7.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (nodeAuActive) Color(0xFF8892B0) else Color(0xFFFF4949)
                                )
                            }
                        }
                    }
                }

                // Mathematical Dynamic Performance Indicators
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0A0F24), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF1E294B), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AGGREGATED POWER OUTPUT", fontSize = 9.sp, color = Color(0xFF5BC0BE), fontFamily = FontFamily.Monospace)
                        Text(
                            text = "${String.format("%.2f", targetNodeFrequency * activeNodesCount)} / 12.60 GHz",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeNodesCount > 0) Color(0xFF00FFCC) else Color(0xFFFF4949),
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = if (activeNodesCount > 0) 1.0f else 0.0f,
                            color = Color(0xFF00FFCC),
                            trackColor = Color(0xFF1A233D),
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("SYSTEM ENTROPY INDEX", fontSize = 9.sp, color = Color(0xFF5BC0BE), fontFamily = FontFamily.Monospace)
                        val entropy = if (activeNodesCount == 3) 0.041f else if (activeNodesCount == 2) 0.125f else if (activeNodesCount == 1) 0.380f else 1.0f
                        Text(
                            text = "H = ${String.format("%.4f", entropy)} bit",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (entropy < 0.2f) Color(0xFF00FFCC) else if (entropy < 0.5f) Color(0xFFFFB703) else Color(0xFFFF4949),
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = entropy.coerceIn(0f, 1f),
                            color = if (entropy < 0.2f) Color(0xFF00FFCC) else if (entropy < 0.5f) Color(0xFFFFB703) else Color(0xFFFF4949),
                            trackColor = Color(0xFF1A233D),
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                        )
                    }
                }

                // ========================== PQC GOSSIP PROTOCOL & CONSENSUS TERMINAL ==========================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF090E20)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E294B))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "📡 PQC GOSSIP & MERKLE TREE CONSENSUS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Zero-Trust Mesh: f(GDR-TE) shared via NATS bus",
                                    fontSize = 8.sp,
                                    color = Color(0xFF8892B0),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            
                            // Gossip On/Off Switch
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "GOSSIP BEACON",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isGossipActive) Color(0xFF00FFCC) else Color(0xFFFF4949)
                                )
                                Switch(
                                    checked = isGossipActive,
                                    onCheckedChange = { isGossipActive = it },
                                    modifier = Modifier.scale(0.7f).testTag("gossip_beacon_switch")
                                )
                            }
                        }

                        Divider(color = Color(0xFF1A233D))

                        // Dynamic Consensus Status Banner
                        val rootHashNa = if (nodeNaActive) "4a2f8b1c90e3f7d2" else "0000000000000000"
                        val rootHashEu = if (nodeEuActive) "c9d2e1f4a5b6c7d8" else "0000000000000000"
                        val rootHashAp = if (nodeApacActive) "7f8e9a0b1c2d3e4f" else "0000000000000000"
                        val rootHashAu = if (nodeAuActive) "3b4c5d6e7f8a9b0c" else "0000000000000000"

                        val isMerkleConsensusMatched = nodeNaActive && nodeEuActive && nodeApacActive && nodeAuActive
                        
                        val consensusHash = if (isMerkleConsensusMatched) {
                            "0xBC74F9A2B1C0E9D8F7A6E5D4C3B2A1F0"
                        } else if (activeNodesCount > 0) {
                            "0x" + String.format("%08X", (rootHashNa + rootHashEu + rootHashAp + rootHashAu).hashCode()).uppercase() + "F9A2B1C0E9D8F7A6"
                        } else {
                            "0x00000000000000000000000000000000"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isMerkleConsensusMatched) Color(0x1100FFCC) else Color(0x11FF4949),
                                    RoundedCornerShape(6.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isMerkleConsensusMatched) Color(0xFF00FFCC) else Color(0xFFFF4949),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isMerkleConsensusMatched) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = "Consensus Status",
                                tint = if (isMerkleConsensusMatched) Color(0xFF00FFCC) else Color(0xFFFF4949),
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = if (isMerkleConsensusMatched) "CONSENSUS SECURED (VALID SYNC)" else "CONSENSUS DEGRADED / DIVERGED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMerkleConsensusMatched) Color(0xFF00FFCC) else Color(0xFFFF4949),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Merkle Root: $consensusHash",
                                    fontSize = 7.5.sp,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Merkle Tree Roots List (Small grid for the 4 nodes)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "LOCAL MERKLE TREE ROOT HASHES:",
                                fontSize = 8.sp,
                                color = Color(0xFF5BC0BE),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            
                            @Composable
                            fun NodeHashRow(nodeLabel: String, isActive: Boolean, hash: String) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(
                                                    if (isActive) Color(0xFF00FFCC) else Color(0xFFFF4949),
                                                    CircleShape
                                                )
                                        )
                                        Text(
                                            text = nodeLabel,
                                            fontSize = 8.sp,
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        text = "Root: $hash",
                                        fontSize = 8.sp,
                                        color = if (isActive) Color(0xFF8892B0) else Color(0xFFFF4949).copy(alpha = 0.6f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            NodeHashRow("NODE NA-1 (Dallas)", nodeNaActive, rootHashNa)
                            NodeHashRow("NODE EU-2 (Frankfurt)", nodeEuActive, rootHashEu)
                            NodeHashRow("NODE AP-4 (Tokyo)", nodeApacActive, rootHashAp)
                            NodeHashRow("NODE AU-5 (Sydney)", nodeAuActive, rootHashAu)
                        }

                        // Real-time Gossip Bus Stream terminal
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "NATS ENCRYPTED GOSSIP STREAM [KYBER PQC]",
                                fontSize = 8.sp,
                                color = Color(0xFF5BC0BE),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(70.dp)
                                    .background(Color(0xFF04060E), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF1E294B), RoundedCornerShape(6.dp))
                                    .padding(6.dp)
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    reverseLayout = true
                                ) {
                                    items(gossipLogs) { log ->
                                        Text(
                                            text = log,
                                            fontSize = 8.sp,
                                            color = if (log.contains("Alert")) Color(0xFFFF4949) else Color(0xFF00FFCC).copy(alpha = 0.85f),
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Cross-Block Attestation Panel
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "🛡️ CROSS-BLOCK ATTESTATION (DILITHIUM-5 MASTER ATTRIBUTION)",
                                fontSize = 8.sp,
                                color = Color(0xFF5BC0BE),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = "Submit verified location/state changes from physical devices. Active peer nodes must cryptographically verify and sign the block before recording the ledger.",
                                fontSize = 8.sp,
                                color = Color(0xFF8892B0),
                                lineHeight = 10.sp
                            )
                            
                            Spacer(modifier = Modifier.height(2.dp))

                            val scope = rememberCoroutineScope()
                            Button(
                                onClick = {
                                    scope.launch {
                                        isAttestationSigning = true
                                        attestationLogs = listOf("⚡ [BLOCK SIGNING] Initiating master Dilithium-5 attestation from Dallas...")
                                        delay(1500)
                                        
                                        attestationLogs = attestationLogs + "🔑 Generating PQC key signature for battle ledger block..."
                                        delay(1500)
                                        
                                        val signature = "0x" + List(24) { "0123456789ABCDEF".random() }.joinToString("")
                                        attestationLogs = attestationLogs + "📝 Master Signature produced: $signature"
                                        delay(1500)
                                        
                                        attestationLogs = attestationLogs + "📡 Broadcasting block via NATS bus for cross-validation..."
                                        delay(1500)
                                        
                                        var validatedCount = 1
                                        if (nodeEuActive) {
                                            attestationLogs = attestationLogs + "✅ Frankfurt [NODE EU-2] checked master signature: VERIFIED. Signing block."
                                            validatedCount++
                                            delay(1200)
                                        }
                                        if (nodeApacActive) {
                                            attestationLogs = attestationLogs + "✅ Tokyo [NODE AP-4] checked master signature: VERIFIED. Signing block."
                                            validatedCount++
                                            delay(1200)
                                        }
                                        if (nodeAuActive) {
                                            attestationLogs = attestationLogs + "✅ Sydney [NODE AU-5] checked master signature: VERIFIED. Signing block."
                                            validatedCount++
                                            delay(1200)
                                        }
                                        
                                        if (validatedCount >= 2) {
                                            attestationLogs = attestationLogs + "🎉 Ledger updated successfully. Block committed across $validatedCount nodes."
                                        } else {
                                            attestationLogs = attestationLogs + "❌ Consensus failure: Not enough active peer nodes to verify block signature (Required >= 2)."
                                        }
                                        isAttestationSigning = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .testTag("broadcast_attestation_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E294B),
                                    contentColor = Color(0xFF00FFCC)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFF00FFCC)),
                                enabled = !isAttestationSigning && activeNodesCount > 0
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isAttestationSigning) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 1.5.dp,
                                            color = Color(0xFF00FFCC)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Broadcast Block",
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Text(
                                        text = if (isAttestationSigning) "VALIDATING..." else "BROADCAST PHYSICAL ATTRIBUTION EVENT",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .background(Color(0xFF04060E), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF1E294B), RoundedCornerShape(6.dp))
                                    .padding(6.dp)
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    items(attestationLogs) { log ->
                                        Text(
                                            text = log,
                                            fontSize = 8.sp,
                                            color = if (log.contains("❌")) Color(0xFFFF4949) else if (log.contains("✅") || log.contains("🎉")) Color(0xFF00FFCC) else Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ========================== THE THREE GATES (PORTALES OPERACIONALES) ==========================
                Text(
                    text = if (currentLanguage == AppLanguage.ENGLISH) "🚪 ACTIVE OPERATIONAL GATES (ISOLATED EXECUTION POOLS)"
                           else if (currentLanguage == AppLanguage.ESPANOL) "🚪 PUERTAS OPERACIONALES (POOLS DE EJECUCIÓN AISLADOS)"
                           else "🚪 PORTAIS OPERACIONAIS ATIVOS (EXECUÇÃO ISOLADA)",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                // Gate Tabs Selector
                TabRow(
                    selectedTabIndex = selectedGateIndex,
                    containerColor = Color(0xFF10172D),
                    contentColor = Color(0xFF00FFCC),
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedGateIndex]),
                            color = Color(0xFF00FFCC)
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedGateIndex == 0,
                        onClick = { selectedGateIndex = 0 },
                        text = {
                            Text(
                                "GATE 1: ENTERPRISE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    )
                    Tab(
                        selected = selectedGateIndex == 1,
                        onClick = { selectedGateIndex = 1 },
                        text = {
                            Text(
                                "GATE 2: ACADEMIC",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    )
                    Tab(
                        selected = selectedGateIndex == 2,
                        onClick = { selectedGateIndex = 2 },
                        text = {
                            Text(
                                "GATE 3: CULT / STUD",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    )
                }

                // Render dynamic content for the selected Gate
                when (selectedGateIndex) {
                    0 -> {
                        // GATE 1: Enterprise - Decisiones Críticas con Lyapunov control
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🏢 ENTERPRISE GATEWAY - LYAPUNOV STABILITY ENGINE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                                Badge(containerColor = Color(0x1D00FFCC)) {
                                    Text("HIGH COMPUTATIONAL SECURITY", fontSize = 8.sp, color = Color(0xFF00FFCC), modifier = Modifier.padding(4.dp))
                                }
                            }

                            Text(
                                text = "Ensures real-time structural stability analysis of crisis nodes using positive-definite quadratic Lyapunov Candidate Functions V(x) = α * x₁² + β * x₂². The load balance remains globally asymptotically stable if dV/dt < 0.",
                                fontSize = 10.sp,
                                color = Color(0xFF8892B0)
                            )

                            // Parameters Sliders
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("α Coefficient: $lyapunovAlpha", fontSize = 9.sp, color = Color(0xFF5BC0BE), fontFamily = FontFamily.Monospace)
                                    Slider(
                                        value = lyapunovAlpha,
                                        onValueChange = { lyapunovAlpha = it },
                                        valueRange = 0.5f..2.5f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00FFCC), activeTrackColor = Color(0xFF00FFCC))
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("β Coefficient: $lyapunovBeta", fontSize = 9.sp, color = Color(0xFF5BC0BE), fontFamily = FontFamily.Monospace)
                                    Slider(
                                        value = lyapunovBeta,
                                        onValueChange = { lyapunovBeta = it },
                                        valueRange = 0.5f..2.5f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00FFCC), activeTrackColor = Color(0xFF00FFCC))
                                    )
                                }
                            }

                            // Dynamic Math Plot: Lyapunov Phase space orbit
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(Color.Black, RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF1E294B), RoundedCornerShape(8.dp))
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val width = size.width
                                    val height = size.height
                                    val centerX = width / 2
                                    val centerY = height / 2

                                    // Draw grid axes
                                    drawLine(
                                        color = Color(0xFF1E294B),
                                        start = androidx.compose.ui.geometry.Offset(0f, centerY),
                                        end = androidx.compose.ui.geometry.Offset(width, centerY),
                                        strokeWidth = 1f
                                    )
                                    drawLine(
                                        color = Color(0xFF1E294B),
                                        start = androidx.compose.ui.geometry.Offset(centerX, 0f),
                                        end = androidx.compose.ui.geometry.Offset(centerX, height),
                                        strokeWidth = 1f
                                    )

                                    // Draw elliptic Lyapunov contour curves V(x) = constant
                                    for (c in listOf(20f, 40f, 60f, 80f)) {
                                        val path = Path()
                                        val steps = 100
                                        for (i in 0..steps) {
                                            val angle = (i.toFloat() / steps) * 2 * Math.PI.toFloat()
                                            val rx = (c / lyapunovAlpha) * 1.8f
                                            val ry = (c / lyapunovBeta) * 0.9f
                                            val x = centerX + rx * cos(angle)
                                            val y = centerY + ry * sin(angle)
                                            if (i == 0) {
                                                path.moveTo(x, y)
                                            } else {
                                                path.lineTo(x, y)
                                            }
                                        }
                                        path.close()
                                        drawPath(
                                            path = path,
                                            color = Color(0x4400FFCC),
                                            style = Stroke(width = 1.5f)
                                        )
                                    }

                                    // Draw decaying orbital trajectory toward asymptotic equilibrium
                                    val trajectoryPath = Path()
                                    var currentR = 100f
                                    val steps = 150
                                    for (i in 0..steps) {
                                        val t = i.toFloat()
                                        val angle = t * 0.15f + (simulationTicks * 0.05f)
                                        currentR = 100f * (1.0f - (t / steps))
                                        val rx = (currentR / lyapunovAlpha) * 1.8f
                                        val ry = (currentR / lyapunovBeta) * 0.9f
                                        val x = centerX + rx * cos(angle)
                                        val y = centerY + ry * sin(angle)
                                        if (i == 0) {
                                            trajectoryPath.moveTo(x, y)
                                        } else {
                                            trajectoryPath.lineTo(x, y)
                                        }
                                    }
                                    drawPath(
                                        path = trajectoryPath,
                                        color = Color(0xFFFFAA00),
                                        style = Stroke(width = 2f)
                                    )
                                }

                                Text(
                                    text = "Asymptotic Lyapunov Decay: V(x) -> 0 (As t -> ∞)",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF8892B0),
                                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                                )
                                Text(
                                    text = "dV/dt = -0.0418 * (f_NA + f_EU)",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF00FFCC),
                                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                                )
                            }
                        }
                    }

                    1 -> {
                        // GATE 2: Elite Academic - Funciones Reales Avanzadas (Interactive Wave Generator)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🎓 ELITE ACADEMIC - REAL REAL-TIME WAVE PARALYSIS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                                Badge(containerColor = Color(0x1D00E5FF)) {
                                    Text("HIGH-PRECISION MATH PROOFS", fontSize = 8.sp, color = Color(0xFF00E5FF), modifier = Modifier.padding(4.dp))
                                }
                            }

                            Text(
                                text = "Visualizing the continuous trigonometric real functions space: f(x) = A * sin(k * x - ω * t) + B * cos(2 * k * x). Adjustable frequency and amplitudes allow testing analytical boundary values.",
                                fontSize = 10.sp,
                                color = Color(0xFF8892B0)
                            )

                            // Parameters Sliders
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Frequency (k): ${String.format("%.1f", waveFrequency)} rad/m", fontSize = 9.sp, color = Color(0xFF5BC0BE), fontFamily = FontFamily.Monospace)
                                    Slider(
                                        value = waveFrequency,
                                        onValueChange = { waveFrequency = it },
                                        valueRange = 1.0f..10.0f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Amplitude (A): ${String.format("%.1f", waveAmplitude)} px", fontSize = 9.sp, color = Color(0xFF5BC0BE), fontFamily = FontFamily.Monospace)
                                    Slider(
                                        value = waveAmplitude,
                                        onValueChange = { waveAmplitude = it },
                                        valueRange = 10.0f..60.0f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                                    )
                                }
                            }

                            // Canvas Wave Plotter
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(Color.Black, RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF1E294B), RoundedCornerShape(8.dp))
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val width = size.width
                                    val height = size.height
                                    val centerY = height / 2

                                    // Axes
                                    drawLine(
                                        color = Color(0xFF1E294B),
                                        start = androidx.compose.ui.geometry.Offset(0f, centerY),
                                        end = androidx.compose.ui.geometry.Offset(width, centerY),
                                        strokeWidth = 1f
                                    )

                                    // Plotting real wave equation f(x)
                                    val wavePath = Path()
                                    val steps = 200
                                    for (i in 0..steps) {
                                        val x = (i.toFloat() / steps) * width
                                        // k variable
                                        val k = (i.toFloat() / steps) * waveFrequency * Math.PI.toFloat()
                                        // f(x) formula
                                        val y1 = waveAmplitude * sin(k - wavePhase)
                                        val y2 = (waveAmplitude / 2f) * cos(2 * k + wavePhase * 0.5f)
                                        val y = centerY + y1 + y2
                                        
                                        if (i == 0) {
                                            wavePath.moveTo(x, y)
                                        } else {
                                            wavePath.lineTo(x, y)
                                        }
                                    }

                                    drawPath(
                                        path = wavePath,
                                        color = Color(0xFF00E5FF),
                                        style = Stroke(width = 2.5f)
                                    )
                                }

                                Text(
                                    text = "Wave Envelope: f(x,t) ∈ L²(ℝ) Hilbert Space",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF8892B0),
                                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                                )
                                Text(
                                    text = "t_phase = ${String.format("%.2f", wavePhase)} rad",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF00E5FF),
                                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                                )
                            }
                        }
                    }

                    2 -> {
                        // GATE 3: Cult/Students - Volumen Masivo con Ingreso por Goteo (Drip Feed Engine)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "👥 CULT & STUDENT GATEWAY - ISOLATED DECENTRALIZED POOL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                                Badge(containerColor = Color(0x1DFFB703)) {
                                    Text("MASSIVE DRIP FLOW", fontSize = 8.sp, color = Color(0xFFFFB703), modifier = Modifier.padding(4.dp))
                                }
                            }

                            Text(
                                text = "Captures high volume micro-payments and student math validation tasks. Operates within an isolated sandboxed layer, guaranteeing that high traffic surges never drain hardware resources allocated for structural analysis of critical core assets.",
                                fontSize = 10.sp,
                                color = Color(0xFF8892B0)
                            )

                            // Interactive stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10172D)),
                                    border = BorderStroke(1.dp, Color(0xFF222C4E))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("TOTAL TASKS", fontSize = 8.sp, color = Color(0xFF8892B0))
                                        Text("$microTasksCompleted", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB703), fontFamily = FontFamily.Monospace)
                                    }
                                }

                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10172D)),
                                    border = BorderStroke(1.dp, Color(0xFF222C4E))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("REVENUE DRIP FLOW", fontSize = 8.sp, color = Color(0xFF8892B0))
                                        Text("$${String.format("%.4f", microTasksCompleted * 0.0003)} /sec", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FFCC), fontFamily = FontFamily.Monospace)
                                    }
                                }

                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10172D)),
                                    border = BorderStroke(1.dp, Color(0xFF222C4E))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("SANDBOX ENFORCED", fontSize = 8.sp, color = Color(0xFF8892B0))
                                        Text("100% ISOLATED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FFCC), fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }

                            // Microtask execution logger
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(Color.Black, RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF222C4E), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val baseIndex = (simulationTicks * 2)
                                for (i in 0..12) {
                                    val taskIndex = baseIndex + i
                                    val userHash = "0x" + (1000 + (taskIndex % 899)).toString()
                                    Text(
                                        text = "[Sandbox Drip] Student Validation $taskIndex -> Verified for $userHash. Reward: 0.0003 USD. Cpu pool isolated.",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (i % 2 == 0) Color(0xFF8892B0) else Color(0xFF5BC0BE)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ========================== SMART LICENSING MARKETPLACE & VALUATION ENGINE ==========================
        if (!isAuthed) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0x44FF4949)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFFF4949))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Locked Gateway",
                        tint = Color(0xFFFF4949),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = if (currentLanguage == AppLanguage.ENGLISH) "SECURE MARKETPLACE LOCKED"
                               else if (currentLanguage == AppLanguage.ESPANOL) "MERCADO SEGURO BLOQUEADO"
                               else "MERCADO SEGURO BLOQUEADO",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (currentLanguage == AppLanguage.ENGLISH) "Please execute the ZKP Secure Authentication above to unlock real-time bank interconnectivity and smart licensing."
                               else if (currentLanguage == AppLanguage.ESPANOL) "Por favor ejecute la Autenticación ZKP arriba para desbloquear la interconexión bancaria real y licencias inteligentes."
                               else "Por favor, execute a Autenticação ZK acima para desbloquear a interconexão bancária e licenciamento inteligente.",
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        color = Color(0xFF8892B0)
                    )
                }
            }
        } else {
            // UNLOCKED STATE: Smart license products & Residual calculator
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Product selector cards
                Text(
                    text = if (currentLanguage == AppLanguage.ENGLISH) "⚙️ SELECT INNOVATIVE SMART LICENSE PRODUCT:"
                           else if (currentLanguage == AppLanguage.ESPANOL) "⚙️ SELECCIONE UN PRODUCTO DE LICENCIA:"
                           else "⚙️ SELECIONE UM PRODUTO DE LICENÇA:",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val products = listOf(
                        Triple("AI_COCKPIT", "AI COCKPIT", "45 USD"),
                        Triple("CONQUEST_NODE", "CONQUEST NODE", "90 USD"),
                        Triple("QUANTUM_GRID", "QUANTUM GRID", "180 USD")
                    )

                    products.forEach { (type, name, price) ->
                        val isSelected = selectedLicenseType == type
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedLicenseType = type },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF1E294B) else Color(0xFF10172D)
                            ),
                            border = BorderStroke(
                                width = 1.5.dp,
                                color = if (isSelected) Color(0xFF00FFCC) else Color(0x338892B0)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = when (type) {
                                        "AI_COCKPIT" -> Icons.Outlined.Info
                                        "CONQUEST_NODE" -> Icons.Outlined.Build
                                        else -> Icons.Outlined.Settings
                                    },
                                    contentDescription = name,
                                    tint = if (isSelected) Color(0xFF00FFCC) else Color(0xFF8892B0)
                                )
                                Text(
                                    text = name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = price,
                                    fontSize = 12.sp,
                                    color = Color(0xFF00FFCC),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Interactive Horizon residual value visualization (Costo Cero / Yield left over time)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131B35)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.ENGLISH) "📈 ZERO NET COST & LIQUIDITY MATRIX"
                                   else if (currentLanguage == AppLanguage.ESPANOL) "📈 MATRIZ DE LIQUIDEZ Y COSTO CERO"
                                   else "📈 MATRIZ DE LIQUIDEZ E CUSTO ZERO",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = if (currentLanguage == AppLanguage.ENGLISH) "By staking license collateral into the secure decentralized network pool, your smart license earns optimization yields. See residual liquid value left at each timeframe:"
                                   else if (currentLanguage == AppLanguage.ESPANOL) "Al depositar colateral de licencia en el pool seguro descentralizado, su licencia gana rendimiento de optimización. Vea el valor líquido residual:"
                                   else "Ao depositar colateral de licença no pool seguro descentralizado, sua licença ganha rendimento de otimização. Veja o valor líquido residual:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8892B0)
                        )

                        // Slider to adjust scale/months of license
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0B1123), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "INTELLIGENT LICENSE TERM",
                                    fontSize = 10.sp,
                                    color = Color(0xFF5BC0BE),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${licensePeriodMonths.toInt()} Months",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = licensePeriodMonths,
                                onValueChange = { licensePeriodMonths = it },
                                valueRange = 1f..12f,
                                steps = 11,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00E5FF),
                                    activeTrackColor = Color(0xFF00E5FF),
                                    inactiveTrackColor = Color(0xFF222C4E)
                                )
                            )
                        }

                        // Display the 5 Horizons: Today, 1 Day, 1 Week, 6 Months, 2 Years
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Helper function to render a time horizon card
                            @Composable
                            fun HorizonCard(title: String, netVal: Float, label: String, modifier: Modifier) {
                                Card(
                                    modifier = modifier,
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10172D)),
                                    border = BorderStroke(1.dp, Color(0xFF222C4E))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5BC0BE))
                                        Text(
                                            text = "$${String.format("%.2f", netVal)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (netVal >= collateralDeposited) Color(0xFF00FFCC) else Color.White
                                        )
                                        Text(
                                            text = label,
                                            fontSize = 8.sp,
                                            color = Color(0xFF8892B0),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HorizonCard("TODAY", netLiquidToday, "Liquid Value Today", Modifier.weight(1f))
                                HorizonCard("1 DAY", netLiquid1Day, "Init Setup", Modifier.weight(1f))
                                HorizonCard("1 WEEK", netLiquid1Week, "Breakeven Check", Modifier.weight(1f))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HorizonCard("6 MONTHS", netLiquid6Months, "Mid Horizon", Modifier.weight(1f))
                                HorizonCard("2 YEARS", netLiquid2Years, "Max Staking Compound", Modifier.weight(1f))
                            }
                        }

                        // Collateral Explainer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x1A00FFCC), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "Yield Model", tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
                            Text(
                                text = "Zero Net Cost Model: With a collateral stake of $${String.format("%.0f", collateralDeposited)} USD, passive yields of ${(baseStakeYieldRate * 100).toInt()}% completely recover the license cost in full over time, leaving a high liquid value on deposit.",
                                fontSize = 10.sp,
                                color = Color(0xFF00FFCC),
                                lineHeight = 13.sp
                            )
                        }
                    }
                }

                // SECURE ZKP BANK PAYMENT GATEWAY PORTAL
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131B35)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF00FFCC))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.ENGLISH) "🏦 INTEGRATED ZERO-KNOWLEDGE PAYMENTS GATEWAY"
                                   else if (currentLanguage == AppLanguage.ESPANOL) "🏦 PASARELA DE PAGOS ZK INTEGRADA"
                                   else "🏦 PASSARELA DE PAGAMENTOS ZK INTEGRADA",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = if (currentLanguage == AppLanguage.ENGLISH) "Direct connection to your private account & bank using a Zero-Knowledge Proof (ZKP) circuit. This proves sufficiency of funds without sharing your total balance or account IBAN."
                                   else if (currentLanguage == AppLanguage.ESPANOL) "Conexión directa a su cuenta privada y banco usando un circuito ZKP. Esto prueba la suficiencia de fondos sin compartir su saldo total o IBAN."
                                   else "Conexão direta à sua conta privada e banco usando circuito ZKP. Prova a suficiência de fundos sem compartilhar seu saldo total ou IBAN.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8892B0)
                        )

                        // Bank link display
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0B1123), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("INTEGRATED BANK ACCOUNT", fontSize = 9.sp, color = Color(0xFF5BC0BE), fontFamily = FontFamily.Monospace)
                                Text("Banco de la República ••••• 9021-ZKP", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Badge(
                                containerColor = Color(0x3300FFCC),
                                contentColor = Color(0xFF00FFCC)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Secure Link", modifier = Modifier.size(10.dp))
                                    Text("ZKP SECURE", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Checkout state and interactive log console
                        if (showZkpCheckout) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black, RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF222C4E), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "ZKP PAYMENT GATEWAY TERMINAL",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color(0xFF00FFCC),
                                    fontWeight = FontWeight.Bold
                                )

                                checkoutLogs.forEach { log ->
                                    Text(
                                        text = ">> $log",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = if (log.contains("SUCCESS") || log.contains("VERIFIED")) Color(0xFF00FFCC) else Color(0xFF8892B0)
                                    )
                                }

                                if (isCheckingOut) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        color = Color(0xFF00FFCC),
                                        trackColor = Color(0xFF10172D)
                                    )
                                }
                            }
                        }

                        // Order Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("TOTAL DUE (STAKE DEPOSIT)", fontSize = 10.sp, color = Color(0xFF8892B0))
                                Text("$${String.format("%.2f", collateralDeposited)} USD", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    showZkpCheckout = true
                                    isCheckingOut = true
                                    checkoutSuccess = false
                                    checkoutLogs = listOf("Initiating zero-knowledge proof circuit for bank balance...")
                                    scope.launch {
                                        delay(800)
                                        checkoutLogs = checkoutLogs + "Formulating Schnorr noninteractive proof of balance (h = g^x)..."
                                        delay(800)
                                        checkoutLogs = checkoutLogs + "Proving: Balance > $collateralDeposited USD without disclosure..."
                                        delay(700)
                                        checkoutLogs = checkoutLogs + "ZKP Proof Hash: 0x8e2b9...e3cf2 submitted to bank gateway."
                                        delay(800)
                                        checkoutLogs = checkoutLogs + "GATEWAY RESPONSE: VERIFIED SUCCESSFULLY. Transfer approved."
                                        delay(500)
                                        checkoutLogs = checkoutLogs + "SUCCESS: Smart License $selectedLicenseType active!"
                                        isCheckingOut = false
                                        checkoutSuccess = true
                                        onAdjustCredits(50000)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00FFCC),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isCheckingOut,
                                modifier = Modifier.testTag("zkp_pay_checkout_btn")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "ZKP Check")
                                    Text("PAY WITH SECURE ZKP", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ========================== GLOBAL SMART CONTRACTS PLATFORM ==========================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10172D)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF222C4E))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = LanguageUtils.translate("secure_smart_contracts", currentLanguage),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Contracts",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = when (currentLanguage) {
                        AppLanguage.ENGLISH -> "Register global digital contracts for non-exclusive distribution and licensing of telemetry-simulation nodes under the standard 'As-Is' framework."
                        AppLanguage.ESPANOL -> "Registre contratos digitales globales para la distribución no exclusiva y licenciamiento de nodos de telemetría bajo el marco estándar 'As-Is'."
                        AppLanguage.PORTUGUES -> "Registre contratos digitais globais para distribuição não exclusiva e licenciamento de nós de telemetria sob a estrutura padrão 'As-Is'."
                    },
                    fontSize = 9.sp,
                    color = Color(0xFF8892B0),
                    lineHeight = 12.sp
                )

                Divider(color = Color(0xFF1E293B))

                // Form Fields
                OutlinedTextField(
                    value = scCounterparty,
                    onValueChange = {
                        audioEngine.playKeyTap()
                        scCounterparty = it
                    },
                    label = { Text(LanguageUtils.translate("sc_counterparty", currentLanguage), fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF222C4E),
                        focusedLabelColor = Color(0xFF00E5FF),
                        unfocusedLabelColor = Color(0xFF64748B)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("sc_counterparty_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${LanguageUtils.translate("sc_micro_tariff", currentLanguage)}: ${String.format("%.1f", scMicroTariff)}",
                        fontSize = 10.sp,
                        color = Color(0xFF8892B0),
                        fontFamily = FontFamily.Monospace
                    )
                    Slider(
                        value = scMicroTariff,
                        onValueChange = {
                            scMicroTariff = it
                        },
                        valueRange = 1.0f..100.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("sc_micro_tariff_slider")
                    )
                }

                OutlinedTextField(
                    value = scContractType,
                    onValueChange = {
                        audioEngine.playKeyTap()
                        scContractType = it
                    },
                    label = { Text(LanguageUtils.translate("sc_contract_type", currentLanguage), fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF222C4E),
                        focusedLabelColor = Color(0xFF00E5FF),
                        unfocusedLabelColor = Color(0xFF64748B)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("sc_contract_type_input")
                )

                // Deterministic signature hash box
                val currentContractHash = remember(scCounterparty, scMicroTariff, scContractType) {
                    calculateSha256("$scCounterparty|$scMicroTariff|$scContractType")
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B1123), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = LanguageUtils.translate("sc_generated_hash", currentLanguage).uppercase() + ":",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB74D),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = currentContractHash,
                        fontSize = 9.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 11.sp
                    )
                }

                // Structured table of contract parameters
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF222C4E), RoundedCornerShape(6.dp))
                        .background(Color(0xFF070B19))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF10172D))
                            .padding(8.dp)
                    ) {
                        Text("PARAMETER", modifier = Modifier.weight(1.2f), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                        Text("VALUE", modifier = Modifier.weight(1.8f), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                        Text("STATUS", modifier = Modifier.weight(1f), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace, textAlign = TextAlign.End)
                    }
                    Divider(color = Color(0xFF222C4E))
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Text("License", modifier = Modifier.weight(1.2f), fontSize = 9.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                        Text("Non-Exclusive As-Is", modifier = Modifier.weight(1.8f), fontSize = 9.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold)
                        Text("VALID", modifier = Modifier.weight(1f), fontSize = 8.sp, color = Color(0xFF00FF88), fontWeight = FontWeight.Bold, textAlign = TextAlign.End, fontFamily = FontFamily.Monospace)
                    }
                    Divider(color = Color(0x1F222C4E))
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Text("Call tariff", modifier = Modifier.weight(1.2f), fontSize = 9.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                        Text("${String.format("%.1f", scMicroTariff)} Credits", modifier = Modifier.weight(1.8f), fontSize = 9.sp, color = Color(0xFFFFB74D))
                        Text("ONLINE", modifier = Modifier.weight(1f), fontSize = 8.sp, color = Color(0xFF00FF88), fontWeight = FontWeight.Bold, textAlign = TextAlign.End, fontFamily = FontFamily.Monospace)
                    }
                }

                // High-fidelity Barcode element
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B1123), RoundedCornerShape(6.dp))
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .background(Color(0xFF070B19))
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val bars = listOf(2, 4, 1, 3, 2, 5, 1, 4, 2, 1, 3, 2, 4, 1, 5, 2, 1, 3, 2, 4, 1)
                        bars.forEach { w ->
                            Spacer(
                                modifier = Modifier
                                    .width(w.dp)
                                    .fillMaxHeight()
                                    .background(Color(0xFF8892B0))
                            )
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "*LIC-" + currentContractHash.take(12).uppercase() + "*",
                        fontSize = 7.sp,
                        color = Color(0xFF8892B0),
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Cryptographic Signature box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .border(1.dp, Color(0xFF222C4E), RoundedCornerShape(8.dp))
                        .background(Color(0xFF070B19))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("AUTHORIZED SIGNATURE MATRIX", fontSize = 7.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sign: /" + scCounterparty.replace(" ", "_") + "/_secured",
                                fontSize = 10.sp,
                                color = Color(0xFF00FFCC),
                                fontWeight = FontWeight.Bold,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text("SEAL VALID", fontSize = 6.sp, color = Color(0xFF00FF88), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("SYSTEMS AS-IS", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        audioEngine.playKeyTap()
                        scIsDeploying = true
                        scDeploySuccess = false
                        scContractLogs = listOf("Initiating smart contract validation pipeline...")
                        scope.launch {
                            delay(600)
                            scContractLogs = scContractLogs + "Validating counterparty parameters: $scCounterparty..."
                            delay(600)
                            scContractLogs = scContractLogs + "Computing SHA-256 hash of non-exclusive distribution contract..."
                            delay(500)
                            scContractLogs = scContractLogs + "SHA-256 Hash Generated: ${currentContractHash.take(24)}..."
                            delay(600)
                            scContractLogs = scContractLogs + "Deploying license node schema to active-active lattice..."
                            delay(500)
                            scContractLogs = scContractLogs + "SUCCESS: Smart Contract fully active and licensed!"
                            scIsDeploying = false
                            scDeploySuccess = true
                            audioEngine.playChimeSuccess()
                            onAddLog(
                                "SMART_CONTRACT",
                                "Contract licensed to [$scCounterparty] with call rate of ${String.format("%.1f", scMicroTariff)} credits. Seal: ${currentContractHash.take(16)}...",
                                "SYSTEM"
                            )
                        }
                    },
                    enabled = !scIsDeploying && scCounterparty.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("register_contract_btn")
                ) {
                    if (scIsDeploying) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("COMPUTING BLOCK SIGNATURE...", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Done, contentDescription = "Deploy Sign", modifier = Modifier.size(16.dp))
                            Text(LanguageUtils.translate("sc_deploy_btn", currentLanguage).uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                if (scContractLogs.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF030712), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        scContractLogs.forEach { log ->
                            Text(
                                text = "> $log",
                                fontSize = 8.sp,
                                color = if (log.contains("SUCCESS")) Color(0xFF00FF88) else Color(0xFF8892B0),
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // ========================== AreIA: 21ST-CENTURY SMART CORPORATE CONTRACTS & AUTOMATED COMPLIANCE SUITE ==========================
        Card(
            modifier = Modifier.fillMaxWidth().testTag("areia_suite_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1229)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, if (isSlaBreached) Color(0xFFFF1744) else Color(0xFF00FFCC))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFF00FFCC).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🤖", fontSize = 14.sp)
                        }
                        Column {
                            Text(
                                text = "AreIA COOP CORE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FFCC),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = when (currentLanguage) {
                                    AppLanguage.ENGLISH -> "21st-Century Intelligent Corporate Contract & SLA Audit"
                                    AppLanguage.ESPANOL -> "Contrato Corporativo Inteligente y Auditoría de SLA del Siglo XXI"
                                    AppLanguage.PORTUGUES -> "Contrato Corporativo Inteligente e Auditoria de SLA do Século XXI"
                                },
                                fontSize = 8.sp,
                                color = Color(0xFF8892B0),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Surface(
                        color = if (isSlaBreached) Color(0xFFFF1744).copy(alpha = 0.2f) else Color(0xFF00FF88).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, if (isSlaBreached) Color(0xFFFF1744) else Color(0xFF00FF88)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isSlaBreached) "⚠️ SLA BREACHED" else "🟢 SLA COMPLIANT",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSlaBreached) Color(0xFFFF1744) else Color(0xFF00FF88),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Text(
                    text = when (currentLanguage) {
                        AppLanguage.ENGLISH -> "AreIA is a smart corporate governance protocol that integrates automated escrow triggers, live multi-tenant SLA auditing, and ZKP-encrypted telemetry contract negotiation in real-time."
                        AppLanguage.ESPANOL -> "AreIA es un protocolo de gobernanza corporativa inteligente que integra disparadores de custodia automatizados, auditoría de SLA multi-inquilino en vivo y negociación de contratos cifrados con ZKP."
                        AppLanguage.PORTUGUES -> "AreIA é um protocolo de governança corporativa inteligente que integra gatilhos de custódia automatizados, auditoria de SLA multi-inquilino e negociação de contratos ZKP."
                    },
                    fontSize = 9.sp,
                    color = Color(0xFF8892B0),
                    lineHeight = 12.sp
                )

                Divider(color = Color(0xFF1E293B))

                // Section 1: Multi-Clause Builder
                Text(
                    text = "STEP 1: SELECT ACTIVE SMART CLAUSES",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val clauses = listOf(
                        Triple("ZKP_PRIVACY", "🛡️ ZKP PRIVACY", "Zero-knowledge shielding of asset balances"),
                        Triple("SMART_ESCROW", "🏦 SMART ESCROW", "Automatic fund locking & refund guarantees"),
                        Triple("CYBER_ARBITRATION", "⚖️ ARBITRATION", "Pre-authorized cybernetic corporate court")
                    )

                    clauses.forEach { (key, label, desc) ->
                        val isSelected = areiaSelectedClauses.contains(key)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                audioEngine.playKeyTap()
                                areiaSelectedClauses = if (isSelected) {
                                    areiaSelectedClauses - key
                                } else {
                                    areiaSelectedClauses + key
                                }
                                // Dynamically adjust Locked Escrow values based on clauses
                                areiaEscrowCreditsLocked = 200 + (areiaSelectedClauses.size * 150)
                                areiaEscrowAlloysLocked = 10 + (areiaSelectedClauses.size * 10)
                                areiaFeedbackLog = areiaFeedbackLog + "AreIA: Updated clause matrix. Locked escrow revised: $areiaEscrowCreditsLocked credits / $areiaEscrowAlloysLocked alloys."
                            },
                            label = { Text(label, fontSize = 8.sp, fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00FFCC).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF00FFCC),
                                containerColor = Color(0xFF070B19),
                                labelColor = Color(0xFF8892B0)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color(0x3300FFCC),
                                selectedBorderColor = Color(0xFF00FFCC)
                            )
                        )
                    }
                }

                // Section 2: ESCROW LOCK STATUS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF070B19), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "SMART CONTRACT ESCROW VAULT",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB74D),
                            fontFamily = FontFamily.Monospace
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "🔒 $areiaEscrowCreditsLocked CREDITS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "🔒 $areiaEscrowAlloysLocked ALLOYS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "ESCROW STATE",
                            fontSize = 6.sp,
                            color = Color(0xFF8892B0),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isSlaBreached) "REVERSION TRIGGERED" else "STOCKED & SIGNED",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSlaBreached) Color(0xFFFF1744) else Color(0xFF00FFCC),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Divider(color = Color(0xFF1E293B))

                // Section 3: SLA compliance Telemetry Live Auditor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STEP 2: LIVE SLA PERFORMANCE AUDITOR",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )

                    // Linear Indicator or Gauge status
                    Text(
                        text = "${String.format("%.2f", areiaSlaPerformance)}% AVAILABILITY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSlaBreached) Color(0xFFFF1744) else Color(0xFF00FF88),
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Telemetry audit bars
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = (areiaSlaPerformance / 100f).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isSlaBreached) Color(0xFFFF1744) else Color(0xFF00FFCC),
                        trackColor = Color(0x1F8892B0)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SLA Limit: 99.95%", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                        Text("Real-Time Telemetry Node Feed", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                    }
                }

                // Live Action Trigger: Breach/Stress test (VAMOS A PONERLO EN FUNCION!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            audioEngine.playChimeWarning()
                            isSlaBreached = true
                            areiaSlaPerformance = 99.34f
                            areiaFeedbackLog = areiaFeedbackLog + listOf(
                                "🚨 ALERT: Telemetry downtime incident detected! SLA dropped to 99.34% (under contract minimum 99.95%).",
                                "🏦 SMART ESCROW CLAUSE EXCITED: Initiating automatic compensation protocol...",
                                "💸 TRANSACTION: Disbursing reimbursement of 150 Credits to Corporate Pilot Victor Barreto."
                            )
                            onAdjustCredits(150) // Adds credits dynamically to player's balance!
                            onAddLog(
                                "AREIA_SLA_COMPLIANCE",
                                "🚨 SLA BREACH: Autonomous Escrow refunded 150 Credits to Player corporate account.",
                                "TACTICAL"
                            )
                        },
                        enabled = !isSlaBreached,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF1744),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF1E1F29),
                            disabledContentColor = Color(0xFF8892B0)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("sim_sla_breach_btn")
                    ) {
                        Text(
                            text = when (currentLanguage) {
                                AppLanguage.ENGLISH -> "💥 SIMULATE SLA INCIDENT"
                                AppLanguage.ESPANOL -> "💥 SIMULAR INCIDENTE DE SLA"
                                AppLanguage.PORTUGUES -> "💥 SIMULAR INCIDENTE DE SLA"
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Button(
                        onClick = {
                            audioEngine.playChimeSuccess()
                            isSlaBreached = false
                            areiaSlaPerformance = 99.98f
                            areiaFeedbackLog = areiaFeedbackLog + "🟢 SYSTEM RESTORE: SLA telemetry reset to 99.98%. Contract state restored."
                            onAddLog(
                                "AREIA_SLA_COMPLIANCE",
                                "🟢 SLA RESTORED: AreIA corporate system status returned to nominal levels (99.98%).",
                                "TACTICAL"
                            )
                        },
                        enabled = isSlaBreached,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00FFCC),
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF1E1F29),
                            disabledContentColor = Color(0xFF8892B0)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("restore_sla_btn")
                    ) {
                        Text(
                            text = when (currentLanguage) {
                                AppLanguage.ENGLISH -> "🔄 RE-CALIBRATE NOMINAL SLA"
                                AppLanguage.ESPANOL -> "🔄 RE-CALIBRAR SLA NOMINAL"
                                AppLanguage.PORTUGUES -> "🔄 RE-CALIBRAR SLA NOMINAL"
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Live auditor telemetry feed
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF030712), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "AreIA AUTONOMOUS COMPLIANCE FEED:",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FFCC),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    areiaFeedbackLog.takeLast(4).forEach { log ->
                        Text(
                            text = "> $log",
                            fontSize = 8.sp,
                            color = if (log.contains("🚨")) Color(0xFFFF1744) else if (log.contains("💸") || log.contains("🟢")) Color(0xFF00FF88) else Color(0xFF8892B0),
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 11.sp
                        )
                    }
                }

                Divider(color = Color(0xFF1E293B))

                // Section 4: AreIA Legal AI Advisor & Smart Chat Dialog
                Text(
                    text = "STEP 3: CONSULT AreIA CORPORATE LEGAL AI ADVISOR",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )

                // Select specialized advisor persona
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "CyberArbitrator" to "⚖️ DR. AreIA ARBITRATOR",
                        "TaxSentinel" to "🇧🇷 TAX-SENTINEL e-CAC",
                        "QuantumAudit" to "📊 QUANTUM SLA AUDITOR"
                    ).forEach { (personaId, label) ->
                        val isSelected = areiaSelectedPersona == personaId
                        Button(
                            onClick = {
                                audioEngine.playKeyTap()
                                areiaSelectedPersona = personaId
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF00FFCC).copy(alpha = 0.2f) else Color(0xFF070B19),
                                contentColor = if (isSelected) Color(0xFF00FFCC) else Color(0xFF8892B0)
                            ),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF00FFCC) else Color(0xFF1E293B)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(24.dp)
                        ) {
                            Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Quick corporate compliance query pills
                Text(
                    text = "QUICK COMPLIANCE DIALECTS:",
                    fontSize = 7.sp,
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickQueries = listOf(
                        "Optimizar e-CAC" to "¿Cómo se conecta la IA de AreIA para mitigar riesgos en la Receita Federal brasileira de e-CAC con ZKP?",
                        "SLA Contract 21" to "Redactar un borrador inteligente para el contrato del Siglo XXI de telemetría as-is.",
                        "Gobernanza Corporate" to "¿Cuáles son las ventajas corporativas del arbitraje cibernético automatizado de AreIA?"
                    )

                    quickQueries.forEach { (label, fullQuery) ->
                        Button(
                            onClick = {
                                audioEngine.playKeyTap()
                                areiaCustomQuery = fullQuery
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF070B19),
                                contentColor = Color(0xFF00FFCC)
                            ),
                            border = BorderStroke(1.dp, Color(0x3300FFCC)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.height(20.dp)
                        ) {
                            Text(label, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Custom Query input
                OutlinedTextField(
                    value = areiaCustomQuery,
                    onValueChange = {
                        audioEngine.playKeyTap()
                        areiaCustomQuery = it
                    },
                    placeholder = {
                        Text(
                            text = "Pregunta a AreIA AI o edita tu propuesta corporativa...",
                            fontSize = 9.sp,
                            color = Color(0xFF64748B)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00FFCC),
                        unfocusedBorderColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("areia_query_input")
                )

                // Transmit button
                Button(
                    onClick = {
                        audioEngine.playChimeSuccess()
                        isAreiaAnalyzing = true
                        areiaAiResponse = ""
                        
                        scope.launch {
                            delay(1200) // Simulated futuristic AI compiler delay
                            
                            areiaGeneratedAgreementHash = calculateSha256("AreIA|$areiaSelectedPersona|$areiaCustomQuery|$areiaSelectedClauses")
                            
                            areiaAiResponse = when (areiaSelectedPersona) {
                                "TaxSentinel" -> """
                                    🛡️ COMPILACIÓN TAX-SENTINEL e-CAC CONTRATO INTELIGENTE [CÓDIGO VERIFICADO]
                                    ========================================================================
                                    EMISOR CORPORATIVO: Victor Barreto (CPF: 880.***.***-53)
                                    SISTEMA COMPLEMENTARIO: Receita Federal e-CAC Integrator
                                    NODO ARBITRAL: AreIA Smart-Gov Ledger (Hash: ${areiaGeneratedAgreementHash.take(16)})
                                    
                                    DIAGNÓSTICO DE LA COMPATIBILIDAD TRIBUTARIA DEL SIGLO XXI:
                                    1. El sistema de AreIA utiliza Criptografía de Conocimiento Cero (ZKP) para demostrar el cumplimiento del impuesto del 30% brasileño (DARF) sin divulgar el saldo bruto acumulado en Nubank.
                                    2. Las cláusulas seleccionadas (${areiaSelectedClauses.joinToString(", ")}) están alineadas con la regulación de licenciamiento tecnológico no exclusivo "As-Is".
                                    3. RECOMENDACIÓN: El micro-arancel de telemetría de ${scMicroTariff} Credits está optimizado y no genera retención doble de ISS o ICMS corporativo internacional.
                                    
                                    ESTADO DEL CONTRATO: APTO PARA TRANSMISIÓN BINARIA.
                                """.trimIndent()
                                "QuantumAudit" -> """
                                    📊 INFORME DE RENDIMIENTO QUANTUM COMPLIANCE AUDIT
                                    ========================================================================
                                    OBJETIVO: Nodos de Simulación del Siglo XXI
                                    MÉTRICA ACTIVA: SLA de Telemetría Multi-Inquilino (Nominal: ${areiaSlaPerformance}%)
                                    CUSTODIA ASIGNADA: ${areiaEscrowCreditsLocked} Credits / ${areiaEscrowAlloysLocked} Alloys
                                    
                                    ANÁLISIS DE RESILIENCIA CORPORATIVA:
                                    - Las cláusulas de Smart Escrow se ejecutan de manera autónoma en la capa de consenso. Si el rendimiento del sensor cae por debajo de 99.95%, un reembolso instantáneo de 150 Credits es reintegrado al balance.
                                    - No se han detectado firmas huérfanas ni intentos de manipulación del software del simulador espacial.
                                    - El balance contractual es óptimo con baja volatilidad de aleación.
                                """.trimIndent()
                                else -> """
                                    ⚖️ DICTAMEN DE ARBITRAJE Dr. AreIA ARBITRATOR (SLA MULTI-CONTRATO)
                                    ========================================================================
                                    CONVENIO CORPORATIVO INTELIGENTE: NASA JPL / Sector Privado
                                    PROTOCOLOS: Custodia Criptográfica Auto-Ejecutable
                                    CÓDIGO DE TRAJECTORIA: ARC-${areiaGeneratedAgreementHash.take(12).uppercase()}
                                    
                                    ESTIPULACIÓN SMART DEL CONTRATO (S. XXI):
                                    1. Se ratifica que AreIA actuará como tribunal algorítmico único para resolver discrepancias en la transmisión de telemetría espacial.
                                    2. Las disputas sobre el consumo de oxígeno, combustible de descenso o peso del aterrizador se resuelven de forma descentralizada mediante transacciones ZKP.
                                    3. Queda prohibida la modificación manual de la constante de aceleración orbital sin el consentimiento firmado de ambas facciones (Alianza del Norte y Rival del Sur).
                                    
                                    ARBITRAJE SELLADO. CUMPLIMIENTO DIGITAL GARANTIZADO.
                                """.trimIndent()
                            }
                            
                            isAreiaAnalyzing = false
                            areiaFeedbackLog = areiaFeedbackLog + "AreIA: AI Consultation complete. Transmitted cryptographic report hash: ${areiaGeneratedAgreementHash.take(20)}"
                        }
                    },
                    enabled = !isAreiaAnalyzing && areiaCustomQuery.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00FFCC),
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF1E1F29),
                        disabledContentColor = Color(0x3300FFCC)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("areia_consult_btn")
                ) {
                    if (isAreiaAnalyzing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AreIA COMPILING CORPORATE LAW STATE...", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🤖 CONSULTAR AUDITOR CORPORATIVO AreIA", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // AI Response box
                if (areiaAiResponse.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF070B19), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🤖 AreIA SMART COMPLIANCE REPORT",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FFCC),
                                fontFamily = FontFamily.Monospace
                            )
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Signed Seal",
                                tint = Color(0xFF00FF88),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = areiaAiResponse,
                            fontSize = 9.sp,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Barcode of the agreement
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SIGN: /AreIA_Core_Arbitration_Seal_SXXI/",
                                fontSize = 7.sp,
                                color = Color(0xFF64748B),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "VERIFIED BY MULTI-TENANT LEDGER",
                                fontSize = 7.sp,
                                color = Color(0xFF00FF88),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // ========================== AreIA vmsb SMART LAUNCH PLATFORM & STUDENT WALLETS ==========================
        LaunchedEffect(playerCredits, areiaAutoOptimizerEnabled) {
            if (areiaAutoOptimizerEnabled && !areiaIsLaunchAgreementSigned) {
                // Autonomous optimization: Calculate ideal "Valor X" to fit player's budget
                val maxAllowedFee = playerCredits.toFloat() - 450f
                val idealFee = maxAllowedFee.coerceIn(500f, 3000f)
                // Round to nearest 50 credits
                areiaRetentionFeeX = (idealFee / 50f).toInt() * 50f
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth().testTag("areia_vmsb_launch_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF090D1C)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, if (areiaIsLaunchAgreementSigned) Color(0xFF00FF88) else Color(0xFFFFB74D))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFFFFB74D).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🚀", fontSize = 14.sp)
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "AreIA vmsb LAUNCH SUITE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB74D),
                                    fontFamily = FontFamily.Monospace
                                )
                                Surface(
                                    color = Color(0xFFFFD54F).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "vmsb Core v3.0",
                                        fontSize = 6.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFD54F),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Text(
                                text = "Gobernanza, Conocimiento y Riqueza del Siglo XXI",
                                fontSize = 8.sp,
                                color = Color(0xFF8892B0),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Surface(
                        color = if (areiaIsLaunchAgreementSigned) Color(0xFF00FF88).copy(alpha = 0.2f) else Color(0xFFFFB74D).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, if (areiaIsLaunchAgreementSigned) Color(0xFF00FF88) else Color(0xFFFFB74D)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (areiaIsLaunchAgreementSigned) "🟢 PLATFORM LAUNCHED" else "⏳ AWAITING LAUNCH",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (areiaIsLaunchAgreementSigned) Color(0xFF00FF88) else Color(0xFFFFB74D),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Sincere description in Spanish aligning with user's core intent
                Text(
                    text = "Este protocolo absorbe el costo final de lanzamiento y costas operativas en el precio final. Al activar la cláusula de retención directa por planeamiento, estudio y análisis, se compensa la propiedad intelectual de vmsb para democratizar la tecnología espacial y potenciar la investigación.",
                    fontSize = 9.sp,
                    color = Color(0xFF8892B0),
                    lineHeight = 12.sp
                )

                Divider(color = Color(0xFF1E293B))

                // Autonomous Switch Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0D1527), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.0f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "MODO AUTÓNOMO RE-BALANCING",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FFCC),
                                fontFamily = FontFamily.Monospace
                            )
                            Surface(
                                color = Color(0xFF00FFCC).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    text = "ACTIVO",
                                    fontSize = 5.sp,
                                    color = Color(0xFF00FFCC),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp)
                                )
                            }
                        }
                        Text(
                            text = "Calibra 'Valor X' inteligentemente para maximizar el aporte estudiantil y regalías vmsb sin riesgo de quiebra.",
                            fontSize = 8.sp,
                            color = Color(0xFF8892B0),
                            lineHeight = 11.sp
                        )
                    }
                    Switch(
                        checked = areiaAutoOptimizerEnabled,
                        onCheckedChange = {
                            audioEngine.playKeyTap()
                            areiaAutoOptimizerEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00FFCC),
                            checkedTrackColor = Color(0xFF00FFCC).copy(alpha = 0.3f),
                            uncheckedThumbColor = Color(0xFF475569),
                            uncheckedTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }

                // Section 1: Dynamic Pricing & Cost Absorption Slider
                Text(
                    text = if (areiaAutoOptimizerEnabled) "PASO 1: SELECCIÓN DE RETENCIÓN DIRECTA (VALOR X) [IA OPTIMIZADO]" else "PASO 1: SELECCIÓN DE RETENCIÓN DIRECTA (VALOR X) [MANUAL]",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF070B19), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (areiaAutoOptimizerEnabled) "Aporte por Planificación (Optimizado Autónomamente)" else "Aporte por Planificación (Selección Manual)",
                            fontSize = 8.sp,
                            color = Color(0xFFFFB74D),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${areiaRetentionFeeX.toInt()} CREDITS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Slider(
                        value = areiaRetentionFeeX,
                        onValueChange = {
                            if (!areiaIsLaunchAgreementSigned && !areiaAutoOptimizerEnabled) {
                                audioEngine.playKeyTap()
                                areiaRetentionFeeX = it
                            }
                        },
                        valueRange = 500f..3000f,
                        enabled = !areiaIsLaunchAgreementSigned && !areiaAutoOptimizerEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = if (areiaAutoOptimizerEnabled) Color(0xFF00FFCC) else Color(0xFFFFB74D),
                            activeTrackColor = if (areiaAutoOptimizerEnabled) Color(0xFF00FFCC) else Color(0xFFFFB74D),
                            inactiveTrackColor = Color(0xFF13192B)
                        ),
                        modifier = Modifier.fillMaxWidth().height(20.dp)
                    )

                    // Presets or status message
                    if (areiaAutoOptimizerEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("⚡", fontSize = 8.sp)
                            Text(
                                text = "El motor calcula el Valor X óptimo para mantener su saldo a salvo.",
                                fontSize = 8.sp,
                                color = Color(0xFF00FFCC),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(500f, 1000f, 1500f, 2500f).forEach { preset ->
                                val isSelected = areiaRetentionFeeX.toInt() == preset.toInt()
                                Button(
                                    onClick = {
                                        if (!areiaIsLaunchAgreementSigned) {
                                            audioEngine.playKeyTap()
                                            areiaRetentionFeeX = preset
                                        }
                                    },
                                    enabled = !areiaIsLaunchAgreementSigned,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) Color(0xFFFFB74D).copy(alpha = 0.2f) else Color(0xFF0F1426),
                                        contentColor = if (isSelected) Color(0xFFFFB74D) else Color(0xFF8892B0)
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFFFB74D) else Color(0xFF1E293B)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    modifier = Modifier.weight(1f).height(20.dp)
                                ) {
                                    Text("${preset.toInt()} Cr", fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }

                // Pricing Calculation Breakdown Panel
                val totalCostValue = areiaBasePrice + areiaRetentionFeeX
                val absorbedLaunchTaxes = totalCostValue * 0.15f
                val academicContributionAmount = totalCostValue * 0.20f

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF050915), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF111A2E), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "DESGLOSE CORPORATIVO DE INTEGRACIÓN",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FFCC),
                        fontFamily = FontFamily.Monospace
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Precio de Mercado Base (Democratizado):", fontSize = 8.sp, color = Color(0xFF8892B0))
                        Text("${areiaBasePrice.toInt()} Cr", fontSize = 8.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Retención Directa de Honorarios vmsb (Estudio & Análisis):", fontSize = 8.sp, color = Color(0xFF8892B0))
                        Text("+ ${areiaRetentionFeeX.toInt()} Cr", fontSize = 8.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Costas y Costos de Lanzamiento (15%):", fontSize = 8.sp, color = Color(0xFF8892B0))
                            Surface(color = Color(0xFF00FF88).copy(alpha = 0.15f), shape = RoundedCornerShape(3.dp)) {
                                Text("ABSORBIDOS EN EL PRECIO FINAL", fontSize = 5.sp, color = Color(0xFF00FF88), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp))
                            }
                        }
                        Text("${absorbedLaunchTaxes.toInt()} Cr", fontSize = 8.sp, color = Color(0xFF00FF88), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Aporte del 20% a Ciencia, Tecnología & Educación:", fontSize = 8.sp, color = Color(0xFFFFB74D))
                        Text("${academicContributionAmount.toInt()} Cr", fontSize = 8.sp, color = Color(0xFFFFB74D), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    Divider(color = Color(0xFF1E293B))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("VALOR NETO CONSOLIDADO (Todo Incluido):", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = "${totalCostValue.toInt()} CREDITS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00FFCC),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Divider(color = Color(0xFF1E293B))

                // Section 2: Student Wallets for active participation
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PASO 2: WALLETS ACADÉMICOS PARA ESTUDIANTES (20% AUTO-DISBURSAL)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Total Distribuido: ${areiaTotalDisbursedToStudents.toInt()} Cr",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FF88),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "El 20% del valor del lanzamiento se destina directamente a financiar carteras estudiantiles de programas aeroespaciales y de contratos inteligentes.",
                        fontSize = 8.sp,
                        color = Color(0xFF8892B0),
                        lineHeight = 11.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // List of 3 student wallets
                    areiaStudentWallets.forEach { (name, address, balance) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF070B19), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF111A2E), RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(address, fontSize = 7.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("BALANCE", fontSize = 6.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                Text("${balance.toInt()} CREDITS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FF88), fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Disburse Grant Button
                    Button(
                        onClick = {
                            audioEngine.playChimeSuccess()
                            val chunk = academicContributionAmount / 3f
                            areiaStudentWallets = areiaStudentWallets.map { (name, addr, bal) ->
                                Triple(name, addr, bal + chunk)
                            }
                            areiaTotalDisbursedToStudents += academicContributionAmount
                            areiaLaunchLog = areiaLaunchLog + "🎁 BECAS DISPERSADAS: Se transfirieron ${chunk.toInt()} Credits a cada una de las 3 wallets de estudiantes."
                            onAddLog(
                                "AREIA_VMSB_ACADEMIC",
                                "🎁 BECA ESTUDIANTIL: Dispersados ${academicContributionAmount.toInt()} Credits a wallets del programa vmsb.",
                                "TACTICAL"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB74D),
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF13192B),
                            disabledContentColor = Color(0xFF8892B0)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(32.dp).testTag("areia_vmsb_disburse_btn")
                    ) {
                        Text(
                            text = "🎁 DISPERSAR APORTE ACADÉMICO (20% - ${academicContributionAmount.toInt()} Cr)",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Divider(color = Color(0xFF1E293B))

                // ========================== IA HARD THINK: SOLUTIONS PORTAL ==========================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0A1024), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF1E2D52), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🧠", fontSize = 14.sp)
                            Column {
                                Text(
                                    text = "AreIA HARD-THINK SOLUTIONS ENGINE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00FFCC),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Asesoría y Soluciones de Gobernanza de Alta Complejidad",
                                    fontSize = 7.sp,
                                    color = Color(0xFF8892B0),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Surface(
                            color = Color(0xFF00FFCC).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, Color(0xFF00FFCC))
                        ) {
                            Text(
                                text = "SOLUTIONS READY",
                                fontSize = 6.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FFCC),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Text(
                        text = "Seleccione un desafío crítico para que la IA realice una simulación de alta complejidad matemática y legal (Hard-Think) y estructure soluciones automatizadas:",
                        fontSize = 8.sp,
                        color = Color(0xFF8892B0),
                        lineHeight = 11.sp
                    )

                    // Interactive Selection List (Selectable Chips)
                    val hardThinkQuestions = listOf(
                        "¿Distribución estudiantil del 20% sin intermediarios financieros?",
                        "¿Absorción legal de impuestos de lanzamiento en el precio?",
                        "¿Garantía perpetua de autoría intelectual 'vmsb' en el Ledger?"
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        hardThinkQuestions.forEachIndexed { index, question ->
                            val isSelected = areiaSelectedQuestionIndex == index
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) Color(0xFF00FFCC).copy(alpha = 0.08f) else Color(0xFF070B19),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF00FFCC) else Color(0xFF151D35),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        audioEngine.playKeyTap()
                                        areiaSelectedQuestionIndex = index
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            if (isSelected) Color(0xFF00FFCC) else Color(0xFF1E293B),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.Black,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    } else {
                                        Text("${index + 1}", fontSize = 7.sp, color = Color(0xFF8892B0), fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    text = question,
                                    fontSize = 8.5.sp,
                                    color = if (isSelected) Color.White else Color(0xFF8892B0),
                                    fontFamily = FontFamily.SansSerif,
                                    modifier = Modifier.weight(1.0f)
                                )
                            }
                        }
                    }

                    // Hard Think Consultation Trigger
                    val scope = rememberCoroutineScope()
                    Button(
                        onClick = {
                            audioEngine.playKeyTap()
                            scope.launch {
                                areiaIsHardThinking = true
                                areiaHardThinkSolution = ""
                                kotlinx.coroutines.delay(1200) // Simulated hyper-deep reasoning
                                areiaIsHardThinking = false
                                areiaHardThinkSolution = when(areiaSelectedQuestionIndex) {
                                    0 -> "🧠 [AreIA SOLUCIÓN DE CONTRATOS INTELIGENTES vmsb]\n" +
                                         "Recomendación: Fideicomiso Autónomo Multipartita (Multi-Sig Safe).\n\n" +
                                         "• Oráculo de AreIA verifica la matrícula estudiantil activa mediante firmas criptográficas ZK-Proofs para resguardar la identidad de los estudiantes.\n" +
                                         "• Al ejecutarse el contrato de lanzamiento de vmsb, se retiene directamente el 20% en un depósito escrow inmutable.\n" +
                                         "• Distribución inmediata sin intermediarios financieros directo a las 3 wallets universitarias registradas, garantizando 0% de costo por tasas bancarias."
                                    1 -> "🧠 [AreIA SOLUCIÓN FISCAL AUTOMATIZADA vmsb]\n" +
                                         "Recomendación: Amortización por Fomento Tecnológico (Gasto I+D).\n\n" +
                                         "• El precio base de mercado de 450 Cr se etiqueta bajo el amparo de la Ley de Innovación Tecnológica del Creador vmsb.\n" +
                                         "• Las costas e impuestos (15%) se absorben en las costas de emisión y minería del bloque en el ledger.\n" +
                                         "• Mediante compensación contable con activos intangibles de investigación aeroespacial, el emisor goza de exención fiscal total legal, consolidando una transacción transparente con 0% de recargo real."
                                    2 -> "🧠 [AreIA PROTOCOLO DE FIRMA INTELECTUAL vmsb]\n" +
                                         "Recomendación: Sello de Copyright Digital Perpetuo e Inmutable.\n\n" +
                                         "• Firma criptográfica SHA-256 inyectada directamente en el bloque génesis del ledger: 'AUTHOR: vmsb©'.\n" +
                                         "• Creación de una licencia de uso abierto de alta tecnología aeroespacial vinculada a la firma digital del creador.\n" +
                                         "• Toda integración o bifurcación subsiguiente comercial de este sistema requiere obligatoriamente heredar el metadato inmutable de autoría, garantizando la visibilidad perpetua del creador original."
                                    else -> ""
                                }
                                areiaLaunchLog = areiaLaunchLog + "🧠 Solución de Inteligencia para el reto #${areiaSelectedQuestionIndex + 1} generada con éxito."
                            }
                        },
                        enabled = !areiaIsHardThinking,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00FFCC),
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF13192B)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(32.dp).testTag("areia_vmsb_hardthink_btn")
                    ) {
                        if (areiaIsHardThinking) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = Color.Black)
                                Text("PROCESANDO HARD-THINK...", fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        } else {
                            Text("🧠 EJECUTAR HARD-THINK IA (SOLUCIONES vmsb)", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Display Answer if present
                    if (areiaHardThinkSolution.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF040712), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF00FFCC).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SOLUCIÓN DE ALTA COMPLEJIDAD GENERADA",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00FFCC),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "vmsb Compliant",
                                    fontSize = 6.sp,
                                    color = Color(0xFF8892B0),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = areiaHardThinkSolution,
                                fontSize = 8.5.sp,
                                color = Color.White,
                                lineHeight = 12.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                }

                Divider(color = Color(0xFF1E293B))

                // Section 3: Smart Ledger Log & Signing Trigger
                Text(
                    text = "PASO 3: CONTROL DE REGISTRO & SELLO CORPORATIVO",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )

                // Log screen for Launch
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF030712), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF111A2E), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "VMSB PROTOCOL REAL-TIME LOGS:",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB74D),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    areiaLaunchLog.takeLast(4).forEach { log ->
                        Text(
                            text = "> $log",
                            fontSize = 8.sp,
                            color = if (log.contains("CONTRATO") || log.contains("EXECUTED")) Color(0xFF00FF88) else if (log.contains("🎁") || log.contains("Solución")) Color(0xFFFFB74D) else Color(0xFF8892B0),
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 11.sp
                        )
                    }
                }

                // Balance warning if players cannot afford
                val canAfford = playerCredits >= totalCostValue.toInt()
                if (!canAfford && !areiaIsLaunchAgreementSigned) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFF1744).copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFFF1744), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️", fontSize = 12.sp)
                        Text(
                            text = "ADVERTENCIA: Balance insuficiente (${playerCredits} Cr). El Modo Autónomo ajustará automáticamente el 'Valor X' si espera un nuevo ciclo de créditos, o configure en Manual.",
                            fontSize = 8.sp,
                            color = Color(0xFFFF8A80),
                            lineHeight = 11.sp
                        )
                    }
                }

                // Execution Button
                Button(
                    onClick = {
                        audioEngine.playChimeSuccess()
                        onAdjustCredits(-totalCostValue.toInt())
                        areiaLaunchSignedHash = calculateSha256("VMSB_LAUNCH|$areiaBasePrice|$areiaRetentionFeeX|$areiaTotalDisbursedToStudents")
                        areiaIsLaunchAgreementSigned = true
                        
                        // Automatically disburse academic contribution to student wallets upon signing, if optimizer is enabled!
                        val chunk = academicContributionAmount / 3f
                        areiaStudentWallets = areiaStudentWallets.map { (name, addr, bal) ->
                            Triple(name, addr, bal + chunk)
                        }
                        areiaTotalDisbursedToStudents += academicContributionAmount

                        areiaLaunchLog = areiaLaunchLog + listOf(
                            "🔑 EXECUTIVE CONTRACT EXECUTED.",
                            "✍️ REGISTRO: vmsb©",
                            "🎁 AUTODISPERSAL ACADÉMICO: ${academicContributionAmount.toInt()} Cr distribuidos a estudiantes.",
                            "📝 CERTIFICADO: Lanzamiento corporativo con retención directa de ${areiaRetentionFeeX.toInt()} Credits.",
                            "🔒 SHA256 HASH COMPILADO: ${areiaLaunchSignedHash.take(32)}"
                        )
                        onAddLog(
                            "AREIA_VMSB_LAUNCH",
                            "🚀 CONTRATO SECTOR PRIVADO: Lanzamiento exitoso bajo el protocolo vmsb. Costo absorbido.",
                            "TACTICAL"
                        )
                    },
                    enabled = !areiaIsLaunchAgreementSigned && canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00FF88),
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF13192B),
                        disabledContentColor = Color(0xFF8892B0)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp).testTag("areia_vmsb_sign_btn")
                ) {
                    Text(
                        text = if (areiaIsLaunchAgreementSigned) "🚀 CONTRATO vmsb EJECUTADO Y EN FUNCIÓN" else "🚀 AUTORIZAR RETENCIÓN & INICIAR LANZAMIENTO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // If Signed, display a magnificent signed corporate barcode ledger stamp
                if (areiaIsLaunchAgreementSigned) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF00FF88).copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF00FF88), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Sello de Gobernanza Digital vmsb del Siglo XXI",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FF88),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "||||| | |||| ||| ||||||| ||| ||| |||| || |||||| |",
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = Color(0xFF00FF88)
                        )
                        Text(
                            text = "REGISTRY COMPLIANT: NASA JPL / BR-SECURE / vmsb-LEDGER",
                            fontSize = 6.sp,
                            color = Color(0xFF8892B0),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "HASH: ${areiaLaunchSignedHash.uppercase()}",
                            fontSize = 7.sp,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // ========================== INTEROPERABLE ACCOUNTING EXPORT ==========================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10172D)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF222C4E))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = LanguageUtils.translate("accounting_export_title", currentLanguage),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Ledger Export",
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = when (currentLanguage) {
                        AppLanguage.ENGLISH -> "Export the unified regional accounting matrix. Integrates seamlessly with international standard ERP formats, protected by ledger-chain validation."
                        AppLanguage.ESPANOL -> "Exporte la matriz unificada de cuentas contables regionales. Integración total con formatos de auditoría internacionales ERP bajo sellado criptográfico."
                        AppLanguage.PORTUGUES -> "Exporte a matriz unificada de contabilidade regional. Integre sem problemas com formatos ERP internacionais, protegidos por validação em cadeia."
                    },
                    fontSize = 9.sp,
                    color = Color(0xFF8892B0),
                    lineHeight = 12.sp
                )

                Divider(color = Color(0xFF1E293B))

                Text(
                    text = LanguageUtils.translate("export_format", currentLanguage).uppercase() + ":",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8892B0),
                    fontFamily = FontFamily.Monospace
                )

                // Selectable formats list
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val formats = listOf(
                        "QUICKBOOKS" to "QuickBooks JSON Interoperability Scheme",
                        "SAP_ERP" to "SAP ERP Ledger Format",
                        "XERO" to "Xero Accounting Standard",
                        "IFRS_9" to "Generic / IFRS-9 GAAP Standard Exchange"
                    )

                    formats.forEach { (code, label) ->
                        val isSelected = selectedExportFormat == code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0x1100FFCC) else Color(0xFF0B1123))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF00FFCC) else Color(0xFF1E293B),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    audioEngine.playKeyTap()
                                    selectedExportFormat = code
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isSelected) "🟢" else "⚫",
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF8892B0)
                                )
                            }
                            Text(
                                text = code,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF00FFCC),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(Color(0xFF030712), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        audioEngine.playKeyTap()
                        showExportConfirmDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D), contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ledger_export_btn")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Ledger Export Icon", modifier = Modifier.size(16.dp))
                        Text("SECURE LEDGER SIGN & EXPORT", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                // Success confirmation display banner
                if (exportSuccessMessage.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x1F00FF88), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF00FF88), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✅", fontSize = 14.sp)
                            Text(
                                text = "LEDGER EXPORT SIGNED SUCCESSFULLY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FF88),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = exportSuccessMessage,
                            fontSize = 8.sp,
                            color = Color.White,
                            lineHeight = 11.sp
                        )
                    }
                }
            }
        }

        // Dialog for Confirmation & Signing
        if (showExportConfirmDialog) {
            val ledgerSeal = remember(selectedExportFormat, playerCredits, playerAlloys) {
                calculateSha256("LEDGER_CHAIN|$selectedExportFormat|$playerCredits|$playerAlloys")
            }
            
            AlertDialog(
                onDismissRequest = {
                    if (!isSigningLedger) {
                        audioEngine.playKeyTap()
                        showExportConfirmDialog = false
                    }
                },
                containerColor = Color(0xFF0F172A),
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔒 CRYPTO SIGNING WIZARD",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = LanguageUtils.translate("export_confirm_title", currentLanguage),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFB74D)
                        )
                        
                        // Confidentiality Protocol Text
                        val disclaimer = when (currentLanguage) {
                            AppLanguage.ENGLISH -> "CONFIDENTIALITY PROTOCOL: This action will compile and cryptographically sign the selected transaction blocks using SHA-256 ledger-chaining. This output is unalterable and fully formatted for ingestion into external ERP engines."
                            AppLanguage.ESPANOL -> "PROTOCOLO DE CONFIDENCIALIDAD: Esta acción compilará y firmará criptográficamente los bloques de transacción seleccionados usando encadenamiento SHA-256. Esta salida es inalterable y está formateada para su importación directa en sistemas ERP externos."
                            AppLanguage.PORTUGUES -> "PROTOCOLO DE CONFIDENCIALIDADE: Esta ação irá compilar e assinar criptograficamente os blocos de transação usando encadeamento SHA-256. Esta ação é inalterável e formatada para ingestão em motores ERP externos."
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x33FF3366), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFFFF3366), RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("🚨", fontSize = 14.sp)
                                Text(
                                    text = disclaimer,
                                    fontSize = 9.sp,
                                    color = Color(0xFFFFCCD5),
                                    lineHeight = 12.sp
                                )
                            }
                        }

                        // Audit Trail Information List
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF030712), RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("LEDGER INCLUSIONS BATCH:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                            listOf(
                                "• System Node Operation Log Ledger Blocks" to "COMPRESSED",
                                "• Region Conquest & Defense Capital Ledger" to "COMPILED",
                                "• SciTech Research Alloys Amortization Record" to "RESOLVED",
                                "• Active Multi-Grid Session Handshakes" to "INTEGRATED"
                            ).forEach { (desc, status) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(desc, fontSize = 8.sp, color = Color(0xFF8892B0))
                                    Text(status, fontSize = 8.sp, color = Color(0xFF00FF88), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        // Deterministic SEAL visualization
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0x6600FFCC), RoundedCornerShape(6.dp))
                                .background(Color(0xFF0A1021))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "DETERMINISTIC SHA-256 SEAL CHAIN:",
                                fontSize = 7.sp,
                                color = Color(0xFF00FFCC),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = ledgerSeal,
                                fontSize = 8.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 11.sp
                            )
                        }

                        if (isSigningLedger) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF00FFCC), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("COMPUTING DETERMINISTIC CRYPTO SEAL...", fontSize = 9.sp, color = Color(0xFF00FFCC), fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            audioEngine.playKeyTap()
                            isSigningLedger = true
                            scope.launch {
                                delay(1200)
                                isSigningLedger = false
                                showExportConfirmDialog = false
                                audioEngine.playChimeSuccess()
                                generatedLedgerSeal = ledgerSeal
                                exportSuccessMessage = LanguageUtils.translate("export_success", currentLanguage) + ledgerSeal
                                onAddLog(
                                    "AUDIT_CORE",
                                    "Ledger exported successfully in format [$selectedExportFormat] with cryptographic Seal: ${ledgerSeal.take(24)}...",
                                    "SYSTEM"
                                )
                            }
                        },
                        enabled = !isSigningLedger,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("confirm_sign_btn")
                    ) {
                        Text("CONFIRM & CRYPTO SIGN", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            audioEngine.playKeyTap()
                            showExportConfirmDialog = false
                        },
                        enabled = !isSigningLedger
                    ) {
                        Text("CANCEL", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            )
        }

        // ========================== BRAZIL TAX COMPLIANCE & e-CAC BOT ==========================
        Card(
            modifier = Modifier.fillMaxWidth().testTag("br_tax_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1B2A)), // Deep navy with subtle green/blue borders
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1B4D3E)) // Forest green border representing Brasil
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageUtils.translate("br_tax_title", currentLanguage),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF00C853), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "BACEN REGULATED",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Tax Compliance Logo",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = LanguageUtils.translate("br_tax_desc", currentLanguage),
                    fontSize = 9.sp,
                    color = Color(0xFF8892B0),
                    lineHeight = 12.sp
                )

                Divider(color = Color(0xFF1B4D3E))

                // Connection state to Nubank via Google account
                if (!isNubankConnected) {
                    Button(
                        onClick = {
                            audioEngine.playKeyTap()
                            isNubankConnected = true
                            onAddLog("NUBANK_BOT", "Connected safely to Google-associated Nubank account under user email [$nubankUserEmail].", "SYSTEM")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8C34FF)), // Nubank Purple
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("br_connect_nubank_btn")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("👤", fontSize = 14.sp)
                            Text(
                                text = LanguageUtils.translate("br_nu_connect_btn", currentLanguage).uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // Connected Status Display Block
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF05111E), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF8C34FF), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🟣", fontSize = 12.sp)
                                Text("NUBANK GOOGLE-LINKED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBA86FF), fontFamily = FontFamily.Monospace)
                            }
                            Text(
                                text = "ACTIVE",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FF88),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .background(Color(0x2200FF88), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Connected Account details (CPF, Nu Acc)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Account Holder:", fontSize = 8.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                Text(if (isDetailsVisible) nubankUserName else "V***** B******", fontSize = 8.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("User Account / Email:", fontSize = 8.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                Text(if (isDetailsVisible) nubankUserEmail else "v***b***@gmail.com", fontSize = 8.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("CPF (ZKP Masked):", fontSize = 8.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(getMaskedCpf(nubankUserCpf, isDetailsVisible), fontSize = 8.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                    Text(
                                        text = if (isDetailsVisible) "[ HIDE ]" else "[ SHOW ]",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDetailsVisible) Color(0xFFFF3366) else Color(0xFF00FF88),
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier
                                            .clickable {
                                                audioEngine.playKeyTap()
                                                isDetailsVisible = !isDetailsVisible
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Nubank Branch & Account:", fontSize = 8.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                Text(nubankAccountNum, fontSize = 8.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Divider(color = Color(0xFF1B4D3E), thickness = 0.5.dp)

                        // Real-time tax matrix (Brasil 30% Tax calculations)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text("TAXABLE REVENUE (BRL)", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                Text("R$ ${String.format("%,.2f", taxableRevenueBrl)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FFCC))
                            }
                            Column(modifier = Modifier.weight(1.5f), horizontalAlignment = Alignment.End) {
                                Text("BRASIL RATE (30%)", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                Text("R$ ${String.format("%,.2f", calculatedTaxBrl)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD600))
                            }
                        }

                        // Change details link button
                        Text(
                            text = "Disconnect account safely",
                            fontSize = 8.sp,
                            color = Color(0xFFFF3366),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clickable {
                                    audioEngine.playKeyTap()
                                    isNubankConnected = false
                                    zkpProofGenerated = false
                                    transmissionSuccess = false
                                    onAddLog("NUBANK_BOT", "Disconnected Google-associated Nubank account.", "SYSTEM")
                                }
                                .padding(vertical = 2.dp)
                        )
                    }
                }

                // ================== ZKP VERIFICATION PORTAL ==================
                if (isNubankConnected) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF05111E), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF1B4D3E), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔒 ZERO-KNOWLEDGE PROOF COMPLIANCE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "ZKP Lock",
                                tint = if (zkpProofGenerated) Color(0xFF00FF88) else Color(0xFF8892B0),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Text(
                            text = "ZKP enables verification that the exact 30% Brasil tax has been computed correctly over your assets, without transmitting raw Nubank ledger data or personal transaction history.",
                            fontSize = 8.sp,
                            color = Color(0xFF8892B0),
                            lineHeight = 11.sp
                        )

                        if (!zkpProofGenerated) {
                            Button(
                                onClick = {
                                    audioEngine.playKeyTap()
                                    isZkpRunning = true
                                    zkpVerificationLogs = listOf("Starting ZKP cryptographic compiler pipeline...")
                                    scope.launch {
                                        delay(500)
                                        zkpVerificationLogs = zkpVerificationLogs + "Isolating transaction inputs..."
                                        delay(500)
                                        zkpVerificationLogs = zkpVerificationLogs + "Generating arithmetic circuit representation: calculatedTax == taxableRevenue * 0.30..."
                                        delay(600)
                                        zkpVerificationLogs = zkpVerificationLogs + "Selecting elliptic curve BN254. Creating witness..."
                                        delay(500)
                                        zkpVerificationLogs = zkpVerificationLogs + "Computing proving key polynomial commits..."
                                        delay(600)
                                        val inputString = "$taxableRevenueBrl|$calculatedTaxBrl|0.30"
                                        zkpProofHash = calculateSha256(inputString)
                                        zkpVerificationLogs = zkpVerificationLogs + "ZKP PROOF GENERATED: proof_snark_0x" + zkpProofHash.take(24)
                                        zkpVerificationLogs = zkpVerificationLogs + "Verifying challenge commitment phase... OK"
                                        isZkpRunning = false
                                        zkpProofGenerated = true
                                        audioEngine.playChimeSuccess()
                                        onAddLog("ZKP_BOT", "Generated deterministic Zero-Knowledge proof of tax computation. Proof Hash: ${zkpProofHash.take(16)}...", "SYSTEM")
                                    }
                                },
                                enabled = !isZkpRunning,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC), contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("br_zkp_calc_btn")
                            ) {
                                if (isZkpRunning) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("COMPUTING ZKP CRYPTO COMMITS...", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                } else {
                                    Text(
                                        text = LanguageUtils.translate("br_zkp_audit_btn", currentLanguage).uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        } else {
                            // ZKP success display
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x1100FF88), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF00FF88), RoundedCornerShape(6.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                              ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("✅", fontSize = 12.sp)
                                    Text("ZKP VALID & AUDITED COMPLIANT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FF88), fontFamily = FontFamily.Monospace)
                                }
                                Text(
                                    text = "Deterministic Proof Hash:\n0x$zkpProofHash",
                                    fontSize = 8.sp,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 11.sp
                                )
                            }
                        }

                        // Verification execution trace
                        if (zkpVerificationLogs.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF030712), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                zkpVerificationLogs.forEach { log ->
                                    Text(
                                        text = "> $log",
                                        fontSize = 8.sp,
                                        color = if (log.contains("PROVED") || log.contains("GENERATED") || log.contains("OK")) Color(0xFF00FF88) else Color(0xFF8892B0),
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // ================== DARF PREVIEW AND TRANSMISSION ==================
                if (zkpProofGenerated) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // DARF View Button
                        Button(
                            onClick = {
                                audioEngine.playKeyTap()
                                darfModalOpen = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853), contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("br_view_darf_btn")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📄", fontSize = 14.sp)
                                Text(
                                    text = "VIEW & AUDIT DARF (IN ORDEN)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Direct Transmission Panel
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF05111E), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF1B4D3E), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "🛰️ TRANSMISSION TO RECEITA FEDERAL & NUBANK",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )

                            if (!transmissionSuccess) {
                                Button(
                                    onClick = {
                                        audioEngine.playKeyTap()
                                        isTransmittingTaxes = true
                                        transmissionLogs = listOf("Generating secure PDF compliance document...")
                                        scope.launch {
                                            delay(500)
                                            transmissionLogs = transmissionLogs + "Signing document with cryptographically authenticated witness..."
                                            delay(600)
                                            transmissionLogs = transmissionLogs + "Establishing TLS 1.3 socket to Receita Federal e-CAC endpoint..."
                                            delay(500)
                                            transmissionLogs = transmissionLogs + "Transmitting audit PDF (ZKP attached) to Receita Federal... OK!"
                                            delay(600)
                                            transmissionLogs = transmissionLogs + "Transmitting compliance ledger block to Nubank / BACEN gateway... OK!"
                                            delay(500)
                                            transmissionLogs = transmissionLogs + "TRANSMISSION COMPLETED SUCCESSFULLY."
                                            isTransmittingTaxes = false
                                            transmissionSuccess = true
                                            audioEngine.playChimeSuccess()
                                            onAddLog(
                                                "TAX_BOT",
                                                "Brasil 30% tax compliance PDF successfully generated, audited via ZKP, and transmitted directly to e-CAC (Receita Federal) and Nubank.",
                                                "SYSTEM"
                                            )
                                        }
                                    },
                                    enabled = !isTransmittingTaxes,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600), contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("br_transmit_pdf_btn")
                                ) {
                                    if (isTransmittingTaxes) {
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("UPLOADING COMPLIANCE PDF...", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                    } else {
                                        Text(
                                            text = LanguageUtils.translate("br_pdf_submit_btn", currentLanguage).uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            } else {
                                // Compliance Success banner
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x2200E676), RoundedCornerShape(6.dp))
                                        .border(1.dp, Color(0xFF00E676), RoundedCornerShape(6.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🇧🇷", fontSize = 14.sp)
                                        Text("TAXES SUBMITTED & DARF IN ORDER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676), fontFamily = FontFamily.Monospace)
                                    }
                                    Text(
                                        text = "The Receita Federal e-CAC system has successfully logged the ZKP audit PDF. Transaction record matches Nubank ledger blocks exactly.",
                                        fontSize = 8.sp,
                                        color = Color.White,
                                        lineHeight = 11.sp
                                    )
                                }
                            }

                            // Submission Console Logs
                            if (transmissionLogs.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF030712), RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                ) {
                                    transmissionLogs.forEach { log ->
                                        Text(
                                            text = "> $log",
                                            fontSize = 8.sp,
                                            color = if (log.contains("SUCCESSFULLY") || log.contains("OK")) Color(0xFF00FF88) else Color(0xFF8892B0),
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- DARF PREVIEW MODAL DIALOG ---
        if (darfModalOpen) {
            AlertDialog(
                onDismissRequest = {
                    audioEngine.playKeyTap()
                    darfModalOpen = false
                },
                containerColor = Color(0xFF1E293B), // Dark slate suitable for official looking document on dark theme
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageUtils.translate("br_darf_title", currentLanguage),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        IconButton(onClick = {
                            audioEngine.playKeyTap()
                            darfModalOpen = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .background(Color.White, RoundedCornerShape(4.dp))
                            .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        // Header section
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.Black)
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1.8f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "MINISTÉRIO DA FAZENDA",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "SECRETARIA DA RECEITA FEDERAL DO BRASIL",
                                    fontSize = 6.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Documento de Arrecadação de Receitas Federais",
                                    fontSize = 7.sp,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "DARF",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(55.dp)
                                    .background(Color.Black)
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .padding(start = 4.dp)
                            ) {
                                Text("02 PERÍODO DE APURAÇÃO", fontSize = 5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text(darfPeriodApuration, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Divider(color = Color.Black, thickness = 0.5.dp)
                                Text("03 NÚMERO DO CPF/CNPJ", fontSize = 5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text(getMaskedCpf(nubankUserCpf, isDetailsVisible), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        // Grid of fields mimicking real DARF
                        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                            Column(
                                modifier = Modifier
                                    .weight(1.8f)
                                    .border(1.dp, Color.Black)
                                    .fillMaxHeight()
                                    .padding(4.dp)
                            ) {
                                Text("01 NOME / TELEFONE", fontSize = 5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text(if (isDetailsVisible) "VICTOR BARRETO SECURE TERMINAL" else "V***** B****** SECURE TERMINAL", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text("NU ASSOCIATED EMAIL: ${if (isDetailsVisible) nubankUserEmail else "v***b***@gmail.com"}", fontSize = 6.sp, color = Color.DarkGray)
                                Text("ZKP VERIFIED TRANS-ID: ${zkpProofHash.take(16)}", fontSize = 6.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .border(1.dp, Color.Black)
                                    .fillMaxHeight()
                            ) {
                                Column(modifier = Modifier.padding(4.dp)) {
                                    Text("04 CÓDIGO DA RECEITA", fontSize = 5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Text(darfPeriodCode, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                                Divider(color = Color.Black, thickness = 0.5.dp)
                                Column(modifier = Modifier.padding(4.dp)) {
                                    Text("05 NÚMERO DE REFERÊNCIA", fontSize = 5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Text("858200000019", fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                            Column(
                                modifier = Modifier
                                    .weight(1.8f)
                                    .border(1.dp, Color.Black)
                                    .fillMaxHeight()
                                    .padding(4.dp)
                            ) {
                                Text("OBSERVAÇÕES", fontSize = 5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text("Tributação Brasil 30% sobre operações simuladas regionais.", fontSize = 6.sp, color = Color.Black)
                                Text("Auditado e selado criptograficamente com prova de conhecimento zero.", fontSize = 6.sp, color = Color.Black)
                                Text("Sem exposição de saldo real ou dados do portador de conta.", fontSize = 6.sp, color = Color.Black)
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .border(1.dp, Color.Black)
                                    .fillMaxHeight()
                            ) {
                                Column(modifier = Modifier.padding(4.dp)) {
                                    Text("06 DATA DE VENCIMENTO", fontSize = 5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Text(darfVencimento, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                                Divider(color = Color.Black, thickness = 0.5.dp)
                                Column(modifier = Modifier.padding(4.dp)) {
                                    Text("07 VALOR DO PRINCIPAL", fontSize = 5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Text("R$ ${String.format("%,.2f", calculatedTaxBrl)}", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                            Column(
                                modifier = Modifier
                                    .weight(1.8f)
                                    .border(1.dp, Color.Black)
                                    .fillMaxHeight()
                                    .padding(4.dp)
                            ) {
                                Text("ASSINATURA DO CONTRIBUINTE", fontSize = 5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Digital Signature Link: Google Accounts OAuth", fontSize = 6.sp, color = Color.DarkGray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .border(1.dp, Color.Black)
                                    .fillMaxHeight()
                            ) {
                                Column(modifier = Modifier.padding(4.dp)) {
                                    Text("08 VALOR DA MULTA", fontSize = 5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Text("R$ 0,00", fontSize = 8.sp, color = Color.Black)
                                }
                                Divider(color = Color.Black, thickness = 0.5.dp)
                                Column(modifier = Modifier.padding(4.dp)) {
                                    Text("09 VALOR DOS JUROS", fontSize = 5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Text("R$ 0,00", fontSize = 8.sp, color = Color.Black)
                                }
                                Divider(color = Color.Black, thickness = 0.5.dp)
                                Column(modifier = Modifier.padding(4.dp)) {
                                    Text("10 VALOR TOTAL", fontSize = 5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Text("R$ ${String.format("%,.2f", calculatedTaxBrl)}", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }

                        // Barcode visual representation
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.Black)
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .background(Color.White),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                val bars = listOf(1, 3, 2, 1, 4, 2, 3, 1, 2, 4, 1, 3, 2, 1, 4, 2, 3, 1, 2, 4, 1, 3, 2, 1, 4)
                                bars.forEach { w ->
                                    Spacer(
                                        modifier = Modifier
                                            .width(w.dp)
                                            .fillMaxHeight()
                                            .background(Color.Black)
                                    )
                                    Spacer(modifier = Modifier.width(1.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = darfReference,
                                fontSize = 7.sp,
                                color = Color.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            audioEngine.playKeyTap()
                            darfModalOpen = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC), contentColor = Color.Black)
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            )
        }

        // ========================== GLOBAL SERVICE CAPACITY, QUALITY & EXCLUSIVITY ENCLAVE ==========================
        Card(
            modifier = Modifier.fillMaxWidth().testTag("global_services_enclave_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B35)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF00FFCC))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.ENGLISH) "🌐 GLOBAL SERVICE MATRIX & EXCLUSIVITY ENCLAVE"
                               else if (currentLanguage == AppLanguage.ESPANOL) "🌐 MATRIZ GLOBAL DE SERVICIOS Y EXCLUSIVIDAD"
                               else "🌐 MATRIZ DE SERVIÇOS GLOBAIS E EXCLUSIVIDADE",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Box(
                        modifier = Modifier
                            .background(Color(0x3300FFCC), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.ENGLISH) "EXCLUSIVE"
                                   else if (currentLanguage == AppLanguage.ESPANOL) "EXCLUSIVO"
                                   else "EXCLUSIVO",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FFCC),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Text(
                    text = if (currentLanguage == AppLanguage.ENGLISH) "Analysis of global infrastructure capabilities, limits, and quality levels to ensure real authenticity and unmatched service levels."
                           else if (currentLanguage == AppLanguage.ESPANOL) "Análisis de capacidades, límites y niveles de calidad de la infraestructura global para garantizar la autenticidad real y un nivel de servicio sin igual."
                           else "Análise de capacidades, limites e níveis de qualidade de infraestrutura global para garantir autenticidade real e nível de serviço inigualável.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8892B0)
                )

                // List of Areas
                val areas = listOf(
                    Triple(
                        if (currentLanguage == AppLanguage.ENGLISH) "South America (BRASIL Enclave)" else if (currentLanguage == AppLanguage.ESPANOL) "Sudamérica (Enclave BRASIL)" else "América do Sul (Enclave BRASIL)",
                        "BACEN Gateway & e-CAC automatic compliance bot with Zero-Knowledge masking.",
                        mapOf(
                            "SLA" to "99.999% Operational Guarantee",
                            "Capacity Limit" to "50,000 tx/day (Exclusivity Cap)",
                            "Current Load" to "14,820 tx processed today",
                            "Quality Level" to "High-Fidelity Millisecond Processing",
                            "Authenticity Seal" to "ZKP-GATEWAY-256",
                            "Exclusivity Slot" to "Tier 1 Nubank Gold Interlock Only"
                        )
                    ),
                    Triple(
                        if (currentLanguage == AppLanguage.ENGLISH) "North America (Dallas)" else if (currentLanguage == AppLanguage.ESPANOL) "Norteamérica (Dallas)" else "América do Norte (Dallas)",
                        "AI Cockpit automated ledger controller and strategic resource allocator.",
                        mapOf(
                            "SLA" to "99.995% Network Availability",
                            "Capacity Limit" to "350 Parallel Staking Pipes",
                            "Current Load" to "218 pipelines active",
                            "Quality Level" to "3.8 GHz Overclock Multi-Thread",
                            "Authenticity Seal" to "SOC-3 QUANTUM CERT",
                            "Exclusivity Slot" to "Private Enclave Routing"
                        )
                    ),
                    Triple(
                        if (currentLanguage == AppLanguage.ENGLISH) "Europe (Frankfurt Enclave)" else if (currentLanguage == AppLanguage.ESPANOL) "Europa (Frankfurt Enclave)" else "Europa (Enclave de Frankfurt)",
                        "Quantum Grid Ledger decentralised storage with active-active redundant mirroring.",
                        mapOf(
                            "SLA" to "100% Redundancy Guarantee",
                            "Capacity Limit" to "500 Megatransactions/sec",
                            "Current Load" to "92M tx/sec background load",
                            "Quality Level" to "4.2 GHz Dynamic Auto-scaling",
                            "Authenticity Seal" to "ISO-27001-MATH FORMAL",
                            "Exclusivity Slot" to "Zero-Knowledge Routing Only"
                        )
                    ),
                    Triple(
                        if (currentLanguage == AppLanguage.ENGLISH) "Asia-Pacific (Tokyo Enclave)" else if (currentLanguage == AppLanguage.ESPANOL) "Asia-Pacífico (Tokyo Enclave)" else "Ásia-Pacífico (Enclave de Tóquio)",
                        "Decentralised alloy mining and smart node automated dispatching network.",
                        mapOf(
                            "SLA" to "99.990% Automated Recovery",
                            "Capacity Limit" to "250 Dispatch Pipelines",
                            "Current Load" to "104 pipelines online",
                            "Quality Level" to "4.5 GHz Peak Speed",
                            "Authenticity Seal" to "AUTONOMOUS-GEO SIGNATURE",
                            "Exclusivity Slot" to "Isolated IP Tunneling"
                        )
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    areas.forEach { (areaName, description, metrics) ->
                        val isExpanded = expandedAreaDetail == areaName
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    audioEngine.playKeyTap()
                                    expandedAreaDetail = if (isExpanded) null else areaName
                                },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF10172D)),
                            border = BorderStroke(1.dp, if (isExpanded) Color(0xFF00FFCC) else Color(0xFF222C4E))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(0xFF00FFCC), RoundedCornerShape(50.dp))
                                        )
                                        Text(
                                            text = areaName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        text = if (isExpanded) "[ COLLAPSE ]" else "[ DETAIL ]",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExpanded) Color(0xFFFF3366) else Color(0xFF00FFCC),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Text(text = description, fontSize = 9.sp, color = Color(0xFF8892B0))

                                if (isExpanded) {
                                    Divider(color = Color(0xFF222C4E), modifier = Modifier.padding(vertical = 4.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        metrics.forEach { (k, v) ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = "$k:", fontSize = 8.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                                Text(
                                                    text = v,
                                                    fontSize = 8.sp,
                                                    color = if (k.contains("Seal") || k.contains("SLA") || k.contains("Exclusivity")) Color(0xFF00FFCC) else Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Interactive Audit Panel
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1123)),
                    border = BorderStroke(1.dp, Color(0xFF222C4E))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.ENGLISH) "🛡️ REAL-TIME EXCLUSIVITY & CAPACITY AUDIT"
                                       else if (currentLanguage == AppLanguage.ESPANOL) "🛡️ AUDITORÍA DE EXCLUSIVIDAD Y CAPACIDAD"
                                       else "🛡️ AUDITORIA DE EXCLUSIVIDADE E CAPACIDADE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            
                            Button(
                                onClick = {
                                    audioEngine.playKeyTap()
                                    isGlobalAuditRunning = true
                                    val randomHash = java.util.UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
                                    val currentTimestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
                                    globalAuditLogs = listOf(
                                        "Initializing Global Capacity Check...",
                                        "Connecting to Brazil Enclave BACEN Hub... Verified ZKP SLA (99.999%)",
                                        "Scanning North America node (Dallas)... Status: ACTIVE [3.8 GHz]",
                                        "Validating Europe Mirroring Enclave (Frankfurt)... 100% Sync",
                                        "Verifying Asia-Pacific Node (Tokyo)... Speed check: 4.5 GHz OK",
                                        "GENUINE PROOF OF AUTHENTICITY GENERATED: SHA-$randomHash",
                                        "Audit completed successfully at $currentTimestamp. Capacity status is optimal and fully exclusive."
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isGlobalAuditRunning) Color(0xFF10172D) else Color(0xFF00FFCC),
                                    contentColor = if (isGlobalAuditRunning) Color(0xFF8892B0) else Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text(
                                    text = if (isGlobalAuditRunning) "RE-AUDIT" else "RUN AUDIT",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF050915))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            globalAuditLogs.forEach { log ->
                                Text(
                                    text = "> $log",
                                    fontSize = 7.5.sp,
                                    color = if (log.contains("GENUINE") || log.contains("completed")) Color(0xFF00FFCC) else Color(0xFF8892B0),
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }


        // ========================== RECHARTS EMULATION: NORTH-SOUTH ECONOMIC MODEL & DYNAMIC LIQUIDITY DASHBOARD ==========================
        Card(
            modifier = Modifier.fillMaxWidth().testTag("north_south_recharts_dashboard_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1424)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, Color(0xFF00FFCC))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (currentLanguage == AppLanguage.ENGLISH) "📊 NORTH-SOUTH ECONOMIC MODEL"
                                   else if (currentLanguage == AppLanguage.ESPANOL) "📊 MODELO ECONÓMICO NORTE-SUR"
                                   else "📊 MODELO ECONÔMICO NORTE-SUL",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (currentLanguage == AppLanguage.ENGLISH) "Real-time liquidity flow & dynamic license valuation (Recharts Engine)"
                                   else if (currentLanguage == AppLanguage.ESPANOL) "Flujo de liquidez y valuación dinámica de licencias en tiempo real"
                                   else "Fluxo de liquidez e valoração dinâmica de licenças em tempo real",
                            fontSize = 8.5.sp,
                            color = Color(0xFF8892B0)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0x3300FFCC), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LIVE GRAPH",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FFCC),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Selector Tabs for Recharts Engine view mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("LIQUIDITY", "VALUATION").forEach { tab ->
                        val isSelected = selectedDashboardTab == tab
                        val tabLabel = when (tab) {
                            "LIQUIDITY" -> if (currentLanguage == AppLanguage.ENGLISH) "🌐 Liquidity Flow" else if (currentLanguage == AppLanguage.ESPANOL) "🌐 Flujo de Liquidez" else "🌐 Fluxo de Liquidez"
                            else -> if (currentLanguage == AppLanguage.ENGLISH) "🎫 Exclusivity Valuation" else if (currentLanguage == AppLanguage.ESPANOL) "🎫 Valuación Exclusiva" else "🎫 Valoração Exclusiva"
                        }

                        Button(
                            onClick = {
                                audioEngine.playKeyTap()
                                selectedDashboardTab = tab
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0x3300FFCC) else Color(0xFF131B30),
                                contentColor = if (isSelected) Color(0xFF00FFCC) else Color(0xFF8892B0)
                            ),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF00FFCC) else Color(0xFF222C4E)),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f).height(28.dp)
                        ) {
                            Text(text = tabLabel, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Dynamic data array based on timeline points (T1 to T8)
                // Calculated to show responsive curves that bend as the user adjusts the sliders
                val pointsCount = 8
                val baseLiquidityValues = listOf(1200f, 1800f, 1500f, 2700f, 3100f, 2400f, 3800f, 4500f)
                val baseValuationValues = listOf(3500f, 4200f, 3900f, 5800f, 6200f, 5100f, 7500f, 9200f)

                val timelineLiquidity = baseLiquidityValues.mapIndexed { index, base ->
                    base * (globalNorthIndex / 85f) * (1.15f - (globalSouthIndex / 180f)) + (index * 150f)
                }

                val timelineValuation = baseValuationValues.mapIndexed { index, base ->
                    base * (globalNorthIndex / 85f) * (1.25f - (globalSouthIndex / 150f)) * (1f + (index * 0.08f))
                }

                val activeLiquidity = timelineLiquidity[selectedTimelinePoint]
                val activeValuation = timelineValuation[selectedTimelinePoint]
                // 30% Local Brazil Tax
                val activeBrazilTax = activeValuation * 0.30f
                // Direct education contribution
                val activeEducationAllocation = activeBrazilTax * (educationTaxAllocationPercent / 100f)

                // THE RECHARTS EMULATOR AREA CHART CANVAS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF070B16), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF1B233F), RoundedCornerShape(8.dp))
                    ) {
                        val width = size.width
                        val height = size.height
                        val paddingL = 40f
                        val paddingR = 20f
                        val paddingT = 15f
                        val paddingB = 25f

                        val chartW = width - paddingL - paddingR
                        val chartH = height - paddingT - paddingB

                        // 1. Draw Axis & Grid lines
                        val yLines = 4
                        for (i in 0..yLines) {
                            val ratio = i.toFloat() / yLines
                            val y = paddingT + chartH * (1f - ratio)
                            drawLine(
                                color = Color(0x118892B0),
                                start = androidx.compose.ui.geometry.Offset(paddingL, y),
                                end = androidx.compose.ui.geometry.Offset(width - paddingR, y),
                                strokeWidth = 1f
                            )
                        }

                        val xStep = chartW / (pointsCount - 1)
                        for (i in 0 until pointsCount) {
                            val x = paddingL + i * xStep
                            drawLine(
                                color = Color(0x0A8892B0),
                                start = androidx.compose.ui.geometry.Offset(x, paddingT),
                                end = androidx.compose.ui.geometry.Offset(x, height - paddingB),
                                strokeWidth = 1f
                            )
                        }

                        // 2. Plot Curves and Gradient Fills
                        val points = if (selectedDashboardTab == "LIQUIDITY") timelineLiquidity else timelineValuation
                        val maxVal = points.maxOrNull() ?: 10000f
                        val minVal = 0f
                        val valRange = maxVal - minVal

                        val path = Path()
                        val areaPath = Path()

                        points.forEachIndexed { index, value ->
                            val x = paddingL + index * xStep
                            val yRatio = (value - minVal) / valRange
                            val y = paddingT + chartH * (1f - yRatio)

                            if (index == 0) {
                                path.moveTo(x, y)
                                areaPath.moveTo(x, height - paddingB)
                                areaPath.lineTo(x, y)
                            } else {
                                path.lineTo(x, y)
                                areaPath.lineTo(x, y)
                            }
                            if (index == pointsCount - 1) {
                                areaPath.lineTo(x, height - paddingB)
                                areaPath.close()
                            }
                        }

                        // Draw Gradient Area Fill (Recharts styling)
                        val mainColor = if (selectedDashboardTab == "LIQUIDITY") Color(0xFFFFD700) else Color(0xFF00FFCC)
                        val areaBrush = Brush.verticalGradient(
                            colors = listOf(mainColor.copy(alpha = 0.25f), mainColor.copy(alpha = 0.0f)),
                            startY = paddingT,
                            endY = height - paddingB
                        )
                        drawPath(path = areaPath, brush = areaBrush)

                        // Draw Main Curve Line
                        drawPath(
                            path = path,
                            color = mainColor,
                            style = Stroke(width = 4f)
                        )

                        // 3. Draw Nodes (interactive dots)
                        points.forEachIndexed { index, value ->
                            val x = paddingL + index * xStep
                            val yRatio = (value - minVal) / valRange
                            val y = paddingT + chartH * (1f - yRatio)

                            val isSelectedPoint = index == selectedTimelinePoint
                            drawCircle(
                                color = if (isSelectedPoint) Color.White else mainColor,
                                radius = if (isSelectedPoint) 6.dp.toPx() else 3.5.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(x, y)
                            )
                            if (isSelectedPoint) {
                                drawCircle(
                                    color = mainColor.copy(alpha = 0.4f),
                                    radius = 11.dp.toPx(),
                                    center = androidx.compose.ui.geometry.Offset(x, y)
                                )
                            }
                        }
                    }
                }

                // Interactive timeline controller (T1 to T8 buttons)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.ENGLISH) "Timeline Epoch:" else "Época del Modelo:",
                        fontSize = 8.5.sp,
                        color = Color(0xFF8892B0),
                        fontFamily = FontFamily.Monospace
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in 0 until pointsCount) {
                            val isSelected = i == selectedTimelinePoint
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) Color(0xFF00FFCC) else Color(0xFF131B30))
                                    .clickable {
                                        audioEngine.playKeyTap()
                                        selectedTimelinePoint = i
                                    }
                                    .border(1.dp, if (isSelected) Color.White else Color(0xFF222C4E), RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "T${i + 1}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Dynamic Tooltip Display for the Selected Epoch
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF070B16)),
                    border = BorderStroke(1.dp, Color(0xFF1B233F))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "📍 EPOCH T${selectedTimelinePoint + 1} LIVE AUDIT DETAILS",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FFCC),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "ZKP-PROOF-OK",
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FF88),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Divider(color = Color(0xFF1B233F), modifier = Modifier.padding(vertical = 2.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Hemisphere Liquidity", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                Text("$${String.format("%.1f", activeLiquidity)} USD", fontSize = 9.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Column {
                                Text("Market Valuation", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                Text("$${String.format("%.1f", activeValuation)} USD", fontSize = 9.sp, color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Column {
                                Text("Local Brazil Tax (30%)", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                Text("$${String.format("%.1f", activeBrazilTax)} USD", fontSize = 9.sp, color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Column {
                                Text("Direct Education Fund", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                Text("$${String.format("%.1f", activeEducationAllocation)} USD", fontSize = 9.sp, color = Color(0xFF00FF88), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                // Economic model controls: Hemispheric sliders
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (currentLanguage == AppLanguage.ENGLISH) "🌐 HEMISPHERIC ECONOMIC INDICATORS CONTROL"
                               else "🌐 CONTROL DE INDICADORES ECONÓMICOS HEMISFÉRICOS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FFCC),
                        fontFamily = FontFamily.Monospace
                    )

                    // North Slider (Tech capital level)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🇺🇸 North Tech Capital Index (Scalability): ${globalNorthIndex.toInt()}%",
                                fontSize = 8.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (globalNorthIndex >= 110f) "🔥 EXPANSION" else "STABLE",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (globalNorthIndex >= 110f) Color(0xFF00FFCC) else Color(0xFF8892B0),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = globalNorthIndex,
                            onValueChange = { globalNorthIndex = it },
                            valueRange = 40f..150f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00FFCC),
                                activeTrackColor = Color(0xFF00FFCC),
                                inactiveTrackColor = Color(0xFF1B233F)
                            )
                        )
                    }

                    // South Slider (Resource and education output level)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🇧🇷 South Domestic Output Index (Resilience): ${globalSouthIndex.toInt()}%",
                                fontSize = 8.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (globalSouthIndex >= 100f) "📈 COMPETITIVE" else "DEVELOPING",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (globalSouthIndex >= 100f) Color(0xFF00FF88) else Color(0xFF8892B0),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = globalSouthIndex,
                            onValueChange = { globalSouthIndex = it },
                            valueRange = 40f..150f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00FF88),
                                activeTrackColor = Color(0xFF00FF88),
                                inactiveTrackColor = Color(0xFF1B233F)
                            )
                        )
                    }
                }

                // Sincere economic model explanation matching the user's intent
                Text(
                    text = if (currentLanguage == AppLanguage.ENGLISH) {
                        "💡 North-South Interconnect: Dynamic pricing adjustments prove that high-fidelity enclaves scale automatically with market conditions. Instead of rigid amortization, the game maintains high liquid differentials, securing educational allocations directly from Day 1."
                    } else if (currentLanguage == AppLanguage.ESPANOL) {
                        "💡 Interconexión Norte-Sur: Los ajustes de precios dinámicos demuestran que las licencias se escalan de acuerdo al mercado. En lugar de amortizaciones rígidas, el sistema mantiene diferenciales líquidos altos y asegura aportes educativos directos desde el Día 1."
                    } else {
                        "💡 Interconexão Norte-Sul: Ajustes de preços dinâmicos demonstram que as licenças se escalam conforme o mercado. Em vez de amortizações rígidas, o sistema mantém diferenciais líquidos altos e assegura aportes educativos diretos desde o Dia 1."
                    },
                    fontSize = 8.sp,
                    color = Color(0xFF8892B0),
                    lineHeight = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        
        // ========================== EDUCATIONAL ENCLAVE & CRISIS MANAGEMENT CENTRE ==========================
        val allocatedEducationFunds = brazilTax30 * (educationTaxAllocationPercent / 100f)
        val netBrazilTaxRemaining = brazilTax30 - allocatedEducationFunds

        Card(
            modifier = Modifier.fillMaxWidth().testTag("educational_enclave_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A30)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, Color(0xFF00FF88))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.ENGLISH) "🎓 SOCIAL EDUCATION & CRISIS PORTAL"
                                   else if (currentLanguage == AppLanguage.ESPANOL) "🎓 PORTAL DE EDUCACIÓN SOCIAL Y CRISIS"
                                   else "🎓 PORTAL DE EDUCAÇÃO SOCIAL E CRISE",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0x3300FF88), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.ENGLISH) "SOCIETAL VALUE"
                                   else if (currentLanguage == AppLanguage.ESPANOL) "VALOR SOCIAL"
                                   else "VALOR SOCIAL",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FF88),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Sincere preamble about education as our true wealth
                Text(
                    text = if (currentLanguage == AppLanguage.ENGLISH) {
                        "Supporting education is our true wealth: the future of our children. Through automated Smart Contracts, we dynamically redirect local tax dividends from licensing directly to public schools, advanced universities, and physics labs to build societal resilience."
                    } else if (currentLanguage == AppLanguage.ESPANOL) {
                        "Apoyar la educación es nuestra verdadera riqueza: el futuro de nuestros niños. A través de Contratos Inteligentes automatizados, redirigimos los dividendos de impuestos locales directamente a escuelas públicas, universidades y laboratorios de física para construir resiliencia social."
                    } else {
                        "Apoiar a educação é a nossa verdadeira riqueza: o futuro das nossas crianças. Através de Contratos Inteligentes automatizados, redirecionamos dividendos de impostos locais diretamente para escolas públicas, universidades e laboratórios de física para construir resiliência social."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8892B0),
                    lineHeight = 14.sp
                )

                Divider(color = Color(0xFF222C4E))

                // Section 1: Dynamic Liquidity & Licensing Tax Splits
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (currentLanguage == AppLanguage.ENGLISH) "📊 DYNAMIC TAX LIQUIDITY SPLIT"
                               else if (currentLanguage == AppLanguage.ESPANOL) "📊 DISTRIBUCIÓN DE LIQUIDEZ IMPOSITIVA"
                               else "📊 DISTRIBUIÇÃO DE LIQUIDEZ TRIBUTÁRIA",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FFCC),
                        fontFamily = FontFamily.Monospace
                    )

                    // Financial metrics card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF081021)),
                        border = BorderStroke(1.dp, Color(0xFF1B2A4A))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Quality-Adjusted License Price:", fontSize = 8.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                Text("$${String.format("%.2f", dynamicLicenseValue)} USD", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Local Brazil Tax (30% cleared Day 1):", fontSize = 8.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                Text("$${String.format("%.2f", brazilTax30)} USD", fontSize = 8.sp, color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Direct Educational Allocation:", fontSize = 8.sp, color = Color(0xFF00FF88), fontFamily = FontFamily.Monospace)
                                Text("$${String.format("%.2f", allocatedEducationFunds)} USD", fontSize = 8.sp, color = Color(0xFF00FF88), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Net Remaining Federal Tax:", fontSize = 8.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                Text("$${String.format("%.2f", netBrazilTaxRemaining)} USD", fontSize = 8.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    // Allocation Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.ENGLISH) "Education Alloc: ${educationTaxAllocationPercent.toInt()}%"
                                   else if (currentLanguage == AppLanguage.ESPANOL) "Asignación Educativa: ${educationTaxAllocationPercent.toInt()}%"
                                   else "Alocação Educativa: ${educationTaxAllocationPercent.toInt()}%",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (educationTaxAllocationPercent >= 70f) "🔥 EXCELLENT SUPPORT" else "STANDARD SUPPORT",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (educationTaxAllocationPercent >= 70f) Color(0xFF00FF88) else Color(0xFF8892B0),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Slider(
                        value = educationTaxAllocationPercent,
                        onValueChange = { educationTaxAllocationPercent = it },
                        valueRange = 10f..100f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00FF88),
                            activeTrackColor = Color(0xFF00FF88),
                            inactiveTrackColor = Color(0xFF1B2A4A)
                        )
                    )
                }

                Divider(color = Color(0xFF222C4E))

                // Section 2: Education Node Options
                Text(
                    text = if (currentLanguage == AppLanguage.ENGLISH) "🎯 TARGET EDUCATIONAL INFRASTRUCTURE"
                           else if (currentLanguage == AppLanguage.ESPANOL) "🎯 INFRAESTRUCTURA EDUCATIVA DESTINO"
                           else "🎯 INFRAESTRUTURA EDUCATIVA DE DESTINO",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FFCC),
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val nodes = listOf("PRIMARY", "UNIVERSITY", "PHYSICS_LAB")
                    nodes.forEach { node ->
                        val isSelected = selectedEducationNode == node
                        val nodeLabel = when (node) {
                            "PRIMARY" -> if (currentLanguage == AppLanguage.ENGLISH) "Schools" else if (currentLanguage == AppLanguage.ESPANOL) "Escuelas" else "Escolas"
                            "UNIVERSITY" -> if (currentLanguage == AppLanguage.ENGLISH) "Universities" else if (currentLanguage == AppLanguage.ESPANOL) "Universidad" else "Universidade"
                            else -> if (currentLanguage == AppLanguage.ENGLISH) "Physics & Math" else if (currentLanguage == AppLanguage.ESPANOL) "Física y Mate" else "Física & Mate"
                        }

                        Button(
                            onClick = {
                                audioEngine.playKeyTap()
                                selectedEducationNode = node
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0x3300FF88) else Color(0xFF121E36),
                                contentColor = if (isSelected) Color(0xFF00FF88) else Color(0xFF8892B0)
                            ),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF00FF88) else Color(0xFF222C4E)),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f).height(28.dp)
                        ) {
                            Text(text = nodeLabel, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Show details based on selected Node
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF081021)),
                    border = BorderStroke(1.dp, Color(0xFF1B2A4A))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (selectedEducationNode) {
                            "PRIMARY" -> {
                                Text(
                                    text = if (currentLanguage == AppLanguage.ENGLISH) "🧒 PRIMARY SCHOOLS & DIGITAL ENCLAVES (\"Los Niños\")"
                                           else if (currentLanguage == AppLanguage.ESPANOL) "🧒 ESCUELAS PRIMARIAS Y ENCLAVES DIGITALES (\"Los Niños\")"
                                           else "🧒 ESCOLAS PRIMÁRIAS E ENCLAVES DIGITAIS (\"Los Niños\")",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (currentLanguage == AppLanguage.ENGLISH) {
                                        "Funding basic educational infrastructure, tablets, and interactive coding kits. By providing technology and warm classrooms, we protect the fragile, true wealth of humanity from structural poverty."
                                    } else if (currentLanguage == AppLanguage.ESPANOL) {
                                        "Financiamiento de infraestructura educativa básica, tabletas y kits de programación interactivos. Al proveer tecnología y aulas cálidas, protegemos la frágil y verdadera riqueza de la humanidad."
                                    } else {
                                        "Financiamento de infraestrutura educacional básica, tablets e kits de programação interativos. Ao prover tecnologia e salas de aula acolhedoras, protegemos a frágil e verdadeira riqueza da humanidade."
                                    },
                                    fontSize = 8.sp,
                                    color = Color(0xFF8892B0),
                                    lineHeight = 11.sp
                                )

                                Divider(color = Color(0xFF1B2A4A))

                                val schoolsBuilt = (educationTaxAllocationPercent * 0.4f).toInt() + 4
                                val studentsFunded = schoolsBuilt * 85
                                val literacyGain = (educationTaxAllocationPercent * 0.12f) + 4.5f

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Schools Built", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                        Text("$schoolsBuilt Schools", fontSize = 10.sp, color = Color(0xFF00FF88), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    Column {
                                        Text("Children Supported", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                        Text("$studentsFunded Students", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    Column {
                                        Text("Math Literacy Gain", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                        Text("+${String.format("%.1f", literacyGain)}%", fontSize = 10.sp, color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                            "UNIVERSITY" -> {
                                Text(
                                    text = if (currentLanguage == AppLanguage.ENGLISH) "🏛️ ACADEMIC UNIVERSITIES & SMART CONTRACT AUDITING"
                                           else if (currentLanguage == AppLanguage.ESPANOL) "🏛️ UNIVERSIDADES ACADÉMICAS Y AUDITORÍA DE CONTRATOS"
                                           else "🏛️ UNIVERSIDADES ACADÊMICAS E AUDITORIA DE CONTRATOS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (currentLanguage == AppLanguage.ENGLISH) {
                                        "Advancing university research programs in Zero-Knowledge proofs, hyper-scalable game engines, micro-tariffs, and decentralized financial stability systems. Graduates build top-tier security contracts."
                                    } else if (currentLanguage == AppLanguage.ESPANOL) {
                                        "Avanzando programas universitarios en pruebas de conocimiento cero, motores de juego hiperescalables, microtarifas y sistemas de estabilidad. Los graduados programan contratos de seguridad de alto nivel."
                                    } else {
                                        "Avançando programas universitários em provas de conhecimento zero, motores de jogo hiperescaláveis, microtarifas e sistemas de estabilidade. Os graduados programam contratos de segurança de alto nível."
                                    },
                                    fontSize = 8.sp,
                                    color = Color(0xFF8892B0),
                                    lineHeight = 11.sp
                                )

                                Divider(color = Color(0xFF1B2A4A))

                                val fellowshipsActive = (educationTaxAllocationPercent * 0.15f).toInt() + 2
                                val researchPapers = (fellowshipsActive * 1.5f).toInt() + 1

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Fellowships Allocated", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                        Text("$fellowshipsActive Active Ph.Ds", fontSize = 10.sp, color = Color(0xFF00FF88), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    Column {
                                        Text("Peer-Reviewed Papers", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                        Text("$researchPapers Published", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    Column {
                                        Text("Audit Validation SLA", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                        Text("100% Formal Math", fontSize = 10.sp, color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                            "PHYSICS_LAB" -> {
                                Text(
                                    text = if (currentLanguage == AppLanguage.ENGLISH) "🔬 PHYSICS & LYAPUNOV STABILITY LABS (FRACTAL TV)"
                                           else if (currentLanguage == AppLanguage.ESPANOL) "🔬 LABS DE FÍSICA Y ESTABILIDAD (TELEVISIÓN FRACTAL)"
                                           else "🔬 LABS DE FÍSICA E ESTABILIDADE (TELEVISÃO FRACTAL)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (currentLanguage == AppLanguage.ENGLISH) {
                                        "Researching chaos theory, attractor dynamics, and Lyapunov candidate functions to balance network-wide resource stress. Below is 'Fractal TV': adjusting physics parameters alters the Julia Set fractal dynamically!"
                                    } else if (currentLanguage == AppLanguage.ESPANOL) {
                                        "Investigación en teoría del caos, atractores y funciones de Lyapunov para balancear redes. A continuación 'Fractal TV': ¡ajustar parámetros altera el fractal en tiempo real!"
                                    } else {
                                        "Pesquisa em teoria do caos, retrato de fases e funções de Lyapunov para balancear redes. Abaixo 'Televisão Fractal': ajustar parâmetros altera o fractal em tempo real!"
                                    },
                                    fontSize = 8.sp,
                                    color = Color(0xFF8892B0),
                                    lineHeight = 11.sp
                                )

                                Divider(color = Color(0xFF1B2A4A))

                                // FRACTAL TV INTERACTIVE CHANNEL
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📺 FRACTAL TV: LIVE CHAOS CHANNEL",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00FF88),
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = if (isFractalTvActive) "• ON-AIR (DYNAMIC)" else "• PAUSED",
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFractalTvActive) Color(0xFF00FF88) else Color(0xFFFF5252),
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.clickable { isFractalTvActive = !isFractalTvActive }
                                    )
                                }

                                if (isFractalTvActive) {
                                    // Live Julia Set fractal canvas!
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .border(1.dp, Color(0xFF222C4E))
                                    ) {
                                        val width = size.width
                                        val height = size.height
                                        val cols = 45
                                        val rows = 25
                                        val cellW = width / cols
                                        val cellH = height / rows

                                        // Julia fractal: z = z^2 + c
                                        val cx = fractalJuliaReal
                                        val cy = fractalJuliaImag

                                        for (i in 0 until cols) {
                                            for (j in 0 until rows) {
                                                var zx = -1.5f + (i.toFloat() / cols) * 3.0f
                                                var zy = -1.0f + (j.toFloat() / rows) * 2.0f
                                                var iter = 0
                                                val maxIter = 10
                                                while (zx * zx + zy * zy < 4.0f && iter < maxIter) {
                                                    val temp = zx * zx - zy * zy + cx
                                                    zy = 2.0f * zx * zy + cy
                                                    zx = temp
                                                    iter++
                                                }
                                                if (iter > 1) {
                                                    val ratio = iter.toFloat() / maxIter
                                                    val color = Color(
                                                        red = (0.0f + ratio * 0.2f).coerceIn(0f, 1f),
                                                        green = (0.4f + ratio * 0.6f).coerceIn(0f, 1f),
                                                        blue = (0.2f + ratio * 0.8f).coerceIn(0f, 1f)
                                                    )
                                                    drawRect(
                                                        color = color,
                                                        topLeft = androidx.compose.ui.geometry.Offset(i * cellW, j * cellH),
                                                        size = androidx.compose.ui.geometry.Size(cellW, cellH)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Real & Imaginary Julia constant controllers
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Real Part (c_r): ${String.format("%.2f", fractalJuliaReal)}", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                            Slider(
                                                value = fractalJuliaReal,
                                                onValueChange = { fractalJuliaReal = it },
                                                valueRange = -1.5f..1.5f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = Color(0xFF00FFCC),
                                                    activeTrackColor = Color(0xFF00FFCC),
                                                    inactiveTrackColor = Color(0xFF121E36)
                                                )
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Imag Part (c_i): ${String.format("%.2f", fractalJuliaImag)}", fontSize = 7.sp, color = Color(0xFF8892B0), fontFamily = FontFamily.Monospace)
                                            Slider(
                                                value = fractalJuliaImag,
                                                onValueChange = { fractalJuliaImag = it },
                                                valueRange = -1.5f..1.5f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = Color(0xFF00FF88),
                                                    activeTrackColor = Color(0xFF00FF88),
                                                    inactiveTrackColor = Color(0xFF121E36)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Divider(color = Color(0xFF222C4E))

                // Section 3: Governments & Corporations Crisis Room
                Text(
                    text = if (currentLanguage == AppLanguage.ENGLISH) "🏛️ GOVERNMENTS & CORPORATIONS CRISIS ROOM"
                           else if (currentLanguage == AppLanguage.ESPANOL) "🏛️ SALA DE CRISIS PARA GOBIERNOS Y CORPORACIONES"
                           else "🏛️ SALA DE CRISE PARA GOVERNOS E CORPORAÇÕES",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FFCC),
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = if (currentLanguage == AppLanguage.ENGLISH) {
                        "Simulate systemic crises and calculate social stabilization. The model proves that directing dynamic funding splits to local schools and science enclaves mitigates social damage and maximizes long-term GDP."
                    } else if (currentLanguage == AppLanguage.ESPANOL) {
                        "Simule crisis sistémicas y calcule la estabilización social. El modelo demuestra que direccionar la liquidez impositiva a la educación mitiga daños y maximiza el PIB a largo plazo."
                    } else {
                        "Simule crises sistêmicas e calcule a estabilização social. O modelo demonstra que direcionar liquidez tributária para a educação mitiga danos e maximiza o PIB de longo prazo."
                    },
                    fontSize = 8.sp,
                    color = Color(0xFF8892B0),
                    lineHeight = 11.sp
                )

                // Selectable Crisis Types
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val crisisScenarios = listOf("ECONOMIC_IMPACT", "CLIMATE_AND_ENERGY", "INFRASTRUCTURE_LOAD")
                    crisisScenarios.forEach { scenario ->
                        val isScenarioSelected = selectedCrisisType == scenario
                        val scenarioLabel = when (scenario) {
                            "ECONOMIC_IMPACT" -> if (currentLanguage == AppLanguage.ENGLISH) "Inflation" else if (currentLanguage == AppLanguage.ESPANOL) "Inflación" else "Inflação"
                            "CLIMATE_AND_ENERGY" -> if (currentLanguage == AppLanguage.ENGLISH) "Energy Grid" else if (currentLanguage == AppLanguage.ESPANOL) "Red Eléctrica" else "Rede Elétrica"
                            else -> if (currentLanguage == AppLanguage.ENGLISH) "Cyber Attack" else if (currentLanguage == AppLanguage.ESPANOL) "Cyber Ataque" else "Cyber Ataque"
                        }

                        Button(
                            onClick = {
                                audioEngine.playKeyTap()
                                selectedCrisisType = scenario
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScenarioSelected) Color(0x33FF5252) else Color(0xFF121E36),
                                contentColor = if (isScenarioSelected) Color(0xFFFF5252) else Color(0xFF8892B0)
                            ),
                            border = BorderStroke(1.dp, if (isScenarioSelected) Color(0xFFFF5252) else Color(0xFF222C4E)),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(24.dp)
                        ) {
                            Text(text = scenarioLabel, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Interactive Run Button & Console
                Button(
                    onClick = {
                        audioEngine.playKeyTap()
                        isSimulationRunning = true
                        val randomHash = java.util.UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
                        val currentTimestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
                        
                        val mitigationPower = educationTaxAllocationPercent / 100f
                        val socialIndex = 65f + (educationTaxAllocationPercent * 0.3f)

                        simulatedCrisisLogs = when (selectedCrisisType) {
                            "ECONOMIC_IMPACT" -> listOf(
                                "[$currentTimestamp] Awaiting secure Smart Contract dispatch...",
                                "[$currentTimestamp] Scenario: Hyperinflationary Shock detected on resource alloys.",
                                "[$currentTimestamp] Education Factor: Allocated $${String.format("%.2f", allocatedEducationFunds)} USD dynamically.",
                                "[$currentTimestamp] MITIGATION CALCULATION: Social Resilience Index = ${String.format("%.1f", socialIndex)}/100",
                                "[$currentTimestamp] result: ${if (mitigationPower >= 0.7f) "EXCELLENT. Hyperinflation fully offset by highly skilled economic engineers." else "WARNING: High volatility remains. Increase education split!"}",
                                "[$currentTimestamp] CRYPTO SEAL: SHA-$randomHash VERIFIED"
                            )
                            "CLIMATE_AND_ENERGY" -> listOf(
                                "[$currentTimestamp] Awaiting secure Smart Contract dispatch...",
                                "[$currentTimestamp] Scenario: Severe Fusion Grid overloaded with 22.4 GW surge.",
                                "[$currentTimestamp] Math Labs Input: Solving Lyapunov stability differential V(x) = αx²...",
                                "[$currentTimestamp] MITIGATION CALCULATION: Active control restored grid stability in 14.8ms.",
                                "[$currentTimestamp] result: ${if (mitigationPower >= 0.5f) "STABLE. Lyapunov damping fully prevents grid cascade collapse." else "CRITICAL: Power blackouts in outer districts. Upgrade Physics Labs!"}",
                                "[$currentTimestamp] CRYPTO SEAL: SHA-$randomHash VERIFIED"
                            )
                            else -> listOf(
                                "[$currentTimestamp] Awaiting secure Smart Contract dispatch...",
                                "[$currentTimestamp] Scenario: Quantum Cyber attack overloading decentralized enclaves.",
                                "[$currentTimestamp] University Security: Deploying 256-bit ZKP signature defenses.",
                                "[$currentTimestamp] MITIGATION CALCULATION: Thwarted 98.4% of packet injections in real-time.",
                                "[$currentTimestamp] result: ${if (mitigationPower >= 0.6f) "SECURE. Decentralized academic defense blocks quantum breaches." else "ALERT: Compromised ledger nodes detected. Increase University funding!"}",
                                "[$currentTimestamp] CRYPTO SEAL: SHA-$randomHash VERIFIED"
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSimulationRunning) Color(0xFF121E36) else Color(0xFF00FF88),
                        contentColor = if (isSimulationRunning) Color(0xFF8892B0) else Color.Black
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth().height(32.dp).testTag("run_crisis_simulation_btn")
                ) {
                    Text(
                        text = if (isSimulationRunning) "RE-RUN IMPACT SIMULATION" else "RUN IMPACT & STABILIZATION SIMULATION",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF050915))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    simulatedCrisisLogs.forEach { log ->
                        Text(
                            text = "> $log",
                            fontSize = 7.5.sp,
                            color = if (log.contains("EXCELLENT") || log.contains("STABLE") || log.contains("SECURE")) Color(0xFF00FF88)
                                    else if (log.contains("WARNING") || log.contains("CRITICAL") || log.contains("ALERT")) Color(0xFFFF5252)
                                    else if (log.contains("VERIFIED")) Color(0xFF00FFCC)
                                    else Color(0xFF8892B0),
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 10.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ========================== VERIFIED CERTIFICATIONS PANEL ==========================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10172D)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF222C4E))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🛡️ SECURE COMPLIANCE & MATHEMATICAL VERIFICATIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val certs = listOf(
                        "ZKP-GATE-ZERO" to "Active Proofs",
                        "SOC-3 QUANTUM" to "Crypto Enclave",
                        "ISO-27001-MATH" to "Formal Verification",
                        "AUTONOMOUS-GEO" to "Active-Active"
                    )

                    certs.forEach { (name, label) ->
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1123)),
                            border = BorderStroke(1.dp, Color(0x3300FFCC))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00FFCC),
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = label,
                                    fontSize = 7.sp,
                                    color = Color(0xFF8892B0),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
