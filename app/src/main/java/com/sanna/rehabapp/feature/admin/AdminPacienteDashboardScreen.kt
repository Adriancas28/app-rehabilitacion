package com.sanna.rehabapp.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.List as ListIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.BotonOutline
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.GraficoBarras
import com.sanna.rehabapp.core.designsystem.GraficoLinea
import com.sanna.rehabapp.core.designsystem.TarjetaBase
import com.sanna.rehabapp.core.designsystem.TarjetaEstadistica
import com.sanna.rehabapp.core.theme.AmbarAlertaTexto
import com.sanna.rehabapp.core.theme.Spacing
import com.sanna.rehabapp.core.theme.VerdeExitoTexto

// Etapa 2A (dashboard Admin, mockup "Dashboard de progreso en el panel
// Administrador") -- dashboard de UN paciente puntual, con dos vistas
// intercambiables: lista de sesiones ejecutadas (por defecto) y gráfico
// (tendencia de precisión y % completado por sesión). Todo sale de las
// sesiones reales del paciente en Firestore.
@Composable
fun AdminPacienteDashboardScreen(
    onVolver: () -> Unit,
    viewModel: AdminPacienteDashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            BarraSuperior(titulo = "Dashboard de paciente", onNavegarAtras = onVolver)
        },
    ) { padding ->
        if (uiState.cargando) {
            EstadoCargando(modifier = Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            CabeceraPaciente(nombre = uiState.nombre, subtitulo = uiState.subtitulo)

            Column(modifier = Modifier.padding(Spacing.md)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    TarjetaEstadistica(
                        icono = Icons.Rounded.BarChart,
                        valor = "${uiState.sesionesEjecutadas}",
                        etiqueta = "Sesiones ejecutadas",
                        modifier = Modifier.weight(1f),
                    )
                    TarjetaEstadistica(
                        icono = Icons.Rounded.BarChart,
                        valor = "${uiState.precisionPromedio}%",
                        etiqueta = "Precisión prom.",
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // ERR-ADM-012: el título ocupa el espacio sobrante y el botón
                    // tiene un ancho propio, para que no se peguen en pantallas estrechas.
                    Text(
                        text = "Detalle por sesión",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f).padding(end = Spacing.sm),
                    )
                    androidx.compose.foundation.layout.Box(modifier = Modifier.width(184.dp)) {
                        BotonOutline(
                            texto = if (uiState.mostrandoGrafico) "Mostrar lista" else "Mostrar gráfico",
                            onClick = viewModel::onAlternarVista,
                            icono = if (uiState.mostrandoGrafico) Icons.Rounded.ListIcon else Icons.Rounded.BarChart,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                when {
                    uiState.sesiones.isEmpty() -> Text(
                        text = "Este paciente todavía no ejecutó ninguna sesión.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    uiState.mostrandoGrafico -> VistaGrafico(uiState)
                    else -> VistaLista(uiState.sesiones)
                }
            }
        }
    }
}

@Composable
private fun CabeceraPaciente(nombre: String, subtitulo: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape),
            ) {
                Text(
                    text = inicialesDe(nombre),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp),
                )
            }
            Spacer(modifier = Modifier.width(Spacing.sm))
            Column {
                Text(text = nombre, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
                if (subtitulo.isNotBlank()) {
                    Text(
                        text = subtitulo,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private fun inicialesDe(nombre: String): String =
    nombre.trim().split(" ").filter { it.isNotBlank() }.take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")

@Composable
private fun VistaLista(sesiones: List<SesionDashboard>) {
    Column {
        sesiones.forEach { sesion ->
            TarjetaBase(modifier = Modifier.padding(vertical = Spacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = Spacing.sm)) {
                        Text(
                            text = "Sesión ${sesion.numero} — ${sesion.ejercicio}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = sesion.fecha,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(IntrinsicSize.Max)) {
                        Text(
                            text = "${sesion.porcentajeCorrectas}% correctas",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (sesion.porcentajeCorrectas >= 75) VerdeExitoTexto else AmbarAlertaTexto,
                        )
                        Text(
                            text = "${sesion.porcentajeCompletado}% completado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VistaGrafico(uiState: AdminPacienteDashboardUiState) {
    // Con muchas sesiones "Sesión N" no cabe en su casilla: solo el número.
    val etiquetasSesion = uiState.sesiones.map { if (uiState.sesiones.size <= 5) "Sesión ${it.numero}" else "${it.numero}" }
    Column {
        Text(text = "Tendencia de precisión", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(Spacing.xs))
        GraficoLinea(
            valores = uiState.sesiones.map { it.porcentajeCorrectas.toFloat() },
            etiquetas = etiquetasSesion,
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(text = "% completado por sesión", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(Spacing.xs))
        GraficoBarras(
            valores = uiState.sesiones.map { it.porcentajeCompletado.toFloat() },
            etiquetas = etiquetasSesion,
        )
    }
}
