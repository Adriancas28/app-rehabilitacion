package com.sanna.rehabapp.core.posedetection

import com.sanna.rehabapp.domain.model.Articulacion
import com.sanna.rehabapp.domain.model.AnguloDetectado
import com.sanna.rehabapp.domain.model.ErrorDetectado
import com.sanna.rehabapp.domain.model.ResultadoSesion

// HU08-CA01/CA02: una medición puntual (de un frame) de una articulación,
// ya comparada contra su ROM esperado. Sin dependencias de MediaPipe —
// testeable con JUnit normal.
data class MedicionArticulacion(
    val articulacion: Articulacion,
    val angulo: Float,
    val anguloMin: Float,
    val anguloMax: Float,
) {
    val dentroDeRango: Boolean get() = angulo in anguloMin..anguloMax

    val desviacion: Float
        get() = when {
            angulo < anguloMin -> anguloMin - angulo
            angulo > anguloMax -> angulo - anguloMax
            else -> 0f
        }

    // HU09-CA02 (Sprint 4) hará una clasificación más completa; esta es la
    // mínima necesaria para poblar erroresDetectados al cerrar la sesión.
    val tipoDeError: String
        get() = when {
            angulo < anguloMin -> "Rango incompleto"
            angulo > anguloMax -> "Desviación angular"
            else -> "Desviación"
        }
}

// HU08-CA04: al finalizar la sesión, agrega todas las mediciones (agrupadas
// por repetición, y dentro de cada una por frame procesado) en el resumen
// que se guarda como ResultadoSesion.
fun construirResultadoSesion(
    medicionesPorRepeticion: List<List<List<MedicionArticulacion>>>,
    repeticionesCompletadas: Int,
    repeticionesAsignadas: Int,
): ResultadoSesion {
    val medicionesPorFrame = medicionesPorRepeticion.flatten()
    val todasLasMediciones = medicionesPorFrame.flatten()
    if (todasLasMediciones.isEmpty()) {
        return ResultadoSesion(
            repeticionesCompletadas = repeticionesCompletadas,
            repeticionesAsignadas = repeticionesAsignadas,
        )
    }

    val angulosDetectados = todasLasMediciones
        .groupBy { it.articulacion }
        .map { (articulacion, mediciones) ->
            AnguloDetectado(
                articulacion = articulacion.etiqueta,
                anguloDetectado = mediciones.map { it.angulo }.average().toFloat(),
                anguloEsperado = (mediciones.first().anguloMin + mediciones.first().anguloMax) / 2f,
                desviacion = mediciones.map { it.desviacion }.average().toFloat(),
            )
        }

    val erroresDetectados = todasLasMediciones
        .filterNot { it.dentroDeRango }
        .groupBy { it.articulacion to it.tipoDeError }
        .map { (clave, mediciones) ->
            ErrorDetectado(
                articulacion = clave.first.etiqueta,
                tipo = clave.second,
                repeticiones = mediciones.size,
            )
        }

    val framesDentroDeRango = medicionesPorFrame.count { frame -> frame.isNotEmpty() && frame.all { it.dentroDeRango } }
    val porcentajeEjecucion = framesDentroDeRango.toFloat() / medicionesPorFrame.size * 100f
    val desviacionPromedio = todasLasMediciones.map { it.desviacion }.average().toFloat()

    // HU11: una repetición cuenta como "correcta" si tuvo frames medidos y
    // todos ellos estuvieron dentro de rango en todas las articulaciones.
    val repeticionesCorrectas = medicionesPorRepeticion.count { frames ->
        frames.isNotEmpty() && frames.all { frame -> frame.isNotEmpty() && frame.all { it.dentroDeRango } }
    }

    return ResultadoSesion(
        angulosDetectados = angulosDetectados,
        desviacionPromedio = desviacionPromedio,
        porcentajeEjecucion = porcentajeEjecucion,
        erroresDetectados = erroresDetectados,
        repeticionesCompletadas = repeticionesCompletadas,
        repeticionesAsignadas = repeticionesAsignadas,
        repeticionesCorrectas = repeticionesCorrectas,
    )
}
