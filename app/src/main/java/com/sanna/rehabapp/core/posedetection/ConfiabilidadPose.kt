package com.sanna.rehabapp.core.posedetection

// HU07 (guía visual): un cuadro es confiable para una acción solo si TODOS los
// landmarks que esa acción usa están presentes y con visibilidad suficiente.
// Un cuadro no confiable no hace avanzar ni penaliza la acción: se le pide al
// paciente reubicarse.
const val VISIBILIDAD_MINIMA_CONFIABLE = 0.5f

fun LandmarksFrame.esConfiable(
    requeridos: Collection<Int>,
    visibilidadMinima: Float = VISIBILIDAD_MINIMA_CONFIABLE,
): Boolean = requeridos.all { indice ->
    val punto = this[indice]
    punto != null && punto.visibilidad >= visibilidadMinima
}
