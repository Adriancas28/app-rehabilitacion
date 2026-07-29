package com.sanna.rehabapp.feature.comunicacion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.domain.model.Recomendacion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarRecomendacionScreen(
    onVolver: () -> Unit,
    viewModel: RegistrarRecomendacionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var recomendacionAEliminar by remember { mutableStateOf<Recomendacion?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recomendaciones") },
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
            Text(
                text = if (uiState.editandoId != null) "Editar recomendación" else "Nueva recomendación",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.texto,
                onValueChange = viewModel::onTextoCambiado,
                placeholder = { Text("Ej. Mantén la espalda recta durante el ejercicio…") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            uiState.error?.let { mensaje ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = mensaje, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                if (uiState.editandoId != null) {
                    TextButton(onClick = viewModel::cancelarEdicion) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Button(onClick = viewModel::guardar, enabled = !uiState.guardando) {
                    Text(if (uiState.editandoId != null) "Guardar cambios" else "Registrar")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Registradas", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            when {
                uiState.cargando -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                uiState.recomendaciones.isEmpty() -> Text(
                    text = "Todavía no hay recomendaciones para esta sesión.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> LazyColumn {
                    items(uiState.recomendaciones, key = { it.id }) { recomendacion ->
                        TarjetaRecomendacion(
                            recomendacion = recomendacion,
                            onEditar = { viewModel.editar(recomendacion) },
                            onEliminar = { recomendacionAEliminar = recomendacion },
                        )
                    }
                }
            }
        }
    }

    recomendacionAEliminar?.let { recomendacion ->
        AlertDialog(
            onDismissRequest = { recomendacionAEliminar = null },
            title = { Text("Eliminar recomendación") },
            text = { Text("¿Seguro que deseas eliminarla? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminar(recomendacion.id)
                    recomendacionAEliminar = null
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { recomendacionAEliminar = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun TarjetaRecomendacion(recomendacion: Recomendacion, onEditar: () -> Unit, onEliminar: () -> Unit) {
    var menuAbierto by remember { mutableStateOf(false) }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = recomendacion.texto, style = MaterialTheme.typography.bodyMedium)
                recomendacion.fecha?.let { fecha ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatearFecha(fecha),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box {
                IconButton(onClick = { menuAbierto = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones")
                }
                DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            menuAbierto = false
                            onEditar()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar") },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = {
                            menuAbierto = false
                            onEliminar()
                        },
                    )
                }
            }
        }
    }
}

private fun formatearFecha(fecha: Date): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fecha)
