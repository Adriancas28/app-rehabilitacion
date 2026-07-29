package com.sanna.rehabapp.feature.pacientes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.domain.model.Ejercicio
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsignarSesionScreen(
    onGuardado: () -> Unit,
    onVolver: () -> Unit,
    viewModel: AsignarSesionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var menuEjercicioAbierto by remember { mutableStateOf(false) }
    var menuRepeticionesAbierto by remember { mutableStateOf(false) }
    var selectorFechaAbierto by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.guardadoExitoso) {
        if (uiState.guardadoExitoso) onGuardado()
    }

    val ejercicioSeleccionado = uiState.ejercicios.find { it.id == uiState.ejercicioSeleccionadoId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.esEdicion) "Editar sesión" else "Asignar sesión") },
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
        if (uiState.cargando) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            EtiquetaConIcono(Icons.Filled.FitnessCenter, "Ejercicio")
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = ejercicioSeleccionado?.nombre ?: "",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Selecciona un ejercicio") },
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                // Overlay transparente: intercepta el toque para abrir el menú
                // sin pelear con el foco/cursor propio del OutlinedTextField.
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
                    if (uiState.ejercicios.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No hay ejercicios registrados todavía.") },
                            onClick = {},
                            enabled = false,
                        )
                    }
                    uiState.ejercicios.forEach { ejercicio ->
                        DropdownMenuItem(
                            text = { Text(ejercicio.nombre) },
                            onClick = {
                                viewModel.onEjercicioSeleccionado(ejercicio.id)
                                menuEjercicioAbierto = false
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            EtiquetaConIcono(Icons.Filled.CalendarMonth, "Fecha de asignación")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { selectorFechaAbierto = true },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.width(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(uiState.fechaAsignacion?.let(::formatearFecha) ?: "Seleccionar fecha")
            }

            Spacer(modifier = Modifier.height(20.dp))
            EtiquetaConIcono(Icons.Filled.Repeat, "Repeticiones")
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.repeticiones?.let { "$it repeticiones" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    enabled = ejercicioSeleccionado != null,
                    placeholder = { Text("Selecciona un ejercicio primero") },
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (ejercicioSeleccionado != null) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { menuRepeticionesAbierto = true },
                    )
                }
                DropdownMenu(
                    expanded = menuRepeticionesAbierto,
                    onDismissRequest = { menuRepeticionesAbierto = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OPCIONES_REPETICIONES.forEach { opcion ->
                        DropdownMenuItem(
                            text = { Text("$opcion repeticiones") },
                            onClick = {
                                viewModel.onRepeticionesCambiadas(opcion)
                                menuRepeticionesAbierto = false
                            },
                        )
                    }
                }
            }

            val repeticionesSeleccionadas = uiState.repeticiones
            if (ejercicioSeleccionado != null && repeticionesSeleccionadas != null) {
                Spacer(modifier = Modifier.height(10.dp))
                TarjetaDuracionEstimada(ejercicioSeleccionado, repeticionesSeleccionadas)
            }

            Spacer(modifier = Modifier.height(20.dp))
            EtiquetaConIcono(Icons.Filled.EditNote, "Notas (opcional)")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.notas,
                onValueChange = viewModel::onNotasCambiadas,
                placeholder = { Text("Indicaciones adicionales…") },
                minLines = 2,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            uiState.error?.let { mensaje ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = mensaje, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = viewModel::guardar,
                enabled = !uiState.guardando,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (uiState.guardando) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        if (viewModel.esEdicion) "Guardar cambios" else "Asignar sesión",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }

    if (selectorFechaAbierto) {
        val estadoDatePicker = rememberDatePickerState(
            initialSelectedDateMillis = uiState.fechaAsignacion?.let(::diaLocalAUtcMillis)
                ?: diaLocalAUtcMillis(Date()),
        )
        DatePickerDialog(
            onDismissRequest = { selectorFechaAbierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoDatePicker.selectedDateMillis?.let { viewModel.onFechaSeleccionada(utcMillisADiaLocal(it)) }
                    selectorFechaAbierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { selectorFechaAbierto = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = estadoDatePicker)
        }
    }
}

@Composable
private fun EtiquetaConIcono(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icono,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = texto, style = MaterialTheme.typography.titleSmall)
    }
}

// HU03-CA06: duración total estimada (repeticiones × duración por
// repetición), solo informativa — no se guarda, se recalcula al vuelo.
@Composable
private fun TarjetaDuracionEstimada(ejercicio: Ejercicio, repeticiones: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Duración estimada",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatearDuracionEstimada(ejercicio.duracionSegundos * repeticiones),
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

private fun formatearDuracionEstimada(segundosTotales: Int): String =
    if (segundosTotales >= 60) "${(segundosTotales + 59) / 60} min" else "$segundosTotales s"

private fun formatearFecha(fecha: Date): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fecha)
