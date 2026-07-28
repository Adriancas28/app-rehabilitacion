package com.sanna.rehabapp.feature.ejercicios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.navigation.FisioterapeutaScaffold
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

    FisioterapeutaScaffold(
        pestanaActual = PestanaFisioterapeuta.EJERCICIOS,
        onCambiarPestana = { pestana ->
            if (pestana == PestanaFisioterapeuta.PACIENTES) onNavegarAPacientes()
        },
        topBar = {
            TopAppBar(
                title = { Text("Ejercicios") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    IconButton(onClick = onRegistrarEjercicio) {
                        Icon(Icons.Filled.Add, contentDescription = "Registrar ejercicio")
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

                uiState.ejercicios.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.SelfImprovement,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            text = "Aún no hay ejercicios registrados.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
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
    var menuAbierto by remember { mutableStateOf(false) }

    Card(
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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = ejercicio.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = ejercicio.categoria,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = ejercicio.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
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
