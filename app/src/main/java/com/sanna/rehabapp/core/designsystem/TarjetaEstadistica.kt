package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.sanna.rehabapp.core.theme.Spacing

// Design System — tarjeta de estadística de dashboard (ícono + cifra
// grande + etiqueta). Usar esta en vez de armar la combinación a mano en
// cada pantalla de resumen/dashboard.
@Composable
fun TarjetaEstadistica(
    icono: ImageVector,
    valor: String,
    etiqueta: String,
    modifier: Modifier = Modifier,
) {
    TarjetaBase(modifier = modifier) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(text = valor, style = MaterialTheme.typography.displaySmall)
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
