package com.franciscor.pruebauni.Partida

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.franciscor.pruebauni.Base_Datos.FuncionesGlobales
import com.franciscor.pruebauni.Dataclass.Carta
import com.franciscor.pruebauni.Dataclass.CartaEspecial
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.jetbrains.compose.resources.painterResource
import pruebauni.composeapp.generated.resources.Res
import pruebauni.composeapp.generated.resources.logoapp2

@Composable
fun Tablero(
    roomCode: String,
    numPlayers: Int = 4,
    cardsPerPlayer: Int = 7,
    maxDrawCards: Int = 2,
    isLocalTurn: Boolean = true,
    isLocalHost: Boolean = false,
    syncDelayMs: Long = 650L
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F1EC))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val minDim = minOf(maxWidth, maxHeight)
        val tableHeight = maxHeight
        val normalizedPlayers = numPlayers.coerceIn(2, 6)
        val normalizedCards = cardsPerPlayer.coerceAtLeast(1)
        val normalizedDraw = maxDrawCards.coerceAtLeast(1)
        val opponentCount = normalizedPlayers - 1
        val playerNames = List(normalizedPlayers) { index -> "J${index + 1}" }
        val opponents = playerNames.drop(1)
        val drawStackCount = normalizedDraw.coerceIn(1, 6)
        val outerPad = minDim * 0.035f
        val cardWidth = minDim * 0.18f
        val cardHeight = cardWidth * 1.45f
        val handCardWidth = cardWidth * 0.9f
        val handCardHeight = cardHeight * 0.9f
        val smallCardWidth = cardWidth * 0.58f
        val smallCardHeight = smallCardWidth * 1.45f
        val cardCorner = cardWidth * 0.12f
        val smallCorner = smallCardWidth * 0.12f
        val border = minDim * 0.006f
        val textSmall = (minDim.value * 0.03f).sp
        val textLarge = (minDim.value * 0.065f).sp
        val pileGap = minDim * 0.07f
        val stackOffset = minDim * 0.006f
        val seatGap = minDim * 0.008f
        val handGap = minDim * 0.01f
        val handRowWidth = maxWidth - (outerPad * 2f)
        val handListState = rememberLazyListState()
        val density = LocalDensity.current
        val turnDuration = 10
        val usesRemoteSync = roomCode.isNotBlank()
        val mesaScope = rememberCoroutineScope()

        val red = Color(0xFFE53935)
        val yellow = Color(0xFFFBC02D)
        val green = Color(0xFF43A047)
        val blue = Color(0xFF1E88E5)
        val black = Color(0xFF151515)
        val tableDark = Color(0xFF0E3B2B)
        val tableLight = Color(0xFF1C5F47)
        val ink = Color(0xFF0C0C0C)

        val tableShape = RoundedCornerShape(minDim * 0.08f)
        val cardShape = RoundedCornerShape(cardCorner)
        val smallCardShape = RoundedCornerShape(smallCorner)
        val circleRadius = minDim * 0.38f
        val circleCenterYOffset = -minDim * 0.08f
        val angleStep = 360.0 / normalizedPlayers
        val opponentPositions = opponents.mapIndexed { index, name ->
            val angleDeg = 90.0 + angleStep * (index + 1)
            val angleRad = angleDeg * (PI / 180.0)
            val offsetX = (cos(angleRad) * circleRadius.value).toFloat().dp
            val offsetY = (sin(angleRad) * circleRadius.value).toFloat().dp + circleCenterYOffset
            OpponentPosition(name = name, offsetX = offsetX, offsetY = offsetY, index = index + 1)
        }

        val opponentCounts = remember(normalizedPlayers, normalizedCards) {
            mutableStateListOf<Int>().apply {
                repeat(opponentCount) { add(normalizedCards) }
            }
        }
        var handCards by remember(normalizedCards) {
            mutableStateOf(List(normalizedCards) { crearCartaAleatoria() })
        }
        var discardTop by remember { mutableStateOf(crearCartaAleatoria(0)) }

        var playPhase by remember { mutableStateOf(PlayPhase.Idle) }
        var selectedIndex by remember { mutableStateOf<Int?>(null) }
        var playedIndex by remember { mutableStateOf<Int?>(null) }
        var playedCard by remember { mutableStateOf<Carta?>(null) }
        var pendingPlusFourCount by remember { mutableStateOf(0) }
        var awaitingColorChoice by remember { mutableStateOf(false) }
        var pendingWildCard by remember { mutableStateOf<Carta?>(null) }
        var turnDirection by remember { mutableStateOf(1) }
        var localPlayerIndex by remember(roomCode) { mutableStateOf(0) }
        var currentTurnIndex by remember(normalizedPlayers, isLocalTurn) {
            mutableStateOf(if (isLocalTurn) 0 else 1.coerceAtMost(normalizedPlayers - 1))
        }
        var turnSecondsLeft by remember { mutableStateOf(turnDuration) }
        var gameOver by remember { mutableStateOf(false) }
        var unoCalled by remember { mutableStateOf(false) }
        val travelProgress = remember { Animatable(0f) }
        val loadingTransition = rememberInfiniteTransition()
        val loadingAlpha by loadingTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
        val syncPulse by loadingTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
        val isLocalTurnActive = currentTurnIndex == localPlayerIndex
        val canInteract = isLocalTurnActive &&
            !gameOver &&
            !awaitingColorChoice &&
            (playPhase == PlayPhase.Idle || playPhase == PlayPhase.Preview)
        val canDraw = isLocalTurnActive &&
            !gameOver &&
            !awaitingColorChoice &&
            playPhase == PlayPhase.Idle &&
            pendingPlusFourCount == 0
        val addCardsToHand: (Int) -> Unit = { count ->
            if (count > 0) {
                handCards = handCards + List(count) { crearCartaAleatoria() }
            }
        }
        val addCardsToOpponent: (Int, Int) -> Unit = { index, count ->
            if (index in opponentCounts.indices && count > 0) {
                opponentCounts[index] = opponentCounts[index] + count
            }
        }
        val advanceTurn: () -> Unit = {
            if (normalizedPlayers > 1) {
                if (usesRemoteSync) {
                    if (isLocalTurnActive) {
                        mesaScope.launch {
                            FuncionesGlobales.avanzarTurno(roomCode, turnDirection)
                        }
                    }
                } else {
                    val next = currentTurnIndex + turnDirection
                    val wrapped = ((next % normalizedPlayers) + normalizedPlayers) % normalizedPlayers
                    currentTurnIndex = wrapped
                    turnSecondsLeft = turnDuration
                }
            }
        }
        val applyColorChoice: (String) -> Unit = { colorName ->
            val wildCard = pendingWildCard
            if (wildCard != null) {
                val updatedCard = wildCard.copy(color = colorName)
                discardTop = updatedCard
                if (roomCode.isNotBlank()) {
                    mesaScope.launch {
                        FuncionesGlobales.actualizarMesaCarta(roomCode, updatedCard)
                    }
                }
                awaitingColorChoice = false
                pendingWildCard = null
                if (handCards.isEmpty() || opponentCounts.any { it == 0 }) {
                    gameOver = true
                } else {
                    advanceTurn()
                }
            }
        }

        LaunchedEffect(roomCode) {
            if (roomCode.isBlank()) {
                localPlayerIndex = 0
            } else {
                val index = FuncionesGlobales.obtenerIndiceJugadorEnPartida(roomCode)
                if (index != null) {
                    localPlayerIndex = index
                }
            }
        }

        DisposableEffect(roomCode) {
            if (roomCode.isBlank()) {
                onDispose {}
            } else {
                val handle = FuncionesGlobales.escucharMesaCarta(roomCode) { carta ->
                    if (carta != null) {
                        discardTop = carta
                    }
                }
                onDispose { handle.remove() }
            }
        }

        DisposableEffect(roomCode) {
            if (roomCode.isBlank()) {
                onDispose {}
            } else {
                val handle = FuncionesGlobales.escucharTurnos(roomCode) { turnos ->
                    val index = turnos.indexOfFirst { it }
                    if (index >= 0) {
                        currentTurnIndex = index
                    }
                }
                onDispose { handle.remove() }
            }
        }

        LaunchedEffect(playPhase) {
            when (playPhase) {
                PlayPhase.Syncing -> {
                    if (isLocalTurnActive) {
                        delay(syncDelayMs)
                        playPhase = PlayPhase.Animating
                    }
                }
                PlayPhase.Animating -> {
                    travelProgress.snapTo(0f)
                    travelProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                    )
                    playedCard?.let { card ->
                        discardTop = card
                        if (roomCode.isNotBlank()) {
                            FuncionesGlobales.actualizarMesaCarta(roomCode, card)
                        }
                        when (card.specialType) {
                            CartaEspecial.REVERSA -> turnDirection *= -1
                            CartaEspecial.MAS_CUATRO -> {
                                pendingPlusFourCount += 1
                                awaitingColorChoice = true
                                pendingWildCard = card
                            }
                            CartaEspecial.CAMBIO_COLOR -> {
                                awaitingColorChoice = true
                                pendingWildCard = card
                            }
                            else -> Unit
                        }
                    }
                    playedIndex?.let { index ->
                        if (index in handCards.indices) {
                            val updated = handCards.toMutableList()
                            updated.removeAt(index)
                            handCards = updated
                        }
                    }
                    gameOver = handCards.isEmpty() || opponentCounts.any { it == 0 }
                    if (!gameOver && !awaitingColorChoice) {
                        advanceTurn()
                    }
                    playPhase = PlayPhase.Settled
                }
                PlayPhase.Settled -> {
                    delay(220)
                    playPhase = PlayPhase.Idle
                    selectedIndex = null
                    playedIndex = null
                    playedCard = null
                }
                else -> Unit
            }
        }

        LaunchedEffect(currentTurnIndex, gameOver, pendingPlusFourCount, awaitingColorChoice) {
            if (gameOver) {
                return@LaunchedEffect
            }
            if (awaitingColorChoice) {
                return@LaunchedEffect
            }
            if (pendingPlusFourCount > 0) {
                val penaltyCards = pendingPlusFourCount * 4
                if (currentTurnIndex == 0) {
                    val hasPlusFour = handCards.any { it.specialType == CartaEspecial.MAS_CUATRO }
                    if (!hasPlusFour) {
                        addCardsToHand(penaltyCards)
                        pendingPlusFourCount = 0
                        advanceTurn()
                        return@LaunchedEffect
                    }
                } else {
                    addCardsToOpponent(currentTurnIndex - 1, penaltyCards)
                    pendingPlusFourCount = 0
                    advanceTurn()
                    return@LaunchedEffect
                }
            }
            turnSecondsLeft = turnDuration
            val turnIndex = currentTurnIndex
            while (turnSecondsLeft > 0 && currentTurnIndex == turnIndex && !gameOver) {
                delay(1000)
                if (playPhase == PlayPhase.Syncing || playPhase == PlayPhase.Animating || awaitingColorChoice) {
                    continue
                }
                turnSecondsLeft -= 1
            }
            if (turnSecondsLeft == 0 && currentTurnIndex == turnIndex && !gameOver) {
                advanceTurn()
            }
        }

        LaunchedEffect(handCards.size, unoCalled, gameOver) {
            if (gameOver) {
                return@LaunchedEffect
            }
            if (handCards.size != 1) {
                unoCalled = false
                return@LaunchedEffect
            }
            if (unoCalled) {
                return@LaunchedEffect
            }
            delay(2000)
            if (handCards.size == 1 && !unoCalled && !gameOver) {
                handCards = handCards + crearCartaAleatoria() + crearCartaAleatoria()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(outerPad)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(tableShape)
                    .background(Brush.verticalGradient(listOf(tableLight, tableDark)))
                    .border(border, Color(0xFF0A2A1F), tableShape)
            )

            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText(
                    "UNO",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = textLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (minDim.value * 0.004f).sp
                    )
                )
                Spacer(Modifier.height(minDim * 0.01f))
                Row(
                    modifier = Modifier
                        .background(Color(0x66000000), RoundedCornerShape(50))
                        .padding(
                            horizontal = minDim * 0.03f,
                            vertical = minDim * 0.01f
                        ),
                    horizontalArrangement = Arrangement.spacedBy(minDim * 0.02f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        "Ronda 1",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = textSmall,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Box(
                        Modifier
                            .size(minDim * 0.01f)
                            .background(Color.White, CircleShape)
                    )
                    BasicText(
                        if (isLocalTurnActive) "Tu turno" else "Turno ${playerNames[currentTurnIndex]}",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = textSmall,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    TurnTimer(
                        secondsLeft = turnSecondsLeft,
                        totalSeconds = turnDuration,
                        timerSize = minDim * 0.035f,
                        color = Color(0xFFF0D79C)
                    )
                    BasicText(
                        "${turnSecondsLeft}s",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = textSmall,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            if (!gameOver && handCards.size == 1 && !unoCalled) {
                Button(
                    onClick = { unoCalled = true },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(minDim * 0.035f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF0D79C),
                        contentColor = Color(0xFF1A1A1A)
                    ),
                    shape = RoundedCornerShape(minDim * 0.04f)
                ) {
                    Image(
                        painter = painterResource(Res.drawable.logoapp2),
                        contentDescription = "UNO",
                        modifier = Modifier.size(minDim * 0.05f)
                    )
                    Spacer(Modifier.size(minDim * 0.012f))
                    Text(
                        "UNO",
                        fontSize = textSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            opponentPositions.forEach { seat ->
                val count = opponentCounts.getOrNull(seat.index - 1) ?: normalizedCards
                OpponentSeat(
                    name = seat.name,
                    cardCount = count,
                    stackCards = count.coerceAtMost(5),
                    smallCardWidth = smallCardWidth,
                    smallCardHeight = smallCardHeight,
                    smallCardShape = smallCardShape,
                    borderWidth = minDim * 0.002f,
                    textSize = textSmall,
                    cardGap = seatGap,
                    black = black,
                    isActive = currentTurnIndex == seat.index,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = seat.offsetX, y = seat.offsetY)
                )
            }

            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(pileGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(cardWidth, cardHeight)
                        .then(
                            if (canDraw) {
                                Modifier.pointerInput(playPhase, pendingPlusFourCount) {
                                    detectTapGestures(
                                        onTap = {
                                            addCardsToHand(normalizedDraw)
                                        }
                                    )
                                }
                            } else {
                                Modifier
                            }
                        )
                ) {
                    repeat(drawStackCount) { i ->
                        Box(
                            Modifier
                                .matchParentSize()
                                .offset(
                                    x = stackOffset * i.toFloat(),
                                    y = -stackOffset * i.toFloat()
                                )
                                .clip(cardShape)
                                .background(black)
                                .border(minDim * 0.003f, Color(0xFF222222), cardShape)
                        )
                    }
                    BasicText(
                        if (normalizedDraw == 1) "ROBAR" else "ROBAR x$normalizedDraw",
                        modifier = Modifier.align(Alignment.Center),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = textSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Box(Modifier.size(cardWidth, cardHeight)) {
                    CartaVisual(
                        carta = discardTop,
                        cardShape = cardShape,
                        borderWidth = minDim * 0.003f,
                        borderColor = ink,
                        innerPadding = cardWidth * 0.08f,
                        labelSize = textLarge,
                        red = red,
                        yellow = yellow,
                        green = green,
                        blue = blue,
                        wild = black,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = minDim * 0.02f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText(
                    if (handCards.size == 1) "Tu mano · 1 carta" else "Tu mano · ${handCards.size} cartas",
                    style = TextStyle(color = Color.White, fontSize = textSmall)
                )
                Spacer(Modifier.height(minDim * 0.01f))
                LazyRow(
                    state = handListState,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(handGap)
                ) {
                    itemsIndexed(handCards) { index, carta ->
                        val isSelected = selectedIndex == index
                        val canPlay = puedeJugar(carta, discardTop, pendingPlusFourCount)
                        val targetLift = when {
                            isSelected && playPhase == PlayPhase.Syncing -> minDim * 0.045f
                            isSelected && playPhase == PlayPhase.Preview -> minDim * 0.03f
                            else -> 0.dp
                        }
                        val lift by animateDpAsState(
                            targetValue = targetLift,
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                        )
                        val cardScale by animateFloatAsState(
                            targetValue = when {
                                isSelected && playPhase == PlayPhase.Syncing -> syncPulse
                                isSelected -> 1.02f
                                else -> 1f
                            },
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                        )
                        val cardAlpha by animateFloatAsState(
                            targetValue = if (isSelected && playPhase == PlayPhase.Animating) 0f else 1f,
                            animationSpec = tween(durationMillis = 120)
                        )
                        val tapModifier = if (canInteract && canPlay) {
                            Modifier.pointerInput(selectedIndex, playPhase) {
                                detectTapGestures(
                                    onTap = {
                                        if (selectedIndex != index) {
                                            selectedIndex = index
                                            playPhase = PlayPhase.Preview
                                        } else if (playPhase == PlayPhase.Preview) {
                                            playedIndex = index
                                            playedCard = carta
                                            playPhase = PlayPhase.Syncing
                                        }
                                    }
                                )
                            }
                        } else {
                            Modifier
                        }
                        val displayAlpha = cardAlpha * if (canPlay) 1f else 0.45f
                        CartaVisual(
                            carta = carta,
                            cardShape = cardShape,
                            borderWidth = minDim * 0.003f,
                            borderColor = if (isSelected) Color(0xFFEAD68B) else ink,
                            innerPadding = cardWidth * 0.08f,
                            labelSize = textSmall,
                            red = red,
                            yellow = yellow,
                            green = green,
                            blue = blue,
                            wild = black,
                            modifier = Modifier
                                .size(handCardWidth, handCardHeight)
                                .offset(y = -lift)
                                .graphicsLayer {
                                    scaleX = cardScale
                                    scaleY = cardScale
                                    alpha = displayAlpha
                                }
                                .then(tapModifier)
                        )
                    }
                }
            }

            if (isLocalTurnActive && playPhase == PlayPhase.Syncing) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0x4D000000))
                )
                BasicText(
                    "Cargando...",
                    modifier = Modifier.align(Alignment.Center),
                    style = TextStyle(
                        color = Color.White.copy(alpha = loadingAlpha),
                        fontSize = textSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                )
                LatencyIndicator(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(minDim * 0.035f),
                    color = Color(0xFFF0D79C).copy(alpha = loadingAlpha)
                )
            }

            if (playPhase == PlayPhase.Animating && playedCard != null) {
                val flyingWidth = cardWidth * 0.9f
                val flyingHeight = cardHeight * 0.9f
                val travelDistance = (tableHeight - flyingHeight) / 2f
                val travelOffset = -travelDistance * travelProgress.value
                val rotation = 4f * (1f - travelProgress.value)
                val scale = 1f + (0.18f * travelProgress.value)
                val scrollOffsetDp = with(density) {
                    handListState.firstVisibleItemScrollOffset.toDp()
                }
                val centerOffset = playedIndex?.let { index ->
                    val slot = handCardWidth + handGap
                    val offsetFromFirst =
                        slot * (index - handListState.firstVisibleItemIndex).toFloat() - scrollOffsetDp
                    val cardCenter = offsetFromFirst + (handCardWidth / 2f)
                    val rowCenter = handRowWidth * 0.5f
                    cardCenter - rowCenter
                } ?: 0.dp
                val travelOffsetX = centerOffset * (1f - travelProgress.value)
                CartaVisual(
                    carta = playedCard!!,
                    cardShape = cardShape,
                    borderWidth = minDim * 0.003f,
                    borderColor = ink,
                    innerPadding = cardWidth * 0.08f,
                    labelSize = textSmall,
                    red = red,
                    yellow = yellow,
                    green = green,
                    blue = blue,
                    wild = black,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(x = travelOffsetX, y = travelOffset)
                        .size(flyingWidth, flyingHeight)
                        .graphicsLayer {
                            rotationZ = rotation
                            scaleX = scale
                            scaleY = scale
                        }
                )
            }

            if (awaitingColorChoice && pendingWildCard != null && !gameOver) {
                val colorChoices = listOf("rojo", "azul", "verde", "amarillo")
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0x99000000))
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(minDim * 0.05f))
                            .background(Color(0xFF142C22))
                            .padding(minDim * 0.05f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(minDim * 0.02f)
                    ) {
                        Text(
                            "Elige color",
                            color = Color(0xFFF6F1E5),
                            fontSize = textSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(minDim * 0.02f)) {
                            colorChoices.forEach { choice ->
                                val choiceCard = Carta(
                                    imagen = "",
                                    numero = null,
                                    color = choice,
                                    isSpecial = false,
                                    specialType = null
                                )
                                CartaVisual(
                                    carta = choiceCard,
                                    cardShape = cardShape,
                                    borderWidth = minDim * 0.003f,
                                    borderColor = ink,
                                    innerPadding = cardWidth * 0.08f,
                                    labelSize = textSmall,
                                    red = red,
                                    yellow = yellow,
                                    green = green,
                                    blue = blue,
                                    wild = black,
                                    modifier = Modifier
                                        .size(cardWidth * 0.6f, cardHeight * 0.6f)
                                        .pointerInput(choice) {
                                            detectTapGestures(
                                                onTap = { applyColorChoice(choice) }
                                            )
                                        }
                                )
                            }
                        }
                    }
                }
            }

            if (gameOver) {
                val ranking = buildList {
                    add("Tu" to handCards.size)
                    opponents.forEachIndexed { index, name ->
                        add(name to (opponentCounts.getOrNull(index) ?: 0))
                    }
                }.sortedBy { it.second }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0xCC000000))
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(minDim * 0.05f))
                            .background(Color(0xFF142C22))
                            .padding(minDim * 0.05f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(minDim * 0.02f)
                    ) {
                        Text(
                            "Fin de partida",
                            color = Color(0xFFF6F1E5),
                            fontSize = textLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Ranking",
                            color = Color(0xFFE2D8C7),
                            fontSize = textSmall,
                            fontWeight = FontWeight.Medium
                        )
                        ranking.forEachIndexed { index, entry ->
                            Text(
                                "${index + 1}. ${entry.first} · ${entry.second} cartas",
                                color = Color.White,
                                fontSize = textSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class PlayPhase {
    Idle,
    Preview,
    Syncing,
    Animating,
    Settled
}

private data class OpponentPosition(
    val name: String,
    val offsetX: Dp,
    val offsetY: Dp,
    val index: Int
)

@Composable
private fun OpponentSeat(
    name: String,
    cardCount: Int,
    stackCards: Int,
    smallCardWidth: Dp,
    smallCardHeight: Dp,
    smallCardShape: RoundedCornerShape,
    borderWidth: Dp,
    textSize: TextUnit,
    cardGap: Dp,
    black: Color,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardLabel = if (cardCount == 1) "1 carta" else "$cardCount cartas"
    val textColor = if (isActive) Color(0xFFF0D79C) else Color.White
    val stairCount = stackCards.coerceAtLeast(0)
    val step = smallCardWidth * 0.18f
    val stackWidth = if (stairCount == 0) smallCardWidth else smallCardWidth + (step * (stairCount - 1))
    val stackHeight = if (stairCount == 0) smallCardHeight else smallCardHeight + (step * (stairCount - 1))
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicText(
            "$name · $cardLabel",
            style = TextStyle(color = textColor, fontSize = textSize)
        )
        Spacer(Modifier.height(cardGap))
        Box(modifier = Modifier.size(stackWidth, stackHeight)) {
            repeat(stairCount) { index ->
                Box(
                    modifier = Modifier
                        .offset(x = step * index, y = step * index)
                        .size(smallCardWidth, smallCardHeight)
                        .clip(smallCardShape)
                        .background(black)
                        .border(borderWidth, Color(0xFF222222), smallCardShape)
                )
            }
        }
    }
}

@Composable
private fun LatencyIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFF0D79C),
    indicatorSize: Dp = 28.dp
) {
    Canvas(modifier = modifier.size(indicatorSize)) {
        val minDim = minOf(size.width, size.height)
        val barCount = 4
        val spacing = minDim * 0.12f
        val barWidth = (minDim - spacing * (barCount - 1)) / barCount
        val heights = listOf(0.35f, 0.55f, 0.75f, 1f)
        heights.forEachIndexed { index, factor ->
            val barHeight = minDim * factor
            val top = minDim - barHeight
            drawRoundRect(
                color = color,
                topLeft = Offset(index * (barWidth + spacing), top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
private fun TurnTimer(
    secondsLeft: Int,
    totalSeconds: Int,
    timerSize: Dp,
    color: Color
) {
    val clampedTotal = totalSeconds.coerceAtLeast(1)
    val progress = secondsLeft.coerceIn(0, clampedTotal).toFloat() / clampedTotal
    Canvas(modifier = Modifier.size(timerSize)) {
        val minDim = minOf(size.width, size.height)
        val stroke = minDim * 0.14f
        drawArc(
            color = color.copy(alpha = 0.2f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = stroke)
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun CartaVisual(
    carta: Carta,
    cardShape: RoundedCornerShape,
    borderWidth: Dp,
    borderColor: Color,
    innerPadding: Dp,
    labelSize: TextUnit,
    red: Color,
    yellow: Color,
    green: Color,
    blue: Color,
    wild: Color,
    modifier: Modifier = Modifier
) {
    val cardColor = cartaColor(carta.color, red, yellow, green, blue, wild)
    val label = cartaLabel(carta)
    Box(
        modifier = modifier
            .clip(cardShape)
            .background(Color.White)
            .border(borderWidth, borderColor, cardShape)
    ) {
        Box(
            Modifier
                .matchParentSize()
                .padding(innerPadding)
                .clip(cardShape)
                .background(cardColor)
        )
        if (label.isNotEmpty()) {
            BasicText(
                label,
                modifier = Modifier.align(Alignment.Center),
                style = TextStyle(
                    color = Color.White,
                    fontSize = labelSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

private fun cartaColor(
    colorName: String,
    red: Color,
    yellow: Color,
    green: Color,
    blue: Color,
    wild: Color
): Color = when (colorName.lowercase()) {
    "rojo", "red" -> red
    "amarillo", "yellow" -> yellow
    "verde", "green" -> green
    "azul", "blue" -> blue
    "wild", "negro", "black" -> wild
    else -> wild
}

private fun cartaLabel(carta: Carta): String = when (carta.specialType) {
    CartaEspecial.MAS_DOS -> "+2"
    CartaEspecial.MAS_CUATRO -> "+4"
    CartaEspecial.REVERSA -> "REV"
    CartaEspecial.CAMBIO_COLOR -> "COLOR"
    null -> carta.numero?.toString().orEmpty()
}

private fun puedeJugar(carta: Carta, top: Carta, pendingPlusFourCount: Int): Boolean {
    if (pendingPlusFourCount > 0) {
        return carta.specialType == CartaEspecial.MAS_CUATRO
    }
    if (carta.specialType == CartaEspecial.MAS_CUATRO ||
        carta.specialType == CartaEspecial.CAMBIO_COLOR
    ) {
        return true
    }
    if (top.specialType == CartaEspecial.MAS_CUATRO ||
        top.specialType == CartaEspecial.CAMBIO_COLOR
    ) {
        return true
    }
    if (carta.specialType != null || top.specialType != null) {
        return carta.color.equals(top.color, ignoreCase = true)
    }
    return carta.color.equals(top.color, ignoreCase = true) || carta.numero == top.numero
}
