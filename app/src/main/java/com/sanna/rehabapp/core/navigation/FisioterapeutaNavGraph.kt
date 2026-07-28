package com.sanna.rehabapp.core.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

fun NavGraphBuilder.fisioterapeutaDestinos(navController: NavHostController) {
    composable(Rutas.PACIENTES) {
        val cerrarSesionViewModel: CerrarSesionViewModel = hiltViewModel()
        PantallaInicioPlaceholder(
            titulo = "Panel del fisioterapeuta",
            onCerrarSesion = {
                cerrarSesionViewModel.cerrarSesion()
                navController.navigate(Rutas.LOGIN) {
                    popUpTo(Rutas.RAIZ) { inclusive = true }
                }
            },
        )
    }
}
