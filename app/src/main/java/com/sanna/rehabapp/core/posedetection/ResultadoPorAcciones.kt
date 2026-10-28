package com.sanna.rehabapp.core.posedetection

import com.sanna.rehabapp.domain.model.Accion
import com.sanna.rehabapp.domain.model.AnguloDetectado
import com.sanna.rehabapp.domain.model.DetalleRepeticion
import com.sanna.rehabapp.domain.model.ErrorDetectado
import com.sanna.rehabapp.domain.model.ResultadoAccion
import com.sanna.rehabapp.domain.model.ResultadoSesion
import kotlin.math.abs

private const val TIPO_RANGO_INCOMPLETO = "Rango incompleto"
private const val TIPO_MANTENIMIENTO_INCOMPLETO = "Mantenimiento incompleto"

// Puntaje de una acción (0–100): completada = 100; si no, qué tan cerca de la meta
// llegó respecto del recorrido total, para que un casi-logro no valga igual que no moverse.
fun puntajeDeAccion(accion: Accion, resultado: ResultadoAccion): Float {
    if (resultado.completada) return 100f
    val recorrido = abs(accion.anguloMeta - accion.anguloInicio)
    if (recorrido == 0f) return 0f
    val faltante = abs(resultado.anguloMaximo - accion.anguloMeta)
    return ((1f - faltante / recorrido).coerceIn(0f, 1f)) * 100f
}

// HU08-CA04/HU09-CA03/HU09-CA04: arma el ResultadoSesion a partir de los resultados
// por acción de cada repetición cerrada. El porcentaje de cada repetición es el
// promedio de los puntajes de sus acciones y el de la sesión, el de sus repeticiones.
fun construirResultadoPorAcciones(
    acciones: List<Accion>,
    repeticiones: List<List<ResultadoAccion>>,
    repeticionesCompletadas: Int,
    repeticionesAsignadas: Int,
    numeroRepeticionInicial: Int = 1,
): ResultadoSesion {
    val porOrden = acciones.associateBy { it.orden }
    if (repeticiones.isEmpty()) {
        return ResultadoSesion(repeticionesCompletadas = repeticionesCompletadas, repeticionesAsignadas = repeticionesAsignadas)
    }

    val detalle = repeticiones.mapIndexed { indice, resultados ->
        val puntajes = resultados.mapNotNull { r -> porOrden[r.ordenAccion]?.let { puntajeDeAccion(it, r) } }
        val errores = resultados.filterNot { it.completada }.mapNotNull { r ->
            val accion = porOrden[r.ordenAccion] ?: return@mapNotNull null
            ErrorDetectado(
                articulacion = accion.nombre,
                tipo = if (r.desviacionAngular == 0f) TIPO_MANTENIMIENTO_INCOMPLETO else TIPO_RANGO_INCOMPLETO,
                repeticiones = 1,
                anguloDetectado = r.anguloMaximo,
                anguloEsperado = accion.anguloMeta,
                segundo = r.tiempoSeg.toInt(),
            )
        }
        DetalleRepeticion(
            numero = indice + numeroRepeticionInicial,
            porcentajeEjecucion = if (puntajes.isEmpty()) 0f else puntajes.average().toFloat(),
            errores = errores,
            acciones = resultados,
        )
    }

    val todos = repeticiones.flatten()
    val angulos = todos.groupBy { it.ordenAccion }.mapNotNull { (orden, lista) ->
        val accion = porOrden[orden] ?: return@mapNotNull null
        AnguloDetectado(
            articulacion = accion.nombre,
            anguloDetectado = lista.map { it.anguloMaximo }.average().toFloat(),
            anguloEsperado = accion.anguloMeta,
            desviacion = lista.map { it.desviacionAngular }.average().toFloat(),
        )
    }
    val erroresAgregados = detalle.flatMap { it.errores }
        .groupBy { it.articulacion to it.tipo }
        .map { (clave, lista) -> ErrorDetectado(articulacion = clave.first, tipo = clave.second, repeticiones = lista.size) }

    return ResultadoSesion(
        angulosDetectados = angulos,
        desviacionPromedio = todos.map { it.desviacionAngular }.average().toFloat(),
        porcentajeEjecucion = detalle.map { it.porcentajeEjecucion }.average().toFloat(),
        erroresDetectados = erroresAgregados,
        repeticionesCompletadas = repeticionesCompletadas,
        repeticionesAsignadas = repeticionesAsignadas,
        repeticionesCorrectas = repeticiones.count { rep -> rep.isNotEmpty() && rep.all { it.completada } },
        detallePorRepeticion = detalle,
    )
}
