package com.example.tallerdom.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// marcador de un jugador: nombre, puntos, territorios y si tiene el turno
@Composable
fun JugadorInfo(
    nombre: String,
    puntaje: Int,
    territorios: Int,
    enTurno: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        border = if (enTurno) BorderStroke(3.dp, color) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(nombre, style = MaterialTheme.typography.titleMedium, color = color)
            Text("Puntos: $puntaje")
            Text("Territorios: $territorios")
            if (enTurno) {
                Text("En turno", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}