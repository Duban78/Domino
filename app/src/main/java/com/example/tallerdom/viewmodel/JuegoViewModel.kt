package com.example.tallerdom.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tallerdom.data.HistorialRepository
import com.example.tallerdom.data.SalaRepository
import com.example.tallerdom.logic.MotorJuego
import com.example.tallerdom.logic.ReglasDomino
import com.example.tallerdom.logic.ResultadoJugada
import com.example.tallerdom.model.Casilla
import com.example.tallerdom.model.EstadoPartida
import com.example.tallerdom.model.Ficha
import com.example.tallerdom.model.Jugador
import com.example.tallerdom.model.Partida
import com.example.tallerdom.model.RegistroHistorial
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class JuegoUiState(
    val miUid: String = "",
    val partida: Partida? = null,
    val fichaSeleccionada: Ficha? = null,
    val casillasValidas: List<Casilla> = emptyList(),
    val mensaje: String? = null,
    val enviando: Boolean = false
) {
    val miJugador: Jugador?
        get() = partida?.jugadorPorUid(miUid)

    val rival: Jugador?
        get() = partida?.jugadores?.firstOrNull { it.uid != miUid }

    val esMiTurno: Boolean
        get() = partida != null && partida.estado == EstadoPartida.EN_JUEGO && partida.turnoUid == miUid


    val enPortal: Boolean
        get() = esMiTurno && (partida?.portalFila ?: -1) >= 0


    val sinJugada: Boolean
        get() {
            val p = partida ?: return false
            val yo = miJugador ?: return false
            return esMiTurno && !enPortal && !ReglasDomino.tieneMovimiento(p.tablero, yo.mano)
        }

    val puedeRobar: Boolean
        get() = sinJugada && partida?.pozo?.isNotEmpty() == true

    val debePasar: Boolean
        get() = sinJugada && partida?.pozo?.isEmpty() == true
}

class JuegoViewModel(
    private val repo: SalaRepository = SalaRepository(),
    private val historialRepo: HistorialRepository = HistorialRepository()
) : ViewModel() {

    private val _estado = MutableStateFlow(JuegoUiState())
    val estado: StateFlow<JuegoUiState> = _estado.asStateFlow()

    private var codigo = ""
    private var escucha: Job? = null
    private var historialGuardado = false


    fun iniciar(codigo: String, miUid: String) {
        if (this.codigo == codigo && escucha != null) return
        this.codigo = codigo
        historialGuardado = false

        _estado.value = JuegoUiState(miUid = miUid)
        escucha?.cancel()
        escucha = viewModelScope.launch {
            repo.escucharSala(codigo)
                .catch { e ->
                    _estado.update { it.copy(mensaje = e.message ?: "Error de conexion") }
                }
                .collect { partida -> actualizarPartida(partida) }
        }
    }


    fun detener() {
        escucha?.cancel()
        escucha = null
        codigo = ""
        historialGuardado = false
        _estado.value = JuegoUiState()
    }


    private fun actualizarPartida(partida: Partida?) {
        _estado.update { actual ->
            // si me toca mover el portal, se resaltan sus destinos
            if (partida != null && partida.turnoUid == actual.miUid && partida.portalFila >= 0) {
                return@update actual.copy(
                    partida = partida,
                    fichaSeleccionada = null,
                    casillasValidas = MotorJuego.destinosPortal(partida)
                )
            }

            val seleccionada = actual.fichaSeleccionada
            val mano = partida?.jugadorPorUid(actual.miUid)?.mano ?: emptyList()


            val validas = if (
                seleccionada != null && partida != null &&
                mano.contains(seleccionada) && partida.turnoUid == actual.miUid
            ) {
                ReglasDomino.casillasValidas(partida.tablero, seleccionada)
            } else {
                null
            }

            actual.copy(
                partida = partida,
                fichaSeleccionada = if (validas != null) seleccionada else null,
                casillasValidas = validas ?: emptyList()
            )
        }


        if (partida?.estado == EstadoPartida.EN_JUEGO) historialGuardado = false
        if (partida != null && partida.estado == EstadoPartida.TERMINADA && !historialGuardado) {
            historialGuardado = true
            guardarHistorial(partida, _estado.value.miUid)
        }
    }


    private fun guardarHistorial(partida: Partida, miUid: String) {
        val yo = partida.jugadorPorUid(miUid) ?: return
        val rival = partida.jugadores.firstOrNull { it.uid != miUid } ?: return
        val resultado = when (partida.ganadorUid) {
            null -> "Empate"
            miUid -> "Victoria"
            else -> "Derrota"
        }
        viewModelScope.launch {
            historialRepo.guardar(
                RegistroHistorial(
                    uid = miUid,
                    rival = rival.nombre,
                    miPuntaje = yo.puntaje,
                    puntajeRival = rival.puntaje,
                    resultado = resultado,
                    fecha = System.currentTimeMillis()
                )
            )
        }
    }

    fun seleccionarFicha(ficha: Ficha) {
        val actual = _estado.value
        val partida = actual.partida ?: return

        if (!actual.esMiTurno) {
            _estado.update { it.copy(mensaje = "No es tu turno") }
            return
        }

        if (actual.enPortal) {
            _estado.update { it.copy(mensaje = "Primero mueve la ficha del portal") }
            return
        }

        if (actual.fichaSeleccionada == ficha) {
            cancelarSeleccion()
            return
        }

        val validas = ReglasDomino.casillasValidas(partida.tablero, ficha)
        if (validas.isEmpty()) {
            _estado.update {
                it.copy(
                    fichaSeleccionada = null,
                    casillasValidas = emptyList(),
                    mensaje = "Esa ficha no se puede poner en ninguna casilla"
                )
            }
            return
        }
        _estado.update {
            it.copy(fichaSeleccionada = ficha, casillasValidas = validas, mensaje = null)
        }
    }

    fun cancelarSeleccion() {

        if (_estado.value.enPortal) return
        _estado.update { it.copy(fichaSeleccionada = null, casillasValidas = emptyList()) }
    }


    fun colocarEn(fila: Int, columna: Int) {
        val actual = _estado.value
        val uid = actual.miUid

        if (actual.enPortal) {
            ejecutar { partida -> MotorJuego.usarPortal(partida, uid, fila, columna) }
            return
        }

        val ficha = actual.fichaSeleccionada ?: return
        ejecutar { partida -> MotorJuego.colocarFicha(partida, uid, ficha, fila, columna) }
    }

    fun robar() {
        val uid = _estado.value.miUid
        ejecutar { partida -> MotorJuego.robarFicha(partida, uid) }
    }

    fun pasar() {
        val uid = _estado.value.miUid
        ejecutar { partida -> MotorJuego.pasarTurno(partida, uid) }
    }

    fun abandonar() {
        val uid = _estado.value.miUid
        ejecutar { partida -> MotorJuego.abandonar(partida, uid) }
    }

    fun reiniciar() {
        ejecutar { partida -> MotorJuego.reiniciar(partida) }
    }

    fun limpiarMensaje() {
        _estado.update { it.copy(mensaje = null) }
    }


    private fun ejecutar(accion: (Partida) -> ResultadoJugada) {
        if (_estado.value.enviando) return

        viewModelScope.launch {
            _estado.update { it.copy(enviando = true) }
            repo.jugar(codigo, accion)
                .onSuccess {
                    _estado.update { s ->
                        s.copy(enviando = false, mensaje = null)
                    }
                }
                .onFailure { e ->
                    _estado.update { s ->
                        s.copy(enviando = false, mensaje = e.message ?: "No se pudo hacer la jugada")
                    }
                }
        }
    }
}