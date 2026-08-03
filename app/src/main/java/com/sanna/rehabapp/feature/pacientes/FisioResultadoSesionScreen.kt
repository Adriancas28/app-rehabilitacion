package com.sanna.rehabapp.feature.pacientes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.BotonPrimario
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.ProgresoCircular
import com.sanna.rehabapp.core.designsystem.TarjetaBase
import com.sanna.rehabapp.core.theme.AmbarAlertaTexto
import com.sanna.rehabapp.core.theme.Spacing
import com.sanna.rehabapp.core.theme.VerdeExitoTexto
import com.sanna.rehabapp.domain.model.AnguloDetectado
import com.sanna.rehabapp.domain.model.DetalleRepeticion
import com.sanna.rehabapp.domain.model.ResultadoSesion

@Composable
fun FisioResultadoSesionScreen(
    onVolver: () -> Unit,
    onRegistrarRecomendacion: (pacienteId: String, sesionId: String) -> Unit,
    viewModel: FisioResultadoSesionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { BarraSuperior(titulo = uiState.ejercicio?.nombre ?: "Resultado", onNavegarAtras = onVolver) },
    ) { padding ->
        when {
            uiState.cargando -> EstadoCargando(modifier = Modifier.fillMaxSize().padding(padding))

            uiState.resultado == null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No se encontró el resultado de esta sesión.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                val resultado = uiState.resultado!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(Spacing.md)
                        .verticalScroll(rememberScrollState()),
                ) {
                    TarjetaResumen(resultado)
                    Spacer(modifier = Modifier.height(Spacing.lg - 4.dp))

                    if (resultado.detallePorRepeticion.isNotEmpty()) {
                        Text(text = "Detalle por repetición", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        resultado.detallePorRepeticion.forEach { detalle ->
                            TarjetaDetalleRepeticion(detalle)
                            Spacer(modifier = Modifier.height(Spacing.sm))
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                    }

                    if (resultado.angulosDetectados.isNotEmpty()) {
                        Text(text = "Ángulos por articulación", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        resultado.angulosDetectados.forEach { angulo ->
                            TarjetaAngulo(angulo)
                            Spacer(modifier = Modifier.height(Spacing.sm))
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))
                    BotonPrimario(
                        texto = "Registrar recomendación",
                        onClick = { onRegistrarRecomendacion(viewModel.pacienteId, viewModel.sesionId) },
                        icono = Icons.Filled.Add,
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaResumen(resultado: ResultadoSesion) {
    TarjetaBase {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgresoCircular(porcentaje = resultado.porcentajeEjecucion / 100f)
            Spacer(modifier = Modifier.width(Spacing.md))
            Column {
                Text(
                    text = "Repeticiones ${resultado.repeticionesCompletadas}/${resultado.repeticionesAsignadas}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Correctas: ${resultado.repeticionesCorrectas}  ·  " +
                        "Errores: ${resultado.repeticionesCompletadas - resultado.repeticionesCorrectas}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Desviación promedio: ${"%.1f".format(resultado.desviacionPromedio)}°",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// HU18-CA04: lo que el fisioterapeuta usa para decidir qué recomendar —
// por cada repetición, si estuvo bien o qué error puntual tuvo.
@Composable
private fun TarjetaDetalleRepeticion(detalle: DetalleRepeticion) {
    TarjetaBase(relleno = Spacing.sm + 6.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (detalle.dentroDeRango) Icons.Filled.Check else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (detalle.dentroDeRango) VerdeExitoTexto else AmbarAlertaTexto,
            )
            Spacer(modifier = Modifier.width(Spacing.sm + 4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Repetición ${detalle.numero}", style = MaterialTheme.typography.bodyMedium)
                if (detalle.dentroDeRango) {
                    Text(
                        text = "Dentro de rango",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    detalle.errores.forEach { error ->
                        Text(
                            text = "${error.articulacion} — ${error.tipo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaAngulo(angulo: AnguloDetectado) {
    TarjetaBase(relleno = Spacing.sm + 6.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = angulo.articulacion, style = MaterialTheme.typography.bodyMedium)
                angulo.anguloEsperado?.let { esperado ->
                    Text(
                        text = "Esperado: ${esperado.toInt()}°",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = "${angulo.anguloDetectado.toInt()}°",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
