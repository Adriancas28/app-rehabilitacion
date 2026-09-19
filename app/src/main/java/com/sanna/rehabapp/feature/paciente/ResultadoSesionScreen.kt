package com.sanna.rehabapp.feature.paciente

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.BotonPrimario
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.theme.Spacing

// Resultado que ve el paciente justo al terminar la sesión (HU11-CA01): éxito,
// resumen y TODAS las repeticiones con su % individual. El historial y el
// detalle de sesiones anteriores viven en "Mis resultados".
@Composable
fun ResultadoSesionScreen(
    onVolver: () -> Unit,
    onIrAMiProgreso: () -> Unit,
    viewModel: ResultadoSesionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { BarraSuperior(titulo = "Resultado del ejercicio", onNavegarAtras = onVolver) },
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(Spacing.md)
                        .verticalScroll(rememberScrollState()),
                ) {
                    EncabezadoExito()
                    Spacer(modifier = Modifier.height(Spacing.md))

                    TarjetasResumenSesion(resultado)
                    Spacer(modifier = Modifier.height(Spacing.md))

                    if (resultado.detallePorRepeticion.isNotEmpty()) {
                        Text(text = "Detalle por repetición", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        ListaRepeticiones(resultado.detallePorRepeticion, total = resultado.repeticionesAsignadas)
                        Spacer(modifier = Modifier.height(Spacing.md))
                    }

                    BotonPrimario(texto = "Ir a mi progreso", onClick = onIrAMiProgreso)
                }
            }
        }
    }
}

@Composable
private fun EncabezadoExito() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = "¡Ejercicio completado!",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}
