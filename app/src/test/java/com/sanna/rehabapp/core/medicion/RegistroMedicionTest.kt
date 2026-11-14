package com.sanna.rehabapp.core.medicion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistroMedicionTest {

    private val lineas = mutableListOf<String>()
    private var ahora = 0L
    private val registro = RegistroMedicion(escribir = { lineas.add(it) }, ahoraNs = { ahora })

    private data class Estado(val valor: Int)

    @Test
    fun `el resultado lleva el instante de llegada del mismo fotograma`() {
        registro.llegada(tsMs = 100, llegadaNs = 1_000)
        ahora = 41_000
        registro.resultado(100)
        assertEquals(listOf("evento=resultado ts=100 llegada=1000 resultado=41000"), lineas)
    }

    @Test
    fun `un resultado sin llegada registrada marca la llegada como -1`() {
        ahora = 5_000
        registro.resultado(7)
        assertEquals("evento=resultado ts=7 llegada=-1 resultado=5000", lineas.single())
    }

    @Test
    fun `la guia se registra una sola vez al dibujarse`() {
        val estado = Estado(1)
        registro.publicado(estado, 100)
        ahora = 90_000
        registro.dibujo(estado)
        registro.dibujo(estado)
        assertEquals(listOf("evento=dibujo ts=100 dibujo=90000"), lineas)
    }

    @Test
    fun `dibujar un estado que no se publico no registra nada`() {
        registro.dibujo(Estado(1))
        assertTrue(lineas.isEmpty())
    }

    @Test
    fun `dos estados iguales por valor se distinguen por identidad`() {
        val publicado = Estado(1)
        val igualPeroOtro = Estado(1)
        registro.publicado(publicado, 100)
        registro.dibujo(igualPeroOtro)
        assertTrue(lineas.isEmpty())
        registro.dibujo(publicado)
        assertEquals(1, lineas.size)
    }

    @Test
    fun `republicar la misma instancia conserva la marca de tiempo original`() {
        val estado = Estado(1)
        registro.publicado(estado, 100)
        registro.publicado(estado, 200)
        registro.dibujo(estado)
        assertTrue(lineas.single().startsWith("evento=dibujo ts=100 "))
    }

    @Test
    fun `un fotograma sin resultado se informa cuando vence el limite`() {
        ahora = 0
        registro.llegada(tsMs = 1, llegadaNs = 0)
        ahora = 2_500_000_000L
        registro.llegada(tsMs = 2, llegadaNs = ahora)
        assertEquals(listOf("evento=sin_resultado ts=1 llegada=0"), lineas)
    }

    @Test
    fun `un fotograma con resultado a tiempo no se informa como descartado`() {
        registro.llegada(tsMs = 1, llegadaNs = 0)
        ahora = 40_000_000L
        registro.resultado(1)
        ahora = 5_000_000_000L
        registro.llegada(tsMs = 2, llegadaNs = ahora)
        assertTrue(lineas.none { it.startsWith("evento=sin_resultado") })
    }

    @Test
    fun `el angulo se registra como maximo cada 500 ms`() {
        ahora = 1_000_000_000L
        registro.angulo(80.123f)
        ahora = 1_300_000_000L
        registro.angulo(81f)
        ahora = 1_600_000_000L
        registro.angulo(82f)
        registro.angulo(null)
        assertEquals(
            listOf(
                "evento=angulo grados=80.12 t=1000000000",
                "evento=angulo grados=82.00 t=1600000000",
            ),
            lineas,
        )
    }

    @Test
    fun `las fases y las repeticiones llevan su instante`() {
        ahora = 9
        registro.fase("monitoreo")
        registro.repeticion(2)
        assertEquals(listOf("evento=fase valor=monitoreo t=9", "evento=repeticion n=2 t=9"), lineas)
    }
}
