package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.game.PdfCertificateExporter
import com.example.ui.game.SecurityZkpReport
import com.example.ui.game.StressTestReport
import com.example.ui.game.SystemAuditEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SystemAuditAndPdfTest {

    @Test
    fun `test audit engine generates default reports and certification`() {
        val stress = StressTestReport(
            concurrencyLevel = 500,
            totalOperations = 5000,
            successfulOperations = 5000,
            throughputOpsPerSec = 1248.5f
        )
        val nodes = SystemAuditEngine.generateDefaultConnectivityNodes()
        val zkp = SecurityZkpReport()

        assertEquals(6, nodes.size)
        assertTrue(nodes.all { it.isHealthy })

        val cert = SystemAuditEngine.createOfficialCertificate(stress, nodes, zkp)
        assertNotNull(cert.certificateId)
        assertTrue(cert.certificateId.startsWith("CERT-ZKP-2026-"))
        assertTrue(cert.merkleRootDigest.startsWith("0x"))
        assertTrue(cert.digitalSignatureHex.startsWith("0x"))
        assertEquals("A+ (EXCELENCIA SISTÉMICA)", cert.certificationGrade)
    }

    @Test
    fun `test pdf certificate generation creates valid file`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val stress = StressTestReport()
        val nodes = SystemAuditEngine.generateDefaultConnectivityNodes()
        val zkp = SecurityZkpReport()
        val subsystems = SystemAuditEngine.generateDefaultSubsystems()
        val optimizations = SystemAuditEngine.generateDefaultOptimizations()
        val cert = SystemAuditEngine.createOfficialCertificate(stress, nodes, zkp)

        val pdfFile = PdfCertificateExporter.generateCertificatePdf(
            context = context,
            certificate = cert,
            stressReport = stress,
            connectivityNodes = nodes,
            zkpReport = zkp,
            subsystems = subsystems,
            optimizations = optimizations
        )

        assertNotNull(pdfFile)
        assertTrue("PDF file must exist", pdfFile.exists())
        assertTrue("PDF file size must be greater than zero bytes", pdfFile.length() > 0)
        assertTrue("PDF filename must end with .pdf", pdfFile.name.endsWith(".pdf"))
    }
}
