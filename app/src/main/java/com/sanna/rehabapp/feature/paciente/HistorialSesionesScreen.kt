package com.sanna.rehabapp.feature.paciente

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.sanna.rehabapp.core.designsystem.TarjetaConIcono
import com.sanna.rehabapp.core.designsystem.TarjetaEstadistica
import com.sanna.rehabapp.core.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistorialSesionesScreen(
    onVolver: () -> Unit,
    onSesionSeleccionada: (sesionId: String) -> Unit,
    viewModel: HistorialSesionesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { BarraSuperior(titulo = "Mi progreso", onNavegarAtras = onVolver) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.md),
        ) {
            // HU12-CA01/CA02: resumen agregado — % general, sesiones
            // completadas y racha de días consecutivos.
            Row(modifier = Modifier.fillMaxWidth()) {
                TarjetaEstadistica(
                    icono = Icons.Filled.TrendingUp,
                    valor = "${uiState.resumen.porcentajeGeneral.toInt()}%",
                    etiqueta = "Progreso general",
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(Spacing.sm + 2.dp))
                TarjetaEstadistica(
                    icono = Icons.Filled.TaskAlt,
                    valor = "${uiState.resumen.sesionesCompletadas}",
                    etiqueta = "Completadas",
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(Spacing.sm + 2.dp))
                TarjetaEstadistica(
                    icono = Icons.Filled.LocalFireDepartment,
                    valor = "${uiState.resumen.rachaActual}",
                    etiqueta = "Racha (días)",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(Spacing.md))

            // HU12-CA03: filtro por período.
            FilaChipsFiltro(
                opciones = PeriodoProgreso.entries,
                seleccionado = uiState.filtroPeriodo,
                etiquetaDeOpcion = { it.etiqueta },
                onSeleccionar = viewModel::onFiltroPeriodoCambiado,
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            when {
                uiState.cargando -> EstadoCargando()

                // HU13-CA04
                uiState.sesiones.isEmpty() -> EstadoVacio(
                    icono = Icons.Filled.History,
                    mensaje = if (uiState.filtroPeriodo == PeriodoProgreso.TODOS) {
                        "Aún no has completado ninguna sesión."
                    } else {
                        "No completaste sesiones en este período."
                    },
                )

                else -> LazyColumn {
                    items(uiState.sesiones, key = { it.sesionId }) { item ->
                        TarjetaConIcono(
                            icono = Icons.Filled.EventNote,
                            titulo = item.ejercicio.nombre,
                            subtitulo = item.sesion.fechaEjecucion?.let(::formatearFecha) ?: "Fecha no disponible",
                            onClick = { onSesionSeleccionada(item.sesionId) },
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
                            },
                            modifier = Modifier.padding(vertical = Spacing.xs),
                        )
                    }
                }
            }
        }
    }
}

private fun formatearFecha(fecha: Date): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fecha)
