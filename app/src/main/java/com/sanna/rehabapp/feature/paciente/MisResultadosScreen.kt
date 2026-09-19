package com.sanna.rehabapp.feature.paciente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.designsystem.BadgeEstado
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.EstadoVacio
import com.sanna.rehabapp.core.designsystem.TarjetaConIcono
import com.sanna.rehabapp.core.designsystem.TipoBadge
import com.sanna.rehabapp.core.navigation.ScaffoldConBarraLateral
import com.sanna.rehabapp.core.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// "Mis resultados": todas las sesiones realizadas, una tarjeta por sesión
// (misma familia visual que las tarjetas de pacientes). Al tocar una se abre
// su detalle en otra pantalla (DetalleResultadoScreen).
@Composable
fun MisResultadosScreen(
    menuVisible: Boolean,
    onCambiarMenuVisible: (Boolean) -> Unit,
    onNavegarAEjercicios: () -> Unit,
    onNavegarAProgreso: () -> Unit,
    onNavegarAPerfil: () -> Unit,
    onSesionSeleccionada: (String) -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                uiState.sesiones.forEach { sesion ->
                    val porcentaje = sesion.resultado.porcentajeEjecucion.toInt()
                    TarjetaConIcono(
                        icono = Icons.Rounded.BarChart,
                        titulo = sesion.nombreEjercicio,
                        subtitulo = sesion.fecha?.let(::etiquetaDeFecha) ?: "Fecha no disponible",
                        onClick = { onSesionSeleccionada(sesion.sesionId) },
                        contenidoFinal = {
                            BadgeEstado(
                                texto = "$porcentaje%",
                                tipo = if (porcentaje >= 75) TipoBadge.EXITO else TipoBadge.ADVERTENCIA,
                            )
                        },
                    )
                }
            }
        }
    }
}

private fun etiquetaDeFecha(fecha: Date): String {
    val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val hoy = Calendar.getInstance()
    val dia = Calendar.getInstance().apply { time = fecha }
    val esHoy = hoy.get(Calendar.YEAR) == dia.get(Calendar.YEAR) && hoy.get(Calendar.DAY_OF_YEAR) == dia.get(Calendar.DAY_OF_YEAR)
    return if (esHoy) "Hoy, ${formato.format(fecha)}" else formato.format(fecha)
}
