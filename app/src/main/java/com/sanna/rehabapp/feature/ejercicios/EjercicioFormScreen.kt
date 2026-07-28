package com.sanna.rehabapp.feature.ejercicios

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EjercicioFormScreen(
    onGuardado: () -> Unit,
    onVolver: () -> Unit,
    viewModel: EjercicioFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectorArchivo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> viewModel.onArchivoSeleccionado(uri) }

    LaunchedEffect(uiState.guardadoExitoso) {
        if (uiState.guardadoExitoso) onGuardado()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.esEdicion) "Editar ejercicio" else "Registrar ejercicio") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = uiState.nombre,
                onValueChange = viewModel::onNombreCambiado,
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.descripcion,
                onValueChange = viewModel::onDescripcionCambiada,
                label = { Text("Descripción") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.categoria,
                onValueChange = viewModel::onCategoriaCambiada,
                label = { Text("Categoría") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Ángulos de referencia por articulación (opcional)",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            uiState.patronesReferencia.forEach { fila ->
                FilaPatronReferencia(
                    fila = fila,
                    onArticulacionCambiada = { viewModel.onArticulacionCambiada(fila.idFila, it) },
                    onAnguloMinCambiado = { viewModel.onAnguloMinCambiado(fila.idFila, it) },
                    onAnguloMaxCambiado = { viewModel.onAnguloMaxCambiado(fila.idFila, it) },
                    onEliminar = { viewModel.eliminarArticulacion(fila.idFila) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            TextButton(onClick = viewModel::agregarArticulacion) {
                Text("+ Agregar articulación")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Material terapéutico (imagen o video)",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { selectorArchivo.launch(arrayOf("image/*", "video/*")) }) {
                Text(
                    when {
                        uiState.archivoSeleccionado != null -> "Archivo seleccionado ✓"
                        uiState.materialUrlActual.isNotBlank() -> "Reemplazar material actual"
                        else -> "Seleccionar archivo"
                    },
                )
            }

            uiState.error?.let { mensaje ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = mensaje, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = viewModel::guardar,
                enabled = !uiState.guardando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.guardando) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Guardar ejercicio")
                }
            }
        }
    }
}

@Composable
private fun FilaPatronReferencia(
    fila: PatronReferenciaFila,
    onArticulacionCambiada: (String) -> Unit,
    onAnguloMinCambiado: (String) -> Unit,
    onAnguloMaxCambiado: (String) -> Unit,
    onEliminar: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = fila.articulacion,
            onValueChange = onArticulacionCambiada,
            label = { Text("Articulación (ej. rodilla derecha)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = fila.anguloMin,
                onValueChange = onAnguloMinCambiado,
                label = { Text("Mín. (°)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedTextField(
                value = fila.anguloMax,
                onValueChange = onAnguloMaxCambiado,
                label = { Text("Máx. (°)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        TextButton(onClick = onEliminar) { Text("Quitar articulación") }
    }
}
