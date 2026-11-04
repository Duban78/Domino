package com.example.tallerdom.logic

import com.example.tallerdom.model.EstadoPartida
import com.example.tallerdom.model.Ficha
import com.example.tallerdom.model.Partida
import com.example.tallerdom.model.TipoCasilla

// Todo el flujo del juego. No usa Android ni Firebase para poder probarlo con tests
object MotorJuego {

    fun colocarFicha(
        partida: Partida,
        uid: String,
        ficha: Ficha,
        fila: Int,
        columna: Int
    ): ResultadoJugada {
        if (partida.estado != EstadoPartida.EN_JUEGO) {
            return ResultadoJugada.Error("La partida no esta en juego")
        }
        if (partida.turnoUid != uid) {
            return ResultadoJugada.Error("No es tu turno")
        }
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

        return ResultadoJugada.Ok(avanzarTurno(actualizada))
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
}