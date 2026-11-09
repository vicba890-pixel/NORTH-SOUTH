package com.example.ui.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

class ProceduralAudioEngine(private val context: Context) {
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var audioThread: Thread? = null

    // Synth Parameters
    @Volatile var volume: Float = 0.5f
    @Volatile var tempoMs: Long = 600 // Melodic tempo
    @Volatile var theme: String = "PRIMITIVE_1" // "PRIMITIVE_1", "COSMIC_2", "OCEAN_VISTA", "TACTICAL"
    @Volatile var activeHarmonics: Int = 3 // Standard additive harmonics

    // Ancient Golden Scales (Pure Frequencies in Hz)
    private val scales = mapOf(
        "PRIMITIVE_1" to listOf(147.0, 165.0, 196.0, 220.0, 294.0, 330.0, 392.0), // G major pentatonic / Old times
        "COSMIC_2" to listOf(110.0, 165.0, 220.0, 330.0, 440.0, 550.0, 660.0),    // Pure math ratios (3:2, 5:4)
        "OCEAN_VISTA" to listOf(130.8, 146.8, 174.6, 196.0, 261.6, 293.7, 349.2), // F Lydian wind vibe
        "TACTICAL" to listOf(120.0, 180.0, 240.0, 360.0, 480.0, 720.0, 1080.0)    // High frequency sonic alerts
    )

    fun start() {
        if (isPlaying) return
        isPlaying = true

        val sampleRate = 22050
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        try {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val format = AudioFormat.Builder()
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build()
            val builder = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)

            audioTrack = builder.build().apply {
                play()
            }
        } catch (e: Exception) {
            Log.e("ProceduralAudioEngine", "AudioTrack Initialization failed: ${e.message}")
            isPlaying = false
            return
        }

        audioThread = thread(isDaemon = true, name = "ProceduralAudioThread") {
            var phase = 0.0
            val noteDurationSamples = sampleRate * 1.0 // 1 second notes
            var sampleCount = 0

            // Melodic sequencer
            var currentNoteIndex = 0
            var currentFreq = 220.0
            var lfoPhase = 0.0

            val buffer = ShortArray(1024)

            while (isPlaying) {
                // Get current scale according to active theme
                val currentScale = scales[theme] ?: scales["PRIMITIVE_1"]!!

                // Decide next note at tempo boundaries
                val tempoSamples = (sampleRate * (tempoMs / 1000.0)).toInt()
                if (sampleCount >= tempoSamples) {
                    sampleCount = 0
                    currentNoteIndex = (currentNoteIndex + 1) % currentScale.size
                    // Procedural melody logic with occasional jump patterns
                    val targetFreq = currentScale[currentNoteIndex]
                    currentFreq = if (Math.random() > 0.8) {
                        currentScale[(currentNoteIndex + 2) % currentScale.size]
                    } else {
                        targetFreq
                    }
                }

                // Fill writing buffer with mathematical additive sine waves + LFO
                for (i in buffer.indices) {
                    val angleIncrement = (2 * PI * currentFreq) / sampleRate
                    phase += angleIncrement
                    if (phase > 2 * PI) phase -= 2 * PI

                    // Wave LFO (Slow volume envelope modulation - 0.5Hz)
                    val lfoInc = (2 * PI * 0.4) / sampleRate
                    lfoPhase += lfoInc
                    if (lfoPhase > 2 * PI) lfoPhase -= 2 * PI
                    val lfoMod = 0.6 + 0.4 * sin(lfoPhase)

                    // Additive Harmonics (Golden Ratio series)
                    var mainWave = sin(phase)
                    
                    // Add up to 'activeHarmonics' harmonics using Fourier concepts
                    var harmonicFactor = 0.5
                    for (h in 2..activeHarmonics) {
                        mainWave += harmonicFactor * sin(phase * h)
                        harmonicFactor *= 0.5
                    }

                    // Add high frequency natural sparkle (ambient)
                    if (theme == "COSMIC_2") {
                        mainWave += 0.08 * sin(phase * 4.5)
                    } else if (theme == "OCEAN_VISTA") {
                        // Generate dynamic ocean breaker spray sound (white noise overlay)
                        val noise = (Math.random() * 2.0 - 1.0) * 0.07
                        mainWave += noise * lfoMod
                    }

                    // Scale overall amplitude and convert to PCM Short
                    val volumeScale = volume * 32767.0 * 0.43
                    val sampleVal = (mainWave * volumeScale * lfoMod).coerceIn(-32767.0, 32767.0)
                    buffer[i] = sampleVal.toInt().toShort()
                    sampleCount++
                }

                try {
                    audioTrack?.write(buffer, 0, buffer.size)
                } catch (e: Exception) {
                    Log.e("ProceduralAudioEngine", "Error writing audio: ${e.message}")
                    break
                }
            }
        }
    }

    fun playChimeSuccess() {
        playWebAudioBattlefieldEffect("SHIELD", 0.9f)
    }

    fun playChimeWarning() {
        playWebAudioBattlefieldEffect("IMPACT", 0.8f)
    }

    fun playKeyTap() {
        playWebAudioBattlefieldEffect("LASER", 0.4f)
    }

    /**
     * Synthesizes dynamic, procedural battlefield sound effects using a virtual "Web Audio API" node graph model:
     * - "LASER": Sweeping Oscillator (1200Hz -> 150Hz) -> Exponential Gain Envelope -> AudioDestination
     * - "IMPACT": WhiteNoiseSource + LowFrequencyOsculator (250Hz -> 50Hz) -> Linear Gain Envelope -> AudioDestination
     * - "SHIELD": RingModulator (900Hz * 1400Hz) + HighPassNoise -> Dual Exponential Envelope -> AudioDestination
     */
    fun playWebAudioBattlefieldEffect(effectType: String, intensity: Float = 1.0f) {
        thread(isDaemon = true, name = "ProceduralSFXThread") {
            val sampleRate = 22050
            val durationSec = when (effectType) {
                "LASER" -> 0.35f
                "IMPACT" -> 0.50f
                "SHIELD" -> 0.25f
                else -> 0.30f
            }
            val totalSamples = (sampleRate * durationSec).toInt()
            val buffer = ShortArray(totalSamples)
            
            var phase1 = 0.0
            var phase2 = 0.0
            
            for (i in 0 until totalSamples) {
                val progress = i.toFloat() / totalSamples
                
                val sampleVal = when (effectType) {
                    "LASER" -> {
                        // Web Audio equivalent: oscillator.frequency.exponentialRampToValueAtTime
                        val freq = 1200.0 * Math.exp(-6.0 * progress) + 150.0
                        phase1 += (2.0 * PI * freq) / sampleRate
                        
                        // Web Audio equivalent: gainNode.gain.exponentialRampToValueAtTime
                        val envelope = Math.exp(-5.0 * progress)
                        
                        val sine = sin(phase1)
                        val tri = if (phase1 % (2.0 * PI) < PI) {
                            (phase1 % (2.0 * PI) / PI) * 2.0 - 1.0
                        } else {
                            3.0 - (phase1 % (2.0 * PI) / PI) * 2.0
                        }
                        // Mix dynamic frequencies
                        val wave = 0.7 * sine + 0.3 * tri
                        wave * envelope * intensity
                    }
                    "IMPACT" -> {
                        // Web Audio equivalent: low-pass oscillator + noise node
                        val freq = 250.0 * (1.0 - progress) + 50.0
                        phase1 += (2.0 * PI * freq) / sampleRate
                        
                        // Procedural White Noise Generator node
                        val noise = Math.random() * 2.0 - 1.0
                        
                        // Web Audio equivalent: gainNode.gain.linearRampToValueAtTime
                        val envelope = (1.0 - progress).toDouble()
                        
                        val wave = 0.4 * sin(phase1) + 0.6 * noise
                        wave * envelope * intensity
                    }
                    "SHIELD" -> {
                        // Web Audio equivalent: OscillatorNode * OscillatorNode (Ring Modulation)
                        val freq1 = 900.0
                        val freq2 = 1350.0
                        phase1 += (2.0 * PI * freq1) / sampleRate
                        phase2 += (2.0 * PI * freq2) / sampleRate
                        
                        val ringMod = sin(phase1) * sin(phase2)
                        
                        // High frequency crackle overlay
                        val noise = if (Math.random() > 0.4) Math.random() * 2.0 - 1.0 else 0.0
                        
                        // Envelope with swift attack and fast decay
                        val envelope = if (progress < 0.08f) {
                            (progress / 0.08f).toDouble()
                        } else {
                            Math.exp(-9.0 * (progress - 0.08f))
                        }
                        
                        val wave = 0.6 * ringMod + 0.4 * noise
                        wave * envelope * intensity
                    }
                    else -> 0.0
                }
                
                // Scale according to global volume setting
                val volumeScale = volume * 32767.0 * 0.45
                val finalVal = (sampleVal * volumeScale).coerceIn(-32767.0, 32767.0)
                buffer[i] = finalVal.toInt().toShort()
            }
            
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                
                track.write(buffer, 0, buffer.size)
                track.play()
                
                Thread.sleep((durationSec * 1000).toLong() + 100)
                track.stop()
                track.release()
            } catch (e: Exception) {
                Log.e("ProceduralAudioEngine", "SFX play failed: ${e.message}")
            }
        }
    }

    fun stop() {
        isPlaying = false
        audioTrack?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                Log.e("ProceduralAudioEngine", "Error stopping track: ${e.message}")
            }
        }
        audioTrack = null
        audioThread = null
    }
}
