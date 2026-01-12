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
    turnDurationSeconds: Int = 10,
    isLocalTurn: Boolean = true,
    isLocalHost: Boolean = false,
    syncDelayMs: Long = 650L,
    onVolverLobby: () -> Unit = {}
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
        val turnDuration = turnDurationSeconds.coerceIn(5, 60)
        val usesRemoteSync = roomCode.isNotBlank()
        val mesaScope = rememberCoroutineScope()
        val localPlayerId = remember { FuncionesGlobales.obtenerJugadorId() }

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
        var localPlayerIndex by remember(roomCode) { mutableStateOf(0) }
        val seatOrderIndices = remember(localPlayerIndex, normalizedPlayers) {
            val baseIndex = localPlayerIndex.coerceIn(0, normalizedPlayers - 1)
            List(normalizedPlayers) { offset -> (baseIndex + offset) % normalizedPlayers }
        }
        val opponentPositions = seatOrderIndices.drop(1).mapIndexed { index, playerIndex ->
            val angleDeg = 90.0 + angleStep * (index + 1)
            val angleRad = angleDeg * (PI / 180.0)
            val offsetX = (cos(angleRad) * circleRadius.value).toFloat().dp
            val offsetY = (sin(angleRad) * circleRadius.value).toFloat().dp + circleCenterYOffset
            val name = playerNames.getOrElse(playerIndex) { "J${playerIndex + 1}" }
            OpponentPosition(name = name, offsetX = offsetX, offsetY = offsetY, playerIndex = playerIndex)
        }
        val opponentNames = seatOrderIndices.drop(1).map { playerIndex ->
            playerNames.getOrElse(playerIndex) { "J${playerIndex + 1}" }
        }

        val opponentCounts = remember(normalizedPlayers, normalizedCards, usesRemoteSync) {
            mutableStateListOf<Int>().apply {
                repeat(opponentCount) { add(if (usesRemoteSync) 0 else normalizedCards) }
            }
        }
        var handCards by remember(roomCode, normalizedCards, usesRemoteSync) {
            mutableStateOf<List<Carta>>(
                if (usesRemoteSync) {
                    emptyList<Carta>()
                } else {
                    List(normalizedCards) { crearCartaAleatoria() }
                }
            )
        }
        var orderedPlayerIds by remember(roomCode) { mutableStateOf<List<String>>(emptyList()) }
        var mazosByPlayer by remember(roomCode) { mutableStateOf<Map<String, List<Carta>>>(emptyMap()) }
        val mesaPlaceholder = remember {
            Carta(
                imagen = "",
                numero = null,
                color = "wild",
                isSpecial = false,
                specialType = null
            )
        }
        var discardTop by remember(roomCode) {
            mutableStateOf(if (usesRemoteSync) mesaPlaceholder else crearCartaAleatoria(0))
        }
        var mesaVersion by remember(roomCode) { mutableStateOf(0L) }
        var turnMesaVersion by remember(roomCode) { mutableStateOf(0L) }
        var partidaSyncReady by remember(roomCode) { mutableStateOf(false) }
        var mesaSyncedForTurn by remember(roomCode) { mutableStateOf(false) }

        var playPhase by remember { mutableStateOf(PlayPhase.Idle) }
        var selectedIndex by remember { mutableStateOf<Int?>(null) }
        var playedIndex by remember { mutableStateOf<Int?>(null) }
        var playedCard by remember { mutableStateOf<Carta?>(null) }
        var pendingDrawCount by remember { mutableStateOf(0) }
        var pendingDrawType by remember { mutableStateOf<CartaEspecial?>(null) }
        var pendingDrawJustPlayedByLocal by remember(roomCode) { mutableStateOf(false) }
        var awaitingColorChoice by remember { mutableStateOf(false) }
        var pendingWildCard by remember { mutableStateOf<Carta?>(null) }
        var turnDirection by remember { mutableStateOf(1) }
        var currentTurnIndex by remember(normalizedPlayers, isLocalTurn) {
            mutableStateOf(if (isLocalTurn) 0 else 1.coerceAtMost(normalizedPlayers - 1))
        }
        var turnSecondsLeft by remember { mutableStateOf(turnDuration) }
        var gameOver by remember { mutableStateOf(false) }
        var unoCalled by remember { mutableStateOf(false) }
        var hadLocalCards by remember { mutableStateOf(false) }
        var hadOpponentCards by remember { mutableStateOf(false) }
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
        val turnReady = !usesRemoteSync || (partidaSyncReady && mesaVersion >= turnMesaVersion)
        val timerReady = turnReady && (!isLocalTurnActive || mesaSyncedForTurn)
        val canInteract = isLocalTurnActive &&
            turnReady &&
            (!usesRemoteSync || mesaSyncedForTurn) &&
            !gameOver &&
            !awaitingColorChoice &&
            (playPhase == PlayPhase.Idle || playPhase == PlayPhase.Preview)
        val canDraw = isLocalTurnActive &&
            turnReady &&
            (!usesRemoteSync || mesaSyncedForTurn) &&
            !gameOver &&
            !awaitingColorChoice &&
            playPhase == PlayPhase.Idle &&
            pendingDrawCount == 0
        val addCardsToHand: (Int) -> Unit = { count ->
            if (count > 0) {
                val updated = handCards + List(count) { crearCartaAleatoria() }
                handCards = updated
                if (usesRemoteSync) {
                    mesaScope.launch {
                        FuncionesGlobales.actualizarMazoJugador(roomCode, localPlayerId, updated)
                    }
                }
            }
        }
        val addCardsToOpponent: (Int, Int) -> Unit = { index, count ->
            if (index in opponentCounts.indices && count > 0) {
                opponentCounts[index] = opponentCounts[index] + count
            }
        }
        val advanceTurn: (Long?, Int) -> Unit = { mesaVersionOverride, stepsOverride ->
            if (normalizedPlayers > 1) {
                val steps = stepsOverride.coerceAtLeast(0)
                if (usesRemoteSync) {
                    if (isLocalTurnActive) {
                        mesaScope.launch {
                            FuncionesGlobales.avanzarTurno(
                                roomCode,
                                turnDirection,
                                mesaVersionOverride,
                                steps
                            )
                        }
                    }
                } else {
                    val next = currentTurnIndex + (turnDirection * steps)
                    val wrapped = ((next % normalizedPlayers) + normalizedPlayers) % normalizedPlayers
                    currentTurnIndex = wrapped
                    turnSecondsLeft = turnDuration
                }
            }
        }
        val drawCardsForTurn: (Int, Boolean) -> Unit = { count, endTurnIfNoPlay ->
            if (count > 0) {
                val updated = handCards + List(count) { crearCartaAleatoria() }
                handCards = updated
                if (usesRemoteSync) {
                    mesaScope.launch {
                        FuncionesGlobales.actualizarMazoJugador(roomCode, localPlayerId, updated)
                    }
                }
                if (endTurnIfNoPlay) {
                    val hasPlayable = updated.any { carta ->
                        puedeJugar(carta, discardTop, pendingDrawCount, pendingDrawType)
                    }
                    if (!hasPlayable) {
                        advanceTurn(null, 1)
                    }
                }
            }
        }
        val applyColorChoice: (String) -> Unit = { colorName ->
            pendingWildCard?.let { wildCard ->
                val updatedCard = wildCard.copy(color = colorName)
                val finishTurn: (Long?) -> Unit = { mesaVersionOverride ->
                    awaitingColorChoice = false
                    pendingWildCard = null
                    val canCheckOpponents = !usesRemoteSync || mazosByPlayer.isNotEmpty()
                    if (handCards.isEmpty() || (canCheckOpponents && opponentCounts.any { it == 0 })) {
                        gameOver = true
                    } else {
                        advanceTurn(mesaVersionOverride, 1)
                    }
                }
                if (usesRemoteSync) {
                    mesaScope.launch {
                        val newMesaVersion = FuncionesGlobales.actualizarMesaCarta(
                            roomCode,
                            updatedCard
                        )
                        finishTurn(newMesaVersion)
                    }
                } else {
                    discardTop = updatedCard
                    finishTurn(null)
                }
            }
        }

        LaunchedEffect(roomCode) {
            if (roomCode.isBlank()) {
                localPlayerIndex = 0
            } else {
                val order = runCatching {
                    FuncionesGlobales.obtenerOrdenJugadores(roomCode)
                }.getOrNull().orEmpty()
                if (order.isNotEmpty()) {
                    orderedPlayerIds = order
                    val index = order.indexOf(localPlayerId)
                    if (index >= 0) {
                        localPlayerIndex = index
                    }
                } else {
                    val index = FuncionesGlobales.obtenerIndiceJugadorEnPartida(roomCode)
                    if (index != null) {
                        localPlayerIndex = index
                    }
                }
                val diagnostico = runCatching {
                    FuncionesGlobales.diagnosticarPartida(roomCode)
                }.getOrNull()
                if (diagnostico != null) {
                    println("DIAG PARTIDA $roomCode -> $diagnostico")
                }
            }
        }

        LaunchedEffect(roomCode, partidaSyncReady, orderedPlayerIds) {
            if (!usesRemoteSync || roomCode.isBlank()) {
                return@LaunchedEffect
            }
            if (!partidaSyncReady || orderedPlayerIds.isNotEmpty()) {
                return@LaunchedEffect
            }
            val order = runCatching {
                FuncionesGlobales.obtenerOrdenJugadores(roomCode)
            }.getOrNull().orEmpty()
            if (order.isNotEmpty()) {
                orderedPlayerIds = order
                val index = order.indexOf(localPlayerId)
                if (index >= 0) {
                    localPlayerIndex = index
                }
            }
        }

        LaunchedEffect(roomCode, currentTurnIndex, localPlayerIndex) {
            if (!usesRemoteSync) {
                mesaSyncedForTurn = true
                return@LaunchedEffect
            }
            if (roomCode.isBlank()) {
                mesaSyncedForTurn = true
                return@LaunchedEffect
            }
            if (currentTurnIndex != localPlayerIndex) {
                pendingDrawJustPlayedByLocal = false
                mesaSyncedForTurn = false
                return@LaunchedEffect
            }
            mesaSyncedForTurn = false
            val estado = runCatching {
                FuncionesGlobales.leerEstadoMesa(roomCode)
            }.getOrNull()
            if (estado != null) {
                mesaVersion = estado.mesaVersion
                turnMesaVersion = estado.turnMesaVersion
                estado.mesaCarta?.let { discardTop = it }
                turnDirection = estado.direction
                pendingDrawCount = estado.pendingDrawCount
                pendingDrawType = estado.pendingDrawType
            }
            mesaSyncedForTurn = true
        }

        LaunchedEffect(orderedPlayerIds, mazosByPlayer, seatOrderIndices) {
            if (!usesRemoteSync) {
                return@LaunchedEffect
            }
            if (orderedPlayerIds.isEmpty()) {
                return@LaunchedEffect
            }
            val countsInOrder = orderedPlayerIds.map { mazosByPlayer[it]?.size ?: 0 }
            val seatIndices = seatOrderIndices.drop(1)
            val seatCounts = seatIndices.map { index -> countsInOrder.getOrNull(index) ?: 0 }
            val paddedOpponentCounts = if (seatCounts.size >= opponentCount) {
                seatCounts.take(opponentCount)
            } else {
                seatCounts + List(opponentCount - seatCounts.size) { 0 }
            }
            opponentCounts.clear()
            opponentCounts.addAll(paddedOpponentCounts)
            val localHand = mazosByPlayer[localPlayerId]
            if (localHand != null) {
                handCards = localHand
            }
        }

        DisposableEffect(roomCode) {
            if (roomCode.isBlank()) {
                onDispose {}
            } else {
                val handle = FuncionesGlobales.escucharPartidaEstado(roomCode) { estado ->
                    partidaSyncReady = estado.turnos.isNotEmpty() || estado.mesaCarta != null
                    mesaVersion = estado.mesaVersion
                    turnMesaVersion = estado.turnMesaVersion
                    estado.mesaCarta?.let { discardTop = it }
                    turnDirection = estado.direction
                    pendingDrawCount = estado.pendingDrawCount
                    pendingDrawType = estado.pendingDrawType
                    val index = estado.turnos.indexOfFirst { it }
                    if (index >= 0) {
                        currentTurnIndex = index
                    }
                }
                onDispose { handle.remove() }
            }
        }

        DisposableEffect(roomCode, orderedPlayerIds, seatOrderIndices) {
            if (roomCode.isBlank()) {
                onDispose {}
            } else {
                val handle = FuncionesGlobales.escucharMazos(roomCode) { mazos ->
                    mazosByPlayer = mazos
                    if (!usesRemoteSync) {
                        return@escucharMazos
                    }
                    val localHand = mazos[localPlayerId]
                    if (localHand != null) {
                        handCards = localHand
                    }
                    if (orderedPlayerIds.isNotEmpty()) {
                        val countsInOrder = orderedPlayerIds.map { mazos[it]?.size ?: 0 }
                        val seatIndices = seatOrderIndices.drop(1)
                        val seatCounts = seatIndices.map { index ->
                            countsInOrder.getOrNull(index) ?: 0
                        }
                        val paddedOpponentCounts = if (seatCounts.size >= opponentCount) {
                            seatCounts.take(opponentCount)
                        } else {
                            seatCounts + List(opponentCount - seatCounts.size) { 0 }
                        }
                        opponentCounts.clear()
                        opponentCounts.addAll(paddedOpponentCounts)
                    }
                }
                onDispose { handle.remove() }
            }
        }

        LaunchedEffect(handCards.size) {
            if (handCards.isNotEmpty()) {
                hadLocalCards = true
            }
        }

        LaunchedEffect(opponentCounts.toList()) {
            if (opponentCounts.any { it > 0 }) {
                hadOpponentCards = true
            }
        }

        LaunchedEffect(
            handCards.size,
            opponentCounts.toList(),
            awaitingColorChoice,
            hadLocalCards,
            hadOpponentCards
        ) {
            if (gameOver || awaitingColorChoice) {
                return@LaunchedEffect
            }
            val canCheckLocal = !usesRemoteSync || hadLocalCards
            val canCheckOpponents = !usesRemoteSync || hadOpponentCards
            if ((canCheckLocal && handCards.isEmpty()) ||
                (canCheckOpponents && opponentCounts.any { it == 0 })
            ) {
                gameOver = true
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
                    var mesaVersionOverride: Long? = null
                    var turnStepsOverride = 1
                    val chainColor = discardTop.color
                    val isPlusFourChain =
                        pendingDrawCount > 0 && pendingDrawType == CartaEspecial.MAS_CUATRO
                    playedCard?.let { card ->
                        val appliedCard = if (
                            isPlusFourChain && card.specialType == CartaEspecial.MAS_CUATRO
                        ) {
                            card.copy(color = chainColor)
                        } else {
                            card
                        }
                        if (!usesRemoteSync) {
                            discardTop = appliedCard
                        }
                        if (usesRemoteSync) {
                            mesaVersionOverride =
                                FuncionesGlobales.actualizarMesaCarta(roomCode, appliedCard)
                        }
                        when (card.specialType) {
                            CartaEspecial.REVERSA -> {
                                turnDirection *= -1
                                if (normalizedPlayers == 2) {
                                    turnStepsOverride = 0
                                    turnSecondsLeft = turnDuration
                                }
                                if (usesRemoteSync) {
                                    FuncionesGlobales.actualizarDireccion(roomCode, turnDirection)
                                }
                            }
                            CartaEspecial.MAS_DOS -> {
                                pendingDrawCount += 2
                                pendingDrawType = if (isPlusFourChain) {
                                    CartaEspecial.MAS_CUATRO
                                } else {
                                    CartaEspecial.MAS_DOS
                                }
                                if (usesRemoteSync) {
                                    FuncionesGlobales.actualizarPendiente(
                                        roomCode,
                                        pendingDrawCount,
                                        pendingDrawType
                                    )
                                }
                                if (isLocalTurnActive) {
                                    pendingDrawJustPlayedByLocal = true
                                }
                            }
                            CartaEspecial.MAS_CUATRO -> {
                                pendingDrawCount += 4
                                pendingDrawType = CartaEspecial.MAS_CUATRO
                                if (isPlusFourChain) {
                                    awaitingColorChoice = false
                                    pendingWildCard = null
                                } else {
                                    awaitingColorChoice = true
                                    pendingWildCard = card
                                }
                                if (usesRemoteSync) {
                                    FuncionesGlobales.actualizarPendiente(
                                        roomCode,
                                        pendingDrawCount,
                                        pendingDrawType
                                    )
                                }
                                if (isLocalTurnActive) {
                                    pendingDrawJustPlayedByLocal = true
                                }
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
                            if (usesRemoteSync) {
                                mesaScope.launch {
                                    FuncionesGlobales.actualizarMazoJugador(roomCode, localPlayerId, updated)
                                }
                            }
                        }
                    }
                    val canCheckOpponents = !usesRemoteSync || mazosByPlayer.isNotEmpty()
                    gameOver = handCards.isEmpty() || (canCheckOpponents && opponentCounts.any { it == 0 })
                    if (!gameOver && !awaitingColorChoice) {
                        advanceTurn(mesaVersionOverride, turnStepsOverride)
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

        LaunchedEffect(
            currentTurnIndex,
            gameOver,
            pendingDrawCount,
            pendingDrawType,
            awaitingColorChoice,
            timerReady
        ) {
            if (gameOver) {
                return@LaunchedEffect
            }
            if (awaitingColorChoice) {
                return@LaunchedEffect
            }
            turnSecondsLeft = turnDuration
            if (!timerReady) {
                return@LaunchedEffect
            }
            if (pendingDrawCount > 0) {
                if (isLocalTurnActive) {
                    if (pendingDrawJustPlayedByLocal) {
                        return@LaunchedEffect
                    }
                    val canStack = when (pendingDrawType) {
                        CartaEspecial.MAS_DOS -> handCards.any { card ->
                            card.specialType == CartaEspecial.MAS_DOS
                        }
                        CartaEspecial.MAS_CUATRO -> {
                            val chainColor = discardTop.color
                            handCards.any { card ->
                                card.specialType == CartaEspecial.MAS_CUATRO ||
                                    (card.specialType == CartaEspecial.MAS_DOS &&
                                        card.color.equals(chainColor, ignoreCase = true))
                            }
                        }
                        else -> false
                    }
                    if (!canStack) {
                        addCardsToHand(pendingDrawCount)
                        pendingDrawCount = 0
                        pendingDrawType = null
                        if (usesRemoteSync) {
                            FuncionesGlobales.actualizarPendiente(roomCode, 0, null)
                        }
                        advanceTurn(null, 1)
                        return@LaunchedEffect
                    }
                } else if (!usesRemoteSync) {
                    val seatIndex = seatOrderIndices.indexOf(currentTurnIndex)
                    val opponentIndex = seatIndex - 1
                    addCardsToOpponent(opponentIndex, pendingDrawCount)
                    pendingDrawCount = 0
                    pendingDrawType = null
                    advanceTurn(null, 1)
                    return@LaunchedEffect
                }
            }
            val turnIndex = currentTurnIndex
            while (turnSecondsLeft > 0 && currentTurnIndex == turnIndex && !gameOver) {
                delay(1000)
                if (playPhase == PlayPhase.Syncing ||
                    playPhase == PlayPhase.Animating ||
                    awaitingColorChoice ||
                    !timerReady
                ) {
                    continue
                }
                turnSecondsLeft -= 1
            }
            if (turnSecondsLeft == 0 && currentTurnIndex == turnIndex && !gameOver) {
                if (isLocalTurnActive) {
                    if (pendingDrawCount > 0) {
                        addCardsToHand(pendingDrawCount)
                        pendingDrawCount = 0
                        pendingDrawType = null
                        if (usesRemoteSync) {
                            FuncionesGlobales.actualizarPendiente(roomCode, 0, null)
                        }
                    } else {
                        drawCardsForTurn(normalizedDraw, false)
                    }
                }
                advanceTurn(null, 1)
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
                val updated = handCards + crearCartaAleatoria() + crearCartaAleatoria()
                handCards = updated
                if (usesRemoteSync) {
                    mesaScope.launch {
                        FuncionesGlobales.actualizarMazoJugador(roomCode, localPlayerId, updated)
                    }
                }
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
                        if (isLocalTurnActive) {
                            "Tu turno"
                        } else {
                            val name = playerNames.getOrElse(currentTurnIndex) {
                                "J${currentTurnIndex + 1}"
                            }
                            "Turno $name"
                        },
                        style = TextStyle(
                            color = Color.White,
                            fontSize = textSmall,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    if (pendingDrawCount > 0) {
                        BasicText(
                            "+$pendingDrawCount",
                            style = TextStyle(
                                color = Color(0xFFF6A040),
                                fontSize = textSmall,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
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

            opponentPositions.forEachIndexed { seatIndex, seat ->
                val fallbackCount = if (usesRemoteSync) 0 else normalizedCards
                val count = opponentCounts.getOrNull(seatIndex) ?: fallbackCount
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
                    isActive = currentTurnIndex == seat.playerIndex,
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
                                Modifier.pointerInput(playPhase, pendingDrawCount) {
                                    detectTapGestures(
                                        onTap = {
                                            drawCardsForTurn(normalizedDraw, true)
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
                        val canPlay = puedeJugar(carta, discardTop, pendingDrawCount, pendingDrawType)
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
                    opponentNames.forEachIndexed { index, name ->
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
                        Spacer(Modifier.height(minDim * 0.02f))
                        Button(
                            onClick = onVolverLobby,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF0D79C),
                                contentColor = Color(0xFF1A1A1A)
                            ),
                            shape = RoundedCornerShape(minDim * 0.04f)
                        ) {
                            Text(
                                "Volver al lobby",
                                fontSize = textSmall,
                                fontWeight = FontWeight.SemiBold
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
    val playerIndex: Int
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

private fun puedeJugar(
    carta: Carta,
    top: Carta,
    pendingDrawCount: Int,
    pendingDrawType: CartaEspecial?
): Boolean {
    if (pendingDrawCount > 0) {
        return when (pendingDrawType) {
            CartaEspecial.MAS_DOS -> carta.specialType == CartaEspecial.MAS_DOS
            CartaEspecial.MAS_CUATRO -> {
                carta.specialType == CartaEspecial.MAS_CUATRO ||
                    (carta.specialType == CartaEspecial.MAS_DOS &&
                        carta.color.equals(top.color, ignoreCase = true))
            }
            else -> false
        }
    }
    if (carta.specialType == CartaEspecial.MAS_CUATRO ||
        carta.specialType == CartaEspecial.CAMBIO_COLOR
    ) {
        return true
    }
    val topSpecial = top.specialType
    if (topSpecial != null && carta.specialType == topSpecial) {
        return true
    }
    if (carta.color.equals(top.color, ignoreCase = true)) {
        return true
    }
    if (carta.numero != null && top.numero != null && carta.numero == top.numero) {
        return true
    }
    return false
}
