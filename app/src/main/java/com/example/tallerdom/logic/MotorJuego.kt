package com.example.tallerdom.logic

import com.example.tallerdom.model.Casilla
import com.example.tallerdom.model.EstadoPartida
import com.example.tallerdom.model.Ficha
import com.example.tallerdom.model.Partida
import com.example.tallerdom.model.TipoCasilla


object MotorJuego {


    private fun errorDeTurno(partida: Partida, uid: String): String? {
        if (partida.estado != EstadoPartida.EN_JUEGO) return "La partida no esta en juego"
        if (partida.turnoUid != uid) return "No es tu turno"
        if (partida.jugadorPorUid(uid) == null) return "Jugador no encontrado"
        return null
    }


    private fun conquistar(
        partida: Partida,
        uid: String,
        ficha: Ficha,
        casilla: Casilla,
        sacarDeMano: Boolean
    ): Partida {

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
                if (sacarDeMano) manoNueva.remove(ficha)
                j.copy(
                    mano = manoNueva,
                    puntaje = j.puntaje + puntos,
                    territorios = j.territorios + if (completa) 1 else 0
                )
            } else j
        }

        return partida.copy(
            tablero = partida.tablero.reemplazar(casillaNueva),
            jugadores = jugadoresNuevos,
            pasesSeguidos = 0
        )
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
        if (partida.portalFila >= 0) {
            return ResultadoJugada.Error("Primero mueve la ficha del portal")
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

        val actualizada = conquistar(partida, uid, ficha, casilla, true)

        if (casilla.tipo == TipoCasilla.PORTAL) {
            val conPortal = actualizada.copy(portalFila = fila, portalColumna = columna)
            if (destinosPortal(conPortal).isNotEmpty()) {
                return ResultadoJugada.Ok(conPortal)
            }
        }

        return ResultadoJugada.Ok(revisarFin(avanzarTurno(actualizada)))
    }


    fun destinosPortal(partida: Partida): List<Casilla> {
        if (partida.portalFila < 0) return emptyList()
        val origen = partida.tablero.casillaEn(partida.portalFila, partida.portalColumna)
            ?: return emptyList()
        val ficha = origen.ficha ?: return emptyList()

        // se prueba como si el portal estuviera vacio
        val sinPortal = partida.tablero.reemplazar(
            origen.copy(ficha = null, duenoUid = null, conquistas = 0)
        )
        return ReglasDomino.casillasValidas(sinPortal, ficha)
            .filter { it.fila != origen.fila || it.columna != origen.columna }
    }


    fun usarPortal(
        partida: Partida,
        uid: String,
        filaDestino: Int,
        colDestino: Int
    ): ResultadoJugada {
        val errorTurno = errorDeTurno(partida, uid)
        if (errorTurno != null) return ResultadoJugada.Error(errorTurno)
        if (partida.portalFila < 0) return ResultadoJugada.Error("No hay portal activo")

        val valido = destinosPortal(partida).any {
            it.fila == filaDestino && it.columna == colDestino
        }
        if (!valido) return ResultadoJugada.Error("Movimiento no valido")

        val origen = partida.tablero.casillaEn(partida.portalFila, partida.portalColumna)
            ?: return ResultadoJugada.Error("El portal no existe")
        val ficha = origen.ficha
            ?: return ResultadoJugada.Error("No hay ficha en el portal")
        val destino = partida.tablero.casillaEn(filaDestino, colDestino)
            ?: return ResultadoJugada.Error("Casilla no existe")


        val liberada = partida.copy(
            tablero = partida.tablero.reemplazar(
                origen.copy(ficha = null, duenoUid = null, conquistas = 0)
            ),
            jugadores = partida.jugadores.map { j ->
                if (j.uid == uid) j.copy(
                    puntaje = j.puntaje - Puntuacion.PUNTOS_TERRITORIO,
                    territorios = j.territorios - 1
                ) else j
            },
            portalFila = -1,
            portalColumna = -1
        )


        val conquistada = conquistar(liberada, uid, ficha, destino, false)
        return ResultadoJugada.Ok(revisarFin(avanzarTurno(conquistada)))
    }


    fun robarFicha(partida: Partida, uid: String): ResultadoJugada {
        val errorTurno = errorDeTurno(partida, uid)
        if (errorTurno != null) return ResultadoJugada.Error(errorTurno)
        if (partida.portalFila >= 0) {
            return ResultadoJugada.Error("Primero mueve la ficha del portal")
        }

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

        return ResultadoJugada.Ok(actualizada)
    }

    fun pasarTurno(partida: Partida, uid: String): ResultadoJugada {
        val errorTurno = errorDeTurno(partida, uid)
        if (errorTurno != null) return ResultadoJugada.Error(errorTurno)
        if (partida.portalFila >= 0) {
            return ResultadoJugada.Error("Primero mueve la ficha del portal")
        }

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


    fun avanzarTurno(partida: Partida): Partida {
        val actual = partida.jugadores.indexOfFirst { it.uid == partida.turnoUid }
        val siguiente = (actual + 1) % partida.jugadores.size
        val nuevaRonda = if (siguiente == 0) partida.ronda + 1 else partida.ronda

        return partida.copy(
            turnoUid = partida.jugadores[siguiente].uid,
            ronda = nuevaRonda
        )
    }


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



    fun reiniciar(partida: Partida): ResultadoJugada {
        if (partida.estado != EstadoPartida.TERMINADA) {
            return ResultadoJugada.Error("La partida todavia no termino")
        }
        return ResultadoJugada.Ok(GeneradorPartida.iniciarPartida(partida))
    }
}