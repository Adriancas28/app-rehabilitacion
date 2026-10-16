package com.sanna.rehabapp.core.posedetection

import com.sanna.rehabapp.domain.model.Accion
import com.sanna.rehabapp.domain.model.ResultadoAccion
import com.sanna.rehabapp.domain.model.formatearDuracion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultadoPorAccionesTest {

    private val subir = Accion(1, "Elevar", 24, 12, puntoDistal = 14, anguloInicio = 15f, anguloMeta = 90f, tolerancia = 10f, tiempoMaxSeg = 8)
    private val bajar = Accion(2, "Bajar", 24, 12, puntoDistal = 14, anguloInicio = 90f, anguloMeta = 15f, tolerancia = 15f, tiempoMaxSeg = 8)

    private fun ok(orden: Int) = ResultadoAccion(orden, 90f, true, 3f, 0f)

    @Test
    fun `una repeticion con todas las acciones cumplidas vale 100 y es correcta`() {
        val r = construirResultadoPorAcciones(listOf(subir, bajar), listOf(listOf(ok(1), ok(2))), 1, 3)
        assertEquals(100f, r.porcentajeEjecucion, 0.001f)
        assertEquals(1, r.repeticionesCorrectas)
        assertTrue(r.detallePorRepeticion.single().errores.isEmpty())
        assertEquals(2, r.detallePorRepeticion.single().acciones.size)
    }

    @Test
    fun `una accion no completada genera un error con angulo detectado y esperado`() {
        val fallida = ResultadoAccion(1, 52.5f, false, 8f, 27.5f)
        val r = construirResultadoPorAcciones(listOf(subir, bajar), listOf(listOf(fallida, ok(2))), 1, 3)
        val detalle = r.detallePorRepeticion.single()
        assertEquals(75f, detalle.porcentajeEjecucion, 0.01f) // (50 + 100) / 2
        val error = detalle.errores.single()
        assertEquals("Elevar", error.articulacion)
        assertEquals("Rango incompleto", error.tipo)
        assertEquals(52.5f, error.anguloDetectado)
        assertEquals(90f, error.anguloEsperado)
        assertEquals(0, r.repeticionesCorrectas)
    }

    @Test
    fun `llegar a la zona sin sostenerla se reporta como mantenimiento incompleto`() {
        val sinSostener = ResultadoAccion(1, 88f, false, 8f, 0f)
        val r = construirResultadoPorAcciones(listOf(subir, bajar), listOf(listOf(sinSostener, ok(2))), 1, 3)
        assertEquals("Mantenimiento incompleto", r.detallePorRepeticion.single().errores.single().tipo)
    }

    @Test
    fun `la numeracion respeta la repeticion inicial de una reanudacion`() {
        val r = construirResultadoPorAcciones(listOf(subir, bajar), listOf(listOf(ok(1), ok(2)), listOf(ok(1), ok(2))), 4, 4, numeroRepeticionInicial = 3)
        assertEquals(listOf(3, 4), r.detallePorRepeticion.map { it.numero })
    }

    @Test
    fun `sin repeticiones cerradas el resultado queda vacio con los contadores`() {
        val r = construirResultadoPorAcciones(listOf(subir), emptyList(), 0, 3)
        assertEquals(0f, r.porcentajeEjecucion, 0.001f)
        assertEquals(3, r.repeticionesAsignadas)
        assertTrue(r.detallePorRepeticion.isEmpty())
    }

    @Test
    fun `el porcentaje de la sesion es el promedio de sus repeticiones`() {
        val mala = listOf(ResultadoAccion(1, 15f, false, 8f, 65f), ResultadoAccion(2, 90f, false, 8f, 60f))
        val r = construirResultadoPorAcciones(listOf(subir, bajar), listOf(listOf(ok(1), ok(2)), mala), 2, 2)
        assertEquals(50f, r.porcentajeEjecucion, 0.5f)
    }

    @Test
    fun `la duracion medida suma el tiempo real de todas las acciones`() {
        val r = construirResultadoPorAcciones(
            listOf(subir, bajar),
            listOf(listOf(ok(1), ok(2)), listOf(ResultadoAccion(1, 50f, false, 8f, 40f), ok(2))),
            2, 2,
        )
        assertEquals(17, r.duracionMedidaSegundos) // 3 + 3 + 8 + 3
        assertEquals("17 s", formatearDuracion(17))
        assertEquals("1 min 5 s", formatearDuracion(65))
    }
}
