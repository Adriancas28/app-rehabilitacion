package com.sanna.rehabapp.feature.pacientes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.designsystem.BarraBusqueda
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.EstadoVacio
import com.sanna.rehabapp.core.designsystem.TarjetaEstadistica
import com.sanna.rehabapp.core.designsystem.TarjetaPersona
import com.sanna.rehabapp.core.navigation.CerrarSesionViewModel
import com.sanna.rehabapp.core.navigation.ItemBarraLateral
import com.sanna.rehabapp.core.navigation.ScaffoldConBarraLateral
import com.sanna.rehabapp.core.theme.Spacing
import com.sanna.rehabapp.domain.model.Usuario

// Dashboard del fisioterapeuta (mockup pantalla 2) + Gestión de Pacientes
// (HU01, mockup pantalla 3): una sola pantalla, no dos — el resumen de
// arriba y la lista de pacientes conviven aquí, decisión ya documentada
// en CLAUDE.md (no se construye un dashboard aparte sin datos reales).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacientesListScreen(
    menuVisible: Boolean,
    onCambiarMenuVisible: (Boolean) -> Unit,
    onPacienteSeleccionado: (String) -> Unit,
    onNavegarAEjercicios: () -> Unit,
    onNavegarAResultados: () -> Unit,
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
            ItemBarraLateral(
                "Resultados",
                Icons.Filled.Assessment,
                seleccionado = false,
                onClick = onNavegarAResultados,
            ),
        ),
        topBar = { onAlternarMenu ->
            BarraSuperior(
                titulo = "Pacientes",
                onAlternarMenu = onAlternarMenu,
                acciones = {
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
                .padding(Spacing.md),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                TarjetaEstadistica(
                    icono = Icons.Filled.People,
                    valor = uiState.totalPacientes.toString(),
                    etiqueta = "Pacientes activos",
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                TarjetaEstadistica(
                    icono = Icons.Filled.CalendarMonth,
                    valor = uiState.sesionesHoy.toString(),
                    etiqueta = "Sesiones hoy",
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                TarjetaEstadistica(
                    icono = Icons.Filled.FitnessCenter,
                    valor = uiState.totalEjercicios.toString(),
                    etiqueta = "Ejercicios",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(Spacing.md))

            BarraBusqueda(
                valor = uiState.consultaBusqueda,
                onValorCambiado = viewModel::onConsultaCambiada,
                placeholder = "Buscar paciente...",
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            when {
                uiState.cargando -> EstadoCargando()

                uiState.totalPacientes == 0 -> EstadoVacio(
                    icono = Icons.Filled.PersonOff,
                    mensaje = "Aún no tienes pacientes asignados.",
                )

                uiState.pacientesFiltrados.isEmpty() -> EstadoVacio(
                    icono = Icons.Filled.PersonOff,
                    mensaje = "No se encontraron pacientes con ese criterio.",
                )

                else -> LazyColumn {
                    items(uiState.pacientesFiltrados, key = { it.uid }) { paciente ->
                        TarjetaPersona(
                            nombre = paciente.nombre,
                            subtitulo = etiquetaDiagnosticos(paciente) ?: paciente.email,
                            onClick = { onPacienteSeleccionado(paciente.uid) },
                            modifier = Modifier.padding(vertical = 4.dp),
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

// HU01-CA06 (ampliación): un paciente puede tener varios diagnósticos; se
// muestra el primero y, si hay más, cuántos adicionales.
private fun etiquetaDiagnosticos(paciente: Usuario): String? {
    val diagnosticos = paciente.diagnosticos
    if (diagnosticos.isEmpty()) return null
    val primero = diagnosticos.first().tipo.etiqueta
    return if (diagnosticos.size == 1) primero else "$primero +${diagnosticos.size - 1} más"
}
