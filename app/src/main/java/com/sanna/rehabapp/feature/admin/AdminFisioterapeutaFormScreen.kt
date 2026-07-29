package com.sanna.rehabapp.feature.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AdminFisioterapeutaFormScreen(
    onGuardado: () -> Unit,
    onVolver: () -> Unit,
    viewModel: AdminFisioterapeutaFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.guardadoExitoso) {
        if (uiState.guardadoExitoso) onGuardado()
    }

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
}
