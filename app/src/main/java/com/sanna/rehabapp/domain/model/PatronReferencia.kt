package com.sanna.rehabapp.domain.model

// HU08-CA01/CA02: un ejercicio puede involucrar varias articulaciones (ej.
// rodilla y cadera en una sentadilla), cada una con su propio ROM esperado.
data class PatronReferencia(
    val articulacion: String,
    val anguloMin: Float,
    val anguloMax: Float,
)
