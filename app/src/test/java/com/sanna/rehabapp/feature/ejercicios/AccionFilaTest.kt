package com.sanna.rehabapp.feature.ejercicios

import com.sanna.rehabapp.domain.model.Articulacion
import com.sanna.rehabapp.domain.model.PoseLandmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccionFilaTest {

    private fun filaValida() = AccionFila().conSegmentoDe(Articulacion.HOMBRO_DERECHO).copy(
        nombre = "Elevar el brazo",
        inicio = "15",
        meta = "90",
        tolerancia = "20",
        mantener = "2",
        tiempoMax = "8",
    )

    @Test
    fun `una fila valida se convierte a accion con su orden`() {
        val accion = filaValida().aAccion(orden = 3).getOrThrow()
        assertEquals(3, accion.orden)
        assertEquals(PoseLandmark.HOMBRO_DERECHO, accion.pivote)
        assertEquals(PoseLandmark.CADERA_DERECHA, accion.puntoReferencia)
        assertEquals(PoseLandmark.CODO_DERECHO, accion.puntoDistal)
        assertEquals(90f, accion.anguloMeta)
    }

    @Test
    fun `una accion hace ida y vuelta por la fila`() {
        val accion = filaValida().aAccion(1).getOrThrow()
        assertEquals(accion, accion.aFila().aAccion(1).getOrThrow())
    }

    @Test
    fun `un texto que no es numero da un mensaje claro`() {
        val mensaje = filaValida().copy(meta = "abc").aAccion(2).exceptionOrNull()?.message.orEmpty()
        assertTrue("Acción 2" in mensaje && "ángulo meta" in mensaje)
    }

    @Test
    fun `una tolerancia en cero se rechaza con la regla de la accion`() {
        val mensaje = filaValida().copy(tolerancia = "0").aAccion(1).exceptionOrNull()?.message.orEmpty()
        assertTrue("tolerancia" in mensaje)
    }

    @Test
    fun `el segmento elegido rellena los landmarks y el nombre solo si estaba vacio`() {
        val fila = AccionFila(nombre = "Mi nombre").conSegmentoDe(Articulacion.RODILLA_IZQUIERDA)
        assertEquals("Mi nombre", fila.nombre)
        assertEquals(PoseLandmark.RODILLA_IZQUIERDA.toString(), fila.pivote)
        assertEquals(PoseLandmark.TOBILLO_IZQUIERDO.toString(), fila.distal)
    }
}
