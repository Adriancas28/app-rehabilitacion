package com.sanna.rehabapp.feature.paciente

import java.util.Calendar
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class CalcularRachaTest {

    private fun hace(diasAtras: Int, referencia: Date): Date =
        Calendar.getInstance().apply {
            time = referencia
            add(Calendar.DAY_OF_YEAR, -diasAtras)
        }.time

    @Test
    fun `sin sesiones la racha es 0`() {
        assertEquals(0, calcularRacha(emptyList()))
    }

    @Test
    fun `tres dias consecutivos hasta hoy dan racha 3`() {
        val hoy = Date()
        val fechas = listOf(hace(0, hoy), hace(1, hoy), hace(2, hoy))
        assertEquals(3, calcularRacha(fechas, ahora = hoy))
    }

    @Test
    fun `la ultima sesion fue ayer, tambien cuenta la racha`() {
        val hoy = Date()
        val fechas = listOf(hace(1, hoy), hace(2, hoy))
        assertEquals(2, calcularRacha(fechas, ahora = hoy))
    }

    @Test
    fun `si la ultima sesion fue hace 3 dias, la racha esta rota`() {
        val hoy = Date()
        val fechas = listOf(hace(3, hoy), hace(4, hoy))
        assertEquals(0, calcularRacha(fechas, ahora = hoy))
    }

    @Test
    fun `un hueco en el medio corta la racha`() {
        val hoy = Date()
        val fechas = listOf(hace(0, hoy), hace(1, hoy), hace(3, hoy))
        assertEquals(2, calcularRacha(fechas, ahora = hoy))
    }

    @Test
    fun `varias sesiones el mismo dia cuentan como un solo dia`() {
        val hoy = Date()
        val fechas = listOf(hace(0, hoy), hace(0, hoy), hace(1, hoy))
        assertEquals(2, calcularRacha(fechas, ahora = hoy))
    }
}
