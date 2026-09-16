package com.sanna.rehabapp.feature.ejercicios

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.designsystem.BadgeEstado
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.DialogoConfirmacion
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.EstadoVacio
import com.sanna.rehabapp.core.designsystem.TarjetaEjercicio
import com.sanna.rehabapp.core.designsystem.TipoBadge
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
    onNavegarAPerfil: () -> Unit,
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
                Icons.Rounded.People,
                seleccionado = false,
                onClick = onNavegarAPacientes,
            ),
            ItemBarraLateral("Ejercicios", Icons.Rounded.FitnessCenter, seleccionado = true, onClick = {}),
            ItemBarraLateral(
                "Resultados",
                Icons.Rounded.Assessment,
                seleccionado = false,
                onClick = onNavegarAResultados,
            ),
            ItemBarraLateral("Perfil", Icons.Rounded.Person, seleccionado = false, onClick = onNavegarAPerfil),
        ),
        topBar = { onAlternarMenu ->
            BarraSuperior(
                titulo = "Ejercicios",
                onAlternarMenu = onAlternarMenu,
                acciones = {
                    IconButton(onClick = onRegistrarEjercicio) {
                        Icon(Icons.Rounded.Add, contentDescription = "Registrar ejercicio")
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
                    icono = Icons.Rounded.SelfImprovement,
                    mensaje = "Aún no hay ejercicios registrados.",
                )

                else -> LazyVerticalGrid(columns = GridCells.Fixed(2)) {
                    items(uiState.ejercicios, key = { it.id }) { ejercicio ->
                        TarjetaEjercicio(
                            nombre = ejercicio.nombre,
                            materialUrl = ejercicio.materialUrl,
                            etiquetaEstado = if (!ejercicio.activo) {
                                { BadgeEstado(texto = "Inactivo", tipo = TipoBadge.NEUTRO) }
                            } else {
                                null
                            },
                            modifier = Modifier.padding(Spacing.xs),
                            menu = { cerrar ->
                                DropdownMenuItem(
                                    text = { Text("Editar") },
                                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                    onClick = {
                                        cerrar()
                                        onEditarEjercicio(ejercicio.id)
                                    },
                                )
                                // HU02-CA10 (actualización del modelo de datos): activar/
                                // desactivar en vez de eliminar, para no perder el
                                // historial de sesiones ya asociadas a este ejercicio.
                                DropdownMenuItem(
                                    text = { Text(if (ejercicio.activo) "Desactivar" else "Activar") },
                                    leadingIcon = {
                                        Icon(
                                            if (ejercicio.activo) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        cerrar()
                                        viewModel.cambiarEstadoActivo(ejercicio)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Eliminar") },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
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
        DialogoConfirmacion(
            titulo = "Eliminar ejercicio",
            mensaje = "¿Seguro que deseas eliminar \"${ejercicio.nombre}\"? Esta acción no se puede deshacer.",
            textoConfirmar = "Eliminar",
            onConfirmar = {
                viewModel.eliminar(ejercicio.id)
                ejercicioAEliminar = null
            },
            onCancelar = { ejercicioAEliminar = null },
        )
    }
}
