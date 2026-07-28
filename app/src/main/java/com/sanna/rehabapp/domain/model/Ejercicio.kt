package com.sanna.rehabapp.domain.model

import java.util.Date

data class Ejercicio(
    val id: String = "",
    val nombre: String,
    val descripcion: String,
    val categoria: String,
    val materialUrl: String = "",
    val patronReferencia: PatronReferencia? = null,
    val creadoPor: String,
    val fechaCreacion: Date? = null,
    val activo: Boolean = true,
)
