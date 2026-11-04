package com.example.tallerdom.model

data class Tablero(
    val filas: Int = 5,
    val columnas: Int = 5,
    val casillas: List<Casilla> = emptyList()
) {
    fun casillaEn(fila: Int, columna: Int): Casilla? =
        casillas.firstOrNull { it.fila == fila && it.columna == columna }


    fun vecinas(fila: Int, columna: Int): List<Casilla> {
        val movs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        return movs.mapNotNull { (df, dc) -> casillaEn(fila + df, columna + dc) }
    }

    fun sinFichas(): Boolean = casillas.none { it.ficha != null }


    fun reemplazar(nueva: Casilla): Tablero = copy(
        casillas = casillas.map {
            if (it.fila == nueva.fila && it.columna == nueva.columna) nueva else it
        }
    )
}