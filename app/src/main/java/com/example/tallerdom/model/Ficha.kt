package com.example.tallerdom.model


data class Ficha(
    val izq: Int = 0,
    val der: Int = 0
) {
    fun contiene(n: Int): Boolean = izq == n || der == n


    fun encajaCon(otra: Ficha): Boolean = contiene(otra.izq) || contiene(otra.der)

    fun suma(): Int = izq + der
}