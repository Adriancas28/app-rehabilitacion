package com.sanna.rehabapp.core.posedetection

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.model.PatronReferencia
import com.sanna.rehabapp.domain.model.ResultadoSesion

private const val VISIBILIDAD_MINIMA = 0.5f

// HU08 — recibe cada resultado de MediaPipe durante la ejecución de un
// ejercicio, mide las articulaciones definidas en su patronesReferencia, y
// al finalizar (HU08-CA04) arma el ResultadoSesion agregado. Es el puente
// entre el mundo de MediaPipe y la lógica pura de MedicionArticulacion.
class ProcesadorMovimiento(private val ejercicio: Ejercicio) {

    private val medicionesPorFrame = mutableListOf<List<MedicionArticulacion>>()

    fun procesarResultado(resultado: PoseLandmarkerResult) {
        // Sin persona detectada en este frame: se ignora sin interrumpir el
        // procesamiento (RNF05-CA02/CA03), no se cuenta como frame medido.
        val landmarks = resultado.landmarks().firstOrNull() ?: return
        val mediciones = ejercicio.patronesReferencia.mapNotNull { patron -> medirArticulacion(patron, landmarks) }
        medicionesPorFrame.add(mediciones)
    }

    fun generarResultado(): ResultadoSesion = construirResultadoSesion(medicionesPorFrame)

    private fun medirArticulacion(
        patron: PatronReferencia,
        landmarks: List<NormalizedLandmark>,
    ): MedicionArticulacion? {
        val articulacion = patron.articulacion
        val inicio = landmarks.getOrNull(articulacion.puntoInicial) ?: return null
        val vertice = landmarks.getOrNull(articulacion.vertice) ?: return null
        val fin = landmarks.getOrNull(articulacion.puntoFinal) ?: return null

        // HU08-CA03: oclusión temporal de un punto anatómico — si la
        // visibilidad del vértice es baja, se salta esta articulación en
        // este frame puntual sin interrumpir el procesamiento del resto.
        if (vertice.visibility().orElse(0f) < VISIBILIDAD_MINIMA) return null

        val angulo = calcularAngulo(
            inicio = Punto3D(inicio.x(), inicio.y(), inicio.z()),
            vertice = Punto3D(vertice.x(), vertice.y(), vertice.z()),
            fin = Punto3D(fin.x(), fin.y(), fin.z()),
        )
        return MedicionArticulacion(
            articulacion = articulacion,
            angulo = angulo,
            anguloMin = patron.anguloMin,
            anguloMax = patron.anguloMax,
        )
    }
}
