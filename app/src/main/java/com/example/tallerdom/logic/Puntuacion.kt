package com.example.tallerdom.logic

import com.example.tallerdom.model.Jugador
import com.example.tallerdom.model.TipoCasilla

object Puntuacion {

    const val PUNTOS_TERRITORIO = 1
    const val PUNTOS_BONUS = 3
    const val PUNTOS_TESORO = 5
    const val PUNTOS_TRAMPA = -2


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