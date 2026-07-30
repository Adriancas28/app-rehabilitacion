package com.sanna.rehabapp.domain.model

import java.util.Date

// HU01-CA06 (ampliación): un paciente puede tener más de un diagnóstico a
// la vez (ej. osteoartrosis de rodilla + desacondicionamiento general),
// cada uno con su propia fecha de registro.
data class DiagnosticoRegistrado(
    val tipo: TipoDiagnostico,
    val fecha: Date? = null,
)
