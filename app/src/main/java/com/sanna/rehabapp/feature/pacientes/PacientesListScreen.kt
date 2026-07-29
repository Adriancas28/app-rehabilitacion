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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.navigation.CerrarSesionViewModel
import com.sanna.rehabapp.core.navigation.ItemBarraLateral
import com.sanna.rehabapp.core.navigation.ScaffoldConBarraLateral
import com.sanna.rehabapp.domain.model.Usuario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacientesListScreen(
    menuVisible: Boolean,
    onCambiarMenuVisible: (Boolean) -> Unit,
    onPacienteSeleccionado: (String) -> Unit,
    onNavegarAEjercicios: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: PacientesViewModel = hiltViewModel(),
    cerrarSesionViewModel: CerrarSesionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    ScaffoldConBarraLateral(
        menuVisible = menuVisible,
        onCambiarMenuVisible = onCambiarMenuVisible,
        items = listOf(
            ItemBarraLateral("Pacientes", Icons.Filled.People, seleccionado = true, onClick = {}),
            ItemBarraLateral(
                "Ejercicios",
                Icons.Filled.FitnessCenter,
                seleccionado = false,
                onClick = onNavegarAEjercicios,
            ),
        ),
        topBar = { onAlternarMenu ->
            TopAppBar(
                title = { Text("Pacientes") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                navigationIcon = {
                    IconButton(onClick = onAlternarMenu) {
                        Icon(Icons.Filled.Menu, contentDescription = "Mostrar/ocultar menú")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        cerrarSesionViewModel.cerrarSesion()
                        onCerrarSesion()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión")
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
            Row(modifier = Modifier.fillMaxWidth()) {
                TarjetaEstadistica(
                    icono = Icons.Filled.People,
                    valor = uiState.totalPacientes,
                    etiqueta = "Pacientes activos",
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(10.dp))
                TarjetaEstadistica(
                    icono = Icons.Filled.CalendarMonth,
                    valor = uiState.sesionesHoy,
                    etiqueta = "Sesiones hoy",
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(10.dp))
                TarjetaEstadistica(
                    icono = Icons.Filled.FitnessCenter,
                    valor = uiState.totalEjercicios,
                    etiqueta = "Ejercicios",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.consultaBusqueda,
                onValueChange = viewModel::onConsultaCambiada,
                placeholder = { Text("Buscar paciente...") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = { Icon(Icons.Filled.FilterList, contentDescription = null) },
                shape = MaterialTheme.shapes.medium,
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
private fun TarjetaEstadistica(icono: ImageVector, valor: Int, etiqueta: String, modifier: Modifier = Modifier) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = valor.toString(), style = MaterialTheme.typography.titleLarge)
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MensajeEstadoVacio(mensaje: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.PersonOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TarjetaPaciente(paciente: Usuario, onClick: () -> Unit) {
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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = obtenerIniciales(paciente.nombre),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = paciente.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = paciente.tipoDiagnostico?.etiqueta ?: paciente.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
