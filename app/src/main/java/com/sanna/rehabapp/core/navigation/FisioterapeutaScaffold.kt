package com.sanna.rehabapp.core.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

enum class PestanaFisioterapeuta { PACIENTES, EJERCICIOS }

// El fisioterapeuta navega por una barra lateral izquierda (como el
// mockup de referencia), no un menú inferior — a diferencia del paciente,
// que no tiene esta barra (solo tiene un home por ahora, HU04 en Sprint 2).
// La barra es plegable: cada pantalla recibe "onAlternarMenu" para
// mostrarla/ocultarla desde un ícono de menú (☰) en su TopAppBar.
@Composable
fun FisioterapeutaScaffold(
    pestanaActual: PestanaFisioterapeuta,
    onCambiarPestana: (PestanaFisioterapeuta) -> Unit,
    topBar: @Composable (onAlternarMenu: () -> Unit) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    var menuVisible by remember { mutableStateOf(true) }

    Row(modifier = Modifier.fillMaxSize()) {
        if (menuVisible) {
            NavigationRail {
                NavigationRailItem(
                    selected = pestanaActual == PestanaFisioterapeuta.PACIENTES,
                    onClick = {
                        onCambiarPestana(PestanaFisioterapeuta.PACIENTES)
                        menuVisible = false
                    },
                    icon = { Icon(Icons.Filled.People, contentDescription = null) },
                    label = { Text("Pacientes") },
                )
                NavigationRailItem(
                    selected = pestanaActual == PestanaFisioterapeuta.EJERCICIOS,
                    onClick = {
                        onCambiarPestana(PestanaFisioterapeuta.EJERCICIOS)
                        menuVisible = false
                    },
                    icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = null) },
                    label = { Text("Ejercicios") },
                )
            }
        }
        Scaffold(
            modifier = Modifier.weight(1f),
            topBar = { topBar { menuVisible = !menuVisible } },
        ) { padding -> content(padding) }
    }
}
