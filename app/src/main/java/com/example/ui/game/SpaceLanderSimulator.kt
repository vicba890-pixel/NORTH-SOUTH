package com.example.ui.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RegionEntity
import kotlin.math.sin
import androidx.compose.foundation.gestures.detectTapGestures

@Composable
fun SpaceLanderSimulator(
    altitude: Float,
    speedY: Float,
    fuel: Float,
    isThrusting: Boolean,
    status: String,
    selectedLanderStyle: String,
    onLanderStyleChange: (String) -> Unit,
    shipWeight: Int,
    onShipWeightChange: (Int) -> Unit,
    targetRegion: RegionEntity?,
    onStartLanding: () -> Unit,
    onThrustChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    currentLanguage: com.example.ui.game.AppLanguage = com.example.ui.game.AppLanguage.ENGLISH,
    accelX: Float = 0f,
    accelY: Float = 0f,
    gyroX: Float = 0f,
    gyroY: Float = 0f,
    gyroZ: Float = 0f,
    landerOffsetX: Float = 0f,
    landerSpeedX: Float = 0f,
    landerWindX: Float = 0f,
    isSensorOverrideActive: Boolean = false,
    onToggleSensorOverride: (Boolean) -> Unit = {},
    onManualSteerLeft: () -> Unit = {},
    onManualSteerRight: () -> Unit = {},
    onReleaseSteer: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ThrusterFlame")
    val flameOscillation by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(130, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FlameSize"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val titleText = when (currentLanguage) {
                com.example.ui.game.AppLanguage.ENGLISH -> "🚀 ORBITAL SPACE-LAND DESCENT MODULE"
                com.example.ui.game.AppLanguage.ESPANOL -> "🚀 MÓDULO DE POUSO E DESCIDA ESPACIAL"
                com.example.ui.game.AppLanguage.PORTUGUES -> "🚀 MÓDULO DE POUSO E DESCIDA ESPACIAL"
            }
            val descText = when (currentLanguage) {
                com.example.ui.game.AppLanguage.ENGLISH -> "Establish permanent regional deep-space harbors to multiply raw metal and credit pipelines. Ensure stable touchdown speeds under 3.8 m/s."
                com.example.ui.game.AppLanguage.ESPANOL -> "Establece puertos espaciales para multiplicar recursos de cristal y créditos. Toca la superficie a menos de 3.8 m/s."
                com.example.ui.game.AppLanguage.PORTUGUES -> "Desenvolva ancoradouros no espaço profundo para duplicar ligas de metal. Pouse com segurança abaixo de 3.8 m/s."
            }
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = descText,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Row(modifier = Modifier.fillMaxWidth().height(290.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Left Panel: Cockpit Controls & Ship Configurations
                Column(
                    modifier = Modifier.weight(1.2f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val vesselClassText = when (currentLanguage) {
                        com.example.ui.game.AppLanguage.ENGLISH -> " Descent Vessel Class"
                        com.example.ui.game.AppLanguage.ESPANOL -> " Clase de Transbordador"
                        com.example.ui.game.AppLanguage.PORTUGUES -> " Classe do Transportador"
                    }
                    Text(
                        text = vesselClassText,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFE2E8F0),
                        fontWeight = FontWeight.Bold
                    )

                    // Craft Selection Tabs
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "ION_CAPSULE" to "Ion S-1",
                            "HEAVY_CARGO" to "Titan II",
                            "RETRO_POD" to "V-S Pod"
                        ).forEach { (style, name) ->
                            val selected = selectedLanderStyle == style
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) Color(0xFF00E5FF) else Color(0xFF1E293B))
                                    .clickable {
                                        onLanderStyleChange(style)
                                        onShipWeightChange(
                                            when (style) {
                                                "HEAVY_CARGO" -> 3500
                                                "RETRO_POD" -> 850
                                                else -> 1500
                                            }
                                        )
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 11.sp,
                                    color = if (selected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Stats indicators
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF020617), shape = RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0x2600B0FF), shape = RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val feedTitle = when (currentLanguage) {
                            com.example.ui.game.AppLanguage.ENGLISH -> "📊 Telemetry Feed"
                            com.example.ui.game.AppLanguage.ESPANOL -> "📊 Flujo de Telemetría"
                            com.example.ui.game.AppLanguage.PORTUGUES -> "📊 Canal de Telemetria"
                        }
                        val dryWeightLabel = when (currentLanguage) {
                            com.example.ui.game.AppLanguage.ENGLISH -> "Engine Dry Weight:"
                            com.example.ui.game.AppLanguage.ESPANOL -> "Masa Seca de Nave:"
                            com.example.ui.game.AppLanguage.PORTUGUES -> "Massa Seca da Nave:"
                        }
                        val propulsionLabel = when (currentLanguage) {
                            com.example.ui.game.AppLanguage.ENGLISH -> "Retro Propulsion:"
                            com.example.ui.game.AppLanguage.ESPANOL -> "Retropropulsión:"
                            com.example.ui.game.AppLanguage.PORTUGUES -> "Retropropulsão:"
                        }
                        val gravityLabel = when (currentLanguage) {
                            com.example.ui.game.AppLanguage.ENGLISH -> "Gravity Constant:"
                            com.example.ui.game.AppLanguage.ESPANOL -> "Gravedad Regional:"
                            com.example.ui.game.AppLanguage.PORTUGUES -> "Constante Gravitacional:"
                        }
                        val fuelLabel = when (currentLanguage) {
                            com.example.ui.game.AppLanguage.ENGLISH -> "Fuel remaining:"
                            com.example.ui.game.AppLanguage.ESPANOL -> "Combustible Restante:"
                            com.example.ui.game.AppLanguage.PORTUGUES -> "Combustível Restante:"
                        }
                        val propulsionType = if (selectedLanderStyle == "ION_CAPSULE") {
                            when (currentLanguage) {
                                com.example.ui.game.AppLanguage.ENGLISH -> "Ion Grid"
                                com.example.ui.game.AppLanguage.ESPANOL -> "Filtro de Iones"
                                com.example.ui.game.AppLanguage.PORTUGUES -> "Propulsor Iônico"
                            }
                        } else {
                            when (currentLanguage) {
                                com.example.ui.game.AppLanguage.ENGLISH -> "Chemical Thruster"
                                com.example.ui.game.AppLanguage.ESPANOL -> "Motor Químico"
                                com.example.ui.game.AppLanguage.PORTUGUES -> "Combustão Química"
                            }
                        }

                        Text(
                            text = feedTitle,
                            fontSize = 10.sp,
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(dryWeightLabel, fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Text("$shipWeight kg", fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(propulsionLabel, fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Text(propulsionType, fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(gravityLabel, fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Text(
                                when (selectedLanderStyle) {
                                    "HEAVY_CARGO" -> "1.62 m/s²"
                                    "RETRO_POD" -> "0.24 m/s²"
                                    else -> "1.25 m/s²"
                                },
                                fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace
                            )
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(fuelLabel, fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Text("${fuel.toInt()}%", fontSize = 10.sp, color = if (fuel < 25f) Color.Red else Color.Green, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        // Mini Fuel gauge bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Color(0xFF1E293B), RoundedCornerShape(2.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fuel / 100f)
                                    .background(if (fuel < 25f) Color(0xFFFF1744) else Color(0xFF00FF88), RoundedCornerShape(2.dp))
                            )
                        }

                        // Live physical tilt and rotation sensor readings
                        Divider(color = Color(0x1F94A3B8), thickness = 1.dp)
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Tilt Sensor X:", fontSize = 9.sp, color = Color(0xFF94A3B8))
                            Text("${String.format("%.2f", accelX)} m/s²", fontSize = 9.sp, color = Color(0xFF00E5FF), fontFamily = FontFamily.Monospace)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Gyro Rot Z:", fontSize = 9.sp, color = Color(0xFF94A3B8))
                            Text("${String.format("%.2f", gyroZ)} rad/s", fontSize = 9.sp, color = Color(0xFF00E5FF), fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Action launch trigger
                    Button(
                        onClick = onStartLanding,
                        modifier = Modifier.fillMaxWidth().testTag("launch_descent_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (status == "PROGRESS") Color(0xFFFF1744) else Color(0xFFFFD700)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = status != "PROGRESS"
                    ) {
                        val launchBtnText = if (status == "PROGRESS") {
                            when (currentLanguage) {
                                com.example.ui.game.AppLanguage.ENGLISH -> "DESCENT COMMENCED"
                                com.example.ui.game.AppLanguage.ESPANOL -> "DESCENSO INICIADO"
                                com.example.ui.game.AppLanguage.PORTUGUES -> "DESCIDA INICIADA"
                            }
                        } else {
                            when (currentLanguage) {
                                com.example.ui.game.AppLanguage.ENGLISH -> "START INERTIAL DESCENT"
                                com.example.ui.game.AppLanguage.ESPANOL -> "DESCONECTAR ÓRBITA"
                                com.example.ui.game.AppLanguage.PORTUGUES -> "INICIAR RETRO POUSO"
                            }
                        }
                        Text(
                            text = launchBtnText,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Right Panel: Visual Canvas & Descent telemetry screen
                Column(
                    modifier = Modifier.weight(1.5f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Altitude Radar Window Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF020617), shape = RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0x40FFFFFF), shape = RoundedCornerShape(8.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().testTag("descent_radar_canvas")) {
                            val w = size.width
                            val h = size.height

                            // 1. Draw static high-fidelity tactical grid pattern
                            val cols = 8
                            val rows = 6
                            for (c in 1 until cols) {
                                val gridX = (w / cols) * c
                                drawLine(
                                    color = Color(0x1200E5FF),
                                    start = Offset(gridX, 0f),
                                    end = Offset(gridX, h),
                                    strokeWidth = 1f
                                )
                            }
                            for (r in 1 until rows) {
                                val gridY = (h / rows) * r
                                drawLine(
                                    color = Color(0x1200E5FF),
                                    start = Offset(0f, gridY),
                                    end = Offset(w, gridY),
                                    strokeWidth = 1f
                                )
                            }

                            // 2. Altitude Radar Boundary and Atmospheric Heat scale (Left sidebar)
                            val barX = 14f
                            val barW = 6f
                            val barTop = 30f
                            val barBottom = h - 20f
                            
                            // Friction heat gradient: Blue -> Orange -> Red
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF00E5FF), Color(0xFFFF9F00), Color(0xFFFF1744)),
                                    startY = barTop,
                                    endY = barBottom
                                ),
                                topLeft = Offset(barX, barTop),
                                size = Size(barW, barBottom - barTop)
                            )
                            
                            // Draw horizontal tick indicators for atmosphere thickness
                            for (tick in 0..4) {
                                val ty = barTop + (barBottom - barTop) * (tick / 4f)
                                drawLine(
                                    color = Color(0x7F94A3B8),
                                    start = Offset(barX + barW, ty),
                                    end = Offset(barX + barW + 5f, ty),
                                    strokeWidth = 1.5f
                                )
                            }

                            // 3. Draw Target Landing Pad Pad Zone
                            val padWidth = w * 0.42f
                            val padHeight = 8f
                            val padX = (w - padWidth) / 2f
                            val padY = h - 20f

                            // Pad border glow
                            drawRect(
                                color = Color(0xFF00FF88),
                                topLeft = Offset(padX, padY),
                                size = Size(padWidth, padHeight),
                                style = Stroke(width = 2.5f)
                            )

                            // Crosshair locking lines for high-precision landing target guides
                            drawLine(
                                color = Color(0x3D00FF88),
                                start = Offset(padX, padY),
                                end = Offset(padX - 25f, padY - 25f),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                            drawLine(
                                color = Color(0x3D00FF88),
                                start = Offset(padX + padWidth, padY),
                                end = Offset(padX + padWidth + 25f, padY - 25f),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )

                            // Pad target pulse dots
                            drawCircle(
                                color = Color(0xFF00FF88),
                                radius = 5f,
                                center = Offset(w / 2f, padY)
                            )

                            // 4. Render Lander Capsule in Real-Time
                            val landerHeight = 24f
                            val landerWidth = 28f
                            val minAltitudePosition = 30f
                            val maxAltitudePosition = padY - landerHeight

                            val landerY = minAltitudePosition + (1f - (altitude / 120f)) * (maxAltitudePosition - minAltitudePosition)
                            // Apply lateral shift from physical accelerometer tilt/drift offset
                            val maxOffsetPx = w * 0.40f
                            val landerX = (w - landerWidth) / 2f + (landerOffsetX / 100f) * maxOffsetPx

                            // Draw Atmospheric indicator needle pointing to friction index
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(barX - 4f, landerY),
                                size = Size(barW + 8f, 2f)
                            )

                            if (status == "PROGRESS" || status == "LANDED" || status == "CRASHED") {
                                
                                // CALCULATE PRECISION LOOKAHEAD TRAJECTORY PATH
                                val gravity = when (selectedLanderStyle) {
                                    "RETRO_POD" -> 0.40f
                                    "HEAVY_CARGO" -> 1.62f
                                    else -> 1.15f
                                }
                                val baseThrust = when (selectedLanderStyle) {
                                    "RETRO_POD" -> 2.60f
                                    "HEAVY_CARGO" -> 12.5f
                                    else -> 4.90f
                                }
                                val totalMass = (shipWeight / 1000f) + (fuel / 100f) * ((shipWeight / 1000f) * 0.35f)
                                
                                var tempAltitude = altitude
                                var tempSpeedY = speedY
                                var tempFuel = fuel
                                var tempOffsetX = landerOffsetX
                                var tempSpeedX = landerSpeedX
                                var trajectoryCrashed = false
                                val trajectoryPoints = mutableListOf<Offset>()
                                
                                for (step in 1..15) {
                                    val simulatedThrust = isThrusting && tempFuel > 0f
                                    val thrustForce = if (simulatedThrust) {
                                        tempFuel = (tempFuel - (if (selectedLanderStyle == "RETRO_POD") 0.9f else if (selectedLanderStyle == "HEAVY_CARGO") 1.4f else 1.1f)).coerceAtLeast(0f)
                                        baseThrust / totalMass
                                    } else {
                                        0f
                                    }
                                    
                                    val logAtmDensity = kotlin.math.ln(1.0f + (120f - tempAltitude) / 12f) * 0.08f
                                    val dragDecel = (logAtmDensity * tempSpeedY).coerceIn(-0.5f, 1.2f)
                                    val netAcceleration = gravity - thrustForce - dragDecel
                                    tempSpeedY += netAcceleration * 0.12f
                                    tempAltitude -= tempSpeedY * 0.12f
                                    
                                    // Simulate horizontal trajectory progression under current physical properties
                                    val tiltInput = -accelX
                                    val lateralForce = tiltInput * 0.45f
                                    val lateralDrag = -0.12f * tempSpeedX
                                    val netAccX = lateralForce + landerWindX + lateralDrag
                                    tempSpeedX += netAccX * 0.12f
                                    tempOffsetX += tempSpeedX * 12f * 0.12f
                                    tempOffsetX = tempOffsetX.coerceIn(-100f, 100f)

                                    val predictedY = minAltitudePosition + (1f - (tempAltitude / 120f)) * (maxAltitudePosition - minAltitudePosition)
                                    val predictedX = (w - landerWidth) / 2f + (tempOffsetX / 100f) * maxOffsetPx + landerWidth / 2f
                                    trajectoryPoints.add(Offset(predictedX, predictedY))
                                    
                                    if (tempAltitude <= 0f) {
                                        val isAligned = kotlin.math.abs(tempOffsetX) <= 35f
                                        if (tempSpeedY > 3.8f || !isAligned) {
                                            trajectoryCrashed = true
                                        }
                                        break
                                    }
                                }
                                
                                // Draw glowing trajectory path forecast
                                if (trajectoryPoints.size > 1 && status == "PROGRESS") {
                                    val forecastColor = if (trajectoryCrashed) Color(0xFFFF4500) else Color(0xFF00FF88)
                                    val trajectoryPath = Path().apply {
                                        moveTo(landerX + landerWidth / 2f, landerY + landerHeight)
                                        trajectoryPoints.forEach { pt ->
                                            lineTo(pt.x, pt.y)
                                        }
                                    }
                                    drawPath(
                                        path = trajectoryPath,
                                        color = forecastColor,
                                        style = Stroke(
                                            width = 3f,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                                        )
                                    )
                                }

                                // Interactive Target Bracket locks around active vehicle
                                val bracketSize = 8f
                                val padOffset = 6f
                                val bracketColor = if (status == "CRASHED") Color(0xFFFF1744) else if (speedY > 3.8f) Color(0xFFFFA500) else Color(0xFF00E5FF)
                                
                                // Left Target Gimbals
                                drawLine(bracketColor, Offset(landerX - padOffset, landerY), Offset(landerX - padOffset + bracketSize, landerY), strokeWidth = 2f)
                                drawLine(bracketColor, Offset(landerX - padOffset, landerY), Offset(landerX - padOffset, landerY + bracketSize), strokeWidth = 2f)
                                drawLine(bracketColor, Offset(landerX - padOffset, landerY + landerHeight), Offset(landerX - padOffset + bracketSize, landerY + landerHeight), strokeWidth = 2f)
                                drawLine(bracketColor, Offset(landerX - padOffset, landerY + landerHeight), Offset(landerX - padOffset, landerY + landerHeight - bracketSize), strokeWidth = 2f)
                                
                                // Right Target Gimbals
                                drawLine(bracketColor, Offset(landerX + landerWidth + padOffset, landerY), Offset(landerX + landerWidth + padOffset - bracketSize, landerY), strokeWidth = 2f)
                                drawLine(bracketColor, Offset(landerX + landerWidth + padOffset, landerY), Offset(landerX + landerWidth + padOffset, landerY + bracketSize), strokeWidth = 2f)
                                drawLine(bracketColor, Offset(landerX + landerWidth + padOffset, landerY + landerHeight), Offset(landerX + landerWidth + padOffset - bracketSize, landerY + landerHeight), strokeWidth = 2f)
                                drawLine(bracketColor, Offset(landerX + landerWidth + padOffset, landerY + landerHeight), Offset(landerX + landerWidth + padOffset, landerY + landerHeight - bracketSize), strokeWidth = 2f)

                                // Draw Lander Body Shape and flames, rotated dynamically with device physical tilt
                                val tiltAngleDegrees = (-accelX * 3.5f).coerceIn(-30f, 30f)
                                rotate(degrees = tiltAngleDegrees, pivot = Offset(landerX + landerWidth / 2f, landerY + landerHeight / 2f)) {
                                    // Draw Lander Body Shape
                                    val landerBody = Path().apply {
                                        moveTo(landerX + landerWidth / 2f, landerY)
                                        cubicTo(
                                            landerX, landerY + 4f,
                                            landerX + 2f, landerY + 12f,
                                            landerX + 2f, landerY + 16f
                                        )
                                        lineTo(landerX - 4f, landerY + 22f)
                                        lineTo(landerX + 4f, landerY + 20f)
                                        lineTo(landerX + landerWidth - 4f, landerY + 20f)
                                        lineTo(landerX + landerWidth + 4f, landerY + 22f)
                                        lineTo(landerX + landerWidth - 2f, landerY + 16f)
                                        cubicTo(
                                            landerX + landerWidth - 2f, landerY + 12f,
                                            landerX + landerWidth, landerY + 4f,
                                            landerX + landerWidth / 2f, landerY
                                        )
                                    }
                                    drawPath(
                                        path = landerBody,
                                        color = if (status == "CRASHED") Color(0xFFFF1744) else Color(0xFF00E5FF)
                                    )

                                    // Thruster Flame visual if booster triggered with procedural sparkling sparks
                                    if (isThrusting && fuel > 0f && status == "PROGRESS") {
                                        val flamePath = Path().apply {
                                            moveTo(landerX + 7f, landerY + 18f)
                                            lineTo(landerX + landerWidth / 2f, landerY + 18f + 18f * flameOscillation)
                                            lineTo(landerX + landerWidth - 7f, landerY + 18f)
                                            close()
                                        }
                                        drawPath(
                                            path = flamePath,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color(0xFFFFD700), Color(0xFFFF4500), Color(0x00FF4500))
                                            )
                                        )

                                        // Spawning retro propulsion embers dynamically based on oscillation values
                                        val sparksOffset = (flameOscillation - 0.8f) * 40f
                                        drawCircle(
                                            color = Color(0xFFFFC107),
                                            radius = 2.5f,
                                            center = Offset(landerX + landerWidth / 2f, landerY + 25f + sparksOffset)
                                        )
                                        drawCircle(
                                            color = Color(0xFFFF5722),
                                            radius = 1.5f,
                                            center = Offset(landerX + landerWidth / 2f - 10f, landerY + 28f + sparksOffset * 0.7f)
                                        )
                                        drawCircle(
                                            color = Color(0xFFFFE082),
                                            radius = 1.5f,
                                            center = Offset(landerX + landerWidth / 2f + 10f, landerY + 28f + sparksOffset * 0.7f)
                                        )
                                    }
                                }
                            } else {
                                // Orbit state text indicator on Radar screen
                                drawCircle(
                                    color = Color(0x2200E5FF),
                                    radius = 45f,
                                    center = Offset(w / 2f, h / 2f)
                                )
                                drawCircle(
                                    color = Color(0x4400E5FF),
                                    radius = 2f,
                                    center = Offset(w / 2f, h / 2f)
                                )
                            }
                        }

                         // Cockpit HUD digits display overlays
                         Column(
                             modifier = Modifier
                                 .align(Alignment.TopEnd)
                                 .padding(8.dp)
                                 .background(Color(0xE6020617), shape = RoundedCornerShape(4.dp))
                                 .padding(4.dp)
                         ) {
                             Text(
                                 "ALT: ${String.format("%.1f", altitude)} m",
                                 color = Color.White,
                                 fontFamily = FontFamily.Monospace,
                                 fontSize = 10.sp,
                                 fontWeight = FontWeight.Bold
                             )
                             Text(
                                 "SPD: ${String.format("%.1f", speedY)} m/s",
                                 color = if (speedY > 3.8f) Color(0xFFFF4500) else Color(0xFF00FF88),
                                 fontFamily = FontFamily.Monospace,
                                 fontSize = 10.sp,
                                 fontWeight = FontWeight.Bold
                             )
                             Text(
                                 "DRIFT: ${String.format("%.1f", landerOffsetX)} m",
                                 color = if (kotlin.math.abs(landerOffsetX) <= 35f) Color(0xFF00FF88) else Color(0xFFFF1744),
                                 fontFamily = FontFamily.Monospace,
                                 fontSize = 10.sp,
                                 fontWeight = FontWeight.Bold
                             )
                             Text(
                                 "WIND: ${String.format("%.1f", landerWindX)} m/s",
                                 color = Color(0xFF00E5FF),
                                 fontFamily = FontFamily.Monospace,
                                 fontSize = 10.sp,
                                 fontWeight = FontWeight.Bold
                             )
                         }

                        // Simulation Status Text message overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(8.dp)
                                .background(Color(0xE60B1329), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            val statusOverlayText = when (status) {
                                "ORBIT" -> {
                                    when (currentLanguage) {
                                        com.example.ui.game.AppLanguage.ENGLISH -> "WARP VECTOR: STANDBY"
                                        com.example.ui.game.AppLanguage.ESPANOL -> "VECTOR DE CURVATURA: EN ESCERA"
                                        com.example.ui.game.AppLanguage.PORTUGUES -> "VETOR DE DOBRA: EM ESPERA"
                                    }
                                }
                                "PROGRESS" -> {
                                    when (currentLanguage) {
                                        com.example.ui.game.AppLanguage.ENGLISH -> "PROPULSION SYSTEM: DESCENT"
                                        com.example.ui.game.AppLanguage.ESPANOL -> "SISTEMA DE RETROPROPULSIÓN: ACTIVO"
                                        com.example.ui.game.AppLanguage.PORTUGUES -> "SISTEMA DE RETROPULSÃO: ATIVO"
                                    }
                                }
                                "LANDED" -> {
                                    when (currentLanguage) {
                                        com.example.ui.game.AppLanguage.ENGLISH -> "SUCCESSFUL TOUCHDOWN!"
                                        com.example.ui.game.AppLanguage.ESPANOL -> "¡ALUNIZAJE EXITOSO EN EL SECTOR!"
                                        com.example.ui.game.AppLanguage.PORTUGUES -> "¡POUSO ESTÁVEL CONCLUÍDO!"
                                    }
                                }
                                "CRASHED" -> {
                                    when (currentLanguage) {
                                        com.example.ui.game.AppLanguage.ENGLISH -> "STRUCTURE CRITICAL: DESTROYED"
                                        com.example.ui.game.AppLanguage.ESPANOL -> "ESTRUCTURA BALÍSTICA: DESTRUIDA"
                                        com.example.ui.game.AppLanguage.PORTUGUES -> "ESTRUTURA CRÍTICA: DESTRUÍDA"
                                    }
                                }
                                else -> {
                                    when (currentLanguage) {
                                        com.example.ui.game.AppLanguage.ENGLISH -> "TELEMETRY LINK STATUS: ACTIVE"
                                        com.example.ui.game.AppLanguage.ESPANOL -> "LÍNEA DE TELEMETRÍA EN DIRECTO"
                                        com.example.ui.game.AppLanguage.PORTUGUES -> "LINK DE TELEMETRIA: SINAL ATIVO"
                                    }
                                }
                            }
                            Text(
                                text = statusOverlayText,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = when (status) {
                                    "LANDED" -> Color(0xFF00FF88)
                                    "CRASHED" -> Color(0xFFFF1744)
                                    else -> Color(0xFF00E5FF)
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Interactive Thruster Boost Panel (Double interactive trigger)
                    if (status == "PROGRESS") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Touch Retro Booster pad (Continuous click press)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(55.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFF1744))
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                onThrustChange(true)
                                                tryAwaitRelease()
                                                onThrustChange(false)
                                            }
                                        )
                                    }
                                    .testTag("booster_thruster_pad"),
                                contentAlignment = Alignment.Center
                            ) {
                                val holdLabel = when (currentLanguage) {
                                    com.example.ui.game.AppLanguage.ENGLISH -> "🔥 [ HOLD ]"
                                    com.example.ui.game.AppLanguage.ESPANOL -> "🔥 [ APRETAR ]"
                                    com.example.ui.game.AppLanguage.PORTUGUES -> "🔥 [ SEGURAR ]"
                                }
                                val bstLabel = when (currentLanguage) {
                                    com.example.ui.game.AppLanguage.ENGLISH -> "IGNITE RETRO BOOSTER"
                                    com.example.ui.game.AppLanguage.ESPANOL -> "IGNICIÓN DE PROPULSOR"
                                    com.example.ui.game.AppLanguage.PORTUGUES -> "ATIVAR SISTEMA DE RETRO POUSO"
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(holdLabel, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(bstLabel, fontSize = 8.sp, color = Color.White.copy(alpha = 0.8f))
                                }
                            }

                            // Dynamic Steering control systems (Tilt Sensors vs Manual Joystick)
                            Row(
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSensorOverrideActive) Color(0xFF0F766E) else Color(0x3300FF88))
                                        .border(1.dp, if (isSensorOverrideActive) Color(0xFF14B8A6) else Color(0xFF00FF88), RoundedCornerShape(6.dp))
                                        .clickable { onToggleSensorOverride(!isSensorOverrideActive) }
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isSensorOverrideActive) "🕹️ MANUAL JOY" else "🛰️ TILT SENSORS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.testTag("sensor_override_toggle")
                                    )
                                }

                                Button(
                                    onClick = {},
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    onManualSteerLeft()
                                                    tryAwaitRelease()
                                                    onReleaseSteer()
                                                }
                                            )
                                        }
                                        .testTag("steer_left_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1E293B),
                                        contentColor = Color.White,
                                        disabledContainerColor = Color(0xFF020617),
                                        disabledContentColor = Color(0xFF475569)
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    enabled = isSensorOverrideActive,
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("◀ STEER L", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {},
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    onManualSteerRight()
                                                    tryAwaitRelease()
                                                    onReleaseSteer()
                                                }
                                            )
                                        }
                                        .testTag("steer_right_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1E293B),
                                        contentColor = Color.White,
                                        disabledContainerColor = Color(0xFF020617),
                                        disabledContentColor = Color(0xFF475569)
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    enabled = isSensorOverrideActive,
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("STEER R ▶", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // Safe informational advice when not flying
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(55.dp)
                                .background(Color(0x2694A3B8), shape = RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val tipText = when (currentLanguage) {
                                com.example.ui.game.AppLanguage.ENGLISH -> "💡 Tip: Select Lunar Zone Alpha in the main navigation map to make space harbor structures, increasing credits yields dramatically!"
                                com.example.ui.game.AppLanguage.ESPANOL -> "💡 Consejo: Selecciona Zona Luna Alfa en el mapa de navegación para construir puertos espaciales y elevar la ganancia de créditos de aleación."
                                com.example.ui.game.AppLanguage.PORTUGUES -> "💡 Dica: Escolha Zona Lunar Alvo no mapa de navegação para criar ancoradouros estelares e bombear os rendimentos de créditos obtidos."
                            }
                            Text(
                                text = tipText,
                                fontSize = 9.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}


