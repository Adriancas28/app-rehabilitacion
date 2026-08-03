package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

// Design System — sección de formulario: título arriba + TarjetaBase
// debajo. Usar esta para agrupar campos relacionados dentro de un
// formulario largo (ej. "Información básica", "Ángulos de referencia").
@Composable
fun SeccionFormulario(
    titulo: String,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = titulo, style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(Spacing.sm))
        TarjetaBase(contenido = contenido)
    }
}
