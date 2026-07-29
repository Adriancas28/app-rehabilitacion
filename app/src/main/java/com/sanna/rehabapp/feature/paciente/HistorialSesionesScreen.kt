package com.sanna.rehabapp.feature.paciente

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialSesionesScreen(
    onVolver: () -> Unit,
    onSesionSeleccionada: (sesionId: String) -> Unit,
    viewModel: HistorialSesionesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            when {
                uiState.cargando -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                // HU13-CA04
                uiState.sesiones.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Aún no has completado ninguna sesión.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> LazyColumn {
                    items(uiState.sesiones, key = { it.sesionId }) { item ->
                        TarjetaSesionCompletada(
                            item = item,
                            onClick = { onSesionSeleccionada(item.sesionId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaSesionCompletada(item: SesionCompletada, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.EventNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.ejercicio.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = item.sesion.fechaEjecucion?.let(::formatearFecha) ?: "Fecha no disponible",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item.sesion.resultado?.let { resultado ->
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "${resultado.porcentajeEjecucion.toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

private fun formatearFecha(fecha: Date): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fecha)
