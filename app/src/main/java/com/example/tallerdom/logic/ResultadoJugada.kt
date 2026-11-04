package com.example.tallerdom.logic

import com.example.tallerdom.model.Partida


sealed class ResultadoJugada {
    data class Ok(val partida: Partida) : ResultadoJugada()
    data class Error(val mensaje: String) : ResultadoJugada()
}