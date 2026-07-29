package com.sanna.rehabapp.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.sanna.rehabapp.domain.model.Rol
import com.sanna.rehabapp.feature.auth.ConsentimientoScreen
import com.sanna.rehabapp.feature.auth.LoginScreen

@Composable
fun RehabNavHost(navController: NavHostController = rememberNavController()) {
    // Se conserva aquí arriba (fuera de las pantallas individuales) para
    // que la barra lateral siga colapsada al navegar entre sus propias
    // pestañas. Se pasa el MutableState en sí (no un Boolean ya resuelto)
    // porque el bloque `builder` de NavHost solo se ejecuta una vez para
    // construir el grafo (su `remember` interno no depende de este
    // estado) — leer `.value` recién dentro de cada composable(ruta) {}
    // es lo que permite que esa pantalla se recomponga cuando cambia.
    val menuBarraLateralVisible = rememberSaveable { mutableStateOf(false) }

    NavHost(navController = navController, startDestination = Rutas.RAIZ) {
        composable(Rutas.RAIZ) {
            PantallaDecisorInicial(navController)
        }
        composable(Rutas.LOGIN) {
            LoginScreen(
                onLoginExitoso = {
                    navController.navigate(Rutas.RAIZ) {
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Rutas.CONSENTIMIENTO) {
            ConsentimientoScreen(
                onAceptado = { rol -> navegarAGrafo(navController, rol) },
            )
        }
        fisioterapeutaDestinos(
            navController = navController,
            menuBarraLateralVisible = menuBarraLateralVisible,
        )
        pacienteDestinos(navController)
        adminDestinos(
            navController = navController,
            menuBarraLateralVisible = menuBarraLateralVisible,
        )
    }
}

@Composable
private fun PantallaDecisorInicial(navController: NavHostController) {
    val viewModel: RaizViewModel = hiltViewModel()
    val destino by viewModel.destino.collectAsState()

    LaunchedEffect(destino) {
        when (val actual = destino) {
            DestinoInicial.Cargando -> Unit
            DestinoInicial.Login -> navController.navigate(Rutas.LOGIN) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            }
            is DestinoInicial.Consentimiento -> navController.navigate(Rutas.CONSENTIMIENTO) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            }
            is DestinoInicial.Grafo -> navegarAGrafo(navController, actual.rol)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

internal fun navegarAGrafo(navController: NavHostController, rol: Rol) {
    val destino = when (rol) {
        Rol.FISIOTERAPEUTA -> Rutas.PACIENTES
        Rol.ADMIN -> Rutas.ADMIN_PACIENTES
        Rol.PACIENTE -> Rutas.INICIO_PACIENTE
    }
    navController.navigate(destino) {
        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
    }
}
