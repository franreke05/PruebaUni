package com.franciscor.pruebauni.Inicio

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.franciscor.pruebauni.Base_Datos.DbLobbyConfig
import com.franciscor.pruebauni.Base_Datos.DbSala
import com.franciscor.pruebauni.Base_Datos.FuncionesGlobales
import com.franciscor.pruebauni.Lobby.LobbyConfig
import com.franciscor.pruebauni.Lobby.LobbyFlow
import com.franciscor.pruebauni.Lobby.LobbyPlayer
import com.franciscor.pruebauni.Lobby.LobbyUiState
import com.franciscor.pruebauni.Partida.PartidaPrefs
import com.franciscor.pruebauni.Partida.Tablero
import com.franciscor.pruebauni.Partida.rememberPartidaStorage
import kotlinx.coroutines.launch

// UI de inicio/mesa. Esta capa solo renderiza estado y dispara acciones.
// Implementacion Firebase (minima):
// 1) El ControladorInicio expone StateFlow/UiState con usuario y sala (solo identidad).
// 2) Desde el Composable solo llamas a acciones del controlador (registrar, crearSala, unirseSala).
// 3) El controlador escucha users/rooms y actualiza el estado; aqui solo lees ese estado.
// 4) El juego (cartas/turnos) queda fuera de la BD.


// Flujo Firebase que debes conectar a esta UI:
// - Pantalla Lobby: crear sala -> Firestore crea doc en "rooms" con codigo corto y estado inicial.
// - Unirse: el usuario ingresa el codigo, se agrega a "rooms/{id}/players".
// - Mesa: renderiza todos desde RoomState (mano propia, carta actual, turno, jugadores).
// - Acciones: jugar/robar pasan por ControladorInicio, que valida y escribe en Firestore.


/*
Inicio basado en Firebase (solo identidad):
- El jugador registra su nombre y obtiene uid anonimo.
- Host crea la sala y comparte el codigo.
- Invitado usa el codigo para ocupar un asiento A/B/C/D.
- La UI solo lee identidad/sala; el juego va por otra via.
*/

@Composable
fun InicioFlow() {
    val partidaStorage = rememberPartidaStorage()
    val coroutineScope = rememberCoroutineScope()
    // Estado de la pantalla actual.
    var uiState by remember { mutableStateOf(InicioUiState()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val canCreateLobby = ControladorInicio.puedeCrearSala(uiState.crearSalaState)
    LaunchedEffect(uiState.screen) {
        errorMessage = null
    }
    val lobbyCode = (uiState.screen as? InicioScreen.Lobby)?.lobbyState?.roomCode
    DisposableEffect(lobbyCode) {
        if (lobbyCode.isNullOrBlank()) {
            onDispose {}
        } else {
            val handle = runCatching {
                FuncionesGlobales.escucharSalaPorCodigo(lobbyCode) { sala ->
                    if (uiState.screen is InicioScreen.Partida) {
                        return@escucharSalaPorCodigo
                    }
                    val currentLobby = (uiState.screen as? InicioScreen.Lobby)?.lobbyState
                    val updatedLobby = sala?.toLobbyUiState(currentLobby)
                    if (updatedLobby != null) {
                        var nextState = uiState.copy(screen = InicioScreen.Lobby(updatedLobby))
                        if (updatedLobby.isStarted) {
                            partidaStorage.save(
                                PartidaPrefs(
                                    roomName = updatedLobby.roomName,
                                    numPlayers = maxOf(2, updatedLobby.players.size)
                                )
                            )
                            nextState = ControladorInicio.empezarPartida(nextState)
                        }
                        uiState = nextState
                    }
                }
            }.getOrNull()
            onDispose {
                handle?.remove()
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        // Selecciona que pantalla renderizar.
        when (val current = uiState.screen) {
            // Pantalla de decision inicial.
            InicioScreen.Decide -> InicioParaDecidirHost(
                // Accion cuando elige crear sala.
                onCrearSala = {
                    errorMessage = null
                    uiState = ControladorInicio.irACrear(uiState)
                          },
                // Accion cuando elige unirse.
                onUnirseSala = {
                    errorMessage = null
                    uiState = ControladorInicio.irAUnirse(uiState)

                }
            )
            // Pantalla de crear sala.
            InicioScreen.Crear -> CrearSala(
                // Estado actual del formulario.
                state = uiState.crearSalaState,
                canCreate = canCreateLobby,
                errorMessage = errorMessage,
                // Actualiza nombre de sala.
                onRoomNameChange = { uiState = ControladorInicio.actualizarNombreSala(uiState, it) },
                // Actualiza max jugadores.
                onMaxPlayersChange = { uiState = ControladorInicio.actualizarMaxPlayersDesdeSlider(uiState, it) },
                // Actualiza privacidad.
                onPrivateChange = { uiState = ControladorInicio.actualizarPrivacidad(uiState, it) },
                // Crear sala en Firebase y mover al lobby.
                onCrearSala = {
                    if (canCreateLobby) {
                        errorMessage = null
                        coroutineScope.launch {
                            runCatching {
                                FuncionesGlobales.crearPartida(
                                    uiState.crearSalaState.roomName,
                                    uiState.crearSalaState.maxPlayers
                                )
                            }.onSuccess {
                                val codigo = FuncionesGlobales.ultimoCodigoCreado
                                val nextState = ControladorInicio.crearLobby(uiState)
                                val screen = nextState.screen as? InicioScreen.Lobby
                                if (screen != null) {
                                    val updatedLobby = if (!codigo.isNullOrBlank()) {
                                        screen.lobbyState.copy(roomCode = codigo)
                                    } else {
                                        screen.lobbyState
                                    }
                                    uiState = nextState.copy(screen = screen.copy(lobbyState = updatedLobby))
                                } else {
                                    errorMessage = "No se pudo abrir el lobby"
                                }
                            }.onFailure { error ->
                                errorMessage = error.message ?: "No se pudo crear la sala"
                            }
                        }
                    }
                },
                // Vuelve a la pantalla inicial.
                onVolver = {
                    errorMessage = null
                    uiState = ControladorInicio.irADecide(uiState)
                }
            )
            // Pantalla de unirse (placeholder).
            InicioScreen.Unirse -> UnirsePlaceholder(
                state = uiState.unirseSalaState,
                errorMessage = errorMessage,
                // Vuelve a la pantalla inicial.
                onCodigoChange = {
                    uiState = ControladorInicio.actualizarCodigoSala(uiState, it)

                },
                onVolver = {
                    errorMessage = null
                    uiState = ControladorInicio.irADecide(uiState)
                },
                onUnirseSala = {
                    val codigo = uiState.unirseSalaState.roomCode.trim()
                    if (codigo.isNotEmpty()) {
                        errorMessage = null
                        coroutineScope.launch {
                            runCatching {
                                FuncionesGlobales.unirseAPartida(codigo)
                                FuncionesGlobales.leerSalaPorCodigo(codigo)
                            }.onSuccess { sala ->
                                val lobbyState = sala?.toLobbyUiState(null)
                                if (lobbyState != null) {
                                    uiState = uiState.copy(screen = InicioScreen.Lobby(lobbyState))
                                } else {
                                    errorMessage = "No se encontro la sala"
                                }
                            }.onFailure { error ->
                                errorMessage = error.message ?: "No se pudo unirse a la sala"
                            }
                        }
                    }
                },

            )
            is InicioScreen.Lobby -> LobbyFlow(
                state = current.lobbyState,
                onVolver = { uiState = ControladorInicio.irADecide(uiState) },
                onToggleConfig = { uiState = ControladorInicio.toggleConfigLobby(uiState) },
                onCartasPorJugadorChange = {
                    val updatedState = ControladorInicio.actualizarCartasPorJugadorLobby(uiState, it)
                    uiState = updatedState
                    val lobby = (updatedState.screen as? InicioScreen.Lobby)?.lobbyState
                    if (lobby != null && lobby.isLocalHost) {
                        coroutineScope.launch {
                            FuncionesGlobales.actualizarConfigLobby(lobby.roomCode, lobby.toDbConfig())
                        }
                    }
                },
                onEspecialesChange = {
                    val updatedState = ControladorInicio.actualizarEspecialesLobby(uiState, it)
                    uiState = updatedState
                    val lobby = (updatedState.screen as? InicioScreen.Lobby)?.lobbyState
                    if (lobby != null && lobby.isLocalHost) {
                        coroutineScope.launch {
                            FuncionesGlobales.actualizarConfigLobby(lobby.roomCode, lobby.toDbConfig())
                        }
                    }
                },
                onMaxRobarChange = {
                    val updatedState = ControladorInicio.actualizarMaxRobarLobby(uiState, it)
                    uiState = updatedState
                    val lobby = (updatedState.screen as? InicioScreen.Lobby)?.lobbyState
                    if (lobby != null && lobby.isLocalHost) {
                        coroutineScope.launch {
                            FuncionesGlobales.actualizarConfigLobby(lobby.roomCode, lobby.toDbConfig())
                        }
                    }
                },
                onEmpezarPartida = {
                    coroutineScope.launch {
                        FuncionesGlobales.iniciarPartida(
                            current.lobbyState.roomCode,
                            current.lobbyState.toDbConfig()
                        )
                    }
                }
            )
            is InicioScreen.Partida -> Tablero(
                roomCode = current.partidaState.roomCode,
                numPlayers = current.partidaState.numPlayers,
                cardsPerPlayer = current.partidaState.cardsPerPlayer,
                maxDrawCards = current.partidaState.maxDrawCards,
                isLocalTurn = current.partidaState.isLocalTurn,
                isLocalHost = current.partidaState.isLocalHost
            )
        }

    }
}

private fun DbSala.toLobbyUiState(existing: LobbyUiState?): LobbyUiState {
    val sortedJugadores = jugadores.sortedBy { jugador ->
        if (jugador.numero > 0) jugador.numero else Int.MAX_VALUE
    }
    val players = sortedJugadores.mapIndexed { index, jugador ->
        val numero = if (jugador.numero > 0) jugador.numero else (index + 1)
        val tag = numero.toString().padStart(3, '0')
        val nombre = jugador.nombre.takeIf { it.isNotBlank() } ?: "Jugador $tag"
        LobbyPlayer(
            name = nombre,
            tag = tag,
            isHost = jugador.uid == hostUid
        )
    }
    val maxPlayersValue = if (maxPlayers > 0) maxPlayers else existing?.maxPlayers ?: maxOf(2, players.size)
    val roomName = nombre.takeIf { it.isNotBlank() }
        ?: existing?.roomName?.takeIf { it.isNotBlank() }
        ?: "Sala $codigo"
    val defaultConfig = LobbyConfig()
    val config = LobbyConfig(
        cardsPerPlayer = this.config.cardsPerPlayer.takeIf { it > 0 }
            ?: existing?.config?.cardsPerPlayer
            ?: defaultConfig.cardsPerPlayer,
        specialCardsPercent = this.config.specialCardsPercent.takeIf { it >= 0 }
            ?: existing?.config?.specialCardsPercent
            ?: defaultConfig.specialCardsPercent,
        maxDrawCards = this.config.maxDrawCards.takeIf { it > 0 }
            ?: existing?.config?.maxDrawCards
            ?: defaultConfig.maxDrawCards
    )
    val showConfig = existing?.showConfig ?: false
    val isLocalHost = FuncionesGlobales.obtenerJugadorId() == hostUid
    return LobbyUiState(
        roomName = roomName,
        roomCode = codigo,
        maxPlayers = maxPlayersValue,
        players = players,
        config = config,
        showConfig = showConfig,
        isLocalHost = isLocalHost,
        isStarted = started
    )
}

private fun LobbyUiState.toDbConfig(): DbLobbyConfig =
    DbLobbyConfig(
        cardsPerPlayer = config.cardsPerPlayer,
        specialCardsPercent = config.specialCardsPercent,
        maxDrawCards = config.maxDrawCards
    )

@Composable
fun InicioParaDecidirHost(
    onCrearSala: () -> Unit,
    onUnirseSala: () -> Unit
) {
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
        val outerPad = minDim * 0.08f
        val titleSize = (minDim.value * 0.09f).sp
        val bodySize = (minDim.value * 0.035f).sp
        val panelShape = RoundedCornerShape(minDim * 0.06f)
        val buttonShape = RoundedCornerShape(minDim * 0.05f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(outerPad),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "UNO",
                style = TextStyle(
                    color = Color(0xFFF6F1E5),
                    fontSize = titleSize,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (minDim.value * 0.006f).sp
                )
            )
            Spacer(Modifier.height(minDim * 0.015f))
            Text(
                "Elige como quieres jugar",
                style = TextStyle(
                    color = Color(0xFFE2D8C7),
                    fontSize = bodySize,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(Modifier.height(minDim * 0.05f))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = panelShape,
                colors = CardDefaults.cardColors(containerColor = Color(0xFF142C22)),
                elevation = CardDefaults.cardElevation(defaultElevation = minDim * 0.02f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = minDim * 0.05f, horizontal = minDim * 0.06f),
                    verticalArrangement = Arrangement.spacedBy(minDim * 0.025f)
                ) {
                    Button(
                        onClick = onCrearSala,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(minDim * 0.09f),
                        shape = buttonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF0D79C),
                            contentColor = Color(0xFF1A1A1A)
                        )
                    ) {
                        Text(
                            "Crear sala",
                            style = TextStyle(
                                fontSize = bodySize,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    Divider(color = Color(0x332D2D2D))
                    Button(
                        onClick = onUnirseSala,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(minDim * 0.09f),
                        shape = buttonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1C1F1D),
                            contentColor = Color(0xFFF4F1E9)
                        )
                    ) {
                        Text(
                            "Unirse a una sala",
                            style = TextStyle(
                                fontSize = bodySize,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CrearSala(
    state: CrearSalaState,
    canCreate: Boolean,
    errorMessage: String?,
    onRoomNameChange: (String) -> Unit,
    onMaxPlayersChange: (Float) -> Unit,
    onPrivateChange: (Boolean) -> Unit,
    onCrearSala: () -> Unit,
    onVolver: () -> Unit
) {
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
        val outerPad = minDim * 0.08f
        val titleSize = (minDim.value * 0.07f).sp
        val bodySize = (minDim.value * 0.033f).sp
        val panelShape = RoundedCornerShape(minDim * 0.06f)
        val buttonShape = RoundedCornerShape(minDim * 0.05f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(outerPad),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = onVolver,
                    modifier = Modifier
                        .size(minDim * 0.07f)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1F1D))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color(0xFFF4F1E9)
                    )
                }
            }
            Spacer(Modifier.height(minDim * 0.02f))
            Text(
                "Crear sala",
                style = TextStyle(
                    color = Color(0xFFF6F1E5),
                    fontSize = titleSize,
                    fontWeight = FontWeight.Black
                )
            )
            Spacer(Modifier.height(minDim * 0.05f))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = panelShape,
                colors = CardDefaults.cardColors(containerColor = Color(0xFF142C22)),
                elevation = CardDefaults.cardElevation(defaultElevation = minDim * 0.02f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = minDim * 0.05f, horizontal = minDim * 0.06f),
                    verticalArrangement = Arrangement.spacedBy(minDim * 0.03f)
                ) {
                    Text(
                        "Nombre de sala",
                        style = TextStyle(
                            color = Color(0xFFE2D8C7),
                            fontSize = bodySize,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    OutlinedTextField(
                        value = state.roomName,
                        onValueChange = onRoomNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = bodySize,
                            color = Color(0xFFF4F1E9)
                        )
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(minDim * 0.015f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Max jugadores",
                                style = TextStyle(
                                    color = Color(0xFFE2D8C7),
                                    fontSize = bodySize,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                state.maxPlayers.toString(),
                                style = TextStyle(
                                    color = Color(0xFFF0D79C),
                                    fontSize = bodySize,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        Slider(
                            value = state.maxPlayers.toFloat(),
                            onValueChange = onMaxPlayersChange,
                            valueRange = 2f..6f
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Sala privada",
                            style = TextStyle(
                                color = Color(0xFFE2D8C7),
                                fontSize = bodySize,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Switch(
                            checked = state.isPrivate,
                            onCheckedChange = onPrivateChange
                        )
                    }
                    Text(
                        "Codigo se genera al crear",
                        style = TextStyle(
                            color = Color(0xFFB7AD9B),
                            fontSize = (minDim.value * 0.028f).sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Start
                        )
                    )
                    if (!errorMessage.isNullOrBlank()) {
                        Text(
                            errorMessage,
                            style = TextStyle(
                                color = Color(0xFFFFC2C2),
                                fontSize = (minDim.value * 0.028f).sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    Spacer(Modifier.height(minDim * 0.01f))
                    Button(
                        onClick = onCrearSala,
                        enabled = canCreate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(minDim * 0.09f),
                        shape = buttonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF0D79C),
                            contentColor = Color(0xFF1A1A1A),
                            disabledContainerColor = Color(0xFFE4D3A7),
                            disabledContentColor = Color(0xFF8B7E66)
                        )
                    ) {
                        Text(
                            "Unirse a sala",
                            style = TextStyle(
                                fontSize = bodySize,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UnirsePlaceholder(
    state: UnirseSalaState,
    errorMessage: String?,
    onVolver: () -> Unit,
    onCodigoChange: (String) -> Unit,
    onUnirseSala: () -> Unit
) {
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
        val outerPad = minDim * 0.08f
        val titleSize = (minDim.value * 0.07f).sp
        val bodySize = (minDim.value * 0.033f).sp
        val panelShape = RoundedCornerShape(minDim * 0.06f)
        val buttonShape = RoundedCornerShape(minDim * 0.05f)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(outerPad),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = onVolver,
                    modifier = Modifier
                        .size(minDim * 0.07f)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1F1D))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color(0xFFF4F1E9)
                    )
                }
            }
            Spacer(Modifier.height(minDim * 0.02f))
            Text(
                "Unirse a sala",
                style = TextStyle(
                    color = Color(0xFFF6F1E5),
                    fontSize = titleSize,
                    fontWeight = FontWeight.Black
                )
            )
            Spacer(Modifier.height(minDim * 0.05f))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = panelShape,
                colors = CardDefaults.cardColors(containerColor = Color(0xFF142C22)),
                elevation = CardDefaults.cardElevation(defaultElevation = minDim * 0.02f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = minDim * 0.05f, horizontal = minDim * 0.06f),
                    verticalArrangement = Arrangement.spacedBy(minDim * 0.02f)
                ) {
                    Text(
                        "Introduce el codigo de la sala",
                        style = TextStyle(
                            color = Color(0xFFE2D8C7),
                            fontSize = bodySize,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    OutlinedTextField(
                        value = state.roomCode,
                        onValueChange = { onCodigoChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = bodySize,
                            color = Color(0xFFF4F1E9)
                        )
                    )
                    Text(
                        "Aqui ira el ingreso de codigo",
                        style = TextStyle(
                            color = Color(0xFFB7AD9B),
                            fontSize = (minDim.value * 0.028f).sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    if (!errorMessage.isNullOrBlank()) {
                        Text(
                            errorMessage,
                            style = TextStyle(
                                color = Color(0xFFFFC2C2),
                                fontSize = (minDim.value * 0.028f).sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    Spacer(Modifier.height(minDim * 0.01f))
                    Button(
                        onClick = onUnirseSala,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(minDim * 0.09f),
                        shape = buttonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF0D79C),
                            contentColor = Color(0xFF1A1A1A)
                        )
                    ) {
                        Text(
                            "Crear sala",
                            style = TextStyle(
                                fontSize = bodySize,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}
