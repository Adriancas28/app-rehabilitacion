package com.sanna.rehabapp.core.posedetection

import com.sanna.rehabapp.domain.model.AnguloDetectado
import com.sanna.rehabapp.domain.model.DetalleRepeticion
import com.sanna.rehabapp.domain.model.ErrorDetectado
import com.sanna.rehabapp.domain.model.ResultadoSesion
import org.junit.Assert.assertEquals
import org.junit.Test

class MergeResultadoSesionTest {

    private val anterior = ResultadoSesion(
        angulosDetectados = listOf(AnguloDetectado("Cuello", anguloDetectado = 140f, anguloEsperado = 159f, desviacion = 10f)),
        desviacionPromedio = 10f,
        porcentajeEjecucion = 40f,
        erroresDetectados = listOf(ErrorDetectado("Cuello", "Rango incompleto", repeticiones = 5)),
        repeticionesCompletadas = 1,
        repeticionesAsignadas = 3,
        repeticionesCorrectas = 0,
        detallePorRepeticion = listOf(
            DetalleRepeticion(
                numero = 1,
                dentroDeRango = false,
                errores = listOf(ErrorDetectado("Cuello", "Rango incompleto", repeticiones = 5)),
            ),
        ),
    )

    @Test
    fun `combina repeticiones completadas y correctas de ambos tramos`() {
        val nuevo = ResultadoSesion(
            angulosDetectados = listOf(AnguloDetectado("Cuello", anguloDetectado = 160f, anguloEsperado = 159f, desviacion = 1f)),
            desviacionPromedio = 1f,
            porcentajeEjecucion = 100f,
            erroresDetectados = emptyList(),
            repeticionesCompletadas = 3,
            repeticionesAsignadas = 3,
            repeticionesCorrectas = 2,
            detallePorRepeticion = listOf(
                DetalleRepeticion(numero = 2, dentroDeRango = true, errores = emptyList()),
                DetalleRepeticion(numero = 3, dentroDeRango = true, errores = emptyList()),
            ),
        )

        val resultado = mergearResultados(anterior, nuevo)

        assertEquals(3, resultado.repeticionesCompletadas)
        assertEquals(3, resultado.repeticionesAsignadas)
        assertEquals(2, resultado.repeticionesCorrectas)
        assertEquals(listOf(1, 2, 3), resultado.detallePorRepeticion.map { it.numero })
        // Promedio ponderado: (40*1 + 100*2) / 3 = 80
        assertEquals(80f, resultado.porcentajeEjecucion, 0.01f)
    }

    @Test
    fun `si no se completo ninguna repeticion nueva, devuelve el anterior sin cambios`() {
        val nuevoVacio = ResultadoSesion(repeticionesCompletadas = 1, repeticionesAsignadas = 3)

        val resultado = mergearResultados(anterior, nuevoVacio)

        assertEquals(anterior, resultado)
    }

    @Test
    fun `errores repetidos del mismo tipo y articulacion se suman al combinar`() {
        val nuevo = ResultadoSesion(
            repeticionesCompletadas = 2,
            repeticionesAsignadas = 3,
            repeticionesCorrectas = 0,
            erroresDetectados = listOf(ErrorDetectado("Cuello", "Rango incompleto", repeticiones = 3)),
            detallePorRepeticion = listOf(
                DetalleRepeticion(
                    numero = 2,
                    dentroDeRango = false,
                    errores = listOf(ErrorDetectado("Cuello", "Rango incompleto", repeticiones = 3)),
                ),
            ),
        )

        val resultado = mergearResultados(anterior, nuevo)

        assertEquals(1, resultado.erroresDetectados.size)
        assertEquals(8, resultado.erroresDetectados.first().repeticiones)
    }
}
