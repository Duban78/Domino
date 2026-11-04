package com.example.tallerdom.model

enum class TipoCasilla {
    NORMAL, BONUS, TESORO, TRAMPA, FORTALEZA, PORTAL
}

// cada casilla es un territorio del tablero
data class Casilla(
    val fila: Int = 0,
    val columna: Int = 0,
    val tipo: TipoCasilla = TipoCasilla.NORMAL,
    val ficha: Ficha? = null,      // ultima ficha puesta aca
    val duenoUid: String? = null,  // quien la controla (null = nadie)
    val conquistas: Int = 0        // la fortaleza necesita 2
) {
    fun libre(): Boolean = ficha == null
}