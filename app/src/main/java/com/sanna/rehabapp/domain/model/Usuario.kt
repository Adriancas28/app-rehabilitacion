package com.sanna.rehabapp.domain.model

import java.util.Date

data class Usuario(
    val uid: String,
    val nombre: String,
    val email: String,
    val rol: Rol,
    val fisioterapeutaId: String? = null,
    // HU01-CA06: solo aplica si rol == PACIENTE; lo elige el fisioterapeuta
    // de un catálogo cerrado (TipoDiagnostico), no texto libre.
    val tipoDiagnostico: TipoDiagnostico? = null,
    // HU20-CA02 (revisión): datos adicionales del paciente que el
    // administrador captura al registrarlo. Solo aplican si rol == PACIENTE.
    val dni: String? = null,
    val edad: Int? = null,
    val fechaRegistro: Date? = null,
)
