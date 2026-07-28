package com.sanna.rehabapp.core.navigation

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
            icon = {},
            label = { Text("Pacientes") },
        )
        NavigationBarItem(
            selected = pestanaActual == PestanaFisioterapeuta.EJERCICIOS,
            onClick = { onCambiarPestana(PestanaFisioterapeuta.EJERCICIOS) },
            icon = {},
            label = { Text("Ejercicios") },
        )
    }
}
