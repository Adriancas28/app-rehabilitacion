package com.sanna.rehabapp.core.navigation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sanna.rehabapp.feature.paciente.DetalleEjercicioAsignadoScreen
import com.sanna.rehabapp.feature.paciente.EjerciciosAsignadosScreen
import com.sanna.rehabapp.feature.paciente.MiProgresoScreen
import com.sanna.rehabapp.feature.paciente.MisResultadosScreen
import com.sanna.rehabapp.feature.paciente.ResultadoSesionScreen
import com.sanna.rehabapp.feature.perfil.PerfilPacienteScreen
import com.sanna.rehabapp.feature.sesiones.EjecutarSesionScreen

fun NavGraphBuilder.pacienteDestinos(
    navController: NavHostController,
    menuBarraLateralVisible: MutableState<Boolean>,
) {
    composable(Rutas.INICIO_PACIENTE) {
        var menuVisible by menuBarraLateralVisible
        EjerciciosAsignadosScreen(
            menuVisible = menuVisible,
            onCambiarMenuVisible = { menuVisible = it },
            onEjercicioSeleccionado = { sesionId ->
                navController.navigate(Rutas.detalleEjercicioAsignado(sesionId))
            },
            onIniciarSesionDirecta = { sesionId ->
                navController.navigate(Rutas.ejecutarSesion(sesionId))
            },
            onNavegarAResultados = {
                navController.navigate(Rutas.MIS_RESULTADOS) { launchSingleTop = true }
            },
            onNavegarAProgreso = {
                navController.navigate(Rutas.PROGRESO_PACIENTE) { launchSingleTop = true }
            },
            onNavegarAPerfil = {
                navController.navigate(Rutas.PERFIL_PACIENTE) { launchSingleTop = true }
            },
        )
    }
    composable(Rutas.MIS_RESULTADOS) {
        var menuVisible by menuBarraLateralVisible
        MisResultadosScreen(
            menuVisible = menuVisible,
            onCambiarMenuVisible = { menuVisible = it },
            onNavegarAEjercicios = {
                navController.navigate(Rutas.INICIO_PACIENTE) { launchSingleTop = true }
            },
            onNavegarAProgreso = {
                navController.navigate(Rutas.PROGRESO_PACIENTE) { launchSingleTop = true }
            },
            onNavegarAPerfil = {
                navController.navigate(Rutas.PERFIL_PACIENTE) { launchSingleTop = true }
            },
        )
    }
    composable(Rutas.PROGRESO_PACIENTE) {
        var menuVisible by menuBarraLateralVisible
        MiProgresoScreen(
            menuVisible = menuVisible,
            onCambiarMenuVisible = { menuVisible = it },
            onNavegarAEjercicios = {
                navController.navigate(Rutas.INICIO_PACIENTE) { launchSingleTop = true }
            },
            onNavegarAResultados = {
                navController.navigate(Rutas.MIS_RESULTADOS) { launchSingleTop = true }
            },
            onNavegarAPerfil = {
                navController.navigate(Rutas.PERFIL_PACIENTE) { launchSingleTop = true }
            },
        )
    }
    composable(Rutas.PERFIL_PACIENTE) {
        var menuVisible by menuBarraLateralVisible
        PerfilPacienteScreen(
            menuVisible = menuVisible,
            onCambiarMenuVisible = { menuVisible = it },
            onNavegarAEjercicios = {
                navController.navigate(Rutas.INICIO_PACIENTE) { launchSingleTop = true }
            },
            onNavegarAResultados = {
                navController.navigate(Rutas.MIS_RESULTADOS) { launchSingleTop = true }
            },
            onNavegarAProgreso = {
                navController.navigate(Rutas.PROGRESO_PACIENTE) { launchSingleTop = true }
            },
            onCerrarSesion = {
                navController.navigate(Rutas.LOGIN) {
                    popUpTo(Rutas.RAIZ) { inclusive = true }
                }
            },
        )
    }
    composable(
        route = Rutas.RESULTADO_SESION,
        arguments = listOf(navArgument(Rutas.ARG_SESION_ID) {}),
    ) {
        ResultadoSesionScreen(
            onVolver = { navController.popBackStack() },
            onIrAMiProgreso = {
                navController.navigate(Rutas.MIS_RESULTADOS) {
                    popUpTo(Rutas.INICIO_PACIENTE)
                }
            },
        )
    }
    composable(
        route = Rutas.DETALLE_EJERCICIO_ASIGNADO,
        arguments = listOf(navArgument(Rutas.ARG_SESION_ID) {}),
    ) {
        DetalleEjercicioAsignadoScreen(
            onVolver = { navController.popBackStack() },
            onIniciarSesion = { sesionId -> navController.navigate(Rutas.ejecutarSesion(sesionId)) },
        )
    }
    composable(
        route = Rutas.EJECUTAR_SESION,
        arguments = listOf(navArgument(Rutas.ARG_SESION_ID) {}),
    ) {
        EjecutarSesionScreen(
            onVolver = { navController.popBackStack() },
            // HU11-CA01: al completar la sesión, se muestra de inmediato el
            // resultado completo (antes solo un mensaje "Sesión completada"
            // sin datos) -- soloLectura=false porque recién se generó, no
            // viene del historial (HU13).
            onSesionCompletada = { sesionId ->
                navController.navigate(Rutas.resultadoSesion(sesionId)) {
                    popUpTo(Rutas.INICIO_PACIENTE)
                }
            },
        )
    }
}
