package com.sanna.rehabapp.feature.paciente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.List
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.BotonOutline
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.EstadoVacio
import com.sanna.rehabapp.core.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Locale

// Detalle de una sesión realizada (se abre al tocar una tarjeta de "Mis
// resultados"): tarjetas Repeticiones/Promedio, detalle por repetición
// (lista o gráfico) y la recomendación del fisioterapeuta.
@Composable
fun DetalleResultadoScreen(
    onVolver: () -> Unit,
    viewModel: DetalleResultadoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var mostrarGrafico by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = uiState.nombreEjercicio.ifBlank { "Resultado" },
                onNavegarAtras = onVolver,
            )
        },
    ) { padding ->
        val resultado = uiState.resultado
        when {
            uiState.cargando -> EstadoCargando(modifier = Modifier.fillMaxSize().padding(padding))

            resultado == null -> EstadoVacio(
                icono = Icons.Rounded.History,
                mensaje = "No se encontró el resultado de esta sesión.",
                modifier = Modifier.fillMaxSize().padding(padding),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(Spacing.md)
                    .verticalScroll(rememberScrollState()),
            ) {
                uiState.fecha?.let {
                    Text(
                        text = "Sesión del ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }

                TarjetasResumenSesion(resultado)
                Spacer(modifier = Modifier.height(Spacing.md))

                if (resultado.detallePorRepeticion.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "Detalle por repetición", style = MaterialTheme.typography.titleSmall)
                        BotonOutline(
                            texto = if (mostrarGrafico) "Mostrar lista" else "Mostrar gráfico",
                            onClick = { mostrarGrafico = !mostrarGrafico },
                            icono = if (mostrarGrafico) Icons.Rounded.List else Icons.Rounded.BarChart,
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    if (mostrarGrafico) {
                        GraficoRepeticiones(resultado.detallePorRepeticion)
                    } else {
                        ListaRepeticiones(resultado.detallePorRepeticion, total = resultado.repeticionesAsignadas)
                    }
                    Spacer(modifier = Modifier.height(Spacing.md))
                }

                // Solo las recomendaciones que el fisioterapeuta ya guardó para ESTA sesión.
                uiState.recomendaciones.forEach { recomendacion ->
                    BloqueRecomendacion(recomendacion)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
            }
        }
    }
}
