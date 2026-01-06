package com.franciscor.pruebauni.Partida

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val PREFS_NAME = "partida_prefs"
private const val KEY_ROOM_NAME = "room_name"
private const val KEY_NUM_PLAYERS = "num_players"

private class AndroidPartidaStorage(
    context: Context
) : PartidaStorage {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): PartidaPrefs? {
        val name = prefs.getString(KEY_ROOM_NAME, null) ?: return null
        val players = prefs.getInt(KEY_NUM_PLAYERS, 0)
        if (players <= 0) {
            return null
        }
        return PartidaPrefs(roomName = name, numPlayers = players)
    }

    override fun save(prefs: PartidaPrefs) {
        this.prefs.edit()
            .putString(KEY_ROOM_NAME, prefs.roomName)
            .putInt(KEY_NUM_PLAYERS, prefs.numPlayers)
            .apply()
    }
}

@Composable
actual fun rememberPartidaStorage(): PartidaStorage {
    val context = LocalContext.current
    return remember(context) { AndroidPartidaStorage(context) }
}
