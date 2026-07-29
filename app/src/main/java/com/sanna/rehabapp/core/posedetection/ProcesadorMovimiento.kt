package com.sanna.rehabapp.core.posedetection

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.model.PatronReferencia
import com.sanna.rehabapp.domain.model.ResultadoSesion

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
        // HU08-CA03: si la articulación está ocluida/fuera de cuadro en
        // este frame puntual, medirAnguloDeArticulacion devuelve null y se
        // salta sin interrumpir el procesamiento del resto.
        val angulo = medirAnguloDeArticulacion(patron.articulacion, landmarks) ?: return null
        return MedicionArticulacion(
            articulacion = patron.articulacion,
            angulo = angulo,
            anguloMin = patron.anguloMin,
            anguloMax = patron.anguloMax,
        )
    }
}
