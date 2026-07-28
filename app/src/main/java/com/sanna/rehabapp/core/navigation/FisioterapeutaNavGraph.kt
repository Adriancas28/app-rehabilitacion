package com.sanna.rehabapp.core.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sanna.rehabapp.feature.pacientes.PacienteDetalleScreen
import com.sanna.rehabapp.feature.pacientes.PacientesListScreen

fun NavGraphBuilder.fisioterapeutaDestinos(navController: NavHostController) {
    composable(Rutas.PACIENTES) {
        PacientesListScreen(
            onPacienteSeleccionado = { pacienteId ->
                navController.navigate(Rutas.pacienteDetalle(pacienteId))
            },
            onCerrarSesion = {
                navController.navigate(Rutas.LOGIN) {
                    popUpTo(Rutas.RAIZ) { inclusive = true }
                }
            },
        )
    }
    composable(
        route = Rutas.PACIENTE_DETALLE,
        arguments = listOf(navArgument(Rutas.ARG_PACIENTE_ID) {}),
    ) {
        PacienteDetalleScreen(onVolver = { navController.popBackStack() })
    }
}
