package com.sanna.rehabapp.feature.pacientes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.EstadoVacio
import com.sanna.rehabapp.core.designsystem.FilaChipsFiltro
import com.sanna.rehabapp.core.designsystem.SelectorDropdown
import com.sanna.rehabapp.core.designsystem.TarjetaConIcono
import com.sanna.rehabapp.core.navigation.ItemBarraLateral
import com.sanna.rehabapp.core.navigation.ScaffoldConBarraLateral
import com.sanna.rehabapp.core.theme.Spacing
import com.sanna.rehabapp.domain.model.Ejercicio
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultadosScreen(
    menuVisible: Boolean,
    onCambiarMenuVisible: (Boolean) -> Unit,
    onNavegarAPacientes: () -> Unit,
    onNavegarAEjercicios: () -> Unit,
    onNavegarAPerfil: () -> Unit,
    onVerResultado: (pacienteId: String, sesionId: String) -> Unit,
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
            // HU18-CA03: filtro por período.
            FilaChipsFiltro(
                opciones = PeriodoFiltro.entries,
                seleccionado = uiState.filtroPeriodo,
                etiquetaDeOpcion = { it.etiqueta },
                onSeleccionar = viewModel::onFiltroPeriodoCambiado,
            )
            Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))

            // HU18-CA03: filtro por ejercicio.
            val ejercicioSeleccionado = uiState.ejerciciosDisponibles.find { it.id == uiState.filtroEjercicioId }
            SelectorDropdown(
                valorSeleccionado = ejercicioSeleccionado,
                opciones = uiState.ejerciciosDisponibles,
                etiquetaDeOpcion = { it.nombre },
                onSeleccionar = { viewModel.onFiltroEjercicioCambiado(it.id) },
                placeholder = "Todos los ejercicios",
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            when {
                uiState.cargando -> EstadoCargando()

                uiState.sesiones.isEmpty() -> EstadoVacio(
                    icono = Icons.Filled.SearchOff,
                    mensaje = "No hay sesiones completadas que cumplan el filtro.",
                )

                else -> LazyColumn {
                    items(
                        uiState.sesiones,
                        key = { "${it.sesion.pacienteId}-${it.sesion.id}" },
                    ) { item ->
                        TarjetaResultado(
                            item = item,
                            onClick = {
                                item.sesion.pacienteId?.let { pacienteId ->
                                    onVerResultado(pacienteId, item.sesion.id)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaResultado(item: SesionConDetalle, onClick: () -> Unit) {
    TarjetaConIcono(
        icono = Icons.AutoMirrored.Filled.EventNote,
        titulo = item.ejercicio?.nombre ?: "Ejercicio eliminado",
        subtitulo = item.nombrePaciente,
        onClick = onClick,
        contenidoFinal = {
            item.sesion.resultado?.let { resultado ->
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small)
                        .padding(horizontal = Spacing.sm + 2.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "${resultado.porcentajeEjecucion.toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(modifier = Modifier.width(Spacing.sm))
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        contenidoInferior = item.sesion.fechaAsignacion?.let { fecha ->
            {
                Text(
                    text = formatearFecha(fecha),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        modifier = Modifier.padding(vertical = Spacing.xs),
    )
}

private fun formatearFecha(fecha: Date): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fecha)
