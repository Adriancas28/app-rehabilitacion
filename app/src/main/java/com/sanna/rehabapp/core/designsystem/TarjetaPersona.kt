package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Design System — fila de persona (paciente/fisioterapeuta/usuario en
// general): avatar circular con iniciales + nombre + subtítulo, con un
// slot final opcional para badge/menú/botón de acción y una línea extra
// opcional debajo del subtítulo (ej. "Fisioterapeuta: Ana Ruiz"). Usar
// esta en vez de armar la fila de avatar+texto a mano en cada lista.
@Composable
fun TarjetaPersona(
    nombre: String,
    subtitulo: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    lineaExtra: (@Composable () -> Unit)? = null,
    contenidoFinal: (@Composable () -> Unit)? = null,
) {
    TarjetaBase(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = inicialesDe(nombre),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                lineaExtra?.invoke()
            }
            contenidoFinal?.invoke()
        }
    }
}

private fun inicialesDe(nombre: String): String =
    nombre.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
