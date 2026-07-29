package com.sanna.rehabapp.feature.pacientes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.sanna.rehabapp.core.navigation.ItemBarraLateral
import com.sanna.rehabapp.core.navigation.ScaffoldConBarraLateral
import com.sanna.rehabapp.domain.model.Ejercicio
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultadosScreen(
    menuVisible: Boolean,
    onCambiarMenuVisible: (Boolean) -> Unit,
    onNavegarAPacientes: () -> Unit,
    onNavegarAEjercicios: () -> Unit,
    onVerResultado: (pacienteId: String, sesionId: String) -> Unit,
    viewModel: ResultadosViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var menuEjercicioAbierto by remember { mutableStateOf(false) }

    ScaffoldConBarraLateral(
        menuVisible = menuVisible,
        onCambiarMenuVisible = onCambiarMenuVisible,
        items = listOf(
            ItemBarraLateral("Pacientes", Icons.Filled.People, seleccionado = false, onClick = onNavegarAPacientes),
            ItemBarraLateral(
                "Ejercicios",
                Icons.Filled.FitnessCenter,
                seleccionado = false,
                onClick = onNavegarAEjercicios,
            ),
            ItemBarraLateral("Resultados", Icons.Filled.Assessment, seleccionado = true, onClick = {}),
        ),
        topBar = { onAlternarMenu ->
            TopAppBar(
                title = { Text("Resultados") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                navigationIcon = {
                    IconButton(onClick = onAlternarMenu) {
                        Icon(Icons.Filled.Menu, contentDescription = "Mostrar/ocultar menú")
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
            // HU18-CA03: filtro por período.
            Row {
                PeriodoFiltro.entries.forEach { periodo ->
                    FilterChip(
                        selected = uiState.filtroPeriodo == periodo,
                        onClick = { viewModel.onFiltroPeriodoCambiado(periodo) },
                        label = { Text(periodo.etiqueta) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // HU18-CA03: filtro por ejercicio.
            val ejercicioSeleccionado = uiState.ejerciciosDisponibles.find { it.id == uiState.filtroEjercicioId }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = ejercicicoNombreOTodos(ejercicioSeleccionado),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { menuEjercicioAbierto = true },
                )
                DropdownMenu(
                    expanded = menuEjercicioAbierto,
                    onDismissRequest = { menuEjercicioAbierto = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    DropdownMenuItem(
                        text = { Text("Todos los ejercicios") },
                        onClick = {
                            viewModel.onFiltroEjercicioCambiado(null)
                            menuEjercicioAbierto = false
                        },
                    )
                    uiState.ejerciciosDisponibles.forEach { ejercicio ->
                        DropdownMenuItem(
                            text = { Text(ejercicio.nombre) },
                            onClick = {
                                viewModel.onFiltroEjercicioCambiado(ejercicio.id)
                                menuEjercicioAbierto = false
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.cargando -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                uiState.sesiones.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No hay sesiones completadas que cumplan el filtro.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> LazyColumn {
                    items(uiState.sesiones, key = { "${it.sesion.pacienteId}-${it.sesion.id}" }) { item ->
                        TarjetaResultado(
                            item = item,
                            onClick = {
                                item.sesion.pacienteId?.let { pacienteId ->
                                    onVerResultado(pacienteId, item.sesion.id)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun ejercicicoNombreOTodos(ejercicio: Ejercicio?): String =
    ejercicio?.nombre ?: "Todos los ejercicios"

@Composable
private fun TarjetaResultado(item: SesionConDetalle, onClick: () -> Unit) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.ejercicio?.nombre ?: "Ejercicio eliminado", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = item.nombrePaciente,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.sesion.fechaAsignacion?.let { fecha ->
                    Text(
                        text = formatearFecha(fecha),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatearFecha(fecha: Date): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fecha)
