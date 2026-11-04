package com.example.tallerdom.data

import com.example.tallerdom.model.RegistroHistorial
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class HistorialRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val coleccion = db.collection("historial")

    suspend fun guardar(registro: RegistroHistorial): Result<Unit> {
        return try {
            coleccion.add(registro).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // se filtra por uid y se ordena aqui, asi Firestore no pide crear un indice
    suspend fun listar(uid: String): Result<List<RegistroHistorial>> {
        return try {
            val snap = coleccion.whereEqualTo("uid", uid).get().await()
            val lista = snap.toObjects(RegistroHistorial::class.java)
                .sortedByDescending { it.fecha }
            Result.success(lista)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}