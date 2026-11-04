package com.example.tallerdom.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tallerdom.model.Partida

@Composable
fun ResultadoScreen(
    partida: Partida?,
    miUid: String,
    mensaje: String?,
    onJugarDeNuevo: () -> Unit,
    onVolverAlMenu: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (partida == null) {
            CircularProgressIndicator()
        } else {
            // ganadorUid null = empate (mismos puntos y mismos territorios)
            val titulo = when (partida.ganadorUid) {
                null -> "Empate"
                miUid -> "Ganaste!"
                else -> "Perdiste"
            }

            Text(titulo, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(24.dp))

            for (jugador in partida.jugadores) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val esYo = if (jugador.uid == miUid) " (tu)" else ""
                        Text(
                            jugador.nombre + esYo,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("Puntos: ${jugador.puntaje}")
                        Text("Territorios conquistados: ${jugador.territorios}")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            mensaje?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = onJugarDeNuevo, modifier = Modifier.fillMaxWidth()) {
                Text("Jugar de nuevo")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onVolverAlMenu, modifier = Modifier.fillMaxWidth()) {
                Text("Volver al menu")
            }
        }
    }
}