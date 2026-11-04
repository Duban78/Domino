package com.example.tallerdom.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tallerdom.data.HistorialRepository
import com.example.tallerdom.model.RegistroHistorial
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistorialUiState(
    val cargando: Boolean = true,
    val partidas: List<RegistroHistorial> = emptyList(),
    val error: String? = null
)

class HistorialViewModel(
    private val repo: HistorialRepository = HistorialRepository()
) : ViewModel() {

    private val _estado = MutableStateFlow(HistorialUiState())
    val estado: StateFlow<HistorialUiState> = _estado.asStateFlow()

    fun cargar(uid: String) {
        viewModelScope.launch {
            _estado.value = HistorialUiState(cargando = true)
            repo.listar(uid)
                .onSuccess { _estado.value = HistorialUiState(cargando = false, partidas = it) }
                .onFailure {
                    _estado.value = HistorialUiState(
                        cargando = false,
                        error = it.message ?: "No se pudo cargar el historial"
                    )
                }
        }
    }
}