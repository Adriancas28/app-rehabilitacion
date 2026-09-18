package com.sanna.rehabapp.feature.pacientes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.BotonOutline
import com.sanna.rehabapp.core.designsystem.BotonPrimario
import com.sanna.rehabapp.core.designsystem.CampoTexto
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.GraficoBarras
import com.sanna.rehabapp.core.designsystem.ProgresoCircular
import com.sanna.rehabapp.core.designsystem.TarjetaBase
import com.sanna.rehabapp.core.theme.AmbarAlertaTexto
import com.sanna.rehabapp.core.theme.Spacing
import com.sanna.rehabapp.core.theme.VerdeExitoTexto
import com.sanna.rehabapp.domain.model.AnguloDetectado
import com.sanna.rehabapp.domain.model.DetalleRepeticion
import com.sanna.rehabapp.domain.model.ResultadoSesion

// HU18-CA04 (ampliación, Etapa 4): solo se muestran las primeras
// repeticiones en la pantalla principal; el resto se ve en el modal
// "Ver más repeticiones" (mockup Idea 10).
private const val REPETICIONES_VISIBLES = 3

@Composable
fun FisioResultadoSesionScreen(
    onVolver: () -> Unit,
    onRegistrarRecomendacion: (pacienteId: String, sesionId: String) -> Unit,
    viewModel: FisioResultadoSesionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { BarraSuperior(titulo = uiState.ejercicio?.nombre ?: "Resultado", onNavegarAtras = onVolver) },
    ) { padding ->
        when {
            uiState.cargando -> EstadoCargando(modifier = Modifier.fillMaxSize().padding(padding))

            uiState.resultado == null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No se encontró el resultado de esta sesión.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                val resultado = uiState.resultado!!
                var mostrarModalRepeticiones by remember { mutableStateOf(false) }
                var mostrarAvisoVideo by remember { mutableStateOf(false) }
                if (mostrarAvisoVideo) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { mostrarAvisoVideo = false },
                        title = { Text("Video de la sesión") },
                        text = { Text("Video no disponible aún.") },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = { mostrarAvisoVideo = false }) {
                                Text("Entendido")
                            }
                        },
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(Spacing.md)
                        .verticalScroll(rememberScrollState()),
                ) {
                    TarjetaResumen(resultado)
                    Spacer(modifier = Modifier.height(Spacing.lg - 4.dp))

                    if (resultado.detallePorRepeticion.isNotEmpty()) {
                        Text(text = "Precisión por repetición", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        GraficoBarras(
                            valores = resultado.detallePorRepeticion
                                .sortedBy { it.numero }
                                .map { it.porcentajeEjecucion },
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))

                        // Parte 3 (video): placeholder -- grabar y subir video
                        // contradice RNF06/HU17-CA02 (nunca se sube video a la
                        // nube) y el consentimiento informado que ve el usuario;
                        // queda pendiente de decisión explícita.
                        BotonOutline(
                            texto = "Ver video de la sesión",
                            onClick = { mostrarAvisoVideo = true },
                        )
                        Spacer(modifier = Modifier.height(Spacing.lg - 4.dp))

                        Text(text = "Detalle por repetición", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        resultado.detallePorRepeticion.take(REPETICIONES_VISIBLES).forEach { detalle ->
                            TarjetaDetalleRepeticion(detalle)
                            Spacer(modifier = Modifier.height(Spacing.sm))
                        }
                        if (resultado.detallePorRepeticion.size > REPETICIONES_VISIBLES) {
                            BotonOutline(
                                texto = "Ver más repeticiones",
                                onClick = { mostrarModalRepeticiones = true },
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                    }

                    if (resultado.angulosDetectados.isNotEmpty()) {
                        Text(text = "Ángulos por articulación", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        resultado.angulosDetectados.forEach { angulo ->
                            TarjetaAngulo(angulo)
                            Spacer(modifier = Modifier.height(Spacing.sm))
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                    }

                    // HU15 (ampliación, Etapa 4): recomendación rápida sin
                    // salir de esta pantalla (mockup Idea 9) -- el paciente
                    // solo la ve una vez guardada (HU16-CA01), nunca antes.
                    Text(text = "Recomendación para el paciente", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    CampoTexto(
                        valor = uiState.recomendacionTexto,
                        onValorCambiado = viewModel::onRecomendacionTextoCambiado,
                        etiqueta = "Escribe una recomendación según lo observado...",
                        soloUnaLinea = false,
                        lineasMinimas = 2,
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    BotonPrimario(
                        texto = "Guardar recomendación",
                        onClick = viewModel::guardarRecomendacion,
                        habilitado = uiState.recomendacionTexto.isNotBlank() && !uiState.guardandoRecomendacion,
                        cargando = uiState.guardandoRecomendacion,
                    )
                    if (uiState.recomendacionGuardada) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "Recomendación guardada. El paciente la verá en \"Mis resultados\".",
                            style = MaterialTheme.typography.bodySmall,
                            color = VerdeExitoTexto,
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    BotonOutline(
                        texto = "Ver todas las recomendaciones",
                        onClick = { onRegistrarRecomendacion(viewModel.pacienteId, viewModel.sesionId) },
                    )
                }

                if (mostrarModalRepeticiones) {
                    ModalRepeticiones(
                        detalles = resultado.detallePorRepeticion,
                        onCerrar = { mostrarModalRepeticiones = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaResumen(resultado: ResultadoSesion) {
    TarjetaBase {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgresoCircular(porcentaje = resultado.porcentajeEjecucion / 100f)
            Spacer(modifier = Modifier.width(Spacing.md))
            Column {
                Text(
                    text = "Repeticiones ${resultado.repeticionesCompletadas}/${resultado.repeticionesAsignadas}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Correctas: ${resultado.repeticionesCorrectas}  ·  " +
                        "Errores: ${resultado.repeticionesCompletadas - resultado.repeticionesCorrectas}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Desviación promedio: ${"%.1f".format(resultado.desviacionPromedio)}°",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// HU18-CA04 (actualización del modelo de datos): lo que el fisioterapeuta
// usa para decidir qué recomendar — por cada repetición, el porcentaje de
// ejecución calculado automáticamente por la IA (en vez del sí/no
// original) y, cuando hubo desviación, qué error puntual tuvo.
@Composable
private fun TarjetaDetalleRepeticion(detalle: DetalleRepeticion) {
    val dentroDeRango = detalle.porcentajeEjecucion >= 100f
    TarjetaBase(relleno = Spacing.sm + 6.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (dentroDeRango) Icons.Filled.Check else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (dentroDeRango) VerdeExitoTexto else AmbarAlertaTexto,
            )
            Spacer(modifier = Modifier.width(Spacing.sm + 4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Repetición ${detalle.numero}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Ejecución: ${detalle.porcentajeEjecucion.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!dentroDeRango) {
                    detalle.errores.forEach { error ->
                        // HU18-CA04 (ampliación, Etapa 4): ángulo real vs
                        // esperado por repetición (mockup Idea 9/10), ej.
                        // "Flexión incorrecta — 150° (esperado 120°)".
                        val sufijoAngulo = if (error.anguloDetectado != null && error.anguloEsperado != null) {
                            " — ${error.anguloDetectado.toInt()}° (esperado ${error.anguloEsperado.toInt()}°)"
                        } else {
                            ""
                        }
                        Text(
                            text = "${error.articulacion} — ${error.tipo}$sufijoAngulo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// Mockup Idea 10: como la lista completa puede ser larga (hasta 12+
// repeticiones), se muestran solo las primeras en la pantalla principal y
// el resto vive en este modal scrolleable.
@Composable
private fun ModalRepeticiones(detalles: List<DetalleRepeticion>, onCerrar: () -> Unit) {
    Dialog(onDismissRequest = onCerrar) {
        TarjetaBase {
            Column(modifier = Modifier.heightIn(max = 480.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Detalle por repetición", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    detalles.sortedBy { it.numero }.forEach { detalle ->
                        TarjetaDetalleRepeticion(detalle)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
                BotonOutline(texto = "Cerrar", onClick = onCerrar)
            }
        }
    }
}

@Composable
private fun TarjetaAngulo(angulo: AnguloDetectado) {
    TarjetaBase(relleno = Spacing.sm + 6.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = angulo.articulacion, style = MaterialTheme.typography.bodyMedium)
                angulo.anguloEsperado?.let { esperado ->
                    Text(
                        text = "Esperado: ${esperado.toInt()}°",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = "${angulo.anguloDetectado.toInt()}°",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
