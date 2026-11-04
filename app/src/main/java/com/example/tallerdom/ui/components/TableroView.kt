package com.example.tallerdom.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tallerdom.logic.GeneradorPartida
import com.example.tallerdom.model.Casilla
import com.example.tallerdom.model.Ficha
import com.example.tallerdom.model.Tablero
import com.example.tallerdom.ui.theme.TallerDomTheme


@Composable
fun TableroView(
    tablero: Tablero,
    casillasValidas: List<Casilla>,
    miUid: String,
    onCasillaClick: (Casilla) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (f in 0 until tablero.filas) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (c in 0 until tablero.columnas) {
                    val casilla = tablero.casillaEn(f, c)
                    if (casilla == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val esValida = casillasValidas.any {
                            it.fila == casilla.fila && it.columna == casilla.columna
                        }
                        val colorDueno: Color? = when (casilla.duenoUid) {
                            null -> null
                            miUid -> ColoresJuego.Yo
                            else -> ColoresJuego.Rival
                        }

                        CasillaView(
                            casilla = casilla,
                            colorDueno = colorDueno,
                            resaltada = esValida,
                            onClick = { if (esValida) onCasillaClick(casilla) },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun TableroPreview() {
    TallerDomTheme {
        val base = GeneradorPartida.crearTablero()
        val centro = base.casillaEn(2, 2)!!
        val tablero = base.reemplazar(
            centro.copy(ficha = Ficha(3, 5), duenoUid = "yo", conquistas = 1)
        )
        TableroView(
            tablero = tablero,
            casillasValidas = listOf(base.casillaEn(2, 3)!!),
            miUid = "yo",
            onCasillaClick = {}
        )
    }
}