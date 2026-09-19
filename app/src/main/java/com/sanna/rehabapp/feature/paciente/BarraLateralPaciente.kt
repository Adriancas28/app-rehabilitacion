package com.sanna.rehabapp.feature.paciente

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Person
import com.sanna.rehabapp.core.navigation.ItemBarraLateral

enum class PestanaPaciente { EJERCICIOS, RESULTADOS, PROGRESO, PERFIL }

// Barra lateral del paciente, la misma en sus cuatro pestañas: Ejercicios,
// Resultados ("Mis resultados"), Progreso ("Mi progreso") y Perfil.
fun itemsBarraPaciente(
    actual: PestanaPaciente,
    onEjercicios: () -> Unit,
    onResultados: () -> Unit,
    onProgreso: () -> Unit,
    onPerfil: () -> Unit,
): List<ItemBarraLateral> = listOf(
    ItemBarraLateral(
        "Ejercicios",
        Icons.Rounded.FitnessCenter,
        seleccionado = actual == PestanaPaciente.EJERCICIOS,
        onClick = onEjercicios,
    ),
    ItemBarraLateral(
        "Resultados",
        Icons.Rounded.Assessment,
        seleccionado = actual == PestanaPaciente.RESULTADOS,
        onClick = onResultados,
    ),
    ItemBarraLateral(
        "Progreso",
        Icons.AutoMirrored.Rounded.TrendingUp,
        seleccionado = actual == PestanaPaciente.PROGRESO,
        onClick = onProgreso,
    ),
    ItemBarraLateral(
        "Perfil",
        Icons.Rounded.Person,
        seleccionado = actual == PestanaPaciente.PERFIL,
        onClick = onPerfil,
    ),
)
