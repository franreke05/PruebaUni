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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.franciscor.pruebauni.Lobby.LobbyFlow

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
    // Estado de la pantalla actual.
    var uiState by remember { mutableStateOf(InicioUiState()) }
    val canCreateLobby = ControladorInicio.puedeCrearSala(uiState.crearSalaState)
    // Selecciona que pantalla renderizar.
    when (val current = uiState.screen) {
        // Pantalla de decision inicial.
        InicioScreen.Decide -> InicioParaDecidirHost(
            // Accion cuando elige crear sala.
            onCrearSala = {
                uiState = ControladorInicio.irACrear(uiState)
                          },
            // Accion cuando elige unirse.
            onUnirseSala = {
                uiState = ControladorInicio.irAUnirse(uiState)

            }
        )
        // Pantalla de crear sala.
        InicioScreen.Crear -> CrearSala(
            // Estado actual del formulario.
            state = uiState.crearSalaState,
            canCreate = canCreateLobby,
            // Actualiza nombre de sala.
            onRoomNameChange = { uiState = ControladorInicio.actualizarNombreSala(uiState, it) },
            // Actualiza max jugadores.
            onMaxPlayersChange = { uiState = ControladorInicio.actualizarMaxPlayersDesdeSlider(uiState, it) },
            // Actualiza privacidad.
            onPrivateChange = { uiState = ControladorInicio.actualizarPrivacidad(uiState, it) },
            // Placeholder de crear sala.
            onCrearSala = {
                uiState = ControladorInicio.crearLobby(uiState)
            },
            // Vuelve a la pantalla inicial.
            onVolver = { uiState = ControladorInicio.irADecide(uiState) }
        )
        // Pantalla de unirse (placeholder).
        InicioScreen.Unirse -> UnirsePlaceholder(
            state = uiState.unirseSalaState,
            // Vuelve a la pantalla inicial.
            onCodigoChange = {
                uiState = ControladorInicio.actualizarCodigoSala(uiState, it)

            },
            onVolver = { uiState = ControladorInicio.irADecide(uiState) },
            onUnirseSala = {

            },

        )
        is InicioScreen.Lobby -> LobbyFlow(
            state = current.lobbyState,
            onVolver = { uiState = ControladorInicio.irADecide(uiState) },
            onToggleConfig = { uiState = ControladorInicio.toggleConfigLobby(uiState) },
            onCartasPorJugadorChange = { uiState = ControladorInicio.actualizarCartasPorJugadorLobby(uiState, it) },
            onEspecialesChange = { uiState = ControladorInicio.actualizarEspecialesLobby(uiState, it) },
            onMaxRobarChange = { uiState = ControladorInicio.actualizarMaxRobarLobby(uiState, it) },
            onEmpezarPartida = { uiState = ControladorInicio.empezarPartida(uiState) }
        )
    }
}

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

@Composable
fun UnirsePlaceholder(
    state: UnirseSalaState,
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
