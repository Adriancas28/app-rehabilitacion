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
// Los frames se agrupan por repetición (HU06-CA06) para poder saber, en
// HU11, cuántas repeticiones no tuvieron ningún error.
class ProcesadorMovimiento(private val ejercicio: Ejercicio) {

    private val medicionesPorRepeticion = mutableListOf<MutableList<List<MedicionArticulacion>>>()

    // Se llama al empezar cada repetición del ciclo de monitoreo (HU06-CA06).
    fun marcarNuevaRepeticion() {
        medicionesPorRepeticion.add(mutableListOf())
    }

    fun procesarResultado(resultado: PoseLandmarkerResult) {
        val bucketRepeticionActual = medicionesPorRepeticion.lastOrNull() ?: return
        // Sin persona detectada en este frame: se ignora sin interrumpir el
        // procesamiento (RNF05-CA02/CA03), no se cuenta como frame medido.
        val landmarks = resultado.landmarks().firstOrNull() ?: return
        val mediciones = ejercicio.patronesReferencia.mapNotNull { patron -> medirArticulacion(patron, landmarks) }
        bucketRepeticionActual.add(mediciones)
    }

    // HU06-CA07: repeticionesCompletadas puede ser menor a las asignadas si
    // el paciente finalizó antes de tiempo — solo se cuentan como
    // "correctas" las repeticiones que sí llegaron a completarse.
    fun generarResultado(repeticionesCompletadas: Int, repeticionesAsignadas: Int): ResultadoSesion =
        construirResultadoSesion(
            medicionesPorRepeticion = medicionesPorRepeticion.take(repeticionesCompletadas),
            repeticionesCompletadas = repeticionesCompletadas,
            repeticionesAsignadas = repeticionesAsignadas,
        )

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
