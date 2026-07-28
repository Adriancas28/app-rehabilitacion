package com.sanna.rehabapp.feature.pacientes

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.navigation.CerrarSesionViewModel
import com.sanna.rehabapp.domain.model.Usuario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacientesListScreen(
    onPacienteSeleccionado: (String) -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: PacientesViewModel = hiltViewModel(),
    cerrarSesionViewModel: CerrarSesionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pacientes") },
                actions = {
                    TextButton(onClick = {
                        cerrarSesionViewModel.cerrarSesion()
                        onCerrarSesion()
                    }) {
                        Text("Salir")
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
            OutlinedTextField(
                value = uiState.consultaBusqueda,
                onValueChange = viewModel::onConsultaCambiada,
                label = { Text("Buscar paciente...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.cargando -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                uiState.totalPacientes == 0 -> MensajeEstadoVacio(
                    "Aún no tienes pacientes asignados.",
                )

                uiState.pacientesFiltrados.isEmpty() -> MensajeEstadoVacio(
                    "No se encontraron pacientes con ese criterio.",
                )

                else -> LazyColumn {
                    items(uiState.pacientesFiltrados, key = { it.uid }) { paciente ->
                        TarjetaPaciente(
                            paciente = paciente,
                            onClick = { onPacienteSeleccionado(paciente.uid) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MensajeEstadoVacio(mensaje: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = mensaje, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TarjetaPaciente(paciente: Usuario, onClick: () -> Unit) {
    Card(
        onClick = onClick,
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
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = obtenerIniciales(paciente.nombre),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = paciente.nombre, style = MaterialTheme.typography.titleMedium)
                Text(text = paciente.email, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun obtenerIniciales(nombre: String): String =
    nombre.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
