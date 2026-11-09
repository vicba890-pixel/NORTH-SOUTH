package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.ui.game.AntiReaDetector
import com.example.ui.game.DilithiumPostQuantumEngine
import com.example.ui.game.VmsbNativeVirtualMachine
import com.example.ui.game.ZkpDilithiumVmsbEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AntiReaVmsbDilithiumTest {

    @Test
    fun testDilithiumKeyPairGenerationAndSigning() {
        val keyPair = DilithiumPostQuantumEngine.generateKeyPair("vmsb_test_seed")

        assertNotNull(keyPair.publicKey)
        assertNotNull(keyPair.privateKey)
        assertTrue(keyPair.publicKey.keyFingerprint.startsWith("dilithium3:0x"))
        assertEquals(DilithiumPostQuantumEngine.ETA, 4)
        assertEquals(DilithiumPostQuantumEngine.K, 6)
        assertEquals(DilithiumPostQuantumEngine.L, 5)

        val vm = VmsbNativeVirtualMachine()
        val bytecode = vm.getProtectedBytecode()

        // Test signing
        val signature = DilithiumPostQuantumEngine.sign(bytecode, keyPair.privateKey)
        assertNotNull(signature)
        assertTrue(signature.fullSignatureHex.startsWith("0x", ignoreCase = true))
        assertTrue(signature.verifiedQuantumSafe)

        // Test verification with intact bytecode
        val isValid = DilithiumPostQuantumEngine.verify(bytecode, signature, keyPair.publicKey, isBytecodeTampered = false)
        assertTrue("Intact bytecode must pass Dilithium verification", isValid)

        // Test verification with tampered bytecode
        val isTamperedValid = DilithiumPostQuantumEngine.verify(bytecode, signature, keyPair.publicKey, isBytecodeTampered = true)
        assertFalse("Tampered bytecode must be rejected mathematically by Dilithium", isTamperedValid)
    }

    @Test
    fun testVmsbVirtualMachineExecutionNominal() {
        val vm = VmsbNativeVirtualMachine()
        val bytecode = vm.getProtectedBytecode()
        assertTrue("Protected bytecode must not be empty", bytecode.isNotEmpty())

        val disassembled = vm.disassemble(bytecode)
        assertTrue("Disassembly should contain instructions", disassembled.isNotEmpty())
        assertTrue("Disassembly should contain VMOV", disassembled.any { it.mnemonic == "VMOV" })
        assertTrue("Disassembly should contain VCANARY_CHECK", disassembled.any { it.mnemonic == "VCANARY_CHECK" })
        assertTrue("Disassembly should contain VDILITHIUM_AUTH", disassembled.any { it.mnemonic == "VDILITHIUM_AUTH" })

        val execResult = vm.execute(bytecode)
        assertTrue("Executed cycles must be > 0", execResult.cyclesExecuted > 0)
        assertTrue("Canaries must be intact in nominal run", execResult.isCanaryIntact)
        assertTrue("Quantum signature must be verified", execResult.isQuantumVerified)
        assertFalse("Tamper flag should not be raised in nominal run", execResult.isTamperDetected)
        assertFalse("Honeypot should not be active in nominal run", execResult.isHoneypotActive)
        assertTrue(execResult.executionHash.startsWith("0x", ignoreCase = true))
    }

    @Test
    fun testVmsbVirtualMachineTrapOnFridaHook() {
        val vm = VmsbNativeVirtualMachine()
        val bytecode = vm.getProtectedBytecode()

        // Execute with simulated Frida hook
        val hookedResult = vm.execute(
            bytecode = bytecode,
            simulateFridaHook = true,
            simulateBytecodePatch = false,
            simulateDebugger = false
        )

        assertTrue("Simulated hook must trigger tamper detection", hookedResult.isTamperDetected)
        assertFalse("Hooked execution must invalidate canaries", hookedResult.isCanaryIntact)
        assertTrue("Honeypot must be engaged on Frida hook", hookedResult.isHoneypotActive)
        assertTrue("Logs must record hook interception", hookedResult.log.any { it.contains("Frida") || it.contains("CANARY") || it.contains("TAR-PIT") })
    }

    @Test
    fun testPolymorphicBytecodeMutation() {
        val vm = VmsbNativeVirtualMachine()
        val originalBytecode = vm.getProtectedBytecode()

        val mutated1 = vm.polymorphicMutate(originalBytecode, rollingKey = 0x42.toByte())
        val mutated2 = vm.polymorphicMutate(originalBytecode, rollingKey = 0x99.toByte())

        // Mutated bytecode must differ from original in internal instruction bytes
        assertFalse("Mutated bytecode 1 must differ from original", originalBytecode.contentEquals(mutated1))
        assertFalse("Mutated bytecode 2 must differ from mutated 1", mutated1.contentEquals(mutated2))

        // Mutation must disassemble without crashing
        val dis1 = vm.disassemble(mutated1)
        val dis2 = vm.disassemble(mutated2)
        assertTrue(dis1.isNotEmpty())
        assertTrue(dis2.isNotEmpty())

        // Must be signable and verifiable with Dilithium-3
        val keyPair = DilithiumPostQuantumEngine.generateKeyPair("polymorphic_salt")
        val sig1 = DilithiumPostQuantumEngine.sign(mutated1, keyPair.privateKey)
        assertTrue(DilithiumPostQuantumEngine.verify(mutated1, sig1, keyPair.publicKey, isBytecodeTampered = false))
    }

    @Test
    fun testUltraFluencyAsyncExecutionAndBenchmark() = runBlocking {
        val vm = VmsbNativeVirtualMachine()
        val bytecode = vm.getProtectedBytecode()

        // Fast dispatch async execution
        val asyncResult = vm.executeFastDispatchAsync(bytecode)
        assertTrue(asyncResult.cyclesExecuted > 0)
        assertFalse(asyncResult.isTamperDetected)

        // Fluency benchmark
        val bench = vm.benchmarkUltraFluency(iterations = 100)
        assertTrue("Benchmark cycles must be positive", bench.totalCycles >= 100)
        assertTrue("Elapsed micros must be recorded", bench.elapsedMicroseconds > 0)
        assertTrue("Throughput must be positive and measurable", bench.throughputOpsPerSec > 1000.0)
        assertTrue("Average latency must be positive", bench.avgLatencyMicros > 0.0)
        assertNotNull(bench.threadName)
    }

    @Test
    fun testHoneypotTarPitDeception() {
        val vm = VmsbNativeVirtualMachine()
        val bytecode = vm.getProtectedBytecode()

        val tarpitResult = vm.execute(bytecode, isHoneypotTarPitForced = true)
        assertTrue("Tar-Pit forced must activate honeypot", tarpitResult.isHoneypotActive)
        assertTrue("Tar-Pit forced must flag tamper detected", tarpitResult.isTamperDetected)
        assertTrue("Logs must announce tar-pit decoy diversion", tarpitResult.log.any { it.contains("TAR-PIT") })
    }

    @Test
    fun testAntiReaDetectorProbes() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val nominalReport = AntiReaDetector.performFullScan(
            context = context,
            simulateDebugger = false,
            simulateFrida = false,
            simulateRoot = false
        )

        assertEquals("vmsb©", nominalReport.creatorAttribution)
        assertTrue("Nominal score must be high", nominalReport.overallSecurityScore >= 70)
        assertEquals("NOMINAL", nominalReport.defenseStatus)
        assertEquals(6, nominalReport.items.size)

        // Test with simulated attack
        val attackReport = AntiReaDetector.performFullScan(
            context = context,
            simulateDebugger = true,
            simulateFrida = true,
            simulateRoot = true
        )

        assertEquals("UNDER_ATTACK", attackReport.defenseStatus)
        assertTrue("Score under attack must drop", attackReport.overallSecurityScore <= 50)
    }

    @Test
    fun testZkpDilithiumVmsbTriadEngine() {
        // 1. Untampered nominal verification
        val nominal = ZkpDilithiumVmsbEngine.verifyTriad(isTampered = false)
        assertTrue("Nominal VMSB canary must be secure", nominal.isCanarySecure)
        assertTrue("Nominal Dilithium signature must be verified", nominal.isDilithiumVerified)
        assertTrue("Nominal ZKP constraints must be satisfied", nominal.isZkpSatisfied)
        assertTrue("Nominal unified verification must pass", nominal.isUnifiedVerified)
        assertEquals(0.0, nominal.zkpResidualDelta, 1e-6)
        assertEquals("vmsb©", nominal.registrationBadge)
        assertTrue(nominal.zkpProofHash.startsWith("0x"))

        // 2. Tampered verification
        val tampered = ZkpDilithiumVmsbEngine.verifyTriad(isTampered = true)
        assertFalse("Tampered VMSB canary must fail", tampered.isCanarySecure)
        assertFalse("Tampered Dilithium must fail", tampered.isDilithiumVerified)
        assertFalse("Tampered ZKP must be unsatisfied", tampered.isZkpSatisfied)
        assertFalse("Tampered unified verification must fail", tampered.isUnifiedVerified)
        assertTrue("Tampered residual delta must be greater than zero", tampered.zkpResidualDelta > 0.0)
    }
}
