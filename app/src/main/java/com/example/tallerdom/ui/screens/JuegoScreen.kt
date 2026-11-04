package com.example.tallerdom.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tallerdom.model.Casilla
import com.example.tallerdom.model.Ficha
import com.example.tallerdom.ui.components.ColoresJuego
import com.example.tallerdom.ui.components.JugadorInfo
import com.example.tallerdom.ui.components.ManoFichas
import com.example.tallerdom.ui.components.TableroView
import com.example.tallerdom.viewmodel.JuegoUiState

// Pantalla sin logica: dibuja el estado y avisa con callbacks
@Composable
fun JuegoScreen(
    estado: JuegoUiState,
    onFichaClick: (Ficha) -> Unit,
    onCasillaClick: (Casilla) -> Unit,
    onRobar: () -> Unit,
    onPasar: () -> Unit,
    onCancelarSeleccion: () -> Unit,
    onAbandonar: () -> Unit
) {
    val partida = estado.partida
    val yo = estado.miJugador
    val rival = estado.rival

    var confirmandoAbandono by remember { mutableStateOf(false) }

    // el boton atras de Android tambien pide confirmar antes de salir
    BackHandler { confirmandoAbandono = true }

    if (partida == null || yo == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            // marcadores de los dos jugadores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                JugadorInfo(
                    nombre = yo.nombre,
                    puntaje = yo.puntaje,
                    territorios = yo.territorios,
                    enTurno = partida.turnoUid == yo.uid,
                    color = ColoresJuego.Yo,
                    modifier = Modifier.weight(1f)
                )
                if (rival != null) {
                    JugadorInfo(
                        nombre = rival.nombre,
                        puntaje = rival.puntaje,
                        territorios = rival.territorios,
                        enTurno = partida.turnoUid == rival.uid,
                        color = ColoresJuego.Rival,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Ronda ${partida.ronda} de ${partida.maxRondas}   |   Pozo: ${partida.pozo.size} fichas",
                style = MaterialTheme.typography.bodyMedium
            )

            val ayuda = when {
                !estado.esMiTurno -> "Esperando a ${rival?.nombre ?: "el rival"}..."
                estado.puedeRobar -> "No tienes jugadas: roba una ficha del pozo"
                estado.debePasar -> "No tienes jugadas y el pozo esta vacio: pasa el turno"
                estado.fichaSeleccionada != null -> "Toca una casilla resaltada para colocar la ficha"
                else -> "Es tu turno: selecciona una ficha de tu mano"
            }
            Text(
                text = ayuda,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // tablero
            TableroView(
                tablero = partida.tablero,
                casillasValidas = estado.casillasValidas,
                miUid = estado.miUid,
                onCasillaClick = onCasillaClick
            )

            estado.mensaje?.let { mensaje ->
                Spacer(Modifier.height(8.dp))
                Text(mensaje, color = MaterialTheme.colorScheme.error)
            }

            // acciones del turno
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRobar,
                    enabled = estado.puedeRobar && !estado.enviando,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Robar ficha")
                }
                Button(
                    onClick = onPasar,
                    enabled = estado.debePasar && !estado.enviando,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Pasar turno")
                }
            }
            if (estado.fichaSeleccionada != null) {
                OutlinedButton(
                    onClick = onCancelarSeleccion,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar seleccion")
                }
            }

            // mano del jugador
            Spacer(Modifier.height(12.dp))
            Text("Tus fichas (${yo.mano.size})", style = MaterialTheme.typography.titleMedium)
            ManoFichas(
                mano = yo.mano,
                seleccionada = estado.fichaSeleccionada,
                onFichaClick = onFichaClick
            )

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { confirmandoAbandono = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Abandonar partida")
            }
        }
    }

    if (confirmandoAbandono) {
        AlertDialog(
            onDismissRequest = { confirmandoAbandono = false },
            title = { Text("Abandonar partida") },
            text = { Text("Si abandonas, tu rival gana la partida. Seguro?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmandoAbandono = false
                    onAbandonar()
                }) {
                    Text("Abandonar")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoAbandono = false }) {
                    Text("Seguir jugando")
                }
            }
        )
    }
}