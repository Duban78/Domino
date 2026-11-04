package com.example.tallerdom.ui.components

import androidx.compose.ui.graphics.Color
import com.example.tallerdom.model.TipoCasilla

// colores e iconos que se repiten en varios componentes
object ColoresJuego {
    val Yo = Color(0xFF2E7D32)
    val Rival = Color(0xFF1565C0)
    val Valida = Color(0xFFFFD600)

    fun deCasilla(tipo: TipoCasilla): Color = when (tipo) {
        TipoCasilla.NORMAL -> Color(0xFFC8B98E)
        TipoCasilla.BONUS -> Color(0xFFE0B13A)
        TipoCasilla.TESORO -> Color(0xFFFFB74D)
        TipoCasilla.TRAMPA -> Color(0xFFC0392B)
        TipoCasilla.FORTALEZA -> Color(0xFF78909C)
        TipoCasilla.PORTAL -> Color(0xFF29B6F6)
    }

    fun icono(tipo: TipoCasilla): String = when (tipo) {
        TipoCasilla.NORMAL -> ""
        TipoCasilla.BONUS -> "⭐"
        TipoCasilla.TESORO -> "💰"
        TipoCasilla.TRAMPA -> "💣"
        TipoCasilla.FORTALEZA -> "🛡"
        TipoCasilla.PORTAL -> "🌀"
    }
}