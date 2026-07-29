package com.sanna.rehabapp.core.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

fun NavGraphBuilder.pacienteDestinos(navController: NavHostController) {
    composable(Rutas.INICIO_PACIENTE) {
        val cerrarSesionViewModel: CerrarSesionViewModel = hiltViewModel()
        PantallaInicioPlaceholder(
            titulo = "Bienvenido",
            mensaje = "Tus ejercicios asignados van a aparecer aquí muy pronto.",
            onCerrarSesion = {
                cerrarSesionViewModel.cerrarSesion()
                navController.navigate(Rutas.LOGIN) {
                    popUpTo(Rutas.RAIZ) { inclusive = true }
                }
            },
        )
    }
}
