package com.sanna.rehabapp.core.navigation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sanna.rehabapp.feature.admin.AdminDashboardScreen
import com.sanna.rehabapp.feature.admin.AdminFisioterapeutaFormScreen
import com.sanna.rehabapp.feature.admin.AdminFisioterapeutasScreen
import com.sanna.rehabapp.feature.admin.AdminPacienteFormScreen
import com.sanna.rehabapp.feature.admin.AdminPacientesScreen

fun NavGraphBuilder.adminDestinos(
    navController: NavHostController,
    menuBarraLateralVisible: MutableState<Boolean>,
) {
    composable(Rutas.ADMIN_DASHBOARD) {
        var menuVisible by menuBarraLateralVisible
        AdminDashboardScreen(
            menuVisible = menuVisible,
            onCambiarMenuVisible = { menuVisible = it },
            onNavegarAPacientes = {
                navController.navigate(Rutas.ADMIN_PACIENTES) { launchSingleTop = true }
            },
            onNavegarAFisioterapeutas = {
                navController.navigate(Rutas.ADMIN_FISIOTERAPEUTAS) { launchSingleTop = true }
            },
            onCerrarSesion = {
                navController.navigate(Rutas.LOGIN) {
                    popUpTo(Rutas.RAIZ) { inclusive = true }
                }
            },
        )
    }

    composable(Rutas.ADMIN_PACIENTES) {
        var menuVisible by menuBarraLateralVisible
        AdminPacientesScreen(
            menuVisible = menuVisible,
            onCambiarMenuVisible = { menuVisible = it },
            onRegistrarPaciente = {
                navController.navigate(Rutas.adminPacienteFormulario())
            },
            onEditarPaciente = { usuarioId ->
                navController.navigate(Rutas.adminPacienteFormulario(usuarioId))
            },
            onNavegarADashboard = {
                navController.navigate(Rutas.ADMIN_DASHBOARD) { launchSingleTop = true }
            },
            onNavegarAFisioterapeutas = {
                navController.navigate(Rutas.ADMIN_FISIOTERAPEUTAS) { launchSingleTop = true }
            },
            onCerrarSesion = {
                navController.navigate(Rutas.LOGIN) {
                    popUpTo(Rutas.RAIZ) { inclusive = true }
                }
            },
        )
    }
    composable(
        route = Rutas.ADMIN_PACIENTE_FORMULARIO,
        arguments = listOf(
            navArgument(Rutas.ARG_ADMIN_USUARIO_ID) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) {
        AdminPacienteFormScreen(
            onGuardado = { navController.popBackStack() },
            onVolver = { navController.popBackStack() },
        )
    }

    composable(Rutas.ADMIN_FISIOTERAPEUTAS) {
        var menuVisible by menuBarraLateralVisible
        AdminFisioterapeutasScreen(
            menuVisible = menuVisible,
            onCambiarMenuVisible = { menuVisible = it },
            onRegistrarFisioterapeuta = {
                navController.navigate(Rutas.adminFisioterapeutaFormulario())
            },
            onEditarFisioterapeuta = { usuarioId ->
                navController.navigate(Rutas.adminFisioterapeutaFormulario(usuarioId))
            },
            onNavegarADashboard = {
                navController.navigate(Rutas.ADMIN_DASHBOARD) { launchSingleTop = true }
            },
            onNavegarAPacientes = {
                navController.navigate(Rutas.ADMIN_PACIENTES) { launchSingleTop = true }
            },
            onCerrarSesion = {
                navController.navigate(Rutas.LOGIN) {
                    popUpTo(Rutas.RAIZ) { inclusive = true }
                }
            },
        )
    }
    composable(
        route = Rutas.ADMIN_FISIOTERAPEUTA_FORMULARIO,
        arguments = listOf(
            navArgument(Rutas.ARG_ADMIN_USUARIO_ID) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) {
        AdminFisioterapeutaFormScreen(
            onGuardado = { navController.popBackStack() },
            onVolver = { navController.popBackStack() },
        )
    }
}
