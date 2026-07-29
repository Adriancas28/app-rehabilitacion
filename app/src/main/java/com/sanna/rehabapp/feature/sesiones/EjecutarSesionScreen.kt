package com.sanna.rehabapp.feature.sesiones

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.camera.CamaraConDeteccionPose
import com.sanna.rehabapp.core.camera.tieneCamaraDisponible

@Composable
fun EjecutarSesionScreen(
    onVolver: () -> Unit,
    viewModel: EjecutarSesionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val contexto = LocalContext.current

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

    Box(modifier = Modifier.fillMaxSize()) {
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

            else -> {
                CamaraConDeteccionPose(
                    modifier = Modifier.fillMaxSize(),
                    onResultado = viewModel::procesarResultadoPose,
                    onError = { error -> viewModel.onErrorCamara(error.message ?: "Error de cámara") },
                )
                ControlesSesion(
                    nombreEjercicio = uiState.ejercicio?.nombre ?: "",
                    sesionIniciada = uiState.sesionIniciada,
                    repeticionActual = uiState.repeticionActual,
                    totalRepeticiones = uiState.totalRepeticiones,
                    segundosRestantes = uiState.segundosRestantes,
                    enDescanso = uiState.enDescanso,
                    segundosDescanso = uiState.segundosDescanso,
                    onIniciar = viewModel::iniciarSesion,
                )
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

@Composable
private fun ControlesSesion(
    nombreEjercicio: String,
    sesionIniciada: Boolean,
    repeticionActual: Int,
    totalRepeticiones: Int,
    segundosRestantes: Int,
    enDescanso: Boolean,
    segundosDescanso: Int,
    onIniciar: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp),
        ) {
            Text(
                text = nombreEjercicio,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            if (sesionIniciada && totalRepeticiones > 1) {
                Text(
                    text = "Repetición $repeticionActual de $totalRepeticiones",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (!sesionIniciada) {
                Button(
                    onClick = onIniciar,
                    modifier = Modifier.height(56.dp),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Iniciar sesión", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (enDescanso) {
                            Text(
                                text = "$segundosDescanso s",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                            )
                            Text(
                                text = "Descansa, viene la repetición ${repeticionActual + 1}…",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                            )
                        } else {
                            Text(
                                text = "$segundosRestantes s",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                            )
                            Text(
                                text = "Monitoreando…",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}
