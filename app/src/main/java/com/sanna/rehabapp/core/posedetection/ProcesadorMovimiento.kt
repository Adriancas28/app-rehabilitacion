package com.sanna.rehabapp.core.posedetection

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.sanna.rehabapp.domain.model.Articulacion
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.model.LadoAfectado
import com.sanna.rehabapp.domain.model.PatronReferencia
import com.sanna.rehabapp.domain.model.ResultadoSesion

// HU08 — recibe cada resultado de MediaPipe durante la ejecución de un
// ejercicio, mide las articulaciones definidas en su patronesReferencia, y
// al finalizar (HU08-CA04) arma el ResultadoSesion agregado. Es el puente
// entre el mundo de MediaPipe y la lógica pura de MedicionArticulacion.
// Los frames se agrupan por repetición (HU06-CA06) para poder saber, en
// HU11, cuántas repeticiones no tuvieron ningún error.
//
// HU20 (ampliación): patronesReferencia siempre está definido sobre el lado
// derecho (convención de autoría del catálogo); si el lado afectado real
// del paciente es el izquierdo, se mide la articulación espejo en su lugar
// (ver Articulacion.espejo()). Con AMBOS se miden los dos lados en cada
// frame: el "activo" es el que tenga mayor ángulo (el que se está
// levantando en ese instante); si los dos superan anguloMin a la vez, el
// paciente está moviendo ambos brazos juntos en un ejercicio pensado para
// alternar uno por uno, y se reporta como "Movimiento simultáneo" en vez
// de una comparación de rango normal.
class ProcesadorMovimiento(
    private val ejercicio: Ejercicio,
    private val ladoAfectado: LadoAfectado = LadoAfectado.DERECHO,
) {

    private val medicionesPorRepeticion = mutableListOf<MutableList<List<MedicionArticulacion>>>()

    // Se llama al empezar cada repetición del ciclo de monitoreo (HU06-CA06).
    fun marcarNuevaRepeticion() {
        medicionesPorRepeticion.add(mutableListOf())
    }

    // HU10 — devuelve las mediciones de ESTE frame (además de seguir
    // acumulándolas internamente) para que quien la llama pueda reaccionar
    // en vivo (retroalimentación visual/por voz), sin duplicar la lógica
    // de medición.
    fun procesarResultado(resultado: PoseLandmarkerResult): List<MedicionArticulacion> {
        val bucketRepeticionActual = medicionesPorRepeticion.lastOrNull() ?: return emptyList()
        // Sin persona detectada en este frame: se ignora sin interrumpir el
        // procesamiento (RNF05-CA02/CA03), no se cuenta como frame medido.
        val landmarks = resultado.landmarks().firstOrNull() ?: return emptyList()
        val mediciones = ejercicio.patronesReferencia.mapNotNull { patron -> medirArticulacion(patron, landmarks) }
        bucketRepeticionActual.add(mediciones)
        return mediciones
    }

    // HU06-CA07: repeticionesCompletadas puede ser menor a las asignadas si
    // el paciente finalizó antes de tiempo — solo se cuentan como
    // "correctas" las repeticiones que sí llegaron a completarse.
    // HU06-CA09: numeroRepeticionInicial es distinto de 1 cuando se está
    // reanudando una sesión ya finalizada antes — así el detalle por
    // repetición numera las repeticiones nuevas con su número real (ej.
    // 2, 3), no reinicia desde 1 como si fueran las primeras.
    fun generarResultado(
        repeticionesCompletadas: Int,
        repeticionesAsignadas: Int,
        numeroRepeticionInicial: Int = 1,
    ): ResultadoSesion {
        val repeticionesMedidasEnEstaEjecucion = repeticionesCompletadas - numeroRepeticionInicial + 1
        return construirResultadoSesion(
            medicionesPorRepeticion = medicionesPorRepeticion.take(repeticionesMedidasEnEstaEjecucion.coerceAtLeast(0)),
            repeticionesCompletadas = repeticionesCompletadas,
            repeticionesAsignadas = repeticionesAsignadas,
            numeroRepeticionInicial = numeroRepeticionInicial,
        )
    }

    private fun medirArticulacion(
        patron: PatronReferencia,
        landmarks: List<NormalizedLandmark>,
    ): MedicionArticulacion? = when (ladoAfectado) {
        LadoAfectado.IZQUIERDO -> medirLadoFijo(patron, patron.articulacion.espejo(), landmarks)
        LadoAfectado.AMBOS -> medirAmbosLados(patron, landmarks)
        LadoAfectado.DERECHO -> medirLadoFijo(patron, patron.articulacion, landmarks)
    }

    private fun medirLadoFijo(
        patron: PatronReferencia,
        articulacionAMedir: Articulacion,
        landmarks: List<NormalizedLandmark>,
    ): MedicionArticulacion? {
        // HU08-CA03: si la articulación está ocluida/fuera de cuadro en
        // este frame puntual, medirAnguloDeArticulacion devuelve null y se
        // salta sin interrumpir el procesamiento del resto.
        val angulo = medirAnguloDeArticulacion(articulacionAMedir, landmarks) ?: return null
        return MedicionArticulacion(
            articulacion = articulacionAMedir,
            angulo = angulo,
            anguloMin = patron.anguloMin,
            anguloMax = patron.anguloMax,
        )
    }

    // HU20 (ampliación) — ejercicios alternados (ej. HOM-01 con "Ambos"
    // brazos): mide los dos lados en el mismo frame y reporta solo el
    // activo (el de mayor ángulo). anguloMin del propio patrón hace de
    // umbral de "está intentando levantarlo" — no hace falta un valor
    // aparte. Si los dos superan ese umbral a la vez, se fuerza el motivo
    // de error "Movimiento simultáneo" en vez de comparar contra el rango.
    private fun medirAmbosLados(
        patron: PatronReferencia,
        landmarks: List<NormalizedLandmark>,
    ): MedicionArticulacion? {
        val anguloDerecho = medirAnguloDeArticulacion(patron.articulacion, landmarks)
        val anguloIzquierdo = medirAnguloDeArticulacion(patron.articulacion.espejo(), landmarks)
        if (anguloDerecho == null && anguloIzquierdo == null) return null

        val derechoElevado = anguloDerecho != null && anguloDerecho > patron.anguloMin
        val izquierdoElevado = anguloIzquierdo != null && anguloIzquierdo > patron.anguloMin

        val (articulacionActiva, anguloActivo) = if ((anguloDerecho ?: Float.NEGATIVE_INFINITY) >= (anguloIzquierdo ?: Float.NEGATIVE_INFINITY)) {
            patron.articulacion to anguloDerecho!!
        } else {
            patron.articulacion.espejo() to anguloIzquierdo!!
        }

        return MedicionArticulacion(
            articulacion = articulacionActiva,
            angulo = anguloActivo,
            anguloMin = patron.anguloMin,
            anguloMax = patron.anguloMax,
            motivoErrorForzado = "Movimiento simultáneo".takeIf { derechoElevado && izquierdoElevado },
        )
    }
}
