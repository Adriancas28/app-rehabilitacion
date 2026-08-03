package com.sanna.rehabapp.feature.ejercicios

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.EstadoVacio
import com.sanna.rehabapp.core.designsystem.TarjetaEjercicio
import com.sanna.rehabapp.core.navigation.ItemBarraLateral
import com.sanna.rehabapp.core.navigation.ScaffoldConBarraLateral
import com.sanna.rehabapp.core.theme.Spacing
import com.sanna.rehabapp.domain.model.Ejercicio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EjerciciosListScreen(
    menuVisible: Boolean,
    onCambiarMenuVisible: (Boolean) -> Unit,
    onRegistrarEjercicio: () -> Unit,
    onEditarEjercicio: (String) -> Unit,
    onNavegarAPacientes: () -> Unit,
    onNavegarAResultados: () -> Unit,
    viewModel: EjerciciosViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var ejercicioAEliminar by remember { mutableStateOf<Ejercicio?>(null) }

    ScaffoldConBarraLateral(
        menuVisible = menuVisible,
        onCambiarMenuVisible = onCambiarMenuVisible,
        items = listOf(
            ItemBarraLateral(
                "Pacientes",
                Icons.Filled.People,
                seleccionado = false,
                onClick = onNavegarAPacientes,
            ),
            ItemBarraLateral("Ejercicios", Icons.Filled.FitnessCenter, seleccionado = true, onClick = {}),
            ItemBarraLateral(
                "Resultados",
                Icons.Filled.Assessment,
                seleccionado = false,
                onClick = onNavegarAResultados,
            ),
        ),
        topBar = { onAlternarMenu ->
            BarraSuperior(
                titulo = "Ejercicios",
                onAlternarMenu = onAlternarMenu,
                acciones = {
                    IconButton(onClick = onRegistrarEjercicio) {
                        Icon(Icons.Filled.Add, contentDescription = "Registrar ejercicio")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.md),
        ) {
            when {
                uiState.cargando -> EstadoCargando()

                uiState.ejercicios.isEmpty() -> EstadoVacio(
                    icono = Icons.Filled.SelfImprovement,
                    mensaje = "Aún no hay ejercicios registrados.",
                )

                else -> LazyVerticalGrid(columns = GridCells.Fixed(2)) {
                    items(uiState.ejercicios, key = { it.id }) { ejercicio ->
                        TarjetaEjercicio(
                            icono = Icons.Filled.FitnessCenter,
                            nombre = ejercicio.nombre,
                            lineaSecundaria = ejercicio.categoria.etiqueta,
                            lineaTerciaria = {
                                Icon(
                                    Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text(
                                    text = "${ejercicio.repeticiones} rep. · ${formatearDuracion(ejercicio.duracionSegundos)} c/u",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            modifier = Modifier.padding(Spacing.xs),
                            menu = { cerrar ->
                                DropdownMenuItem(
                                    text = { Text("Editar") },
                                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                    onClick = {
                                        cerrar()
                                        onEditarEjercicio(ejercicio.id)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Eliminar") },
                                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                    onClick = {
                                        cerrar()
                                        ejercicioAEliminar = ejercicio
                                    },
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    ejercicioAEliminar?.let { ejercicio ->
        AlertDialog(
            onDismissRequest = { ejercicioAEliminar = null },
            title = { Text("Eliminar ejercicio") },
            text = {
                Text("¿Seguro que deseas eliminar \"${ejercicio.nombre}\"? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminar(ejercicio.id)
                    ejercicioAEliminar = null
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { ejercicioAEliminar = null }) { Text("Cancelar") }
            },
        )
    }
}

private fun formatearDuracion(segundos: Int): String =
    if (segundos >= 60) "${segundos / 60} min" else "$segundos s"
