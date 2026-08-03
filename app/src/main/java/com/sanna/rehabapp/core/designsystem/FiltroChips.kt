package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sanna.rehabapp.core.theme.Spacing

// Design System — fila de chips de filtro de una sola selección (ej.
// período Todos/Última semana/Último mes). Reemplaza el Row+FilterChip
// repetido a mano en cada pantalla con un filtro por período/categoría.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FilaChipsFiltro(
    opciones: List<T>,
    seleccionado: T,
    etiquetaDeOpcion: (T) -> String,
    onSeleccionar: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        opciones.forEach { opcion ->
            FilterChip(
                selected = opcion == seleccionado,
                onClick = { onSeleccionar(opcion) },
                label = { Text(etiquetaDeOpcion(opcion)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
        }
    }
}
