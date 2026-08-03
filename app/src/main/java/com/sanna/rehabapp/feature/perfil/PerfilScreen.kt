package com.sanna.rehabapp.feature.perfil

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.BotonOutline
import com.sanna.rehabapp.core.designsystem.BotonPrimario
import com.sanna.rehabapp.core.designsystem.CampoSoloLectura
import com.sanna.rehabapp.core.designsystem.CampoTexto
import com.sanna.rehabapp.core.designsystem.DialogoConfirmacion
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.EstadoError
import com.sanna.rehabapp.core.designsystem.SeccionFormulario
import com.sanna.rehabapp.core.designsystem.rememberSnackbarDeMensaje
import com.sanna.rehabapp.core.navigation.ItemBarraLateral
import com.sanna.rehabapp.core.navigation.ScaffoldConBarraLateral
import com.sanna.rehabapp.core.theme.Spacing
import com.sanna.rehabapp.domain.model.Rol

// HU22 — Perfil del paciente: sin barra lateral, igual que el resto de
// pantallas de detalle del lado paciente (solo botón atrás).
@Composable
fun PerfilPacienteScreen(
    onVolver: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: PerfilViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = rememberSnackbarDeMensaje(uiState.mensaje, viewModel::mensajeMostrado)

    Scaffold(
        topBar = { BarraSuperior(titulo = "Perfil", onNavegarAtras = onVolver) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        ContenidoPerfil(
            uiState = uiState,
            onNombreCambiado = viewModel::onNombreCambiado,
            onGuardarNombre = viewModel::guardarNombre,
            onCerrarSesion = {
                viewModel.cerrarSesion()
                onCerrarSesion()
            },
            modifier = Modifier.padding(padding),
        )
    }
}

// HU23 — Perfil del fisioterapeuta: es una pestaña más de la barra
// lateral, junto a Pacientes/Ejercicios/Resultados.
@Composable
fun PerfilFisioterapeutaScreen(
    menuVisible: Boolean,
    onCambiarMenuVisible: (Boolean) -> Unit,
    onNavegarAPacientes: () -> Unit,
    onNavegarAEjercicios: () -> Unit,
    onNavegarAResultados: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: PerfilViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = rememberSnackbarDeMensaje(uiState.mensaje, viewModel::mensajeMostrado)

    Box(modifier = Modifier.fillMaxSize()) {
        ScaffoldConBarraLateral(
            menuVisible = menuVisible,
            onCambiarMenuVisible = onCambiarMenuVisible,
            items = listOf(
                ItemBarraLateral("Pacientes", Icons.Filled.People, seleccionado = false, onClick = onNavegarAPacientes),
                ItemBarraLateral(
                    "Ejercicios",
                    Icons.Filled.FitnessCenter,
                    seleccionado = false,
                    onClick = onNavegarAEjercicios,
                ),
                ItemBarraLateral(
                    "Resultados",
                    Icons.Filled.Assessment,
                    seleccionado = false,
                    onClick = onNavegarAResultados,
                ),
                ItemBarraLateral("Perfil", Icons.Filled.Person, seleccionado = true, onClick = {}),
            ),
            topBar = { onAlternarMenu -> BarraSuperior(titulo = "Perfil", onAlternarMenu = onAlternarMenu) },
        ) { padding ->
            ContenidoPerfil(
                uiState = uiState,
                onNombreCambiado = viewModel::onNombreCambiado,
                onGuardarNombre = viewModel::guardarNombre,
                onCerrarSesion = {
                    viewModel.cerrarSesion()
                    onCerrarSesion()
                },
                modifier = Modifier.padding(padding),
            )
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ContenidoPerfil(
    uiState: PerfilUiState,
    onNombreCambiado: (String) -> Unit,
    onGuardarNombre: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmandoCierre by remember { mutableStateOf(false) }
    val usuario = uiState.usuario

    when {
        uiState.cargando -> EstadoCargando(modifier = modifier.fillMaxSize())

        usuario == null -> EstadoError(
            mensaje = "No se pudo cargar tu perfil.",
            modifier = modifier.fillMaxSize(),
        )

        else -> Column(
            modifier = modifier
                .fillMaxSize()
                .padding(Spacing.md)
                .verticalScroll(rememberScrollState()),
        ) {
            SeccionFormulario(titulo = "Datos de la cuenta") {
                CampoTexto(
                    valor = uiState.nombre,
                    onValorCambiado = onNombreCambiado,
                    etiqueta = "Nombre",
                )
                Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                CampoSoloLectura(etiqueta = "Correo electrónico", valor = usuario.email)
                Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                BotonPrimario(
                    texto = "Guardar cambios",
                    onClick = onGuardarNombre,
                    habilitado = uiState.nombre.isNotBlank() && !uiState.guardando,
                    cargando = uiState.guardando,
                )
            }

            if (usuario.rol == Rol.PACIENTE) {
                Spacer(modifier = Modifier.height(Spacing.md))
                SeccionFormulario(titulo = "Información clínica") {
                    CampoSoloLectura(etiqueta = "DNI", valor = usuario.dni ?: "No registrado")
                    Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                    CampoSoloLectura(etiqueta = "Edad", valor = usuario.edad?.toString() ?: "No registrada")
                    Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                    CampoSoloLectura(
                        etiqueta = "Diagnóstico(s)",
                        valor = if (usuario.diagnosticos.isEmpty()) {
                            "Sin diagnóstico registrado."
                        } else {
                            usuario.diagnosticos.joinToString { it.tipo.etiqueta }
                        },
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                    CampoSoloLectura(
                        etiqueta = "Fisioterapeuta asignado",
                        valor = uiState.nombreFisioterapeuta ?: "Sin asignar",
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
            BotonOutline(
                texto = "Cerrar sesión",
                onClick = { confirmandoCierre = true },
                esDestructivo = true,
                icono = Icons.AutoMirrored.Filled.Logout,
            )
        }
    }

    if (confirmandoCierre) {
        DialogoConfirmacion(
            titulo = "Cerrar sesión",
            mensaje = "¿Seguro que deseas cerrar sesión?",
            textoConfirmar = "Cerrar sesión",
            onConfirmar = {
                confirmandoCierre = false
                onCerrarSesion()
            },
            onCancelar = { confirmandoCierre = false },
        )
    }
}
