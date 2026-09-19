package com.sanna.rehabapp.feature.pacientes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.EstadoVacio
import com.sanna.rehabapp.core.designsystem.TarjetaPersona
import com.sanna.rehabapp.core.navigation.ItemBarraLateral
import com.sanna.rehabapp.core.navigation.ScaffoldConBarraLateral
import com.sanna.rehabapp.core.theme.Spacing

// HU18 — "Resultados": lista de los pacientes del fisioterapeuta. Al
// seleccionar uno se abren todas sus sesiones (ResultadosPacienteScreen).
@Composable
fun ResultadosScreen(
    menuVisible: Boolean,
    onCambiarMenuVisible: (Boolean) -> Unit,
    onNavegarAPacientes: () -> Unit,
    onNavegarAEjercicios: () -> Unit,
    onNavegarAPerfil: () -> Unit,
    onSeleccionarPaciente: (pacienteId: String) -> Unit,
    viewModel: ResultadosViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

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
            ItemBarraLateral("Resultados", Icons.Filled.Assessment, seleccionado = true, onClick = {}),
            ItemBarraLateral("Perfil", Icons.Filled.Person, seleccionado = false, onClick = onNavegarAPerfil),
        ),
        topBar = { onAlternarMenu -> BarraSuperior(titulo = "Resultados", onAlternarMenu = onAlternarMenu) },
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
                    mensaje = "Aún no tienes pacientes asignados.",
                )

                else -> LazyColumn {
                    items(uiState.pacientes, key = { it.uid }) { paciente ->
                        TarjetaPersona(
                            nombre = paciente.nombre,
                            subtitulo = paciente.diagnosticos.firstOrNull()?.tipo?.etiqueta ?: paciente.email,
                            onClick = { onSeleccionarPaciente(paciente.uid) },
                            modifier = Modifier.padding(vertical = Spacing.xs),
                            contenidoFinal = {
                                Icon(
                                    Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
