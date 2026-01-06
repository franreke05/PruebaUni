package com.franciscor.pruebauni.Lobby

import kotlin.math.roundToInt
import kotlin.random.Random

data class LobbyPlayer(
    val name: String,
    val tag: String,
    val isHost: Boolean = false
)

data class LobbyUiState(
    val roomName: String,
    val roomCode: String,
    val maxPlayers: Int,
    val players: List<LobbyPlayer>,
    val config: LobbyConfig,
    val showConfig: Boolean = false,
    val isLocalHost: Boolean = false,
    val isStarted: Boolean = false
)

data class LobbyConfig(
    val cardsPerPlayer: Int = 7,
    val specialCardsPercent: Int = 20,
    val maxDrawCards: Int = 2
)

object ControladorLobby {
    fun puedeCrearSala(roomName: String): Boolean = roomName.trim().isNotEmpty()

    fun crearLobbyState(roomName: String, maxPlayers: Int): LobbyUiState {
        val normalizedMax = maxPlayers.coerceIn(2, 6)
        val code = generarCodigoSala()
        val players = listOf(
            LobbyPlayer(name = "Jugador 1", tag = "0001", isHost = true)
        )
        return LobbyUiState(
            roomName = roomName,
            roomCode = code,
            maxPlayers = normalizedMax,
            players = players,
            config = LobbyConfig(),
            isLocalHost = true,
            isStarted = false
        )
    }

    fun generarCodigoSala(): String = Random.nextInt(1000, 10000).toString()

    fun toggleConfig(state: LobbyUiState): LobbyUiState =
        state.copy(showConfig = !state.showConfig)

    fun actualizarCartasPorJugador(state: LobbyUiState, value: Float): LobbyUiState {
        val normalized = value.roundToInt().coerceIn(3, 15)
        return state.copy(config = state.config.copy(cardsPerPlayer = normalized))
    }

    fun actualizarEspeciales(state: LobbyUiState, value: Float): LobbyUiState {
        val normalized = value.roundToInt().coerceIn(0, 100)
        return state.copy(config = state.config.copy(specialCardsPercent = normalized))
    }

    fun actualizarMaxRobar(state: LobbyUiState, value: Float): LobbyUiState {
        val normalized = value.roundToInt().coerceIn(1, 6)
        return state.copy(config = state.config.copy(maxDrawCards = normalized))
    }
}
