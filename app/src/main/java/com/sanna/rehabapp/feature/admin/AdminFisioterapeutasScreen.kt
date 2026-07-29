package com.sanna.rehabapp.feature.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.sanna.rehabapp.core.navigation.CerrarSesionViewModel
import com.sanna.rehabapp.core.navigation.ItemBarraLateral
import com.sanna.rehabapp.core.navigation.ScaffoldConBarraLateral
import com.sanna.rehabapp.domain.model.Usuario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFisioterapeutasScreen(
    menuVisible: Boolean,
    onCambiarMenuVisible: (Boolean) -> Unit,
    onRegistrarFisioterapeuta: () -> Unit,
    onEditarFisioterapeuta: (String) -> Unit,
    onNavegarAPacientes: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: AdminFisioterapeutasViewModel = hiltViewModel(),
    cerrarSesionViewModel: CerrarSesionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var fisioAEliminar by remember { mutableStateOf<Usuario?>(null) }

    ScaffoldConBarraLateral(
        menuVisible = menuVisible,
        onCambiarMenuVisible = onCambiarMenuVisible,
        items = listOf(
            ItemBarraLateral("Pacientes", Icons.Filled.People, seleccionado = false, onClick = onNavegarAPacientes),
            ItemBarraLateral(
                "Fisioterapeutas",
                Icons.Filled.MedicalServices,
                seleccionado = true,
                onClick = {},
            ),
        ),
        topBar = { onAlternarMenu ->
            TopAppBar(
                title = { Text("Fisioterapeutas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                navigationIcon = {
                    IconButton(onClick = onAlternarMenu) {
                        Icon(Icons.Filled.Menu, contentDescription = "Mostrar/ocultar menú")
                    }
                },
                actions = {
                    IconButton(onClick = onRegistrarFisioterapeuta) {
                        Icon(Icons.Filled.Add, contentDescription = "Registrar fisioterapeuta")
                    }
                    IconButton(onClick = {
                        cerrarSesionViewModel.cerrarSesion()
                        onCerrarSesion()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            when {
                uiState.cargando -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                uiState.fisioterapeutas.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.PersonOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            text = "Aún no hay fisioterapeutas registrados.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }

                else -> LazyColumn {
                    items(uiState.fisioterapeutas, key = { it.uid }) { fisio ->
                        TarjetaUsuarioAdmin(
                            usuario = fisio,
                            onEditar = { onEditarFisioterapeuta(fisio.uid) },
                            onEliminar = { fisioAEliminar = fisio },
                        )
                    }
                }
            }
        }
    }

    fisioAEliminar?.let { fisio ->
        AlertDialog(
            onDismissRequest = { fisioAEliminar = null },
            title = { Text("Eliminar fisioterapeuta") },
            text = { Text("¿Seguro que deseas eliminar la cuenta de \"${fisio.nombre}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminar(fisio.uid)
                    fisioAEliminar = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { fisioAEliminar = null }) { Text("Cancelar") }
            },
        )
    }
}
