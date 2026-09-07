package com.sanna.rehabapp.feature.sesiones

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.camera.CamaraConDeteccionPose
import com.sanna.rehabapp.core.camera.tieneCamaraDisponible
import com.sanna.rehabapp.core.designsystem.BadgeEstado
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.BotonOutline
import com.sanna.rehabapp.core.designsystem.BotonPrimario
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.ProgresoCircular
import com.sanna.rehabapp.core.designsystem.TipoBadge
import com.sanna.rehabapp.core.theme.AmbarAlertaTexto
import com.sanna.rehabapp.core.theme.Spacing
import com.sanna.rehabapp.core.theme.VerdeExitoTexto
import com.sanna.rehabapp.core.tts.rememberLectorInstrucciones

// HU06-CA02/CA06: mismos valores que EjecutarSesionViewModel — se usan
// aquí únicamente para calcular el porcentaje del anillo de progreso
// (visual), no para ninguna decisión de negocio.
private const val SEGUNDOS_PREPARACION_INICIAL = 10
private const val SEGUNDOS_DESCANSO_ENTRE_REPETICIONES = 5

@Composable
fun EjecutarSesionScreen(
    onVolver: () -> Unit,
    viewModel: EjecutarSesionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val contexto = LocalContext.current
    val leerInstrucciones = rememberLectorInstrucciones()

    var permisoConcedido by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val solicitarPermiso = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { concedido -> permisoConcedido = concedido }

    LaunchedEffect(Unit) {
        if (!permisoConcedido) solicitarPermiso.launch(Manifest.permission.CAMERA)
    }

    // HU06 — al iniciar el monitoreo se leen en voz alta las instrucciones
    // del ejercicio (una sola vez, no en cada repetición).
    LaunchedEffect(uiState.sesionIniciada) {
        if (uiState.sesionIniciada) {
            uiState.ejercicio?.descripcion?.takeIf { it.isNotBlank() }?.let(leerInstrucciones)
        }
    }

    // HU10-CA06 — cada vez que el ViewModel decide una corrección nueva
    // (ya con el debounce aplicado), se lee en voz alta. `eventoVoz` trae
    // su propio timestamp, así que esto se dispara aunque el mensaje se
    // repita.
    LaunchedEffect(uiState.eventoVoz) {
        uiState.eventoVoz?.let { leerInstrucciones(it.mensaje) }
    }

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = uiState.ejercicio?.nombre ?: "",
                onNavegarAtras = onVolver,
                acciones = {
                    // Fiel al mockup original: "Salir" es un acceso adicional
                    // a la misma acción que la flecha atrás — abandona sin
                    // registrar, distinto de "Finalizar ejercicio" (CA07).
                    TextButton(onClick = onVolver) {
                        Text("Salir", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.cargando -> EstadoCargando()

                uiState.error != null -> EstadoCentrado {
                    MensajeConIcono(Icons.Rounded.VideocamOff, uiState.error ?: "")
                    Spacer(modifier = Modifier.height(Spacing.lg - 4.dp))
                    BotonPrimario(texto = "Volver", onClick = onVolver, modifier = Modifier.width(200.dp))
                }

                !tieneCamaraDisponible(contexto) -> EstadoCentrado {
                    // RNF03-CA03: el dispositivo no cumple los requisitos mínimos.
                    MensajeConIcono(
                        Icons.Rounded.VideocamOff,
                        "Este dispositivo no tiene cámara disponible, así que no puede ejecutar sesiones con monitoreo.",
                    )
                    Spacer(modifier = Modifier.height(Spacing.lg - 4.dp))
                    BotonPrimario(texto = "Volver", onClick = onVolver, modifier = Modifier.width(200.dp))
                }

                !permisoConcedido -> EstadoCentrado {
                    MensajeConIcono(
                        Icons.Rounded.CameraAlt,
                        "Se necesita permiso de cámara para monitorear el ejercicio.",
                    )
                    Spacer(modifier = Modifier.height(Spacing.lg - 4.dp))
                    BotonPrimario(
                        texto = "Conceder permiso",
                        onClick = { solicitarPermiso.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.width(220.dp),
                    )
                }

                uiState.sesionCompletada -> EstadoCentrado {
                    MensajeConIcono(Icons.Rounded.CheckCircle, "Sesión completada")
                    Spacer(modifier = Modifier.height(Spacing.lg - 4.dp))
                    BotonPrimario(texto = "Volver", onClick = onVolver, modifier = Modifier.width(200.dp))
                }

                !uiState.sesionIniciada -> Box(modifier = Modifier.fillMaxSize()) {
                    CamaraConDeteccionPose(
                        modifier = Modifier.fillMaxSize(),
                        onResultado = viewModel::procesarResultadoPose,
                        onError = { error -> viewModel.onErrorCamara(error.message ?: "Error de cámara") },
                    )
                    BotonPrimario(
                        texto = "Iniciar sesión",
                        onClick = viewModel::iniciarSesion,
                        icono = Icons.Rounded.PlayArrow,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                            .width(220.dp),
                    )
                }

                // Fiel al mockup original (HU06): video + instrucciones lado a
                // lado en la mitad superior, repetición/temporizador/botón
                // "Finalizar ejercicio" a todo el ancho debajo.
                else -> Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(Spacing.md),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .fillMaxHeight()
                                .clip(MaterialTheme.shapes.large),
                        ) {
                            CamaraConDeteccionPose(
                                modifier = Modifier.fillMaxSize(),
                                onResultado = viewModel::procesarResultadoPose,
                                onError = { error -> viewModel.onErrorCamara(error.message ?: "Error de cámara") },
                            )
                            BadgeEstado(
                                texto = "Cámara en vivo",
                                tipo = TipoBadge.EXITO,
                                mostrarPunto = true,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(Spacing.sm),
                            )
                            // HU10-CA01/CA02: ícono mínimo, sin texto — la
                            // corrección en sí la lleva la voz (CA06). El
                            // paciente no puede leer la pantalla mientras se mueve.
                            IconoEstadoCorreccion(
                                enCorreccion = uiState.enCorreccion,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(Spacing.sm),
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                        ) {
                            Text(text = "Instrucciones", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = uiState.ejercicio?.descripcion.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    PanelProgreso(
                        modifier = Modifier.fillMaxWidth(),
                        enPreparacion = uiState.enPreparacion,
                        segundosPreparacion = uiState.segundosPreparacion,
                        repeticionActual = uiState.repeticionActual,
                        totalRepeticiones = uiState.totalRepeticiones,
                        segundosRestantes = uiState.segundosRestantes,
                        duracionRepeticionSegundos = uiState.ejercicio?.duracionSegundos ?: 1,
                        enDescanso = uiState.enDescanso,
                        segundosDescanso = uiState.segundosDescanso,
                        onFinalizar = viewModel::finalizarAntesDeTiempo,
                    )
                }
            }
        }
    }
}

@Composable
private fun EstadoCentrado(contenido: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { contenido() }
    }
}

@Composable
private fun MensajeConIcono(icono: ImageVector, mensaje: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(Spacing.lg)) {
        Icon(
            icono,
            contentDescription = null,
            modifier = Modifier.height(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// HU10-CA01/CA02: indicador mínimo superpuesto en la cámara — sin texto,
// solo color/ícono. El mensaje de corrección en sí lo lleva la voz.
@Composable
private fun IconoEstadoCorreccion(enCorreccion: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .background(
                if (enCorreccion) AmbarAlertaTexto else VerdeExitoTexto,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (enCorreccion) Icons.Rounded.PriorityHigh else Icons.Rounded.Check,
            contentDescription = if (enCorreccion) "Corrige la postura" else "Postura correcta",
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

// HU06 — repetición actual + temporizador circular + botón de finalizar,
// a todo el ancho debajo del video y las instrucciones (mockup original).
@Composable
private fun PanelProgreso(
    modifier: Modifier,
    enPreparacion: Boolean,
    segundosPreparacion: Int,
    repeticionActual: Int,
    totalRepeticiones: Int,
    segundosRestantes: Int,
    duracionRepeticionSegundos: Int,
    enDescanso: Boolean,
    segundosDescanso: Int,
    onFinalizar: () -> Unit,
) {
    if (enPreparacion) {
        Column(
            modifier = modifier.padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "Prepárate", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
            ProgresoCircular(
                porcentaje = 1f - (segundosPreparacion.toFloat() / SEGUNDOS_PREPARACION_INICIAL),
                tamano = 120.dp,
                grosor = 8.dp,
                texto = "$segundosPreparacion s",
                estiloTexto = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "El monitoreo comienza en breve…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    Column(
        modifier = modifier.padding(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (totalRepeticiones > 1) {
            Text(
                text = "Repetición $repeticionActual/$totalRepeticiones",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
        }

        val porcentaje = if (enDescanso) {
            1f - (segundosDescanso.toFloat() / SEGUNDOS_DESCANSO_ENTRE_REPETICIONES)
        } else {
            1f - (segundosRestantes.toFloat() / duracionRepeticionSegundos.coerceAtLeast(1))
        }
        ProgresoCircular(
            porcentaje = porcentaje,
            tamano = 120.dp,
            grosor = 8.dp,
            texto = if (enDescanso) "$segundosDescanso s" else "$segundosRestantes s",
            estiloTexto = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = if (enDescanso) "Descansa, viene la repetición ${repeticionActual + 1}…" else "Monitoreando…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Spacing.lg - 4.dp))
        BotonOutline(
            texto = "Finalizar ejercicio",
            onClick = onFinalizar,
            esDestructivo = true,
            icono = Icons.Rounded.Stop,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
    }
}
