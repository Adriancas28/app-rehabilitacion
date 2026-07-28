package com.sanna.rehabapp.domain.model

// HU17-CA02 / RNF06-CA02: únicamente datos numéricos (ángulos, métricas).
// Nunca debe agregarse aquí una referencia a imagen o video.
data class ResultadoSesion(
    val angulosDetectados: List<Float> = emptyList(),
    val desviacionPromedio: Float = 0f,
    val porcentajeEjecucion: Float = 0f,
    val tipoError: String? = null,
)
