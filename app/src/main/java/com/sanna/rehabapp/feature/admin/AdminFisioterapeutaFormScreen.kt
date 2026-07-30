package com.sanna.rehabapp.feature.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AdminFisioterapeutaFormScreen(
    onGuardado: () -> Unit,
    onVolver: () -> Unit,
    viewModel: AdminFisioterapeutaFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Confirmación visible (Snackbar) antes de volver a la lista, para que
    // el CRUD de fisioterapeuta no sea una acción silenciosa.
    LaunchedEffect(uiState.guardadoExitoso) {
        if (uiState.guardadoExitoso) {
            launch {
                snackbarHostState.showSnackbar(
                    if (viewModel.esEdicion) "Fisioterapeuta actualizado." else "Fisioterapeuta creado.",
                )
            }
            delay(700)
            onGuardado()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AdminUsuarioFormContenido(
            titulo = if (viewModel.esEdicion) "Editar fisioterapeuta" else "Registrar fisioterapeuta",
            esEdicion = viewModel.esEdicion,
            uiState = uiState,
            onNombreCambiado = viewModel::onNombreCambiado,
            onEmailCambiado = viewModel::onEmailCambiado,
            onPasswordCambiado = viewModel::onPasswordCambiado,
            onGuardar = viewModel::guardar,
            onVolver = onVolver,
        )
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
