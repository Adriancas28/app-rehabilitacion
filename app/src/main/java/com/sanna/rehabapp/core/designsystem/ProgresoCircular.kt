package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Design System — indicadores de progreso (anillo y lineal). Usar estos en
// vez de armar un CircularProgressIndicator/LinearProgressIndicator a mano
// en cada pantalla de resumen/resultado/progreso.

@Composable
fun ProgresoCircular(
    porcentaje: Float,
    modifier: Modifier = Modifier,
    tamano: Dp = 64.dp,
    grosor: Dp = 6.dp,
) {
    Box(modifier = modifier.size(tamano), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { porcentaje.coerceIn(0f, 1f) },
            modifier = Modifier.size(tamano),
            strokeWidth = grosor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text(text = "${(porcentaje * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ProgresoLineal(
    porcentaje: Float,
    modifier: Modifier = Modifier,
    grosor: Dp = 8.dp,
) {
    LinearProgressIndicator(
        progress = { porcentaje.coerceIn(0f, 1f) },
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .height(grosor),
    )
}
