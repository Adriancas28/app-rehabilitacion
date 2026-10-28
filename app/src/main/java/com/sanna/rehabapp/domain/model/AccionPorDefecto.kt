package com.sanna.rehabapp.domain.model

// HU02-CA19: los ejercicios registrados antes de la guía visual no tienen
// acciones. Se tratan como un ejercicio de una única acción generada a partir
// de su rango de referencia. Es solo un respaldo: los ejercicios del catálogo
// llevan acciones explícitas (ver backend/seed).
fun Ejercicio.accionesEfectivas(): List<Accion> =
    if (acciones.isNotEmpty()) {
        acciones.sortedBy { it.orden }
    } else {
        patronesReferencia.mapIndexed { indice, patron -> patron.aAccionPorDefecto(orden = indice + 1) }
    }

private const val MANTENER_POR_DEFECTO_SEG = 2
private const val TIEMPO_MAX_POR_DEFECTO_SEG = 10

// Meta = punto medio del rango; tolerancia = mitad de su ancho. El ángulo de
// inicio es una aproximación: 0° si la meta es menor o igual a 90° y 180° si es
// mayor (brazo caído / articulación extendida); depende de cada ejercicio.
fun PatronReferencia.aAccionPorDefecto(orden: Int = 1): Accion {
    val meta = (anguloMin + anguloMax) / 2f
    val roles = articulacion.roles()
    return Accion(
        orden = orden,
        nombre = articulacion.etiqueta,
        puntoReferencia = roles.referencia,
        pivote = articulacion.vertice,
        puntoDistal = roles.distal,
        lado = articulacion.lado(),
        anguloInicio = if (meta <= 90f) 0f else 180f,
        anguloMeta = meta,
        tolerancia = ((anguloMax - anguloMin) / 2f).coerceAtLeast(1f),
        mantenerSeg = MANTENER_POR_DEFECTO_SEG,
        tiempoMaxSeg = TIEMPO_MAX_POR_DEFECTO_SEG,
    )
}
