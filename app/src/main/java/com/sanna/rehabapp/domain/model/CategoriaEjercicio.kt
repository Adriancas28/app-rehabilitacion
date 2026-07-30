package com.sanna.rehabapp.domain.model

// Catálogo de tipos de ejercicio (ampliación acordada, no en la versión
// original): el alcance de la app es movilidad y control motor —
// MediaPipe Pose trackea articulaciones grandes del cuerpo, no motricidad
// fina de manos/dedos, así que un tercer tipo no debería agregarse por
// conveniencia sin antes revisar el alcance con el asesor de tesis.
enum class CategoriaEjercicio(val etiqueta: String) {
    MOVILIDAD("Movilidad"),
    CONTROL_MOTOR("Control motor");

    fun aFirestore(): String = name

    companion object {
        // Ejercicios creados antes de este cambio (texto libre) se
        // descartan en el mapeo en vez de lanzar una excepción, mismo
        // criterio que Articulacion/TipoDiagnostico.
        fun desdeFirestoreOrNull(valor: String?): CategoriaEjercicio? = entries.find { it.name == valor }
    }
}
