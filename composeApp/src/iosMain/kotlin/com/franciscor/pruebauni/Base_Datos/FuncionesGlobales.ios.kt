package com.franciscor.pruebauni.Base_Datos

actual object FuncionesGlobales {
    actual var ultimoCodigoCreado: String? = null

    actual suspend fun registrarUsuario(nombre: String, correo: String, contrasena: String) {
        error("Firebase iOS no esta implementado en Kotlin aun")
    }

    actual suspend fun crearPartida() {
        error("Firebase iOS no esta implementado en Kotlin aun")
    }

    actual suspend fun unirseAPartida(codigo: String) {
        error("Firebase iOS no esta implementado en Kotlin aun")
    }

    actual suspend fun leerSalaPorCodigo(codigo: String): DbSala? {
        error("Firebase iOS no esta implementado en Kotlin aun")
    }

    actual fun escucharSalaPorCodigo(codigo: String, onCambio: (DbSala?) -> Unit): ListenerHandle {
        error("Firebase iOS no esta implementado en Kotlin aun")
    }
}
