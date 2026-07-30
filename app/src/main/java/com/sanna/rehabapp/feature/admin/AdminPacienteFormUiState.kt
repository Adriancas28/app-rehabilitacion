package com.sanna.rehabapp.feature.admin

import com.sanna.rehabapp.domain.model.TipoDiagnostico

// HU20-CA02 (revisión): el registro de un paciente captura además DNI, edad
// y diagnóstico — datos que el fisioterapeuta no necesita (ver
// AdminUsuarioFormUiState), por eso este formulario tiene su propio estado.
data class AdminPacienteFormUiState(
    val nombre: String = "",
    val email: String = "",
    val password: String = "",
    val dni: String = "",
    val edad: String = "",
    val tipoDiagnostico: TipoDiagnostico? = null,
    val cargando: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null,
    val guardadoExitoso: Boolean = false,
)
