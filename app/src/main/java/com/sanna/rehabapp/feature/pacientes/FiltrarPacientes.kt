package com.sanna.rehabapp.feature.pacientes

import com.sanna.rehabapp.domain.model.Usuario

// HU01-CA04: filtra la lista de pacientes por nombre o correo.
fun filtrarPacientes(pacientes: List<Usuario>, consulta: String): List<Usuario> {
    if (consulta.isBlank()) return pacientes
    val consultaNormalizada = consulta.trim().lowercase()
    return pacientes.filter {
        it.nombre.lowercase().contains(consultaNormalizada) ||
            it.email.lowercase().contains(consultaNormalizada)
    }
}
