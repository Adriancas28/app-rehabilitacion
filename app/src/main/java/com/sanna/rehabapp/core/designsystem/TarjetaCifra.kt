package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.sanna.rehabapp.core.theme.Spacing

// Design System — tarjeta de resumen compacta y centrada (etiqueta arriba,
// cifra debajo), en pares lado a lado: "Repeticiones 12 / 12" + "Promedio 80%".
// Distinta de TarjetaEstadistica (ícono + cifra alineados a la izquierda).
@Composable
fun TarjetaCifra(
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier,
    colorValor: Color = MaterialTheme.colorScheme.onSurface,
) {
    TarjetaBase(modifier = modifier) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = valor,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = colorValor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
