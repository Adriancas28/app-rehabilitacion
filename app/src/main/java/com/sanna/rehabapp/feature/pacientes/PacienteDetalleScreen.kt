package com.sanna.rehabapp.feature.pacientes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.sanna.rehabapp.core.theme.AmbarAlertaContenedor
import com.sanna.rehabapp.core.theme.AmbarAlertaTexto
import com.sanna.rehabapp.core.theme.VerdeExitoContenedor
import com.sanna.rehabapp.core.theme.VerdeExitoTexto
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
            TopAppBar(
                title = { Text(uiState.paciente?.nombre ?: "Paciente") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
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
                .padding(16.dp),
        ) {
            uiState.paciente?.let { paciente ->
                Text(
                    text = paciente.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                TarjetaDiagnostico(
                    paciente = paciente,
                    onGuardar = viewModel::actualizarDiagnosticos,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // HU12-CA01/CA02: progreso general del paciente (% de sesiones
            // completadas + promedio de calidad de ejecución).
            TarjetaProgreso(
                completadas = uiState.sesionesCompletadas,
                total = uiState.sesionesFiltradas.size,
                porcentajePromedio = uiState.porcentajePromedio,
            )
            Spacer(modifier = Modifier.height(16.dp))

            // HU12-CA02 (ampliación): progreso total por cada ejercicio
            // realizado, con barras (mockup pantalla 10).
            if (uiState.progresoPorEjercicio.isNotEmpty()) {
                Text(text = "Progreso por ejercicio", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        uiState.progresoPorEjercicio.forEachIndexed { indice, progreso ->
                            BarraProgresoEjercicio(progreso)
                            if (indice != uiState.progresoPorEjercicio.lastIndex) {
                                Spacer(modifier = Modifier.height(14.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
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
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Sesiones registradas", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

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

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.MedicalInformation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
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
            Spacer(modifier = Modifier.height(8.dp))
            if (editando) {
                TipoDiagnostico.entries.groupBy { it.regionCorporal }.forEach { (region, diagnosticos) ->
                    Text(
                        text = region,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
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
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    TextButton(onClick = {
                        seleccionados = paciente.diagnosticos.map { it.tipo }.toSet()
                        editando = false
                    }) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
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
}

@Composable
private fun TarjetaProgreso(completadas: Int, total: Int, porcentajePromedio: Float) {
    val porcentaje = if (total == 0) 0f else completadas.toFloat() / total.toFloat()
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { porcentaje },
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 6.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    text = "${(porcentaje * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
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
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progreso.porcentajePromedio / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
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
    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.EventNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
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
            Spacer(modifier = Modifier.width(4.dp))
            EstadoPill(completada = sesion.estado == EstadoSesion.COMPLETADA)
        }
    }
}

@Composable
private fun EstadoPill(completada: Boolean) {
    val fondo = if (completada) VerdeExitoContenedor else AmbarAlertaContenedor
    val texto = if (completada) VerdeExitoTexto else AmbarAlertaTexto
    Box(
        modifier = Modifier
            .background(fondo, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = if (completada) "Completada" else "Pendiente",
            style = MaterialTheme.typography.labelSmall,
            color = texto,
        )
    }
}
