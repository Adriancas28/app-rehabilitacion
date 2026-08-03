package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sanna.rehabapp.core.theme.Spacing

// Design System — tarjeta base: toda tarjeta nueva de la app debe envolver
// su contenido con esta, en vez de crear un Card suelto con su propia
// elevación/forma/color.
@Composable
fun TarjetaBase(
    modifier: Modifier = Modifier,
    relleno: Dp = Spacing.md,
    onClick: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val forma = MaterialTheme.shapes.large
    val elevacion = CardDefaults.cardElevation(defaultElevation = 1.dp)
    val colores = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)

    if (onClick != null) {
        Card(
            onClick = onClick,
            shape = forma,
            elevation = elevacion,
            colors = colores,
            modifier = modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(relleno), content = contenido)
        }
    } else {
        Card(
            shape = forma,
            elevation = elevacion,
            colors = colores,
            modifier = modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(relleno), content = contenido)
        }
    }
}
