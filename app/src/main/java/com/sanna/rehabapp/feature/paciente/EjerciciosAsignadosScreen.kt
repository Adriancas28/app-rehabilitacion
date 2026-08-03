package com.sanna.rehabapp.feature.paciente

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.designsystem.BotonPrimario
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.EstadoVacio
import com.sanna.rehabapp.core.designsystem.TarjetaConIcono
import com.sanna.rehabapp.core.theme.AmbarAlertaContenedor
import com.sanna.rehabapp.core.theme.AmbarAlertaTexto
import com.sanna.rehabapp.core.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EjerciciosAsignadosScreen(
    onEjercicioSeleccionado: (sesionId: String) -> Unit,
    onIniciarSesionDirecta: (sesionId: String) -> Unit,
    onNavegarAHistorial: () -> Unit,
    onNavegarAPerfil: () -> Unit,
    viewModel: EjerciciosAsignadosViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val primerNombre = uiState.nombrePaciente.trim().substringBefore(" ")

    Scaffold(
        topBar = {
            // Banner tipo mockup: fondo blanco y saludo grande en vez de la
            // barra teal que usa el resto de pantallas — a propósito, esta es
            // la única así, igual que en el mockup de referencia (la barra
            // teal vuelve en Ejecutar sesión, Historial, Resultado, etc.).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md + 4.dp, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (primerNombre.isNotBlank()) "Hola, $primerNombre" else "Mis ejercicios",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onNavegarAPerfil) {
                    Icon(Icons.Filled.Person, contentDescription = "Perfil")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.md),
        ) {
            Spacer(modifier = Modifier.height(Spacing.md))
            // Siempre visible, sin importar si hay ejercicios pendientes:
            // antes vivía dentro de la rama "else" de abajo, así que si el
            // paciente se quedaba sin pendientes (ej. termina su única
            // sesión) perdía toda forma de llegar al historial — parecía
            // que sus sesiones completadas "desaparecían".
            TarjetaConIcono(
                icono = Icons.Filled.History,
                titulo = "Mi progreso",
                onClick = onNavegarAHistorial,
                contenidoFinal = {
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.cargando -> EstadoCargando()

                    uiState.ejerciciosAsignados.isEmpty() && uiState.sesionesReanudables.isEmpty() ->
                        EstadoVacio(
                            icono = Icons.Filled.SelfImprovement,
                            mensaje = "Todavía no tienes ejercicios asignados.",
                        )

                    else -> {
                        Column {
                            // HU06-CA09 — sesiones finalizadas antes de tiempo:
                            // se pueden reanudar desde la repetición siguiente.
                            if (uiState.sesionesReanudables.isNotEmpty()) {
                                Text(text = "Reanudar sesión", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                uiState.sesionesReanudables.forEach { reanudable ->
                                    TarjetaReanudable(
                                        item = reanudable,
                                        onReanudar = { onIniciarSesionDirecta(reanudable.sesionId) },
                                    )
                                    Spacer(modifier = Modifier.height(Spacing.sm))
                                }
                                Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                            }

                            if (uiState.ejerciciosAsignados.isNotEmpty()) {
                                val proxima = uiState.ejerciciosAsignados.first()
                                val resto = uiState.ejerciciosAsignados.drop(1)

                                Text(text = "Próxima sesión", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                TarjetaProximaSesion(
                                    item = proxima,
                                    onIniciar = { onIniciarSesionDirecta(proxima.sesionId) },
                                    onClick = { onEjercicioSeleccionado(proxima.sesionId) },
                                )

                                if (resto.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(Spacing.lg - 4.dp))
                                    Text(text = "Todos mis ejercicios", style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(Spacing.sm))
                                    LazyColumn {
                                        items(resto, key = { it.sesionId }) { item ->
                                            TarjetaConIcono(
                                                icono = Icons.Filled.FitnessCenter,
                                                titulo = item.ejercicio.nombre,
                                                subtitulo = item.ejercicio.categoria.etiqueta,
                                                onClick = { onEjercicioSeleccionado(item.sesionId) },
                                                contenidoFinal = {
                                                    Icon(
                                                        Icons.Filled.ChevronRight,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                },
                                                modifier = Modifier.padding(vertical = Spacing.xs),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaProximaSesion(item: EjercicioAsignado, onIniciar: () -> Unit, onClick: () -> Unit) {
    val ejercicio = item.ejercicio
    TarjetaConIcono(
        icono = Icons.Filled.FitnessCenter,
        titulo = ejercicio.nombre,
        subtitulo = item.fechaAsignacion?.let(::formatearFechaHora) ?: ejercicio.categoria.etiqueta,
        onClick = onClick,
        contenidoInferior = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    text = "${item.repeticiones} rep. · " +
                        formatearDuracionTotal(ejercicio.duracionSegundos * item.repeticiones),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(Spacing.md))
            BotonPrimario(
                texto = "Iniciar sesión",
                onClick = onIniciar,
                icono = Icons.Filled.PlayArrow,
            )
        },
    )
}

// HU06-CA09: distinta a la tarjeta de "próxima sesión" (color ámbar) para
// que se note que es un ejercicio interrumpido, no uno nuevo.
@Composable
private fun TarjetaReanudable(item: SesionReanudable, onReanudar: () -> Unit) {
    TarjetaConIcono(
        icono = Icons.Filled.Replay,
        titulo = item.ejercicio.nombre,
        subtitulo = "${item.repeticionesCompletadas}/${item.repeticionesAsignadas} repeticiones completadas",
        colorContenedorIcono = AmbarAlertaContenedor,
        colorIcono = AmbarAlertaTexto,
        onClick = onReanudar,
        contenidoInferior = {
            BotonPrimario(
                texto = "Reanudar sesión",
                onClick = onReanudar,
                icono = Icons.Filled.Replay,
            )
        },
    )
}

private fun formatearFechaHora(fecha: Date): String =
    SimpleDateFormat("d 'de' MMMM, HH:mm", Locale("es", "ES")).format(fecha)

private fun formatearDuracionTotal(segundosTotales: Int): String =
    if (segundosTotales >= 60) "${(segundosTotales + 59) / 60} min" else "$segundosTotales s"
