package com.sanna.rehabapp.domain.model

import java.util.Date

data class Sesion(
    val id: String = "",
    // HU18-CA01 (Sprint 5): solo se completa cuando la sesión viene de una
    // consulta agregada entre pacientes (collection group) — las consultas
    // de un paciente puntual ya lo conocen por fuera, no lo necesitan aquí.
    val pacienteId: String? = null,
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
