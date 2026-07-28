package com.sanna.rehabapp.feature.pacientes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.domain.model.EstadoSesion
import com.sanna.rehabapp.domain.model.Sesion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacienteDetalleScreen(
    onVolver: () -> Unit,
    viewModel: PacienteDetalleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.paciente?.nombre ?: "Paciente") },
                navigationIcon = {
                    TextButton(onClick = onVolver) { Text("Atrás") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            uiState.paciente?.let { paciente ->
                Text(text = paciente.email, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Progreso: ${uiState.sesionesCompletadas} de ${uiState.sesiones.size} " +
                    "sesiones completadas",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Sesiones registradas", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.sesiones.isEmpty()) {
                Text(
                    text = "Este paciente aún no tiene sesiones registradas.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn {
                    items(uiState.sesiones, key = { it.id }) { sesion ->
                        TarjetaSesion(sesion)
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaSesion(sesion: Sesion) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Ejercicio: ${sesion.ejercicioId}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = if (sesion.estado == EstadoSesion.COMPLETADA) "Completada" else "Pendiente",
                style = MaterialTheme.typography.bodySmall,
            )
            sesion.resultado?.let { resultado ->
                Text(
                    text = "Ejecución: ${resultado.porcentajeEjecucion.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
