package com.sanna.rehabapp.feature.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

// Etapa 2A (dashboard Admin) — punto de entrada: lista de pacientes (mismo
// patrón visual que AdminPacientesScreen). Al seleccionar uno se abre su
// dashboard de progreso (AdminPacienteDashboardScreen).
@Composable
fun AdminDashboardScreen(
    menuVisible: Boolean,
    onCambiarMenuVisible: (Boolean) -> Unit,
    onSeleccionarPaciente: (String, String) -> Unit,
    onNavegarAPacientes: () -> Unit,
    onNavegarAFisioterapeutas: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: AdminDashboardViewModel = hiltViewModel(),
    cerrarSesionViewModel: CerrarSesionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var confirmandoCierreSesion by remember { mutableStateOf(false) }

    ScaffoldConBarraLateral(
        menuVisible = menuVisible,
        onCambiarMenuVisible = onCambiarMenuVisible,
        items = listOf(
            ItemBarraLateral("Dashboard", Icons.Rounded.Dashboard, seleccionado = true, onClick = {}),
            ItemBarraLateral(
                "Pacientes",
                Icons.Rounded.People,
                seleccionado = false,
                onClick = onNavegarAPacientes,
            ),
            ItemBarraLateral(
                "Fisioterapeutas",
                Icons.Rounded.MedicalServices,
                seleccionado = false,
                onClick = onNavegarAFisioterapeutas,
            ),
            ItemBarraLateral(
                "Cerrar sesión",
                Icons.AutoMirrored.Rounded.Logout,
                seleccionado = false,
                onClick = { confirmandoCierreSesion = true },
            ),
        ),
        topBar = { onAlternarMenu ->
            BarraSuperior(titulo = "Dashboard", onAlternarMenu = onAlternarMenu)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.md),
        ) {
            Text(
                text = "Selecciona un paciente para ver su dashboard de progreso.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.sm),
            )
            when {
                uiState.cargando -> EstadoCargando()
                uiState.pacientes.isEmpty() -> EstadoVacio(
                    icono = Icons.Rounded.PersonOff,
                    mensaje = "Aún no hay pacientes registrados.",
                )
                else -> LazyColumn {
                    items(uiState.pacientes, key = { it.uid }) { paciente ->
                        val diagnostico = paciente.diagnosticos.firstOrNull()?.tipo?.etiqueta
                        TarjetaPersona(
                            nombre = paciente.nombre,
                            subtitulo = listOfNotNull(diagnostico, if (paciente.activo) "Activo" else "Inactivo")
                                .joinToString(" · "),
                            onClick = { onSeleccionarPaciente(paciente.uid, paciente.nombre) },
                            modifier = Modifier.padding(vertical = Spacing.xs),
                        )
                    }
                }
            }
        }
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
}
