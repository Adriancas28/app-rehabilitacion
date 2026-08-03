package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Design System — anillo circular de progreso/porcentaje, con la cifra
// centrada. Usar este en vez de armar un CircularProgressIndicator +
// Text superpuestos a mano en cada pantalla de resumen/resultado.
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
