package com.franciscor.pruebauni.Lobby

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font
import pruebauni.composeapp.generated.resources.Res
import pruebauni.composeapp.generated.resources.uno

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun unoFontFamily(): FontFamily = FontFamily(Font(Res.font.uno))

@Composable
fun LobbyFlow(
    state: LobbyUiState,
    onVolver: () -> Unit,
    onToggleConfig: () -> Unit,
    onCartasPorJugadorChange: (Float) -> Unit,
    onEspecialesChange: (Float) -> Unit,
    onMaxRobarChange: (Float) -> Unit,
    onEmpezarPartida: () -> Unit
) {
    var reveal by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        reveal = true
    }
    val unoFont = unoFontFamily()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1E5B46), Color(0xFF0E3B2B))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val minDim = minOf(maxWidth, maxHeight)
        val outerPad = minDim * 0.06f
        val titleSize = (minDim.value * 0.055f).sp
        val bodySize = (minDim.value * 0.032f).sp
        val labelSize = (minDim.value * 0.028f).sp
        val headerShape = RoundedCornerShape(minDim * 0.06f)
        val cardShape = RoundedCornerShape(minDim * 0.05f)
        val avatarSize = minDim * 0.12f
        val iconSize = minDim * 0.06f
        val headerPadding = minDim * 0.05f
        val headerAlpha by animateFloatAsState(
            targetValue = if (reveal) 1f else 0f,
            animationSpec = tween(durationMillis = 500)
        )
        val headerOffset by animateDpAsState(
            targetValue = if (reveal) 0.dp else minDim * 0.02f,
            animationSpec = tween(durationMillis = 500)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(outerPad),
            verticalArrangement = Arrangement.spacedBy(minDim * 0.04f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onVolver,
                    modifier = Modifier
                        .size(iconSize)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1F1D))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color(0xFFF4F1E9)
                    )
                }
                Text(
                    "Lobby",
                    style = TextStyle(
                        color = Color(0xFFF6F1E5),
                        fontSize = bodySize,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = unoFont
                    )
                )
                Spacer(modifier = Modifier.size(iconSize))
            }

            LobbyHeaderCard(
                roomName = state.roomName,
                roomCode = state.roomCode,
                shape = headerShape,
                titleSize = titleSize,
                bodySize = bodySize,
                labelSize = labelSize,
                contentPadding = headerPadding,
                titleFont = unoFont,
                modifier = Modifier
                    .graphicsLayer(alpha = headerAlpha)
                    .offset(y = headerOffset)
            )

            Text(
                "Jugadores (${state.maxPlayers})",
                style = TextStyle(
                    color = Color(0xFFF6F1E5),
                    fontSize = bodySize,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = unoFont
                )
            )

            val canEditConfig = state.isLocalHost && !state.isStarted
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(minDim * 0.02f)
            ) {
                itemsIndexed(state.players) { index, player ->
                    val itemAlpha by animateFloatAsState(
                        targetValue = if (reveal) 1f else 0f,
                        animationSpec = tween(
                            durationMillis = 400,
                            delayMillis = 100 + (index * 90)
                        )
                    )
                    val itemOffset by animateDpAsState(
                        targetValue = if (reveal) 0.dp else minDim * 0.02f,
                        animationSpec = tween(
                            durationMillis = 400,
                            delayMillis = 100 + (index * 90)
                        )
                    )
                    LobbyPlayerCard(
                        player = player,
                        shape = cardShape,
                        avatarSize = avatarSize,
                        bodySize = bodySize,
                        labelSize = labelSize,
                        titleFont = unoFont,
                        modifier = Modifier
                            .graphicsLayer(alpha = itemAlpha)
                            .offset(y = itemOffset)
                    )
                }
                item {
                    LobbyConfigCard(
                        config = state.config,
                        isOpen = state.showConfig && canEditConfig,
                        canEdit = canEditConfig,
                        onToggle = onToggleConfig,
                        onCartasPorJugadorChange = onCartasPorJugadorChange,
                        onEspecialesChange = onEspecialesChange,
                        onMaxRobarChange = onMaxRobarChange,
                        shape = cardShape,
                        bodySize = bodySize,
                        labelSize = labelSize,
                        titleFont = unoFont
                    )
                }
                if (state.isLocalHost && !state.isStarted) {
                    item {
                        LobbyStartButton(
                            onEmpezarPartida = onEmpezarPartida,
                            enabled = state.players.size >= 2,
                            bodySize = bodySize,
                            titleFont = unoFont
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LobbyHeaderCard(
    roomName: String,
    roomCode: String,
    shape: RoundedCornerShape,
    titleSize: TextUnit,
    bodySize: TextUnit,
    labelSize: TextUnit,
    contentPadding: Dp,
    titleFont: FontFamily,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = contentPadding * 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(contentPadding * 0.5f)
        ) {
            Text(
                roomName,
                style = TextStyle(
                    color = Color(0xFF111827),
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = titleFont
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(contentPadding * 0.4f)
            ) {
                Text(
                    "Codigo",
                    style = TextStyle(
                        color = Color(0xFF6B7280),
                        fontSize = labelSize,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Serif
                    )
                )
                Card(
                    shape = RoundedCornerShape(contentPadding * 0.6f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE7EDF2))
                ) {
                    Text(
                        roomCode,
                        modifier = Modifier.padding(
                            horizontal = contentPadding * 0.6f,
                            vertical = contentPadding * 0.25f
                        ),
                        style = TextStyle(
                            color = Color(0xFF2E3A45),
                            fontSize = bodySize,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
            Text(
                "Esperando jugadores...",
                style = TextStyle(
                    color = Color(0xFF8C96A3),
                    fontSize = labelSize,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Serif
                )
            )
        }
    }
}

@Composable
private fun LobbyConfigCard(
    config: LobbyConfig,
    isOpen: Boolean,
    canEdit: Boolean,
    onToggle: () -> Unit,
    onCartasPorJugadorChange: (Float) -> Unit,
    onEspecialesChange: (Float) -> Unit,
    onMaxRobarChange: (Float) -> Unit,
    shape: RoundedCornerShape,
    bodySize: TextUnit,
    labelSize: TextUnit,
    titleFont: FontFamily
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Configuracion",
                    style = TextStyle(
                        color = Color(0xFF1B1F23),
                        fontSize = bodySize,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = titleFont
                    )
                )
                Button(
                    onClick = onToggle,
                    enabled = canEdit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF0D79C),
                        contentColor = Color(0xFF1A1A1A)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (isOpen) "Cerrar" else "Configurar",
                        style = TextStyle(
                            fontSize = labelSize,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
            if (!isOpen) {
                Text(
                    "Cartas ${config.cardsPerPlayer} · Especiales ${config.specialCardsPercent}% · Robar ${config.maxDrawCards}",
                    style = TextStyle(
                        color = Color(0xFF6B7280),
                        fontSize = labelSize,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            if (isOpen) {
                LobbyConfigSlider(
                    title = "Cartas por jugador",
                    valueLabel = config.cardsPerPlayer.toString(),
                    value = config.cardsPerPlayer.toFloat(),
                    valueRange = 3f..15f,
                    onValueChange = onCartasPorJugadorChange,
                    enabled = canEdit,
                    bodySize = bodySize,
                    labelSize = labelSize,
                    titleFont = titleFont
                )
                LobbyConfigSlider(
                    title = "Especiales (%)",
                    valueLabel = "${config.specialCardsPercent}%",
                    value = config.specialCardsPercent.toFloat(),
                    valueRange = 0f..100f,
                    onValueChange = onEspecialesChange,
                    enabled = canEdit,
                    bodySize = bodySize,
                    labelSize = labelSize,
                    titleFont = titleFont
                )
                LobbyConfigSlider(
                    title = "Max robar",
                    valueLabel = config.maxDrawCards.toString(),
                    value = config.maxDrawCards.toFloat(),
                    valueRange = 1f..6f,
                    onValueChange = onMaxRobarChange,
                    enabled = canEdit,
                    bodySize = bodySize,
                    labelSize = labelSize,
                    titleFont = titleFont
                )
            }
        }
    }
}

@Composable
private fun LobbyConfigSlider(
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    enabled: Boolean,
    bodySize: TextUnit,
    labelSize: TextUnit,
    titleFont: FontFamily
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = TextStyle(
                    color = Color(0xFF111827),
                    fontSize = labelSize,
                    fontWeight = FontWeight.Medium,
                    fontFamily = titleFont
                )
            )
            Text(
                valueLabel,
                style = TextStyle(
                    color = Color(0xFF1F2937),
                    fontSize = bodySize,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled
        )
    }
}

@Composable
private fun LobbyStartButton(
    onEmpezarPartida: () -> Unit,
    enabled: Boolean,
    bodySize: TextUnit,
    titleFont: FontFamily
) {
    Button(
        onClick = onEmpezarPartida,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFF0D79C),
            contentColor = Color(0xFF1A1A1A),
            disabledContainerColor = Color(0xFFE3E0D6),
            disabledContentColor = Color(0xFF7A7A7A)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            "Empezar partida",
            style = TextStyle(
                fontSize = bodySize,
                fontWeight = FontWeight.Bold,
                fontFamily = titleFont
            )
        )
    }
}
@Composable
private fun LobbyPlayerCard(
    player: LobbyPlayer,
    shape: RoundedCornerShape,
    avatarSize: Dp,
    bodySize: TextUnit,
    labelSize: TextUnit,
    titleFont: FontFamily,
    modifier: Modifier = Modifier
) {
    val accent = if (player.isHost) Color(0xFFF3C26B) else Color(0xFFD7E2EA)
    val badgeColor = if (player.isHost) Color(0xFFF3C26B) else Color(0xFFE7EDF2)
    val badgeText = if (player.isHost) "HOST" else "LISTO"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = avatarSize * 0.12f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = avatarSize * 0.35f, vertical = avatarSize * 0.25f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(avatarSize * 0.25f)
        ) {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    player.name.take(1).uppercase(),
                    style = TextStyle(
                        color = Color(0xFF1F2937),
                        fontSize = bodySize,
                        fontWeight = FontWeight.Bold,
                        fontFamily = titleFont
                    )
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(avatarSize * 0.05f)
            ) {
                Text(
                    player.name,
                    style = TextStyle(
                        color = Color(0xFF111827),
                        fontSize = bodySize,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = titleFont
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "#${player.tag}",
                    style = TextStyle(
                        color = Color(0xFF6B7280),
                        fontSize = labelSize,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(avatarSize * 0.12f)
            ) {
                if (player.isHost) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Host",
                        tint = Color(0xFFCC8A1F),
                        modifier = Modifier.size(avatarSize * 0.35f)
                    )
                }
                Card(
                    shape = RoundedCornerShape(avatarSize),
                    colors = CardDefaults.cardColors(containerColor = badgeColor)
                ) {
                    Text(
                        badgeText,
                        modifier = Modifier.padding(
                            horizontal = avatarSize * 0.3f,
                            vertical = avatarSize * 0.12f
                        ),
                        style = TextStyle(
                            color = Color(0xFF1F2937),
                            fontSize = labelSize,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }
    }
}
