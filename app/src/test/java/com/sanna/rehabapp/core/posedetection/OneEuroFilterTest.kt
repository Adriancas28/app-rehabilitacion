package com.sanna.rehabapp.core.posedetection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class OneEuroFilterTest {

    @Test
    fun `el primer valor pasa sin cambios`() {
        assertEquals(0.7f, OneEuroFilter().filtrar(0.7f, 0.0), 0.0001f)
    }

    @Test
    fun `una senal constante se mantiene constante`() {
        val filtro = OneEuroFilter()
        var ultimo = 0f
        for (i in 0..30) ultimo = filtro.filtrar(0.5f, i / 30.0)
        assertEquals(0.5f, ultimo, 0.0001f)
    }

    @Test
    fun `el temblor pequeno se reduce`() {
        val filtro = OneEuroFilter(minCutoff = 1f, beta = 0.01f)
        var maxCrudo = 0f
        var maxFiltrado = 0f
        for (i in 0..120) {
            val ruido = if (i % 2 == 0) 0.01f else -0.01f
            val salida = filtro.filtrar(0.5f + ruido, i / 30.0)
            if (i > 30) {
                maxCrudo = maxOf(maxCrudo, abs(ruido))
                maxFiltrado = maxOf(maxFiltrado, abs(salida - 0.5f))
            }
        }
        assertTrue("filtrado=$maxFiltrado crudo=$maxCrudo", maxFiltrado < maxCrudo / 2f)
    }

    @Test
    fun `un movimiento rapido se sigue sin quedarse muy atras`() {
        val filtro = OneEuroFilter(minCutoff = 1f, beta = 1f)
        var salida = 0f
        for (i in 0..30) salida = filtro.filtrar(i / 30f, i / 30.0)
        // Tras 1 s de rampa de 0 a 1, el filtro debe estar cerca del valor real.
        assertTrue("salida=$salida", salida > 0.85f)
    }

    @Test
    fun `reiniciar vuelve a tomar el primer valor tal cual`() {
        val filtro = OneEuroFilter()
        filtro.filtrar(0f, 0.0)
        filtro.filtrar(1f, 0.1)
        filtro.reiniciar()
        assertEquals(5f, filtro.filtrar(5f, 1.0), 0.0001f)
    }

    @Test
    fun `un instante repetido o anterior devuelve el valor previo`() {
        val filtro = OneEuroFilter()
        filtro.filtrar(1f, 1.0)
        assertEquals(1f, filtro.filtrar(9f, 1.0), 0.0001f)
    }

    @Test
    fun `el filtro de landmarks solo toca los indices pedidos`() {
        val filtro = FiltroLandmarks()
        val base = LandmarksFrame(List(33) { PuntoPose(0.1f, 0.1f) }, 100, 100, marcaMs = 0)
        filtro.aplicar(base, indices = setOf(12))
        val siguiente = base.copy(
            puntos = List(33) { PuntoPose(0.9f, 0.9f) },
            marcaMs = 33,
        )
        val salida = filtro.aplicar(siguiente, indices = setOf(12))
        assertEquals(0.9f, salida[11]!!.x, 0.0001f)
        assertTrue(salida[12]!!.x < 0.9f)
    }

    @Test
    fun `la visibilidad no se filtra`() {
        val filtro = FiltroLandmarks()
        filtro.aplicar(LandmarksFrame(listOf(PuntoPose(0.1f, 0.1f, 0f, 1f)), 10, 10, 0))
        val salida = filtro.aplicar(LandmarksFrame(listOf(PuntoPose(0.1f, 0.1f, 0f, 0.2f)), 10, 10, 33))
        assertEquals(0.2f, salida[0]!!.visibilidad, 0.0001f)
    }
}
