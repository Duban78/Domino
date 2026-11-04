package com.example.tallerdom.logic

import com.example.tallerdom.model.Casilla
import com.example.tallerdom.model.EstadoPartida
import com.example.tallerdom.model.Ficha
import com.example.tallerdom.model.Partida
import com.example.tallerdom.model.Tablero
import com.example.tallerdom.model.TipoCasilla

object GeneradorPartida {

    const val FICHAS_POR_JUGADOR = 7

    // tablero de 5x5 con casillas especiales en posiciones al azar
    // el centro siempre es normal porque ahi va la primera ficha
    fun crearTablero(filas: Int = 5, columnas: Int = 5): Tablero {
        val centroF = filas / 2
        val centroC = columnas / 2

        val posiciones = mutableListOf<Pair<Int, Int>>()
        for (f in 0 until filas) {
            for (c in 0 until columnas) {
                if (f == centroF && c == centroC) continue
                posiciones.add(f to c)
            }
        }
        posiciones.shuffle()

        val especiales = listOf(
            TipoCasilla.BONUS to 3,
            TipoCasilla.TESORO to 2,
            TipoCasilla.TRAMPA to 2,
            TipoCasilla.FORTALEZA to 2,
            TipoCasilla.PORTAL to 2
        )

        val tipos = mutableMapOf<Pair<Int, Int>, TipoCasilla>()
        var i = 0
        for ((tipo, cantidad) in especiales) {
            repeat(cantidad) {
                tipos[posiciones[i]] = tipo
                i++
            }
        }

        val casillas = mutableListOf<Casilla>()
        for (f in 0 until filas) {
            for (c in 0 until columnas) {
                val tipo = tipos[f to c] ?: TipoCasilla.NORMAL
                casillas.add(Casilla(fila = f, columna = c, tipo = tipo))
            }
        }
        return Tablero(filas, columnas, casillas)
    }

    // las 28 fichas del doble seis, revueltas
    fun crearFichas(): MutableList<Ficha> {
        val fichas = mutableListOf<Ficha>()
        for (i in 0..6) {
            for (j in i..6) {
                fichas.add(Ficha(i, j))
            }
        }
        fichas.shuffle()
        return fichas
    }

    // reparte 7 fichas a cada jugador y lo que sobra queda en el pozo
    fun iniciarPartida(partida: Partida): Partida {
        val fichas = crearFichas()

        val jugadores = partida.jugadores.map { jugador ->
            val mano = fichas.take(FICHAS_POR_JUGADOR)
            repeat(FICHAS_POR_JUGADOR) { fichas.removeAt(0) }
            jugador.copy(mano = mano, puntaje = 0, territorios = 0)
        }

        return partida.copy(
            estado = EstadoPartida.EN_JUEGO,
            jugadores = jugadores,
            tablero = crearTablero(),
            pozo = fichas.toList(),
            turnoUid = jugadores.first().uid,
            ronda = 1,
            pasesSeguidos = 0,
            ganadorUid = null
        )
    }
}