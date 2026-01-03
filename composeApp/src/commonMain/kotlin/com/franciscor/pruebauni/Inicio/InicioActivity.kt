package com.franciscor.pruebauni.Inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// UI de inicio/mesa. Esta capa solo renderiza estado y dispara acciones.
// Implementacion Firebase (resumen):
// 1) El ControladorInicio expone un StateFlow/UiState con datos de sala/jugadores/turno/mesa.
// 2) Desde el Composable solo llamas a acciones del controlador (crearSala, unirseSala, jugarCarta, robar).
// 3) El controlador escucha snapshots de Firestore y actualiza el estado; aqui solo lees ese estado.
// 4) Muestra estados de carga/error (sala inexistente, desconexion, permiso).


// Flujo Firebase que debes conectar a esta UI:
// - Pantalla Lobby: crear sala -> Firestore crea doc en "rooms" con codigo corto y estado inicial.
// - Unirse: el usuario ingresa el codigo, se agrega a "rooms/{id}/players".
// - Mesa: renderiza todos desde RoomState (mano propia, carta actual, turno, jugadores).
// - Acciones: jugar/robar pasan por ControladorInicio, que valida y escribe en Firestore.
@Composable
fun Tablero() {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F1EC))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val minDim = minOf(maxWidth, maxHeight)
        val outerPad = minDim * 0.035f
        val cardWidth = minDim * 0.18f
        val cardHeight = cardWidth * 1.45f
        val smallCardWidth = cardWidth * 0.58f
        val smallCardHeight = smallCardWidth * 1.45f
        val cardCorner = cardWidth * 0.12f
        val smallCorner = smallCardWidth * 0.12f
        val border = minDim * 0.006f
        val textSmall = (minDim.value * 0.03f).sp
        val textLarge = (minDim.value * 0.065f).sp
        val pileGap = minDim * 0.07f
        val stackOffset = minDim * 0.006f

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
                        "Tu turno",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = textSmall,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = minDim * 0.16f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText(
                    "J2 · 5 cartas",
                    style = TextStyle(color = Color.White, fontSize = textSmall)
                )
                Spacer(Modifier.height(minDim * 0.01f))
                Row(horizontalArrangement = Arrangement.spacedBy(minDim * 0.008f)) {
                    repeat(6) {
                        Box(
                            modifier = Modifier
                                .size(smallCardWidth, smallCardHeight)
                                .clip(smallCardShape)
                                .background(black)
                                .border(minDim * 0.002f, Color(0xFF222222), smallCardShape)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = minDim * 0.02f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(minDim * 0.008f)
            ) {
                BasicText(
                    "J3 · 4",
                    style = TextStyle(color = Color.White, fontSize = textSmall)
                )
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .size(smallCardWidth, smallCardHeight)
                            .clip(smallCardShape)
                            .background(black)
                            .border(minDim * 0.002f, Color(0xFF222222), smallCardShape)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = minDim * 0.02f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(minDim * 0.008f)
            ) {
                BasicText(
                    "J4 · 3",
                    style = TextStyle(color = Color.White, fontSize = textSmall)
                )
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(smallCardWidth, smallCardHeight)
                            .clip(smallCardShape)
                            .background(black)
                            .border(minDim * 0.002f, Color(0xFF222222), smallCardShape)
                    )
                }
            }

            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(pileGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(cardWidth, cardHeight)) {
                    repeat(3) { i ->
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
                        "ROBAR",
                        modifier = Modifier.align(Alignment.Center),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = textSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Box(Modifier.size(cardWidth, cardHeight)) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .clip(cardShape)
                            .background(black)
                            .border(minDim * 0.003f, Color(0xFF222222), cardShape)
                    )
                    Box(
                        Modifier
                            .matchParentSize()
                            .padding(cardWidth * 0.08f)
                            .clip(cardShape)
                            .background(Color(0xFF101010))
                            .rotate(-12f)
                    ) {
                        Row(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(1f).fillMaxHeight().background(red))
                            Box(Modifier.weight(1f).fillMaxHeight().background(yellow))
                            Box(Modifier.weight(1f).fillMaxHeight().background(green))
                            Box(Modifier.weight(1f).fillMaxHeight().background(blue))
                        }
                    }
                    BasicText(
                        "7",
                        modifier = Modifier.align(Alignment.Center),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = textLarge,
                            fontWeight = FontWeight.Black
                        )
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
                    "Tu mano",
                    style = TextStyle(color = Color.White, fontSize = textSmall)
                )
                Spacer(Modifier.height(minDim * 0.01f))
                Row(horizontalArrangement = Arrangement.spacedBy(minDim * 0.01f)) {
                    val handColors = listOf(red, yellow, green, blue, red, green, black)
                    val angles = listOf(-10f, -6f, -3f, 0f, 3f, 6f, 10f)
                    handColors.forEachIndexed { index, color ->
                        Box(
                            Modifier
                                .size(cardWidth * 0.9f, cardHeight * 0.9f)
                                .rotate(angles[index])
                                .clip(cardShape)
                                .background(Color.White)
                                .border(minDim * 0.003f, ink, cardShape)
                        ) {
                            Box(
                                Modifier
                                    .matchParentSize()
                                    .padding(cardWidth * 0.08f)
                                    .clip(cardShape)
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}

/*
Inicio basado en Firebase, sin peer-to-peer:
- El jugador elige "Crear sala" (host) o "Unirse" (invitado).
- Host crea el doc de sala y comparte el codigo.
- Invitado usa el codigo para unirse al doc y al sub-collection de players.
- UI siempre deriva del estado remoto; no uses estado local como fuente de verdad.
*/
data class CrearSalaState(
    val roomName: String = "",
    val maxPlayers: Int = 2,
    val isPrivate: Boolean = false
)
data class UnirseSalaState(
    val roomCode: String = "a"
)


sealed interface InicioScreen {
    data object Decide : InicioScreen
    data object Crear : InicioScreen
    data object Unirse : InicioScreen
}

@Composable
fun InicioFlow() {
    // Estado de la pantalla actual.
    var screen by remember { mutableStateOf<InicioScreen>(InicioScreen.Decide) }
    // Estado unificado del formulario de crear sala.
    var crearSalaState by remember { mutableStateOf(CrearSalaState()) }
    // Estado unificado del formulario de unirse sala.
    var unirseSalaState by remember { mutableStateOf(UnirseSalaState()) }
    // Selecciona que pantalla renderizar.
    when (screen) {
        // Pantalla de decision inicial.
        InicioScreen.Decide -> InicioParaDecidirHost(
            // Accion cuando elige crear sala.
            onCrearSala = {
                screen = InicioScreen.Crear
                          },
            // Accion cuando elige unirse.
            onUnirseSala = {

                screen = InicioScreen.Unirse

            }
        )
        // Pantalla de crear sala.
        InicioScreen.Crear -> CrearSala(
            // Estado actual del formulario.
            state = crearSalaState,
            // Actualiza nombre de sala.
            onRoomNameChange = { crearSalaState = crearSalaState.copy(roomName = it) },
            // Actualiza max jugadores.
            onMaxPlayersChange = { crearSalaState = crearSalaState.copy(maxPlayers = it) },
            // Actualiza privacidad.
            onPrivateChange = { crearSalaState = crearSalaState.copy(isPrivate = it) },
            // Placeholder de crear sala.
            onCrearSala = {

            },
            // Vuelve a la pantalla inicial.
            onVolver = { screen = InicioScreen.Decide }
        )
        // Pantalla de unirse (placeholder).
        InicioScreen.Unirse -> UnirsePlaceholder(
            state = unirseSalaState,
            // Vuelve a la pantalla inicial.
            onCodigoChange = {
                unirseSalaState = unirseSalaState.copy(roomCode = it)

            },
            onVolver = { screen = InicioScreen.Decide },
            onUnirseSala = {

            },

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
    onRoomNameChange: (String) -> Unit,
    onMaxPlayersChange: (Int) -> Unit,
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
                            onValueChange = { value ->
                                onMaxPlayersChange(value.roundToInt().coerceIn(2, 6))
                            },
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
