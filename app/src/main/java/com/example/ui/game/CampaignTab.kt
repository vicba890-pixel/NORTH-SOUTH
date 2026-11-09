package com.example.ui.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CampaignSaveEntity
import com.example.data.FirebaseUserStats
import com.example.data.FirebaseTacticalProgress
import com.example.data.MultiplayerMatchStats
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TacticalConquestHeaderIllustration(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "HeaderIllustration")
    
    // Rotating radar sweep angle
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    // Pulsing nodes
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(Color(0xFF020617))
            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f
            val radius = minOf(width, height) * 0.45f

            // 1. Draw glowing digital cyber grid lines
            val gridSpacing = 24f
            for (x in 0..(width / gridSpacing).toInt()) {
                val xPos = x * gridSpacing
                drawLine(
                    color = Color(0x0A00E5FF),
                    start = Offset(xPos, 0f),
                    end = Offset(xPos, height),
                    strokeWidth = 1f
                )
            }
            for (y in 0..(height / gridSpacing).toInt()) {
                val yPos = y * gridSpacing
                drawLine(
                    color = Color(0x0A00E5FF),
                    start = Offset(0f, yPos),
                    end = Offset(width, yPos),
                    strokeWidth = 1f
                )
            }

            // 2. Draw outer HUD scopes
            drawCircle(
                color = Color(0x1F00E5FF),
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 15f), 0f))
            )

            drawCircle(
                color = Color(0x2F00E5FF),
                radius = radius * 0.8f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1f)
            )

            drawCircle(
                color = Color(0x0F00E5FF),
                radius = radius * 0.4f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1f)
            )

            // 3. Draw horizontal and vertical main radar axises
            drawLine(
                color = Color(0x1F00E5FF),
                start = Offset(centerX - radius - 20f, centerY),
                end = Offset(centerX + radius + 20f, centerY),
                strokeWidth = 1f
            )
            drawLine(
                color = Color(0x1F00E5FF),
                start = Offset(centerX, centerY - radius - 20f),
                end = Offset(centerX, centerY + radius + 20f),
                strokeWidth = 1f
            )

            // 4. Draw glowing rotating sweeping sector
            val sweepRad = Math.toRadians(sweepAngle.toDouble())
            val sweepEndX = centerX + radius * Math.cos(sweepRad).toFloat()
            val sweepEndY = centerY + radius * Math.sin(sweepRad).toFloat()
            drawLine(
                color = Color(0x7700E5FF),
                start = Offset(centerX, centerY),
                end = Offset(sweepEndX, sweepEndY),
                strokeWidth = 2f
            )

            // Transparent sweeping sector glow
            val arcSize = radius * 2
            drawArc(
                color = Color(0x1100E5FF),
                startAngle = sweepAngle - 30f,
                sweepAngle = 30f,
                useCenter = true,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(arcSize, arcSize)
            )

            // 5. Draw tactical node dots and orbit vectors
            val nodeA = Offset(centerX - radius * 0.5f, centerY - radius * 0.3f)
            val nodeB = Offset(centerX + radius * 0.6f, centerY + radius * 0.2f)
            val nodeC = Offset(centerX + radius * 0.1f, centerY - radius * 0.5f)
            val nodeD = Offset(centerX - radius * 0.3f, centerY + radius * 0.4f)

            // Vector lines connecting tactical nodes
            drawLine(Color(0x2F00FF88), nodeA, nodeC, strokeWidth = 1.5f)
            drawLine(Color(0x2F00FF88), nodeC, nodeB, strokeWidth = 1.5f)
            drawLine(Color(0x2F00FF88), nodeB, nodeD, strokeWidth = 1.5f)
            drawLine(Color(0x2F00FF88), nodeD, nodeA, strokeWidth = 1.5f)

            // Pulsing target nodes
            val nodes = listOf(nodeA, nodeB, nodeC, nodeD)
            nodes.forEachIndexed { index, node ->
                val color = if (index % 2 == 0) Color(0xFF00FF88) else Color(0xFFFF3366)
                
                // Outer pulse circle
                drawCircle(
                    color = color.copy(alpha = 0.3f),
                    radius = 8.dp.toPx() * pulseScale,
                    center = node
                )

                // Inner core
                drawCircle(
                    color = color,
                    radius = 3.dp.toPx(),
                    center = node
                )

                // Draw target crosshairs for node A
                if (index == 0) {
                    val cr = 8.dp.toPx()
                    drawLine(color, Offset(node.x - cr, node.y), Offset(node.x + cr, node.y), strokeWidth = 1f)
                    drawLine(color, Offset(node.x, node.y - cr), Offset(node.x, node.y + cr), strokeWidth = 1f)
                }
            }

            // 6. Draw glowing global grid outline mimicking geographic continents
            val path = Path()
            // Stylized hexagon/continents outline
            path.moveTo(centerX - radius * 0.7f, centerY)
            path.lineTo(centerX - radius * 0.5f, centerY - radius * 0.4f)
            path.lineTo(centerX - radius * 0.2f, centerY - radius * 0.3f)
            path.lineTo(centerX, centerY - radius * 0.7f)
            path.lineTo(centerX + radius * 0.3f, centerY - radius * 0.4f)
            path.lineTo(centerX + radius * 0.7f, centerY - radius * 0.1f)
            path.lineTo(centerX + radius * 0.6f, centerY + radius * 0.5f)
            path.lineTo(centerX + radius * 0.1f, centerY + radius * 0.3f)
            path.lineTo(centerX - radius * 0.3f, centerY + radius * 0.6f)
            path.lineTo(centerX - radius * 0.5f, centerY + radius * 0.2f)
            path.close()

            drawPath(
                path = path,
                color = Color(0x2F00E5FF),
                style = Stroke(width = 1f)
            )
        }

        // Overlay text and indicators to look incredibly high-tech
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Text(
                text = "🛰️ SAT-LINK: ACTIVE",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00FF88),
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "SECTOR: 10-A // ORBITAL",
                fontSize = 7.sp,
                color = Color(0xFF94A3B8),
                fontFamily = FontFamily.Monospace
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "COORDINATES DETECTED",
                fontSize = 7.sp,
                color = Color(0xFFFFB74D),
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "LAT: 34.0522° N // LON: 118.2437° W",
                fontSize = 7.sp,
                color = Color(0xFF94A3B8),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun CampaignTab(
    campaignSaves: List<CampaignSaveEntity>,
    onSave: (Int) -> Unit,
    onLoad: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onResetCampaign: () -> Unit,
    currentLanguage: AppLanguage,
    firebaseUser: FirebaseUserStats? = null,
    firebaseAuthError: String? = null,
    firebaseAuthLoading: Boolean = false,
    firebaseSyncLoading: Boolean = false,
    firebaseSyncSuccessMessage: String? = null,
    firebaseSyncErrorMessage: String? = null,
    firebaseProgressList: List<FirebaseTacticalProgress> = emptyList(),
    firebaseMatchStatsList: List<MultiplayerMatchStats> = emptyList(),
    isRealFirebaseActive: Boolean = false,
    onRegisterUser: (String, String) -> Unit = { _, _ -> },
    onLoginUser: (String, String) -> Unit = { _, _ -> },
    onLogoutUser: () -> Unit = {},
    onSaveToFirestore: (Int) -> Unit = {},
    onRefreshFirebase: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    // State for login form
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sci-Fi Glowing Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, Color(0xFF00E5FF))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Beautiful animated tactical visual illustration matching command center dashboard
                TacticalConquestHeaderIllustration()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = LanguageUtils.translate("campaign_headline", currentLanguage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = LanguageUtils.translate("campaign_desc", currentLanguage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8),
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // --- SECTION 1: FIREBASE COMMANDER AUTH TERMINAL ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("firebase_auth_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Terminal Header Row
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
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isRealFirebaseActive) Color(0xFF4ADE80) else Color(0xFFFBBF24))
                        )
                        Text(
                            text = if (isRealFirebaseActive) "🛰️ FIREBASE SECURE LIVE" else "🛰️ LOCAL WASM SANDBOX MODE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isRealFirebaseActive) Color(0xFF4ADE80) else Color(0xFFFBBF24),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (isRealFirebaseActive) {
                        Badge(containerColor = Color(0x334ADE80), contentColor = Color(0xFF4ADE80)) {
                            Text(
                                text = "CLOUD ACTIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Badge(containerColor = Color(0x33FBBF24), contentColor = Color(0xFFFBBF24)) {
                            Text(
                                text = "OFFLINE-FIRST",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Divider(color = Color(0x1F94A3B8), thickness = 1.dp)

                if (firebaseUser == null) {
                    // Login / Registration Form
                    Text(
                        text = if (isRegisterMode) "PROVISION NEW COMMANDER CREDENTIALS" else "SECURE COMMANDER LOG IN",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Commander Email") },
                        modifier = Modifier.fillMaxWidth().testTag("auth_email_field"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0x5500E5FF),
                            focusedLabelColor = Color(0xFF00E5FF),
                            unfocusedLabelColor = Color(0xFF94A3B8),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Security Token / Password") },
                        modifier = Modifier.fillMaxWidth().testTag("auth_password_field"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0x5500E5FF),
                            focusedLabelColor = Color(0xFF00E5FF),
                            unfocusedLabelColor = Color(0xFF94A3B8),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    if (firebaseAuthError != null) {
                        Text(
                            text = "❌ Auth Error: $firebaseAuthError",
                            color = Color(0xFFFF5252),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (firebaseAuthLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFF00E5FF),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Securing socket connection...", color = Color(0xFF00E5FF), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        } else {
                            Button(
                                onClick = {
                                    if (emailInput.isNotEmpty() && passwordInput.isNotEmpty()) {
                                        if (isRegisterMode) {
                                            onRegisterUser(emailInput, passwordInput)
                                        } else {
                                            onLoginUser(emailInput, passwordInput)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.2f).testTag("auth_submit_btn")
                            ) {
                                Text(
                                    text = if (isRegisterMode) "REGISTER & SYNC" else "SECURE AUTHORIZE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            TextButton(
                                onClick = { isRegisterMode = !isRegisterMode },
                                modifier = Modifier.weight(1f).testTag("auth_mode_toggle_btn")
                            ) {
                                Text(
                                    text = if (isRegisterMode) "Need Login?" else "Create ID",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    if (!isRealFirebaseActive) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "ℹ️ Note: You can enter any mock email/password to log in locally. To connect to your actual live cloud Firebase project, set up FIREBASE_API_KEY, FIREBASE_PROJECT_ID, and FIREBASE_APP_ID inside your secrets panel.",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                } else {
                    // Logged In Status View
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                                Text(
                                    text = "AUTHORIZED COMMANDER PROFILE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = firebaseUser.email,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "UID: ${firebaseUser.uid}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }

                        Button(
                            onClick = { onLogoutUser() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252).copy(alpha = 0.2f), contentColor = Color(0xFFFF5252)),
                            border = BorderStroke(1.dp, Color(0xFFFF5252)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("auth_logout_btn")
                        ) {
                            Text("LOGOUT", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        Divider(color = Color(0x3300E5FF), thickness = 1.dp)

        Text(
            text = "💾 LOCAL WAR CAMPAIGN DATA SLOTS",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )

        // Slot list: 1, 2, 3
        listOf(1, 2, 3).forEach { slot ->
            val matchingSave = campaignSaves.find { it.slotId == slot }
            val matchingCloudSave = firebaseProgressList.find { it.slotId == slot }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_slot_card_$slot"),
                colors = CardDefaults.cardColors(
                    containerColor = if (matchingSave != null) Color(0xFF1C2541) else Color(0x111C2541)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (matchingSave != null) Color(0xFF00E5FF) else Color(0x3394A3B8)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Row: Slot Number and State Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = "Slot Icon",
                                tint = if (matchingSave != null) Color(0xFF00E5FF) else Color(0xFF94A3B8)
                            )
                            Text(
                                text = "${LanguageUtils.translate("save_slot", currentLanguage)} #$slot",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (matchingCloudSave != null) {
                                Badge(
                                    containerColor = Color(0x3300E5FF),
                                    contentColor = Color(0xFF00E5FF)
                                ) {
                                    Text(
                                        text = "☁️ CLOUD BACKED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (matchingSave != null) {
                                Badge(
                                    containerColor = Color(0xFF00E5FF),
                                    contentColor = Color.Black
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Badge(
                                    containerColor = Color(0x1F94A3B8),
                                    contentColor = Color(0xFF94A3B8)
                                ) {
                                    Text(
                                        text = "EMPTY",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (matchingSave != null) {
                        // Display save metadata stats
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x11000000), RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            // Date
                            Text(
                                text = "📅 Saved at: ${sdf.format(Date(matchingSave.timestamp))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF5BC0BE),
                                fontFamily = FontFamily.Monospace
                            )

                            // Commander / Faction
                            Text(
                                text = "👤 Call-sign: ${matchingSave.commanderName} (${if (matchingSave.commanderFaction == "north_union") "North Union" else "South Alliance"})",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )

                            // Stats Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${LanguageUtils.translate("save_credits", currentLanguage)}: ${matchingSave.credits}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF00E5FF)
                                    )
                                    Text(
                                        text = "${LanguageUtils.translate("save_alloys", currentLanguage)}: ${matchingSave.alloys}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF00E5FF)
                                    )
                                }
                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text(
                                        text = "${LanguageUtils.translate("save_pop", currentLanguage)}: ${String.format("%,d", matchingSave.globalPopulation)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF00E5FF)
                                    )
                                    // Count sectors owned by inspecting regionsJson
                                    val ownedCount = remember(matchingSave.regionsJson) {
                                        try {
                                            val arr = org.json.JSONArray(matchingSave.regionsJson)
                                            var count = 0
                                            for (k in 0 until arr.length()) {
                                                if (arr.getJSONObject(k).getString("controller") == "player") {
                                                    count++
                                                }
                                            }
                                            count
                                        } catch (e: Exception) {
                                            0
                                        }
                                    }
                                    Text(
                                        text = "🗺️ ${LanguageUtils.translate("save_regions_owned", currentLanguage)}: $ownedCount / 7",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF00E5FF)
                                    )
                                }
                            }
                        }

                        // Save, Load, and Delete options bar for populated slots
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Open / Load save button
                            ElevatedButton(
                                onClick = { onLoad(slot) },
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = Color(0xFF00E5FF),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("load_btn_slot_$slot")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Load", modifier = Modifier.size(14.dp))
                                    Text(
                                        text = LanguageUtils.translate("save_btn_load", currentLanguage),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Overwrite Save Button
                            Button(
                                onClick = { onSave(slot) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF5BC0BE),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_btn_slot_$slot")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Save", modifier = Modifier.size(14.dp))
                                    Text(
                                        text = LanguageUtils.translate("save_btn_save", currentLanguage),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Cloud Backup Button
                            if (firebaseUser != null) {
                                Button(
                                    onClick = { onSaveToFirestore(slot) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1E293B),
                                        contentColor = Color(0xFF00E5FF)
                                    ),
                                    border = BorderStroke(1.dp, Color(0x7700E5FF)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .testTag("cloud_backup_btn_$slot")
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Sync Cloud", modifier = Modifier.size(12.dp))
                                        Text(
                                            text = "BACKUP ☁️",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Delete button
                            OutlinedButton(
                                onClick = { onDelete(slot) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFFF5252)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFFF5252)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(0.8f)
                                    .testTag("delete_btn_slot_$slot")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp))
                                    Text(
                                        text = LanguageUtils.translate("save_btn_delete", currentLanguage),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    } else {
                        // Empty slot display
                        Text(
                            text = LanguageUtils.translate("save_empty", currentLanguage),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B),
                            fontFamily = FontFamily.Monospace
                        )

                        // Snap save progress button to occupy this empty slot
                        Button(
                            onClick = { onSave(slot) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1F2937),
                                contentColor = Color(0xFF00E5FF)
                            ),
                            border = BorderStroke(1.dp, Color(0x5500E5FF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_btn_slot_$slot")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "New Save")
                                Text(
                                    text = LanguageUtils.translate("save_btn_save", currentLanguage),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Cloud Backup metadata info if it exists
                    if (matchingCloudSave != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x1F00E5FF), RoundedCornerShape(4.dp))
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "☁️ Latest Cloud Backup: ${sdf.format(Date(matchingCloudSave.timestamp))}",
                                fontSize = 9.sp,
                                color = Color(0xFF00E5FF),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Credits: ${matchingCloudSave.credits} // Sectors: ${matchingCloudSave.regionsCount}",
                                fontSize = 9.sp,
                                color = Color(0xFF818CF8),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        if (firebaseSyncSuccessMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x224ADE80)),
                border = BorderStroke(1.dp, Color(0xFF4ADE80)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "✅ $firebaseSyncSuccessMessage",
                    color = Color(0xFF4ADE80),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (firebaseSyncErrorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x22FF5252)),
                border = BorderStroke(1.dp, Color(0xFFFF5252)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "❌ Sync Error: $firebaseSyncErrorMessage",
                    color = Color(0xFFFF5252),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // --- SECTION 3: TACTICAL LOG PANEL & MULTIPLAYER MATCH CHRONICLES ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tactical_log_panel"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.List, contentDescription = "Log Icon", tint = Color(0xFF00E5FF))
                        Text(
                            text = "🛰️ TACTICAL LOG PANEL & TELEMETRY",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(onClick = { onRefreshFirebase() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Telemetry", tint = Color(0xFF00E5FF))
                    }
                }

                Divider(color = Color(0x1F94A3B8), thickness = 1.dp)

                // Summary stats bar
                val totalVictories = firebaseMatchStatsList.sumOf { it.victories }
                val totalDefeats = firebaseMatchStatsList.sumOf { it.defeats }
                val totalGames = firebaseMatchStatsList.size
                val winRate = if (totalGames > 0) (totalVictories.toDouble() / totalGames * 100).toInt() else 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF020617), RoundedCornerShape(6.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL MATCHES", fontSize = 8.sp, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
                        Text("$totalGames", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("VICTORIES", fontSize = 8.sp, color = Color(0xFF4ADE80), fontFamily = FontFamily.Monospace)
                        Text("$totalVictories", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80), fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DEFEATS", fontSize = 8.sp, color = Color(0xFFFF5252), fontFamily = FontFamily.Monospace)
                        Text("$totalDefeats", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252), fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("WIN RATE", fontSize = 8.sp, color = Color(0xFF00E5FF), fontFamily = FontFamily.Monospace)
                        Text("$winRate%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontFamily = FontFamily.Monospace)
                    }
                }

                // Match log history list
                Text(
                    text = "CHRONICLES FEED (FIRESTORE STREAM)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    fontFamily = FontFamily.Monospace
                )

                if (firebaseMatchStatsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Color(0xFF020617), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0x1F94A3B8), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tactical telemetry recorded yet.\nEngage in combat on the Tactical Map!",
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .background(Color(0xFF020617), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0x1F94A3B8), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        firebaseMatchStatsList.forEach { match ->
                            val isWin = match.outcome == "VICTORY"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x0F000000), RoundedCornerShape(4.dp))
                                    .border(0.5.dp, if (isWin) Color(0x334ADE80) else Color(0x33FF5252), RoundedCornerShape(4.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = if (isWin) "🏆 VICTORY" else "💀 DEFEAT",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isWin) Color(0xFF4ADE80) else Color(0xFFFF5252),
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = match.matchId,
                                            fontSize = 9.sp,
                                            color = Color(0xFF94A3B8),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        text = "Commander ${match.commanderName} (${if (match.commanderFaction == "north_union") "North Union" else "South Alliance"})",
                                        fontSize = 10.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "📅 ${sdf.format(Date(match.timestamp))}",
                                        fontSize = 8.sp,
                                        color = Color(0xFF64748B),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Rounds: ${match.totalRounds}",
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Latency: ${String.format("%.2fms", match.averageLatencyMs)}",
                                        fontSize = 9.sp,
                                        color = Color(0xFF00E5FF),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Redundant / General Restart Command Button
        OutlinedButton(
            onClick = { onResetCampaign() },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFFF5252)
            ),
            border = BorderStroke(1.dp, Color(0xFFFF5252)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .testTag("reset_campaign_btn")
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Reset War Campaign")
                Text(
                    text = LanguageUtils.translate("save_btn_reset", currentLanguage),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
