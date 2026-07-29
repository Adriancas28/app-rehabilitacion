package com.sanna.rehabapp.feature.pacientes

import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FechaDatePickerTest {

    private lateinit var zonaOriginal: TimeZone

    @Before
    fun fijarZonaHorariaDeLima() {
        zonaOriginal = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Lima")) // UTC-5
    }

    @After
    fun restaurarZonaHoraria() {
        TimeZone.setDefault(zonaOriginal)
    }

    @Test
    fun `31 de julio elegido en el picker no se corre a 30 de julio en UTC-5`() {
        // El DatePicker de Material3 entrega medianoche UTC del dia elegido.
        val medianocheUtcDel31 = ZonedDateTime.of(2026, 7, 31, 0, 0, 0, 0, ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()

        val fechaCorregida = utcMillisADiaLocal(medianocheUtcDel31)
        val diaLocal = fechaCorregida.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

        assertEquals(31, diaLocal.dayOfMonth)
        assertEquals(7, diaLocal.monthValue)
    }

    @Test
    fun `ida y vuelta entre picker y fecha guardada conserva el mismo dia`() {
        val medianocheUtcDel31 = ZonedDateTime.of(2026, 7, 31, 0, 0, 0, 0, ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()

        val fechaGuardada = utcMillisADiaLocal(medianocheUtcDel31)
        val millisParaReabrirElPicker = diaLocalAUtcMillis(fechaGuardada)

        assertEquals(medianocheUtcDel31, millisParaReabrirElPicker)
    }
}
