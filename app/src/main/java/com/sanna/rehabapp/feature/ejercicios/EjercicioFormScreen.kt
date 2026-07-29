package com.sanna.rehabapp.feature.ejercicios

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            SeccionFormulario(titulo = "Información básica") {
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            SeccionFormulario(titulo = "Ángulos de referencia por articulación (opcional)") {
                uiState.patronesReferencia.forEach { fila ->
                    FilaPatronReferencia(
                        fila = fila,
                        onArticulacionCambiada = { viewModel.onArticulacionCambiada(fila.idFila, it) },
                        onAnguloMinCambiado = { viewModel.onAnguloMinCambiado(fila.idFila, it) },
                        onAnguloMaxCambiado = { viewModel.onAnguloMaxCambiado(fila.idFila, it) },
                        onEliminar = { viewModel.eliminarArticulacion(fila.idFila) },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                OutlinedButton(onClick = viewModel::agregarArticulacion, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.width(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Agregar articulación")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SeccionFormulario(titulo = "Material terapéutico (imagen o video)") {
                OutlinedButton(
                    onClick = { selectorArchivo.launch(arrayOf("image/*", "video/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = if (uiState.archivoSeleccionado != null) Icons.Filled.CheckCircle else Icons.Filled.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.width(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        when {
                            uiState.archivoSeleccionado != null -> "Archivo seleccionado"
                            uiState.materialUrlActual.isNotBlank() -> "Reemplazar material actual"
                            else -> "Seleccionar archivo"
                        },
                    )
                }
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
                    Text("Guardar ejercicio", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun SeccionFormulario(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Text(text = titulo, style = MaterialTheme.typography.titleSmall)
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = contenido)
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = fila.articulacion,
                onValueChange = onArticulacionCambiada,
                label = { Text("Articulación (ej. rodilla derecha)") },
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onEliminar) {
                Icon(
                    Icons.Filled.RemoveCircleOutline,
                    contentDescription = "Quitar articulación",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
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
    }
}
