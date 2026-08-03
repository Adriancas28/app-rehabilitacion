package com.sanna.rehabapp.feature.comunicacion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.sanna.rehabapp.core.designsystem.BotonOutline
import com.sanna.rehabapp.core.designsystem.BotonPrimario
import com.sanna.rehabapp.core.designsystem.CampoTexto
import com.sanna.rehabapp.core.designsystem.DialogoConfirmacion
import com.sanna.rehabapp.core.designsystem.EstadoCargando
import com.sanna.rehabapp.core.designsystem.SeccionFormulario
import com.sanna.rehabapp.core.designsystem.TarjetaBase
import com.sanna.rehabapp.core.theme.Spacing
import com.sanna.rehabapp.domain.model.Recomendacion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RegistrarRecomendacionScreen(
    onVolver: () -> Unit,
    viewModel: RegistrarRecomendacionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var recomendacionAEliminar by remember { mutableStateOf<Recomendacion?>(null) }

    Scaffold(
        topBar = { BarraSuperior(titulo = "Recomendaciones", onNavegarAtras = onVolver) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.md),
        ) {
            SeccionFormulario(
                titulo = if (uiState.editandoId != null) "Editar recomendación" else "Nueva recomendación",
            ) {
                CampoTexto(
                    valor = uiState.texto,
                    onValorCambiado = viewModel::onTextoCambiado,
                    etiqueta = "Recomendación",
                    soloUnaLinea = false,
                    lineasMinimas = 3,
                )
                uiState.error?.let { mensaje ->
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(text = mensaje, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                Row {
                    if (uiState.editandoId != null) {
                        BotonOutline(
                            texto = "Cancelar",
                            onClick = viewModel::cancelarEdicion,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                    }
                    BotonPrimario(
                        texto = if (uiState.editandoId != null) "Guardar cambios" else "Registrar",
                        onClick = viewModel::guardar,
                        habilitado = !uiState.guardando,
                        cargando = uiState.guardando,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
            Text(text = "Registradas", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(Spacing.sm))

            when {
                uiState.cargando -> EstadoCargando()

                uiState.recomendaciones.isEmpty() -> Text(
                    text = "Todavía no hay recomendaciones para esta sesión.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> LazyColumn {
                    items(uiState.recomendaciones, key = { it.id }) { recomendacion ->
                        TarjetaRecomendacion(
                            recomendacion = recomendacion,
                            onEditar = { viewModel.editar(recomendacion) },
                            onEliminar = { recomendacionAEliminar = recomendacion },
                        )
                    }
                }
            }
        }
    }

    recomendacionAEliminar?.let { recomendacion ->
        DialogoConfirmacion(
            titulo = "Eliminar recomendación",
            mensaje = "¿Seguro que deseas eliminarla? Esta acción no se puede deshacer.",
            textoConfirmar = "Eliminar",
            onConfirmar = {
                viewModel.eliminar(recomendacion.id)
                recomendacionAEliminar = null
            },
            onCancelar = { recomendacionAEliminar = null },
        )
    }
}

@Composable
private fun TarjetaRecomendacion(recomendacion: Recomendacion, onEditar: () -> Unit, onEliminar: () -> Unit) {
    var menuAbierto by remember { mutableStateOf(false) }

    TarjetaBase(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = recomendacion.texto, style = MaterialTheme.typography.bodyMedium)
                recomendacion.fecha?.let { fecha ->
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = formatearFecha(fecha),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box {
                IconButton(onClick = { menuAbierto = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones")
                }
                DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            menuAbierto = false
                            onEditar()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar") },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = {
                            menuAbierto = false
                            onEliminar()
                        },
                    )
                }
            }
        }
    }
}

private fun formatearFecha(fecha: Date): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fecha)
