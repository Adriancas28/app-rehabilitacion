package com.sanna.rehabapp.feature.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.sanna.rehabapp.core.navigation.CerrarSesionViewModel
import com.sanna.rehabapp.core.navigation.ItemBarraLateral
import com.sanna.rehabapp.core.navigation.ScaffoldConBarraLateral
import com.sanna.rehabapp.core.theme.Spacing
import com.sanna.rehabapp.domain.model.Usuario

@Composable
fun AdminFisioterapeutasScreen(
    menuVisible: Boolean,
    onCambiarMenuVisible: (Boolean) -> Unit,
    onRegistrarFisioterapeuta: () -> Unit,
    onEditarFisioterapeuta: (String) -> Unit,
    onNavegarAPacientes: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: AdminFisioterapeutasViewModel = hiltViewModel(),
    cerrarSesionViewModel: CerrarSesionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var fisioAEliminar by remember { mutableStateOf<Usuario?>(null) }
    var confirmandoCierreSesion by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Confirmación visible (Snackbar) tras eliminar un fisioterapeuta —
    // antes no daba ninguna señal al administrador.
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
                ItemBarraLateral(
                    "Pacientes",
                    Icons.Filled.People,
                    seleccionado = false,
                    onClick = onNavegarAPacientes,
                ),
                ItemBarraLateral(
                    "Fisioterapeutas",
                    Icons.Filled.MedicalServices,
                    seleccionado = true,
                    onClick = {},
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
                    titulo = "Fisioterapeutas",
                    onAlternarMenu = onAlternarMenu,
                    acciones = {
                        IconButton(onClick = onRegistrarFisioterapeuta) {
                            Icon(Icons.Filled.Add, contentDescription = "Registrar fisioterapeuta")
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

                    uiState.fisioterapeutas.isEmpty() -> EstadoVacio(
                        icono = Icons.Filled.PersonOff,
                        mensaje = "Aún no hay fisioterapeutas registrados.",
                    )

                    else -> LazyColumn {
                        items(uiState.fisioterapeutas, key = { it.uid }) { fisio ->
                            val cantidadPacientes = uiState.pacientesPorFisioterapeuta[fisio.uid] ?: 0
                            TarjetaUsuarioAdmin(
                                usuario = fisio,
                                lineaExtra = {
                                    Text(
                                        text = if (cantidadPacientes == 1) {
                                            "1 paciente asignado"
                                        } else {
                                            "$cantidadPacientes pacientes asignados"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                onEditar = { onEditarFisioterapeuta(fisio.uid) },
                                onEliminar = { fisioAEliminar = fisio },
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

    fisioAEliminar?.let { fisio ->
        DialogoConfirmacion(
            titulo = "Eliminar fisioterapeuta",
            mensaje = "¿Seguro que deseas eliminar la cuenta de \"${fisio.nombre}\"?",
            textoConfirmar = "Eliminar",
            onConfirmar = {
                viewModel.eliminar(fisio.uid, fisio.nombre)
                fisioAEliminar = null
            },
            onCancelar = { fisioAEliminar = null },
        )
    }
}
