package com.sanna.rehabapp.core.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sanna.rehabapp.feature.paciente.DetalleEjercicioAsignadoScreen
import com.sanna.rehabapp.feature.paciente.EjerciciosAsignadosScreen

fun NavGraphBuilder.pacienteDestinos(navController: NavHostController) {
    composable(Rutas.INICIO_PACIENTE) {
        val cerrarSesionViewModel: CerrarSesionViewModel = hiltViewModel()
        EjerciciosAsignadosScreen(
            onEjercicioSeleccionado = { sesionId ->
                navController.navigate(Rutas.detalleEjercicioAsignado(sesionId))
            },
            onCerrarSesion = {
                cerrarSesionViewModel.cerrarSesion()
                navController.navigate(Rutas.LOGIN) {
                    popUpTo(Rutas.RAIZ) { inclusive = true }
                }
            },
        )
    }
    composable(
        route = Rutas.DETALLE_EJERCICIO_ASIGNADO,
        arguments = listOf(navArgument(Rutas.ARG_SESION_ID) {}),
    ) {
        DetalleEjercicioAsignadoScreen(onVolver = { navController.popBackStack() })
    }
}
