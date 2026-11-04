package com.example.tallerdom.model

// una ficha de domino, ej: 3|5
data class Ficha(
    val izq: Int = 0,
    val der: Int = 0
) {
    fun contiene(n: Int): Boolean = izq == n || der == n

    // dos fichas "encajan" si comparten algun numero
    fun encajaCon(otra: Ficha): Boolean = contiene(otra.izq) || contiene(otra.der)

    fun suma(): Int = izq + der
}