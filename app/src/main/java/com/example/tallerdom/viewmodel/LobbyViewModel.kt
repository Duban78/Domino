package com.example.tallerdom.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tallerdom.data.SalaRepository
import com.example.tallerdom.model.Partida
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LobbyUiState(
    val cargando: Boolean = false,
    val error: String? = null,
    val partida: Partida? = null,      // la sala en la que estamos esperando
    val salaCerrada: Boolean = false   // true si el anfitrion cancelo la sala
)

class LobbyViewModel(
    private val repo: SalaRepository = SalaRepository()
) : ViewModel() {

    private val _estado = MutableStateFlow(LobbyUiState())
    val estado: StateFlow<LobbyUiState> = _estado.asStateFlow()

    private var escucha: Job? = null

    fun crearSala(uid: String, nombre: String, onCreada: (codigo: String) -> Unit) {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            repo.crearSala(uid, nombre)
                .onSuccess { codigo ->
                    _estado.update { it.copy(cargando = false) }
                    onCreada(codigo)
                }
                .onFailure { e ->
                    _estado.update {
                        it.copy(cargando = false, error = e.message ?: "No se pudo crear la sala")
                    }
                }
        }
    }

    fun unirse(codigo: String, uid: String, nombre: String, onUnido: (codigo: String) -> Unit) {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            repo.unirse(codigo, uid, nombre)
                .onSuccess {
                    _estado.update { it.copy(cargando = false) }
                    onUnido(codigo.trim().uppercase())
                }
                .onFailure { e ->
                    _estado.update {
                        it.copy(cargando = false, error = e.message ?: "No se pudo unir a la sala")
                    }
                }
        }
    }

    // empieza a escuchar la sala en tiempo real
    fun escuchar(codigo: String) {
        escucha?.cancel()
        _estado.update { it.copy(partida = null, salaCerrada = false, error = null) }

        escucha = viewModelScope.launch {
            repo.escucharSala(codigo)
                .catch { e -> _estado.update { it.copy(error = e.message ?: "Error de conexion") } }
                .collect { partida ->
                    _estado.update {
                        // si el documento ya no existe es porque cancelaron la sala
                        if (partida == null) it.copy(salaCerrada = true) else it.copy(partida = partida)
                    }
                }
        }
    }

    fun detenerEscucha() {
        escucha?.cancel()
        escucha = null
    }

    fun iniciarPartida(codigo: String) {
        viewModelScope.launch {
            repo.iniciarPartida(codigo)
                .onFailure { e -> _estado.update { it.copy(error = e.message) } }
        }
    }

    fun salirDeSala(codigo: String, uid: String, onListo: () -> Unit) {
        // primero dejamos de escuchar para no reaccionar a nuestro propio cambio
        detenerEscucha()
        viewModelScope.launch {
            repo.salirDeEspera(codigo, uid)
            onListo()
        }
    }

    fun limpiarError() {
        _estado.update { it.copy(error = null) }
    }
}