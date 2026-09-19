package com.sanna.rehabapp.feature.paciente

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.BotonPrimario
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.ReproductorVideo
import com.sanna.rehabapp.core.designsystem.TarjetaBase
import com.sanna.rehabapp.core.designsystem.TarjetaCifra
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
                    Text(text = "Cómo realizar el ejercicio", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = ejercicio.descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))

                    // HU05-CA01/CA02 — material terapéutico (imagen o video), justo
                    // después de la descripción, si el ejercicio tiene uno asociado
                    // (es opcional, HU02-CA03).
                    if (ejercicio.materialUrl.isNotBlank()) {
                        MaterialTerapeutico(url = ejercicio.materialUrl)
                        Spacer(modifier = Modifier.height(Spacing.md))
                    }

                    // Ángulo que medirá la IA en esta sesión y repeticiones asignadas.
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        TarjetaCifra(
                            etiqueta = "Ángulo objetivo",
                            valor = uiState.anguloObjetivo ?: "—",
                            colorValor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        TarjetaCifra(
                            etiqueta = "Repeticiones",
                            valor = "${uiState.repeticiones}",
                            modifier = Modifier.weight(1f),
                        )
                    }

                    // Solo si el fisioterapeuta dejó una nota al asignar la sesión.
                    uiState.notaClinica?.let { nota ->
                        Spacer(modifier = Modifier.height(Spacing.md))
                        NotaDelFisioterapeuta(nota)
                    }

                    // HU16 — recomendaciones registradas por el fisioterapeuta.
                    uiState.recomendaciones.forEach { rec ->
                        Spacer(modifier = Modifier.height(Spacing.md))
                        BloqueRecomendacion(rec)
                    }

                    // HU06-CA01/CA02: solo tiene sentido iniciar una sesión
                    // que todavía está pendiente.
                    if (uiState.sesionPendiente) {
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        BotonPrimario(
                            texto = if (uiState.reanudable) "Reanudar sesión" else "Iniciar sesión",
                            onClick = { onIniciarSesion(viewModel.sesionId) },
                            icono = Icons.Rounded.PlayArrow,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotaDelFisioterapeuta(nota: String) {
    TarjetaBase {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = "Nota de tu fisioterapeuta",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(text = nota, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MaterialTerapeutico(url: String) {
    val forma = RoundedCornerShape(16.dp)
    if (esMaterialVideo(url)) {
        ReproductorVideo(url = url, permitirAmpliar = true)
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
