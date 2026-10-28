package com.sanna.rehabapp.core.posedetection

import com.sanna.rehabapp.domain.model.Accion
import com.sanna.rehabapp.domain.model.SentidoGiro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PoseGuideCalculatorTest {

    // Landmarks de la prueba: 0 = referencia, 1 = pivote, 2 = distal, 3 = intermedio.
    private fun accion(
        inicio: Float = 15f,
        meta: Float = 90f,
        tolerancia: Float = 20f,
        sentido: SentidoGiro = SentidoGiro.AUTOMATICO,
        intermedio: Int? = null,
    ) = Accion(
        orden = 1, nombre = "x", puntoReferencia = 0, pivote = 1, puntoIntermedio = intermedio, puntoDistal = 2,
        anguloInicio = inicio, anguloMeta = meta, tolerancia = tolerancia, tiempoMaxSeg = 8, sentido = sentido,
    )

    private fun frame(
        referencia: PuntoPose,
        pivote: PuntoPose,
        distal: PuntoPose,
        intermedio: PuntoPose = PuntoPose(0.5f, 0.5f),
        ancho: Int = 100,
        alto: Int = 100,
    ): LandmarksFrame {
        val puntos = MutableList(33) { PuntoPose(0.5f, 0.5f) }
        puntos[0] = referencia
        puntos[1] = pivote
        puntos[2] = distal
        puntos[3] = intermedio
        return LandmarksFrame(puntos, ancho, alto, 0)
    }

    // Brazo derecho visto de frente en una imagen espejada: la cadera (referencia)
    // queda debajo del hombro (pivote) y el codo (distal) a la derecha.
    private fun brazoAlLado() = frame(
        referencia = PuntoPose(0.5f, 0.7f),
        pivote = PuntoPose(0.5f, 0.4f),
        distal = PuntoPose(0.7f, 0.4f),
    )

    @Test
    fun `el angulo es el del segmento pivote-distal respecto a pivote-referencia`() {
        val guia = PoseGuideCalculator.calcular(brazoAlLado(), accion())!!
        assertEquals(90f, guia.anguloActual, 0.01f)
    }

    @Test
    fun `el angulo se calcula en pixeles y no en coordenadas normalizadas`() {
        // Imagen 300x400: referencia 100 px abajo del pivote; distal 100 px a la derecha y 100 px abajo = 45°.
        val f = frame(
            referencia = PuntoPose(0.5f, 0.75f),
            pivote = PuntoPose(0.5f, 0.5f),
            distal = PuntoPose(0.5f + 100f / 300f, 0.75f),
            ancho = 300,
            alto = 400,
        )
        assertEquals(45f, PoseGuideCalculator.calcular(f, accion())!!.anguloActual, 0.01f)
    }

    @Test
    fun `en zona cuando la diferencia con la meta no supera la tolerancia`() {
        assertTrue(PoseGuideCalculator.calcular(brazoAlLado(), accion(meta = 100f, tolerancia = 10f))!!.enZona)
        assertFalse(PoseGuideCalculator.calcular(brazoAlLado(), accion(meta = 120f, tolerancia = 10f))!!.enZona)
    }

    @Test
    fun `el progreso funciona cuando la meta es mayor que el inicio`() {
        val guia = PoseGuideCalculator.calcular(brazoAlLado(), accion(inicio = 30f, meta = 150f))!!
        assertEquals(0.5f, guia.progreso, 0.001f)
    }

    @Test
    fun `el progreso funciona cuando la meta es menor que el inicio`() {
        val guia = PoseGuideCalculator.calcular(brazoAlLado(), accion(inicio = 150f, meta = 30f))!!
        assertEquals(0.5f, guia.progreso, 0.001f)
    }

    @Test
    fun `el progreso se acota entre 0 y 1`() {
        assertEquals(1f, PoseGuideCalculator.calcular(brazoAlLado(), accion(inicio = 15f, meta = 60f))!!.progreso, 0.001f)
        assertEquals(0f, PoseGuideCalculator.calcular(brazoAlLado(), accion(inicio = 120f, meta = 170f))!!.progreso, 0.001f)
    }

    @Test
    fun `el color depende de la distancia a la meta`() {
        // Ángulo actual 90°, tolerancia 10: verde a 0°, ámbar hasta 30°, rojo más lejos.
        assertEquals(EstadoGuia.VERDE, PoseGuideCalculator.calcular(brazoAlLado(), accion(meta = 95f, tolerancia = 10f))!!.estado)
        assertEquals(EstadoGuia.AMBAR, PoseGuideCalculator.calcular(brazoAlLado(), accion(meta = 115f, tolerancia = 10f))!!.estado)
        assertEquals(EstadoGuia.ROJO, PoseGuideCalculator.calcular(brazoAlLado(), accion(meta = 150f, tolerancia = 10f))!!.estado)
    }

    @Test
    fun `los umbrales de color son configurables`() {
        val estricto = UmbralesGuia(factorAmbar = 1.5f)
        assertEquals(EstadoGuia.ROJO, PoseGuideCalculator.calcular(brazoAlLado(), accion(meta = 115f, tolerancia = 10f), umbrales = estricto)!!.estado)
    }

    @Test
    fun `un landmark requerido poco visible devuelve null`() {
        val base = brazoAlLado()
        val puntos = base.puntos.toMutableList()
        puntos[2] = puntos[2].copy(visibilidad = 0.2f)
        assertNull(PoseGuideCalculator.calcular(base.copy(puntos = puntos), accion()))
    }

    @Test
    fun `un segmento de largo cero devuelve null`() {
        val f = frame(PuntoPose(0.5f, 0.7f), PuntoPose(0.5f, 0.4f), PuntoPose(0.5f, 0.4f))
        assertNull(PoseGuideCalculator.calcular(f, accion()))
    }

    @Test
    fun `el sentido automatico se deduce de que lado esta el segmento`() {
        // Distal a la derecha de una referencia que apunta hacia abajo = antihorario en pantalla.
        assertEquals(-1, PoseGuideCalculator.calcular(brazoAlLado(), accion())!!.signoGiro)
        val izquierdo = frame(PuntoPose(0.5f, 0.7f), PuntoPose(0.5f, 0.4f), PuntoPose(0.3f, 0.4f))
        assertEquals(1, PoseGuideCalculator.calcular(izquierdo, accion())!!.signoGiro)
    }

    @Test
    fun `el sentido explicito manda sobre la deduccion`() {
        assertEquals(1, PoseGuideCalculator.calcular(brazoAlLado(), accion(sentido = SentidoGiro.HORARIO))!!.signoGiro)
        assertEquals(-1, PoseGuideCalculator.calcular(brazoAlLado(), accion(sentido = SentidoGiro.ANTIHORARIO))!!.signoGiro)
    }

    @Test
    fun `sobre la linea de referencia se conserva el sentido anterior`() {
        val alineado = frame(PuntoPose(0.5f, 0.7f), PuntoPose(0.5f, 0.4f), PuntoPose(0.5f, 0.5f))
        assertEquals(-1, PoseGuideCalculator.calcular(alineado, accion(), signoAnterior = -1)!!.signoGiro)
        assertEquals(1, PoseGuideCalculator.calcular(alineado, accion(), signoAnterior = null)!!.signoGiro)
    }

    @Test
    fun `el punto meta queda en el angulo meta del mismo lado que el segmento`() {
        val guia = PoseGuideCalculator.calcular(brazoAlLado(), accion(meta = 90f))!!
        // Segmento de 20 px a la derecha del pivote (0,5 ; 0,4) en una imagen de 100x100.
        assertEquals(0.7f, guia.puntoMeta.x, 0.001f)
        assertEquals(0.4f, guia.puntoMeta.y, 0.001f)
    }

    @Test
    fun `el punto meta en 180 grados queda opuesto a la referencia`() {
        val guia = PoseGuideCalculator.calcular(brazoAlLado(), accion(meta = 180f))!!
        assertEquals(0.5f, guia.puntoMeta.x, 0.001f)
        assertEquals(0.2f, guia.puntoMeta.y, 0.001f)
    }

    @Test
    fun `con punto intermedio la longitud es la suma de los dos tramos`() {
        val f = frame(
            referencia = PuntoPose(0.5f, 0.7f),
            pivote = PuntoPose(0.5f, 0.4f),
            distal = PuntoPose(0.7f, 0.4f),
            intermedio = PuntoPose(0.5f, 0.4f).copy(x = 0.6f),
        )
        val guia = PoseGuideCalculator.calcular(f, accion(intermedio = 3))!!
        assertEquals(20f, guia.longitudSegmento, 0.01f)
        assertNotNull(guia.intermedio)
    }

    @Test
    fun `el calculo funciona igual con cualquier landmark y su espejo`() {
        // El mismo brazo, pero del lado izquierdo (a la izquierda del pivote): mismo ángulo.
        val izquierdo = frame(PuntoPose(0.5f, 0.7f), PuntoPose(0.5f, 0.4f), PuntoPose(0.3f, 0.4f))
        assertEquals(90f, PoseGuideCalculator.calcular(izquierdo, accion())!!.anguloActual, 0.01f)
    }
}
