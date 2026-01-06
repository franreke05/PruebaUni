package com.franciscor.pruebauni.Base_Datos

data class DbUsuario(
    val uid: String,
    val nombre: String,
    val correo: String,
    val createdAtMillis: Long? = null,
    val lastSeenMillis: Long? = null
)

data class DbJugador(
    val uid: String,
    val nombre: String,
    val joinedAtMillis: Long? = null
)

data class DbSala(
    val id: String,
    val codigo: String,
    val hostUid: String,
    val jugadores: List<DbJugador>,
    val createdAtMillis: Long? = null
)

interface ListenerHandle {
    fun remove()
}

expect object FuncionesGlobales {
    var ultimoCodigoCreado: String?

    suspend fun registrarUsuario(nombre: String, correo: String, contrasena: String)
    suspend fun crearPartida()
    suspend fun unirseAPartida(codigo: String)

    suspend fun leerSalaPorCodigo(codigo: String): DbSala?
    fun escucharSalaPorCodigo(codigo: String, onCambio: (DbSala?) -> Unit): ListenerHandle
}
