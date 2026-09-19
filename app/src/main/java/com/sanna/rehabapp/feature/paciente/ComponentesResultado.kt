package com.sanna.rehabapp.feature.paciente

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sanna.rehabapp.core.designsystem.GraficoBarras
import com.sanna.rehabapp.core.designsystem.TarjetaBase
import com.sanna.rehabapp.core.designsystem.TarjetaCifra
import com.sanna.rehabapp.core.theme.AmbarAlertaTexto
import com.sanna.rehabapp.core.theme.Spacing
import com.sanna.rehabapp.core.theme.VerdeExitoTexto
import com.sanna.rehabapp.domain.model.DetalleRepeticion
import com.sanna.rehabapp.domain.model.Recomendacion
import com.sanna.rehabapp.domain.model.ResultadoSesion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Piezas que comparten el resultado recién terminado (ResultadoSesionScreen)
// y el detalle de una sesión en "Mis resultados" (MisResultadosScreen): el
// paciente ve TODAS las repeticiones (a diferencia del fisioterapeuta, que
// solo ve las que tuvieron error), verde si >= 75% y ámbar si es menor.
private const val UMBRAL_PRECISION = 75f

@Composable
internal fun TarjetasResumenSesion(resultado: ResultadoSesion) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        TarjetaCifra(
            etiqueta = "Repeticiones",
            valor = "${resultado.repeticionesCompletadas} / ${resultado.repeticionesAsignadas}",
            modifier = Modifier.weight(1f),
        )
        TarjetaCifra(
            etiqueta = "Promedio",
            valor = "${resultado.porcentajeEjecucion.toInt()}%",
            colorValor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun ListaRepeticiones(detalle: List<DetalleRepeticion>, total: Int) {
    val forma = MaterialTheme.shapes.medium
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs + 1.dp)) {
        detalle.sortedBy { it.numero }.forEach { repeticion ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, forma)
                    .padding(horizontal = Spacing.sm + 4.dp, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Repetición ${repeticion.numero}/$total",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "${repeticion.porcentajeEjecucion.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorDePrecision(repeticion.porcentajeEjecucion),
                )
            }
        }
    }
}

@Composable
internal fun GraficoRepeticiones(detalle: List<DetalleRepeticion>) {
    val ordenadas = detalle.sortedBy { it.numero }
    GraficoBarras(
        valores = ordenadas.map { it.porcentajeEjecucion },
        umbral = UMBRAL_PRECISION,
        etiquetas = ordenadas.map { "${it.numero}" },
        mostrarValores = false,
        mostrarEjeY = true,
    )
    Spacer(modifier = Modifier.height(Spacing.sm))
    Text(
        text = "% de precisión por repetición (verde = 75% o más)",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun BloqueRecomendacion(recomendacion: Recomendacion) {
    TarjetaBase {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Rounded.Comment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = "Recomendación de tu fisioterapeuta",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(text = recomendacion.texto, style = MaterialTheme.typography.bodyMedium)
        recomendacion.fecha?.let { fecha ->
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Guardada el ${formatearFechaCorta(fecha)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun colorDePrecision(porcentaje: Float) =
    if (porcentaje >= UMBRAL_PRECISION) VerdeExitoTexto else AmbarAlertaTexto

internal fun formatearFechaCorta(fecha: Date): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fecha)
