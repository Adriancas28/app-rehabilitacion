package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sanna.rehabapp.core.theme.Spacing

// Design System — lista de selección múltiple agrupada por categoría
// (ej. diagnósticos por región corporal): encabezado de grupo + checkbox
// por opción. Reemplaza el patrón repetido a mano en el selector de
// diagnóstico del paciente y el de diagnósticos sugeridos de un ejercicio.
@Composable
fun <T> ChecklistAgrupado(
    opciones: List<T>,
    seleccionados: Set<T>,
    agruparPor: (T) -> String,
    etiquetaDeOpcion: (T) -> String,
    onAlternar: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        opciones.groupBy(agruparPor).forEach { (grupo, itemsDelGrupo) ->
            Text(
                text = grupo,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = Spacing.sm, bottom = 2.dp),
            )
            itemsDelGrupo.forEach { opcion ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAlternar(opcion) },
                ) {
                    Checkbox(
                        checked = opcion in seleccionados,
                        onCheckedChange = { onAlternar(opcion) },
                    )
                    Text(text = etiquetaDeOpcion(opcion), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
