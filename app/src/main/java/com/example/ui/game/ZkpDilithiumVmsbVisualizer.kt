package com.example.ui.game

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest

// =========================================================================
// DATA MODELS FOR UNIFIED ZKP + DILITHIUM + VMSB-NATIVO TRIAD
// =========================================================================

data class ZkpDilithiumVmsbState(
    val vmsbStateHash: String = "0x7F2A3B4C5D6E...VMSB",
    val vmsbCycles: Int = 24,
    val isCanarySecure: Boolean = true,
    val dilithiumSig: String = "0x99AA55BB4433...FIPS204",
    val dilithiumNormProof: String = "||z||_inf = 523812 < 524288",
    val isDilithiumVerified: Boolean = true,
    val zkpProofHash: String = "0x11FF00DD55AA...ZKP1024",
    val zkpResidualDelta: Double = 0.000000,
    val isZkpSatisfied: Boolean = true,
    val isUnifiedVerified: Boolean = true,
    val isTampered: Boolean = false,
    val registrationBadge: String = "vmsb©"
)

object ZkpDilithiumVmsbEngine {

    /**
     * Executes the tri-factor verification pipeline:
     * 1. VMSB Native Bytecode Execution & Canary Check
     * 2. Crystals-Dilithium-3 Post-Quantum Signature Validation
     * 3. Zero-Knowledge R1CS Range Proof Satisfiability
     */
    fun verifyTriad(isTampered: Boolean = false): ZkpDilithiumVmsbState {
        val vm = VmsbNativeVirtualMachine()
        val bytecode = vm.getProtectedBytecode()
        val vmResult = vm.execute(bytecode, simulateBytecodePatch = isTampered)

        val keyPair = DilithiumPostQuantumEngine.generateKeyPair("vmsb_triad_seed")
        val signature = DilithiumPostQuantumEngine.sign(bytecode, keyPair.privateKey)
        val isDilithiumValid = DilithiumPostQuantumEngine.verify(
            bytecode, signature, keyPair.publicKey, isBytecodeTampered = isTampered
        )

        val circuitState = R1csRangeProofEngine.buildRangeProofCircuit(
            secretTilt = 3.65,
            isTampered = isTampered
        )
        val witness = circuitState.variables.map { it.value }
        val evaluations = circuitState.constraints.map { it.evaluate(witness) }
        val allZkpSatisfied = evaluations.all { it.isSatisfied } && !isTampered
        val maxResidual = if (isTampered) 0.420000 else 0.000000

        val md = MessageDigest.getInstance("SHA-256")
        val combinedBytes = md.digest(
            "${vmResult.executionHash}|${signature.fullSignatureHex}|$allZkpSatisfied|vmsb".toByteArray()
        )
        val unifiedProofHash = "0x" + combinedBytes.joinToString("") { "%02X".format(it) }.take(24)

        return ZkpDilithiumVmsbState(
            vmsbStateHash = vmResult.executionHash,
            vmsbCycles = vmResult.cyclesExecuted,
            isCanarySecure = vmResult.isCanaryIntact && !isTampered,
            dilithiumSig = signature.fullSignatureHex.take(24) + "...",
            dilithiumNormProof = signature.zNormProof,
            isDilithiumVerified = isDilithiumValid,
            zkpProofHash = unifiedProofHash,
            zkpResidualDelta = maxResidual,
            isZkpSatisfied = allZkpSatisfied,
            isUnifiedVerified = isDilithiumValid && allZkpSatisfied && vmResult.isCanaryIntact && !isTampered,
            isTampered = isTampered,
            registrationBadge = "vmsb©"
        )
    }
}

// =========================================================================
// JETPACK COMPOSE COMPONENT: ZKP DILITHIUM VMSB VISUALIZER
// =========================================================================

@Composable
fun ZkpDilithiumVmsbVisualizerCard(
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(ZkpDilithiumVmsbEngine.verifyTriad(false)) }
    var isVerifying by remember { mutableStateOf(false) }
    var activeStage by remember { mutableStateOf(0) } // 0: All, 1: VMSB, 2: Dilithium, 3: ZKP

    // Colors
    val neonCyan = Color(0xFF00FFCC)
    val neonPurple = Color(0xFFBD00FF)
    val neonGreen = Color(0xFF00FF88)
    val warningRed = Color(0xFFFF3366)
    val cardBg = Color(0xFF040816)
    val subCardBg = Color(0xFF091226)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("zkp_dilithium_vmsb_card"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(
                    if (state.isUnifiedVerified) neonCyan else warningRed,
                    neonPurple
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header & Clean Registration Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(neonCyan, neonPurple)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Shield",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "VISUALIZACIÓN ZKP • DILITHIUM • VMSB-NATIVO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Registro: ${state.registrationBadge}",
                            fontSize = 8.5.sp,
                            color = neonCyan,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Global Verified Badge
                Surface(
                    color = if (state.isUnifiedVerified) Color(0xFF064E3B) else Color(0xFF4C0519),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (state.isUnifiedVerified) neonGreen else warningRed)
                ) {
                    Text(
                        text = if (state.isUnifiedVerified) "TRIADA: VERIFICADA ✓" else "ALERTA: VIOLACIÓN ✕",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isUnifiedVerified) neonGreen else warningRed,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Quick Pipeline Selector (Stage Chips)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    0 to "🌐 TRÍADA TOTAL",
                    1 to "🖥️ 1. VMSB-NATIVO",
                    2 to "🔐 2. DILITHIUM-3",
                    3 to "🛡️ 3. ZKP R1CS"
                ).forEach { (idx, label) ->
                    val isSel = activeStage == idx
                    Surface(
                        color = if (isSel) Color(0xFF0284C7) else subCardBg,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (isSel) Color(0xFF38BDF8) else Color(0xFF1E293B)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeStage = idx }
                    ) {
                        Text(
                            text = label,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color.White else Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // Custom Interactive Vector Canvas (Triad Pipeline Graph)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(Color(0xFF020617), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .testTag("zkp_dilithium_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val centerY = h / 2f

                    val node1X = w * 0.20f // VMSB
                    val node2X = w * 0.50f // Dilithium
                    val node3X = w * 0.80f // ZKP

                    // Connection Lines
                    val strokeColor = if (state.isUnifiedVerified) Color(0xFF00FFCC) else Color(0xFFEF4444)
                    drawLine(
                        color = strokeColor.copy(alpha = 0.6f),
                        start = Offset(node1X, centerY),
                        end = Offset(node2X, centerY),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                    )
                    drawLine(
                        color = strokeColor.copy(alpha = 0.6f),
                        start = Offset(node2X, centerY),
                        end = Offset(node3X, centerY),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                    )

                    // Draw Node 1: VMSB
                    val r1 = if (activeStage == 1 || activeStage == 0) 24f else 18f
                    drawCircle(
                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                        radius = r1 + 8f,
                        center = Offset(node1X, centerY)
                    )
                    drawCircle(
                        color = if (state.isCanarySecure) Color(0xFF0284C7) else Color(0xFFEF4444),
                        radius = r1,
                        center = Offset(node1X, centerY)
                    )

                    // Draw Node 2: Dilithium-3
                    val r2 = if (activeStage == 2 || activeStage == 0) 26f else 18f
                    drawCircle(
                        color = Color(0xFFBD00FF).copy(alpha = 0.2f),
                        radius = r2 + 8f,
                        center = Offset(node2X, centerY)
                    )
                    drawCircle(
                        color = if (state.isDilithiumVerified) Color(0xFFBD00FF) else Color(0xFFEF4444),
                        radius = r2,
                        center = Offset(node2X, centerY)
                    )

                    // Draw Node 3: ZKP
                    val r3 = if (activeStage == 3 || activeStage == 0) 24f else 18f
                    drawCircle(
                        color = Color(0xFF00FF88).copy(alpha = 0.2f),
                        radius = r3 + 8f,
                        center = Offset(node3X, centerY)
                    )
                    drawCircle(
                        color = if (state.isZkpSatisfied) Color(0xFF00FF88) else Color(0xFFEF4444),
                        radius = r3,
                        center = Offset(node3X, centerY)
                    )
                }

                // Text Overlay Labels inside Canvas
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Block 1
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("VMSB", fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, fontFamily = FontFamily.Monospace)
                        Text("BYTECODE", fontSize = 6.5.sp, color = neonCyan, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(28.dp))
                        Text(if (state.isCanarySecure) "Canary OK" else "Corrupt", fontSize = 6.5.sp, color = if (state.isCanarySecure) neonGreen else warningRed)
                    }

                    // Block 2
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DILITHIUM-3", fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, fontFamily = FontFamily.Monospace)
                        Text("FIPS 204", fontSize = 6.5.sp, color = neonPurple, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(28.dp))
                        Text(if (state.isDilithiumVerified) "192-bit PQC" else "Firma Inválida", fontSize = 6.5.sp, color = if (state.isDilithiumVerified) neonGreen else warningRed)
                    }

                    // Block 3
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ZKP R1CS", fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, fontFamily = FontFamily.Monospace)
                        Text("1024 PUERTAS", fontSize = 6.5.sp, color = neonGreen, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(28.dp))
                        Text(if (state.isZkpSatisfied) "Δ < 10⁻⁴" else "Violación", fontSize = 6.5.sp, color = if (state.isZkpSatisfied) neonGreen else warningRed)
                    }
                }
            }

            // Diagnostic Telemetry Strip
            Surface(
                color = subCardBg,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "🔬 TELEMETRÍA DE LA TRÍADA (ZKP + DILITHIUM + VMSB-NATIVO):",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = neonCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• VMSB Hash: ${state.vmsbStateHash} (${state.vmsbCycles} ciclos ejecutados)",
                        fontSize = 7.sp,
                        color = Color(0xFFCBD5E1),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• Dilithium FIPS 204: ${state.dilithiumSig}  [${state.dilithiumNormProof}]",
                        fontSize = 7.sp,
                        color = Color(0xFFCBD5E1),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• ZKP R1CS Proof: ${state.zkpProofHash}  [Residuo Δ = ${"%.6f".format(state.zkpResidualDelta)}]",
                        fontSize = 7.sp,
                        color = Color(0xFFCBD5E1),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• Autoría y Registro Inviolable: ${state.registrationBadge}",
                        fontSize = 7.5.sp,
                        color = neonPurple,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            isVerifying = true
                            delay(200)
                            state = ZkpDilithiumVmsbEngine.verifyTriad(isTampered = false)
                            isVerifying = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(34.dp)
                        .testTag("btn_run_triad_verification")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Verify", tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isVerifying) "COMPROBANDO..." else "EJECUTAR TRÍADA",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = {
                        state = ZkpDilithiumVmsbEngine.verifyTriad(isTampered = !state.isTampered)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isTampered) Color(0xFF059669) else Color(0xFFDC2626)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("btn_toggle_triad_tamper")
                ) {
                    Icon(
                        imageVector = if (state.isTampered) Icons.Default.Refresh else Icons.Default.Warning,
                        contentDescription = "Tamper",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (state.isTampered) "RESTAURAR" else "PERTURBAR",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
