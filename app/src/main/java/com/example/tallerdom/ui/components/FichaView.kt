package com.example.tallerdom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// una ficha de domino vertical: numero de arriba, linea y numero de abajo
@Composable
fun FichaView(
    ficha: com.example.tallerdom.model.Ficha,
    modifier: Modifier = Modifier,
    seleccionada: Boolean = false,
    ancho: Dp = 44.dp,
    onClick: (() -> Unit)? = null
) {
    val forma = RoundedCornerShape(6.dp)
    val colorBorde = if (seleccionada) MaterialTheme.colorScheme.primary else Color.DarkGray
    val grosor = if (seleccionada) 3.dp else 1.dp

    Column(
        modifier = modifier
            .width(ancho)
            .height(ancho * 2)
            .clip(forma)
            .background(Color(0xFFF5F0E1))
            .border(grosor, colorBorde, forma)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            text = ficha.izq.toString(),
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = (ancho.value * 0.45f).sp
        )
        HorizontalDivider(modifier = Modifier.width(ancho * 0.6f), color = Color.DarkGray)
        Text(
            text = ficha.der.toString(),
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = (ancho.value * 0.45f).sp
        )
    }
}