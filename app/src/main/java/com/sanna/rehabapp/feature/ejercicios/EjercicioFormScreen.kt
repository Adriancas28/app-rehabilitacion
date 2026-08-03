package com.sanna.rehabapp.feature.ejercicios

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.sanna.rehabapp.core.designsystem.BotonOutline
import com.sanna.rehabapp.core.designsystem.BotonPrimario
import com.sanna.rehabapp.core.designsystem.CampoTexto
import com.sanna.rehabapp.core.designsystem.ChecklistAgrupado
import com.sanna.rehabapp.core.designsystem.SeccionFormulario
import com.sanna.rehabapp.core.designsystem.SelectorDropdown
import com.sanna.rehabapp.core.theme.Spacing
import com.sanna.rehabapp.domain.model.Articulacion
import com.sanna.rehabapp.domain.model.CategoriaEjercicio
import com.sanna.rehabapp.domain.model.TipoDiagnostico

@Composable
fun EjercicioFormScreen(
    onGuardado: () -> Unit,
    onVolver: () -> Unit,
    viewModel: EjercicioFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectorArchivo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> viewModel.onArchivoSeleccionado(uri) }

    LaunchedEffect(uiState.guardadoExitoso) {
        if (uiState.guardadoExitoso) onGuardado()
    }

    Scaffold(
        topBar = { BarraSuperior(titulo = if (viewModel.esEdicion) "Editar ejercicio" else "Registrar ejercicio", onNavegarAtras = onVolver) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.md)
                .verticalScroll(rememberScrollState()),
        ) {
            SeccionFormulario(titulo = "Información básica") {
                CampoTexto(
                    valor = uiState.nombre,
                    onValorCambiado = viewModel::onNombreCambiado,
                    etiqueta = "Nombre",
                )
                Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                CampoTexto(
                    valor = uiState.descripcion,
                    onValorCambiado = viewModel::onDescripcionCambiada,
                    etiqueta = "Descripción",
                    soloUnaLinea = false,
                    lineasMinimas = 3,
                )
                Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                SelectorDropdown(
                    valorSeleccionado = uiState.categoria,
                    opciones = CategoriaEjercicio.entries,
                    etiquetaDeOpcion = { it.etiqueta },
                    onSeleccionar = viewModel::onCategoriaCambiada,
                    etiqueta = "Categoría",
                    placeholder = "Selecciona una categoría",
                )
                Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    CampoTexto(
                        valor = uiState.duracionSegundos,
                        onValorCambiado = viewModel::onDuracionCambiada,
                        etiqueta = "Duración por repetición (s)",
                        tipoTeclado = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm + 4.dp))
                    CampoTexto(
                        valor = uiState.repeticiones,
                        onValorCambiado = viewModel::onRepeticionesCambiadas,
                        etiqueta = "Repeticiones",
                        tipoTeclado = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            SeccionFormulario(titulo = "Ángulos de referencia por articulación (opcional)") {
                uiState.patronesReferencia.forEach { fila ->
                    FilaPatronReferencia(
                        fila = fila,
                        onArticulacionCambiada = { viewModel.onArticulacionCambiada(fila.idFila, it) },
                        onAnguloMinCambiado = { viewModel.onAnguloMinCambiado(fila.idFila, it) },
                        onAnguloMaxCambiado = { viewModel.onAnguloMaxCambiado(fila.idFila, it) },
                        onEliminar = { viewModel.eliminarArticulacion(fila.idFila) },
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                }
                BotonOutline(
                    texto = "Agregar articulación",
                    onClick = viewModel::agregarArticulacion,
                    icono = Icons.Filled.Add,
                )

                // HU02-CA07: si el material es un video y ya hay al menos
                // una articulación elegida, se puede calcular el rango
                // automáticamente en vez de escribirlo a mano.
                if (uiState.esVideoSeleccionado && uiState.patronesReferencia.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                    BotonOutline(
                        texto = if (uiState.calculandoRom) "Analizando video…" else "Calcular rango automáticamente desde el video",
                        onClick = viewModel::calcularRomDesdeVideo,
                        cargando = uiState.calculandoRom,
                        icono = Icons.Filled.AutoAwesome,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // HU03-CA05 (ampliación): diagnósticos para los que este
            // ejercicio se sugiere primero al asignar una sesión.
            SeccionFormulario(titulo = "Diagnósticos sugeridos (opcional)") {
                ChecklistAgrupado(
                    opciones = TipoDiagnostico.entries,
                    seleccionados = uiState.diagnosticosAplicables,
                    agruparPor = { it.regionCorporal },
                    etiquetaDeOpcion = { it.etiqueta },
                    onAlternar = viewModel::onDiagnosticoAplicableAlternado,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            SeccionFormulario(titulo = "Material terapéutico (imagen o video)") {
                BotonOutline(
                    texto = when {
                        uiState.archivoSeleccionado != null -> "Archivo seleccionado"
                        uiState.materialUrlActual.isNotBlank() -> "Reemplazar material actual"
                        else -> "Seleccionar archivo"
                    },
                    onClick = { selectorArchivo.launch(arrayOf("image/*", "video/*")) },
                    icono = if (uiState.archivoSeleccionado != null) Icons.Filled.CheckCircle else Icons.Filled.AttachFile,
                )
            }

            uiState.error?.let { mensaje ->
                Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                Text(text = mensaje, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
            BotonPrimario(
                texto = "Guardar ejercicio",
                onClick = viewModel::guardar,
                habilitado = !uiState.guardando,
                cargando = uiState.guardando,
            )
        }
    }
}

@Composable
private fun FilaPatronReferencia(
    fila: PatronReferenciaFila,
    onArticulacionCambiada: (Articulacion) -> Unit,
    onAnguloMinCambiado: (String) -> Unit,
    onAnguloMaxCambiado: (String) -> Unit,
    onEliminar: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                SelectorDropdown(
                    valorSeleccionado = fila.articulacion,
                    opciones = Articulacion.entries,
                    etiquetaDeOpcion = { it.etiqueta },
                    onSeleccionar = onArticulacionCambiada,
                    etiqueta = "Articulación",
                    placeholder = "Selecciona una articulación",
                )
            }
            IconButton(onClick = onEliminar) {
                Icon(
                    Icons.Filled.RemoveCircleOutline,
                    contentDescription = "Quitar articulación",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        Row(modifier = Modifier.fillMaxWidth()) {
            CampoTexto(
                valor = fila.anguloMin,
                onValorCambiado = onAnguloMinCambiado,
                etiqueta = "Mín. (°)",
                tipoTeclado = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(Spacing.sm + 4.dp))
            CampoTexto(
                valor = fila.anguloMax,
                onValorCambiado = onAnguloMaxCambiado,
                etiqueta = "Máx. (°)",
                tipoTeclado = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
