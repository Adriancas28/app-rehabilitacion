package com.sanna.rehabapp.core.posedetection

import kotlin.math.PI
import kotlin.math.abs

// Filtro One Euro (Casiez et al., 2012): suaviza mucho cuando la señal está casi
// quieta (quita el temblor) y poco cuando se mueve rápido (sin retraso visible).
// - minCutoff: frecuencia de corte base en Hz; menor = más suavizado en reposo.
// - beta: cuánto sube la frecuencia de corte con la velocidad; mayor = menos retraso al moverse.
// - dCutoff: frecuencia de corte del filtro de la derivada.
class OneEuroFilter(
    private val minCutoff: Float = 1.0f,
    private val beta: Float = 0.05f,
    private val dCutoff: Float = 1.0f,
) {
    private var valorPrevio: Float? = null
    private var derivadaPrevia = 0f
    private var instantePrevioSeg = 0.0

    fun filtrar(valor: Float, instanteSeg: Double): Float {
        val previo = valorPrevio
        if (previo == null) {
            valorPrevio = valor
            instantePrevioSeg = instanteSeg
            return valor
        }
        val dt = (instanteSeg - instantePrevioSeg).toFloat()
        if (dt <= 0f) return previo
        val derivada = (valor - previo) / dt
        val derivadaSuave = suavizar(derivada, derivadaPrevia, alfa(dCutoff, dt))
        val corte = minCutoff + beta * abs(derivadaSuave)
        val filtrado = suavizar(valor, previo, alfa(corte, dt))
        valorPrevio = filtrado
        derivadaPrevia = derivadaSuave
        instantePrevioSeg = instanteSeg
        return filtrado
    }

    fun reiniciar() {
        valorPrevio = null
        derivadaPrevia = 0f
    }

    private fun alfa(corte: Float, dt: Float): Float {
        val tau = 1f / (2f * PI.toFloat() * corte)
        return 1f / (1f + tau / dt)
    }

    private fun suavizar(actual: Float, previo: Float, alfa: Float): Float = alfa * actual + (1f - alfa) * previo
}

// Suaviza los landmarks de un cuadro de pose, un filtro por coordenada y por
// landmark. `indices` limita qué landmarks se filtran (los usados por la acción
// actual); el resto pasa sin cambios. La visibilidad no se filtra: se usa tal
// cual para decidir si el cuadro es confiable.
class FiltroLandmarks(
    private val minCutoff: Float = 1.0f,
    private val beta: Float = 0.05f,
    private val dCutoff: Float = 1.0f,
) {
    private class Canales(minCutoff: Float, beta: Float, dCutoff: Float) {
        val x = OneEuroFilter(minCutoff, beta, dCutoff)
        val y = OneEuroFilter(minCutoff, beta, dCutoff)
        val z = OneEuroFilter(minCutoff, beta, dCutoff)
    }

    private val porLandmark = mutableMapOf<Int, Canales>()

    fun aplicar(frame: LandmarksFrame, indices: Collection<Int>? = null): LandmarksFrame {
        val instanteSeg = frame.marcaMs / 1000.0
        val nuevos = frame.puntos.mapIndexed { indice, punto ->
            if (indices != null && indice !in indices) {
                punto
            } else {
                val canales = porLandmark.getOrPut(indice) { Canales(minCutoff, beta, dCutoff) }
                punto.copy(
                    x = canales.x.filtrar(punto.x, instanteSeg),
                    y = canales.y.filtrar(punto.y, instanteSeg),
                    z = canales.z.filtrar(punto.z, instanteSeg),
                )
            }
        }
        return frame.copy(puntos = nuevos)
    }

    fun reiniciar() {
        porLandmark.clear()
    }
}
