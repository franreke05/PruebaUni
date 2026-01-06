package com.franciscor.pruebauni.Base_Datos

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

private const val USERS_COLLECTION = "users"
private const val ROOMS_COLLECTION = "rooms"
private const val CODE_LENGTH = 4

actual object FuncionesGlobales {
    actual var ultimoCodigoCreado: String? = null

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    actual suspend fun registrarUsuario(nombre: String, correo: String, contrasena: String) {
        val isNewUser = try {
            auth.createUserWithEmailAndPassword(correo, contrasena).await()
            true
        } catch (e: FirebaseAuthUserCollisionException) {
            auth.signInWithEmailAndPassword(correo, contrasena).await()
            false
        }

        val uid = requireNotNull(auth.currentUser?.uid) { "Usuario no autenticado" }
        val update = mutableMapOf<String, Any>(
            "name" to nombre,
            "email" to correo,
            "lastSeen" to FieldValue.serverTimestamp()
        )
        if (isNewUser) {
            update["createdAt"] = FieldValue.serverTimestamp()
        }
        db.collection(USERS_COLLECTION).document(uid).set(update, SetOptions.merge()).await()
    }

    actual suspend fun crearPartida() {
        val user = requireNotNull(auth.currentUser) { "Usuario no autenticado" }
        val nombre = obtenerNombreUsuario(user.uid, user.email)
        val codigo = generarCodigoUnico()

        val jugador = mapOf(
            "uid" to user.uid,
            "name" to nombre,
            "joinedAt" to FieldValue.serverTimestamp()
        )

        val salaData = mapOf(
            "code" to codigo,
            "hostUid" to user.uid,
            "players" to mapOf(user.uid to jugador),
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection(ROOMS_COLLECTION).document().set(salaData).await()
        ultimoCodigoCreado = codigo
    }

    actual suspend fun unirseAPartida(codigo: String) {
        val user = requireNotNull(auth.currentUser) { "Usuario no autenticado" }
        val salaRef = buscarSalaPorCodigo(codigo) ?: return
        val nombre = obtenerNombreUsuario(user.uid, user.email)

        val jugador = mapOf(
            "uid" to user.uid,
            "name" to nombre,
            "joinedAt" to FieldValue.serverTimestamp()
        )

        salaRef.set(mapOf("players.${user.uid}" to jugador), SetOptions.merge()).await()
    }

    actual suspend fun leerSalaPorCodigo(codigo: String): DbSala? {
        val snapshot = db.collection(ROOMS_COLLECTION)
            .whereEqualTo("code", codigo)
            .limit(1)
            .get()
            .await()

        val doc = snapshot.documents.firstOrNull() ?: return null
        return doc.toDbSala()
    }

    actual fun escucharSalaPorCodigo(codigo: String, onCambio: (DbSala?) -> Unit): ListenerHandle {
        val registration = db.collection(ROOMS_COLLECTION)
            .whereEqualTo("code", codigo)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onCambio(null)
                    return@addSnapshotListener
                }
                val doc = snapshot?.documents?.firstOrNull()
                onCambio(doc?.toDbSala())
            }

        return AndroidListenerHandle(registration)
    }

    private suspend fun obtenerNombreUsuario(uid: String, correo: String?): String {
        val doc = db.collection(USERS_COLLECTION).document(uid).get().await()
        val nombre = doc.getString("name")
        return nombre ?: correo?.substringBefore("@") ?: "Jugador"
    }

    private suspend fun generarCodigoUnico(): String {
        repeat(25) {
            val codigo = Random.nextInt(0, 10_000).toString().padStart(CODE_LENGTH, '0')
            val existe = db.collection(ROOMS_COLLECTION)
                .whereEqualTo("code", codigo)
                .limit(1)
                .get()
                .await()
                .documents
                .isNotEmpty()
            if (!existe) {
                return codigo
            }
        }
        throw IllegalStateException("No se pudo generar un codigo unico")
    }

    private suspend fun buscarSalaPorCodigo(codigo: String) =
        db.collection(ROOMS_COLLECTION)
            .whereEqualTo("code", codigo)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.reference

    private fun DocumentSnapshot.toDbSala(): DbSala? {
        val data = data ?: return null

        val codigo = data["code"] as? String ?: ""
        val hostUid = data["hostUid"] as? String ?: ""
        val createdAtMillis = (data["createdAt"] as? Timestamp)?.toDate()?.time

        val jugadores = parseJugadores(data["players"])
        return DbSala(
            id = id,
            codigo = codigo,
            hostUid = hostUid,
            jugadores = jugadores,
            createdAtMillis = createdAtMillis
        )
    }

    private fun parseJugadores(playersData: Any?): List<DbJugador> {
        val map = playersData as? Map<*, *> ?: return emptyList()
        return map.mapNotNull { (uid, value) ->
            val uidStr = uid as? String ?: return@mapNotNull null
            val playerMap = value as? Map<*, *> ?: return@mapNotNull null
            val nombre = playerMap["name"] as? String ?: "Jugador"
            val joinedAtMillis = (playerMap["joinedAt"] as? Timestamp)?.toDate()?.time
            DbJugador(uid = uidStr, nombre = nombre, joinedAtMillis = joinedAtMillis)
        }
    }
}

private class AndroidListenerHandle(
    private val registration: ListenerRegistration
) : ListenerHandle {
    override fun remove() {
        registration.remove()
    }
}
