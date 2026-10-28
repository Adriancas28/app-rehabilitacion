package com.sanna.rehabapp.core.posedetection

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

// Guía visual en vivo: un landmark reducido a lo que hace falta para geometría
// y dibujo, sin depender del tipo real de MediaPipe (así la lógica es pura y
// se prueba con JUnit normal). x e y están normalizados (0..1) sobre la imagen
// analizada; z conserva la escala de MediaPipe.
data class PuntoPose(val x: Float, val y: Float, val z: Float = 0f, val visibilidad: Float = 1f)

// Un cuadro de pose: los 33 landmarks de la persona detectada y el tamaño de
// la imagen sobre la que están normalizados (ya rotada y espejada).
data class LandmarksFrame(
    val puntos: List<PuntoPose>,
    val anchoImagen: Int,
    val altoImagen: Int,
    val marcaMs: Long,
) {
    operator fun get(indice: Int): PuntoPose? = puntos.getOrNull(indice)
}

// Adaptador desde MediaPipe; devuelve null si no hay persona en el cuadro.
fun PoseLandmarkerResult.aFrame(anchoImagen: Int, altoImagen: Int): LandmarksFrame? {
    val landmarks = landmarks().firstOrNull() ?: return null
    return LandmarksFrame(
        puntos = landmarks.map { PuntoPose(it.x(), it.y(), it.z(), it.visibility().orElse(0f)) },
        anchoImagen = anchoImagen,
        altoImagen = altoImagen,
        marcaMs = timestampMs(),
    )
}

// Conexiones del esqueleto de BlazePose, para el modo de depuración.
val CONEXIONES_ESQUELETO: List<Pair<Int, Int>> = listOf(
    11 to 12, 11 to 13, 13 to 15, 12 to 14, 14 to 16,
    11 to 23, 12 to 24, 23 to 24,
    23 to 25, 25 to 27, 27 to 29, 29 to 31, 27 to 31,
    24 to 26, 26 to 28, 28 to 30, 30 to 32, 28 to 32,
    15 to 17, 15 to 19, 15 to 21, 16 to 18, 16 to 20, 16 to 22,
    0 to 11, 0 to 12,
)
