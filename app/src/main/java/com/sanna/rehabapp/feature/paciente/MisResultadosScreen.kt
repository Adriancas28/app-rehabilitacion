package com.sanna.rehabapp.feature.paciente

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.BotonOutline
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.EstadoVacio
import com.sanna.rehabapp.core.navigation.ScaffoldConBarraLateral
import com.sanna.rehabapp.core.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Pantalla 3 del mockup del paciente: "Sesiones realizadas" arriba (la más
// reciente resaltada) y, debajo, el detalle de la seleccionada.
@Composable
fun MisResultadosScreen(
    menuVisible: Boolean,
    onCambiarMenuVisible: (Boolean) -> Unit,
    onNavegarAEjercicios: () -> Unit,
    onNavegarAProgreso: () -> Unit,
    onNavegarAPerfil: () -> Unit,
    viewModel: MisResultadosViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    ScaffoldConBarraLateral(
        menuVisible = menuVisible,
        onCambiarMenuVisible = onCambiarMenuVisible,
        items = itemsBarraPaciente(
            actual = PestanaPaciente.RESULTADOS,
            onEjercicios = onNavegarAEjercicios,
            onResultados = {},
            onProgreso = onNavegarAProgreso,
            onPerfil = onNavegarAPerfil,
        ),
        topBar = { onAlternarMenu -> BarraSuperior(titulo = "Mis resultados", onAlternarMenu = onAlternarMenu) },
    ) { padding ->
        when {
            uiState.cargando -> EstadoCargando(modifier = Modifier.fillMaxSize().padding(padding))

            uiState.sesiones.isEmpty() -> EstadoVacio(
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
                Text(text = "Sesiones realizadas", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(Spacing.sm))
                // Lista con altura propia (scroll interno): con muchas sesiones
                // el detalle de la seleccionada queda justo debajo, sin tener
                // que recorrer todo el historial para llegar a él.
                Column(
                    modifier = Modifier
                        .heightIn(max = 250.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    uiState.sesiones.forEach { sesion ->
                        FilaSesion(
                            sesion = sesion,
                            seleccionada = sesion.sesionId == uiState.seleccionadaId,
                            onClick = { viewModel.onSesionSeleccionada(sesion.sesionId) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.sm))

                uiState.seleccionada?.let { sesion ->
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "Detalle de la sesión seleccionada (${sesion.fecha?.let(::formatearDiaMes) ?: "—"}):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    DetalleSesion(sesion = sesion, recomendaciones = uiState.recomendaciones)
                }
            }
        }
    }
}

@Composable
private fun FilaSesion(sesion: SesionRealizada, seleccionada: Boolean, onClick: () -> Unit) {
    val forma = RoundedCornerShape(12.dp)
    val colorBorde = if (seleccionada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Card(
        onClick = onClick,
        shape = forma,
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionada) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        modifier = Modifier.fillMaxWidth().border(1.dp, colorBorde, forma),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm + 6.dp, vertical = Spacing.sm + 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = sesion.nombreEjercicio,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = sesion.fecha?.let(::etiquetaDeFecha) ?: "Fecha no disponible",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${sesion.resultado.porcentajeEjecucion.toInt()}% ›",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (seleccionada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun DetalleSesion(sesion: SesionRealizada, recomendaciones: List<com.sanna.rehabapp.domain.model.Recomendacion>) {
    // El estado del toggle es por sesión: al cambiar de sesión vuelve a la lista.
    var mostrarGrafico by remember(sesion.sesionId) { mutableStateOf(false) }
    val resultado = sesion.resultado

    TarjetasResumenSesion(resultado)
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
                icono = if (mostrarGrafico) Icons.Rounded.List else Icons.Rounded.BarChart,
            )
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        if (mostrarGrafico) {
            GraficoRepeticiones(resultado.detallePorRepeticion)
        } else {
            ListaRepeticiones(resultado.detallePorRepeticion, total = resultado.repeticionesAsignadas)
        }
        Spacer(modifier = Modifier.height(Spacing.md))
    }

    // Solo las recomendaciones que el fisioterapeuta ya guardó para ESTA sesión.
    recomendaciones.forEach { recomendacion ->
        BloqueRecomendacion(recomendacion)
        Spacer(modifier = Modifier.height(Spacing.sm))
    }
}

private fun formatearDiaMes(fecha: Date): String = SimpleDateFormat("dd/MM", Locale.getDefault()).format(fecha)

private fun etiquetaDeFecha(fecha: Date): String {
    val hoy = Calendar.getInstance()
    val dia = Calendar.getInstance().apply { time = fecha }
    val esHoy = hoy.get(Calendar.YEAR) == dia.get(Calendar.YEAR) && hoy.get(Calendar.DAY_OF_YEAR) == dia.get(Calendar.DAY_OF_YEAR)
    return if (esHoy) "Hoy, ${formatearDiaMes(fecha)}" else formatearDiaMes(fecha)
}
