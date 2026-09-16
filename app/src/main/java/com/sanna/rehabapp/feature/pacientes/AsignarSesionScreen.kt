package com.sanna.rehabapp.feature.pacientes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.BotonPrimario
import com.sanna.rehabapp.core.designsystem.BotonSelectorFecha
import com.sanna.rehabapp.core.designsystem.CampoTexto
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.SeccionFormulario
import com.sanna.rehabapp.core.designsystem.SelectorDropdown
import com.sanna.rehabapp.core.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AsignarSesionScreen(
    onGuardado: () -> Unit,
    onVolver: () -> Unit,
    viewModel: AsignarSesionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.guardadoExitoso) {
        if (uiState.guardadoExitoso) onGuardado()
    }

    val ejercicioSeleccionado = uiState.ejercicios.find { it.id == uiState.ejercicioSeleccionadoId }

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = if (viewModel.esEdicion) "Editar sesión" else "Asignar sesión",
                onNavegarAtras = onVolver,
            )
        },
    ) { padding ->
        if (uiState.cargando) {
            EstadoCargando(modifier = Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.md)
                .verticalScroll(rememberScrollState()),
        ) {
            SeccionFormulario(titulo = "Ejercicio") {
                SelectorDropdown(
                    valorSeleccionado = ejercicioSeleccionado,
                    opciones = uiState.ejerciciosOrdenados,
                    etiquetaDeOpcion = { if (uiState.esSugerido(it)) "★ ${it.nombre}" else it.nombre },
                    onSeleccionar = { viewModel.onEjercicioSeleccionado(it.id) },
                    placeholder = "Selecciona un ejercicio",
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            SeccionFormulario(titulo = "Fecha de asignación") {
                BotonSelectorFecha(
                    fecha = uiState.fechaAsignacion,
                    onFechaSeleccionada = viewModel::onFechaSeleccionada,
                    placeholder = "Seleccionar fecha",
                    formatear = ::formatearFecha,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            SeccionFormulario(titulo = "Repeticiones") {
                SelectorDropdown(
                    valorSeleccionado = uiState.repeticiones,
                    opciones = OPCIONES_REPETICIONES,
                    etiquetaDeOpcion = { "$it repeticiones" },
                    onSeleccionar = viewModel::onRepeticionesCambiadas,
                    placeholder = "Selecciona un ejercicio primero",
                    habilitado = ejercicioSeleccionado != null,
                )

                Spacer(modifier = Modifier.height(Spacing.sm + 2.dp))

                // HU03-CA06 (ampliacion): override de la duracion por
                // repeticion solo para esta sesion -- se precarga con el
                // valor por defecto del ejercicio (ej. 10s), pero el
                // fisioterapeuta puede cambiarla (ej. 30s) sin alterar el
                // ejercicio ni otras sesiones.
                CampoTexto(
                    valor = uiState.duracionSegundosTexto,
                    onValorCambiado = viewModel::onDuracionSegundosCambiada,
                    etiqueta = "Duración por repetición (s)",
                    tipoTeclado = KeyboardType.Number,
                    habilitado = ejercicioSeleccionado != null,
                )

                val repeticionesSeleccionadas = uiState.repeticiones
                val duracionSeleccionada = uiState.duracionSegundosTexto.trim().toIntOrNull()
                if (ejercicioSeleccionado != null && repeticionesSeleccionadas != null) {
                    Spacer(modifier = Modifier.height(Spacing.sm + 2.dp))
                    FilaDuracionEstimada(
                        duracionPorRepeticion = duracionSeleccionada ?: ejercicioSeleccionado.duracionSegundos,
                        repeticiones = repeticionesSeleccionadas,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // HU03 (ampliación, Etapa 4): personalizar el ángulo objetivo
            // (min/max) solo para esta sesión/paciente -- ej. un paciente en
            // primera sesión puede necesitar un rango más permisivo que el
            // por defecto del ejercicio, sin alterarlo para otros pacientes.
            SeccionFormulario(titulo = "Ángulo objetivo") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = uiState.personalizarAngulo,
                        onCheckedChange = viewModel::onPersonalizarAnguloCambiado,
                        enabled = ejercicioSeleccionado != null,
                    )
                    Text(
                        text = "Personalizar ángulo objetivo para este paciente",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (uiState.personalizarAngulo) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        CampoTexto(
                            valor = uiState.anguloMinTexto,
                            onValorCambiado = viewModel::onAnguloMinCambiado,
                            etiqueta = "Mínimo (°)",
                            tipoTeclado = KeyboardType.Number,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        CampoTexto(
                            valor = uiState.anguloMaxTexto,
                            onValorCambiado = viewModel::onAnguloMaxCambiado,
                            etiqueta = "Máximo (°)",
                            tipoTeclado = KeyboardType.Number,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            SeccionFormulario(titulo = "Notas (opcional)") {
                CampoTexto(
                    valor = uiState.notas,
                    onValorCambiado = viewModel::onNotasCambiadas,
                    etiqueta = "Indicaciones adicionales",
                    soloUnaLinea = false,
                    lineasMinimas = 2,
                )
            }

            uiState.error?.let { mensaje ->
                Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                Text(text = mensaje, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
            BotonPrimario(
                texto = if (viewModel.esEdicion) "Guardar cambios" else "Asignar sesión",
                onClick = viewModel::guardar,
                habilitado = !uiState.guardando,
                cargando = uiState.guardando,
            )
        }
    }
}

// HU03-CA06: duración total estimada (repeticiones × duración por
// repetición), solo informativa — no se guarda, se recalcula al vuelo.
@Composable
private fun FilaDuracionEstimada(duracionPorRepeticion: Int, repeticiones: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(18.dp),
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = "Duración estimada",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatearDuracionEstimada(duracionPorRepeticion * repeticiones),
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

private fun formatearDuracionEstimada(segundosTotales: Int): String =
    if (segundosTotales >= 60) "${(segundosTotales + 59) / 60} min" else "$segundosTotales s"

private fun formatearFecha(fecha: Date): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fecha)
