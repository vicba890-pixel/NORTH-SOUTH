package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.game.AuditEventLogExporter
import com.example.ui.game.ConnectivityNodeStatus
import com.example.ui.game.OfficialAuditCertificate
import com.example.ui.game.R1csRangeProofEngine
import com.example.ui.game.SecurityZkpReport
import com.example.ui.game.StressTestReport
import org.json.JSONObject
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
class R1csAndLogExporterTest {

    @Test
    fun `test R1CS circuit satisfies all constraints with valid witness`() {
        val state = R1csRangeProofEngine.buildRangeProofCircuit(
            secretTilt = 3.65,
            isTampered = false
        )

        assertFalse("Circuit must not be tampered by default", state.isTampered)
        assertEquals(14, state.variables.size)
        assertEquals(14, state.constraints.size)

        val witness = state.variables.map { it.value }

        // Evaluate every single constraint in (A * s) o (B * s) = (C * s)
        for (constraint in state.constraints) {
            val eval = constraint.evaluate(witness)
            assertTrue(
                "Constraint row ${constraint.rowIndex} (${constraint.formulaDisplay}) must be satisfied, residual=${eval.residual}",
                eval.isSatisfied
            )
            assertTrue(eval.residual < 1e-4)
        }

        // Verify matrix dimensions
        assertEquals(state.constraints.size, state.matrixA.size)
        assertEquals(state.constraints.size, state.matrixB.size)
        assertEquals(state.constraints.size, state.matrixC.size)
        for (row in state.matrixA) {
            assertEquals(state.variables.size, row.size)
        }
    }

    @Test
    fun `test R1CS circuit catches tampering and detects constraint violation`() {
        val tamperedState = R1csRangeProofEngine.buildRangeProofCircuit(
            secretTilt = 3.65,
            isTampered = true
        )

        assertTrue("Circuit should indicate tampered state", tamperedState.isTampered)
        val witness = tamperedState.variables.map { it.value }

        // Find failed constraints
        val failedConstraints = tamperedState.constraints.map { it to it.evaluate(witness) }
            .filter { !it.second.isSatisfied }

        assertTrue(
            "Tampered witness must produce at least one constraint failure",
            failedConstraints.isNotEmpty()
        )

        // Specifically bit 3 constraint (row 3: b3 * (1 - b3) = 0) must fail because bit 3 was set to 1.40
        val bit3Constraint = tamperedState.constraints.first { it.rowIndex == 3 }
        val bit3Eval = bit3Constraint.evaluate(witness)
        assertFalse("Bit 3 booleanity constraint must fail for 1.40", bit3Eval.isSatisfied)
        assertTrue("Residual must be significant", bit3Eval.residual > 0.1)
    }

    @Test
    fun `test AuditEventLogExporter generates valid structured JSON`() {
        val logs = listOf(
            ">> Suite de Auditoría inicializada [AreIA Enclave v4.2].",
            ">> Fase 1: Stress test 500 hilos completado.",
            ">> Fase 3: R1CS solver verificado. Cero-conocimiento preservado."
        )

        val cert = OfficialAuditCertificate(
            certificateId = "CERT-ZKP-2026-TEST01",
            issuedAtEpochMs = System.currentTimeMillis(),
            formattedDate = "2026-10-22",
            issuerAuthority = "AreIA Quantum Certification Authority",
            overallScorePercent = 99.8f,
            certificationGrade = "A+ (EXCELENCIA SISTÉMICA)",
            merkleRootDigest = "0x7F2A3B4C5D6E",
            digitalSignatureHex = "0x99AA88BB77CC",
            isOfficiallyCertified = true
        )

        val stress = StressTestReport(
            concurrencyLevel = 500,
            totalOperations = 5000,
            successfulOperations = 5000,
            failedOperations = 0,
            throughputOpsPerSec = 1450.0f
        )

        val nodes = listOf(
            ConnectivityNodeStatus("n1", "Serverless Edge", "https://edge", 8, 1, 0f, "gRPC / TLS 1.3", true, true)
        )

        val zkp = SecurityZkpReport(
            proofHash = "0xABC123",
            isWitnessValid = true,
            antiTamperBlocked = true,
            zeroKnowledgeScore = 100.0f
        )

        val jsonString = AuditEventLogExporter.buildStructuredJson(
            logs = logs,
            certificate = cert,
            stressReport = stress,
            connectivityNodes = nodes,
            zkpReport = zkp,
            isR1csSatisfied = true
        )

        assertNotNull(jsonString)
        val json = JSONObject(jsonString)

        assertTrue(json.has("metadata"))
        assertEquals("AreIA Quantum Enclave - System Audit & Certification Suite", json.getJSONObject("metadata").getString("systemTitle"))

        assertTrue(json.has("certificate"))
        assertEquals("CERT-ZKP-2026-TEST01", json.getJSONObject("certificate").getString("certificateId"))

        assertTrue(json.has("stressTelemetry"))
        assertEquals(500, json.getJSONObject("stressTelemetry").getInt("concurrencyLevelThreads"))

        assertTrue(json.has("crossPlatformInterop"))
        val interop = json.getJSONObject("crossPlatformInterop")
        assertEquals("Brands-VMSB© Lattice ZKP Native Architecture", interop.getString("standardName"))
        assertTrue(interop.getBoolean("frictionlessExecution"))
        assertEquals("CLEARED_WORLDWIDE (NIST FIPS 204 Open Standard)", interop.getString("legalComplianceStatus"))

        // Assert EFTA - MERCOSUR Swiss Protocol Coverage in JSON
        assertTrue("JSON must contain EFTA-MERCOSUR Swiss coverage", json.has("eftaMercosurSwissProtocol"))
        val eftaJson = json.getJSONObject("eftaMercosurSwissProtocol")
        assertTrue(eftaJson.getString("treatyTitle").contains("EFTA - MERCOSUR"))
        assertTrue(eftaJson.getString("jurisdictionAnchor").contains("Suiza"))
        assertEquals("CH-IPR-EFTA-MERCOSUR-2026-VMSB-DILITHIUM-001", eftaJson.getString("authorialRegistrationId"))
        assertTrue(eftaJson.getBoolean("isOfficiallyRegistered"))
        assertTrue(eftaJson.getJSONArray("protectedCommercialAreas").length() > 0)

        // Assert MERCOSUR - Singapore e-Registry in JSON
        assertTrue("JSON must contain Singapore e-Registry", json.has("mercosurSingaporeElectronicRegistry"))
        val sgJson = json.getJSONObject("mercosurSingaporeElectronicRegistry")
        assertTrue(sgJson.getString("treatyTitle").contains("MERCOSUR - Singapur"))
        assertEquals("SG-ETA-MLETR-2026-VMSB-890F-99A", sgJson.getString("electronicRegistryId"))
        assertTrue(sgJson.getString("governingJurisdiction").contains("Singapur"))
        assertEquals("DOCUMENTO_ELECTRÓNICO_TRANSFERIBLE_CON_VALIDEZ_JURÍDICA_PLENA", sgJson.getString("legalValidityStatus"))
        assertTrue(sgJson.getBoolean("isOfficiallyCertified"))

        assertTrue(json.has("auditEvents"))
        val events = json.getJSONArray("auditEvents")
        assertEquals(3, events.length())
    }

    @Test
    fun `test AuditEventLogExporter generates structured text and saves to file`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val logs = listOf(
            ">> [TEST] Evento 1",
            ">> [TEST] Evento 2 con certificación"
        )

        val txt = AuditEventLogExporter.buildStructuredText(
            logs = logs,
            certificate = null,
            stressReport = null,
            connectivityNodes = emptyList(),
            zkpReport = null,
            isR1csSatisfied = true
        )

        assertTrue(txt.contains("AREIA QUANTUM ENCLAVE"))
        assertTrue(txt.contains("REGISTRO CRONOLÓGICO DE EVENTOS"))
        assertTrue(txt.contains("ESPECIFICACIÓN CROSS-PLATFORM: iOS + BRANDS-VMSB© LATTICE"))
        assertTrue(txt.contains("COBERTURA AUTORAL Y JURÍDICA: PROTOCOLO EFTA - MERCOSUR"))
        assertTrue(txt.contains("Confederación Suiza"))
        assertTrue(txt.contains("E-REGISTRO ELECTRÓNICO OFICIAL: PROTOCOLO MERCOSUR - SINGAPUR"))
        assertTrue(txt.contains("SG-ETA-MLETR-2026-VMSB-890F-99A"))

        val file = AuditEventLogExporter.exportLogsToFile(
            context = context,
            content = txt,
            isJson = false,
            certificateId = "TEST_CERT"
        )

        assertNotNull(file)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertTrue(file.name.endsWith(".txt"))
    }

    @Test
    fun `test official audit certificate embeds EFTA-MERCOSUR and Singapore e-Registry in Merkle root`() {
        val stress = StressTestReport(
            concurrencyLevel = 250,
            totalOperations = 2500,
            successfulOperations = 2500,
            failedOperations = 0,
            throughputOpsPerSec = 1100f
        )
        val zkp = SecurityZkpReport(
            proofHash = "0x998877AABB",
            isWitnessValid = true,
            antiTamperBlocked = true,
            zeroKnowledgeScore = 100f
        )
        val nodes = com.example.ui.game.SystemAuditEngine.generateDefaultConnectivityNodes()

        val cert = com.example.ui.game.SystemAuditEngine.createOfficialCertificate(
            stressReport = stress,
            connectivityNodes = nodes,
            zkpReport = zkp
        )

        assertNotNull(cert)
        assertTrue(cert.isOfficiallyCertified)
        assertNotNull(cert.eftaMercosurCoverage)
        assertNotNull(cert.singaporeERegistry)

        assertEquals("CH-IPR-EFTA-MERCOSUR-2026-VMSB-DILITHIUM-001", cert.eftaMercosurCoverage.authorialRegistrationId)
        assertTrue(cert.eftaMercosurCoverage.jurisdictionAnchor.contains("Suiza"))
        assertTrue(cert.eftaMercosurCoverage.isOfficiallyRegistered)

        assertEquals("SG-ETA-MLETR-2026-VMSB-890F-99A", cert.singaporeERegistry.electronicRegistryId)
        assertTrue(cert.singaporeERegistry.governingJurisdiction.contains("Singapur"))
        assertTrue(cert.singaporeERegistry.isOfficiallyCertified)

        // Merkle root and digital signature must be non-empty and 66 chars (0x + 64 hex)
        assertTrue(cert.merkleRootDigest.startsWith("0x"))
        assertEquals(66, cert.merkleRootDigest.length)
        assertTrue(cert.digitalSignatureHex.startsWith("0x"))
        assertEquals(66, cert.digitalSignatureHex.length)
    }

    @Test
    fun `test R1CS interactive chart calculations and evaluations across modes`() {
        val circuitState = R1csRangeProofEngine.buildRangeProofCircuit(
            secretTilt = 3.65,
            isTampered = false
        )
        val witness = circuitState.variables.map { it.value }
        val evaluations = circuitState.constraints.map { it.evaluate(witness) }

        // Mode 0: LHS vs RHS evaluation
        assertEquals(14, evaluations.size)
        for (i in evaluations.indices) {
            val eval = evaluations[i]
            assertEquals("Row $i: LHS must equal RHS within tolerance", eval.lhs, eval.rhs, 1e-4)
            assertTrue("Row $i must be satisfied", eval.isSatisfied)
        }

        // Mode 1: Residuals Delta evaluation
        for (eval in evaluations) {
            assertTrue("Residual must be below 1e-4 threshold", eval.residual < 1e-4)
        }

        // Mode 2: Witness variables spectrum
        assertEquals(14, circuitState.variables.size)
        val publicInputs = circuitState.variables.filter { it.visibility == com.example.ui.game.R1csVarVisibility.PUBLIC_INPUT }
        val shieldedWitnesses = circuitState.variables.filter { it.visibility == com.example.ui.game.R1csVarVisibility.SHIELDED_WITNESS }
        val auxiliarySlack = circuitState.variables.filter { it.visibility == com.example.ui.game.R1csVarVisibility.AUXILIARY_SLACK }

        assertTrue("Must have public inputs (e.g. ONE constant, range bound, public commitment)", publicInputs.isNotEmpty())
        assertTrue("Must have shielded witness variables (bits b0..b7, secret tilt)", shieldedWitnesses.isNotEmpty())
        assertTrue("Must have auxiliary slack variables", auxiliarySlack.isNotEmpty())

        // Test with perturbation / tamper
        val tamperedState = R1csRangeProofEngine.buildRangeProofCircuit(
            secretTilt = 3.65,
            isTampered = true
        )
        val tamperedWitness = tamperedState.variables.map { it.value }
        val tamperedEvaluations = tamperedState.constraints.map { it.evaluate(tamperedWitness) }
        val violations = tamperedEvaluations.filter { !it.isSatisfied }
        assertTrue("Tampered circuit must have constraint violations displayed on chart", violations.isNotEmpty())
    }

    @Test
    fun `test D3R1csClusterRadialHtml generates valid 1024 constraints radial structure`() {
        val html = com.example.ui.game.generateD3R1csClusterRadialHtml()

        assertNotNull(html)
        assertTrue("HTML must contain doctype", html.contains("<!DOCTYPE html>"))
        assertTrue("HTML must load D3.js or fallback", html.contains("d3js.org/d3.v7.min.js"))
        assertTrue("HTML must reference 1024 radial cluster", html.contains("1024 Nodos") || html.contains("1024"))
        assertTrue("HTML must include BOOLEANITY quadrant", html.contains("BOOLEANITY"))
        assertTrue("HTML must include RANGE quadrant", html.contains("RANGE"))
        assertTrue("HTML must include ARITHMETIC quadrant", html.contains("ARITHMETIC"))
        assertTrue("HTML must include SCHNORR quadrant", html.contains("SCHNORR"))
        assertTrue("HTML must include fallback renderer for offline resilience", html.contains("renderPureSvgFallback"))
        assertTrue("HTML must support bridge callback", html.contains("D3R1csBridge"))
    }

    @Test
    fun `test D3R1csJsBridge deserializes node correctly`() {
        var clickedNode: com.example.ui.game.R1csConstraint1024Node? = null
        var changedCategory: String? = null

        val bridge = com.example.ui.game.D3R1csJsBridge(
            onConstraintSelected = { clickedNode = it },
            onCategoryFilterChanged = { changedCategory = it }
        )

        val sampleJson = """
            {
                "index": 512,
                "id": "C#0512",
                "name": "Restricción #512",
                "category": "ARITHMETIC",
                "formula": "(A·s) ∘ (B·s) = C·s",
                "lhs": 14.85,
                "rhs": 14.85,
                "residual": 0.0,
                "isSatisfied": true
            }
        """.trimIndent()

        bridge.onNodeClicked(sampleJson)
        assertNotNull(clickedNode)
        assertEquals(512, clickedNode?.index)
        assertEquals("C#0512", clickedNode?.id)
        assertEquals("ARITHMETIC", clickedNode?.category)
        assertTrue(clickedNode?.isSatisfied == true)

        bridge.onFilterCategory("RANGE")
        assertEquals("RANGE", changedCategory)
    }

    @Test
    fun `test SystemAuditLogItem model maintains identity and text`() {
        val item1 = com.example.ui.game.SystemAuditLogItem(id = 1L, text = "Log 1", timestamp = 1000L)
        val item2 = com.example.ui.game.SystemAuditLogItem(id = 2L, text = "Log 2", timestamp = 2000L)

        assertEquals(1L, item1.id)
        assertEquals("Log 1", item1.text)
        assertEquals(1000L, item1.timestamp)
        assertTrue(item1 != item2)
    }
}

