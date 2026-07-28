package com.sanna.rehabapp.core.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

// Placeholder temporal — HU20/HU21 lo reemplazan en el siguiente commit
// por las pantallas reales de gestión de pacientes/fisioterapeutas.
fun NavGraphBuilder.adminDestinos(navController: NavHostController) {
    composable(Rutas.ADMIN_PACIENTES) {
        val cerrarSesionViewModel: CerrarSesionViewModel = hiltViewModel()
        PantallaInicioPlaceholder(
            titulo = "Panel de administrador",
            onCerrarSesion = {
                cerrarSesionViewModel.cerrarSesion()
                navController.navigate(Rutas.LOGIN) {
                    popUpTo(Rutas.RAIZ) { inclusive = true }
                }
            },
        )
    }
}
