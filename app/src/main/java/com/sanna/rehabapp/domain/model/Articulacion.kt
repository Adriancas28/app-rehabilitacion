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
    CADERA_DERECHA("Cadera derecha", puntoInicial = 12, vertice = 24, puntoFinal = 26),

    // Ángulo cadera-hombro-nariz (24-12-0): se abre al levantar el rostro
    // (mirar hacia arriba) y se cierra al bajarlo (mentón al pecho). Más
    // fácil de ejecutar solo frente a la cámara que los ejercicios de
    // pierna/brazo — útil para pruebas rápidas de HU06/07/08/10.
    CUELLO("Cuello", puntoInicial = 24, vertice = 12, puntoFinal = 0),

    // Ángulo rodilla-tobillo-punta del pie: se abre en flexión dorsal
    // (punta del pie hacia arriba) y se cierra en flexión plantar (punta
    // hacia abajo) — catálogo de ejercicios predeterminados, sección 7/8.
    TOBILLO_IZQUIERDO("Tobillo izquierdo", puntoInicial = 25, vertice = 27, puntoFinal = 31),
    TOBILLO_DERECHO("Tobillo derecho", puntoInicial = 26, vertice = 28, puntoFinal = 32),

    // Ángulo hombro-cadera-hombro (11-24-12): aproximación 2D de la
    // rotación/inclinación de tronco vista de frente — no es goniometría
    // exacta de rotación axial (eso requeriría profundidad/3D), pero
    // cambia de forma consistente con el movimiento del ejercicio
    // "Rotación de tronco (sentado)" del catálogo predeterminado, mismo
    // criterio pragmático que ya se usó para CUELLO.
    TRONCO("Tronco", puntoInicial = 11, vertice = 24, puntoFinal = 12);

    fun aFirestore(): String = name

    companion object {
        // Documentos de ejercicios previos a este cambio pueden tener
        // articulacion como texto libre; se descartan en el mapeo (ver
        // EjercicioRepositoryImpl) en vez de lanzar una excepción.
        fun desdeFirestoreOrNull(valor: String?): Articulacion? = entries.find { it.name == valor }
    }
}
