package com.sanna.rehabapp.feature.ejercicios

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.navigation.FisioterapeutaBottomBar
import com.sanna.rehabapp.core.navigation.PestanaFisioterapeuta
import com.sanna.rehabapp.domain.model.Ejercicio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EjerciciosListScreen(
    onRegistrarEjercicio: () -> Unit,
    onEditarEjercicio: (String) -> Unit,
    onNavegarAPacientes: () -> Unit,
    viewModel: EjerciciosViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var ejercicioAEliminar by remember { mutableStateOf<Ejercicio?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ejercicios") },
                actions = {
                    TextButton(onClick = onRegistrarEjercicio) { Text("+ Registrar") }
                },
            )
        },
        bottomBar = {
            FisioterapeutaBottomBar(
                pestanaActual = PestanaFisioterapeuta.EJERCICIOS,
                onCambiarPestana = { pestana ->
                    if (pestana == PestanaFisioterapeuta.PACIENTES) onNavegarAPacientes()
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

                uiState.ejercicios.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Aún no hay ejercicios registrados.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                else -> LazyColumn {
                    items(uiState.ejercicios, key = { it.id }) { ejercicio ->
                        TarjetaEjercicio(
                            ejercicio = ejercicio,
                            onEditar = { onEditarEjercicio(ejercicio.id) },
                            onEliminar = { ejercicioAEliminar = ejercicio },
                        )
                    }
                }
            }
        }
    }

    ejercicioAEliminar?.let { ejercicio ->
        AlertDialog(
            onDismissRequest = { ejercicioAEliminar = null },
            title = { Text("Eliminar ejercicio") },
            text = {
                Text("¿Seguro que deseas eliminar \"${ejercicio.nombre}\"? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminar(ejercicio.id)
                    ejercicioAEliminar = null
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { ejercicioAEliminar = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun TarjetaEjercicio(ejercicio: Ejercicio, onEditar: () -> Unit, onEliminar: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = ejercicio.nombre, style = MaterialTheme.typography.titleMedium)
            Text(text = ejercicio.categoria, style = MaterialTheme.typography.bodySmall)
            Text(
                text = ejercicio.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(modifier = Modifier.padding(top = 8.dp)) {
                TextButton(onClick = onEditar) { Text("Editar") }
                TextButton(onClick = onEliminar) { Text("Eliminar") }
            }
        }
    }
}
