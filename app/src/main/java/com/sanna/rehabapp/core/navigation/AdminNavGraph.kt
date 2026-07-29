package com.sanna.rehabapp.core.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sanna.rehabapp.feature.admin.AdminFisioterapeutaFormScreen
import com.sanna.rehabapp.feature.admin.AdminFisioterapeutasScreen
import com.sanna.rehabapp.feature.admin.AdminPacienteFormScreen
import com.sanna.rehabapp.feature.admin.AdminPacientesScreen

fun NavGraphBuilder.adminDestinos(
    navController: NavHostController,
    menuVisible: Boolean,
    onCambiarMenuVisible: (Boolean) -> Unit,
) {
    composable(Rutas.ADMIN_PACIENTES) {
        AdminPacientesScreen(
            menuVisible = menuVisible,
            onCambiarMenuVisible = onCambiarMenuVisible,
            onRegistrarPaciente = {
                navController.navigate(Rutas.adminPacienteFormulario())
            },
            onEditarPaciente = { usuarioId ->
                navController.navigate(Rutas.adminPacienteFormulario(usuarioId))
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
        AdminFisioterapeutasScreen(
            menuVisible = menuVisible,
            onCambiarMenuVisible = onCambiarMenuVisible,
            onRegistrarFisioterapeuta = {
                navController.navigate(Rutas.adminFisioterapeutaFormulario())
            },
            onEditarFisioterapeuta = { usuarioId ->
                navController.navigate(Rutas.adminFisioterapeutaFormulario(usuarioId))
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
