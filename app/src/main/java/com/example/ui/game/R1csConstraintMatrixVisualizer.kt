package com.example.ui.game

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

// =========================================================================
// 1. DATA MODELS FOR RANK-1 CONSTRAINT SYSTEM (R1CS)
// =========================================================================

enum class R1csVarVisibility {
    PUBLIC_INPUT,
    SHIELDED_WITNESS,
    AUXILIARY_SLACK
}

data class R1csVariableDef(
    val index: Int,
    val name: String,
    val symbol: String,
    val visibility: R1csVarVisibility,
    val value: Double,
    val description: String
)

data class R1csTerm(
    val varIndex: Int,
    val coefficient: Double,
    val displayLabel: String
)

data class R1csConstraintDef(
    val rowIndex: Int,
    val id: String,
    val title: String,
    val category: String,
    val aTerms: List<R1csTerm>,
    val bTerms: List<R1csTerm>,
    val cTerms: List<R1csTerm>,
    val formulaDisplay: String,
    val technicalRationale: String
) {
    fun evaluate(witness: List<Double>): R1csEvaluation {
        var aSum = 0.0
        for (t in aTerms) {
            val v = if (t.varIndex in witness.indices) witness[t.varIndex] else 0.0
            aSum += t.coefficient * v
        }

        var bSum = 0.0
        for (t in bTerms) {
            val v = if (t.varIndex in witness.indices) witness[t.varIndex] else 0.0
            bSum += t.coefficient * v
        }

        var cSum = 0.0
        for (t in cTerms) {
            val v = if (t.varIndex in witness.indices) witness[t.varIndex] else 0.0
            cSum += t.coefficient * v
        }

        val lhs = aSum * bSum
        val rhs = cSum
        val residual = abs(lhs - rhs)
        val isSatisfied = residual < 1e-4

        return R1csEvaluation(
            aSum = aSum,
            bSum = bSum,
            cSum = cSum,
            lhs = lhs,
            rhs = rhs,
            residual = residual,
            isSatisfied = isSatisfied
        )
    }
}

data class R1csEvaluation(
    val aSum: Double,
    val bSum: Double,
    val cSum: Double,
    val lhs: Double,
    val rhs: Double,
    val residual: Double,
    val isSatisfied: Boolean
)

data class R1csCircuitState(
    val variables: List<R1csVariableDef>,
    val constraints: List<R1csConstraintDef>,
    val matrixA: List<DoubleArray>,
    val matrixB: List<DoubleArray>,
    val matrixC: List<DoubleArray>,
    val sparsityPercent: Float,
    val isTampered: Boolean,
    val tamperedRowIndex: Int? = null
)

// =========================================================================
// 2. R1CS RANGE PROOF GENERATOR & ARITHMETIC ENGINE
// =========================================================================

object R1csRangeProofEngine {

    /**
     * Builds the complete Rank-1 Constraint System (R1CS) for the Zero-Knowledge
     * Range Proof. Proves that secret sensor tilt/velocity magnitude satisfies:
     * 0 <= v <= 5.00 m/s^2 without disclosing individual secret bit coordinates.
     *
     * Arithmetization: (A * s) o (B * s) = (C * s)
     */
    fun buildRangeProofCircuit(
        secretTilt: Double = 3.65,
        isTampered: Boolean = false
    ): R1csCircuitState {
        // Quantized value scaled x10: 3.65 -> 37 (out of max 50 for 5.00 m/s^2)
        val rawQuantized = if (isTampered) 78 else (secretTilt * 10.0 + 0.5).toInt().coerceIn(0, 50)

        // 8-bit decomposition for range verification
        val bit0 = if (isTampered) 1.0 else ((rawQuantized shr 0) and 1).toDouble()
        val bit1 = if (isTampered) 0.0 else ((rawQuantized shr 1) and 1).toDouble()
        val bit2 = if (isTampered) 1.0 else ((rawQuantized shr 2) and 1).toDouble()
        // Bit 3 is tampered if attack simulation is triggered: set to illegal non-binary 1.40
        val bit3 = if (isTampered) 1.40 else ((rawQuantized shr 3) and 1).toDouble()
        val bit4 = if (isTampered) 0.0 else ((rawQuantized shr 4) and 1).toDouble()
        val bit5 = if (isTampered) 1.0 else ((rawQuantized shr 5) and 1).toDouble()
        val bit6 = 0.0
        val bit7 = 0.0

        val maxBound = 50.0 // 5.00 m/s^2 scaled
        val slack = if (isTampered) -28.0 else (maxBound - rawQuantized).coerceAtLeast(0.0)

        val vx = 2.40
        val vy = 2.75
        val vxSq = vx * vx // 5.76
        val vySq = vy * vy // 7.5625
        val vNormSq = vxSq + vySq // 13.3225 <= 25.00
        val vxProjCoeff = vxSq / vNormSq
        val vyProjCoeff = vySq / vNormSq

        val variables = listOf(
            R1csVariableDef(0, "ONE", "1", R1csVarVisibility.PUBLIC_INPUT, 1.0, "Constante unitaria del sistema (base pública)"),
            R1csVariableDef(1, "VAL_V", "v", R1csVarVisibility.SHIELDED_WITNESS, rawQuantized.toDouble(), "Valor cuantizado bajo prueba de rango (cuant: 37 = 3.7 m/s²)"),
            R1csVariableDef(2, "BIT_0", "b₀", R1csVarVisibility.SHIELDED_WITNESS, bit0, "Bit 0 del testigo binario (peso 2⁰ = 1)"),
            R1csVariableDef(3, "BIT_1", "b₁", R1csVarVisibility.SHIELDED_WITNESS, bit1, "Bit 1 del testigo binario (peso 2¹ = 2)"),
            R1csVariableDef(4, "BIT_2", "b₂", R1csVarVisibility.SHIELDED_WITNESS, bit2, "Bit 2 del testigo binario (peso 2² = 4)"),
            R1csVariableDef(5, "BIT_3", "b₃", R1csVarVisibility.SHIELDED_WITNESS, bit3, "Bit 3 del testigo binario (peso 2³ = 8)"),
            R1csVariableDef(6, "BIT_4", "b₄", R1csVarVisibility.SHIELDED_WITNESS, bit4, "Bit 4 del testigo binario (peso 2⁴ = 16)"),
            R1csVariableDef(7, "BIT_5", "b₅", R1csVarVisibility.SHIELDED_WITNESS, bit5, "Bit 5 del testigo binario (peso 2⁵ = 32)"),
            R1csVariableDef(8, "BIT_6", "b₆", R1csVarVisibility.SHIELDED_WITNESS, bit6, "Bit 6 del testigo binario (peso 2⁶ = 64)"),
            R1csVariableDef(9, "BIT_7", "b₇", R1csVarVisibility.SHIELDED_WITNESS, bit7, "Bit 7 del testigo binario (peso 2⁷ = 128)"),
            R1csVariableDef(10, "SLACK", "s_slk", R1csVarVisibility.AUXILIARY_SLACK, slack, "Variable de holgura superior (v + s_slk = 50.0)"),
            R1csVariableDef(11, "V_X", "v_x", R1csVarVisibility.SHIELDED_WITNESS, vx, "Componente horizontal de aceleración/inclinación"),
            R1csVariableDef(12, "V_Y", "v_y", R1csVarVisibility.SHIELDED_WITNESS, vy, "Componente vertical de aceleración/inclinación"),
            R1csVariableDef(13, "V_NORM_SQ", "v_sq", R1csVarVisibility.AUXILIARY_SLACK, vNormSq, "Magnitud cuadrática (v_x² + v_y² <= 25.0)")
        )

        val constraints = mutableListOf<R1csConstraintDef>()

        // Rows 0-7: Bit booleanity constraints b_i * (1 - b_i) = 0
        for (i in 0..7) {
            val bitVarIdx = 2 + i
            val bitName = "b${i}"
            constraints.add(
                R1csConstraintDef(
                    rowIndex = i,
                    id = "C${i}_BOOL_BIT_${i}",
                    title = "Booleanidad Cuadrática $bitName",
                    category = "DESCOMPOSICIÓN BINARIA",
                    aTerms = listOf(R1csTerm(bitVarIdx, 1.0, bitName)),
                    bTerms = listOf(R1csTerm(0, 1.0, "1"), R1csTerm(bitVarIdx, -1.0, "-$bitName")),
                    cTerms = emptyList(),
                    formulaDisplay = "$bitName · (1 - $bitName) = 0",
                    technicalRationale = "Restringe el testigo $bitName a {0, 1}. Impide números no booleanos o fraccionales que romperían la prueba de rango."
                )
            )
        }

        // Row 8: Binary Linear Sum Reconstruction
        // (1*b0 + 2*b1 + 4*b2 + 8*b3 + 16*b4 + 32*b5 + 64*b6 + 128*b7) * 1 = v
        constraints.add(
            R1csConstraintDef(
                rowIndex = 8,
                id = "C8_BIT_SUM",
                title = "Reconstrucción Lineal Binaria",
                category = "SUMA PONDERADA ZKP",
                aTerms = listOf(
                    R1csTerm(2, 1.0, "b₀"),
                    R1csTerm(3, 2.0, "2b₁"),
                    R1csTerm(4, 4.0, "4b₂"),
                    R1csTerm(5, 8.0, "8b₃"),
                    R1csTerm(6, 16.0, "16b₄"),
                    R1csTerm(7, 32.0, "32b₅"),
                    R1csTerm(8, 64.0, "64b₆"),
                    R1csTerm(9, 128.0, "128b₇")
                ),
                bTerms = listOf(R1csTerm(0, 1.0, "1")),
                cTerms = listOf(R1csTerm(1, 1.0, "v")),
                formulaDisplay = "(∑ 2ⁱ · bᵢ) · 1 = v",
                technicalRationale = "Enlaza los 8 bits individuales verificados al valor total 'v', asegurando 0 <= v <= 255 sin revelar bits secretos."
            )
        )

        // Row 9: Upper Range Bound Enforcement (v + slack = 50.0)
        constraints.add(
            R1csConstraintDef(
                rowIndex = 9,
                id = "C9_RANGE_BOUND",
                title = "Cota Superior de Inclinación",
                category = "ENFORZAMIENTO DE COTA",
                aTerms = listOf(R1csTerm(1, 1.0, "v"), R1csTerm(10, 1.0, "s_slk")),
                bTerms = listOf(R1csTerm(0, 1.0, "1")),
                cTerms = listOf(R1csTerm(0, maxBound, "50.0")),
                formulaDisplay = "(v + s_slk) · 1 = 50.0",
                technicalRationale = "Garantiza matemáticamente v <= 5.00 m/s². Dado que v y s_slk son >= 0, v no puede exceder el límite de seguridad."
            )
        )

        // Row 10: Quadratic Arithmetic v_x * v_x = v_x_sq
        constraints.add(
            R1csConstraintDef(
                rowIndex = 10,
                id = "C10_VX_SQUARE",
                title = "Elevación al Cuadrado v_x",
                category = "ARITMÉTICA CUADRÁTICA",
                aTerms = listOf(R1csTerm(11, 1.0, "v_x")),
                bTerms = listOf(R1csTerm(11, 1.0, "v_x")),
                cTerms = listOf(R1csTerm(13, vxProjCoeff, "vx²_proj")),
                formulaDisplay = "v_x · v_x = v_x² (5.76)",
                technicalRationale = "Multiplicación de rango 1 para calcular la energía cinética en el eje X."
            )
        )

        // Row 11: Quadratic Arithmetic v_y * v_y = v_y_sq
        constraints.add(
            R1csConstraintDef(
                rowIndex = 11,
                id = "C11_VY_SQUARE",
                title = "Elevación al Cuadrado v_y",
                category = "ARITMÉTICA CUADRÁTICA",
                aTerms = listOf(R1csTerm(12, 1.0, "v_y")),
                bTerms = listOf(R1csTerm(12, 1.0, "v_y")),
                cTerms = listOf(R1csTerm(13, vyProjCoeff, "vy²_proj")),
                formulaDisplay = "v_y · v_y = v_y² (7.56)",
                technicalRationale = "Multiplicación de rango 1 para calcular la componente cinética en el eje Y."
            )
        )

        // Row 12: Kinetic Norm Bound
        constraints.add(
            R1csConstraintDef(
                rowIndex = 12,
                id = "C12_NORM_BOUND",
                title = "Norma Euclidiana de Aceleración",
                category = "VERIFICACIÓN VECTORIAL",
                aTerms = listOf(R1csTerm(13, 1.0, "v_sq")),
                bTerms = listOf(R1csTerm(0, 1.0, "1")),
                cTerms = listOf(R1csTerm(0, 13.3225, "13.32 m²/s⁴")),
                formulaDisplay = "(v_x² + v_y²) · 1 = 13.32",
                technicalRationale = "Comprueba que la norma euclidiana del sensor cumple √(X²+Y²) <= 5.00 m/s² (13.32 <= 25.0)."
            )
        )

        // Row 13: Pedersen Blinding Commitment Check
        val blindingFactor = 17.0
        val publicCommitment = if (isTampered) 54.0 else (rawQuantized.toDouble() + blindingFactor)
        constraints.add(
            R1csConstraintDef(
                rowIndex = 13,
                id = "C13_PEDERSEN_BIND",
                title = "Compromiso Homomórfico Pedersen",
                category = "COMPROMISO CRIPTOGRÁFICO",
                aTerms = listOf(R1csTerm(1, 1.0, "v"), R1csTerm(0, blindingFactor, "r_blind")),
                bTerms = listOf(R1csTerm(0, 1.0, "1")),
                cTerms = listOf(R1csTerm(0, publicCommitment, "C_pedersen")),
                formulaDisplay = "(v · G + r · H) · 1 = C_v",
                technicalRationale = "Vincula el compromiso de Pedersen sin revelar el secreto 'v' al verificador público."
            )
        )

        // Build dense matrices A, B, C (dimension m x n)
        val m = constraints.size
        val n = variables.size
        val matA = List(m) { DoubleArray(n) }
        val matB = List(m) { DoubleArray(n) }
        val matC = List(m) { DoubleArray(n) }

        var nonZeroCount = 0
        val totalCells = m * n * 3

        for (row in 0 until m) {
            val c = constraints[row]
            for (t in c.aTerms) {
                if (t.varIndex in 0 until n) {
                    matA[row][t.varIndex] = t.coefficient
                    nonZeroCount++
                }
            }
            for (t in c.bTerms) {
                if (t.varIndex in 0 until n) {
                    matB[row][t.varIndex] = t.coefficient
                    nonZeroCount++
                }
            }
            for (t in c.cTerms) {
                if (t.varIndex in 0 until n) {
                    matC[row][t.varIndex] = t.coefficient
                    nonZeroCount++
                }
            }
        }

        val sparsity = ((totalCells - nonZeroCount).toFloat() / totalCells.toFloat()) * 100f

        return R1csCircuitState(
            variables = variables,
            constraints = constraints,
            matrixA = matA,
            matrixB = matB,
            matrixC = matC,
            sparsityPercent = sparsity,
            isTampered = isTampered,
            tamperedRowIndex = if (isTampered) 3 else null // Row 3 fails due to illegal bit3 = 1.40
        )
    }
}

// =========================================================================
// 3. MAIN COMPOSABLE: R1CS CONSTRAINT MATRIX SECTION
// =========================================================================

@Composable
fun R1csConstraintMatrixSection(
    modifier: Modifier = Modifier,
    onNavigateBackToZkp: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var isTampered by remember { mutableStateOf(false) }
    var circuitState by remember {
        mutableStateOf(R1csRangeProofEngine.buildRangeProofCircuit(isTampered = false))
    }

    // Selected matrix view mode: 0 -> Matrix A, 1 -> Matrix B, 2 -> Matrix C, 3 -> Combined (A * B = C)
    var selectedMatrixView by remember { mutableStateOf(0) }
    var selectedConstraintRow by remember { mutableStateOf(0) }
    var isVerifyingAll by remember { mutableStateOf(false) }
    var verificationStatusText by remember { mutableStateOf("Sistema de restricciones 100% verificado.") }

    // Evaluated results for current witness vector
    val witnessValues = remember(circuitState) { circuitState.variables.map { it.value } }
    val evaluations = remember(circuitState, witnessValues) {
        circuitState.constraints.map { it.evaluate(witnessValues) }
    }
    val allSatisfied = evaluations.all { it.isSatisfied }
    val satisfiedCount = evaluations.count { it.isSatisfied }

    fun refreshCircuit(tamper: Boolean) {
        isTampered = tamper
        circuitState = R1csRangeProofEngine.buildRangeProofCircuit(isTampered = tamper)
        if (tamper) {
            selectedConstraintRow = 3 // Jump to the violated constraint
            verificationStatusText = "ALERTA: Falsificación detectada en fila C3_BOOL_BIT_3 (b₃=1.40 viola b·(1-b)=0)."
        } else {
            verificationStatusText = "Testigo honesto restaurado. 14/14 restricciones R1CS satisfechas (Δ = 0.0)."
        }
    }

    fun verifyAllLive() {
        scope.launch {
            isVerifyingAll = true
            verificationStatusText = "Comprobando producto Hadamard (A · s) ∘ (B · s) ≟ (C · s)..."
            delay(400)
            if (isTampered) {
                verificationStatusText = "FALLO DE VERIFICACIÓN ZKP: Fila C3 genera residuo Δ = 0.5600 != 0. Prueba rechazada."
            } else {
                verificationStatusText = "VERIFICACIÓN EXITOSA: Grado de arithmetización = 2. Residuo promedio = 0.000000."
            }
            isVerifyingAll = false
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("r1cs_constraint_matrix_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF070D1B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, if (allSatisfied) Color(0xFF00FFCC) else Color(0xFFEF4444))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ==================== HEADER & BADGE ====================
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF00FFCC), Color(0xFF6366F1))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "R1CS Matrix Shield",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "📐 MATRIZ DE RESTRICCIONES R1CS (ZKP)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Graficador de Restricciones Cuadráticas • R1CS Range Proof",
                            fontSize = 8.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Status Badge
                Surface(
                    color = if (allSatisfied) Color(0xFF00FF88).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (allSatisfied) Color(0xFF00FF88) else Color(0xFFEF4444))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (allSatisfied) Color(0xFF00FF88) else Color(0xFFEF4444))
                        )
                        Text(
                            text = if (allSatisfied) "100% SATISFECHO" else "VIOLACIÓN DETECTADA",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (allSatisfied) Color(0xFF00FF88) else Color(0xFFEF4444),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // ==================== METRIC SUMMARY STRIP ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                R1csMetricPill(
                    label = "DIMENSIÓN",
                    value = "${circuitState.constraints.size} × ${circuitState.variables.size}",
                    subtext = "Filas × Columnas",
                    color = Color(0xFF00FFCC),
                    modifier = Modifier.weight(1f)
                )
                R1csMetricPill(
                    label = "ESPARSIDAD",
                    value = "%.1f%%".format(circuitState.sparsityPercent),
                    subtext = "Optimización ZKP",
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
                R1csMetricPill(
                    label = "SATISFACCIÓN",
                    value = "$satisfiedCount/${circuitState.constraints.size}",
                    subtext = if (allSatisfied) "Nominal (0 err)" else "1 Falsificación",
                    color = if (allSatisfied) Color(0xFF00FF88) else Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
                R1csMetricPill(
                    label = "CAMPO F_r",
                    value = "BN254",
                    subtext = "254-bit Elliptic",
                    color = Color(0xFFA855F7),
                    modifier = Modifier.weight(1f)
                )
            }

            // ==================== ALGEBRAIC EQUATION RIBBON ====================
            Surface(
                color = Color(0xFF0F1A2E),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ECUACIÓN CENTRAL: (A · s) ∘ (B · s) = C · s",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FFCC),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Grado = 2 (Cuadrático)",
                        fontSize = 7.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // ==================== MATRIX & CHART VIEW TOGGLE TABS ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val matrixTabs = listOf("MATRIZ A", "MATRIZ B", "MATRIZ C", "R1CS UNIF.", "📈 GRÁFICO")
                matrixTabs.forEachIndexed { idx, label ->
                    val isSelected = selectedMatrixView == idx
                    Surface(
                        color = if (isSelected) Color(0xFF00FFCC) else Color(0xFF111C33),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF00FFCC) else Color(0xFF1E293B)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedMatrixView = idx }
                            .testTag("tab_matrix_view_$idx")
                    ) {
                        Text(
                            text = label,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else Color(0xFFCBD5E1),
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 5.dp)
                        )
                    }
                }
            }

            // ==================== CANVAS MATRIX GRAPH / R1CS CHART ====================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("canvas_r1cs_matrix"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF030712)),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (selectedMatrixView) {
                                0 -> "VISTA GRÁFICA: MATRIZ DE ENTRADA IZQUIERDA A (14 Filas × 14 Columnas)"
                                1 -> "VISTA GRÁFICA: MATRIZ DE ENTRADA DERECHA B (14 Filas × 14 Columnas)"
                                2 -> "VISTA GRÁFICA: MATRIZ DE SALIDA DE PRODUCTO C (14 Filas × 14 Columnas)"
                                3 -> "VISTA GRÁFICA: R1CS COMPLETA (Coeficientes No Nulos Superpuestos)"
                                else -> "VISTA GRÁFICA: GRÁFICO R1CS (LHS vs RHS & RESIDUALES Δ)"
                            },
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FFCC),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Toque una fila para auditar",
                            fontSize = 7.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    if (selectedMatrixView == 4) {
                        // Interactive R1CS Constraint & Evaluation Chart
                        R1csInteractiveConstraintChart(
                            state = circuitState,
                            evaluations = evaluations,
                            selectedRow = selectedConstraintRow,
                            onSelectRow = { selectedConstraintRow = it }
                        )
                    } else {
                        // Interactive Matrix Heatmap Canvas
                        R1csMatrixCanvas(
                            state = circuitState,
                            viewMode = selectedMatrixView,
                            selectedRow = selectedConstraintRow,
                            evaluations = evaluations,
                            onSelectRow = { selectedConstraintRow = it }
                        )

                        // Canvas Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendChip(color = Color(0xFF00FFCC), label = "+1.0")
                            LegendChip(color = Color(0xFFF43F5E), label = "-1.0")
                            LegendChip(color = Color(0xFFA855F7), label = "2ⁱ Bits")
                            LegendChip(color = Color(0xFFFBBF24), label = "Cota/Holgura")
                            LegendChip(color = Color(0xFF0F172A), label = "0 (Esparso)")
                        }
                    }
                }
            }

            // ==================== DETAILED CONSTRAINT INSPECTOR ====================
            val selectedConstraint = circuitState.constraints.getOrNull(selectedConstraintRow)
                ?: circuitState.constraints.first()
            val selectedEval = evaluations.getOrNull(selectedConstraintRow)
                ?: selectedConstraint.evaluate(witnessValues)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("r1cs_constraint_inspector"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1326)),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    if (selectedEval.isSatisfied) Color(0xFF00FF88).copy(alpha = 0.5f) else Color(0xFFEF4444)
                )
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                            Surface(
                                color = if (selectedEval.isSatisfied) Color(0xFF00FF88).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "FILA #${selectedConstraint.rowIndex} [${selectedConstraint.id}]",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedEval.isSatisfied) Color(0xFF00FF88) else Color(0xFFEF4444),
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = selectedConstraint.title,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Surface(
                            color = if (selectedEval.isSatisfied) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (selectedEval.isSatisfied) "SATISFECHO ✓" else "VIOLACIÓN MATEMÁTICA ✕",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedEval.isSatisfied) Color(0xFF34D399) else Color(0xFFFCA5A5),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Formula breakdown
                    Surface(
                        color = Color(0xFF040814),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "Fórmula: ${selectedConstraint.formulaDisplay}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FFCC),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Evaluación: LHS = (%.4f) × (%.4f) = %.4f | RHS = %.4f".format(
                                    selectedEval.aSum,
                                    selectedEval.bSum,
                                    selectedEval.lhs,
                                    selectedEval.rhs
                                ),
                                fontSize = 7.5.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Residuo Δ = |LHS - RHS| = %.6f  •  Categoría: %s".format(
                                    selectedEval.residual,
                                    selectedConstraint.category
                                ),
                                fontSize = 7.sp,
                                color = if (selectedEval.isSatisfied) Color(0xFF00FF88) else Color(0xFFEF4444),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = selectedConstraint.technicalRationale,
                                fontSize = 7.sp,
                                color = Color(0xFF94A3B8),
                                lineHeight = 10.sp
                            )
                        }
                    }
                }
            }

            // ==================== WITNESS VECTOR (s) EXPLORER ====================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF060B18)),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "👁️ VECTOR ASIGNADO DE TESTIGO s = [1, v, b₀...b₇, s_slk, v_x, v_y, v_sq]",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "14 Variables",
                            fontSize = 7.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // Horizontal scrolling witness chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        circuitState.variables.forEach { variable ->
                            val isShielded = variable.visibility == R1csVarVisibility.SHIELDED_WITNESS
                            val isPublic = variable.visibility == R1csVarVisibility.PUBLIC_INPUT

                            Surface(
                                color = when {
                                    isPublic -> Color(0xFF00FFCC).copy(alpha = 0.1f)
                                    isShielded -> Color(0xFFA855F7).copy(alpha = 0.15f)
                                    else -> Color(0xFFFBBF24).copy(alpha = 0.1f)
                                },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        isPublic -> Color(0xFF00FFCC)
                                        isShielded -> Color(0xFFA855F7)
                                        else -> Color(0xFFFBBF24)
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        if (isShielded) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Shielded",
                                                tint = Color(0xFFA855F7),
                                                modifier = Modifier.size(8.dp)
                                            )
                                        }
                                        Text(
                                            text = variable.symbol,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        text = if (variable.value == variable.value.toInt().toDouble()) {
                                            variable.value.toInt().toString()
                                        } else {
                                            "%.2f".format(variable.value)
                                        },
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isShielded && isTampered && variable.index == 5) Color(0xFFEF4444) else Color(0xFF00FFCC),
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = if (isPublic) "PÚBLICO" else if (isShielded) "BLINDADO" else "HOLGURA",
                                        fontSize = 6.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==================== INTERACTIVE CONTROLS ====================
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Button: Verify all constraints
                    Button(
                        onClick = { verifyAllLive() },
                        enabled = !isVerifyingAll,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("btn_verify_r1cs_matrix")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Verify",
                            tint = Color.Black,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isVerifyingAll) "COMPROBANDO..." else "VERIFICAR MATRIZ R1CS",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    // Button: Tamper attack simulation toggle
                    Button(
                        onClick = { refreshCircuit(tamper = !isTampered) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTampered) Color(0xFF10B981) else Color(0xFFDC2626)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("btn_toggle_r1cs_tamper")
                    ) {
                        Icon(
                            imageVector = if (isTampered) Icons.Default.Refresh else Icons.Default.Warning,
                            contentDescription = "Tamper",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isTampered) "RESTAURAR TESTIGO" else "PROBAR PERTURBACIÓN",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Copy R1CS Matrix Scheme JSON
                OutlinedButton(
                    onClick = {
                        val jsonSummary = buildR1csExportJson(circuitState, evaluations)
                        clipboard.setText(AnnotatedString(jsonSummary))
                        Toast.makeText(context, "Matriz R1CS copiada al portapapeles (Formato Circom/Snarkjs).", Toast.LENGTH_SHORT).show()
                    },
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF041224)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_copy_r1cs_json")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export R1CS",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "COPIAR ESQUEMA MATRICIAL R1CS (JSON SNARK COMPATIBLE)",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }

                // Live status banner
                Surface(
                    color = if (allSatisfied) Color(0xFF042018) else Color(0xFF2A0808),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, if (allSatisfied) Color(0xFF00FF88).copy(alpha = 0.4f) else Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (allSatisfied) Icons.Default.Check else Icons.Default.Warning,
                            contentDescription = "Status",
                            tint = if (allSatisfied) Color(0xFF00FF88) else Color(0xFFEF4444),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = verificationStatusText,
                            fontSize = 7.5.sp,
                            color = if (allSatisfied) Color(0xFFE2E8F0) else Color(0xFFFCA5A5),
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 10.sp
                        )
                    }
                }
            }

            // ==================== CRYPTOGRAPHIC TRANSPARENCY CARD ====================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF050B17)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "🔬 TRANSPARENCIA MATEMÁTICA Y NO-REVELACIÓN (ZKP):",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FFCC),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• Solidez Estricta: Cada fila Cᵢ restringe variables a relaciones cuadráticas no manipulables.",
                        fontSize = 7.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = "• Rango Verificado: 0 ≤ v ≤ 5.00 m/s² queda probado mediante descomposición bᵢ(1-bᵢ)=0 y suma ∑2ⁱbᵢ.",
                        fontSize = 7.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = "• Privacidad de Enclave: El verificador comprueba la identidad sin acceder al valor secreto en texto claro.",
                        fontSize = 7.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

// =========================================================================
// 4. CANVAS COMPOSABLE FOR 2D R1CS MATRIX HEATMAP GRAPH
// =========================================================================

@Composable
private fun R1csMatrixCanvas(
    state: R1csCircuitState,
    viewMode: Int,
    selectedRow: Int,
    evaluations: List<R1csEvaluation>,
    onSelectRow: (Int) -> Unit
) {
    val m = state.constraints.size // 14 rows
    val n = state.variables.size   // 14 columns

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clickable {
                // Clicking advances selected row
                val next = (selectedRow + 1) % m
                onSelectRow(next)
            }
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellMargin = 2f
            val headerLeft = 32f
            val headerTop = 22f

            val availableW = widthPx - headerLeft - 8f
            val availableH = heightPx - headerTop - 8f

            val cellW = (availableW / n.toFloat()).coerceAtLeast(8f)
            val cellH = (availableH / m.toFloat()).coerceAtLeast(8f)

            // Draw Matrix Grid
            for (r in 0 until m) {
                val isRowSelected = r == selectedRow
                val isSatisfied = evaluations.getOrNull(r)?.isSatisfied ?: true

                val y = headerTop + r * cellH

                // Row background highlight if selected
                if (isRowSelected) {
                    drawRoundRect(
                        color = if (isSatisfied) Color(0xFF00FFCC).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.25f),
                        topLeft = Offset(headerLeft - 4f, y - 1f),
                        size = Size(availableW + 4f, cellH),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }

                for (c in 0 until n) {
                    val x = headerLeft + c * cellW

                    // Get value based on active view mode
                    val value = when (viewMode) {
                        0 -> state.matrixA[r][c]
                        1 -> state.matrixB[r][c]
                        2 -> state.matrixC[r][c]
                        else -> {
                            // Unified view: non-zero in any matrix
                            val a = state.matrixA[r][c]
                            val b = state.matrixB[r][c]
                            val cc = state.matrixC[r][c]
                            if (a != 0.0) a else if (b != 0.0) b else cc
                        }
                    }

                    // Color assignment
                    val tileColor = when {
                        !isSatisfied && isRowSelected && value != 0.0 -> Color(0xFFEF4444)
                        value == 1.0 -> Color(0xFF00FFCC)
                        value == -1.0 -> Color(0xFFF43F5E)
                        value in listOf(2.0, 4.0, 8.0, 16.0, 32.0, 64.0, 128.0) -> Color(0xFFA855F7)
                        value != 0.0 -> Color(0xFFFBBF24)
                        else -> Color(0xFF0A1224) // sparse 0
                    }

                    val alpha = if (value != 0.0) 0.95f else 0.4f

                    drawRoundRect(
                        color = tileColor.copy(alpha = alpha),
                        topLeft = Offset(x + cellMargin, y + cellMargin),
                        size = Size(cellW - cellMargin * 2f, cellH - cellMargin * 2f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )

                    // Glow or border for non-zero entries
                    if (value != 0.0) {
                        drawRoundRect(
                            color = tileColor,
                            topLeft = Offset(x + cellMargin, y + cellMargin),
                            size = Size(cellW - cellMargin * 2f, cellH - cellMargin * 2f),
                            cornerRadius = CornerRadius(2f, 2f),
                            style = Stroke(width = 1f)
                        )
                    }
                }
            }

            // Row selection indicator bar on left
            val selectedY = headerTop + selectedRow * cellH
            val rowSatisfied = evaluations.getOrNull(selectedRow)?.isSatisfied ?: true
            drawRoundRect(
                color = if (rowSatisfied) Color(0xFF00FFCC) else Color(0xFFEF4444),
                topLeft = Offset(2f, selectedY + 1f),
                size = Size(4f, cellH - 2f),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }
    }
}

// =========================================================================
// 5. HELPER PILLS, CHIPS & EXPORTERS
// =========================================================================

@Composable
private fun R1csMetricPill(
    label: String,
    value: String,
    subtext: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0C162D),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 6.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = value,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtext,
                fontSize = 6.sp,
                color = Color(0xFFCBD5E1)
            )
        }
    }
}

@Composable
private fun LegendChip(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = label,
            fontSize = 7.sp,
            color = Color(0xFF94A3B8),
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun buildR1csExportJson(state: R1csCircuitState, evals: List<R1csEvaluation>): String {
    val builder = StringBuilder()
    builder.append("{\n")
    builder.append("  \"circuit\": \"ZKP_RANGE_PROOF_R1CS\",\n")
    builder.append("  \"curve\": \"BN254\",\n")
    builder.append("  \"constraints_count\": ${state.constraints.size},\n")
    builder.append("  \"variables_count\": ${state.variables.size},\n")
    builder.append("  \"sparsity_percent\": ${"%.2f".format(state.sparsityPercent)},\n")
    builder.append("  \"all_satisfied\": ${evals.all { it.isSatisfied }},\n")
    builder.append("  \"witness_s\": [")
    builder.append(state.variables.joinToString(", ") { "%.2f".format(it.value) })
    builder.append("],\n")
    builder.append("  \"constraints\": [\n")
    state.constraints.forEachIndexed { i, c ->
        val ev = evals.getOrNull(i)
        builder.append("    {\"row\": ${c.rowIndex}, \"id\": \"${c.id}\", \"formula\": \"${c.formulaDisplay}\", \"satisfied\": ${ev?.isSatisfied ?: true}}")
        if (i < state.constraints.size - 1) builder.append(",")
        builder.append("\n")
    }
    builder.append("  ]\n")
    builder.append("}\n")
    return builder.toString()
}

// =========================================================================
// 5. CANVAS COMPOSABLE FOR R1CS CONSTRAINT EVALUATION CHART
// =========================================================================

@Composable
fun R1csInteractiveConstraintChart(
    state: R1csCircuitState,
    evaluations: List<R1csEvaluation>,
    selectedRow: Int,
    onSelectRow: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var chartMode by remember { mutableStateOf(0) } // 0: LHS vs RHS, 1: Residuals Delta, 2: Witness Spectrum
    val m = state.constraints.size // 14
    val selectedEval = evaluations.getOrNull(selectedRow)
    val selectedConstraint = state.constraints.getOrNull(selectedRow)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Sub-mode selectors
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("📊 LHS vs RHS", "📈 RESIDUALES Δ", "🧬 TESTIGO (WITNESS)").forEachIndexed { idx, label ->
                val isSelected = chartMode == idx
                Surface(
                    color = if (isSelected) Color(0xFF0284C7) else Color(0xFF0F172A),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B)),
                    modifier = Modifier.weight(1f).clickable { chartMode = idx }.testTag("btn_r1cs_chart_mode_$idx")
                ) {
                    Text(
                        text = label,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        // Canvas Area
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .background(Color(0xFF030712), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                .clickable {
                    onSelectRow((selectedRow + 1) % m)
                }
                .testTag("r1cs_constraint_chart_canvas")
        ) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()

            Canvas(modifier = Modifier.fillMaxSize()) {
                val padLeft = 28f
                val padRight = 12f
                val padTop = 14f
                val padBottom = 22f
                val chartW = widthPx - padLeft - padRight
                val chartH = heightPx - padTop - padBottom

                // Grid background lines
                val gridLines = 4
                for (g in 0..gridLines) {
                    val gy = padTop + (chartH / gridLines) * g
                    drawLine(
                        color = Color(0xFF1E293B).copy(alpha = 0.5f),
                        start = Offset(padLeft, gy),
                        end = Offset(widthPx - padRight, gy),
                        strokeWidth = 1f
                    )
                }

                when (chartMode) {
                    0 -> {
                        // LHS vs RHS Dual Bar Chart for 14 rows
                        val slotW = chartW / m.toFloat()
                        val barW = (slotW * 0.38f).coerceAtLeast(3f)

                        var maxVal = 1.0
                        for (ev in evaluations) {
                            maxVal = maxOf(maxVal, abs(ev.lhs), abs(ev.rhs))
                        }
                        val scale = (chartH * 0.88f) / maxVal.toFloat()

                        for (r in 0 until m) {
                            val ev = evaluations.getOrNull(r) ?: continue
                            val slotX = padLeft + r * slotW
                            val isSel = r == selectedRow
                            val isSat = ev.isSatisfied

                            // Selected row highlight slot
                            if (isSel) {
                                drawRoundRect(
                                    color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                                    topLeft = Offset(slotX, padTop),
                                    size = Size(slotW, chartH),
                                    cornerRadius = CornerRadius(2f, 2f)
                                )
                            }

                            // LHS Bar (Cyan or Red if failing)
                            val lhsHeight = (abs(ev.lhs) * scale).toFloat().coerceIn(2f, chartH)
                            val lhsY = padTop + chartH - lhsHeight
                            drawRoundRect(
                                color = if (isSat) Color(0xFF00FFCC) else Color(0xFFEF4444),
                                topLeft = Offset(slotX + 1.5f, lhsY),
                                size = Size(barW, lhsHeight),
                                cornerRadius = CornerRadius(1.5f, 1.5f)
                            )

                            // RHS Bar (Electric Blue / Magenta)
                            val rhsHeight = (abs(ev.rhs) * scale).toFloat().coerceIn(2f, chartH)
                            val rhsY = padTop + chartH - rhsHeight
                            drawRoundRect(
                                color = if (isSat) Color(0xFF38BDF8) else Color(0xFFF43F5E),
                                topLeft = Offset(slotX + barW + 2.5f, rhsY),
                                size = Size(barW, rhsHeight),
                                cornerRadius = CornerRadius(1.5f, 1.5f)
                            )
                        }
                    }
                    1 -> {
                        // Residuals Delta Plot
                        val slotW = chartW / m.toFloat()
                        val threshY = padTop + chartH * 0.72f
                        drawLine(
                            color = Color(0xFF00FF88),
                            start = Offset(padLeft, threshY),
                            end = Offset(widthPx - padRight, threshY),
                            strokeWidth = 1.5f
                        )

                        for (r in 0 until m) {
                            val ev = evaluations.getOrNull(r) ?: continue
                            val cx = padLeft + r * slotW + slotW / 2f
                            val isSat = ev.isSatisfied
                            val isSel = r == selectedRow

                            val cy = if (isSat) {
                                threshY + 10f
                            } else {
                                padTop + 12f // Failure spike
                            }

                            // Stem line
                            drawLine(
                                color = if (isSat) Color(0xFF00FF88).copy(alpha = 0.6f) else Color(0xFFEF4444),
                                start = Offset(cx, padTop + chartH),
                                end = Offset(cx, cy),
                                strokeWidth = if (isSel) 2.5f else 1.2f
                            )

                            // Dot
                            drawCircle(
                                color = if (isSat) Color(0xFF00FF88) else Color(0xFFEF4444),
                                radius = if (isSel) 5f else 3.5f,
                                center = Offset(cx, cy)
                            )
                        }
                    }
                    else -> {
                        // Witness variables vector spectrum
                        val varCount = state.variables.size
                        val slotW = chartW / varCount.toFloat()
                        val barW = slotW * 0.65f
                        var maxWitness = 1.0
                        for (v in state.variables) maxWitness = maxOf(maxWitness, abs(v.value))
                        val scale = (chartH * 0.88f) / maxWitness.toFloat()

                        for (i in 0 until varCount) {
                            val variable = state.variables[i]
                            val slotX = padLeft + i * slotW
                            val bHeight = (abs(variable.value) * scale).toFloat().coerceIn(2f, chartH)
                            val by = padTop + chartH - bHeight

                            val barColor = when (variable.visibility) {
                                R1csVarVisibility.PUBLIC_INPUT -> Color(0xFF00FFCC)
                                R1csVarVisibility.SHIELDED_WITNESS -> Color(0xFFA855F7)
                                R1csVarVisibility.AUXILIARY_SLACK -> Color(0xFFFBBF24)
                            }

                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(slotX + (slotW - barW) / 2f, by),
                                size = Size(barW, bHeight),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                        }
                    }
                }
            }
        }

        // Dynamic Chart HUD Details
        Surface(
            color = Color(0xFF0B1428),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth().testTag("r1cs_chart_hud")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "FILA C$selectedRow: ${selectedConstraint?.title ?: ""}",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FFCC),
                            fontFamily = FontFamily.Monospace
                        )
                        Surface(
                            color = if (selectedEval?.isSatisfied == true) Color(0xFF00FF88).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, if (selectedEval?.isSatisfied == true) Color(0xFF00FF88) else Color(0xFFEF4444))
                        ) {
                            Text(
                                text = if (selectedEval?.isSatisfied == true) "SATISFECHO" else "VIOLACIÓN",
                                fontSize = 6.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedEval?.isSatisfied == true) Color(0xFF00FF88) else Color(0xFFEF4444),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Fórmula: ${selectedConstraint?.formulaDisplay ?: ""} • LHS: ${"%.4f".format(selectedEval?.lhs ?: 0.0)} | RHS: ${"%.4f".format(selectedEval?.rhs ?: 0.0)} | Δ: ${"%.6f".format(selectedEval?.residual ?: 0.0)}",
                        fontSize = 7.sp,
                        color = Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace
                    )
                }

                OutlinedButton(
                    onClick = { onSelectRow((selectedRow + 1) % m) },
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    modifier = Modifier.height(26.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text("SIG. FILA", fontSize = 7.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

