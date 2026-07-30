package com.sanna.rehabapp.feature.paciente

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.navigation.CerrarSesionViewModel
import com.sanna.rehabapp.core.theme.AmbarAlertaContenedor
import com.sanna.rehabapp.core.theme.AmbarAlertaTexto
import com.sanna.rehabapp.domain.model.Ejercicio
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EjerciciosAsignadosScreen(
    onEjercicioSeleccionado: (sesionId: String) -> Unit,
    onIniciarSesionDirecta: (sesionId: String) -> Unit,
    onNavegarAHistorial: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: EjerciciosAsignadosViewModel = hiltViewModel(),
    cerrarSesionViewModel: CerrarSesionViewModel = hiltViewModel(),
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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (primerNombre.isNotBlank()) "Hola, $primerNombre" else "Mis ejercicios",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    cerrarSesionViewModel.cerrarSesion()
                    onCerrarSesion()
                }) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            // Siempre visible, sin importar si hay ejercicios pendientes:
            // antes vivía dentro de la rama "else" de abajo, así que si el
            // paciente se quedaba sin pendientes (ej. termina su única
            // sesión) perdía toda forma de llegar al historial — parecía
            // que sus sesiones completadas "desaparecían".
            AccesoRapidoProgreso(onClick = onNavegarAHistorial)
            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.cargando -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }

                    uiState.ejerciciosAsignados.isEmpty() && uiState.sesionesReanudables.isEmpty() ->
                        MensajeSinEjercicios()

                    else -> {
                        Column {
                            // HU06-CA09 — sesiones finalizadas antes de tiempo:
                            // se pueden reanudar desde la repetición siguiente.
                            if (uiState.sesionesReanudables.isNotEmpty()) {
                                Text(text = "Reanudar sesión", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                uiState.sesionesReanudables.forEach { reanudable ->
                                    TarjetaReanudable(
                                        item = reanudable,
                                        onReanudar = { onIniciarSesionDirecta(reanudable.sesionId) },
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            if (uiState.ejerciciosAsignados.isNotEmpty()) {
                                val proxima = uiState.ejerciciosAsignados.first()
                                val resto = uiState.ejerciciosAsignados.drop(1)

                                Text(text = "Próxima sesión", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                TarjetaProximaSesion(
                                    item = proxima,
                                    onIniciar = { onIniciarSesionDirecta(proxima.sesionId) },
                                    onClick = { onEjercicioSeleccionado(proxima.sesionId) },
                                )

                                if (resto.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(text = "Todos mis ejercicios", style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyColumn {
                                        items(resto, key = { it.sesionId }) { item ->
                                            TarjetaEjercicioAsignado(
                                                ejercicio = item.ejercicio,
                                                onClick = { onEjercicioSeleccionado(item.sesionId) },
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
private fun MensajeSinEjercicios() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.SelfImprovement,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Todavía no tienes ejercicios asignados.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TarjetaProximaSesion(item: EjercicioAsignado, onIniciar: () -> Unit, onClick: () -> Unit) {
    val ejercicio = item.ejercicio
    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = ejercicio.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = item.fechaAsignacion?.let(::formatearFechaHora) ?: ejercicio.categoria.etiqueta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${item.repeticiones} rep. · " +
                        formatearDuracionTotal(ejercicio.duracionSegundos * item.repeticiones),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onIniciar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Iniciar sesión")
            }
        }
    }
}

// HU06-CA09: distinta a la tarjeta de "próxima sesión" (color ámbar) para
// que se note que es un ejercicio interrumpido, no uno nuevo.
@Composable
private fun TarjetaReanudable(item: SesionReanudable, onReanudar: () -> Unit) {
    Card(
        onClick = onReanudar,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(AmbarAlertaContenedor, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Replay, contentDescription = null, tint = AmbarAlertaTexto)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.ejercicio.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${item.repeticionesCompletadas}/${item.repeticionesAsignadas} repeticiones completadas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onReanudar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Icon(Icons.Filled.Replay, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reanudar sesión")
            }
        }
    }
}

@Composable
private fun AccesoRapidoProgreso(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(text = "Mi progreso", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TarjetaEjercicioAsignado(ejercicio: Ejercicio, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = ejercicio.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = ejercicio.categoria.etiqueta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatearFechaHora(fecha: Date): String =
    SimpleDateFormat("d 'de' MMMM, HH:mm", Locale("es", "ES")).format(fecha)

private fun formatearDuracionTotal(segundosTotales: Int): String =
    if (segundosTotales >= 60) "${(segundosTotales + 59) / 60} min" else "$segundosTotales s"
