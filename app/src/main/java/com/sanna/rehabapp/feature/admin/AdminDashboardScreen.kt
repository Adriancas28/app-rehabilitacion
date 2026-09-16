package com.sanna.rehabapp.feature.admin

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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.DialogoConfirmacion
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.GraficoBarras
import com.sanna.rehabapp.core.designsystem.SeccionFormulario
import com.sanna.rehabapp.core.designsystem.TarjetaEstadistica
import com.sanna.rehabapp.core.navigation.CerrarSesionViewModel
import com.sanna.rehabapp.core.navigation.ItemBarraLateral
import com.sanna.rehabapp.core.navigation.ScaffoldConBarraLateral
import com.sanna.rehabapp.core.theme.Spacing

// Etapa 2A (dashboard Admin, ampliación acordada) — resumen agregado de
// pacientes, fisioterapeutas y adherencia terapéutica global, apoyado en
// los mismos datos que ya usan HU20/HU21 (AdminRepository) y HU18
// (SesionRepository), sin un modelo de datos nuevo.
@Composable
fun AdminDashboardScreen(
    menuVisible: Boolean,
    onCambiarMenuVisible: (Boolean) -> Unit,
    onNavegarAPacientes: () -> Unit,
    onNavegarAFisioterapeutas: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: AdminDashboardViewModel = hiltViewModel(),
    cerrarSesionViewModel: CerrarSesionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var confirmandoCierreSesion by remember { mutableStateOf(false) }

    ScaffoldConBarraLateral(
        menuVisible = menuVisible,
        onCambiarMenuVisible = onCambiarMenuVisible,
        items = listOf(
            ItemBarraLateral("Dashboard", Icons.Rounded.Dashboard, seleccionado = true, onClick = {}),
            ItemBarraLateral(
                "Pacientes",
                Icons.Rounded.People,
                seleccionado = false,
                onClick = onNavegarAPacientes,
            ),
            ItemBarraLateral(
                "Fisioterapeutas",
                Icons.Rounded.MedicalServices,
                seleccionado = false,
                onClick = onNavegarAFisioterapeutas,
            ),
            ItemBarraLateral(
                "Cerrar sesión",
                Icons.AutoMirrored.Rounded.Logout,
                seleccionado = false,
                onClick = { confirmandoCierreSesion = true },
            ),
        ),
        topBar = { onAlternarMenu ->
            BarraSuperior(titulo = "Dashboard", onAlternarMenu = onAlternarMenu)
        },
    ) { padding ->
        if (uiState.cargando) {
            EstadoCargando(modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(Spacing.md)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    TarjetaEstadistica(
                        icono = Icons.Rounded.People,
                        valor = "${uiState.totalPacientesActivos}/${uiState.totalPacientes}",
                        etiqueta = "Pacientes activos",
                        modifier = Modifier.weight(1f),
                    )
                    TarjetaEstadistica(
                        icono = Icons.Rounded.MedicalServices,
                        valor = "${uiState.totalFisioterapeutasActivos}/${uiState.totalFisioterapeutas}",
                        etiqueta = "Fisios activos",
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    TarjetaEstadistica(
                        icono = Icons.Rounded.Percent,
                        valor = "${uiState.porcentajeAdherenciaGlobal.toInt()}%",
                        etiqueta = "Adherencia global",
                        modifier = Modifier.weight(1f),
                    )
                    TarjetaEstadistica(
                        icono = Icons.Rounded.Star,
                        valor = "${uiState.promedioCalidadEjecucion.toInt()}%",
                        etiqueta = "Calidad promedio",
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                Text(
                    text = "${uiState.totalSesionesCompletadas}/${uiState.totalSesionesAsignadas} sesiones completadas en total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                if (uiState.adherenciaPorFisioterapeuta.isNotEmpty()) {
                    SeccionFormulario(titulo = "Adherencia por fisioterapeuta") {
                        GraficoBarras(
                            valores = uiState.adherenciaPorFisioterapeuta.map { it.porcentajeAdherencia },
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        uiState.adherenciaPorFisioterapeuta.forEach { fila ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Spacing.xs),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(text = fila.nombre, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${fila.porcentajeAdherencia.toInt()}% " +
                                        "(${fila.sesionesCompletadas}/${fila.sesionesAsignadas})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Aún no hay sesiones asignadas para calcular adherencia por fisioterapeuta.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (confirmandoCierreSesion) {
        DialogoConfirmacion(
            titulo = "Cerrar sesión",
            mensaje = "¿Seguro que deseas cerrar sesión?",
            textoConfirmar = "Cerrar sesión",
            onConfirmar = {
                confirmandoCierreSesion = false
                cerrarSesionViewModel.cerrarSesion()
                onCerrarSesion()
            },
            onCancelar = { confirmandoCierreSesion = false },
        )
    }
}
