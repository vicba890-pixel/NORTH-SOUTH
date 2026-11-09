package com.example.ui.game

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LogEntity
import com.example.data.RegionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatAndConquestTabs(
    regions: List<RegionEntity>,
    logs: List<LogEntity>,
    playerCredits: Int,
    playerAlloys: Int,
    chatInput: String,
    onChatInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onConquerRegion: (RegionEntity) -> Unit,
    onSelectRegionOnMap: (RegionEntity) -> Unit,
    isLobbyJoined: Boolean,
    commanderName: String,
    commanderFaction: String,
    serverRoomId: String,
    onJoinLobby: (String, String, String) -> Unit,
    onLeaveLobby: () -> Unit,
    currentLanguage: com.example.ui.game.AppLanguage,
    playerGold: Double,
    playerFood: Double,
    playerManpower: Double,
    goldGrowthRate: Double,
    foodGrowthRate: Double,
    manpowerGrowthRate: Double,
    goldFocusCoeff: Double,
    foodFocusCoeff: Double,
    manpowerFocusCoeff: Double,
    onGoldCoeffChange: (Double) -> Unit,
    onFoodCoeffChange: (Double) -> Unit,
    onManpowerCoeffChange: (Double) -> Unit,
    onGenerateMap: (Long, Int, String, Float) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableIntStateOf(0) } // 0: Live Chat Lobby, 1: Conquest Deck, 2: Resource Logistics, 3: Map Generator

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Secondary Segmented Selector Header (Chat / Conquest Table)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val chatTabLabel = when (currentLanguage) {
                    com.example.ui.game.AppLanguage.ENGLISH -> "💬 LOBBY"
                    com.example.ui.game.AppLanguage.ESPANOL -> "💬 LOBBY"
                    com.example.ui.game.AppLanguage.PORTUGUES -> "💬 SALÃO"
                }
                val conquestTabLabel = when (currentLanguage) {
                    com.example.ui.game.AppLanguage.ENGLISH -> "⚔️ CONQUEST"
                    com.example.ui.game.AppLanguage.ESPANOL -> "⚔️ CONQUISTA"
                    com.example.ui.game.AppLanguage.PORTUGUES -> "⚔️ CONQUISTA"
                }
                val logisticsTabLabel = when (currentLanguage) {
                    com.example.ui.game.AppLanguage.ENGLISH -> "📈 LOGISTICS"
                    com.example.ui.game.AppLanguage.ESPANOL -> "📈 LOGÍSTICA"
                    com.example.ui.game.AppLanguage.PORTUGUES -> "📈 LOGÍSTICA"
                }
                val mapGenTabLabel = when (currentLanguage) {
                    com.example.ui.game.AppLanguage.ENGLISH -> "🛠️ GENERATOR"
                    com.example.ui.game.AppLanguage.ESPANOL -> "🛠️ GENERADOR"
                    com.example.ui.game.AppLanguage.PORTUGUES -> "🛠️ GERADOR"
                }

                listOf(
                    0 to chatTabLabel,
                    1 to conquestTabLabel,
                    2 to logisticsTabLabel,
                    3 to mapGenTabLabel
                ).forEach { (id, label) ->
                    val selected = activeSubTab == id
                    Button(
                        onClick = { activeSubTab = id },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sub_tab_btn_$id"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) Color(0xFF00E5FF) else Color.Transparent,
                            contentColor = if (selected) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }

            // Tab Content Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(10.dp)
            ) {
                if (activeSubTab == 0) {
                    // Chat and Logs Transmitter Deck
                    if (!isLobbyJoined) {
                        // High fidelity online transmitter entrance cockpit
                        var nameInput by remember { mutableStateOf(commanderName) }
                        var factionInput by remember { mutableStateOf(commanderFaction) }
                        var roomInput by remember { mutableStateOf(serverRoomId) }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF020617), shape = RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val lobbyTitle = when (currentLanguage) {
                                com.example.ui.game.AppLanguage.ENGLISH -> "🛰️ JOIN SECURE COMMANDER LOBBY"
                                com.example.ui.game.AppLanguage.ESPANOL -> "🛰️ CONEXIÓN LOBBY DE COMBATE"
                                com.example.ui.game.AppLanguage.PORTUGUES -> "🛰️ TERMINAL MULTIPLAYER ACTIVO"
                            }
                            Text(
                                text = lobbyTitle,
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )

                            val lobbyDesc = when (currentLanguage) {
                                com.example.ui.game.AppLanguage.ENGLISH -> "Register to sync actions with players on the battlefield."
                                com.example.ui.game.AppLanguage.ESPANOL -> "Inscríbete para coordinar ataques estratégicos con aliados."
                                com.example.ui.game.AppLanguage.PORTUGUES -> "Conecte-se para lutar pelo território contra generais."
                            }
                            Text(
                                text = lobbyDesc,
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 9.sp
                            )

                            // Commander Call-Sign Input
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val callSignLabel = when (currentLanguage) {
                                    com.example.ui.game.AppLanguage.ENGLISH -> "Call-sign:"
                                    com.example.ui.game.AppLanguage.ESPANOL -> "Nombre:"
                                    com.example.ui.game.AppLanguage.PORTUGUES -> "General:"
                                }
                                Text(text = callSignLabel, fontSize = 9.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(60.dp))

                                TextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    modifier = Modifier.weight(1f).height(38.dp).testTag("lobby_name_input"),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF1E293B),
                                        unfocusedContainerColor = Color(0xFF0F172A),
                                        focusedIndicatorColor = Color(0xFF00E5FF),
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    singleLine = true
                                )
                            }

                            // Server Arena Choice input
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val roomLabel = when (currentLanguage) {
                                    com.example.ui.game.AppLanguage.ENGLISH -> "Room ID:"
                                    com.example.ui.game.AppLanguage.ESPANOL -> "Sala ID:"
                                    com.example.ui.game.AppLanguage.PORTUGUES -> "Sala ID:"
                                }
                                Text(text = roomLabel, fontSize = 9.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(60.dp))

                                TextField(
                                    value = roomInput,
                                    onValueChange = { roomInput = it },
                                    modifier = Modifier.weight(1f).height(38.dp).testTag("lobby_room_input"),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF1E293B),
                                        unfocusedContainerColor = Color(0xFF0F172A),
                                        focusedIndicatorColor = Color(0xFF00E5FF),
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    singleLine = true
                                )
                            }

                            // Faction Selection button row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val factionLabel = when (currentLanguage) {
                                    com.example.ui.game.AppLanguage.ENGLISH -> "Faction:"
                                    com.example.ui.game.AppLanguage.ESPANOL -> "Facción:"
                                    com.example.ui.game.AppLanguage.PORTUGUES -> "Facção:"
                                }
                                Text(text = factionLabel, fontSize = 9.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(60.dp))

                                // Segmented faction selection buttons
                                listOf("north_union", "south_alliance").forEach { faction ->
                                    val selected = faction == factionInput
                                    val label = when (faction) {
                                        "north_union" -> when (currentLanguage) {
                                            com.example.ui.game.AppLanguage.ENGLISH -> "North Union"
                                            com.example.ui.game.AppLanguage.ESPANOL -> "U. del Norte"
                                            com.example.ui.game.AppLanguage.PORTUGUES -> "U. do Norte"
                                        }
                                        else -> when (currentLanguage) {
                                            com.example.ui.game.AppLanguage.ENGLISH -> "South Alliance"
                                            com.example.ui.game.AppLanguage.ESPANOL -> "Alianza Sur"
                                            com.example.ui.game.AppLanguage.PORTUGUES -> "Aliança Sul"
                                        }
                                    }
                                    Button(
                                        onClick = { factionInput = faction },
                                        modifier = Modifier.weight(1f).height(28.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selected) {
                                                if (faction == "north_union") Color(0xFF00FF88) else Color(0xFFFF1744)
                                            } else Color(0xFF1E293B),
                                            contentColor = if (selected) Color.Black else Color.White
                                        ),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(text = label, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Action button enlistment
                            val joinBtnText = when (currentLanguage) {
                                com.example.ui.game.AppLanguage.ENGLISH -> "🛰️ TRANSMITTER ONLINE CONNECT"
                                com.example.ui.game.AppLanguage.ESPANOL -> "🛰️ ENLAZAR CONSOLA DE COMBATE"
                                com.example.ui.game.AppLanguage.PORTUGUES -> "🛰️ CONECTAR TERMINAL DE GUERRA"
                            }
                            Button(
                                onClick = {
                                    onJoinLobby(
                                        if (nameInput.isNotBlank()) nameInput else "Anon_Commander",
                                        factionInput,
                                        if (roomInput.isNotBlank()) roomInput else "ARENA-01"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(36.dp).testTag("lobby_join_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(text = joinBtnText, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    } else {
                        // Joined! Show chat layout plus a clean Disconnect row on top.
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, Color(0x3300E5FF), shape = RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val statusText = when (currentLanguage) {
                                    com.example.ui.game.AppLanguage.ENGLISH -> "Connected: $serverRoomId • $commanderName"
                                    com.example.ui.game.AppLanguage.ESPANOL -> "Conectado: $serverRoomId • $commanderName"
                                    com.example.ui.game.AppLanguage.PORTUGUES -> "Conectado: $serverRoomId • $commanderName"
                                }
                                Text(
                                    text = statusText,
                                    fontSize = 9.sp,
                                    color = Color(0xFF00E5FF),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )

                                val disconnectText = when (currentLanguage) {
                                    com.example.ui.game.AppLanguage.ENGLISH -> "LEAVE"
                                    com.example.ui.game.AppLanguage.ESPANOL -> "SALIR"
                                    com.example.ui.game.AppLanguage.PORTUGUES -> "SAIR"
                                }
                                Text(
                                    text = disconnectText,
                                    fontSize = 8.sp,
                                    color = Color(0xFFFF1744),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { onLeaveLobby() }
                                        .padding(horizontal = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Scrolling chat message lines
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(Color(0xFF020617), shape = RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                                    .testTag("chat_logs_column"),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(logs) { log ->
                                    ChatBubbleItem(log)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Custom chat typing row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val placeholderText = when (currentLanguage) {
                                    com.example.ui.game.AppLanguage.ENGLISH -> "Type message..."
                                    com.example.ui.game.AppLanguage.ESPANOL -> "Escribir mensaje..."
                                    com.example.ui.game.AppLanguage.PORTUGUES -> "Digitar mensagem..."
                                }
                                TextField(
                                    value = chatInput,
                                    onValueChange = onChatInputChange,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("chat_text_input"),
                                    placeholder = { Text(placeholderText, fontSize = 11.sp, color = Color(0xFF64748B)) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF1E293B),
                                        unfocusedContainerColor = Color(0xFF0F172A),
                                        focusedIndicatorColor = Color(0xFF00E5FF),
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = { onSendMessage() })
                                )

                                IconButton(
                                    onClick = onSendMessage,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFF00E5FF), shape = RoundedCornerShape(8.dp))
                                        .testTag("chat_send_button")
                                ) {
                                    Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.Black)
                                }
                            }
                        }
                    }
                } else if (activeSubTab == 1) {
                    // Conquest Deck
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().testTag("conquest_deck_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(regions) { region ->
                            ConquestOutpostCard(
                                region = region,
                                playerCredits = playerCredits,
                                playerAlloys = playerAlloys,
                                currentLanguage = currentLanguage,
                                onConquer = { onConquerRegion(region) },
                                onSelectOnMap = { onSelectRegionOnMap(region) }
                            )
                        }
                    }
                } else if (activeSubTab == 2) {
                    // Resource Logistics Logarithmic Models
                    LogisticsModelsDashboard(
                        playerGold = playerGold,
                        playerFood = playerFood,
                        playerManpower = playerManpower,
                        goldGrowthRate = goldGrowthRate,
                        foodGrowthRate = foodGrowthRate,
                        manpowerGrowthRate = manpowerGrowthRate,
                        goldFocusCoeff = goldFocusCoeff,
                        foodFocusCoeff = foodFocusCoeff,
                        manpowerFocusCoeff = manpowerFocusCoeff,
                        onGoldCoeffChange = onGoldCoeffChange,
                        onFoodCoeffChange = onFoodCoeffChange,
                        onManpowerCoeffChange = onManpowerCoeffChange,
                        regions = regions,
                        currentLanguage = currentLanguage
                    )
                } else {
                    // MAP GENERATOR SECTION
                    MapGeneratorDashboard(
                        currentLanguage = currentLanguage,
                        onGenerateMap = onGenerateMap
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(log: LogEntity) {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timestampStr = formatter.format(Date(log.timestamp))

    val isSystem = log.type == "TACTICAL" || log.type == "SERVER" || log.type == "SYSTEM"
    val isPresident = log.sender == "PRESIDENT_VS" || log.sender.startsWith("VS_")

    val senderColor = if (isSystem) {
        Color(0xFFFFB300)
    } else if (isPresident) {
        Color(0xFF00E5FF)
    } else {
        Color(0xFFECEFF1)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isPresident) Color(0x1F00E5FF) else if (isSystem) Color(0x1FFF6F00) else Color(0x0F94A3B8))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = log.sender.uppercase(),
                fontSize = 9.sp,
                color = senderColor,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = timestampStr,
                fontSize = 8.sp,
                color = Color(0x66ECEFF1),
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = log.message,
            fontSize = 11.sp,
            color = Color(0xFFE2E8F0)
        )
    }
}

@Composable
fun ConquestOutpostCard(
    region: RegionEntity,
    playerCredits: Int,
    playerAlloys: Int,
    currentLanguage: com.example.ui.game.AppLanguage,
    onConquer: () -> Unit,
    onSelectOnMap: () -> Unit
) {
    val isOwned = region.controller == "player"
    val conquerCreditsCost = region.defenseLevel * 40
    val conquerAlloysCost = region.defenseLevel * 15
    val canAfford = playerCredits >= conquerCreditsCost && playerAlloys >= conquerAlloysCost

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isOwned) Color(0x3300E676) else Color(0x1F94A3B8),
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isOwned) Color(0x1400E676) else Color(0xFF020617)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = region.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (region.isSpaceLanded) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0x33FFD700), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            val harborLabel = when (currentLanguage) {
                                com.example.ui.game.AppLanguage.ENGLISH -> "SPACE PORT"
                                com.example.ui.game.AppLanguage.ESPANOL -> "PUERTO ESPACIAL"
                                com.example.ui.game.AppLanguage.PORTUGUES -> "PORTO ESPACIAL"
                            }
                            Text(harborLabel, fontSize = 7.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                val populationLabel = when (currentLanguage) {
                    com.example.ui.game.AppLanguage.ENGLISH -> "Population"
                    com.example.ui.game.AppLanguage.ESPANOL -> "Habitantes"
                    com.example.ui.game.AppLanguage.PORTUGUES -> "Habitantes"
                }
                Text(
                    text = "Lat ${region.latitude}° / Lng ${region.longitude}° • $populationLabel: ${region.population}",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(2.dp))
                val yieldLabel = when (currentLanguage) {
                    com.example.ui.game.AppLanguage.ENGLISH -> "Yield: +${region.creditsOutput} Credits, +${region.alloyOutput} Alloys/tick"
                    com.example.ui.game.AppLanguage.ESPANOL -> "Producción: +${region.creditsOutput} Créditos, +${region.alloyOutput} Aleaciones/tick"
                    com.example.ui.game.AppLanguage.PORTUGUES -> "Rendimento: +${region.creditsOutput} Créditos, +${region.alloyOutput} Ligas/tick"
                }

                Text(
                    text = yieldLabel,
                    fontSize = 10.sp,
                    color = Color(0xFF00FF88),
                    fontWeight = FontWeight.SemiBold
                )

                if (!isOwned) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val annexLabel = when (currentLanguage) {
                        com.example.ui.game.AppLanguage.ENGLISH -> "Annex cost"
                        com.example.ui.game.AppLanguage.ESPANOL -> "Costo de anexión"
                        com.example.ui.game.AppLanguage.PORTUGUES -> "Custo de anexação"
                    }
                    val creditsMetric = when (currentLanguage) {
                        com.example.ui.game.AppLanguage.ENGLISH -> "Credits"
                        com.example.ui.game.AppLanguage.ESPANOL -> "Créditos"
                        com.example.ui.game.AppLanguage.PORTUGUES -> "Créditos"
                    }
                    val alloysMetric = when (currentLanguage) {
                        com.example.ui.game.AppLanguage.ENGLISH -> "Alloys"
                        com.example.ui.game.AppLanguage.ESPANOL -> "Aleaciones"
                        com.example.ui.game.AppLanguage.PORTUGUES -> "Ligas"
                    }
                    Text(
                        text = "$annexLabel: 🪙 $conquerCreditsCost $creditsMetric / ⚙️ $conquerAlloysCost $alloysMetric",
                        fontSize = 10.sp,
                        color = if (canAfford) Color(0xFFE2E8F0) else Color(0xFFFF1744)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onSelectOnMap,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("GPS PIN", fontSize = 8.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onConquer,
                    enabled = !isOwned && canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOwned) Color(0xFF1E293B) else Color(0xFF00E5FF),
                        disabledContainerColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    val statusText = if (isOwned) {
                        when (currentLanguage) {
                            com.example.ui.game.AppLanguage.ENGLISH -> "SECURED"
                            com.example.ui.game.AppLanguage.ESPANOL -> "ASEGURADO"
                            com.example.ui.game.AppLanguage.PORTUGUES -> "GUANHADO"
                        }
                    } else {
                        when (currentLanguage) {
                            com.example.ui.game.AppLanguage.ENGLISH -> "CONQUER"
                            com.example.ui.game.AppLanguage.ESPANOL -> "CONQUISTAR"
                            com.example.ui.game.AppLanguage.PORTUGUES -> "ANEXAR"
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogisticsModelsDashboard(
    playerGold: Double,
    playerFood: Double,
    playerManpower: Double,
    goldGrowthRate: Double,
    foodGrowthRate: Double,
    manpowerGrowthRate: Double,
    goldFocusCoeff: Double,
    foodFocusCoeff: Double,
    manpowerFocusCoeff: Double,
    onGoldCoeffChange: (Double) -> Unit,
    onFoodCoeffChange: (Double) -> Unit,
    onManpowerCoeffChange: (Double) -> Unit,
    regions: List<com.example.data.RegionEntity>,
    currentLanguage: com.example.ui.game.AppLanguage
) {
    val scrollState = rememberScrollState()
    val heldRegions = remember(regions) { regions.filter { it.controller == "player" } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Explanatory Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0x1F00E5FF)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E5FF)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "LOGARITHMIC RESOURCE MODEL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF),
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Geographic expansion follows the law of diminishing returns: dR/dt = α + β * C * ln(1 + Σ Inputs). Adjust coefficients to focus logistics.",
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 12.sp
                )
            }
        }

        // --- GOLD MODEL CARD ---
        LogisticsResourceSection(
            title = "🪙 GOLD SUPPLY MATRIX",
            value = String.format("%.1f", playerGold),
            rate = goldGrowthRate,
            equation = "G_rate = 5.0 + 8.5 * C_g * ln(1 + Σ Credits_Out)",
            coefficient = goldFocusCoeff,
            onCoeffChange = onGoldCoeffChange,
            accentColor = Color(0xFFFFD700),
            inputValue = heldRegions.sumOf { it.creditsOutput.toDouble() },
            inputLabel = "Credits Output of Owned Territories",
            curveLabel = "Gold Logarithmic Curve"
        )

        // --- FOOD MODEL CARD ---
        LogisticsResourceSection(
            title = "🌾 FOOD LOGISTICS CORRIDOR",
            value = String.format("%.1f", playerFood),
            rate = foodGrowthRate,
            equation = "F_rate = 3.0 + 6.0 * C_f * ln(1 + Σ Pop_Index)",
            coefficient = foodFocusCoeff,
            onCoeffChange = onFoodCoeffChange,
            accentColor = Color(0xFFFF8A65),
            inputValue = heldRegions.sumOf { it.population.toDouble() / 10_000_000.0 },
            inputLabel = "Population Scale (Pop / 10M)",
            curveLabel = "Food Logarithmic Curve"
        )

        // --- MANPOWER MODEL CARD ---
        LogisticsResourceSection(
            title = "⚔️ CONVERSION MOBILIZATION MATRIX",
            value = String.format("%.1f", playerManpower),
            rate = manpowerGrowthRate,
            equation = "M_rate = 2.0 + 10.0 * C_m * ln(1 + Σ Mob_Index)",
            coefficient = manpowerFocusCoeff,
            onCoeffChange = onManpowerCoeffChange,
            accentColor = Color(0xFF00FF88),
            inputValue = heldRegions.sumOf { r -> r.defenseLevel.toDouble() * (1.0 + r.population.toDouble() / 50_000_000.0) },
            inputLabel = "Sect Defense & Urbanization Mobilization index",
            curveLabel = "Manpower Logarithmic Curve"
        )
    }
}

@Composable
fun LogisticsResourceSection(
    title: String,
    value: String,
    rate: Double,
    equation: String,
    coefficient: Double,
    onCoeffChange: (Double) -> Unit,
    accentColor: Color,
    inputValue: Double,
    inputLabel: String,
    curveLabel: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1F94A3B8)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header: Title, accumulated amount, and rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = equation, fontSize = 8.sp, color = accentColor, fontFamily = FontFamily.Monospace)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = String.format("+%.2f/sec", rate), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FF88))
                }
            }

            // Input description
            Text(
                text = String.format("%s: %.2f", inputLabel, inputValue),
                fontSize = 8.sp,
                color = Color(0xFF94A3B8),
                fontFamily = FontFamily.Monospace
            )

            // Logarithmic Curve Plotter (Canvas)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
                    .border(0.5.dp, Color(0x3394A3B8), RoundedCornerShape(4.dp))
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    // Draw grid lines
                    val gridPaint = Color(0x1194A3B8)
                    drawLine(gridPaint, start = androidx.compose.ui.geometry.Offset(0f, height / 2), end = androidx.compose.ui.geometry.Offset(width, height / 2), strokeWidth = 1f)
                    drawLine(gridPaint, start = androidx.compose.ui.geometry.Offset(width / 2, 0f), end = androidx.compose.ui.geometry.Offset(width / 2, height), strokeWidth = 1f)

                    // Draw logarithmic distribution curve path: y = coefficient * ln(1 + x)
                    val curvePath = androidx.compose.ui.graphics.Path()
                    val maxDomain = 150.0 // arbitrary domain scale
                    val maxRange = 50.0 // arbitrary range scale for ln

                    for (i in 0..width.toInt()) {
                        val pct = i.toFloat() / width
                        val xVal = pct * maxDomain
                        // logarithmic formula
                        val yVal = coefficient * kotlin.math.ln(1.0 + xVal)
                        // Map coordinates (invert y as Canvas (0,0) is top-left)
                        val xCoord = pct * width
                        val yNormalized = (yVal / maxRange).toFloat().coerceIn(0f, 1f)
                        val yCoord = height - (yNormalized * height)

                        if (i == 0) {
                            curvePath.moveTo(xCoord, yCoord)
                        } else {
                            curvePath.lineTo(xCoord, yCoord)
                        }
                    }

                    drawPath(
                        path = curvePath,
                        color = accentColor.copy(alpha = 0.8f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                    )

                    // Draw current state dot along the logarithmic curve
                    val currentX = inputValue.coerceIn(0.0, maxDomain)
                    val currentY = coefficient * kotlin.math.ln(1.0 + currentX)
                    val dotPctX = (currentX / maxDomain).toFloat()
                    val dotPctY = (currentY / maxRange).toFloat().coerceIn(0f, 1f)

                    val dotX = dotPctX * width
                    val dotY = height - (dotPctY * height)

                    // Glow circle
                    drawCircle(
                        color = accentColor.copy(alpha = 0.3f),
                        radius = 8.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(dotX, dotY)
                    )
                    // Core point circle
                    drawCircle(
                        color = accentColor,
                        radius = 4.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(dotX, dotY)
                    )
                }

                Text(
                    text = curveLabel,
                    fontSize = 7.sp,
                    color = accentColor.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(4.dp).align(Alignment.TopStart)
                )
            }

            // Interactive Coefficient Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Focus Coefficient (C):",
                        fontSize = 8.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = String.format("%.2f x", coefficient),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = coefficient.toFloat(),
                    onValueChange = { onCoeffChange(it.toDouble()) },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = Color(0x1F94A3B8)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                )
            }
        }
    }
}

@Composable
fun MapGeneratorDashboard(
    currentLanguage: com.example.ui.game.AppLanguage,
    onGenerateMap: (Long, Int, String, Float) -> Unit
) {
    var seedInput by remember { mutableStateOf("777") }
    var regionCount by remember { mutableFloatStateOf(10f) }
    var distributionMode by remember { mutableStateOf("balanced") } // "balanced", "player_heavy", "rival_heavy", "neutral_heavy"
    var resourceFactor by remember { mutableFloatStateOf(1.0f) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Aesthetic Title Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0x1F00E5FF)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E5FF)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                val title = when (currentLanguage) {
                    AppLanguage.ENGLISH -> "🌌 COGNITIVE SEED MAP COMPILER"
                    AppLanguage.ESPANOL -> "🌌 COMPILADOR DE MAPAS POR COGNICIÓN"
                    AppLanguage.PORTUGUES -> "🌌 COMPILADOR DE MAPAS COGNITIVOS"
                }
                val subtitle = when (currentLanguage) {
                    AppLanguage.ENGLISH -> "Synthesizing territories using deterministic chaotic orbits: x_n = f(seed, μ). Real-time math engine."
                    AppLanguage.ESPANOL -> "Sintetiza territorios usando órbitas caóticas deterministas: x_n = f(semilla, μ). Motor matemático en tiempo real."
                    AppLanguage.PORTUGUES -> "Sintetizando territórios usando órbitas caóticas determinísticas: x_n = f(semente, μ). Motor matemático em tempo real."
                }
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF),
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 8.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 11.sp
                )
            }
        }

        // 1. SEED INPUT FIELD
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1F94A3B8)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val seedLabel = when (currentLanguage) {
                    AppLanguage.ENGLISH -> "Deterministic Prime Seed:"
                    AppLanguage.ESPANOL -> "Semilla Prima Determinista:"
                    AppLanguage.PORTUGUES -> "Semente Prima Determinística:"
                }
                Text(text = seedLabel, fontSize = 9.sp, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = seedInput,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                seedInput = newValue
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("map_generator_seed_input"),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedIndicatorColor = Color(0xFF00E5FF),
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val randomSeed = (100..9999).random()
                            seedInput = randomSeed.toString()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("RAND", fontSize = 9.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. REGION COUNT SLIDER
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1F94A3B8)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val countLabel = when (currentLanguage) {
                        AppLanguage.ENGLISH -> "Outpost Abundance (Density):"
                        AppLanguage.ESPANOL -> "Abundancia de Puestos (Densidad):"
                        AppLanguage.PORTUGUES -> "Abundância de Postos (Densidade):"
                    }
                    Text(text = countLabel, fontSize = 9.sp, color = Color(0xFF94A3B8))
                    Text(
                        text = "${regionCount.toInt()} Outposts",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Slider(
                    value = regionCount,
                    onValueChange = { regionCount = it },
                    valueRange = 5f..25f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color(0x1F94A3B8)
                    ),
                    modifier = Modifier.fillMaxWidth().height(24.dp)
                )
            }
        }

        // 3. FACTION DISTRIBUTION
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1F94A3B8)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val distLabel = when (currentLanguage) {
                    AppLanguage.ENGLISH -> "Geopolitical Distribution Focus:"
                    AppLanguage.ESPANOL -> "Enfoque Geopolítico de Distribución:"
                    AppLanguage.PORTUGUES -> "Foco Geopolítico de Distribuição:"
                }
                Text(text = distLabel, fontSize = 9.sp, color = Color(0xFF94A3B8))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val options = listOf("balanced", "player_heavy", "rival_heavy", "neutral_heavy")
                    options.forEach { opt ->
                        val selected = distributionMode == opt
                        val label = when (opt) {
                            "balanced" -> "BAL"
                            "player_heavy" -> "ALLIED"
                            "rival_heavy" -> "RIVAL"
                            else -> "NEUT"
                        }
                        Button(
                            onClick = { distributionMode = opt },
                            modifier = Modifier.weight(1f).height(24.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) Color(0xFF00E5FF) else Color(0xFF1E293B),
                                contentColor = if (selected) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(text = label, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4. RESOURCE FACTOR SLIDER
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1F94A3B8)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val resourceLabel = when (currentLanguage) {
                        AppLanguage.ENGLISH -> "Resource Amplification Factor (μ):"
                        AppLanguage.ESPANOL -> "Factor Amplificación Recursos (μ):"
                        AppLanguage.PORTUGUES -> "Fator de Amplificação (μ):"
                    }
                    Text(text = resourceLabel, fontSize = 9.sp, color = Color(0xFF94A3B8))
                    Text(
                        text = String.format("%.2f x", resourceFactor),
                        fontSize = 10.sp,
                        color = Color(0xFF00FF88),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Slider(
                    value = resourceFactor,
                    onValueChange = { resourceFactor = it },
                    valueRange = 0.5f..3.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00FF88),
                        activeTrackColor = Color(0xFF00FF88),
                        inactiveTrackColor = Color(0x1F94A3B8)
                    ),
                    modifier = Modifier.fillMaxWidth().height(24.dp)
                )
            }
        }

        // 5. TRIGGER BUTTON
        val generateBtnText = when (currentLanguage) {
            AppLanguage.ENGLISH -> "⚙️ COMPILE DETERMINISTIC WORLD MAP"
            AppLanguage.ESPANOL -> "⚙️ COMPILAR MAPA MUNDIAL DETERMINISTA"
            AppLanguage.PORTUGUES -> "⚙️ COMPILAR MAPA MUNDIAL DETERMINÍSTICO"
        }
        Button(
            onClick = {
                val seed = seedInput.toLongOrNull() ?: 777L
                onGenerateMap(seed, regionCount.toInt(), distributionMode, resourceFactor)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .testTag("trigger_generator_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(text = generateBtnText, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
