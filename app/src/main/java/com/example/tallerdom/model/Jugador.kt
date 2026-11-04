package com.example.tallerdom.model

data class Jugador(
    val uid: String = "",
    val nombre: String = "",
    val mano: List<Ficha> = emptyList(),
    val puntaje: Int = 0,
    val territorios: Int = 0
)