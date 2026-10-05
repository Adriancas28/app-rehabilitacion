package com.sanna.rehabapp.core.posedetection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfiabilidadPoseTest {

    private fun frame(visibilidades: Map<Int, Float>) = LandmarksFrame(
        puntos = List(33) { PuntoPose(0.5f, 0.5f, 0f, visibilidades[it] ?: 1f) },
        anchoImagen = 100, altoImagen = 100, marcaMs = 0,
    )

    @Test
    fun `con todos los landmarks visibles el cuadro es confiable`() {
        assertTrue(frame(emptyMap()).esConfiable(listOf(12, 14, 24)))
    }

    @Test
    fun `un landmark requerido con visibilidad menor a 0,5 vuelve el cuadro no confiable`() {
        assertFalse(frame(mapOf(14 to 0.49f)).esConfiable(listOf(12, 14, 24)))
    }

    @Test
    fun `un landmark no requerido poco visible no afecta`() {
        assertTrue(frame(mapOf(30 to 0.1f)).esConfiable(listOf(12, 14, 24)))
    }

    @Test
    fun `la visibilidad exactamente en el umbral cuenta como visible`() {
        assertTrue(frame(mapOf(12 to 0.5f)).esConfiable(listOf(12)))
    }

    @Test
    fun `un indice fuera del cuadro no es confiable`() {
        assertFalse(frame(emptyMap()).esConfiable(listOf(40)))
    }
}
