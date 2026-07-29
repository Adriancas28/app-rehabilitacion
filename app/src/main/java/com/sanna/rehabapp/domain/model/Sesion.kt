package com.sanna.rehabapp.domain.model

import java.util.Date

data class Sesion(
    val id: String = "",
    val ejercicioId: String,
    val fisioterapeutaId: String,
    val fechaAsignacion: Date? = null,
    val fechaEjecucion: Date? = null,
    val estado: EstadoSesion = EstadoSesion.PENDIENTE,
    // HU03-CA05: nota opcional del fisioterapeuta sobre esta sesión puntual.
    val notas: String? = null,
    // HU03-CA06: override opcional de las repeticiones del ejercicio, solo
    // para esta sesión. Si es null, se usa Ejercicio.repeticiones.
    val repeticiones: Int? = null,
    val resultado: ResultadoSesion? = null,
    val sincronizado: Boolean = true,
)
