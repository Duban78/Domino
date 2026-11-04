package com.example.tallerdom.data

import com.example.tallerdom.logic.GeneradorPartida
import com.example.tallerdom.logic.ResultadoJugada
import com.example.tallerdom.model.EstadoPartida
import com.example.tallerdom.model.Jugador
import com.example.tallerdom.model.Partida
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// Habla con Firestore. Las reglas del juego NO estan aqui, estan en MotorJuego
class SalaRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        const val MAX_JUGADORES = 2
        private const val LETRAS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }

    private val salas = db.collection("salas")

    private fun generarCodigo(): String {
        return (1..4).map { LETRAS.random() }.joinToString("")
    }

    // crea la sala con el creador como primer jugador y devuelve el codigo
    suspend fun crearSala(uid: String, nombre: String): Result<String> {
        return try {
            var codigo = generarCodigo()
            var intentos = 0
            // si el codigo ya existe se genera otro
            while (salas.document(codigo).get().await().exists() && intentos < 5) {
                codigo = generarCodigo()
                intentos++
            }

            val partida = Partida(
                codigo = codigo,
                jugadores = listOf(Jugador(uid = uid, nombre = nombre))
            )
            salas.document(codigo).set(partida).await()
            Result.success(codigo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // mete al jugador en la sala si existe, esta esperando y hay cupo
    suspend fun unirse(codigo: String, uid: String, nombre: String): Result<Unit> {
        return try {
            val ref = salas.document(codigo.trim().uppercase())

            val error = db.runTransaction { tr ->
                val partida = tr.get(ref).toObject(Partida::class.java)
                if (partida == null) return@runTransaction "La sala no existe"

                // si ya estaba dentro no hace falta hacer nada
                if (partida.jugadorPorUid(uid) == null) {
                    if (partida.estado != EstadoPartida.ESPERANDO) {
                        return@runTransaction "La partida ya empezo"
                    }
                    if (partida.jugadores.size >= MAX_JUGADORES) {
                        return@runTransaction "La sala esta llena"
                    }
                    val nuevo = Jugador(uid = uid, nombre = nombre)
                    tr.set(ref, partida.copy(jugadores = partida.jugadores + nuevo))
                }
                null
            }.await()

            if (error != null) Result.failure(Exception(error)) else Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // cada vez que alguien cambia la sala, este flow emite la partida nueva (tiempo real)
    fun escucharSala(codigo: String): Flow<Partida?> = callbackFlow {
        val registro = salas.document(codigo).addSnapshotListener { snap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snap?.toObject(Partida::class.java))
        }
        awaitClose { registro.remove() }
    }

    // aplica una accion del MotorJuego sobre la partida dentro de una transaccion
    // si el motor devuelve error no se guarda nada
    suspend fun jugar(codigo: String, accion: (Partida) -> ResultadoJugada): Result<Unit> {
        return try {
            val ref = salas.document(codigo)

            val error = db.runTransaction { tr ->
                val partida = tr.get(ref).toObject(Partida::class.java)
                if (partida == null) return@runTransaction "La sala no existe"

                when (val resultado = accion(partida)) {
                    is ResultadoJugada.Error -> resultado.mensaje
                    is ResultadoJugada.Ok -> {
                        tr.set(ref, resultado.partida)
                        null
                    }
                }
            }.await()

            if (error != null) Result.failure(Exception(error)) else Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // reparte fichas y arranca la partida (necesita 2 jugadores)
    suspend fun iniciarPartida(codigo: String): Result<Unit> = jugar(codigo) { partida ->
        if (partida.jugadores.size < MAX_JUGADORES) {
            ResultadoJugada.Error("Falta un jugador")
        } else if (partida.estado != EstadoPartida.ESPERANDO) {
            ResultadoJugada.Error("La partida ya empezo")
        } else {
            ResultadoJugada.Ok(GeneradorPartida.iniciarPartida(partida))
        }
    }

    // el anfitrion (primer jugador) cancela y borra la sala, el invitado solo sale
    suspend fun salirDeEspera(codigo: String, uid: String): Result<Unit> {
        return try {
            val ref = salas.document(codigo)

            db.runTransaction { tr ->
                val partida = tr.get(ref).toObject(Partida::class.java)
                if (partida != null && partida.estado == EstadoPartida.ESPERANDO) {
                    if (partida.jugadores.firstOrNull()?.uid == uid) {
                        tr.delete(ref)
                    } else {
                        val quedan = partida.jugadores.filter { it.uid != uid }
                        tr.set(ref, partida.copy(jugadores = quedan))
                    }
                }
                null
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}