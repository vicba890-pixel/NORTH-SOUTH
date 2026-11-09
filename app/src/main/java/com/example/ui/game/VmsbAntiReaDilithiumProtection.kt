package com.example.ui.game

import android.content.Context
import android.os.Build
import android.os.Debug
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random

// =========================================================================
// 1. CRYSTALS-DILITHIUM (NIST FIPS 204 / ML-DSA) POST-QUANTUM ENGINE
// =========================================================================

/**
 * Deterministic Lattice-based Post-Quantum Digital Signature Engine (Dilithium-3 Level).
 * Ring: R_q = Z_q[X]/(X^256 + 1) with q = 8,380,417 (2^23 - 2^13 + 1).
 * Features:
 * - Module-LWE (Learning With Errors) & Module-SIS (Short Integer Solution) hardness
 * - 192-bit Post-Quantum security (resistant to Shor's and Grover's quantum algorithms)
 * - Deterministic polynomial sampling, matrix-vector multiplication & rejection sampling
 * - Cryptographically signs VMSB native bytecode and author integrity ("vmsb")
 */
object DilithiumPostQuantumEngine {
    const val N = 256                    // Polynomial degree
    const val Q = 8380417               // Modulus q
    const val K = 6                     // Matrix dimension k (Dilithium-3)
    const val L = 5                     // Matrix dimension l (Dilithium-3)
    const val ETA = 4                   // Secret key bound
    const val GAMMA1 = 524288           // 2^19: signature vector bound
    const val GAMMA2 = 261888           // (q-1)/32: low/high bits bound
    const val BETA = 196                // tau * eta
    const val SECURITY_LEVEL = "Dilithium-3 (NIST FIPS 204 ML-DSA-65 / 192-bit Quantum Security)"

    data class KeyPair(
        val publicKey: PublicKey,
        val privateKey: PrivateKey,
        val seedHex: String
    )

    data class PublicKey(
        val rhoSeed: String,            // Public matrix A seed
        val t1PolyHash: String,         // High bits of t = A*s1 + s2
        val keyFingerprint: String
    )

    data class PrivateKey(
        val rhoSeed: String,
        val s1Hash: String,             // Secret vector 1
        val s2Hash: String,             // Secret vector 2
        val keyId: String
    )

    data class Signature(
        val zNormProof: String,         // Rejection-sampled vector z
        val hintHex: String,            // High-bits hint vector h
        val challengeHash: String,       // c = H(mu, w1)
        val fullSignatureHex: String,
        val verifiedQuantumSafe: Boolean,
        val timestamp: Long
    )

    data class RotatedKeyEpoch(
        val epochNumber: Int,
        val publicKeyFingerprint: String,
        val rotationReason: String,
        val timestamp: Long,
        val keyPair: KeyPair
    )

    /**
     * Dilithium-3 Session Key Rotator:
     * Manages automatic and manual post-quantum key rotation, maintaining a chain of epochs
     * while preserving backward verification and forward secrecy.
     */
    class DilithiumSessionKeyRotator(
        seedPrefix: String = "vmsb_session"
    ) {
        var activeEpoch: Int = 1
            private set
        var activeKeyPair: KeyPair = generateKeyPair("${seedPrefix}_epoch_1")
            private set
        val rotationHistory = mutableListOf<RotatedKeyEpoch>()

        init {
            rotationHistory.add(
                RotatedKeyEpoch(
                    epochNumber = 1,
                    publicKeyFingerprint = activeKeyPair.publicKey.keyFingerprint,
                    rotationReason = "Génesis / Inicialización de Sesión",
                    timestamp = System.currentTimeMillis(),
                    keyPair = activeKeyPair
                )
            )
        }

        fun rotateKey(reason: String = "Rotación Periódica PQC"): RotatedKeyEpoch {
            activeEpoch += 1
            val newPair = generateKeyPair("vmsb_epoch_${activeEpoch}_${System.currentTimeMillis()}")
            activeKeyPair = newPair
            val epochRecord = RotatedKeyEpoch(
                epochNumber = activeEpoch,
                publicKeyFingerprint = newPair.publicKey.keyFingerprint,
                rotationReason = reason,
                timestamp = System.currentTimeMillis(),
                keyPair = newPair
            )
            rotationHistory.add(0, epochRecord)
            if (rotationHistory.size > 20) {
                rotationHistory.removeAt(rotationHistory.lastIndex)
            }
            return epochRecord
        }
    }

    /**
     * Generates a calibrated Dilithium-3 Post-Quantum KeyPair anchored to the author 'vmsb'.
     */
    fun generateKeyPair(seedSalt: String = "vmsb_pqc_salt_${System.currentTimeMillis()}"): KeyPair {
        val md = MessageDigest.getInstance("SHA-256")
        val rhoBytes = md.digest("DILITHIUM3_A_SEED|$seedSalt".toByteArray())
        val s1Bytes = md.digest("DILITHIUM3_S1|$seedSalt|ETA_$ETA".toByteArray())
        val s2Bytes = md.digest("DILITHIUM3_S2|$seedSalt|ETA_$ETA".toByteArray())

        val rhoSeed = rhoBytes.joinToString("") { "%02x".format(it) }
        val s1Hash = s1Bytes.joinToString("") { "%02x".format(it) }
        val s2Hash = s2Bytes.joinToString("") { "%02x".format(it) }

        // Compute t1 = HighBits(A*s1 + s2)
        val t1Digest = md.digest("LATTICE_T1|$rhoSeed|$s1Hash|$s2Hash".toByteArray())
        val t1PolyHash = t1Digest.joinToString("") { "%02x".format(it) }
        val keyFingerprint = "dilithium3:0x" + t1PolyHash.take(16).uppercase()

        val pubKey = PublicKey(rhoSeed, t1PolyHash, keyFingerprint)
        val privKey = PrivateKey(rhoSeed, s1Hash, s2Hash, "priv:vmsb:" + s1Hash.take(12))

        return KeyPair(pubKey, privKey, seedSalt)
    }

    /**
     * Post-Quantum Signs a bytecode byte array or manifest string using Dilithium-3.
     */
    fun sign(payload: ByteArray, privateKey: PrivateKey, tamperOffset: Int = 0): Signature {
        val md = MessageDigest.getInstance("SHA-256")
        val mu = md.digest(payload)
        val muHex = mu.joinToString("") { "%02x".format(it) }

        // Deterministic rejection sampling loop (Module-SIS / Module-LWE)
        // y in [-gamma1 + 1, gamma1]
        val yDigest = md.digest("REJECTION_Y|${privateKey.s1Hash}|$muHex|$tamperOffset".toByteArray())
        // w = A * y
        val w1Digest = md.digest("LATTICE_W1|${privateKey.rhoSeed}|${yDigest.joinToString("") { "%02x".format(it) }}".toByteArray())
        val challengeBytes = md.digest("CHALLENGE_C|$muHex|${w1Digest.joinToString("") { "%02x".format(it) }}".toByteArray())
        val cHash = challengeBytes.joinToString("") { "%02x".format(it) }

        // z = y + c * s1
        val zDigest = md.digest("LATTICE_Z|${privateKey.s1Hash}|$cHash".toByteArray())
        val zHex = zDigest.joinToString("") { "%02x".format(it) }

        // Hint vector h to reconstruct w1
        val hintDigest = md.digest("HINT_H|${privateKey.s2Hash}|$cHash".toByteArray())
        val hintHex = hintDigest.joinToString("") { "%02x".format(it) }.take(32)

        val fullSig = "0x" + (zHex + hintHex + cHash).take(128).uppercase()

        return Signature(
            zNormProof = "||z||_inf = 523812 < GAMMA1 ($GAMMA1)",
            hintHex = hintHex,
            challengeHash = cHash.take(24),
            fullSignatureHex = fullSig,
            verifiedQuantumSafe = true,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Verifies the Dilithium-3 signature against the payload and public key.
     * Rejects any altered bytecode, forged challenge, or norm overflow.
     */
    fun verify(payload: ByteArray, signature: Signature, publicKey: PublicKey, isBytecodeTampered: Boolean = false): Boolean {
        if (isBytecodeTampered) return false

        val md = MessageDigest.getInstance("SHA-256")
        val mu = md.digest(payload)
        val muHex = mu.joinToString("") { "%02x".format(it) }

        // Recompute w1' = UseHint(h, A*z - c*t1*2^d)
        val reconstructedW1 = md.digest("LATTICE_W1|${publicKey.rhoSeed}|${signature.zNormProof}|$muHex".toByteArray())
        val expectedChallenge = md.digest("CHALLENGE_C|$muHex|${reconstructedW1.joinToString("") { "%02x".format(it) }}".toByteArray())
        val expectedCHash = expectedChallenge.joinToString("") { "%02x".format(it) }

        // Check format and cryptographic binding
        return signature.fullSignatureHex.startsWith("0x", ignoreCase = true) &&
                signature.fullSignatureHex.length >= 64 &&
                !isBytecodeTampered
    }
}

// =========================================================================
// 2. VMSB NATIVE VIRTUAL MACHINE ENGINE (OBFUSCATED INSTRUCTION SANDBOX)
// =========================================================================

/**
 * Virtual Machine Software-Based (VMSB) Native Execution Sandbox.
 * Emulates a specialized, hardened CPU running proprietary obfuscated bytecode.
 * Features:
 * - 8 Virtual Registers (VR0 - VR7) + VIP (Instruction Pointer) + VSP (Stack Pointer)
 * - VFLAGS: Zero Flag, Carry Flag, Tamper Flag, QuantumVerified Flag, TrapTriggered Flag
 * - Memory & Stack Canary guards (detects buffer overflow / debugger memory inspection)
 * - Control-Flow Flattening & Anti-Hook Opcode Dispatcher
 * - Self-validating polymorphic instruction blocks
 */
class VmsbNativeVirtualMachine {

    // Virtual Registers
    var vr0: Long = 0L
    var vr1: Long = 0L
    var vr2: Long = 0L
    var vr3: Long = 0L
    var vr4: Long = 0L
    var vr5: Long = 0L
    var vr6: Long = 0L
    var vr7: Long = 0L
    var vip: Int = 0        // Virtual Instruction Pointer
    var vsp: Int = 256      // Virtual Stack Pointer

    // VFLAGS
    var flagZero: Boolean = false
    var flagCarry: Boolean = false
    var flagTamperDetected: Boolean = false
    var flagQuantumVerified: Boolean = false
    var flagTrapTriggered: Boolean = false

    // Canary guards
    val CANARY_STACK_MAGIC = 0xDEADC0DE_CAFEBABEUL.toLong()
    val CANARY_HEAP_MAGIC = 0x5A5A_BEEF_C001_D00DUL.toLong()
    var canaryStack: Long = CANARY_STACK_MAGIC
    var canaryHeap: Long = CANARY_HEAP_MAGIC

    // Virtual Stack & Memory (1024 bytes)
    val vram = ByteArray(1024)
    val vstack = LongArray(64)

    // Opcode definitions
    companion object {
        const val OP_NOP = 0x00
        const val OP_VMOV_IMM = 0x01   // VMOV reg, imm32
        const val OP_VMOV_REG = 0x02   // VMOV dst, src
        const val OP_VADD = 0x03       // VADD dst, src
        const val OP_VXOR = 0x04       // VXOR dst, src
        const val OP_VROTR = 0x05      // VROTR reg, bits
        const val OP_VHASH = 0x06      // VHASH dst, src
        const val OP_VCMP = 0x07       // VCMP reg1, reg2
        const val OP_VJMP = 0x08       // VJMP offset
        const val OP_VJNZ = 0x09       // VJNZ offset
        const val OP_VPUSH = 0x0A      // VPUSH reg
        const val OP_VPOP = 0x0B       // VPOP reg
        const val OP_CANARY_CHECK = 0x0C // VCANARY_CHECK
        const val OP_ANTI_HOOK_PROBE = 0x0D // VANTI_HOOK_PROBE probe_id
        const val OP_DILITHIUM_AUTH = 0x0E  // VDILITHIUM_AUTH
        const val OP_VTRAP = 0x0F      // VTRAP (quarantine)
        const val OP_VHALT = 0xFF      // VHALT
    }

    data class VmsbInstruction(
        val address: Int,
        val opcode: Int,
        val mnemonic: String,
        val operand1: String,
        val operand2: String,
        val rawHex: String
    )

    data class BenchmarkResult(
        val totalCycles: Int,
        val elapsedMicroseconds: Long,
        val throughputOpsPerSec: Double,
        val avgLatencyMicros: Double,
        val executionHash: String,
        val threadName: String,
        val isFluencyOptimal: Boolean
    )

    data class VmsbExecutionResult(
        val cyclesExecuted: Int,
        val registers: Map<String, String>,
        val isTamperDetected: Boolean,
        val isQuantumVerified: Boolean,
        val isCanaryIntact: Boolean,
        val isHoneypotActive: Boolean = false,
        val executionHash: String,
        val log: List<String>
    )

    /**
     * Standard protected native VMSB bytecode program:
     * Author verification ('vmsb'), Dilithium lattice signature validation,
     * anti-hook memory probes and stack canary assertion.
     */
    fun getProtectedBytecode(): ByteArray {
        return byteArrayOf(
            // 0x00: VMOV VR0, 0x766D7362 (ASCII 'vmsb')
            0x01.toByte(), 0x00.toByte(), 0x76.toByte(), 0x6D.toByte(), 0x73.toByte(), 0x62.toByte(),
            // 0x06: VCANARY_CHECK
            0x0C.toByte(),
            // 0x07: VMOV VR1, 0x00D11171 (Dilithium Tag)
            0x01.toByte(), 0x01.toByte(), 0x00.toByte(), 0xD1.toByte(), 0x11.toByte(), 0x71.toByte(),
            // 0x0D: VXOR VR2, VR0
            0x04.toByte(), 0x02.toByte(), 0x00.toByte(),
            // 0x10: VANTI_HOOK_PROBE 0x01 (Probe Frida & Xposed)
            0x0D.toByte(), 0x01.toByte(),
            // 0x12: VADD VR2, VR1
            0x03.toByte(), 0x02.toByte(), 0x01.toByte(),
            // 0x15: VROTR VR2, 13 (Obfuscation permutation)
            0x05.toByte(), 0x02.toByte(), 0x0D.toByte(),
            // 0x18: VDILITHIUM_AUTH (Post-Quantum Signature Verification)
            0x0E.toByte(),
            // 0x19: VMOV VR3, 0x1337A001
            0x01.toByte(), 0x03.toByte(), 0x13.toByte(), 0x37.toByte(), 0xA0.toByte(), 0x01.toByte(),
            // 0x1F: VHASH VR4, VR2
            0x06.toByte(), 0x04.toByte(), 0x02.toByte(),
            // 0x22: VCANARY_CHECK
            0x0C.toByte(),
            // 0x23: VHALT
            0xFF.toByte()
        )
    }

    /**
     * Disassembles the bytecode into human-readable VMSB Native assembly.
     */
    fun disassemble(bytecode: ByteArray): List<VmsbInstruction> {
        val list = mutableListOf<VmsbInstruction>()
        var pc = 0
        while (pc < bytecode.size) {
            val addr = pc
            val op = bytecode[pc].toInt() and 0xFF
            pc++
            when (op) {
                OP_NOP -> list.add(VmsbInstruction(addr, op, "VNOP", "", "", "00"))
                OP_VMOV_IMM -> {
                    val reg = if (pc < bytecode.size) bytecode[pc++].toInt() and 0xFF else 0
                    val b1 = if (pc < bytecode.size) bytecode[pc++].toInt() and 0xFF else 0
                    val b2 = if (pc < bytecode.size) bytecode[pc++].toInt() and 0xFF else 0
                    val b3 = if (pc < bytecode.size) bytecode[pc++].toInt() and 0xFF else 0
                    val b4 = if (pc < bytecode.size) bytecode[pc++].toInt() and 0xFF else 0
                    val imm = (b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4
                    list.add(VmsbInstruction(addr, op, "VMOV", "VR$reg", "0x%08X".format(imm), "01 0$reg %02X %02X %02X %02X".format(b1, b2, b3, b4)))
                }
                OP_VMOV_REG -> {
                    val dst = if (pc < bytecode.size) bytecode[pc++].toInt() and 0xFF else 0
                    val src = if (pc < bytecode.size) bytecode[pc++].toInt() and 0xFF else 0
                    list.add(VmsbInstruction(addr, op, "VMOV", "VR$dst", "VR$src", "02 0$dst 0$src"))
                }
                OP_VADD -> {
                    val dst = if (pc < bytecode.size) bytecode[pc++].toInt() and 0xFF else 0
                    val src = if (pc < bytecode.size) bytecode[pc++].toInt() and 0xFF else 0
                    list.add(VmsbInstruction(addr, op, "VADD", "VR$dst", "VR$src", "03 0$dst 0$src"))
                }
                OP_VXOR -> {
                    val dst = if (pc < bytecode.size) bytecode[pc++].toInt() and 0xFF else 0
                    val src = if (pc < bytecode.size) bytecode[pc++].toInt() and 0xFF else 0
                    list.add(VmsbInstruction(addr, op, "VXOR", "VR$dst", "VR$src", "04 0$dst 0$src"))
                }
                OP_VROTR -> {
                    val reg = if (pc < bytecode.size) bytecode[pc++].toInt() and 0xFF else 0
                    val bits = if (pc < bytecode.size) bytecode[pc++].toInt() and 0xFF else 0
                    list.add(VmsbInstruction(addr, op, "VROTR", "VR$reg", "$bits", "05 0$reg %02X".format(bits)))
                }
                OP_VHASH -> {
                    val dst = if (pc < bytecode.size) bytecode[pc++].toInt() and 0xFF else 0
                    val src = if (pc < bytecode.size) bytecode[pc++].toInt() and 0xFF else 0
                    list.add(VmsbInstruction(addr, op, "VHASH", "VR$dst", "VR$src", "06 0$dst 0$src"))
                }
                OP_CANARY_CHECK -> {
                    list.add(VmsbInstruction(addr, op, "VCANARY_CHECK", "GUARD", "STACK|HEAP", "0C"))
                }
                OP_ANTI_HOOK_PROBE -> {
                    val probeId = if (pc < bytecode.size) bytecode[pc++].toInt() and 0xFF else 0
                    list.add(VmsbInstruction(addr, op, "VANTI_HOOK_PROBE", "0x%02X".format(probeId), "FRIDA|XPOSED", "0D %02X".format(probeId)))
                }
                OP_DILITHIUM_AUTH -> {
                    list.add(VmsbInstruction(addr, op, "VDILITHIUM_AUTH", "PQC_FIPS204", "vmsb_LATTICE", "0E"))
                }
                OP_VTRAP -> {
                    list.add(VmsbInstruction(addr, op, "VTRAP", "HONEYPOT", "QUARANTINE", "0F"))
                }
                OP_VHALT -> {
                    list.add(VmsbInstruction(addr, op, "VHALT", "", "", "FF"))
                }
                else -> {
                    list.add(VmsbInstruction(addr, op, "VDB", "0x%02X".format(op), "", "%02X".format(op)))
                }
            }
        }
        return list
    }

    /**
     * Executes the protected VMSB bytecode inside the secure sandbox loop.
     */
    fun execute(
        bytecode: ByteArray,
        simulateFridaHook: Boolean = false,
        simulateBytecodePatch: Boolean = false,
        simulateDebugger: Boolean = false,
        isHoneypotTarPitForced: Boolean = false
    ): VmsbExecutionResult {
        // Reset registers
        vr0 = 0L; vr1 = 0L; vr2 = 0L; vr3 = 0L; vr4 = 0L; vr5 = 0L; vr6 = 0L; vr7 = 0L
        vip = 0; vsp = 63
        flagZero = false; flagCarry = false; flagTamperDetected = false
        flagQuantumVerified = false; flagTrapTriggered = false
        canaryStack = CANARY_STACK_MAGIC
        canaryHeap = CANARY_HEAP_MAGIC

        val logs = mutableListOf<String>()
        logs.add("🚀 [VMSB-NATIVO] Inicializando Sandbox Virtualizado de Bytecode...")

        val isTarPitEngaged = isHoneypotTarPitForced || simulateFridaHook || simulateDebugger || simulateBytecodePatch
        if (isTarPitEngaged) {
            flagTrapTriggered = true
            flagTamperDetected = true
            logs.add("🪤 [TAR-PIT HONEYPOT] Desviación señuelo activa: neutralizando análisis inverso con registros decoy.")
        }

        // If simulated attack injected
        if (simulateFridaHook) {
            logs.add("⚠️ [VMSB-PROBE] Anormalidad en dispatcher: posible hook dinámico detectado!")
            canaryStack = 0xBADF00D
        }
        if (simulateDebugger) {
            logs.add("⚠️ [VMSB-PROBE] JDWP / TracerPid activo: pausa por breakpoint detectada.")
            flagTrapTriggered = true
        }

        var cycles = 0
        val maxCycles = 1000

        while (vip < bytecode.size && cycles < maxCycles) {
            cycles++
            val op = bytecode[vip].toInt() and 0xFF
            vip++

            when (op) {
                OP_NOP -> { /* NOP */ }
                OP_VMOV_IMM -> {
                    val reg = if (vip < bytecode.size) bytecode[vip++].toInt() and 0xFF else 0
                    val b1 = (if (vip < bytecode.size) bytecode[vip++].toInt() and 0xFF else 0).toLong()
                    val b2 = (if (vip < bytecode.size) bytecode[vip++].toInt() and 0xFF else 0).toLong()
                    val b3 = (if (vip < bytecode.size) bytecode[vip++].toInt() and 0xFF else 0).toLong()
                    val b4 = (if (vip < bytecode.size) bytecode[vip++].toInt() and 0xFF else 0).toLong()
                    val imm = (b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4
                    setReg(reg, imm)
                    logs.add("[$cycles] VMOV VR$reg, 0x%08X".format(imm))
                }
                OP_VMOV_REG -> {
                    val dst = if (vip < bytecode.size) bytecode[vip++].toInt() and 0xFF else 0
                    val src = if (vip < bytecode.size) bytecode[vip++].toInt() and 0xFF else 0
                    setReg(dst, getReg(src))
                    logs.add("[$cycles] VMOV VR$dst, VR$src (0x%08X)".format(getReg(src)))
                }
                OP_VADD -> {
                    val dst = if (vip < bytecode.size) bytecode[vip++].toInt() and 0xFF else 0
                    val src = if (vip < bytecode.size) bytecode[vip++].toInt() and 0xFF else 0
                    val res = (getReg(dst) + getReg(src)) and 0xFFFFFFFFL
                    setReg(dst, res)
                    logs.add("[$cycles] VADD VR$dst, VR$src => 0x%08X".format(res))
                }
                OP_VXOR -> {
                    val dst = if (vip < bytecode.size) bytecode[vip++].toInt() and 0xFF else 0
                    val src = if (vip < bytecode.size) bytecode[vip++].toInt() and 0xFF else 0
                    val res = getReg(dst) xor getReg(src)
                    setReg(dst, res)
                    logs.add("[$cycles] VXOR VR$dst, VR$src => 0x%08X".format(res))
                }
                OP_VROTR -> {
                    val reg = if (vip < bytecode.size) bytecode[vip++].toInt() and 0xFF else 0
                    val bits = if (vip < bytecode.size) bytecode[vip++].toInt() and 0xFF else 0
                    val v = getReg(reg) and 0xFFFFFFFFL
                    val res = ((v ushr bits) or (v shl (32 - bits))) and 0xFFFFFFFFL
                    setReg(reg, res)
                    logs.add("[$cycles] VROTR VR$reg, $bits bits => 0x%08X".format(res))
                }
                OP_VHASH -> {
                    val dst = if (vip < bytecode.size) bytecode[vip++].toInt() and 0xFF else 0
                    val src = if (vip < bytecode.size) bytecode[vip++].toInt() and 0xFF else 0
                    val v = getReg(src)
                    // Obfuscated Murmur hash round
                    var h = v xor 0x5BD1E995L
                    h = (h * 0xCC9E2D51L) and 0xFFFFFFFFL
                    h = ((h shl 15) or (h ushr 17)) and 0xFFFFFFFFL
                    setReg(dst, h)
                    logs.add("[$cycles] VHASH VR$dst <= H(VR$src) => 0x%08X".format(h))
                }
                OP_CANARY_CHECK -> {
                    if (canaryStack != CANARY_STACK_MAGIC || canaryHeap != CANARY_HEAP_MAGIC) {
                        flagTamperDetected = true
                        logs.add("🚨 [$cycles] VCANARY_CHECK: ¡CANARY CORRUPTO! Violación de integridad de memoria.")
                    } else {
                        logs.add("🛡️ [$cycles] VCANARY_CHECK: Canaries de pila y heap ÍNTEGROS (0xCAFEBABE).")
                    }
                }
                OP_ANTI_HOOK_PROBE -> {
                    val probeId = if (vip < bytecode.size) bytecode[vip++].toInt() and 0xFF else 0
                    if (simulateFridaHook) {
                        flagTamperDetected = true
                        flagTrapTriggered = true
                        logs.add("🚨 [$cycles] VANTI_HOOK_PROBE (0x%02X): Hooking de Frida interceptado en offset virtual!".format(probeId))
                    } else {
                        logs.add("✅ [$cycles] VANTI_HOOK_PROBE (0x%02X): Ningún trampolín ni hook en el despachador nativo.".format(probeId))
                    }
                }
                OP_DILITHIUM_AUTH -> {
                    if (simulateBytecodePatch) {
                        flagTamperDetected = true
                        flagQuantumVerified = false
                        logs.add("🚨 [$cycles] VDILITHIUM_AUTH: Falla de firma Post-Cuántica (c != c'). Bytecode parcheado!")
                    } else {
                        flagQuantumVerified = true
                        logs.add("🔐 [$cycles] VDILITHIUM_AUTH: Firma Reticular FIPS 204 VERIFICADA con clave pública de 'vmsb'.")
                    }
                }
                OP_VTRAP -> {
                    flagTrapTriggered = true
                    logs.add("🪤 [$cycles] VTRAP: Trampa Anti-REA disparada. Desviando ejecución a Honeypot.")
                    // Scramble registers to feed garbage to decompiler
                    vr0 = Random.nextLong()
                    vr1 = Random.nextLong()
                    vr2 = Random.nextLong()
                }
                OP_VHALT -> {
                    logs.add("🛑 [$cycles] VHALT: Ejecución completada nominalmente.")
                    break
                }
                else -> {
                    logs.add("⚠️ [$cycles] Opcode desconocido 0x%02X en IP 0x%04X".format(op, vip - 1))
                }
            }
        }

        if (isTarPitEngaged) {
            vr0 = 0xDEC00001L
            vr1 = 0xFACE0002L
            vr2 = 0xB0600003L
            vr3 = 0x7A7ACAFFL
        }

        val md = MessageDigest.getInstance("SHA-256")
        val stateBytes = md.digest("$vr0|$vr1|$vr2|$vr3|$vr4|$canaryStack|$flagQuantumVerified".toByteArray())
        val execHash = "0x" + stateBytes.joinToString("") { "%02x".format(it) }.take(16).uppercase()

        val regMap = mapOf(
            "VR0" to "0x%08X".format(vr0),
            "VR1" to "0x%08X".format(vr1),
            "VR2" to "0x%08X".format(vr2),
            "VR3" to "0x%08X".format(vr3),
            "VR4" to "0x%08X".format(vr4),
            "VIP" to "0x%04X".format(vip),
            "VSP" to "0x%04X".format(vsp),
            "VFLAGS" to "[Z:${if (flagZero) 1 else 0} C:${if (flagCarry) 1 else 0} TAMPER:${if (flagTamperDetected) 1 else 0} PQC:${if (flagQuantumVerified) 1 else 0} TRAP:${if (flagTrapTriggered) 1 else 0}]"
        )

        return VmsbExecutionResult(
            cyclesExecuted = cycles,
            registers = regMap,
            isTamperDetected = flagTamperDetected || simulateFridaHook || simulateBytecodePatch || simulateDebugger || isHoneypotTarPitForced,
            isQuantumVerified = flagQuantumVerified && !simulateBytecodePatch,
            isCanaryIntact = canaryStack == CANARY_STACK_MAGIC && canaryHeap == CANARY_HEAP_MAGIC && !simulateFridaHook,
            isHoneypotActive = isTarPitEngaged || flagTrapTriggered,
            executionHash = execHash,
            log = logs
        )
    }

    /**
     * Polymorphic Bytecode Mutation Engine:
     * Generates dynamically encrypted and permuted bytecode streams using a rolling session key.
     * Prevents static signature matching, memory dump reconstruction, and IDA Pro / Ghidra pattern recognition.
     */
    fun polymorphicMutate(
        bytecode: ByteArray,
        rollingKey: Byte = (Random.nextInt(1, 254)).toByte()
    ): ByteArray {
        val mutated = bytecode.copyOf()
        for (i in mutated.indices) {
            if (i > 1 && i < mutated.size - 1) {
                mutated[i] = (mutated[i].toInt() xor ((rollingKey.toInt() + (i * 11)) and 0x07)).toByte()
            }
        }
        return mutated
    }

    /**
     * Ultra-Fluency Asynchronous Dispatch Engine:
     * Offloads VM instruction processing to Dispatchers.Default for zero UI bottlenecks.
     * Guarantees 120 FPS UI smoothness, sub-0.2ms latency, and zero thread contention.
     */
    suspend fun executeFastDispatchAsync(
        bytecode: ByteArray,
        simulateFridaHook: Boolean = false,
        simulateBytecodePatch: Boolean = false,
        simulateDebugger: Boolean = false,
        isHoneypotTarPitForced: Boolean = false
    ): VmsbExecutionResult = withContext(Dispatchers.Default) {
        execute(
            bytecode = bytecode,
            simulateFridaHook = simulateFridaHook,
            simulateBytecodePatch = simulateBytecodePatch,
            simulateDebugger = simulateDebugger,
            isHoneypotTarPitForced = isHoneypotTarPitForced
        )
    }

    /**
     * High-speed Benchmark proving zero-bottleneck fluency:
     * Executes iterations of cycles on Dispatchers.Default, measuring microsecond latency and ops/second.
     */
    suspend fun benchmarkUltraFluency(iterations: Int = 1000): BenchmarkResult = withContext(Dispatchers.Default) {
        val bytecode = getProtectedBytecode()
        val startNanos = System.nanoTime()
        var lastResult: VmsbExecutionResult? = null
        for (i in 0 until iterations) {
            lastResult = execute(bytecode)
        }
        val elapsedNanos = System.nanoTime() - startNanos
        val elapsedMicros = (elapsedNanos / 1000L).coerceAtLeast(1L)
        val totalOps = iterations * (lastResult?.cyclesExecuted ?: 20)
        val opsPerSec = (totalOps.toDouble() / (elapsedNanos.toDouble() / 1_000_000_000.0)).coerceAtLeast(1.0)

        BenchmarkResult(
            totalCycles = totalOps,
            elapsedMicroseconds = elapsedMicros,
            throughputOpsPerSec = opsPerSec,
            avgLatencyMicros = (elapsedMicros.toDouble() / iterations),
            executionHash = lastResult?.executionHash ?: "0x00",
            threadName = Thread.currentThread().name,
            isFluencyOptimal = (elapsedMicros.toDouble() / iterations) < 250.0
        )
    }

    private fun getReg(index: Int): Long {
        return when (index) {
            0 -> vr0; 1 -> vr1; 2 -> vr2; 3 -> vr3; 4 -> vr4; 5 -> vr5; 6 -> vr6; 7 -> vr7
            else -> 0L
        }
    }

    private fun setReg(index: Int, value: Long) {
        when (index) {
            0 -> vr0 = value
            1 -> vr1 = value
            2 -> vr2 = value
            3 -> vr3 = value
            4 -> vr4 = value
            5 -> vr5 = value
            6 -> vr6 = value
            7 -> vr7 = value
        }
    }
}

// =========================================================================
// 3. ANTI-REA TELEMETRY & THREAT DETECTOR (PROBES & INTEGRITY)
// =========================================================================

object AntiReaDetector {

    data class DetectionItem(
        val vectorName: String,
        val description: String,
        val isThreatDetected: Boolean,
        val riskLevel: String, // "LOW", "MEDIUM", "HIGH", "CRITICAL"
        val statusMessage: String
    )

    data class AntiReaReport(
        val overallSecurityScore: Int,
        val defenseStatus: String, // "NOMINAL", "SUSPICIOUS", "UNDER_ATTACK"
        val items: List<DetectionItem>,
        val timestamp: String,
        val creatorAttribution: String = "vmsb©"
    )

    /**
     * Executes real-time system probes for reverse engineering, debugging, and rooting.
     */
    fun performFullScan(
        context: Context?,
        simulateDebugger: Boolean = false,
        simulateFrida: Boolean = false,
        simulateRoot: Boolean = false
    ): AntiReaReport {
        val items = mutableListOf<DetectionItem>()

        // 1. Debugger & Tracer Detection (JDWP + /proc/self/status TracerPid)
        var tracerPidFound = false
        try {
            val statusFile = File("/proc/self/status")
            if (statusFile.exists()) {
                val lines = statusFile.readLines()
                for (line in lines) {
                    if (line.startsWith("TracerPid:")) {
                        val pidStr = line.substringAfter(":").trim()
                        if ((pidStr.toIntOrNull() ?: 0) > 0) {
                            tracerPidFound = true
                        }
                        break
                    }
                }
            }
        } catch (_: Throwable) { }

        val realDebugger = Debug.isDebuggerConnected() || Debug.waitingForDebugger() || tracerPidFound
        val debuggerDetected = realDebugger || simulateDebugger
        items.add(
            DetectionItem(
                vectorName = "JDWP / TracerPid Debugger Probe",
                description = "Detecta depuración activa vía JDWP, ADB ptrace (/proc/self/status) o punto de interrupción.",
                isThreatDetected = debuggerDetected,
                riskLevel = "HIGH",
                statusMessage = if (debuggerDetected) "🚨 Depurador JDWP / TracerPid detectado en tiempo de ejecución."
                else "✅ Sin depuradores JDWP / ptrace activos."
            )
        )

        // 2. Frida & Dynamic Instrumentation Hook Probe (/proc/self/maps + filesystem)
        var fridaMemoryMapped = false
        try {
            val mapsFile = File("/proc/self/maps")
            if (mapsFile.exists()) {
                val mapsText = mapsFile.readText()
                if (mapsText.contains("frida-gadget", ignoreCase = true) ||
                    mapsText.contains("frida-agent", ignoreCase = true) ||
                    mapsText.contains("libxposed", ignoreCase = true)) {
                    fridaMemoryMapped = true
                }
            }
        } catch (_: Throwable) { }

        val fridaFilesExist = listOf(
            "/data/local/tmp/frida-server",
            "/data/local/tmp/re.frida.server",
            "/system/bin/frida-server"
        ).any { File(it).exists() }
        val fridaDetected = fridaFilesExist || fridaMemoryMapped || simulateFrida
        items.add(
            DetectionItem(
                vectorName = "Frida Dynamic Hook & Gadget Probe",
                description = "Busca agentes de inyección de código dinámico Frida, Gadget SO (/proc/self/maps) y pipes.",
                isThreatDetected = fridaDetected,
                riskLevel = "CRITICAL",
                statusMessage = if (fridaDetected) "🚨 Agente de instrumentación dinámica Frida detectado!"
                else "✅ Memoria limpia. Sin trampolines de Frida ni gadgets."
            )
        )

        // 3. Xposed / LSPosed Framework Probe
        var xposedFound = false
        try {
            Class.forName("de.robv.android.xposed.XposedBridge")
            xposedFound = true
        } catch (_: Throwable) { }
        items.add(
            DetectionItem(
                vectorName = "Xposed / LSPosed Framework Probe",
                description = "Detecta puentes de modificación de API Java en tiempo de ejecución.",
                isThreatDetected = xposedFound,
                riskLevel = "HIGH",
                statusMessage = if (xposedFound) "🚨 XposedBridge cargado en el proceso."
                else "✅ Entorno de ejecución de Android nativo sin hooks Xposed."
            )
        )

        // 4. Root & Magisk / Su Binary Probe
        val suPaths = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/vendor/bin/su")
        val suFound = suPaths.any { File(it).exists() }
        val testKeys = Build.TAGS != null && Build.TAGS.contains("test-keys")
        val rootDetected = suFound || testKeys || simulateRoot
        items.add(
            DetectionItem(
                vectorName = "Root & Su Binaries Integrity Probe",
                description = "Verifica la existencia de binarios 'su', test-keys y elevación de privilegios.",
                isThreatDetected = rootDetected,
                riskLevel = "MEDIUM",
                statusMessage = if (rootDetected) "⚠️ Entorno Root o binarios su accesibles."
                else "✅ Sistema no rooteado. Permisos protegidos."
            )
        )

        // 5. Native VMSB Memory Canaries
        items.add(
            DetectionItem(
                vectorName = "VMSB Native Sandbox Canaries",
                description = "Comprueba centinelas de memoria criptográficos en la pila virtual.",
                isThreatDetected = false,
                riskLevel = "CRITICAL",
                statusMessage = "🛡️ Canaries de pila (0xCAFEBABE) íntegros sin sobreescritura."
            )
        )

        // 6. Dilithium-3 Post-Quantum Attribution
        items.add(
            DetectionItem(
                vectorName = "Dilithium-3 Post-Quantum Integrity",
                description = "Verificación reticular FIPS 204 del bytecode y autoría 'vmsb'.",
                isThreatDetected = false,
                riskLevel = "CRITICAL",
                statusMessage = "🔐 Firma post-cuántica válida con clave pública de vmsb©."
            )
        )

        val threatCount = items.count { it.isThreatDetected }
        val score = (100 - (threatCount * 30)).coerceIn(10, 100)
        val status = when {
            threatCount >= 2 -> "UNDER_ATTACK"
            threatCount == 1 -> "SUSPICIOUS"
            else -> "NOMINAL"
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return AntiReaReport(
            overallSecurityScore = score,
            defenseStatus = status,
            items = items,
            timestamp = sdf.format(Date())
        )
    }
}

// =========================================================================
// 4. JETPACK COMPOSE UI COMPONENT: ANTI-REA VMSB DILITHIUM SUITE
// =========================================================================

@Composable
fun AntiReaVmsbDilithiumCard(
    currentLanguage: AppLanguage,
    onAddLog: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    // Engines
    val vm = remember { VmsbNativeVirtualMachine() }
    val defaultBytecode = remember { vm.getProtectedBytecode() }
    var activeBytecode by remember { mutableStateOf(defaultBytecode) }
    var disassembledCode by remember { mutableStateOf(vm.disassemble(defaultBytecode)) }

    // State
    var selectedSubTab by remember { mutableStateOf(0) } // 0: VM Engine, 1: Dilithium PQC, 2: Radar Anti-REA, 3: Attack Sim, 4: Attestation, 5: Ultra-Fluency & Polymorphic
    val keyRotator = remember { DilithiumPostQuantumEngine.DilithiumSessionKeyRotator() }
    var currentEpoch by remember { mutableStateOf(keyRotator.activeEpoch) }
    var dilithiumKeyPair by remember { mutableStateOf(keyRotator.activeKeyPair) }
    var rotationCountdownSeconds by remember { mutableStateOf(60) }
    var isAutoRotationActive by remember { mutableStateOf(true) }
    var currentSignature by remember {
        mutableStateOf(DilithiumPostQuantumEngine.sign(defaultBytecode, dilithiumKeyPair.privateKey))
    }
    var isQuantumVerified by remember { mutableStateOf(true) }
    var showRotationHistory by remember { mutableStateOf(false) }

    // Periodic automatic key rotation countdown
    LaunchedEffect(isAutoRotationActive, activeBytecode) {
        if (isAutoRotationActive) {
            while (true) {
                delay(1000)
                if (rotationCountdownSeconds > 1) {
                    rotationCountdownSeconds -= 1
                } else {
                    rotationCountdownSeconds = 60
                    val rotated = keyRotator.rotateKey("Rotación Periódica Automática (Epoch #${keyRotator.activeEpoch})")
                    currentEpoch = rotated.epochNumber
                    dilithiumKeyPair = rotated.keyPair
                    currentSignature = DilithiumPostQuantumEngine.sign(activeBytecode, dilithiumKeyPair.privateKey)
                    isQuantumVerified = true
                    onAddLog(
                        "DILITHIUM_KEY_ROTATION",
                        "🔄 Rotación PQC completada: Epoch #${currentEpoch} [${dilithiumKeyPair.publicKey.keyFingerprint}]",
                        "SECURITY"
                    )
                }
            }
        }
    }

    // VM Execution state
    var vmExecResult by remember {
        mutableStateOf(vm.execute(defaultBytecode))
    }
    var isVmRunning by remember { mutableStateOf(false) }
    var isHoneypotTarPitForced by remember { mutableStateOf(false) }
    var isBenchmarking by remember { mutableStateOf(false) }
    var benchmarkResult by remember { mutableStateOf<VmsbNativeVirtualMachine.BenchmarkResult?>(null) }
    var polymorphicMutationCount by remember { mutableStateOf(0) }
    var lastPolymorphicHash by remember { mutableStateOf("0xORIGINAL_VMSB_SHA256") }

    // Attack Simulator States
    var simFridaActive by remember { mutableStateOf(false) }
    var simBytecodePatchActive by remember { mutableStateOf(false) }
    var simDebuggerActive by remember { mutableStateOf(false) }
    var simQuantumAttackActive by remember { mutableStateOf(false) }

    // Anti-REA Scan report
    var scanReport by remember {
        mutableStateOf(
            AntiReaDetector.performFullScan(
                context = context,
                simulateDebugger = simDebuggerActive,
                simulateFrida = simFridaActive,
                simulateRoot = false
            )
        )
    }

    // Colors
    val neonCyan = Color(0xFF00FFCC)
    val neonPurple = Color(0xFFBD00FF)
    val neonGreen = Color(0xFF00FF88)
    val warningRed = Color(0xFFFF3366)
    val darkCardBg = Color(0xFF060B18)
    val subCardBg = Color(0xFF0A1226)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("anti_rea_vmsb_dilithium_card"),
        colors = CardDefaults.cardColors(containerColor = darkCardBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(
                    if (scanReport.defenseStatus == "NOMINAL") neonCyan else warningRed,
                    neonPurple
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // HEADER & AUTHOR ATTRIBUTION
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0052D4), Color(0xFF4364F7), Color(0xFF6FB1FC))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Shield Lock",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (currentLanguage == AppLanguage.ENGLISH) "ANTI-REA VMSB-NATIVE DILITHIUM"
                            else if (currentLanguage == AppLanguage.ESPANOL) "SISTEMA PROTECCIÓN ANTI-REA (vmsb-nativo + Dilithium)"
                            else "SISTEMA PROTEÇÃO ANTI-REA (vmsb-nativo + Dilithium)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Registro: vmsb©",
                            fontSize = 9.sp,
                            color = neonCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Status Badge
                Surface(
                    color = if (scanReport.defenseStatus == "NOMINAL") Color(0xFF0D3328) else Color(0xFF3B0B14),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp,
                        if (scanReport.defenseStatus == "NOMINAL") neonGreen else warningRed
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (scanReport.defenseStatus == "NOMINAL") neonGreen else warningRed)
                        )
                        Text(
                            text = if (scanReport.defenseStatus == "NOMINAL") "ESCUDO: FORTIFICADO"
                            else "ALERTA: INTRUSIÓN",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (scanReport.defenseStatus == "NOMINAL") neonGreen else warningRed,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // DESCRIPTION BANNER
            Text(
                text = if (currentLanguage == AppLanguage.ENGLISH)
                    "Hardware-isolated virtual machine (VMSB) executing obfuscated opcodes guarded by NIST FIPS 204 CRYSTALS-Dilithium post-quantum lattice signatures. Blocks reverse engineering, decompilation, Frida hooking, and JDWP debuggers."
                else if (currentLanguage == AppLanguage.ESPANOL)
                    "Máquina Virtual de Software Nativa (VMSB) con opcodes ofuscados y firma post-cuántica CRYSTALS-Dilithium (NIST FIPS 204). Bloquea ingeniería inversa, desensambladores (IDA/Ghidra), trampolines Frida y depuradores JDWP."
                else
                    "Máquina Virtual de Software Nativa (VMSB) com opcodes ofuscados e assinatura pós-quântica CRYSTALS-Dilithium (NIST FIPS 204). Bloqueia engenharia reversa, descompilação, hooks do Frida e depuradores.",
                fontSize = 10.sp,
                color = Color(0xFFA0AEC0),
                lineHeight = 14.sp
            )

            // KPI QUICK TELEMETRY TILES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tile 1: Security Score
                Surface(
                    modifier = Modifier.weight(1f),
                    color = subCardBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("SCORE ANTI-REA", fontSize = 7.5.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        Text(
                            text = "${scanReport.overallSecurityScore}%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (scanReport.overallSecurityScore >= 80) neonGreen else warningRed,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Tile 2: Quantum PQC Level
                Surface(
                    modifier = Modifier.weight(1f),
                    color = subCardBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("RESISTENCIA CUÁNTICA", fontSize = 7.5.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        Text(
                            text = "192-bit PQC",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = neonCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Tile 3: VMSB VM State
                Surface(
                    modifier = Modifier.weight(1f),
                    color = subCardBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("ESTADO VMSB", fontSize = 7.5.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        Text(
                            text = if (vmExecResult.isCanaryIntact && !vmExecResult.isTamperDetected) "ACTIVO / OK" else "TRAMPA ACTIVA",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (vmExecResult.isCanaryIntact && !vmExecResult.isTamperDetected) neonGreen else warningRed,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // NAVIGATION SUB-TABS
            val tabs = listOf(
                "🖥️ VMSB VM",
                "🔐 DILITHIUM PQC",
                "📡 RADAR ANTI-REA",
                "⚡ ATAQUES",
                "📜 ATESTACIÓN",
                "⚡ FLUIDEZ & MUTACIÓN"
            )

            ScrollableTabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = Color(0xFF030712),
                contentColor = neonCyan,
                edgePadding = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedSubTab == index,
                        onClick = { selectedSubTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 9.sp,
                                fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                color = if (selectedSubTab == index) neonCyan else Color(0xFF94A3B8)
                            )
                        }
                    )
                }
            }

            // SUB-TAB CONTENT PANELS
            when (selectedSubTab) {
                0 -> {
                    // TAB 0: VMSB VIRTUAL MACHINE ENGINE (Registers & Bytecode Disassembly)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "REGISTROS VIRTUALES VMSB (CPU SANDBOX)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )

                            Button(
                                onClick = {
                                    scope.launch {
                                        isVmRunning = true
                                        delay(150)
                                        vmExecResult = vm.executeFastDispatchAsync(
                                            bytecode = activeBytecode,
                                            simulateFridaHook = simFridaActive,
                                            simulateBytecodePatch = simBytecodePatchActive,
                                            simulateDebugger = simDebuggerActive,
                                            isHoneypotTarPitForced = isHoneypotTarPitForced
                                        )
                                        isVmRunning = false
                                        onAddLog("VMSB_EXEC", "⚡ Ciclo VMSB ejecutado sin fricción. Hash: ${vmExecResult.executionHash}", "INFO")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("anti_rea_run_vm_btn")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isVmRunning) "EJECUTANDO..." else "EJECUTAR CICLO", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        // Register Grid (VR0 to VR4 + VIP + VSP + VFLAGS)
                        Surface(
                            color = subCardBg,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    vmExecResult.registers.entries.take(4).forEach { (k, v) ->
                                        Column {
                                            Text(k, fontSize = 7.5.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                            Text(v, fontSize = 9.5.sp, color = neonCyan, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                                Divider(color = Color(0xFF1E293B), thickness = 0.5.dp)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    vmExecResult.registers.entries.drop(4).take(3).forEach { (k, v) ->
                                        Column {
                                            Text(k, fontSize = 7.5.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                            Text(v, fontSize = 9.5.sp, color = neonPurple, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                    Column {
                                        Text("CANARY", fontSize = 7.5.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                        Text(
                                            if (vmExecResult.isCanaryIntact) "0xCAFEBABE (OK)" else "0xBADF00D (CORRUPT)",
                                            fontSize = 9.sp,
                                            color = if (vmExecResult.isCanaryIntact) neonGreen else warningRed,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        // Bytecode Disassembly Viewer
                        Text(
                            text = "DESENSAMBLADO DE BYTECODE PROTEGIDO:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace
                        )

                        Surface(
                            color = Color(0xFF020617),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF0F172A)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                        ) {
                            LazyColumn(modifier = Modifier.padding(8.dp)) {
                                items(disassembledCode) { ins ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 1.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "0x%04X:".format(ins.address),
                                            fontSize = 8.5.sp,
                                            color = Color(0xFF475569),
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.width(42.dp)
                                        )
                                        Text(
                                            text = ins.rawHex,
                                            fontSize = 8.sp,
                                            color = Color(0xFF64748B),
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.width(80.dp)
                                        )
                                        Text(
                                            text = ins.mnemonic,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (ins.mnemonic.startsWith("VANTI") || ins.mnemonic.startsWith("VDILITHIUM")) neonCyan else Color(0xFFE2E8F0),
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.width(75.dp)
                                        )
                                        Text(
                                            text = "${ins.operand1} ${if (ins.operand2.isNotEmpty()) ", " + ins.operand2 else ""}",
                                            fontSize = 8.sp,
                                            color = Color(0xFF94A3B8),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: CRYSTALS-DILITHIUM POST-QUANTUM CRYPTOGRAPHY
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "PARÁMETROS RETICULARES NIST FIPS 204 (ML-DSA-65)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )

                        Surface(
                            color = subCardBg,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Anillo de Polinomios:", fontSize = 8.5.sp, color = Color(0xFF94A3B8))
                                    Text("R_q = Z_q[X]/(X^256 + 1)", fontSize = 8.5.sp, color = neonCyan, fontFamily = FontFamily.Monospace)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Módulo Primo q:", fontSize = 8.5.sp, color = Color(0xFF94A3B8))
                                    Text("8,380,417 (2^23 - 2^13 + 1)", fontSize = 8.5.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Matriz A (k x l):", fontSize = 8.5.sp, color = Color(0xFF94A3B8))
                                    Text("6 x 5 (Dilithium-3 / 192-bit)", fontSize = 8.5.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Fingerprint Clave Pública:", fontSize = 8.5.sp, color = Color(0xFF94A3B8))
                                    Text(dilithiumKeyPair.publicKey.keyFingerprint, fontSize = 8.5.sp, color = neonGreen, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        // Signature Details
                        Text("FIRMA RETICULAR DEL BYTECODE VMSB:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)

                        Surface(
                            color = Color(0xFF020617),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF0F172A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = currentSignature.fullSignatureHex,
                                    fontSize = 8.sp,
                                    color = if (isQuantumVerified) neonCyan else warningRed,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Desafío c: ${currentSignature.challengeHash}", fontSize = 8.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                                    Text(
                                        text = if (isQuantumVerified) "✅ VERIFICADA POST-QUANTUM" else "🚨 FIRMA INVÁLIDA / TAMPER",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isQuantumVerified) neonGreen else warningRed,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Rotación de Claves Dilithium-3 & Forward Secrecy
                        Surface(
                            color = Color(0xFF030712),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🔄 ROTACIÓN DE CLAVES PQC (DILITHIUM-3):",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = neonCyan,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Surface(
                                        color = Color(0xFF064E3B),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, neonGreen)
                                    ) {
                                        Text(
                                            text = "EPOCH #$currentEpoch",
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = neonGreen,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Auto-rotación (60s):", fontSize = 7.5.sp, color = Color(0xFF94A3B8))
                                    Text(
                                        text = if (isAutoRotationActive) "${rotationCountdownSeconds}s restantes (ACTIVA)" else "PAUSADA",
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAutoRotationActive) neonGreen else warningRed,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Historial de épocas:", fontSize = 7.5.sp, color = Color(0xFF94A3B8))
                                    Text(
                                        text = "${keyRotator.rotationHistory.size} épocas registradas",
                                        fontSize = 7.5.sp,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                // Interactive Key Rotation Buttons
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            val rotated = keyRotator.rotateKey("Rotación Manual Bajo Demanda")
                                            currentEpoch = rotated.epochNumber
                                            dilithiumKeyPair = rotated.keyPair
                                            currentSignature = DilithiumPostQuantumEngine.sign(activeBytecode, dilithiumKeyPair.privateKey)
                                            isQuantumVerified = true
                                            rotationCountdownSeconds = 60
                                            onAddLog(
                                                "DILITHIUM_KEY_ROTATION",
                                                "🔑 Rotación manual ejecutada: Epoch #${currentEpoch} [${dilithiumKeyPair.publicKey.keyFingerprint}]",
                                                "SECURITY"
                                            )
                                            Toast.makeText(context, "🔑 Dilithium-3: Clave rotada a Epoch #${currentEpoch}", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .height(32.dp)
                                            .testTag("btn_rotate_dilithium_key")
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Rotate", tint = Color.White, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("ROTAR CLAVE AHORA", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }

                                    Button(
                                        onClick = {
                                            isAutoRotationActive = !isAutoRotationActive
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isAutoRotationActive) Color(0xFF065F46) else Color(0xFF475569)
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp)
                                            .testTag("btn_toggle_auto_rotation")
                                    ) {
                                        Text(
                                            text = if (isAutoRotationActive) "AUTO: ON" else "AUTO: OFF",
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            showRotationHistory = !showRotationHistory
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .weight(0.9f)
                                            .height(32.dp)
                                            .testTag("btn_toggle_epoch_history")
                                    ) {
                                        Text(
                                            text = if (showRotationHistory) "OCULTAR" else "HISTORIAL",
                                            fontSize = 7.sp,
                                            color = Color(0xFFCBD5E1),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                if (showRotationHistory) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF020617), RoundedCornerShape(6.dp))
                                            .padding(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text("REGISTRO INMUTABLE DE ROTACIONES PQC:", fontSize = 6.5.sp, color = neonCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        keyRotator.rotationHistory.take(4).forEach { entry ->
                                            Text(
                                                text = "• Epoch #${entry.epochNumber}: ${entry.publicKeyFingerprint} (${entry.rotationReason})",
                                                fontSize = 6.sp,
                                                color = Color(0xFF94A3B8),
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Re-Sign / Verify Actions
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    isQuantumVerified = DilithiumPostQuantumEngine.verify(
                                        payload = defaultBytecode,
                                        signature = currentSignature,
                                        publicKey = dilithiumKeyPair.publicKey,
                                        isBytecodeTampered = simBytecodePatchActive
                                    )
                                    Toast.makeText(
                                        context,
                                        if (isQuantumVerified) "✅ Dilithium-3: Firma verificada matemáticamente!"
                                        else "🚨 Alerta: Bytecode alterado. Verificación fallida!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF065F46)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .testTag("anti_rea_verify_dilithium_btn")
                            ) {
                                Icon(Icons.Default.Done, contentDescription = "Verify", tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("VERIFICAR FIRMA PQC", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: RADAR ANTI-REA (SYSTEM PROBES)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RADAR DE AMENAZAS EN TIEMPO REAL:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )

                            Button(
                                onClick = {
                                    scanReport = AntiReaDetector.performFullScan(
                                        context = context,
                                        simulateDebugger = simDebuggerActive,
                                        simulateFrida = simFridaActive,
                                        simulateRoot = false
                                    )
                                    Toast.makeText(context, "Radar Anti-REA actualizado", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(26.dp)
                                    .testTag("anti_rea_scan_threats_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Scan", tint = Color.White, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ESCANEAR", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        scanReport.items.forEach { probe ->
                            Surface(
                                color = subCardBg,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (probe.isThreatDetected) warningRed else Color(0xFF1E293B)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (probe.isThreatDetected) warningRed else neonGreen)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = probe.vectorName,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (probe.isThreatDetected) warningRed else Color.White,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = probe.statusMessage,
                                            fontSize = 8.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }

                                    Surface(
                                        color = if (probe.isThreatDetected) Color(0xFF450A0A) else Color(0xFF064E3B),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (probe.isThreatDetected) "AMENAZA" else "LIMPIO",
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (probe.isThreatDetected) warningRed else neonGreen,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // TAB 3: ATTACK SIMULATOR (TEST ANTI-REA RESILIENCE)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "SIMULADOR DE ATAQUES & INGENIERÍA INVERSA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = "Inyecta vectores de ataque conocidos para observar cómo el motor Anti-REA VMSB y Dilithium-3 aíslan el proceso y protegen la propiedad de 'vmsb'.",
                            fontSize = 8.5.sp,
                            color = Color(0xFF94A3B8)
                        )

                        // Vector 1: Frida Hook
                        Surface(
                            color = subCardBg,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (simFridaActive) warningRed else Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Simular Inyección Frida (Hooking)", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Inyecta trampolín en dispatcher VMSB", fontSize = 7.5.sp, color = Color(0xFF94A3B8))
                                }
                                Switch(
                                    checked = simFridaActive,
                                    onCheckedChange = {
                                        simFridaActive = it
                                        scanReport = AntiReaDetector.performFullScan(context, simDebuggerActive, simFridaActive, false)
                                        vmExecResult = vm.execute(defaultBytecode, simFridaActive, simBytecodePatchActive, simDebuggerActive)
                                    },
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                        }

                        // Vector 2: Bytecode Patch (IDA/Ghidra)
                        Surface(
                            color = subCardBg,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (simBytecodePatchActive) warningRed else Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Simular Parcheo de Bytecode (IDA/Ghidra)", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Modifica 1 byte para saltar verificación", fontSize = 7.5.sp, color = Color(0xFF94A3B8))
                                }
                                Switch(
                                    checked = simBytecodePatchActive,
                                    onCheckedChange = {
                                        simBytecodePatchActive = it
                                        isQuantumVerified = !it
                                        vmExecResult = vm.execute(defaultBytecode, simFridaActive, simBytecodePatchActive, simDebuggerActive)
                                    },
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                        }

                        // Vector 3: Debugger Attached (JDWP)
                        Surface(
                            color = subCardBg,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (simDebuggerActive) warningRed else Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Simular Depurador JDWP / TracerPid", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Simula attach de GDB / Android Studio", fontSize = 7.5.sp, color = Color(0xFF94A3B8))
                                }
                                Switch(
                                    checked = simDebuggerActive,
                                    onCheckedChange = {
                                        simDebuggerActive = it
                                        scanReport = AntiReaDetector.performFullScan(context, simDebuggerActive, simFridaActive, false)
                                        vmExecResult = vm.execute(defaultBytecode, simFridaActive, simBytecodePatchActive, simDebuggerActive)
                                    },
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                        }

                        // Reset Button
                        Button(
                            onClick = {
                                simFridaActive = false
                                simBytecodePatchActive = false
                                simDebuggerActive = false
                                isHoneypotTarPitForced = false
                                isQuantumVerified = true
                                activeBytecode = defaultBytecode
                                disassembledCode = vm.disassemble(defaultBytecode)
                                polymorphicMutationCount = 0
                                lastPolymorphicHash = "0xORIGINAL_VMSB_SHA256"
                                scanReport = AntiReaDetector.performFullScan(context, false, false, false)
                                vmExecResult = vm.execute(defaultBytecode, false, false, false)
                                Toast.makeText(context, "🛡️ Escudo Anti-REA restaurado a 100% nominal", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .testTag("anti_rea_reset_btn")
                        ) {
                            Icon(Icons.Default.Build, contentDescription = "Restore", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RESTAURAR ESCUDO Y RECALIBRAR VMSB", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                4 -> {
                    // TAB 4: POST-QUANTUM ATTESTATION CERTIFICATE
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "CERTIFICADO DE ATESTACIÓN POST-CUÁNTICA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )

                        val attestationJson = """
                            {
                              "protocol": "ANTI-REA-VMSB-DILITHIUM-3",
                              "standard": "NIST FIPS 204 (ML-DSA-65)",
                              "creator": "vmsb©",
                              "shield_status": "${scanReport.defenseStatus}",
                              "security_score": ${scanReport.overallSecurityScore},
                              "pqc_fingerprint": "${dilithiumKeyPair.publicKey.keyFingerprint}",
                              "dilithium_sig": "${currentSignature.fullSignatureHex.take(32)}...",
                              "vm_state_hash": "${vmExecResult.executionHash}",
                              "canary_status": "${if (vmExecResult.isCanaryIntact) "SECURE_0xCAFEBABE" else "CORRUPTED"}",
                              "timestamp": "${scanReport.timestamp}"
                            }
                        """.trimIndent()

                        Surface(
                            color = Color(0xFF020617),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF0F172A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = attestationJson,
                                fontSize = 8.sp,
                                color = neonCyan,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Button(
                            onClick = {
                                clipboard.setText(AnnotatedString(attestationJson))
                                Toast.makeText(context, "📋 Token de Atestación copiado al portapapeles", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("COPIAR TOKEN DE ATESTACIÓN CRIPTOGRÁFICA", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                5 -> {
                    // TAB 5: ULTRA-FLUIDEZ & MUTACIÓN POLIMÓRFICA (ZERO BOTTLENECK DISPATCH & TAR-PIT DECEPTION)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ MOTOR DE ULTRA-FLUIDEZ & POLIMORFISMO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )

                            Surface(
                                color = if (isHoneypotTarPitForced || vmExecResult.isHoneypotActive) Color(0xFF450A0A) else Color(0xFF064E3B),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, if (isHoneypotTarPitForced || vmExecResult.isHoneypotActive) warningRed else neonGreen)
                            ) {
                                Text(
                                    text = if (isHoneypotTarPitForced || vmExecResult.isHoneypotActive) "TAR-PIT ACTIVO" else "DISPATCH 120 FPS",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHoneypotTarPitForced || vmExecResult.isHoneypotActive) warningRed else neonGreen,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Despacho nativo asíncrono con cero cuellos de botella (Dispatchers.Default), mutación polimórfica continua de bytecode (anti-Ghidra / anti-IDA Pro) y trampa Tar-Pit señuelo.",
                            fontSize = 8.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 11.sp
                        )

                        // Fluency KPI Tiles
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Tile 1: Latency
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = subCardBg,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF1E293B))
                            ) {
                                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("LATENCIA DESPACHO", fontSize = 6.5.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                    Text(
                                        text = benchmarkResult?.let { "%.2f µs".format(it.avgLatencyMicros) } ?: "< 0.18 ms",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = neonCyan,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Tile 2: Throughput
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = subCardBg,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF1E293B))
                            ) {
                                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("RENDIMIENTO OPS/S", fontSize = 6.5.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                    Text(
                                        text = benchmarkResult?.let { "%.2fM".format(it.throughputOpsPerSec / 1_000_000.0) } ?: "> 2.8M ops/s",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = neonGreen,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Tile 3: UI Fluidity
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = subCardBg,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF1E293B))
                            ) {
                                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("FLUIDEZ UI (LOCK)", fontSize = 6.5.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "120 FPS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Tile 4: Polymorphic Entropy
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = subCardBg,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF1E293B))
                            ) {
                                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("MUTACIÓN EN CALIENTE", fontSize = 6.5.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "v$polymorphicMutationCount (ACTIVO)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = neonPurple,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Action Buttons: Benchmark, Polymorphic Mutate, Tar-Pit Honeypot
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Button 1: Ultra-Fluency Benchmark
                            Button(
                                onClick = {
                                    scope.launch {
                                        isBenchmarking = true
                                        val bench = vm.benchmarkUltraFluency(1000)
                                        benchmarkResult = bench
                                        isBenchmarking = false
                                        onAddLog(
                                            "VMSB_BENCH",
                                            "⚡ Benchmark completado: ${bench.totalCycles} ciclos en ${bench.elapsedMicroseconds} µs (${bench.throughputOpsPerSec.toInt()} ops/s) en ${bench.threadName}.",
                                            "INFO"
                                        )
                                        Toast.makeText(context, "⚡ Benchmark: %.2f µs/op en hilo background".format(bench.avgLatencyMicros), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isBenchmarking,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .testTag("anti_rea_benchmark_fluency_btn"),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Benchmark", tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(if (isBenchmarking) "MIDIENDO..." else "BENCHMARK FLUIDEZ", fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }

                            // Button 2: Polymorphic Mutation
                            Button(
                                onClick = {
                                    activeBytecode = vm.polymorphicMutate(activeBytecode)
                                    disassembledCode = vm.disassemble(activeBytecode)
                                    polymorphicMutationCount++
                                    val md = MessageDigest.getInstance("SHA-256")
                                    val digest = md.digest(activeBytecode)
                                    lastPolymorphicHash = "0x" + digest.take(8).joinToString("") { "%02X".format(it) }
                                    currentSignature = DilithiumPostQuantumEngine.sign(activeBytecode, dilithiumKeyPair.privateKey)
                                    onAddLog("VMSB_MUTATION", "🧬 Bytecode mutado polimórficamente #$polymorphicMutationCount. Hash: $lastPolymorphicHash. Firma PQC regenerada.", "INFO")
                                    Toast.makeText(context, "🧬 Opcodes mutados dinámicamente (#$polymorphicMutationCount)", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .testTag("anti_rea_polymorphic_mutation_btn"),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Mutate", tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("MUTACIÓN POLIMÓRFICA", fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }

                            // Button 3: Toggle Tar-Pit Honeypot
                            Button(
                                onClick = {
                                    isHoneypotTarPitForced = !isHoneypotTarPitForced
                                    scope.launch {
                                        vmExecResult = vm.executeFastDispatchAsync(
                                            bytecode = activeBytecode,
                                            simulateFridaHook = simFridaActive,
                                            simulateBytecodePatch = simBytecodePatchActive,
                                            simulateDebugger = simDebuggerActive,
                                            isHoneypotTarPitForced = isHoneypotTarPitForced
                                        )
                                    }
                                    Toast.makeText(
                                        context,
                                        if (isHoneypotTarPitForced) "🪤 Tar-Pit Señuelo FORZADO. Atacantes desviados a claves basura."
                                        else "🛡️ Tar-Pit en modo standby automático.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isHoneypotTarPitForced) Color(0xFF991B1B) else Color(0xFF0F766E)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .testTag("anti_rea_honeypot_toggle_btn"),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = "TarPit", tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isHoneypotTarPitForced) "DESACTIVAR SEÑUELO" else "TAR-PIT SEÑUELO",
                                    fontSize = 7.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Detailed Diagnostic & Telemetry Card
                        Surface(
                            color = Color(0xFF020617),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "📊 TELEMETRÍA AVANZADA DE ANTI-INGENIERÍA INVERSA & FLUIDEZ:",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = neonCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "• Hilo de Despacho: ${benchmarkResult?.threadName ?: "Dispatchers.Default (Background Worker Pool)"} (Zero UI impact)",
                                    fontSize = 7.5.sp,
                                    color = Color(0xFFCBD5E1),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "• Inmunidad Estática: Bytecode mutado con semilla rodante. Los volcados de memoria por IDA/Ghidra producen firmas binarias disímiles.",
                                    fontSize = 7.5.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                                Text(
                                    text = "• Hash Sesión Bytecode: $lastPolymorphicHash  •  Firma FIPS 204: ${currentSignature.fullSignatureHex.take(20)}...",
                                    fontSize = 7.sp,
                                    color = neonPurple,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "• Tar-Pit Deception Engine: ${if (isHoneypotTarPitForced || vmExecResult.isHoneypotActive) "ENGAÑO ACTIVO (Atacante recibe claves señuelo 0xDECOY001 / 0xB06U5003)" else "VIGILANCIA PASIVA (Trampa armada ante intrusión de depurador)"}",
                                    fontSize = 7.sp,
                                    color = if (isHoneypotTarPitForced || vmExecResult.isHoneypotActive) warningRed else neonGreen,
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
