package com.example.tallerdom.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

// Habla con Firebase Authentication. El ViewModel nunca toca FirebaseAuth directo
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    fun usuarioActual(): FirebaseUser? = auth.currentUser

    suspend fun registrar(nombre: String, correo: String, clave: String): Result<FirebaseUser> {
        return try {
            val res = auth.createUserWithEmailAndPassword(correo, clave).await()
            val usuario = res.user
                ?: return Result.failure(Exception("No se pudo crear el usuario"))

            // guardamos el nombre en el perfil para mostrarlo despues
            val cambios = UserProfileChangeRequest.Builder()
                .setDisplayName(nombre)
                .build()
            usuario.updateProfile(cambios).await()

            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun iniciarSesion(correo: String, clave: String): Result<FirebaseUser> {
        return try {
            val res = auth.signInWithEmailAndPassword(correo, clave).await()
            val usuario = res.user
                ?: return Result.failure(Exception("No se pudo iniciar sesion"))
            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun cerrarSesion() {
        auth.signOut()
    }
}