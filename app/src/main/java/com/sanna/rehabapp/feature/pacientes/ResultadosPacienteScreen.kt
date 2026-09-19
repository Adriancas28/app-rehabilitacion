package com.sanna.rehabapp.feature.pacientes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.designsystem.BadgeEstado
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.EstadoVacio
import com.sanna.rehabapp.core.designsystem.FilaChipsFiltro
import com.sanna.rehabapp.core.designsystem.SelectorDropdown
import com.sanna.rehabapp.core.designsystem.TarjetaConIcono
import com.sanna.rehabapp.core.designsystem.TipoBadge
import com.sanna.rehabapp.core.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Todas las sesiones de un paciente (completas, incompletas y por hacer). Las
// completas e incompletas abren el detalle con gráficos y lista de
// repeticiones; una sesión por hacer todavía no tiene nada que mostrar.
@Composable
fun ResultadosPacienteScreen(
    onVolver: () -> Unit,
    onVerResultado: (pacienteId: String, sesionId: String) -> Unit,
    pacienteId: String,
    viewModel: ResultadosPacienteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = uiState.nombrePaciente.ifBlank { "Resultados" },
                onNavegarAtras = onVolver,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.md),
        ) {
            FilaChipsFiltro(
                opciones = PeriodoFiltro.entries,
                seleccionado = uiState.filtroPeriodo,
                etiquetaDeOpcion = { it.etiqueta },
                onSeleccionar = viewModel::onFiltroPeriodoCambiado,
            )
            Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))

            val ejercicioSeleccionado = uiState.ejerciciosDisponibles.find { it.id == uiState.filtroEjercicioId }
            SelectorDropdown(
                valorSeleccionado = ejercicioSeleccionado?.nombre ?: "Todos los ejercicios",
                opciones = listOf("Todos los ejercicios") + uiState.ejerciciosDisponibles.map { it.nombre },
                etiquetaDeOpcion = { it },
                onSeleccionar = { nombre ->
                    viewModel.onFiltroEjercicioCambiado(uiState.ejerciciosDisponibles.find { it.nombre == nombre }?.id)
                },
                placeholder = "Todos los ejercicios",
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            when {
                uiState.cargando -> EstadoCargando()

                uiState.filas.isEmpty() -> EstadoVacio(
                    icono = Icons.Filled.SearchOff,
                    mensaje = "Este paciente no tiene sesiones que cumplan el filtro.",
                )

                else -> LazyColumn {
                    items(uiState.filas, key = { it.sesion.id }) { fila ->
                        TarjetaSesion(
                            fila = fila,
                            onClick = if (fila.estado == EstadoFila.POR_HACER) {
                                null
                            } else {
                                { onVerResultado(pacienteId, fila.sesion.id) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaSesion(fila: FilaSesion, onClick: (() -> Unit)?) {
    val resultado = fila.sesion.resultado
    TarjetaConIcono(
        icono = Icons.AutoMirrored.Filled.EventNote,
        titulo = fila.ejercicio?.nombre ?: "Ejercicio eliminado",
        subtitulo = (fila.sesion.fechaEjecucion ?: fila.sesion.fechaAsignacion)?.let(::formatearFecha),
        onClick = onClick,
        contenidoFinal = {
            when (fila.estado) {
                EstadoFila.COMPLETADA -> BadgeEstado(texto = "Completada", tipo = TipoBadge.EXITO)
                EstadoFila.INCOMPLETA -> BadgeEstado(texto = "Incompleta", tipo = TipoBadge.ADVERTENCIA)
                EstadoFila.POR_HACER -> BadgeEstado(texto = "Por hacer", tipo = TipoBadge.NEUTRO)
            }
            if (onClick != null) {
                Spacer(modifier = Modifier.width(Spacing.xs))
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        contenidoInferior = resultado?.let {
            {
                Text(
                    text = "${it.repeticionesCompletadas}/${it.repeticionesAsignadas} repeticiones · " +
                        "Ejecución ${it.porcentajeEjecucion.toInt()}%",
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
