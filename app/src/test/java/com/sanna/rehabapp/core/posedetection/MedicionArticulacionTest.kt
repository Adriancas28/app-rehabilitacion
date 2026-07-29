package com.sanna.rehabapp.core.posedetection

import com.sanna.rehabapp.domain.model.Articulacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicionArticulacionTest {

    private fun medicion(angulo: Float, min: Float = 90f, max: Float = 120f) =
        MedicionArticulacion(Articulacion.RODILLA_DERECHA, angulo, min, max)

    @Test
    fun `sin mediciones devuelve un resultado vacio sin lanzar excepcion`() {
        val resultado = construirResultadoSesion(emptyList())
        assertEquals(0f, resultado.porcentajeEjecucion, 0.01f)
        assertTrue(resultado.angulosDetectados.isEmpty())
        assertTrue(resultado.erroresDetectados.isEmpty())
    }

    @Test
    fun `todos los frames dentro de rango dan 100 por ciento y sin errores`() {
        val frames = listOf(
            listOf(medicion(100f)),
            listOf(medicion(110f)),
        )
        val resultado = construirResultadoSesion(frames)
        assertEquals(100f, resultado.porcentajeEjecucion, 0.01f)
        assertEquals(0f, resultado.desviacionPromedio, 0.01f)
        assertTrue(resultado.erroresDetectados.isEmpty())
    }

    @Test
    fun `un frame fuera de rango se clasifica como rango incompleto y baja el porcentaje`() {
        val frames = listOf(
            listOf(medicion(100f)),
            listOf(medicion(60f)), // por debajo del minimo (90)
        )
        val resultado = construirResultadoSesion(frames)
        assertEquals(50f, resultado.porcentajeEjecucion, 0.01f)
        assertEquals(1, resultado.erroresDetectados.size)
        assertEquals("Rango incompleto", resultado.erroresDetectados.first().tipo)
        assertEquals(1, resultado.erroresDetectados.first().repeticiones)
    }

    @Test
    fun `frame sin mediciones (articulacion ocluida) no cuenta como dentro de rango`() {
        val frames = listOf(
            listOf(medicion(100f)),
            emptyList(), // toda la articulacion se ocluyo ese frame
        )
        val resultado = construirResultadoSesion(frames)
        assertEquals(50f, resultado.porcentajeEjecucion, 0.01f)
    }

    @Test
    fun `errores repetidos del mismo tipo y articulacion se agrupan`() {
        val frames = listOf(
            listOf(medicion(200f)), // por encima del maximo (120) -> desviacion angular
            listOf(medicion(200f)),
            listOf(medicion(200f)),
        )
        val resultado = construirResultadoSesion(frames)
        assertEquals(1, resultado.erroresDetectados.size)
        assertEquals(3, resultado.erroresDetectados.first().repeticiones)
        assertEquals("Desviación angular", resultado.erroresDetectados.first().tipo)
    }
}
