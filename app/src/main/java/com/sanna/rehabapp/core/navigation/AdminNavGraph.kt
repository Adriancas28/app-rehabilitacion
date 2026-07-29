package com.sanna.rehabapp.core.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sanna.rehabapp.feature.admin.AdminPacienteFormScreen
import com.sanna.rehabapp.feature.admin.AdminPacientesScreen

fun NavGraphBuilder.adminDestinos(navController: NavHostController) {
    composable(Rutas.ADMIN_PACIENTES) {
        AdminPacientesScreen(
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

    // Placeholder temporal — HU21 lo reemplaza por la pantalla real.
    composable(Rutas.ADMIN_FISIOTERAPEUTAS) {
        val cerrarSesionViewModel: CerrarSesionViewModel = hiltViewModel()
        PantallaInicioPlaceholder(
            titulo = "Fisioterapeutas",
            onCerrarSesion = {
                cerrarSesionViewModel.cerrarSesion()
                navController.navigate(Rutas.LOGIN) {
                    popUpTo(Rutas.RAIZ) { inclusive = true }
                }
            },
        )
    }
}
