package com.sanna.rehabapp.domain.model

import java.util.Date

data class Sesion(
    val id: String = "",
    val ejercicioId: String,
    val fisioterapeutaId: String,
    val fechaAsignacion: Date? = null,
    val fechaEjecucion: Date? = null,
    val estado: EstadoSesion = EstadoSesion.PENDIENTE,
    val resultado: ResultadoSesion? = null,
    val sincronizado: Boolean = true,
)
