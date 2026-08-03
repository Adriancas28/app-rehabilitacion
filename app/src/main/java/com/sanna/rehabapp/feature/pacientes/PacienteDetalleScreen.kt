package com.sanna.rehabapp.feature.pacientes

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.sanna.rehabapp.core.designsystem.BadgeEstado
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.ProgresoCircular
import com.sanna.rehabapp.core.designsystem.ProgresoLineal
import com.sanna.rehabapp.core.designsystem.TarjetaBase
import com.sanna.rehabapp.core.designsystem.TipoBadge
import com.sanna.rehabapp.core.theme.Spacing
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.model.EstadoSesion
import com.sanna.rehabapp.domain.model.Sesion
import com.sanna.rehabapp.domain.model.TipoDiagnostico
import com.sanna.rehabapp.domain.model.Usuario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacienteDetalleScreen(
    onVolver: () -> Unit,
    onAsignarSesion: (pacienteId: String) -> Unit,
    onEditarSesion: (pacienteId: String, sesionId: String) -> Unit,
    // HU18-CA02: ver el detalle de una sesión ya completada.
    onVerResultado: (pacienteId: String, sesionId: String) -> Unit,
    // HU15 (acceso rápido): registrar una recomendación sin pasar primero
    // por el detalle de la sesión.
    onRecomendar: (pacienteId: String, sesionId: String) -> Unit,
    viewModel: PacienteDetalleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = uiState.paciente?.nombre ?: "Paciente",
                onNavegarAtras = onVolver,
                acciones = {
                    IconButton(onClick = { onAsignarSesion(viewModel.pacienteId) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Asignar sesión")
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
            uiState.paciente?.let { paciente ->
                Text(
                    text = paciente.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                TarjetaDiagnostico(
                    paciente = paciente,
                    onGuardar = viewModel::actualizarDiagnosticos,
                )
            }
            Spacer(modifier = Modifier.height(Spacing.md))

            // HU12-CA01/CA02: progreso general del paciente (% de sesiones
            // completadas + promedio de calidad de ejecución).
            TarjetaProgreso(
                completadas = uiState.sesionesCompletadas,
                total = uiState.sesionesFiltradas.size,
                porcentajePromedio = uiState.porcentajePromedio,
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            // HU12-CA02 (ampliación): progreso total por cada ejercicio
            // realizado, con barras (mockup pantalla 10).
            if (uiState.progresoPorEjercicio.isNotEmpty()) {
                Text(text = "Progreso por ejercicio", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(Spacing.sm))
                TarjetaBase {
                    uiState.progresoPorEjercicio.forEachIndexed { indice, progreso ->
                        BarraProgresoEjercicio(progreso)
                        if (indice != uiState.progresoPorEjercicio.lastIndex) {
                            Spacer(modifier = Modifier.height(Spacing.md - Spacing.xs))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.md))
            }

            // HU12-CA03: filtro por período — afecta tanto el resumen de
            // arriba como la lista de sesiones de abajo.
            Row {
                PeriodoFiltro.entries.forEach { periodo ->
                    FilterChip(
                        selected = uiState.filtroPeriodo == periodo,
                        onClick = { viewModel.onFiltroPeriodoCambiado(periodo) },
                        label = { Text(periodo.etiqueta) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))

            Text(text = "Sesiones registradas", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(Spacing.sm))

            if (uiState.sesionesFiltradas.isEmpty()) {
                Text(
                    text = if (uiState.filtroPeriodo == PeriodoFiltro.TODOS) {
                        "Este paciente aún no tiene sesiones registradas."
                    } else {
                        "No hay sesiones en este período."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn {
                    items(uiState.sesionesFiltradas, key = { it.id }) { sesion ->
                        TarjetaSesion(
                            sesion = sesion,
                            ejercicio = uiState.ejerciciosPorId[sesion.ejercicioId],
                            onClick = {
                                if (sesion.estado == EstadoSesion.PENDIENTE) {
                                    onEditarSesion(viewModel.pacienteId, sesion.id)
                                } else {
                                    onVerResultado(viewModel.pacienteId, sesion.id)
                                }
                            },
                            onRecomendar = { onRecomendar(viewModel.pacienteId, sesion.id) },
                        )
                    }
                }
            }
        }
    }
}

// HU01-CA06 (ampliación) — el fisioterapeuta elige uno o más diagnósticos
// del paciente de un catálogo cerrado (TipoDiagnostico), no texto libre.
@Composable
private fun TarjetaDiagnostico(paciente: Usuario, onGuardar: (List<TipoDiagnostico>) -> Unit) {
    var editando by remember { mutableStateOf(false) }
    var seleccionados by remember(paciente.diagnosticos) {
        mutableStateOf(paciente.diagnosticos.map { it.tipo }.toSet())
    }

    TarjetaBase {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.MedicalInformation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = "Diagnóstico(s)",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            if (!editando) {
                IconButton(onClick = { editando = true }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar diagnóstico")
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        if (editando) {
            TipoDiagnostico.entries.groupBy { it.regionCorporal }.forEach { (region, diagnosticos) ->
                Text(
                    text = region,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Spacing.sm, bottom = 2.dp),
                )
                diagnosticos.forEach { tipo ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                seleccionados = if (tipo in seleccionados) {
                                    seleccionados - tipo
                                } else {
                                    seleccionados + tipo
                                }
                            },
                    ) {
                        Checkbox(
                            checked = tipo in seleccionados,
                            onCheckedChange = {
                                seleccionados = if (tipo in seleccionados) {
                                    seleccionados - tipo
                                } else {
                                    seleccionados + tipo
                                }
                            },
                        )
                        Text(text = tipo.etiqueta, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            Row {
                TextButton(onClick = {
                    seleccionados = paciente.diagnosticos.map { it.tipo }.toSet()
                    editando = false
                }) { Text("Cancelar") }
                Spacer(modifier = Modifier.width(Spacing.sm))
                TextButton(
                    enabled = seleccionados.isNotEmpty(),
                    onClick = {
                        onGuardar(seleccionados.toList())
                        editando = false
                    },
                ) { Text("Guardar") }
            }
        } else if (paciente.diagnosticos.isEmpty()) {
            Text(
                text = "Sin diagnóstico registrado.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = paciente.diagnosticos.joinToString { it.tipo.etiqueta },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TarjetaProgreso(completadas: Int, total: Int, porcentajePromedio: Float) {
    val porcentaje = if (total == 0) 0f else completadas.toFloat() / total.toFloat()
    TarjetaBase(relleno = Spacing.lg - Spacing.xs) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgresoCircular(porcentaje = porcentaje)
            Spacer(modifier = Modifier.width(Spacing.md))
            Column {
                Text(text = "Progreso", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "$completadas de $total sesiones completadas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // HU12-CA01/CA02: promedio de calidad de ejecución, distinto
                // del % de arriba (que es tasa de cumplimiento, no calidad).
                Text(
                    text = "Progreso general: ${porcentajePromedio.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BarraProgresoEjercicio(progreso: ProgresoEjercicio) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = progreso.nombre,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${progreso.porcentajePromedio.toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(Spacing.xs + 2.dp))
        ProgresoLineal(porcentaje = progreso.porcentajePromedio / 100f)
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = if (progreso.sesionesCompletadas == 1) {
                "1 sesión completada"
            } else {
                "${progreso.sesionesCompletadas} sesiones completadas"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TarjetaSesion(
    sesion: Sesion,
    ejercicio: Ejercicio?,
    onClick: () -> Unit,
    onRecomendar: () -> Unit,
) {
    TarjetaBase(
        onClick = onClick,
        relleno = Spacing.sm + 6.dp,
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.EventNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(Spacing.sm + 4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ejercicio?.nombre ?: "Ejercicio eliminado",
                    style = MaterialTheme.typography.bodyMedium,
                )
                sesion.resultado?.let { resultado ->
                    Text(
                        text = "Ejecución: ${resultado.porcentajeEjecucion.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (sesion.estado == EstadoSesion.COMPLETADA) {
                IconButton(onClick = onRecomendar) {
                    Icon(
                        Icons.Filled.RateReview,
                        contentDescription = "Registrar recomendación",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.width(Spacing.xs))
            BadgeEstado(
                texto = if (sesion.estado == EstadoSesion.COMPLETADA) "Completada" else "Pendiente",
                tipo = if (sesion.estado == EstadoSesion.COMPLETADA) TipoBadge.EXITO else TipoBadge.ADVERTENCIA,
            )
        }
    }
}
