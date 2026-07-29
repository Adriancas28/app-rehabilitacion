package com.sanna.rehabapp.feature.paciente

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.domain.model.AnguloDetectado
import com.sanna.rehabapp.domain.model.ErrorDetectado
import com.sanna.rehabapp.domain.model.ResultadoSesion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultadoSesionScreen(
    onVolver: () -> Unit,
    onVerProgreso: () -> Unit,
    onVolverAlInicio: () -> Unit,
    viewModel: ResultadoSesionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.ejercicio?.nombre ?: "Resultado") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.cargando -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

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
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    TarjetaPorcentaje(resultado)
                    Spacer(modifier = Modifier.height(20.dp))

                    if (resultado.angulosDetectados.isNotEmpty()) {
                        Text(text = "Ángulos por articulación", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        resultado.angulosDetectados.forEach { angulo ->
                            TarjetaAngulo(angulo)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (resultado.erroresDetectados.isNotEmpty()) {
                        Text(text = "Observaciones", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        resultado.erroresDetectados.forEach { error ->
                            TarjetaError(error)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onVerProgreso,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Text("Ver mi progreso")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onVolverAlInicio,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Text("Volver al inicio")
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaPorcentaje(resultado: ResultadoSesion) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { (resultado.porcentajeEjecucion / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 6.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    text = "${resultado.porcentajeEjecucion.toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
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
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
private fun TarjetaError(error: ErrorDetectado) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(12.dp))
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
