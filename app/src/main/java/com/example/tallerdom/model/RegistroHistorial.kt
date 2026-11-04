package com.example.tallerdom.model

// una partida jugada, vista desde un jugador
data class RegistroHistorial(
    val uid: String = "",
    val rival: String = "",
    val miPuntaje: Int = 0,
    val puntajeRival: Int = 0,
    val resultado: String = "",   // Victoria, Derrota o Empate
    val fecha: Long = 0L
)