package com.example.tallerdom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tallerdom.model.Casilla
import com.example.tallerdom.model.TipoCasilla

// un territorio del tablero
// colorDueno = null significa que nadie lo ha conquistado
@Composable
fun CasillaView(
    casilla: Casilla,
    colorDueno: Color?,
    resaltada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val forma = RoundedCornerShape(8.dp)
    val colorBorde = if (resaltada) ColoresJuego.Valida else Color(0x55000000)
    val grosor = if (resaltada) 3.dp else 1.dp
    val tieneFicha = casilla.ficha != null

    Box(
        modifier = modifier
            .clip(forma)
            .background(ColoresJuego.deCasilla(casilla.tipo))
            .background(colorDueno?.copy(alpha = 0.45f) ?: Color.Transparent)
            .border(grosor, colorBorde, forma)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // icono del tipo: grande si esta vacia, chiquito en la esquina si ya tiene ficha
        if (casilla.tipo != TipoCasilla.NORMAL) {
            Text(
                text = ColoresJuego.icono(casilla.tipo),
                fontSize = if (tieneFicha) 11.sp else 22.sp,
                modifier = Modifier
                    .align(if (tieneFicha) Alignment.TopStart else Alignment.Center)
                    .padding(2.dp)
            )
        }

        casilla.ficha?.let { ficha ->
            val formaFicha = RoundedCornerShape(4.dp)
            Box(
                modifier = Modifier
                    .clip(formaFicha)
                    .background(Color(0xFFF5F0E1))
                    .border(2.dp, colorDueno ?: Color.DarkGray, formaFicha)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${ficha.izq}|${ficha.der}",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // fortaleza con una sola conquista: falta la segunda
        if (casilla.tipo == TipoCasilla.FORTALEZA && tieneFicha && casilla.conquistas < 2) {
            Text(
                text = "1/2",
                fontSize = 10.sp,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
            )
        }
    }
}