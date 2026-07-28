package com.sanna.rehabapp.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

enum class PestanaFisioterapeuta { PACIENTES, EJERCICIOS }

@Composable
fun FisioterapeutaBottomBar(
    pestanaActual: PestanaFisioterapeuta,
    onCambiarPestana: (PestanaFisioterapeuta) -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = pestanaActual == PestanaFisioterapeuta.PACIENTES,
            onClick = { onCambiarPestana(PestanaFisioterapeuta.PACIENTES) },
            icon = { Icon(Icons.Filled.People, contentDescription = null) },
            label = { Text("Pacientes") },
        )
        NavigationBarItem(
            selected = pestanaActual == PestanaFisioterapeuta.EJERCICIOS,
            onClick = { onCambiarPestana(PestanaFisioterapeuta.EJERCICIOS) },
            icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = null) },
            label = { Text("Ejercicios") },
        )
    }
}
