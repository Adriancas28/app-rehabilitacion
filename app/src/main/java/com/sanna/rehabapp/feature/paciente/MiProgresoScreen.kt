package com.sanna.rehabapp.feature.paciente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.EstadoVacio
import com.sanna.rehabapp.core.designsystem.GraficoLinea
import com.sanna.rehabapp.core.designsystem.TarjetaCifra
import com.sanna.rehabapp.core.navigation.ScaffoldConBarraLateral
import com.sanna.rehabapp.core.theme.Spacing

// Pantalla 4 del mockup del paciente: cifras generales y gráfico de puntos
// con la evolución sesión a sesión.
@Composable
fun MiProgresoScreen(
    menuVisible: Boolean,
    onCambiarMenuVisible: (Boolean) -> Unit,
    onNavegarAEjercicios: () -> Unit,
    onNavegarAResultados: () -> Unit,
    onNavegarAPerfil: () -> Unit,
    viewModel: MiProgresoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    ScaffoldConBarraLateral(
        menuVisible = menuVisible,
        onCambiarMenuVisible = onCambiarMenuVisible,
        items = itemsBarraPaciente(
            actual = PestanaPaciente.PROGRESO,
            onEjercicios = onNavegarAEjercicios,
            onResultados = onNavegarAResultados,
            onProgreso = {},
            onPerfil = onNavegarAPerfil,
        ),
        topBar = { onAlternarMenu -> BarraSuperior(titulo = "Mi progreso", onAlternarMenu = onAlternarMenu) },
    ) { padding ->
        when {
            uiState.cargando -> EstadoCargando(modifier = Modifier.fillMaxSize().padding(padding))

            uiState.sesionesRealizadas == 0 -> EstadoVacio(
                icono = Icons.Rounded.History,
                mensaje = "Aún no has completado ninguna sesión.",
                modifier = Modifier.fillMaxSize().padding(padding),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(Spacing.md)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    TarjetaCifra(
                        etiqueta = "Sesiones realizadas",
                        valor = "${uiState.sesionesRealizadas}",
                        modifier = Modifier.weight(1f),
                    )
                    TarjetaCifra(
                        etiqueta = "Promedio general",
                        valor = "${uiState.promedioGeneral.toInt()}%",
                        colorValor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.md))

                Text(text = "Evolución por sesión", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(Spacing.sm))
                GraficoLinea(
                    valores = uiState.precisionPorSesion,
                    // Con muchas sesiones "Sesión N" no cabe en su casilla: solo el número.
                    etiquetas = uiState.precisionPorSesion.indices.map {
                        if (uiState.precisionPorSesion.size <= 5) "Sesión ${it + 1}" else "${it + 1}"
                    },
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Eje X: número de sesión · Eje Y: % de precisión de cada sesión",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
