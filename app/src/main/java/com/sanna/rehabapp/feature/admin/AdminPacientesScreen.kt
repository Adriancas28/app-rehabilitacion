package com.sanna.rehabapp.feature.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.DialogoConfirmacion
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.EstadoVacio
import com.sanna.rehabapp.core.designsystem.TarjetaPersona
import com.sanna.rehabapp.core.navigation.CerrarSesionViewModel
import com.sanna.rehabapp.core.navigation.ItemBarraLateral
import com.sanna.rehabapp.core.navigation.ScaffoldConBarraLateral
import com.sanna.rehabapp.core.theme.Spacing
import com.sanna.rehabapp.domain.model.Usuario

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
    var confirmandoCierreSesion by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Confirmación visible (Snackbar) tras cualquier acción CRUD sobre un
    // paciente — antes eliminar/asignar no daban ninguna señal al fisio.
    LaunchedEffect(uiState.mensaje) {
        uiState.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.mensajeMostrado()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                ItemBarraLateral(
                    "Cerrar sesión",
                    Icons.AutoMirrored.Filled.Logout,
                    seleccionado = false,
                    onClick = { confirmandoCierreSesion = true },
                ),
            ),
            topBar = { onAlternarMenu ->
                BarraSuperior(
                    titulo = "Pacientes",
                    onAlternarMenu = onAlternarMenu,
                    acciones = {
                        IconButton(onClick = onRegistrarPaciente) {
                            Icon(Icons.Filled.Add, contentDescription = "Registrar paciente")
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(Spacing.md),
            ) {
                when {
                    uiState.cargando -> EstadoCargando()

                    uiState.pacientes.isEmpty() -> EstadoVacio(
                        icono = Icons.Filled.PersonOff,
                        mensaje = "Aún no hay pacientes registrados.",
                    )

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

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (confirmandoCierreSesion) {
        DialogoConfirmacion(
            titulo = "Cerrar sesión",
            mensaje = "¿Seguro que deseas cerrar sesión?",
            textoConfirmar = "Cerrar sesión",
            onConfirmar = {
                confirmandoCierreSesion = false
                cerrarSesionViewModel.cerrarSesion()
                onCerrarSesion()
            },
            onCancelar = { confirmandoCierreSesion = false },
        )
    }

    pacienteAEliminar?.let { paciente ->
        DialogoConfirmacion(
            titulo = "Eliminar paciente",
            mensaje = "¿Seguro que deseas eliminar la cuenta de \"${paciente.nombre}\"?",
            textoConfirmar = "Eliminar",
            onConfirmar = {
                viewModel.eliminar(paciente.uid, paciente.nombre)
                pacienteAEliminar = null
            },
            onCancelar = { pacienteAEliminar = null },
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
                                    viewModel.asignarFisioterapeuta(paciente.uid, fisio.uid, fisio.nombre)
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

// Compartida con AdminFisioterapeutasScreen — misma fila de avatar+nombre+
// email+línea extra opcional, con menú "⋮" de editar/eliminar.
@Composable
internal fun TarjetaUsuarioAdmin(
    usuario: Usuario,
    lineaExtra: (@Composable () -> Unit)? = null,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
) {
    var menuAbierto by remember { mutableStateOf(false) }

    TarjetaPersona(
        nombre = usuario.nombre,
        subtitulo = usuario.email,
        lineaExtra = lineaExtra,
        modifier = Modifier.padding(vertical = Spacing.xs),
        contenidoFinal = {
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
        },
    )
}
