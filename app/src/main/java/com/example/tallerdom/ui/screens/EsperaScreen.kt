package com.example.tallerdom.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tallerdom.model.Partida
import com.example.tallerdom.ui.components.JugadorItem

@Composable
fun EsperaScreen(
    codigo: String,
    partida: Partida?,
    miUid: String,
    error: String?,
    onIniciar: () -> Unit,
    onSalir: () -> Unit,
    maxJugadores: Int = 2
) {
    val jugadores = partida?.jugadores ?: emptyList()
    val soyAnfitrion = jugadores.firstOrNull()?.uid == miUid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sala de espera", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text("Codigo de sala")
        Text(codigo, style = MaterialTheme.typography.displayMedium)
        Text("Comparte este codigo con tu amigo", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))

        for (i in 0 until maxJugadores) {
            val jugador = jugadores.getOrNull(i)
            JugadorItem(
                nombre = jugador?.nombre ?: "Esperando jugador...",
                subtitulo = when {
                    jugador == null -> ""
                    i == 0 -> "Anfitrion"
                    else -> "Listo"
                }
            )
            Spacer(Modifier.height(8.dp))
        }

        error?.let { mensaje ->
            Spacer(Modifier.height(8.dp))
            Text(mensaje, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))
        if (soyAnfitrion) {
            Button(
                onClick = onIniciar,
                enabled = jugadores.size >= maxJugadores,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Iniciar partida")
            }
        } else {
            Text("Esperando a que el anfitrion inicie la partida")
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onSalir, modifier = Modifier.fillMaxWidth()) {
            Text(if (soyAnfitrion) "Cancelar partida" else "Salir de la sala")
        }
    }
}