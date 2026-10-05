package com.sanna.rehabapp.core.posedetection

import org.junit.Assert.assertEquals
import org.junit.Test

class LandmarkMapperTest {

    private val delta = 0.01f

    @Test
    fun `con la misma relacion de aspecto el mapeo es una simple escala`() {
        val mapper = LandmarkMapper(anchoImagen = 480, altoImagen = 640, anchoVista = 960f, altoVista = 1280f)
        val p = mapper.mapear(0.5f, 0.25f)
        assertEquals(480f, p.x, delta)
        assertEquals(320f, p.y, delta)
    }

    @Test
    fun `FILL_CENTER recorta los lados cuando la vista es mas ancha que la imagen`() {
        // Imagen 3:4 sobre una vista cuadrada de 400: se escala por el ancho y se recorta arriba y abajo.
        val mapper = LandmarkMapper(anchoImagen = 300, altoImagen = 400, anchoVista = 400f, altoVista = 400f)
        // Factor = 400/300; alto escalado 533,33; desplazamiento vertical -66,67.
        val centro = mapper.mapear(0.5f, 0.5f)
        assertEquals(200f, centro.x, delta)
        assertEquals(200f, centro.y, delta)
        val arriba = mapper.mapear(0.5f, 0f)
        assertEquals(-66.67f, arriba.y, delta)
    }

    @Test
    fun `FILL_CENTER recorta arriba y abajo cuando la vista es mas alta que la imagen`() {
        // Imagen 4:3 horizontal sobre vista vertical: se escala por el alto y se recorta a los lados.
        val mapper = LandmarkMapper(anchoImagen = 400, altoImagen = 300, anchoVista = 300f, altoVista = 600f)
        // Factor = 600/300 = 2; ancho escalado 800; desplazamiento horizontal -250.
        assertEquals(-250f, mapper.mapear(0f, 0f).x, delta)
        assertEquals(550f, mapper.mapear(1f, 1f).x, delta)
        assertEquals(600f, mapper.mapear(1f, 1f).y, delta)
    }

    @Test
    fun `FIT_CENTER deja barras en vez de recortar`() {
        val mapper = LandmarkMapper(
            anchoImagen = 300, altoImagen = 400, anchoVista = 400f, altoVista = 400f,
            escala = EscalaVista.FIT_CENTER,
        )
        // Factor = 1; ancho escalado 300; barras de 50 a cada lado.
        assertEquals(50f, mapper.mapear(0f, 0f).x, delta)
        assertEquals(350f, mapper.mapear(1f, 0f).x, delta)
    }

    @Test
    fun `espejar X invierte el eje horizontal`() {
        val mapper = LandmarkMapper(100, 100, 100f, 100f, espejarX = true)
        assertEquals(80f, mapper.mapear(0.2f, 0.5f).x, delta)
        assertEquals(50f, mapper.mapear(0.2f, 0.5f).y, delta)
    }

    @Test
    fun `rotar 90 grados lleva la esquina superior izquierda a la superior derecha`() {
        // Imagen horizontal 200x100 sin rotar; al rotar 90° horario queda 100x200.
        val mapper = LandmarkMapper(anchoImagen = 200, altoImagen = 100, anchoVista = 100f, altoVista = 200f, rotacionGrados = 90)
        val p = mapper.mapear(0f, 0f)
        assertEquals(100f, p.x, delta)
        assertEquals(0f, p.y, delta)
        val q = mapper.mapear(1f, 0f)
        assertEquals(100f, q.x, delta)
        assertEquals(200f, q.y, delta)
    }

    @Test
    fun `rotar 270 grados es la rotacion contraria`() {
        val mapper = LandmarkMapper(200, 100, 100f, 200f, rotacionGrados = 270)
        val p = mapper.mapear(0f, 0f)
        assertEquals(0f, p.x, delta)
        assertEquals(200f, p.y, delta)
    }

    @Test
    fun `rotar 180 grados voltea ambos ejes`() {
        val mapper = LandmarkMapper(100, 100, 100f, 100f, rotacionGrados = 180)
        val p = mapper.mapear(0.25f, 0.25f)
        assertEquals(75f, p.x, delta)
        assertEquals(75f, p.y, delta)
    }

    @Test
    fun `camara frontal con rotacion 90 y espejo combina ambas transformaciones`() {
        val mapper = LandmarkMapper(200, 100, 100f, 200f, rotacionGrados = 90, espejarX = true)
        val p = mapper.mapear(0f, 0f)
        assertEquals(0f, p.x, delta)
        assertEquals(0f, p.y, delta)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `una rotacion que no es multiplo de 90 se rechaza`() {
        LandmarkMapper(100, 100, 100f, 100f, rotacionGrados = 45)
    }
}
