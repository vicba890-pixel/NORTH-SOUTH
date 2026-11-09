package com.example.ui.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TechEntity
import kotlin.math.sin
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke

@Composable
fun ResearchLabTab(
    techs: List<TechEntity>,
    playerAlloys: Int,
    onUnlockTech: (TechEntity) -> Unit,
    modifier: Modifier = Modifier,
    currentLanguage: com.example.ui.game.AppLanguage = com.example.ui.game.AppLanguage.ENGLISH
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val titleText = when (currentLanguage) {
                com.example.ui.game.AppLanguage.ENGLISH -> "🔬 CIVILIZATION TECHNOLOGY DECK"
                com.example.ui.game.AppLanguage.ESPANOL -> "🔬 REGISTRO MAESTRO DE TECNOLOGÍA"
                com.example.ui.game.AppLanguage.PORTUGUES -> "🔬 DIRETÓRIO DE TECNOLOGIA AVANÇADA"
            }
            val subtitleText = when (currentLanguage) {
                com.example.ui.game.AppLanguage.ENGLISH -> "Normalize ancient and quantum technologies using mathematical alloy multipliers."
                com.example.ui.game.AppLanguage.ESPANOL -> "Usa aleaciones metalúrgicas para activar modificadores de demografía y propulsores."
                com.example.ui.game.AppLanguage.PORTUGUES -> "Estabilize modificadores quânticos para acelerar a demografia cósmica."
            }
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().height(290.dp).testTag("tech_tree_grid"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(techs) { tech ->
                    TechCard(tech = tech, playerAlloys = playerAlloys, onUnlock = { onUnlockTech(tech) }, currentLanguage = currentLanguage)
                }
            }
        }
    }
}

@Composable
fun TechCard(
    tech: TechEntity,
    playerAlloys: Int,
    onUnlock: () -> Unit,
    currentLanguage: com.example.ui.game.AppLanguage = com.example.ui.game.AppLanguage.ENGLISH
) {
    val canAfford = playerAlloys >= tech.cost
    val isUnlocked = tech.isUnlocked

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isUnlocked) Color(0x3300FF88) else Color(0x1F94A3B8),
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) Color(0x1200FF88) else Color(0xFF020617)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .background(
                            when (tech.category) {
                                "Civilization" -> Color(0x1A00E5FF)
                                "Signal" -> Color(0x1AE040FB)
                                "Aerospace" -> Color(0x1AFFD700)
                                else -> Color(0x1A90A4AE)
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tech.category.uppercase(),
                        fontSize = 8.sp,
                        color = when (tech.category) {
                            "Civilization" -> Color(0xFF00E5FF)
                            "Signal" -> Color(0xFFE040FB)
                            "Aerospace" -> Color(0xFFFFD700)
                            else -> Color(0xFF90A4AE)
                        },
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                val normalizedLabel = when (currentLanguage) {
                    com.example.ui.game.AppLanguage.ENGLISH -> "NORMALIZED"
                    com.example.ui.game.AppLanguage.ESPANOL -> "NORMALIZADO"
                    com.example.ui.game.AppLanguage.PORTUGUES -> "NORMALIZADO"
                }
                val alloysLabel = when (currentLanguage) {
                    com.example.ui.game.AppLanguage.ENGLISH -> "Alloys"
                    com.example.ui.game.AppLanguage.ESPANOL -> "Aleaciones"
                    com.example.ui.game.AppLanguage.PORTUGUES -> "Ligas"
                }
                val costLabel = if (isUnlocked) normalizedLabel else "⚙️ ${tech.cost} $alloysLabel"

                Text(
                    text = costLabel,
                    fontSize = 9.sp,
                    color = if (isUnlocked) Color(0xFF00FF88) else if (canAfford) Color.White else Color(0xFFFF1744),
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = tech.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Dynamic scientific formula explanation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), shape = RoundedCornerShape(4.dp))
                    .padding(5.dp)
            ) {
                Text(
                    text = tech.equation,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA5B4FC)
                )
            }

            if (!isUnlocked) {
                Button(
                    onClick = onUnlock,
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF),
                        disabledContainerColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                    modifier = Modifier.fillMaxWidth().height(24.dp).testTag("unlock_btn_${tech.id}")
                ) {
                    val btnText = when (currentLanguage) {
                        com.example.ui.game.AppLanguage.ENGLISH -> "EXTRACT COGNITIVE MATRIX"
                        com.example.ui.game.AppLanguage.ESPANOL -> "EXTRAER MATRIZ INTEGRAL"
                        com.example.ui.game.AppLanguage.PORTUGUES -> "ATIVAR COGNITIVO MATRIX"
                    }
                    Text(btnText, fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(Color(0x2600FF88), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val activeText = when (currentLanguage) {
                        com.example.ui.game.AppLanguage.ENGLISH -> "ACTIVE MODIFIER INTEGRATED"
                        com.example.ui.game.AppLanguage.ESPANOL -> "MODIFICADOR ACTIVO ASIGNADO"
                        com.example.ui.game.AppLanguage.PORTUGUES -> "MODIFICADOR ATIVO ACOPLADO"
                    }
                    Text(activeText, fontSize = 8.sp, color = Color(0xFF00FF88), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SoundMixerTab(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    tempoMs: Long,
    onTempoChange: (Long) -> Unit,
    activeHarmonics: Int,
    onHarmonicsChange: (Int) -> Unit,
    theme: String,
    onThemeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    currentLanguage: com.example.ui.game.AppLanguage = com.example.ui.game.AppLanguage.ENGLISH
) {
    val scopePhaseTransition = rememberInfiniteTransition(label = "RadarWave")
    val waveOffset by scopePhaseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    // --- High-tech state models for simulated hardware peripheral integrations ---
    var isBluetoothActive by remember { mutableStateOf(false) }
    var isScanningBluetooth by remember { mutableStateOf(false) }
    var connectedBluetoothDevice by remember { mutableStateOf<String?>(null) }
    
    var isWifiStreamingActive by remember { mutableStateOf(false) }
    var wifiFrequencyBand by remember { mutableStateOf("5.8 GHz Core") }
    var frameMirroringFps by remember { mutableStateOf(120) }

    var isUsbConnected by remember { mutableStateOf(false) }
    var usbDacSampleRate by remember { mutableStateOf("192 KHz / 32-bit (Double Precision)") }
    var usbUniversalSeriesType by remember { mutableStateOf("Type-C All-Series Universal Deck") }

    // Natural Movement inertial coordinates gyro steering
    var isGyroSteeringActive by remember { mutableStateOf(false) }
    var simulatedGyroTiltX by remember { mutableStateOf(0f) } // -45 to +45 deg tilt

    val scrollState = rememberScrollState()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF000511)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val titleText = when (currentLanguage) {
                com.example.ui.game.AppLanguage.ENGLISH -> "📻 NATIVE SYNTHESIZER AND MASTER MIXER"
                com.example.ui.game.AppLanguage.ESPANOL -> "📻 SINTETIZADOR NATIVO Y MEZCLADOR MAESTRO"
                com.example.ui.game.AppLanguage.PORTUGUES -> "📻 SINTETIZADOR NATIVO E MISTURADOR MASTER"
            }
            val descText = when (currentLanguage) {
                com.example.ui.game.AppLanguage.ENGLISH -> "Each song is naturally synthesized on-the-fly using additive sine-wave math."
                com.example.ui.game.AppLanguage.ESPANOL -> "Cada melodía se sintetiza proceduralmente usando síntesis aditiva sinusoidal."
                com.example.ui.game.AppLanguage.PORTUGUES -> "Cada trilha é sintetizada em tempo real via equações de ondas senoidais."
            }
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = descText,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )

            Row(modifier = Modifier.fillMaxWidth().height(260.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Left Panel: Frequency wave visual scope drawing
                val scopeText = when (currentLanguage) {
                    com.example.ui.game.AppLanguage.ENGLISH -> "🔊 Live Output Scope"
                    com.example.ui.game.AppLanguage.ESPANOL -> "🔊 Osciloscopio de Frecuencias"
                    com.example.ui.game.AppLanguage.PORTUGUES -> "🔊 Osciloscópio de Som"
                }
                Column(modifier = Modifier.weight(1.2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(scopeText, fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF020617), shape = RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0x3300E5FF), shape = RoundedCornerShape(8.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().testTag("sound_frequency_scope")) {
                            val w = size.width
                            val h = size.height

                            // Draw central baseline
                            drawLine(
                                color = Color(0x1A00E5FF),
                                start = Offset(0f, h / 2f),
                                end = Offset(w, h / 2f),
                                strokeWidth = 1f
                            )

                            // Mathematical additive rendering
                            val path = Path()
                            val points = 200
                            for (x in 0..points) {
                                val t = x.toFloat() / points
                                val posX = t * w
                                
                                // Base sine oscillation
                                var yOsc = sin(t * 4 * Math.PI + waveOffset)
                                
                                // Add harmonics based on active count
                                var harmonicFactor = 0.5
                                for (hIndex in 2..activeHarmonics) {
                                    yOsc += harmonicFactor * sin(t * 4 * Math.PI * hIndex + waveOffset * hIndex)
                                    harmonicFactor *= 0.5
                                }

                                val posY = h / 2f + (yOsc.toFloat() * (h * 0.28f) * volume)
                                if (x == 0) path.moveTo(posX, posY) else path.lineTo(posX, posY)
                            }

                            drawPath(
                                path = path,
                                color = when (theme) {
                                    "COSMIC_2" -> Color(0xFFE040FB)
                                    "OCEAN_VISTA" -> Color(0xFF00FF88)
                                    "TACTICAL" -> Color(0xFFFF1744)
                                    else -> Color(0xFF00E5FF)
                                },
                                style = Stroke(width = 2.5f)
                            )
                        }
                    }

                    val spectrumText = when (currentLanguage) {
                        com.example.ui.game.AppLanguage.ENGLISH -> "Frequency spectrum:"
                        com.example.ui.game.AppLanguage.ESPANOL -> "Espectro de salida:"
                        com.example.ui.game.AppLanguage.PORTUGUES -> "Espectro acústico:"
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(spectrumText, fontSize = 9.sp, color = Color(0xFF64748B))
                        Text(
                            when (theme) {
                                "COSMIC_2" -> "Perfect Harmonics (1:1, 3:2)"
                                "OCEAN_VISTA" -> "F-Lydian modulated drift"
                                "TACTICAL" -> "Sonic War Beacons"
                                else -> "Dorian Pentatonic Modality"
                            },
                            fontSize = 9.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Right Panel: Synthesizer Knobs & Sliders controls
                Column(modifier = Modifier.weight(1.5f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val synthTitleText = when (currentLanguage) {
                        com.example.ui.game.AppLanguage.ENGLISH -> "🎚️ Synthesizer Constants"
                        com.example.ui.game.AppLanguage.ESPANOL -> "🎚️ Constantes de Síntesis"
                        com.example.ui.game.AppLanguage.PORTUGUES -> "🎚️ Parâmetros do Sinal"
                    }
                    Text(synthTitleText, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)

                    // Volume Knob Slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val volText = com.example.ui.game.LanguageUtils.translate("volume", currentLanguage)
                        Text(volText, fontSize = 9.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(55.dp))
                        Slider(
                            value = volume,
                            onValueChange = onVolumeChange,
                            valueRange = 0f..1f,
                            modifier = Modifier.weight(1f).testTag("synth_volume_slider"),
                            colors = SliderDefaults.colors(activeTrackColor = Color(0xFF00E5FF), thumbColor = Color(0xFF00E5FF))
                        )
                    }

                    // Tempo sequence speed slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val tempText = com.example.ui.game.LanguageUtils.translate("tempo", currentLanguage)
                        Text(tempText, fontSize = 9.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(55.dp))
                        Slider(
                            value = tempoMs.toFloat(),
                            onValueChange = { onTempoChange(it.toLong()) },
                            valueRange = 150f..1200f,
                            modifier = Modifier.weight(1f).testTag("synth_tempo_slider"),
                            colors = SliderDefaults.colors(activeTrackColor = Color(0xFFFFD700), thumbColor = Color(0xFFFFD700))
                        )
                    }

                    // Harmonics multiplier slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val harText = when (currentLanguage) {
                            com.example.ui.game.AppLanguage.ENGLISH -> "Fourier"
                            com.example.ui.game.AppLanguage.ESPANOL -> "Ondas"
                            com.example.ui.game.AppLanguage.PORTUGUES -> "Fourier"
                        }
                        Text(harText, fontSize = 9.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(55.dp))
                        Slider(
                            value = activeHarmonics.toFloat(),
                            onValueChange = { onHarmonicsChange(it.toInt()) },
                            valueRange = 1f..6f,
                            steps = 4,
                            modifier = Modifier.weight(1f).testTag("synth_harmonics_slider"),
                            colors = SliderDefaults.colors(activeTrackColor = Color(0xFFE040FB), thumbColor = Color(0xFFE040FB))
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    val presetChoiceText = com.example.ui.game.LanguageUtils.translate("active_theme", currentLanguage)
                    Text(presetChoiceText, fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)

                    // Theme selector grid row
                    val themePresets = when (currentLanguage) {
                        com.example.ui.game.AppLanguage.ENGLISH -> listOf(
                            Triple("PRIMITIVE_1", "Bronze Age", "🪵"),
                            Triple("COSMIC_2", "Cosmic Core", "🌌"),
                            Triple("OCEAN_VISTA", "Deep Ocean", "🌊"),
                            Triple("TACTICAL", "War Sonar", "🛰️")
                        )
                        com.example.ui.game.AppLanguage.ESPANOL -> listOf(
                            Triple("PRIMITIVE_1", "Edad Bronce", "🪵"),
                            Triple("COSMIC_2", "Eje Cósmico", "🌌"),
                            Triple("OCEAN_VISTA", "Profundo", "🌊"),
                            Triple("TACTICAL", "Sonar Guerra", "🛰️")
                        )
                        com.example.ui.game.AppLanguage.PORTUGUES -> listOf(
                            Triple("PRIMITIVE_1", "Era Bronze", "🪵"),
                            Triple("COSMIC_2", "Eixo Cósmico", "🌌"),
                            Triple("OCEAN_VISTA", "Mar Profundo", "🌊"),
                            Triple("TACTICAL", "Sonar Guerra", "🛰️")
                        )
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(themePresets) { (themeId, label, emoji) ->
                            val selected = theme == themeId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) Color(0xFF00E5FF) else Color(0xFF111827))
                                    .clickable { onThemeChange(themeId) }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$emoji $label", fontSize = 9.sp, color = if (selected) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Divider separating Synthesizer from advanced Universal Interconnectivity Deck
            HorizontalDivider(color = Color(0x2200E5FF), thickness = 1.dp)

            // ---📡 UNIVERSAL HARDWARE INTERCONNECT & NATURAL MOTION PANEL ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF080F25), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0x3D00FF88), RoundedCornerShape(10.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val interceptLabel = when (currentLanguage) {
                        com.example.ui.game.AppLanguage.ENGLISH -> "🛰️ UNIVERSAL PERIPHERAL SOCKET"
                        com.example.ui.game.AppLanguage.ESPANOL -> "🛰️ PUERTO DE INTERCONEXIÓN UNIVERSAL"
                        com.example.ui.game.AppLanguage.PORTUGUES -> "🛰️ SOQUETE DE PERIFÉRICOS UNIVERSAL"
                    }
                    Text(
                        text = interceptLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF00FF88),
                        fontWeight = FontWeight.Bold
                    )
                    
                    val renderModeLabel = when (currentLanguage) {
                        com.example.ui.game.AppLanguage.ENGLISH -> "120FPS UHD MIRROR"
                        com.example.ui.game.AppLanguage.ESPANOL -> "UHD 120FPS ESPEJO"
                        com.example.ui.game.AppLanguage.PORTUGUES -> "ESPELHO UHD 120FPS"
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0x2200E5FF), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = renderModeLabel,
                            fontSize = 8.sp,
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                val explanationText = when (currentLanguage) {
                    com.example.ui.game.AppLanguage.ENGLISH -> "Open-ended equipment link configured. Stream live procedural audio synthesis, 4K render views, and physical control vectors directly to external devices."
                    com.example.ui.game.AppLanguage.ESPANOL -> "Enlace de equipamiento universal configurado. Transmite audio procedural, renders 4K y vectores de guiado físico directamente a controladores externos."
                    com.example.ui.game.AppLanguage.PORTUGUES -> "Link de equipamentos universais ativo. Transmita áudio sintetizado, renderização 4K e vetores de orientação de voo para dispositivos externos."
                }
                Text(
                    text = explanationText,
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 13.sp
                )

                // 2x2 Grid for Bluetooth, Wifi, USB, and Natural Gyro Controls
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    
                    // ROW 1: BLUETOOTH & WI-FI DIRECT SENSORY FEEDBACK
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card A: BLUETOOTH LC3 TRANSCEIVER
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C132E)),
                            border = BorderStroke(1.dp, if (isBluetoothActive) Color(0xFF00E5FF) else Color(0x1F94A3B8))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🔵 Bluetooth Link", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Switch(
                                        checked = isBluetoothActive,
                                        onCheckedChange = {
                                            isBluetoothActive = it
                                            if (!it) {
                                                connectedBluetoothDevice = null
                                                isScanningBluetooth = false
                                            }
                                        },
                                        modifier = Modifier.testTag("bluetooth_switch")
                                    )
                                }
                                
                                if (isBluetoothActive) {
                                    if (connectedBluetoothDevice == null) {
                                        Button(
                                            onClick = {
                                                isScanningBluetooth = true
                                                // Simulated device pick
                                                connectedBluetoothDevice = "Haptic Thruster-Glove X9"
                                                isScanningBluetooth = false
                                            },
                                            modifier = Modifier.fillMaxWidth().height(26.dp).testTag("bluetooth_scan_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF)),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(if (isScanningBluetooth) "SCANNING..." else "DISCOVER EQUIPMENT", fontSize = 8.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Column {
                                            Text("✔️ CONNECTED", fontSize = 8.sp, color = Color(0xFF00FF88), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            Text(connectedBluetoothDevice!!, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                            Text("LC3 Dual Audio & Sub-8ms Haptic Sync Active", fontSize = 8.sp, color = Color(0xFF94A3B8))
                                        }
                                    }
                                } else {
                                    Text("Transceiver Sleep Mode. Offline.", fontSize = 8.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        // Card B: WI-FI DIRECT TELEMETRY BROADCASTER (120Hz)
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C132E)),
                            border = BorderStroke(1.dp, if (isWifiStreamingActive) Color(0xFFE040FB) else Color(0x1F94A3B8))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📶 Wi-Fi 4K Mirror", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Switch(
                                        checked = isWifiStreamingActive,
                                        onCheckedChange = { isWifiStreamingActive = it },
                                        modifier = Modifier.testTag("wifi_switch")
                                    )
                                }

                                if (isWifiStreamingActive) {
                                    Column {
                                        Text("🛰️ BROADCASTING", fontSize = 8.sp, color = Color(0xFFE040FB), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Text("Node: 192.168.0.250:8080", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                                        
                                        // Slider to control FPS
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("${frameMirroringFps}FPS", fontSize = 8.sp, color = Color(0xFFFFD700), modifier = Modifier.width(34.dp))
                                            Slider(
                                                value = frameMirroringFps.toFloat(),
                                                onValueChange = { frameMirroringFps = it.toInt() },
                                                valueRange = 30f..150f,
                                                modifier = Modifier.weight(1f),
                                                colors = SliderDefaults.colors(activeTrackColor = Color(0xFFE040FB), thumbColor = Color(0xFFE040FB))
                                            )
                                        }
                                    }
                                } else {
                                    Text("Raw TCP Stream Idle.", fontSize = 8.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    // ROW 2: USB PLUG & NATURAL MOTION GYROSCOPE COORDINATES
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card C: UNIVERSAL USB-C OTG PLUG
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C132E)),
                            border = BorderStroke(1.dp, if (isUsbConnected) Color(0xFFFFD700) else Color(0x1F94A3B8))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🔌 USB Deck Plug", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Switch(
                                        checked = isUsbConnected,
                                        onCheckedChange = { isUsbConnected = it },
                                        modifier = Modifier.testTag("usb_switch")
                                    )
                                }

                                if (isUsbConnected) {
                                    Column {
                                        Text("🔌 USB HOTPLUG IN", fontSize = 8.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Text(usbUniversalSeriesType, fontSize = 9.sp, color = Color.White)
                                        
                                        Spacer(modifier = Modifier.height(2.dp))
                                        
                                        // Sample rate picker simulation
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF020617), RoundedCornerShape(4.dp))
                                                .clickable {
                                                    usbDacSampleRate = if (usbDacSampleRate.startsWith("192")) {
                                                        "384 KHz / 32-bit (Dual DAC)"
                                                    } else {
                                                        "192 KHz / 32-bit (Double Precision)"
                                                    }
                                                }
                                                .padding(4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(usbDacSampleRate, fontSize = 7.5.sp, color = Color(0xFF00FF88), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    Text("Unplugged. Direct jack is open.", fontSize = 8.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        // Card D: NATURAL MOVEMENT INERTIAL GYROSCOPE COORDINATOR
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C132E)),
                            border = BorderStroke(1.dp, if (isGyroSteeringActive) Color(0xFF00FF88) else Color(0x1F94A3B8))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🌀 Natural Gyro", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Switch(
                                        checked = isGyroSteeringActive,
                                        onCheckedChange = { isGyroSteeringActive = it },
                                        modifier = Modifier.testTag("gyro_switch")
                                    )
                                }

                                if (isGyroSteeringActive) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("INERTIAL SENSE ON", fontSize = 8.sp, color = Color(0xFF00FF88), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        
                                        // Circular bubble coordinate visualization
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(28.dp)
                                                .background(Color(0xFF020617), RoundedCornerShape(4.dp))
                                                .border(0.5.dp, Color(0xFF00FF88)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Render coordinate crosshairs
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                val w = size.width
                                                val h = size.height
                                                // Center Reticle
                                                drawLine(Color(0x3300FF88), Offset(w / 2f, 0f), Offset(w / 2f, h), strokeWidth = 1f)
                                                drawLine(Color(0x3300FF88), Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = 1f)
                                                
                                                // Horizontal level bubble movement
                                                val bubbleX = w / 2f + (simulatedGyroTiltX / 45f) * (w / 2.5f)
                                                drawCircle(
                                                    color = Color(0xFF00FF88),
                                                    radius = 4f,
                                                    center = Offset(bubbleX, h / 2f)
                                                )
                                            }
                                        }

                                        // Slider to simulate actual physical device tilting (Natural movement)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("${simulatedGyroTiltX.toInt()}°", fontSize = 8.sp, color = Color.White, modifier = Modifier.width(22.dp), fontFamily = FontFamily.Monospace)
                                            Slider(
                                                value = simulatedGyroTiltX,
                                                onValueChange = { simulatedGyroTiltX = it },
                                                valueRange = -45f..45f,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                } else {
                                    Text("Tilt Coordinates Disabled.", fontSize = 8.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
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
fun LogicalMathAdvisor(
    globalPopulation: Long,
    landerVelocityY: Float,
    activeVolume: Float,
    playerCredits: Int,
    playerAlloys: Int,
    regions: List<com.example.data.RegionEntity>,
    techs: List<com.example.data.TechEntity>,
    advisorReport: String?,
    isGeneratingAdvisor: Boolean,
    advisorErrorMessage: String?,
    customAdvisorQuery: String,
    onQueryChange: (String) -> Unit,
    onGenerateReport: (String) -> Unit,
    modifier: Modifier = Modifier,
    currentLanguage: com.example.ui.game.AppLanguage = com.example.ui.game.AppLanguage.ENGLISH,
    isHighThinkingModeEnabled: Boolean = true,
    onHighThinkingModeChange: (Boolean) -> Unit = {},
    isZkpEnabled: Boolean = true,
    onZkpChange: (Boolean) -> Unit = {},
    thinkingSteps: List<String> = emptyList()
) {
    val scrollState = rememberScrollState()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF090D16)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = com.example.ui.game.LanguageUtils.translate("advisor_headline", currentLanguage),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = com.example.ui.game.LanguageUtils.translate("advisor_desc", currentLanguage),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )

            // Let's draw the 3 Mathematical Cards:
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Logistic growth formula container math Card
                Card(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(com.example.ui.game.LanguageUtils.translate("growth_title", currentLanguage), fontSize = 10.sp, color = Color(0xFF00FF88), fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(Color(0xFF020617))
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "dP/dt=r·P(1-P/K)",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFFC107),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        val logisticDesc = when (currentLanguage) {
                            com.example.ui.game.AppLanguage.ENGLISH -> "K saturation limit is set to 5 Billion."
                            com.example.ui.game.AppLanguage.ESPANOL -> "Límite K de saturación es de 5 Mil Millones."
                            com.example.ui.game.AppLanguage.PORTUGUES -> "Limite K de saturação é 5 Bilhões."
                        }
                        Text(
                            text = logisticDesc,
                            fontSize = 8.sp,
                            color = Color(0xFFCBD5E1)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "+${(0.0018 * globalPopulation * (1.0 - (globalPopulation.toDouble() / 5000000000L))).toInt()}/s",
                            fontSize = 8.sp,
                            color = Color(0xFF00FF88),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 2. Space landing kinetic decelerating formula Card
                Card(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(com.example.ui.game.LanguageUtils.translate("thrust_title", currentLanguage) + " Log Math", fontSize = 10.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(Color(0xFF020617))
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "a=g-T/m - ln(1+dy)·v",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFFC107),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        val speedDesc = when (currentLanguage) {
                            com.example.ui.game.AppLanguage.ENGLISH -> "Tsiolkovsky dynamic mass ratio with logarithmic drag."
                            com.example.ui.game.AppLanguage.ESPANOL -> "Masa dinámica Tsiolkovsky con fricción de arrasto log."
                            com.example.ui.game.AppLanguage.PORTUGUES -> "Relação de Tsiolkovsky e arrasto logarítmico."
                        }
                        Text(
                            text = speedDesc,
                            fontSize = 8.sp,
                            color = Color(0xFFCBD5E1)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.2f", landerVelocityY)} m/s",
                            fontSize = 8.sp,
                            color = if (landerVelocityY > 3.8f) Color(0xFFFF1744) else Color(0xFF00FF88),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 3. Synth harmonic audio fourier formula Card
                Card(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(com.example.ui.game.LanguageUtils.translate("fourier_title", currentLanguage), fontSize = 10.sp, color = Color(0xFFE040FB), fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(Color(0xFF020617))
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "y(t)=∑[sin(h)/h]",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFFC107),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        val fourierDesc = when (currentLanguage) {
                            com.example.ui.game.AppLanguage.ENGLISH -> "Additive wave model for synthesized waveforms."
                            com.example.ui.game.AppLanguage.ESPANOL -> "Modelo de síntesis aditiva de ondas de sonido."
                            com.example.ui.game.AppLanguage.PORTUGUES -> "Modelo de áudio sintetizado em ondas aditivas."
                        }
                        Text(
                            text = fourierDesc,
                            fontSize = 8.sp,
                            color = Color(0xFFCBD5E1)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.0f", activeVolume * 100)}% Amp",
                            fontSize = 8.sp,
                            color = Color(0xFFE040FB),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ---📡 COSMIC TACTICAL AI UPLINK PANEL ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0x3300FF88))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = com.example.ui.game.LanguageUtils.translate("jarvis_headline", currentLanguage),
                        fontSize = 12.sp,
                        color = Color(0xFF00FF88),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    
                    Text(
                        text = com.example.ui.game.LanguageUtils.translate("jarvis_desc", currentLanguage),
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8)
                    )

                    // HIGH THINKING AI AND ZKP SETTINGS TOGGLES
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onHighThinkingModeChange(!isHighThinkingModeEnabled) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isHighThinkingModeEnabled) Color(0x3300FF88) else Color(0xFF1E293B),
                                contentColor = if (isHighThinkingModeEnabled) Color(0xFF00FF88) else Color(0xFF94A3B8)
                            ),
                            border = BorderStroke(1.dp, if (isHighThinkingModeEnabled) Color(0xFF00FF88) else Color(0x3394A3B8)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .testTag("high_thinking_ai_toggle"),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (isHighThinkingModeEnabled) "🧠 HIGH THINKING: ON" else "🧠 STANDARD AI",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Button(
                            onClick = { onZkpChange(!isZkpEnabled) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isZkpEnabled) Color(0x3300E5FF) else Color(0xFF1E293B),
                                contentColor = if (isZkpEnabled) Color(0xFF00E5FF) else Color(0xFF94A3B8)
                            ),
                            border = BorderStroke(1.dp, if (isZkpEnabled) Color(0xFF00E5FF) else Color(0x3394A3B8)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .testTag("zkp_verification_toggle"),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (isZkpEnabled) "🔐 ZKP VERIFY: ON" else "🔐 ZKP BYPASSED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Prompt field search/query
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customAdvisorQuery,
                            onValueChange = onQueryChange,
                            placeholder = { Text(com.example.ui.game.LanguageUtils.translate("jarvis_placeholder", currentLanguage), fontSize = 10.sp, color = Color(0xFF64748B)) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("ai_advisor_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF020617),
                                unfocusedContainerColor = Color(0xFF020617),
                                focusedBorderColor = Color(0xFF00FF88),
                                unfocusedBorderColor = Color(0x3394A3B8)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = { onGenerateReport(customAdvisorQuery) },
                            enabled = !isGeneratingAdvisor,
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("ai_advisor_generate_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00FF88),
                                disabledContainerColor = Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(
                                text = if (isGeneratingAdvisor) com.example.ui.game.LanguageUtils.translate("compiling", currentLanguage) else com.example.ui.game.LanguageUtils.translate("transmit", currentLanguage),
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Loading / Activity indicator state
                    if (isGeneratingAdvisor) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF020617), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0x3300FF88), RoundedCornerShape(6.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF00FF88)
                                )
                                Text(
                                    text = if (isHighThinkingModeEnabled) "COGNITIVE REASONING PIPELINE ACTIVE" else "PROCESSING DIRECTIVE...",
                                    fontSize = 9.sp,
                                    color = Color(0xFF00FF88),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // Render all processed thinking steps in order
                            thinkingSteps.forEach { step ->
                                Text(
                                    text = step,
                                    fontSize = 8.sp,
                                    color = Color(0xFF94A3B8),
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(start = 22.dp)
                                )
                            }
                        }
                    }

                    // Notice / Key status banner
                    if (advisorErrorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x1F00E5FF), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0x8000E5FF), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = advisorErrorMessage,
                                fontSize = 9.sp,
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Generated intelligence report output with scroll and styling
                    if (advisorReport != null && !isGeneratingAdvisor) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(6.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617))
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = com.example.ui.game.LanguageUtils.translate("broadcast_title", currentLanguage),
                                        fontSize = 11.sp,
                                        color = Color(0xFFFFD700),
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = com.example.ui.game.LanguageUtils.translate("decrypted", currentLanguage),
                                        fontSize = 8.sp,
                                        color = Color(0xFF00FF88),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .border(1.dp, Color(0xFF00FF88), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Text(
                                    text = advisorReport,
                                    fontSize = 10.sp,
                                    color = Color(0xFFECEFF1),
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    } else if (advisorReport == null && !isGeneratingAdvisor) {
                        // Empty guidance prompt placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x0F94A3B8), RoundedCornerShape(6.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = com.example.ui.game.LanguageUtils.translate("waiting_tactical", currentLanguage),
                                fontSize = 9.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
