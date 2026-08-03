package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sanna.rehabapp.core.theme.Spacing

// Design System — tarjeta con avatar de ícono (círculo de color + ícono),
// título y subtítulo opcional, con slots para contenido final (chip/menú/
// chevron) y contenido inferior (ej. una línea de metadatos + un botón de
// acción). Distinta de TarjetaPersona (avatar con iniciales, para
// personas) y de TarjetaEjercicio (tarjeta de grid con imagen/ícono como
// banner) — esta es para tarjetas de "acción" en forma de fila: próxima
// sesión, sesión reanudable, acceso rápido, ejercicio asignado, etc.
@Composable
fun TarjetaConIcono(
    icono: ImageVector,
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    colorContenedorIcono: Color = MaterialTheme.colorScheme.primaryContainer,
    colorIcono: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: (() -> Unit)? = null,
    contenidoFinal: (@Composable () -> Unit)? = null,
    contenidoInferior: (@Composable ColumnScope.() -> Unit)? = null,
) {
    TarjetaBase(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(colorContenedorIcono, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icono, contentDescription = null, tint = colorIcono)
            }
            Spacer(modifier = Modifier.width(Spacing.sm + 6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = titulo, style = MaterialTheme.typography.titleMedium)
                subtitulo?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            contenidoFinal?.invoke()
        }
        if (contenidoInferior != null) {
            Spacer(modifier = Modifier.height(Spacing.md))
            contenidoInferior()
        }
    }
}
