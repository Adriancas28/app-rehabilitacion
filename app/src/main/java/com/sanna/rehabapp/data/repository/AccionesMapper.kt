package com.sanna.rehabapp.data.repository

import com.sanna.rehabapp.domain.model.Accion
import com.sanna.rehabapp.domain.model.LadoCuerpo
import com.sanna.rehabapp.domain.model.ResultadoAccion
import com.sanna.rehabapp.domain.model.SentidoGiro

// Guía visual en vivo: conversión entre el modelo de dominio y los mapas de
// Firestore. Sin dependencias de Firebase para poder probarlo con JUnit.
//
// ejercicios/{id}.acciones: [{ orden, nombre, puntoReferencia, pivote,
//   puntoIntermedio?, puntoDistal, lado, anguloInicio, anguloMeta, tolerancia,
//   mantenerSeg, tiempoMaxSeg, sentido }]
// sesiones/{id}.repeticiones[].acciones: [{ ordenAccion, anguloMaximo,
//   completada, tiempoSeg, desviacionAngular }]
internal fun Accion.aMapa(): Map<String, Any> = buildMap {
    put("orden", orden)
    put("nombre", nombre)
    put("puntoReferencia", puntoReferencia)
    put("pivote", pivote)
    puntoIntermedio?.let { put("puntoIntermedio", it) }
    put("puntoDistal", puntoDistal)
    put("lado", lado.aFirestore())
    put("anguloInicio", anguloInicio)
    put("anguloMeta", anguloMeta)
    put("tolerancia", tolerancia)
    put("mantenerSeg", mantenerSeg)
    put("tiempoMaxSeg", tiempoMaxSeg)
    put("sentido", sentido.name)
}

// Devuelve null si falta algún dato obligatorio: una acción dañada no debe
// romper la lectura del ejercicio completo.
internal fun Map<*, *>.aAccion(): Accion? {
    fun entero(clave: String): Int? = (this[clave] as? Number)?.toInt()
    fun decimal(clave: String): Float? = (this[clave] as? Number)?.toFloat()
    return Accion(
        orden = entero("orden") ?: return null,
        nombre = this["nombre"] as? String ?: return null,
        puntoReferencia = entero("puntoReferencia") ?: return null,
        pivote = entero("pivote") ?: return null,
        puntoIntermedio = entero("puntoIntermedio"),
        puntoDistal = entero("puntoDistal") ?: return null,
        lado = LadoCuerpo.desdeFirestoreOrNull(this["lado"] as? String) ?: LadoCuerpo.DERECHO,
        anguloInicio = decimal("anguloInicio") ?: return null,
        anguloMeta = decimal("anguloMeta") ?: return null,
        tolerancia = decimal("tolerancia") ?: return null,
        mantenerSeg = entero("mantenerSeg") ?: 0,
        tiempoMaxSeg = entero("tiempoMaxSeg") ?: return null,
        sentido = SentidoGiro.desdeFirestoreOrNull(this["sentido"] as? String) ?: SentidoGiro.AUTOMATICO,
    )
}

internal fun ResultadoAccion.aMapa(): Map<String, Any> = mapOf(
    "ordenAccion" to ordenAccion,
    "anguloMaximo" to anguloMaximo,
    "completada" to completada,
    "tiempoSeg" to tiempoSeg,
    "desviacionAngular" to desviacionAngular,
)

internal fun Map<*, *>.aResultadoAccion(): ResultadoAccion? {
    return ResultadoAccion(
        ordenAccion = (this["ordenAccion"] as? Number)?.toInt() ?: return null,
        anguloMaximo = (this["anguloMaximo"] as? Number)?.toFloat() ?: return null,
        completada = this["completada"] as? Boolean ?: return null,
        tiempoSeg = (this["tiempoSeg"] as? Number)?.toFloat() ?: 0f,
        desviacionAngular = (this["desviacionAngular"] as? Number)?.toFloat() ?: 0f,
    )
}

internal fun List<*>?.aAcciones(): List<Accion> =
    this?.mapNotNull { (it as? Map<*, *>)?.aAccion() }?.sortedBy { it.orden } ?: emptyList()

internal fun List<*>?.aResultadosAccion(): List<ResultadoAccion> =
    this?.mapNotNull { (it as? Map<*, *>)?.aResultadoAccion() } ?: emptyList()
