package com.sanna.rehabapp.feature.admin

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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonOff
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.navigation.CerrarSesionViewModel
import com.sanna.rehabapp.core.navigation.ItemBarraLateral
import com.sanna.rehabapp.core.navigation.ScaffoldConBarraLateral
import com.sanna.rehabapp.domain.model.Usuario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPacientesScreen(
    menuVisible: Boolean,
    onCambiarMenuVisible: (Boolean) -> Unit,
    onRegistrarPaciente: () -> Unit,
    onEditarPaciente: (String) -> Unit,
    onNavegarAFisioterapeutas: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: AdminPacientesViewModel = hiltViewModel(),
    cerrarSesionViewModel: CerrarSesionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var pacienteAEliminar by remember { mutableStateOf<Usuario?>(null) }
    var pacienteAAsignar by remember { mutableStateOf<Usuario?>(null) }

    ScaffoldConBarraLateral(
        menuVisible = menuVisible,
        onCambiarMenuVisible = onCambiarMenuVisible,
        items = listOf(
            ItemBarraLateral("Pacientes", Icons.Filled.People, seleccionado = true, onClick = {}),
            ItemBarraLateral(
                "Fisioterapeutas",
                Icons.Filled.MedicalServices,
                seleccionado = false,
                onClick = onNavegarAFisioterapeutas,
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
                    IconButton(onClick = onRegistrarPaciente) {
                        Icon(Icons.Filled.Add, contentDescription = "Registrar paciente")
                    }
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
            when {
                uiState.cargando -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                uiState.pacientes.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.PersonOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            text = "Aún no hay pacientes registrados.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }

                else -> LazyColumn {
                    items(uiState.pacientes, key = { it.uid }) { paciente ->
                        val fisioAsignado = paciente.fisioterapeutaId?.let { fid ->
                            uiState.fisioterapeutas.find { it.uid == fid }
                        }
                        TarjetaUsuarioAdmin(
                            usuario = paciente,
                            lineaExtra = {
                                if (paciente.fisioterapeutaId == null) {
                                    TextButton(onClick = { pacienteAAsignar = paciente }) {
                                        Text("Asignar fisioterapeuta")
                                    }
                                } else {
                                    Text(
                                        text = "Fisioterapeuta: ${fisioAsignado?.nombre ?: "—"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            onEditar = { onEditarPaciente(paciente.uid) },
                            onEliminar = { pacienteAEliminar = paciente },
                        )
                    }
                }
            }
        }
    }

    pacienteAEliminar?.let { paciente ->
        AlertDialog(
            onDismissRequest = { pacienteAEliminar = null },
            title = { Text("Eliminar paciente") },
            text = { Text("¿Seguro que deseas eliminar la cuenta de \"${paciente.nombre}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminar(paciente.uid)
                    pacienteAEliminar = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { pacienteAEliminar = null }) { Text("Cancelar") }
            },
        )
    }

    pacienteAAsignar?.let { paciente ->
        AlertDialog(
            onDismissRequest = { pacienteAAsignar = null },
            title = { Text("Asignar fisioterapeuta") },
            text = {
                if (uiState.fisioterapeutas.isEmpty()) {
                    Text("No hay fisioterapeutas registrados todavía.")
                } else {
                    Column {
                        uiState.fisioterapeutas.forEach { fisio ->
                            TextButton(
                                onClick = {
                                    viewModel.asignarFisioterapeuta(paciente.uid, fisio.uid)
                                    pacienteAAsignar = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(fisio.nombre, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pacienteAAsignar = null }) { Text("Cerrar") }
            },
        )
    }
}

@Composable
internal fun TarjetaUsuarioAdmin(
    usuario: Usuario,
    lineaExtra: (@Composable () -> Unit)? = null,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
) {
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
                Text(
                    text = obtenerIniciales(usuario.nombre),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = usuario.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = usuario.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                lineaExtra?.invoke()
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

private fun obtenerIniciales(nombre: String): String =
    nombre.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
