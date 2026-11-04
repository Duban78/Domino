package com.example.tallerdom.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tallerdom.data.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// lo que la pantalla necesita saber del login
data class AuthUiState(
    val cargando: Boolean = false,
    val error: String? = null,
    val usuarioUid: String? = null,   // null = no hay sesion
    val nombre: String? = null
)

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _estado = MutableStateFlow(
        AuthUiState(
            usuarioUid = repo.usuarioActual()?.uid,
            nombre = repo.usuarioActual()?.displayName
        )
    )
    val estado: StateFlow<AuthUiState> = _estado.asStateFlow()

    fun iniciarSesion(correo: String, clave: String) {
        if (correo.isBlank() || clave.isBlank()) {
            _estado.update { it.copy(error = "Llena todos los campos") }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            repo.iniciarSesion(correo.trim(), clave)
                .onSuccess { usuario ->
                    _estado.update {
                        it.copy(cargando = false, usuarioUid = usuario.uid, nombre = usuario.displayName)
                    }
                }
                .onFailure { e ->
                    _estado.update { it.copy(cargando = false, error = traducirError(e)) }
                }
        }
    }

    fun registrar(nombre: String, correo: String, clave: String, confirmar: String) {
        if (nombre.isBlank() || correo.isBlank() || clave.isBlank()) {
            _estado.update { it.copy(error = "Llena todos los campos") }
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches()) {
            _estado.update { it.copy(error = "El correo no es valido") }
            return
        }
        if (clave.length < 6) {
            _estado.update { it.copy(error = "La contrasena debe tener minimo 6 caracteres") }
            return
        }
        if (clave != confirmar) {
            _estado.update { it.copy(error = "Las contrasenas no coinciden") }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            repo.registrar(nombre.trim(), correo.trim(), clave)
                .onSuccess { usuario ->
                    _estado.update {
                        it.copy(cargando = false, usuarioUid = usuario.uid, nombre = nombre.trim())
                    }
                }
                .onFailure { e ->
                    _estado.update { it.copy(cargando = false, error = traducirError(e)) }
                }
        }
    }

    fun cerrarSesion() {
        repo.cerrarSesion()
        _estado.value = AuthUiState()
    }

    fun limpiarError() {
        _estado.update { it.copy(error = null) }
    }

    // convierte los errores de firebase a mensajes entendibles
    // ojo con el orden: WeakPassword hereda de InvalidCredentials
    private fun traducirError(e: Throwable): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "La contrasena es muy debil"
        is FirebaseAuthUserCollisionException -> "Ese correo ya esta registrado"
        is FirebaseAuthInvalidCredentialsException -> "Correo o contrasena incorrectos"
        is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo"
        is FirebaseNetworkException -> "Sin conexion a internet"
        else -> e.message ?: "Error desconocido"
    }
}