package com.sanna.rehabapp.feature.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.domain.model.TipoDiagnostico
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPacienteFormScreen(
    onGuardado: () -> Unit,
    onVolver: () -> Unit,
    viewModel: AdminPacienteFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var mostrarPassword by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Confirmación visible (Snackbar) antes de volver a la lista, para que
    // el CRUD de paciente no sea una acción silenciosa.
    LaunchedEffect(uiState.guardadoExitoso) {
        if (uiState.guardadoExitoso) {
            // No se espera a que el Snackbar termine de mostrarse (duraría
            // varios segundos) — se lanza aparte y se navega tras una
            // pausa breve, para que alcance a verse antes de salir.
            launch { snackbarHostState.showSnackbar(if (viewModel.esEdicion) "Paciente actualizado." else "Paciente creado.") }
            delay(700)
            onGuardado()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.esEdicion) "Editar paciente" else "Registrar paciente") },
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
            OutlinedTextField(
                value = uiState.nombre,
                onValueChange = viewModel::onNombreCambiado,
                label = { Text("Nombre completo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailCambiado,
                label = { Text("Correo electrónico") },
                singleLine = true,
                enabled = !viewModel.esEdicion,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            if (!viewModel.esEdicion) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordCambiado,
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = if (mostrarPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { mostrarPassword = !mostrarPassword }) {
                            Icon(
                                imageVector = if (mostrarPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (mostrarPassword) "Ocultar contraseña" else "Ver contraseña",
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.dni,
                onValueChange = viewModel::onDniCambiado,
                label = { Text("DNI") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.edad,
                onValueChange = viewModel::onEdadCambiado,
                label = { Text("Edad") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Diagnóstico(s)", style = MaterialTheme.typography.titleSmall)
            SelectorDiagnosticos(
                seleccionados = uiState.diagnosticosSeleccionados,
                onAlternar = viewModel::onDiagnosticoAlternado,
            )

            uiState.error?.let { mensaje ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = mensaje, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = viewModel::guardar,
                enabled = !uiState.guardando && !uiState.cargando,
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
                    Text(if (viewModel.esEdicion) "Guardar cambios" else "Crear cuenta")
                }
            }
        }
    }
}

// HU01-CA06/HU20-CA02 (ampliación): un paciente puede tener varios
// diagnósticos a la vez, agrupados por región corporal para que la lista
// de 13 valores sea fácil de recorrer.
@Composable
private fun SelectorDiagnosticos(
    seleccionados: Set<TipoDiagnostico>,
    onAlternar: (TipoDiagnostico) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TipoDiagnostico.entries.groupBy { it.regionCorporal }.forEach { (region, diagnosticos) ->
            Text(
                text = region,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
            )
            diagnosticos.forEach { tipo ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAlternar(tipo) },
                ) {
                    Checkbox(checked = tipo in seleccionados, onCheckedChange = { onAlternar(tipo) })
                    Text(text = tipo.etiqueta, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
