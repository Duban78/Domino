package com.example.tallerdom.logic

import com.example.tallerdom.model.Jugador
import com.example.tallerdom.model.TipoCasilla

object Puntuacion {

    const val PUNTOS_TERRITORIO = 1
    const val PUNTOS_BONUS = 3
    const val PUNTOS_TESORO = 5
    const val PUNTOS_TRAMPA = -2

    // puntos que gana el jugador al poner ficha en una casilla
    // conquistaCompleta es false cuando es la primera ficha de una fortaleza
    fun puntosPorConquista(tipo: TipoCasilla, conquistaCompleta: Boolean): Int {
        if (!conquistaCompleta) return 0

        val efecto = when (tipo) {
            TipoCasilla.BONUS -> PUNTOS_BONUS
            TipoCasilla.TESORO -> PUNTOS_TESORO
            TipoCasilla.TRAMPA -> PUNTOS_TRAMPA
            else -> 0
        }
        return PUNTOS_TERRITORIO + efecto
    }

    // gana el de mas puntos, si empatan gana el de mas territorios
    // si siguen empatados devuelve null (empate)
    fun ganador(jugadores: List<Jugador>): Jugador? {
        val ordenados = jugadores.sortedWith(
            compareByDescending<Jugador> { it.puntaje }
                .thenByDescending { it.territorios }
        )
        if (ordenados.size > 1) {
            val a = ordenados[0]
            val b = ordenados[1]
            if (a.puntaje == b.puntaje && a.territorios == b.territorios) return null
        }
        return ordenados.firstOrNull()
    }
}