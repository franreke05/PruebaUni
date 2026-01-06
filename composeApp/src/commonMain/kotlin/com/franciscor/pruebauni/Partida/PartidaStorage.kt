package com.franciscor.pruebauni.Partida

import androidx.compose.runtime.Composable

data class PartidaPrefs(
    val roomName: String,
    val numPlayers: Int
)

interface PartidaStorage {
    fun load(): PartidaPrefs?
    fun save(prefs: PartidaPrefs)
}

@Composable
expect fun rememberPartidaStorage(): PartidaStorage
