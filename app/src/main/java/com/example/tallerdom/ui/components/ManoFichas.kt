package com.example.tallerdom.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tallerdom.model.Ficha

// las fichas del jugador en una fila con scroll horizontal
@Composable
fun ManoFichas(
    mano: List<Ficha>,
    seleccionada: Ficha?,
    onFichaClick: (Ficha) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
    ) {
        items(items = mano, key = { "${it.izq}-${it.der}" }) { ficha ->
            FichaView(
                ficha = ficha,
                seleccionada = ficha == seleccionada,
                onClick = { onFichaClick(ficha) }
            )
        }
    }
}