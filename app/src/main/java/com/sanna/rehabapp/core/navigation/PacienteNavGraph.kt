package com.sanna.rehabapp.core.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sanna.rehabapp.feature.paciente.DetalleEjercicioAsignadoScreen
import com.sanna.rehabapp.feature.paciente.EjerciciosAsignadosScreen
import com.sanna.rehabapp.feature.paciente.HistorialSesionesScreen
import com.sanna.rehabapp.feature.paciente.ResultadoSesionScreen
import com.sanna.rehabapp.feature.perfil.PerfilPacienteScreen
import com.sanna.rehabapp.feature.sesiones.EjecutarSesionScreen

fun NavGraphBuilder.pacienteDestinos(navController: NavHostController) {
    composable(Rutas.INICIO_PACIENTE) {
        EjerciciosAsignadosScreen(
            onEjercicioSeleccionado = { sesionId ->
                navController.navigate(Rutas.detalleEjercicioAsignado(sesionId))
            },
            onIniciarSesionDirecta = { sesionId ->
                navController.navigate(Rutas.ejecutarSesion(sesionId))
            },
            onNavegarAHistorial = {
                navController.navigate(Rutas.HISTORIAL_SESIONES)
            },
            onNavegarAPerfil = {
                navController.navigate(Rutas.PERFIL_PACIENTE)
            },
        )
    }
    composable(Rutas.PERFIL_PACIENTE) {
        PerfilPacienteScreen(
            onVolver = { navController.popBackStack() },
            onCerrarSesion = {
                navController.navigate(Rutas.LOGIN) {
                    popUpTo(Rutas.RAIZ) { inclusive = true }
                }
            },
        )
    }
    composable(Rutas.HISTORIAL_SESIONES) {
        HistorialSesionesScreen(
            onVolver = { navController.popBackStack() },
            onSesionSeleccionada = { sesionId -> navController.navigate(Rutas.resultadoSesion(sesionId)) },
        )
    }
    composable(
        route = Rutas.RESULTADO_SESION,
        arguments = listOf(navArgument(Rutas.ARG_SESION_ID) {}),
    ) {
        ResultadoSesionScreen(
            onVolver = { navController.popBackStack() },
            onVerProgreso = { navController.navigate(Rutas.HISTORIAL_SESIONES) },
            onVolverAlInicio = { navController.popBackStack(Rutas.INICIO_PACIENTE, inclusive = false) },
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
        EjecutarSesionScreen(onVolver = { navController.popBackStack() })
    }
}
