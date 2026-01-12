package com.franciscor.pruebauni.Inicio

import com.franciscor.pruebauni.Lobby.ControladorLobby
import com.franciscor.pruebauni.Lobby.LobbyUiState
import com.franciscor.pruebauni.Partida.PartidaUiState
import kotlin.math.roundToInt

// Controlador de Inicio (orquestacion de Firebase).
/*
Implementacion minima (solo identidad):
1) Identidad: Firebase Auth anonimo -> uid estable.
2) Registro: users/{uid} con name, createdAt y lastSeen.
3) Sala opcional: rooms/{roomId} con seats {A, B, C, D} y code.
4) No guardes estado del juego en BD; solo identificacion.
*/

data class CrearSalaState(
    val roomName: String = "",
    val maxPlayers: Int = 2,
    val isPrivate: Boolean = false
)

data class UnirseSalaState(
    val roomCode: String = ""
)

sealed interface InicioScreen {
    data object Decide : InicioScreen
    data object Crear : InicioScreen
    data object Unirse : InicioScreen
    data class Lobby(val lobbyState: LobbyUiState) : InicioScreen
    data class Partida(val partidaState: PartidaUiState) : InicioScreen
}

data class InicioUiState(
    val screen: InicioScreen = InicioScreen.Decide,
    val crearSalaState: CrearSalaState = CrearSalaState(),
    val unirseSalaState: UnirseSalaState = UnirseSalaState(),
    val suppressLobbyAutoStart: Boolean = false
)

object ControladorInicio {
    fun puedeCrearSala(state: CrearSalaState): Boolean =
        ControladorLobby.puedeCrearSala(state.roomName)

    fun irADecide(state: InicioUiState): InicioUiState =
        state.copy(screen = InicioScreen.Decide, suppressLobbyAutoStart = false)

    fun irACrear(state: InicioUiState): InicioUiState =
        state.copy(screen = InicioScreen.Crear, suppressLobbyAutoStart = false)

    fun irAUnirse(state: InicioUiState): InicioUiState =
        state.copy(screen = InicioScreen.Unirse, suppressLobbyAutoStart = false)

    fun actualizarNombreSala(state: InicioUiState, name: String): InicioUiState =
        state.copy(crearSalaState = state.crearSalaState.copy(roomName = name))

    fun actualizarMaxPlayers(state: InicioUiState, maxPlayers: Int): InicioUiState =
        state.copy(crearSalaState = state.crearSalaState.copy(maxPlayers = maxPlayers))

    fun actualizarMaxPlayersDesdeSlider(state: InicioUiState, value: Float): InicioUiState {
        val normalized = value.roundToInt().coerceIn(2, 6)
        return actualizarMaxPlayers(state, normalized)
    }

    fun actualizarPrivacidad(state: InicioUiState, isPrivate: Boolean): InicioUiState =
        state.copy(crearSalaState = state.crearSalaState.copy(isPrivate = isPrivate))

    fun actualizarCodigoSala(state: InicioUiState, code: String): InicioUiState =
        state.copy(unirseSalaState = state.unirseSalaState.copy(roomCode = code))

    fun crearLobby(state: InicioUiState): InicioUiState {
        if (!puedeCrearSala(state.crearSalaState)) {
            return state
        }
        val lobbyState = ControladorLobby.crearLobbyState(
            state.crearSalaState.roomName,
            state.crearSalaState.maxPlayers
        )
        return state.copy(screen = InicioScreen.Lobby(lobbyState), suppressLobbyAutoStart = false)
    }

    fun toggleConfigLobby(state: InicioUiState): InicioUiState =
        actualizarLobbyState(state) { ControladorLobby.toggleConfig(it) }

    fun actualizarCartasPorJugadorLobby(state: InicioUiState, value: Float): InicioUiState =
        actualizarLobbyState(state) { ControladorLobby.actualizarCartasPorJugador(it, value) }

    fun actualizarEspecialesLobby(state: InicioUiState, value: Float): InicioUiState =
        actualizarLobbyState(state) { ControladorLobby.actualizarEspeciales(it, value) }

    fun actualizarMaxRobarLobby(state: InicioUiState, value: Float): InicioUiState =
        actualizarLobbyState(state) { ControladorLobby.actualizarMaxRobar(it, value) }

    fun actualizarTiempoTurnoLobby(state: InicioUiState, value: Float): InicioUiState =
        actualizarLobbyState(state) { ControladorLobby.actualizarTiempoTurno(it, value) }

    fun empezarPartida(state: InicioUiState): InicioUiState {
        val screen = state.screen
        if (screen !is InicioScreen.Lobby) {
            return state
        }
        val lobby = screen.lobbyState
        if (lobby.players.size < 2) {
            return state
        }
        val partidaState = PartidaUiState(
            roomCode = lobby.roomCode,
            roomName = lobby.roomName,
            numPlayers = lobby.players.size,
            cardsPerPlayer = lobby.config.cardsPerPlayer,
            maxDrawCards = lobby.config.maxDrawCards,
            turnDurationSeconds = lobby.config.turnDurationSeconds,
            isLocalHost = lobby.isLocalHost
        )
        return state.copy(
            screen = InicioScreen.Partida(partidaState),
            suppressLobbyAutoStart = false
        )
    }

    private fun actualizarLobbyState(
        state: InicioUiState,
        updater: (LobbyUiState) -> LobbyUiState
    ): InicioUiState {
        val screen = state.screen
        if (screen !is InicioScreen.Lobby) {
            return state
        }
        return state.copy(screen = screen.copy(lobbyState = updater(screen.lobbyState)))
    }
}
