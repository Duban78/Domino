package com.example.tallerdom.logic

import com.example.tallerdom.model.Partida

// lo que devuelve el motor despues de intentar una jugada
sealed class ResultadoJugada {
    data class Ok(val partida: Partida) : ResultadoJugada()
    data class Error(val mensaje: String) : ResultadoJugada()
}