package com.example.tallerdom.logic

import com.example.tallerdom.model.EstadoPartida
import com.example.tallerdom.model.Ficha
import com.example.tallerdom.model.Partida
import com.example.tallerdom.model.TipoCasilla

// Todo el flujo del juego. No usa Android ni Firebase para poder probarlo con tests
object MotorJuego {

    // revisa lo basico de cualquier accion: partida activa y turno del jugador
    private fun errorDeTurno(partida: Partida, uid: String): String? {
        if (partida.estado != EstadoPartida.EN_JUEGO) return "La partida no esta en juego"
        if (partida.turnoUid != uid) return "No es tu turno"
        if (partida.jugadorPorUid(uid) == null) return "Jugador no encontrado"
        return null
    }

    fun colocarFicha(
        partida: Partida,
        uid: String,
        ficha: Ficha,
        fila: Int,
        columna: Int
    ): ResultadoJugada {
        val errorTurno = errorDeTurno(partida, uid)
        if (errorTurno != null) return ResultadoJugada.Error(errorTurno)

        val jugador = partida.jugadorPorUid(uid)
            ?: return ResultadoJugada.Error("Jugador no encontrado")
        if (!jugador.mano.contains(ficha)) {
            return ResultadoJugada.Error("Esa ficha no esta en tu mano")
        }
        if (!ReglasDomino.movimientoValido(partida.tablero, ficha, fila, columna)) {
            return ResultadoJugada.Error("Movimiento no valido")
        }
        val casilla = partida.tablero.casillaEn(fila, columna)
            ?: return ResultadoJugada.Error("Casilla no existe")

        // la fortaleza necesita dos conquistas, las demas una sola
        val conquistas = casilla.conquistas + 1
        val completa = casilla.tipo != TipoCasilla.FORTALEZA || conquistas >= 2

        val casillaNueva = casilla.copy(
            ficha = ficha,
            conquistas = conquistas,
            duenoUid = if (completa) uid else casilla.duenoUid
        )

        val puntos = Puntuacion.puntosPorConquista(casilla.tipo, completa)

        val jugadoresNuevos = partida.jugadores.map { j ->
            if (j.uid == uid) {
                val manoNueva = j.mano.toMutableList()
                manoNueva.remove(ficha)
                j.copy(
                    mano = manoNueva,
                    puntaje = j.puntaje + puntos,
                    territorios = j.territorios + if (completa) 1 else 0
                )
            } else j
        }

        val actualizada = partida.copy(
            tablero = partida.tablero.reemplazar(casillaNueva),
            jugadores = jugadoresNuevos,
            pasesSeguidos = 0
        )

        // TODO: portal (lo hace mi companero), aqui hay que revisar si casilla.tipo == PORTAL

        return ResultadoJugada.Ok(revisarFin(avanzarTurno(actualizada)))
    }

    // si no tiene jugada, el jugador toma la primera ficha del pozo
    fun robarFicha(partida: Partida, uid: String): ResultadoJugada {
        val errorTurno = errorDeTurno(partida, uid)
        if (errorTurno != null) return ResultadoJugada.Error(errorTurno)

        val jugador = partida.jugadorPorUid(uid)
            ?: return ResultadoJugada.Error("Jugador no encontrado")
        if (ReglasDomino.tieneMovimiento(partida.tablero, jugador.mano)) {
            return ResultadoJugada.Error("Tienes una ficha que se puede jugar")
        }
        if (partida.pozo.isEmpty()) {
            return ResultadoJugada.Error("El pozo esta vacio")
        }

        val fichaRobada = partida.pozo.first()
        val actualizada = partida.copy(
            pozo = partida.pozo.drop(1),
            jugadores = partida.jugadores.map { j ->
                if (j.uid == uid) j.copy(mano = j.mano + fichaRobada) else j
            }
        )
        // no se cambia el turno: puede jugar la ficha que robo o seguir robando
        return ResultadoJugada.Ok(actualizada)
    }

    // pierde el turno: no tiene jugada y no quedan fichas en el pozo
    fun pasarTurno(partida: Partida, uid: String): ResultadoJugada {
        val errorTurno = errorDeTurno(partida, uid)
        if (errorTurno != null) return ResultadoJugada.Error(errorTurno)

        val jugador = partida.jugadorPorUid(uid)
            ?: return ResultadoJugada.Error("Jugador no encontrado")
        if (ReglasDomino.tieneMovimiento(partida.tablero, jugador.mano)) {
            return ResultadoJugada.Error("Tienes una ficha que se puede jugar")
        }
        if (partida.pozo.isNotEmpty()) {
            return ResultadoJugada.Error("Debes robar del pozo")
        }

        val conPase = partida.copy(pasesSeguidos = partida.pasesSeguidos + 1)
        return ResultadoJugada.Ok(revisarFin(avanzarTurno(conPase)))
    }

    // pasa el turno al siguiente jugador, si vuelve al primero sube la ronda
    fun avanzarTurno(partida: Partida): Partida {
        val actual = partida.jugadores.indexOfFirst { it.uid == partida.turnoUid }
        val siguiente = (actual + 1) % partida.jugadores.size
        val nuevaRonda = if (siguiente == 0) partida.ronda + 1 else partida.ronda

        return partida.copy(
            turnoUid = partida.jugadores[siguiente].uid,
            ronda = nuevaRonda
        )
    }

    // detecta si la partida debe terminar y calcula el ganador
    fun revisarFin(partida: Partida): Partida {
        val alguienSinFichas = partida.jugadores.any { it.mano.isEmpty() }
        val nadiePuedeMover = partida.pasesSeguidos >= partida.jugadores.size
        val seAcabaronRondas = partida.ronda > partida.maxRondas

        if (!alguienSinFichas && !nadiePuedeMover && !seAcabaronRondas) return partida

        val ganador = Puntuacion.ganador(partida.jugadores)
        return partida.copy(
            estado = EstadoPartida.TERMINADA,
            ganadorUid = ganador?.uid   // null = empate
        )
    }

    // el jugador se sale en plena partida: termina y gana el rival
    fun abandonar(partida: Partida, uid: String): ResultadoJugada {
        if (partida.estado != EstadoPartida.EN_JUEGO) {
            return ResultadoJugada.Error("La partida no esta en juego")
        }
        if (partida.jugadorPorUid(uid) == null) {
            return ResultadoJugada.Error("Jugador no encontrado")
        }

        val rival = partida.jugadores.firstOrNull { it.uid != uid }
        return ResultadoJugada.Ok(
            partida.copy(estado = EstadoPartida.TERMINADA, ganadorUid = rival?.uid)
        )
    }

    // vuelve a repartir y empezar con los mismos jugadores (solo si ya termino)
    fun reiniciar(partida: Partida): ResultadoJugada {
        if (partida.estado != EstadoPartida.TERMINADA) {
            return ResultadoJugada.Error("La partida todavia no termino")
        }
        return ResultadoJugada.Ok(GeneradorPartida.iniciarPartida(partida))
    }
}