package com.sanna.rehabapp.domain.model

import java.util.Date

data class Ejercicio(
    val id: String = "",
    val nombre: String,
    val descripcion: String,
    val categoria: String,
    val materialUrl: String = "",
    val duracionSegundos: Int = 30,
    // HU02-CA08 — veces que se repite el ciclo de monitoreo (HU06-CA06)
    // dentro de una misma sesión; no son sesiones separadas.
    val repeticiones: Int = 1,
    val patronesReferencia: List<PatronReferencia> = emptyList(),
    val creadoPor: String,
    val fechaCreacion: Date? = null,
    val activo: Boolean = true,
)
