package com.sanna.rehabapp.core.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class PestanaFisioterapeuta { PACIENTES, EJERCICIOS }

// El fisioterapeuta navega por una barra lateral izquierda (como el
// mockup de referencia), no un menú inferior — a diferencia del paciente,
// que no tiene esta barra (solo tiene un home por ahora, HU04 en Sprint 2).
@Composable
fun FisioterapeutaScaffold(
    pestanaActual: PestanaFisioterapeuta,
    onCambiarPestana: (PestanaFisioterapeuta) -> Unit,
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail {
            NavigationRailItem(
                selected = pestanaActual == PestanaFisioterapeuta.PACIENTES,
                onClick = { onCambiarPestana(PestanaFisioterapeuta.PACIENTES) },
                icon = { Icon(Icons.Filled.People, contentDescription = null) },
                label = { Text("Pacientes") },
            )
            NavigationRailItem(
                selected = pestanaActual == PestanaFisioterapeuta.EJERCICIOS,
                onClick = { onCambiarPestana(PestanaFisioterapeuta.EJERCICIOS) },
                icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = null) },
                label = { Text("Ejercicios") },
            )
        }
        Scaffold(
            modifier = Modifier.weight(1f),
            topBar = topBar,
        ) { padding -> content(padding) }
    }
}
