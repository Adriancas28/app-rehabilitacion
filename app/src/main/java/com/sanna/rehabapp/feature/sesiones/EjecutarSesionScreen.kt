package com.sanna.rehabapp.feature.sesiones

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.camera.CamaraConDeteccionPose
import com.sanna.rehabapp.core.camera.tieneCamaraDisponible
import com.sanna.rehabapp.core.tts.rememberLectorInstrucciones

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

    Column(modifier = Modifier.fillMaxSize()) {
        EncabezadoSesion(nombreEjercicio = uiState.ejercicio?.nombre ?: "", onSalir = onVolver)

        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.cargando -> EstadoCentrado { CircularProgressIndicator() }

                uiState.error != null -> EstadoCentrado {
                    MensajeConIcono(Icons.Filled.VideocamOff, uiState.error ?: "")
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onVolver) { Text("Volver") }
                }

                !tieneCamaraDisponible(contexto) -> EstadoCentrado {
                    // RNF03-CA03: el dispositivo no cumple los requisitos mínimos.
                    MensajeConIcono(
                        Icons.Filled.VideocamOff,
                        "Este dispositivo no tiene cámara disponible, así que no puede ejecutar sesiones con monitoreo.",
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onVolver) { Text("Volver") }
                }

                !permisoConcedido -> EstadoCentrado {
                    MensajeConIcono(
                        Icons.Filled.CameraAlt,
                        "Se necesita permiso de cámara para monitorear el ejercicio.",
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = { solicitarPermiso.launch(Manifest.permission.CAMERA) }) {
                        Text("Conceder permiso")
                    }
                }

                uiState.sesionCompletada -> EstadoCentrado {
                    MensajeConIcono(Icons.Filled.CheckCircle, "Sesión completada")
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onVolver) { Text("Volver") }
                }

                !uiState.sesionIniciada -> Box(modifier = Modifier.fillMaxSize()) {
                    CamaraConDeteccionPose(
                        modifier = Modifier.fillMaxSize(),
                        onResultado = viewModel::procesarResultadoPose,
                        onError = { error -> viewModel.onErrorCamara(error.message ?: "Error de cámara") },
                    )
                    Button(
                        onClick = viewModel::iniciarSesion,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                            .height(56.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Iniciar sesión", style = MaterialTheme.typography.titleMedium)
                    }
                }

                // Cámara en vivo a un lado y el panel de progreso/instrucciones
                // al otro — igual que el mockup de referencia (HU06/HU07).
                else -> Row(modifier = Modifier.fillMaxSize()) {
                    CamaraConDeteccionPose(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onResultado = viewModel::procesarResultadoPose,
                        onError = { error -> viewModel.onErrorCamara(error.message ?: "Error de cámara") },
                    )
                    PanelProgreso(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        descripcion = uiState.ejercicio?.descripcion ?: "",
                        repeticionActual = uiState.repeticionActual,
                        totalRepeticiones = uiState.totalRepeticiones,
                        segundosRestantes = uiState.segundosRestantes,
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
private fun EncabezadoSesion(nombreEjercicio: String, onSalir: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onSalir) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Salir",
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Text(
            text = nombreEjercicio,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.weight(1f),
        )
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
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        Icon(
            icono,
            contentDescription = null,
            modifier = Modifier.height(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// HU06/HU10 (parcial): panel lateral con las instrucciones del ejercicio,
// la repetición actual y el tiempo restante. La retroalimentación en vivo
// por desviación (HU10-CA01/CA06) sigue pendiente para Sprint 4 — aquí solo
// se muestra la instrucción estática del ejercicio (Ejercicio.descripcion).
@Composable
private fun PanelProgreso(
    modifier: Modifier,
    descripcion: String,
    repeticionActual: Int,
    totalRepeticiones: Int,
    segundosRestantes: Int,
    enDescanso: Boolean,
    segundosDescanso: Int,
    onFinalizar: () -> Unit,
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "Instrucciones", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = descripcion.ifBlank { "Sigue el movimiento indicado por tu fisioterapeuta." },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (totalRepeticiones > 1) {
            Text(
                text = "Repetición $repeticionActual/$totalRepeticiones",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Box(
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (enDescanso) "$segundosDescanso s" else "$segundosRestantes s",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (enDescanso) "Descansa, viene la repetición ${repeticionActual + 1}…" else "Monitoreando…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(
            onClick = onFinalizar,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.height(44.dp),
        ) {
            Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.width(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Finalizar ejercicio")
        }
    }
}
