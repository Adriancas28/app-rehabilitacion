package com.sanna.rehabapp.feature.pacientes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.List as ListaIcono
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.BotonOutline
import com.sanna.rehabapp.core.designsystem.BotonPrimario
import com.sanna.rehabapp.core.designsystem.CampoTexto
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.GraficoBarras
import com.sanna.rehabapp.core.designsystem.ReproductorVideo
import com.sanna.rehabapp.core.designsystem.TarjetaCifra
import com.sanna.rehabapp.core.designsystem.TarjetaBase
import com.sanna.rehabapp.core.theme.ErrorColor
import com.sanna.rehabapp.core.theme.RojoErrorContenedor
import com.sanna.rehabapp.core.theme.RojoErrorTexto
import com.sanna.rehabapp.core.theme.Spacing
import com.sanna.rehabapp.core.theme.VerdeExitoTexto
import com.sanna.rehabapp.domain.model.DetalleRepeticion
import com.sanna.rehabapp.domain.model.ResultadoSesion

// Idea 9 (mockup fisio): en la pantalla principal solo se listan las primeras
// repeticiones con error; el resto vive en el modal "Ver más repeticiones".
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
                var mostrarGrafico by remember { mutableStateOf(false) }
                var mostrarModalRepeticiones by remember { mutableStateOf(false) }
                var mostrarVideo by remember { mutableStateOf(false) }

                // Solo las repeticiones donde se midió AL MENOS un ángulo
                // incorrecto: una repetición perfecta no aparece en la lista.
                val repeticionesConError = resultado.detallePorRepeticion
                    .filter { it.errores.isNotEmpty() }
                    .sortedBy { it.numero }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(Spacing.md)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        TarjetaCifra(
                            etiqueta = "Repeticiones completas",
                            valor = "${resultado.repeticionesCompletadas} / ${resultado.repeticionesAsignadas}",
                            modifier = Modifier.weight(1f),
                        )
                        TarjetaCifra(
                            etiqueta = "Promedio correcto",
                            valor = "${resultado.porcentajeEjecucion.toInt()}%",
                            colorValor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.md))

                    if (resultado.detallePorRepeticion.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = "Detalle por repetición", style = MaterialTheme.typography.titleSmall)
                            BotonOutline(
                                texto = if (mostrarGrafico) "Mostrar lista" else "Mostrar gráfico",
                                onClick = { mostrarGrafico = !mostrarGrafico },
                                icono = if (mostrarGrafico) Icons.Rounded.ListaIcono else Icons.Rounded.BarChart,
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm))

                        if (mostrarGrafico) {
                            val todas = resultado.detallePorRepeticion.sortedBy { it.numero }
                            GraficoBarras(
                                valores = todas.map { it.porcentajeEjecucion },
                                umbral = 100f,
                                colorSobreUmbral = VerdeExitoTexto,
                                colorBajoUmbral = ErrorColor,
                                etiquetas = todas.map { "${it.numero}" },
                                mostrarValores = false,
                                mostrarEjeY = true,
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = "% de precisión por repetición (rojo = tuvo algún ángulo incorrecto)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Text(
                                text = "Solo se listan las repeticiones con algún error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            if (repeticionesConError.isEmpty()) {
                                Text(
                                    text = "Ninguna repetición presentó ángulos incorrectos.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            repeticionesConError.take(REPETICIONES_VISIBLES).forEach { detalle ->
                                FilaRepeticionConError(detalle, resultado.repeticionesAsignadas)
                                Spacer(modifier = Modifier.height(Spacing.sm))
                            }
                            if (repeticionesConError.size > REPETICIONES_VISIBLES) {
                                BotonOutline(
                                    texto = "Ver más repeticiones",
                                    onClick = { mostrarModalRepeticiones = true },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }

                    // El video es independiente del análisis: puede existir aunque
                    // no haya detalle por repetición.
                    BotonOutline(
                        texto = "Ver video de la sesión",
                        onClick = { mostrarVideo = true },
                    )
                    Spacer(modifier = Modifier.height(Spacing.lg - 4.dp))

                    // HU15 (ampliación, Etapa 4): recomendación rápida sin
                    // salir de esta pantalla -- el paciente solo la ve una vez
                    // guardada (HU16-CA01), nunca antes.
                    Text(text = "Recomendación para el paciente", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    CampoTexto(
                        valor = uiState.recomendacionTexto,
                        onValorCambiado = viewModel::onRecomendacionTextoCambiado,
                        maxCaracteres = com.sanna.rehabapp.feature.comunicacion.LIMITE_RECOMENDACION,
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
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = if (uiState.recomendacionGuardada) {
                            "Recomendación guardada. El paciente la verá en \"Mis resultados\"."
                        } else {
                            "El paciente la verá en \"Mis resultados\" una vez guardada."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.recomendacionGuardada) VerdeExitoTexto else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        onClick = { onRegistrarRecomendacion(viewModel.pacienteId, viewModel.sesionId) },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text("Ver todas las recomendaciones")
                    }
                }

                if (mostrarModalRepeticiones) {
                    ModalRepeticiones(
                        conError = repeticionesConError,
                        todas = resultado.detallePorRepeticion,
                        total = resultado.repeticionesAsignadas,
                        onCerrar = { mostrarModalRepeticiones = false },
                    )
                }

                if (mostrarVideo) {
                    val videoUrl = uiState.videoUrl
                    if (videoUrl != null) {
                        Dialog(onDismissRequest = { mostrarVideo = false }) {
                            TarjetaBase {
                                Text(text = "Video de la sesión", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                ReproductorVideo(url = videoUrl, alto = 360.dp)
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                BotonOutline(texto = "Cerrar", onClick = { mostrarVideo = false })
                            }
                        }
                    } else {
                        AlertDialog(
                            onDismissRequest = { mostrarVideo = false },
                            title = { Text("Video de la sesión") },
                            text = { Text("Video no disponible aún.") },
                            confirmButton = {
                                TextButton(onClick = { mostrarVideo = false }) { Text("Entendido") }
                            },
                        )
                    }
                }
            }
        }
    }
}

// Una repetición con al menos un ángulo incorrecto: su número, el % de
// precisión de ESA repetición (puede ser alto aunque tenga un error) y, por
// cada error, el segundo en que ocurrió con el ángulo detectado vs esperado.
@Composable
private fun FilaRepeticionConError(detalle: DetalleRepeticion, total: Int) {
    val forma = MaterialTheme.shapes.large
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RojoErrorContenedor, forma)
            .border(1.dp, ErrorColor.copy(alpha = 0.55f), forma)
            .padding(Spacing.sm + 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Repetición ${detalle.numero}/$total",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${detalle.porcentajeEjecucion.toInt()}% correcto",
                style = MaterialTheme.typography.titleSmall,
                color = RojoErrorTexto,
            )
        }
        detalle.errores.forEach { error ->
            Spacer(modifier = Modifier.height(Spacing.xs))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = RojoErrorTexto,
                    modifier = Modifier.padding(top = 2.dp).width(16.dp),
                )
                Spacer(modifier = Modifier.width(Spacing.xs + 2.dp))
                Text(
                    text = descripcionError(error),
                    style = MaterialTheme.typography.bodySmall,
                    color = RojoErrorTexto,
                )
            }
        }
    }
}

// ERR-FIS-005: "Segundo 8 — Rodilla derecha — ángulo 81° (esperado 90°)": la
// articulación se muestra siempre junto al segundo. Las sesiones guardadas
// antes de registrar el segundo caen a "Articulación — ángulo incorrecto...",
// y sin ángulos a "Articulación — tipo de error".
private fun descripcionError(error: com.sanna.rehabapp.domain.model.ErrorDetectado): String {
    val articulacion = com.sanna.rehabapp.domain.model.Articulacion.desdeFirestoreOrNull(error.articulacion)?.etiqueta
        ?: error.articulacion
    val prefijo = error.segundo?.let { "Segundo $it — $articulacion" } ?: articulacion
    val detectado = error.anguloDetectado
    val esperado = error.anguloEsperado
    return if (detectado != null && esperado != null) {
        "$prefijo — ángulo incorrecto: ${detectado.toInt()}° (esperado ${esperado.toInt()}°)"
    } else {
        "$prefijo — ${error.tipo}"
    }
}

// Idea 10 (mockup fisio): listado COMPLETO de las repeticiones con error, en
// el mismo formato que la pantalla principal, y al final cuáles no tuvieron
// ningún error.
@Composable
private fun ModalRepeticiones(
    conError: List<DetalleRepeticion>,
    todas: List<DetalleRepeticion>,
    total: Int,
    onCerrar: () -> Unit,
) {
    val sinError = todas.filter { it.errores.isEmpty() }.map { it.numero }.sorted()
    Dialog(onDismissRequest = onCerrar) {
        TarjetaBase {
            Column(modifier = Modifier.heightIn(max = 520.dp)) {
                Text(text = "Detalle por repetición", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(Spacing.sm))
                Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    conError.forEach { detalle ->
                        FilaRepeticionConError(detalle, total)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }
                    if (sinError.isNotEmpty()) {
                        Text(
                            text = "Las repeticiones ${listaConY(sinError)} no presentaron ángulos incorrectos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
                BotonOutline(texto = "Cerrar", onClick = onCerrar)
            }
        }
    }
}

// [2, 4, 5] -> "2, 4 y 5"; [7] -> "7"
private fun listaConY(numeros: List<Int>): String = when (numeros.size) {
    0 -> ""
    1 -> "${numeros[0]}"
    else -> numeros.dropLast(1).joinToString(", ") + " y ${numeros.last()}"
}
