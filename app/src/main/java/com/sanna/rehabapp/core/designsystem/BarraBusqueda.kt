package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sanna.rehabapp.core.theme.Spacing

// Design System — barra de búsqueda con lupa + botón de filtro opcional a
// un lado. Usar esta en vez de un OutlinedTextField suelto para buscar.
@Composable
fun BarraBusqueda(
    valor: String,
    onValorCambiado: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Buscar...",
    onFiltrar: (() -> Unit)? = null,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = valor,
            onValueChange = onValorCambiado,
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        if (onFiltrar != null) {
            Spacer(modifier = Modifier.width(Spacing.sm))
            IconButton(onClick = onFiltrar) {
                Icon(Icons.Filled.FilterList, contentDescription = "Filtrar")
            }
        }
    }
}
