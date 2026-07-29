package com.sanna.rehabapp.feature.pacientes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
            Text(text = "Ejercicio", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = menuEjercicioAbierto,
                onExpandedChange = { menuEjercicioAbierto = it },
            ) {
                OutlinedTextField(
                    value = ejercicioSeleccionado?.nombre ?: "",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Selecciona un ejercicio") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuEjercicioAbierto)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = menuEjercicioAbierto, onDismissRequest = { menuEjercicioAbierto = false }) {
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
            Text(text = "Fecha de asignación", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { selectorFechaAbierto = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.width(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(uiState.fechaAsignacion?.let(::formatearFecha) ?: "Seleccionar fecha")
            }

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
            initialSelectedDateMillis = uiState.fechaAsignacion?.time ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { selectorFechaAbierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoDatePicker.selectedDateMillis?.let { viewModel.onFechaSeleccionada(Date(it)) }
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

private fun formatearFecha(fecha: Date): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fecha)
