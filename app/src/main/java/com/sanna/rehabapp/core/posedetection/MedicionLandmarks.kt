package com.sanna.rehabapp.core.posedetection

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.sanna.rehabapp.domain.model.Articulacion

private const val VISIBILIDAD_MINIMA = 0.5f

// Punto en común entre HU07/HU08 (medición en vivo, ProcesadorMovimiento) y
// HU02-CA07 (análisis de un video de referencia grabado,
// AnalizadorVideoReferencia): dado un listado de landmarks de un frame,
// calcula el ángulo de una articulación puntual, o null si está
// ocluida/fuera de cuadro en ese frame (HU08-CA03).
fun medirAnguloDeArticulacion(articulacion: Articulacion, landmarks: List<NormalizedLandmark>): Float? {
    val inicio = landmarks.getOrNull(articulacion.puntoInicial) ?: return null
    val vertice = landmarks.getOrNull(articulacion.vertice) ?: return null
    val fin = landmarks.getOrNull(articulacion.puntoFinal) ?: return null

    if (vertice.visibility().orElse(0f) < VISIBILIDAD_MINIMA) return null

    return calcularAngulo(
        inicio = Punto3D(inicio.x(), inicio.y(), inicio.z()),
        vertice = Punto3D(vertice.x(), vertice.y(), vertice.z()),
        fin = Punto3D(fin.x(), fin.y(), fin.z()),
    )
}
