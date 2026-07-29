package com.sanna.rehabapp.domain.model

import java.util.Date

data class Usuario(
    val uid: String,
    val nombre: String,
    val email: String,
    val rol: Rol,
    val fisioterapeutaId: String? = null,
    val fechaRegistro: Date? = null,
)
