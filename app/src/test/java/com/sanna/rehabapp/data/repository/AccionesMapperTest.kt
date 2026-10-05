package com.sanna.rehabapp.data.repository

import com.sanna.rehabapp.domain.model.Accion
import com.sanna.rehabapp.domain.model.LadoCuerpo
import com.sanna.rehabapp.domain.model.PoseLandmark
import com.sanna.rehabapp.domain.model.ResultadoAccion
import com.sanna.rehabapp.domain.model.SentidoGiro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccionesMapperTest {

    private val accion = Accion(
        orden = 1,
        nombre = "Elevar el brazo",
        puntoReferencia = PoseLandmark.CADERA_DERECHA,
        pivote = PoseLandmark.HOMBRO_DERECHO,
        puntoIntermedio = PoseLandmark.CODO_DERECHO,
        puntoDistal = PoseLandmark.MUNECA_DERECHA,
        lado = LadoCuerpo.IZQUIERDO,
        anguloInicio = 15f,
        anguloMeta = 90f,
        tolerancia = 20f,
        mantenerSeg = 2,
        tiempoMaxSeg = 8,
        sentido = SentidoGiro.HORARIO,
    )

    @Test
    fun `una accion hace ida y vuelta por el mapa de Firestore`() {
        assertEquals(accion, accion.aMapa().aAccion())
    }

    @Test
    fun `el punto intermedio opcional no se escribe si no existe`() {
        val mapa = accion.copy(puntoIntermedio = null).aMapa()
        assertTrue("puntoIntermedio" !in mapa)
        assertNull(mapa.aAccion()?.puntoIntermedio)
    }

    @Test
    fun `una accion con datos obligatorios faltantes se descarta sin romper la lectura`() {
        val dañada = accion.aMapa() - "pivote"
        val lista = listOf(dañada, accion.copy(orden = 2).aMapa()).aAcciones()
        assertEquals(listOf(2), lista.map { it.orden })
    }

    @Test
    fun `los valores opcionales usan su valor por defecto`() {
        val minimo = accion.aMapa() - "lado" - "sentido" - "mantenerSeg" - "puntoIntermedio"
        val leida = minimo.aAccion()!!
        assertEquals(LadoCuerpo.DERECHO, leida.lado)
        assertEquals(SentidoGiro.AUTOMATICO, leida.sentido)
        assertEquals(0, leida.mantenerSeg)
    }

    @Test
    fun `las acciones se leen ordenadas por su orden`() {
        val lista = listOf(accion.copy(orden = 2).aMapa(), accion.aMapa()).aAcciones()
        assertEquals(listOf(1, 2), lista.map { it.orden })
    }

    @Test
    fun `una lista nula o ajena da lista vacia`() {
        assertTrue((null as List<*>?).aAcciones().isEmpty())
        assertTrue(listOf("texto", 3).aAcciones().isEmpty())
    }

    @Test
    fun `un resultado de accion hace ida y vuelta`() {
        val resultado = ResultadoAccion(ordenAccion = 2, anguloMaximo = 88.5f, completada = true, tiempoSeg = 3.2f, desviacionAngular = 0f)
        assertEquals(resultado, resultado.aMapa().aResultadoAccion())
    }

    @Test
    fun `un resultado de accion sin completada se descarta`() {
        val mapa = ResultadoAccion(1, 80f, false, 5f).aMapa() - "completada"
        assertNull(mapa.aResultadoAccion())
        assertTrue(listOf(mapa).aResultadosAccion().isEmpty())
    }
}
