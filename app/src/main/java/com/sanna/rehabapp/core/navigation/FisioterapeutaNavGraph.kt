package com.sanna.rehabapp.core.navigation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sanna.rehabapp.feature.ejercicios.EjercicioFormScreen
import com.sanna.rehabapp.feature.ejercicios.EjerciciosListScreen
import com.sanna.rehabapp.feature.pacientes.PacienteDetalleScreen
import com.sanna.rehabapp.feature.pacientes.PacientesListScreen

fun NavGraphBuilder.fisioterapeutaDestinos(
    navController: NavHostController,
    menuBarraLateralVisible: MutableState<Boolean>,
) {
    composable(Rutas.PACIENTES) {
        var menuVisible by menuBarraLateralVisible
        PacientesListScreen(
            menuVisible = menuVisible,
            onCambiarMenuVisible = { menuVisible = it },
            onPacienteSeleccionado = { pacienteId ->
                navController.navigate(Rutas.pacienteDetalle(pacienteId))
            },
            onNavegarAEjercicios = {
                navController.navigate(Rutas.EJERCICIOS) { launchSingleTop = true }
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
    composable(Rutas.EJERCICIOS) {
        var menuVisible by menuBarraLateralVisible
        EjerciciosListScreen(
            menuVisible = menuVisible,
            onCambiarMenuVisible = { menuVisible = it },
            onRegistrarEjercicio = {
                navController.navigate(Rutas.ejercicioFormulario())
            },
            onEditarEjercicio = { ejercicioId ->
                navController.navigate(Rutas.ejercicioFormulario(ejercicioId))
            },
            onNavegarAPacientes = {
                navController.navigate(Rutas.PACIENTES) { launchSingleTop = true }
            },
        )
    }
    composable(
        route = Rutas.EJERCICIO_FORMULARIO,
        arguments = listOf(
            navArgument(Rutas.ARG_EJERCICIO_ID) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) {
        EjercicioFormScreen(
            onGuardado = { navController.popBackStack() },
            onVolver = { navController.popBackStack() },
        )
    }
}
