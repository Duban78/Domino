package com.example.tallerdom.logic

import com.example.tallerdom.model.Casilla
import com.example.tallerdom.model.Ficha
import com.example.tallerdom.model.Tablero
import com.example.tallerdom.model.TipoCasilla


object ReglasDomino {


    private fun casillaDisponible(casilla: Casilla): Boolean {
        if (casilla.libre()) return true
        return casilla.tipo == TipoCasilla.FORTALEZA && casilla.conquistas < 2
    }

    fun movimientoValido(tablero: Tablero, ficha: Ficha, fila: Int, columna: Int): Boolean {
        val casilla = tablero.casillaEn(fila, columna) ?: return false
        if (!casillaDisponible(casilla)) return false


        if (tablero.sinFichas()) {
            return fila == tablero.filas / 2 && columna == tablero.columnas / 2
        }


        val vecinasOcupadas = tablero.vecinas(fila, columna).filter { it.ficha != null }
        if (vecinasOcupadas.isEmpty()) return false

        return vecinasOcupadas.any { v -> v.ficha != null && ficha.encajaCon(v.ficha) }
    }


    fun casillasValidas(tablero: Tablero, ficha: Ficha): List<Casilla> {
        return tablero.casillas.filter { movimientoValido(tablero, ficha, it.fila, it.columna) }
    }


    fun tieneMovimiento(tablero: Tablero, mano: List<Ficha>): Boolean {
        return mano.any { casillasValidas(tablero, it).isNotEmpty() }
    }
}