package com.franciscor.pruebauni.Partida

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

private const val KEY_ROOM_NAME = "room_name"
private const val KEY_NUM_PLAYERS = "num_players"

private class IosPartidaStorage : PartidaStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun load(): PartidaPrefs? {
        val name = defaults.stringForKey(KEY_ROOM_NAME) ?: return null
        val players = defaults.integerForKey(KEY_NUM_PLAYERS).toInt()
        if (players <= 0) {
            return null
        }
        return PartidaPrefs(roomName = name, numPlayers = players)
    }

    override fun save(prefs: PartidaPrefs) {
        defaults.setObject(prefs.roomName, forKey = KEY_ROOM_NAME)
        defaults.setInteger(prefs.numPlayers.toLong(), forKey = KEY_NUM_PLAYERS)
    }
}

@Composable
actual fun rememberPartidaStorage(): PartidaStorage = remember { IosPartidaStorage() }
