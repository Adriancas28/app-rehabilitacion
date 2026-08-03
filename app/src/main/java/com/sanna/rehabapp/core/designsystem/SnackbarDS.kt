package com.sanna.rehabapp.core.designsystem

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

// Design System — helper para el patrón "mensaje de confirmación de una
// sola vez" (crear/editar/eliminar exitoso o fallido). Reemplaza el
// SnackbarHostState + LaunchedEffect que se repetía a mano en cada
// pantalla de administración. Uso:
//   val snackbarHostState = rememberSnackbarDeMensaje(uiState.mensaje, viewModel::mensajeMostrado)
//   Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { ... }
@Composable
fun rememberSnackbarDeMensaje(mensaje: String?, alMostrarlo: () -> Unit): SnackbarHostState {
    val estado = remember { SnackbarHostState() }
    LaunchedEffect(mensaje) {
        mensaje?.let {
            estado.showSnackbar(it)
            alMostrarlo()
        }
    }
    return estado
}
