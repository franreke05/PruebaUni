package com.franciscor.pruebauni.Partida

data class PartidaUiState(
    val roomCode: String,
    val roomName: String,
    val numPlayers: Int,
    val cardsPerPlayer: Int = 7,
    val maxDrawCards: Int = 2,
    val turnDurationSeconds: Int = 10,
    val isLocalTurn: Boolean = true,
    val isLocalHost: Boolean = false
)
