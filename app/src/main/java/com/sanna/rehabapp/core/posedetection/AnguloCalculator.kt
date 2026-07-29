package com.sanna.rehabapp.core.posedetection

import kotlin.math.acos
import kotlin.math.sqrt

// Un landmark de MediaPipe reducido a lo que hace falta para geometría —
// sin depender del tipo real de MediaPipe, para que esta función sea pura
// y testeable con JUnit normal (sin Android ni el SDK de MediaPipe).
data class Punto3D(val x: Float, val y: Float, val z: Float = 0f)

// HU08-CA01: ángulo (en grados, 0-180) formado en `vertice` por los
// segmentos vertice→inicio y vertice→fin, vía el producto punto entre
// ambos vectores. Usa x/y/z (no solo el plano de la cámara) para que el
// ángulo sea razonable aunque el paciente no esté perfectamente de frente.
fun calcularAngulo(inicio: Punto3D, vertice: Punto3D, fin: Punto3D): Float {
    val v1x = inicio.x - vertice.x
    val v1y = inicio.y - vertice.y
    val v1z = inicio.z - vertice.z
    val v2x = fin.x - vertice.x
    val v2y = fin.y - vertice.y
    val v2z = fin.z - vertice.z

    val magnitud1 = sqrt(v1x * v1x + v1y * v1y + v1z * v1z)
    val magnitud2 = sqrt(v2x * v2x + v2y * v2y + v2z * v2z)
    if (magnitud1 == 0f || magnitud2 == 0f) return 0f

    val productoPunto = v1x * v2x + v1y * v2y + v1z * v2z
    val coseno = (productoPunto / (magnitud1 * magnitud2)).coerceIn(-1f, 1f)
    return Math.toDegrees(acos(coseno).toDouble()).toFloat()
}
