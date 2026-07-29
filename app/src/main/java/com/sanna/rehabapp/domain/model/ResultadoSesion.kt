package com.sanna.rehabapp.domain.model

// HU08-CA04: desglose del ángulo detectado por articulación frente a lo
// esperado (patronesReferencia del ejercicio), no un número suelto sin
// contexto de a qué articulación corresponde.
data class AnguloDetectado(
    val articulacion: String,
    val anguloDetectado: Float,
    val anguloEsperado: Float? = null,
    val desviacion: Float? = null,
)

// HU09-CA02 / HU10 (retroalimentación auditiva) — un error puntual detectado
// durante la ejecución, con cuántas veces se repitió. Si "repeticiones"
// supera un umbral se considera relevante para el fisioterapeuta (HU01/HU18).
data class ErrorDetectado(
    val articulacion: String,
    val tipo: String,
    val repeticiones: Int = 1,
)

// HU17-CA02 / RNF06-CA02: únicamente datos numéricos (ángulos, métricas).
// Nunca debe agregarse aquí una referencia a imagen o video.
// HU11-CA01/CA02 (revisión Sprint 3): además del % global, se guarda cuántas
// repeticiones se llegaron a completar (puede ser menos que las asignadas
// si se finalizó antes de tiempo, HU06-CA07) y cuántas de esas no tuvieron
// ningún error detectado.
data class ResultadoSesion(
    val angulosDetectados: List<AnguloDetectado> = emptyList(),
    val desviacionPromedio: Float = 0f,
    val porcentajeEjecucion: Float = 0f,
    val erroresDetectados: List<ErrorDetectado> = emptyList(),
    val repeticionesCompletadas: Int = 0,
    val repeticionesAsignadas: Int = 0,
    val repeticionesCorrectas: Int = 0,
)
