package com.sanna.rehabapp.domain.model

// HU08-CA01: cada articulación se mapea a una tripleta de landmarks de
// MediaPipe Pose Landmarker (33 puntos, índices oficiales de BlazePose) —
// puntoInicial-vertice-puntoFinal — para poder calcular el ángulo real
// entre esos tres puntos durante la ejecución del ejercicio (Sprint 3).
// Antes de esto `articulacion` era texto libre (Sprint 1/2): no alcanzaba
// para saber qué landmarks corresponden a cada patrón de referencia.
enum class Articulacion(
    val etiqueta: String,
    val puntoInicial: Int,
    val vertice: Int,
    val puntoFinal: Int,
) {
    RODILLA_IZQUIERDA("Rodilla izquierda", puntoInicial = 23, vertice = 25, puntoFinal = 27),
    RODILLA_DERECHA("Rodilla derecha", puntoInicial = 24, vertice = 26, puntoFinal = 28),
    CODO_IZQUIERDO("Codo izquierdo", puntoInicial = 11, vertice = 13, puntoFinal = 15),
    CODO_DERECHO("Codo derecho", puntoInicial = 12, vertice = 14, puntoFinal = 16),
    HOMBRO_IZQUIERDO("Hombro izquierdo", puntoInicial = 13, vertice = 11, puntoFinal = 23),
    HOMBRO_DERECHO("Hombro derecho", puntoInicial = 14, vertice = 12, puntoFinal = 24),
    CADERA_IZQUIERDA("Cadera izquierda", puntoInicial = 11, vertice = 23, puntoFinal = 25),
    CADERA_DERECHA("Cadera derecha", puntoInicial = 12, vertice = 24, puntoFinal = 26);

    fun aFirestore(): String = name

    companion object {
        // Documentos de ejercicios previos a este cambio pueden tener
        // articulacion como texto libre; se descartan en el mapeo (ver
        // EjercicioRepositoryImpl) en vez de lanzar una excepción.
        fun desdeFirestoreOrNull(valor: String?): Articulacion? = entries.find { it.name == valor }
    }
}
