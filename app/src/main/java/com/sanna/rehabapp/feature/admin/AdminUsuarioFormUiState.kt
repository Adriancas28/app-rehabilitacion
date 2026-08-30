package com.sanna.rehabapp.feature.admin

import com.sanna.rehabapp.domain.model.Genero

// Formulario de fisioterapeuta del panel de administrador (HU21). El de
// paciente tiene su propio estado (AdminPacienteFormUiState) por necesitar
// campos clínicos (DNI, diagnósticos, lado afectado) que este no tiene.
// HU21-CA02/CA03 (actualización del modelo de datos): edad, género y
// número de contacto son obligatorios; especialidad y número de
// colegiatura son opcionales.
data class AdminUsuarioFormUiState(
    val nombre: String = "",
    val email: String = "",
    val password: String = "",
    val edad: String = "",
    val genero: Genero? = null,
    val numeroContacto: String = "",
    val especialidad: String = "",
    val numeroColegiatura: String = "",
    val cargando: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null,
    val guardadoExitoso: Boolean = false,
)
