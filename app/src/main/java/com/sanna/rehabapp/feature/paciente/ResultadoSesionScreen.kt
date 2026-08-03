package com.sanna.rehabapp.feature.paciente

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
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
import com.sanna.rehabapp.core.designsystem.BotonOutline
import com.sanna.rehabapp.core.designsystem.BotonPrimario
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.ProgresoCircular
import com.sanna.rehabapp.core.designsystem.TarjetaBase
import com.sanna.rehabapp.core.theme.Spacing
import com.sanna.rehabapp.domain.model.AnguloDetectado
import com.sanna.rehabapp.domain.model.ErrorDetectado
import com.sanna.rehabapp.domain.model.Recomendacion
import com.sanna.rehabapp.domain.model.ResultadoSesion

@Composable
fun ResultadoSesionScreen(
    onVolver: () -> Unit,
    onVerProgreso: () -> Unit,
    onVolverAlInicio: () -> Unit,
    viewModel: ResultadoSesionViewModel = hiltViewModel(),
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
                    TarjetaPorcentaje(resultado)
                    Spacer(modifier = Modifier.height(Spacing.lg - 4.dp))

                    if (resultado.angulosDetectados.isNotEmpty()) {
                        Text(text = "Ángulos por articulación", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        resultado.angulosDetectados.forEach { angulo ->
                            TarjetaAngulo(angulo)
                            Spacer(modifier = Modifier.height(Spacing.sm))
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                    }

                    if (resultado.erroresDetectados.isNotEmpty()) {
                        Text(text = "Observaciones", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        resultado.erroresDetectados.forEach { error ->
                            TarjetaError(error)
                            Spacer(modifier = Modifier.height(Spacing.sm))
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                    }

                    // HU16-CA01/CA02/CA04 — a diferencia de "Observaciones",
                    // esta sección siempre se muestra, con un mensaje de
                    // ausencia si todavía no hay recomendaciones (CA04).
                    Text(text = "Recomendaciones de tu fisioterapeuta", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    if (uiState.recomendaciones.isEmpty()) {
                        Text(
                            text = "Tu fisioterapeuta aún no registró recomendaciones para esta sesión.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        uiState.recomendaciones.forEach { recomendacion ->
                            TarjetaRecomendacion(recomendacion)
                            Spacer(modifier = Modifier.height(Spacing.sm))
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.lg - 4.dp))
                    BotonPrimario(texto = "Ver mi progreso", onClick = onVerProgreso)
                    Spacer(modifier = Modifier.height(Spacing.sm + 2.dp))
                    BotonOutline(texto = "Volver al inicio", onClick = onVolverAlInicio)
                }
            }
        }
    }
}

@Composable
private fun TarjetaPorcentaje(resultado: ResultadoSesion) {
    TarjetaBase {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgresoCircular(porcentaje = resultado.porcentajeEjecucion / 100f)
            Spacer(modifier = Modifier.width(Spacing.md))
            Column {
                // HU06-CA07: si se finalizó antes de tiempo, repeticionesCompletadas
                // es menor a repeticionesAsignadas — se ve reflejado aquí.
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

@Composable
private fun TarjetaRecomendacion(recomendacion: Recomendacion) {
    TarjetaBase(relleno = Spacing.sm + 6.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(Spacing.sm + 4.dp))
            Text(text = recomendacion.texto, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TarjetaError(error: ErrorDetectado) {
    TarjetaBase(relleno = Spacing.sm + 6.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(Spacing.sm + 4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "${error.articulacion} — ${error.tipo}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Detectado ${error.repeticiones} ${if (error.repeticiones == 1) "vez" else "veces"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
