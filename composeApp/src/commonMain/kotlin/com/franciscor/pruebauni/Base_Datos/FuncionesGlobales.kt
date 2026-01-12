package com.franciscor.pruebauni.Base_Datos

import com.franciscor.pruebauni.Dataclass.Carta
import com.franciscor.pruebauni.Dataclass.CartaEspecial
import com.franciscor.pruebauni.Partida.crearCartaAleatoria
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.DataSnapshot
import dev.gitlive.firebase.database.DatabaseReference
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.random.Random

private const val DATABASE_URL = "https://pruebaunogame-default-rtdb.europe-west1.firebasedatabase.app/"
private const val LOBBY_PATH = "uno/lobby"
private const val PARTIDA_PATH = "uno/partida"
private const val CODE_LENGTH = 4

data class DbJugador(
    val uid: String,
    val nombre: String,
    val numero: Int,
    val joinedAtMillis: Long? = null
)

data class DbLobbyConfig(
    val cardsPerPlayer: Int = 7,
    val specialCardsPercent: Int = 20,
    val maxDrawCards: Int = 2,
    val turnDurationSeconds: Int = 10
)

data class DbSala(
    val id: String,
    val codigo: String,
    val nombre: String,
    val hostUid: String,
    val maxPlayers: Int,
    val playerCount: Int,
    val jugadores: List<DbJugador>,
    val config: DbLobbyConfig = DbLobbyConfig(),
    val started: Boolean = false,
    val partidaId: String? = null,
    val createdAtMillis: Long? = null
)

data class DbPartidaEstado(
    val mesaCarta: Carta?,
    val mesaVersion: Long,
    val turnos: List<Boolean>,
    val turnMesaVersion: Long
)

interface ListenerHandle {
    fun remove()
}

object FuncionesGlobales {
    var ultimoCodigoCreado: String? = null

    private val db by lazy { Firebase.database(DATABASE_URL) }
    private var localPlayerId: String? = null

    fun obtenerJugadorId(): String = getLocalPlayerId()

    suspend fun crearPartida(nombreSala: String, maxPlayers: Int) {
        val playerId = getLocalPlayerId()
        val codigo = generarCodigoUnico()
        val normalizedMax = maxPlayers.coerceAtLeast(2)
        val numero = 1
        val nombre = "Jugador ${numero.toString().padStart(3, '0')}"
        val jugador = RtdbPlayer(
            uid = playerId,
            name = nombre,
            number = numero,
            joinedAt = null
        )
        val lobbyData = RtdbLobby(
            code = codigo,
            name = nombreSala,
            hostUid = playerId,
            maxPlayers = normalizedMax,
            playerCount = 1,
            players = mapOf(playerId to jugador),
            config = RtdbLobbyConfig(),
            started = false,
            partidaId = null,
            createdAt = null
        )

        lobbyRef(codigo).setValue(lobbyData)
        ultimoCodigoCreado = codigo
    }

    suspend fun unirseAPartida(codigo: String) {
        val playerId = getLocalPlayerId()
        val salaSnapshot = lobbyRef(codigo).valueEvents.first()
        if (!salaSnapshot.exists) {
            return
        }
        val salaRef = salaSnapshot.ref

        salaRef.runTransaction(RtdbLobby.serializer().nullable) { current ->
            val sala = current ?: return@runTransaction current
            if (sala.started) {
                throw IllegalStateException("La partida ya inicio")
            }
            val maxPlayers = if (sala.maxPlayers > 0) sala.maxPlayers else 2
            val playersMap = sala.players.toMutableMap()
            if (playersMap.containsKey(playerId)) {
                return@runTransaction sala
            }
            val usedNumbers = playersMap.values.mapNotNull { player ->
                player.number.takeIf { it > 0 }
            }.toSet()
            val nextNumber = (1..maxPlayers).firstOrNull { it !in usedNumbers }
            val currentCount = if (sala.playerCount > 0) sala.playerCount else playersMap.size
            if (nextNumber == null || currentCount >= maxPlayers) {
                throw IllegalStateException("Sala llena")
            }
            val nombre = "Jugador ${nextNumber.toString().padStart(3, '0')}"
            playersMap[playerId] = RtdbPlayer(
                uid = playerId,
                name = nombre,
                number = nextNumber,
                joinedAt = null
            )
            sala.copy(
                playerCount = maxOf(currentCount, playersMap.size),
                players = playersMap
            )
        }
    }

    suspend fun iniciarPartida(codigo: String, config: DbLobbyConfig) {
        val playerId = getLocalPlayerId()
        val lobbySnapshot = lobbyRef(codigo).valueEvents.first()
        val lobby = lobbySnapshot.value<RtdbLobby>()
        if (lobby.hostUid != playerId || lobby.started) {
            return
        }
        val playerCount = if (lobby.playerCount > 0) lobby.playerCount else lobby.players.size
        if (playerCount < 2) {
            return
        }
        val mesaInicial = crearCartaAleatoria(0)
        val orderedPlayers = lobby.players.entries.sortedBy { entry ->
            val number = entry.value.number
            if (number > 0) number else Int.MAX_VALUE
        }
        val turnos = orderedPlayers.mapIndexed { index, _ -> index == 0 }
        val mazos = orderedPlayers.associate { entry ->
            entry.key to emptyList<RtdbCarta>()
        }
        val partidaData = RtdbPartida(
            code = codigo,
            players = lobby.players,
            turnos = turnos,
            mesaCarta = mesaInicial.toRtdbCarta(),
            mesaVersion = 1L,
            turnMesaVersion = 1L,
            mazos = mazos,
            config = RtdbLobbyConfig(
                cardsPerPlayer = config.cardsPerPlayer,
                specialCardsPercent = config.specialCardsPercent,
                maxDrawCards = config.maxDrawCards,
                turnDurationSeconds = config.turnDurationSeconds
            ),
            createdAt = null
        )

        val updates = mapOf(
            "$PARTIDA_PATH/$codigo" to partidaData,
            "$LOBBY_PATH/$codigo/started" to true,
            "$LOBBY_PATH/$codigo/partidaId" to codigo,
            "$LOBBY_PATH/$codigo/config" to RtdbLobbyConfig(
                cardsPerPlayer = config.cardsPerPlayer,
                specialCardsPercent = config.specialCardsPercent,
                maxDrawCards = config.maxDrawCards,
                turnDurationSeconds = config.turnDurationSeconds
            )
        )
        db.reference().updateChildren(updates)
    }

    suspend fun finalizarPartida(codigo: String) {
        if (codigo.isBlank()) {
            return
        }
        val playerId = getLocalPlayerId()
        val lobbySnapshot = lobbyRef(codigo).valueEvents.first()
        val lobby = lobbySnapshot.value<RtdbLobby>() ?: return
        if (lobby.hostUid != playerId) {
            return
        }
        val updates = mapOf<String, Any?>(
            "$LOBBY_PATH/$codigo/started" to false,
            "$LOBBY_PATH/$codigo/partidaId" to null,
            "$PARTIDA_PATH/$codigo" to null
        )
        db.reference().updateChildren(updates)
    }

    suspend fun actualizarConfigLobby(codigo: String, config: DbLobbyConfig) {
        val configData = RtdbLobbyConfig(
            cardsPerPlayer = config.cardsPerPlayer,
            specialCardsPercent = config.specialCardsPercent,
            maxDrawCards = config.maxDrawCards,
            turnDurationSeconds = config.turnDurationSeconds
        )
        lobbyRef(codigo).child("config").setValue(configData)
    }

    suspend fun leerSalaPorCodigo(codigo: String): DbSala? {
        val snapshot = lobbyRef(codigo).valueEvents.first()
        return snapshot.toDbSala()
    }

    suspend fun obtenerIndiceJugadorEnPartida(codigo: String): Int? {
        val playerId = getLocalPlayerId()
        val partidaSnapshot = partidaRef(codigo).valueEvents.first()
        val partida = runCatching { partidaSnapshot.value<RtdbPartida>() }.getOrNull() ?: return null
        val orderedPlayers = partida.players.entries.sortedBy { entry ->
            val number = entry.value.number
            if (number > 0) number else Int.MAX_VALUE
        }
        val index = orderedPlayers.indexOfFirst { it.key == playerId }
        return index.takeIf { it >= 0 }
    }

    suspend fun actualizarMesaCarta(codigo: String, carta: Carta) {
        val serializer = RtdbPartida.serializer().nullable
        partidaRef(codigo).runTransaction(serializer) { current ->
            val partida = current ?: return@runTransaction current
            val nextVersion = (partida.mesaVersion.takeIf { it > 0 } ?: 0L) + 1L
            partida.copy(
                mesaCarta = carta.toRtdbCarta(),
                mesaVersion = nextVersion
            )
        }
    }

    suspend fun avanzarTurno(codigo: String, direction: Int) {
        val safeDirection = if (direction >= 0) 1 else -1
        val serializer = RtdbPartida.serializer().nullable
        partidaRef(codigo).runTransaction(serializer) { current ->
            val partida = current ?: return@runTransaction current
            val turnos = partida.turnos
            if (turnos.isEmpty()) {
                return@runTransaction partida
            }
            val currentIndex = turnos.indexOfFirst { it }
            val resolvedIndex = if (currentIndex >= 0) currentIndex else 0
            val nextIndex = ((resolvedIndex + safeDirection) % turnos.size + turnos.size) % turnos.size
            val nextTurnos = List(turnos.size) { index -> index == nextIndex }
            val mesaVersion = if (partida.mesaVersion > 0) partida.mesaVersion else 0L
            partida.copy(
                turnos = nextTurnos,
                turnMesaVersion = mesaVersion
            )
        }
    }

    fun escucharMesaCarta(codigo: String, onCambio: (Carta?) -> Unit): ListenerHandle {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val job = scope.launch {
            partidaRef(codigo)
                .child("mesaCarta")
                .valueEvents
                .collect { snapshot ->
                    val carta = runCatching { snapshot.value<RtdbCarta?>() }.getOrNull()
                    onCambio(carta?.toCarta())
                }
        }
        return JobListenerHandle(job)
    }

    fun escucharTurnos(codigo: String, onCambio: (List<Boolean>) -> Unit): ListenerHandle {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val job = scope.launch {
            partidaRef(codigo)
                .child("turnos")
                .valueEvents
                .collect { snapshot ->
                    val turnos = runCatching { snapshot.value<List<Boolean>>() }.getOrNull() ?: emptyList()
                    onCambio(turnos)
                }
        }
        return JobListenerHandle(job)
    }

    fun escucharPartidaEstado(codigo: String, onCambio: (DbPartidaEstado) -> Unit): ListenerHandle {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val job = scope.launch {
            partidaRef(codigo)
                .valueEvents
                .collect { snapshot ->
                    val partida = runCatching { snapshot.value<RtdbPartida>() }.getOrNull()
                    onCambio(
                        DbPartidaEstado(
                            mesaCarta = partida?.mesaCarta?.toCarta(),
                            mesaVersion = partida?.mesaVersion ?: 0L,
                            turnos = partida?.turnos ?: emptyList(),
                            turnMesaVersion = partida?.turnMesaVersion ?: 0L
                        )
                    )
                }
        }
        return JobListenerHandle(job)
    }

    fun escucharSalaPorCodigo(codigo: String, onCambio: (DbSala?) -> Unit): ListenerHandle {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val job = scope.launch {
            lobbyRef(codigo)
                .valueEvents
                .collect { snapshot ->
                    onCambio(snapshot.toDbSala())
                }
        }
        return JobListenerHandle(job)
    }

    private suspend fun generarCodigoUnico(): String {
        repeat(25) {
            val codigo = Random.nextInt(0, 10_000).toString().padStart(CODE_LENGTH, '0')
            val exists = lobbyRef(codigo).valueEvents.first().exists
            if (!exists) {
                return codigo
            }
        }
        throw IllegalStateException("No se pudo generar un codigo unico")
    }

    private fun lobbyRef(codigo: String): DatabaseReference =
        db.reference("$LOBBY_PATH/$codigo")

    private fun partidaRef(codigo: String): DatabaseReference =
        db.reference("$PARTIDA_PATH/$codigo")

    private fun getLocalPlayerId(): String {
        val current = localPlayerId
        if (!current.isNullOrBlank()) {
            return current
        }
        val generated = "local-${Random.nextInt(0, Int.MAX_VALUE)}"
        localPlayerId = generated
        return generated
    }
}

private class JobListenerHandle(
    private val job: Job
) : ListenerHandle {
    override fun remove() {
        job.cancel()
    }
}

private fun DataSnapshot.toDbSala(): DbSala? {
    if (!exists) return null
    val lobby = runCatching { value<RtdbLobby>() }.getOrNull() ?: return null
    val jugadores = lobby.players.map { (uid, player) ->
        val resolvedUid = player.uid.ifBlank { uid }
        DbJugador(
            uid = resolvedUid,
            nombre = player.name,
            numero = player.number,
            joinedAtMillis = player.joinedAt
        )
    }
    val normalizedCount = if (lobby.playerCount > 0) lobby.playerCount else jugadores.size
    return DbSala(
        id = key.orEmpty(),
        codigo = lobby.code,
        nombre = lobby.name,
        hostUid = lobby.hostUid,
        maxPlayers = lobby.maxPlayers,
        playerCount = normalizedCount,
        jugadores = jugadores,
        config = DbLobbyConfig(
            cardsPerPlayer = lobby.config.cardsPerPlayer,
            specialCardsPercent = lobby.config.specialCardsPercent,
            maxDrawCards = lobby.config.maxDrawCards,
            turnDurationSeconds = lobby.config.turnDurationSeconds
        ),
        started = lobby.started,
        partidaId = lobby.partidaId,
        createdAtMillis = lobby.createdAt
    )
}

private fun Carta.toRtdbCarta(): RtdbCarta =
    RtdbCarta(
        imagen = imagen,
        numero = numero,
        color = color,
        isSpecial = isSpecial,
        specialType = specialType?.name
    )

private fun RtdbCarta.toCarta(): Carta {
    val resolvedColor = color ?: "wild"
    val resolvedSpecial = specialType?.let { runCatching { CartaEspecial.valueOf(it) }.getOrNull() }
    return Carta(
        imagen = imagen.orEmpty(),
        numero = numero,
        color = resolvedColor,
        isSpecial = isSpecial || resolvedSpecial != null,
        specialType = resolvedSpecial
    )
}

@Serializable
private data class RtdbPlayer(
    val uid: String = "",
    val name: String = "",
    val number: Int = 0,
    val joinedAt: Long? = null
)

@Serializable
private data class RtdbLobbyConfig(
    val cardsPerPlayer: Int = 7,
    val specialCardsPercent: Int = 20,
    val maxDrawCards: Int = 2,
    val turnDurationSeconds: Int = 10
)

@Serializable
private data class RtdbLobby(
    val code: String = "",
    val name: String = "",
    val hostUid: String = "",
    val maxPlayers: Int = 0,
    val playerCount: Int = 0,
    val players: Map<String, RtdbPlayer> = emptyMap(),
    val config: RtdbLobbyConfig = RtdbLobbyConfig(),
    @Serializable(with = FlexibleBooleanSerializer::class)
    val started: Boolean = false,
    val partidaId: String? = null,
    val createdAt: Long? = null
)

private object FlexibleBooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleBoolean", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean {
        return try {
            decoder.decodeBoolean()
        } catch (_: Exception) {
            val number = try {
                decoder.decodeLong()
            } catch (_: Exception) {
                val intValue = try {
                    decoder.decodeInt()
                } catch (_: Exception) {
                    val text = try {
                        decoder.decodeString()
                    } catch (_: Exception) {
                        return false
                    }
                    return when (text.trim().lowercase()) {
                        "true", "1", "yes" -> true
                        else -> false
                    }
                }
                intValue.toLong()
            }
            number != 0L
        }
    }

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeBoolean(value)
    }
}

@Serializable
private data class RtdbCarta(
    val imagen: String? = null,
    val numero: Int? = null,
    val color: String? = null,
    val isSpecial: Boolean = false,
    val specialType: String? = null
)

@Serializable
private data class RtdbPartida(
    val code: String = "",
    val players: Map<String, RtdbPlayer> = emptyMap(),
    val turnos: List<Boolean> = emptyList(),
    val mesaCarta: RtdbCarta? = null,
    val mesaVersion: Long = 0,
    val turnMesaVersion: Long = 0,
    val mazos: Map<String, List<RtdbCarta>> = emptyMap(),
    val config: RtdbLobbyConfig = RtdbLobbyConfig(),
    val createdAt: Long? = null
)
