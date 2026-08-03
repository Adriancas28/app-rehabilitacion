package com.sanna.rehabapp.feature.paciente

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.BotonPrimario
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.theme.Spacing

@Composable
fun DetalleEjercicioAsignadoScreen(
    onVolver: () -> Unit,
    onIniciarSesion: (sesionId: String) -> Unit,
    viewModel: DetalleEjercicioAsignadoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { BarraSuperior(titulo = uiState.ejercicio?.nombre ?: "Ejercicio", onNavegarAtras = onVolver) },
    ) { padding ->
        when {
            uiState.cargando -> EstadoCargando(modifier = Modifier.fillMaxSize().padding(padding))

            uiState.ejercicio == null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No se encontró el ejercicio.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                val ejercicio = uiState.ejercicio!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(Spacing.md)
                        .verticalScroll(rememberScrollState()),
                ) {
                    // HU05-CA01/CA02 — material terapéutico (imagen o video),
                    // si el ejercicio tiene uno asociado (es opcional, HU02-CA03).
                    if (ejercicio.materialUrl.isNotBlank()) {
                        MaterialTerapeutico(url = ejercicio.materialUrl)
                        Spacer(modifier = Modifier.height(Spacing.md))
                    }

                    Text(
                        text = ejercicio.categoria.etiqueta,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(text = "Instrucciones", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = ejercicio.descripcion, style = MaterialTheme.typography.bodyMedium)

                    // HU06-CA01/CA02: solo tiene sentido iniciar una sesión
                    // que todavía está pendiente.
                    if (uiState.sesionPendiente) {
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        BotonPrimario(
                            texto = "Iniciar sesión",
                            onClick = { onIniciarSesion(viewModel.sesionId) },
                            icono = Icons.Filled.PlayArrow,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialTerapeutico(url: String) {
    val forma = RoundedCornerShape(16.dp)
    if (esMaterialVideo(url)) {
        AndroidView(
            factory = { contexto ->
                VideoView(contexto).apply {
                    setVideoURI(Uri.parse(url))
                    setMediaController(MediaController(contexto).also { it.setAnchorView(this) })
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(forma),
        )
    } else {
        AsyncImage(
            model = url,
            contentDescription = "Material terapéutico",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(forma),
        )
    }
}
