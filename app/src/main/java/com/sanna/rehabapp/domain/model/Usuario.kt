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
    val fechaRegistro: Date? = null,
)
