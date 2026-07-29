package com.sanna.rehabapp.core.posedetection

import com.sanna.rehabapp.domain.model.AnguloDetectado
import com.sanna.rehabapp.domain.model.ErrorDetectado
import com.sanna.rehabapp.domain.model.ResultadoSesion

// HU06-CA09 — combina el resultado ya guardado de una sesión finalizada
// antes de tiempo con lo medido al reanudarla, para que el resultado final
// refleje TODAS las repeticiones (las de antes y las nuevas), no solo el
// último tramo. Los promedios/porcentajes globales se ponderan por cuántas
// repeticiones aportó cada tramo — no hay forma de promediar frame a frame
// porque los frames crudos del tramo anterior ya no están (solo se guardan
// datos numéricos agregados, RNF06).
fun mergearResultados(anterior: ResultadoSesion, nuevo: ResultadoSesion): ResultadoSesion {
    if (anterior.detallePorRepeticion.isEmpty()) return nuevo
    // Se reanudó pero no se completó ninguna repetición nueva antes de
    // volver a finalizar: no hay nada que combinar.
    if (nuevo.detallePorRepeticion.isEmpty()) return anterior

    val pesoAnterior = anterior.detallePorRepeticion.size
    val pesoNuevo = nuevo.detallePorRepeticion.size

    return ResultadoSesion(
        angulosDetectados = mergearAngulos(anterior.angulosDetectados, nuevo.angulosDetectados, pesoAnterior, pesoNuevo),
        desviacionPromedio = promedioPonderado(anterior.desviacionPromedio, pesoAnterior, nuevo.desviacionPromedio, pesoNuevo),
        porcentajeEjecucion = promedioPonderado(anterior.porcentajeEjecucion, pesoAnterior, nuevo.porcentajeEjecucion, pesoNuevo),
        erroresDetectados = mergearErrores(anterior.erroresDetectados, nuevo.erroresDetectados),
        repeticionesCompletadas = nuevo.repeticionesCompletadas,
        repeticionesAsignadas = nuevo.repeticionesAsignadas,
        repeticionesCorrectas = anterior.repeticionesCorrectas + nuevo.repeticionesCorrectas,
        detallePorRepeticion = anterior.detallePorRepeticion + nuevo.detallePorRepeticion,
    )
}

private fun promedioPonderado(a: Float, pesoA: Int, b: Float, pesoB: Int): Float =
    (a * pesoA + b * pesoB) / (pesoA + pesoB)

private fun mergearAngulos(
    anterior: List<AnguloDetectado>,
    nuevo: List<AnguloDetectado>,
    pesoAnterior: Int,
    pesoNuevo: Int,
): List<AnguloDetectado> {
    val articulaciones = (anterior.map { it.articulacion } + nuevo.map { it.articulacion }).distinct()
    return articulaciones.map { articulacion ->
        val a = anterior.find { it.articulacion == articulacion }
        val n = nuevo.find { it.articulacion == articulacion }
        when {
            a != null && n != null -> AnguloDetectado(
                articulacion = articulacion,
                anguloDetectado = promedioPonderado(a.anguloDetectado, pesoAnterior, n.anguloDetectado, pesoNuevo),
                anguloEsperado = a.anguloEsperado ?: n.anguloEsperado,
                desviacion = promedioPonderado(a.desviacion ?: 0f, pesoAnterior, n.desviacion ?: 0f, pesoNuevo),
            )
            a != null -> a
            else -> requireNotNull(n)
        }
    }
}

private fun mergearErrores(anterior: List<ErrorDetectado>, nuevo: List<ErrorDetectado>): List<ErrorDetectado> =
    (anterior + nuevo)
        .groupBy { it.articulacion to it.tipo }
        .map { (clave, errores) ->
            ErrorDetectado(articulacion = clave.first, tipo = clave.second, repeticiones = errores.sumOf { it.repeticiones })
        }
