package com.sanna.rehabapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccionTest {

    private fun accionHombro() = Accion(
        orden = 1,
        nombre = "Elevar el brazo",
        puntoReferencia = PoseLandmark.CADERA_DERECHA,
        pivote = PoseLandmark.HOMBRO_DERECHO,
        puntoIntermedio = PoseLandmark.CODO_DERECHO,
        puntoDistal = PoseLandmark.MUNECA_DERECHA,
        anguloInicio = 15f,
        anguloMeta = 90f,
        tolerancia = 20f,
        mantenerSeg = 2,
        tiempoMaxSeg = 8,
    )

    @Test
    fun `una accion bien definida no tiene errores`() {
        assertTrue(accionHombro().errores().isEmpty())
    }

    @Test
    fun `tolerancia y tiempo maximo deben ser mayores a cero`() {
        val errores = accionHombro().copy(tolerancia = 0f, tiempoMaxSeg = 0, mantenerSeg = 0).errores()
        assertTrue(errores.any { "tolerancia" in it })
        assertTrue(errores.any { "tiempo máximo" in it })
    }

    @Test
    fun `los angulos deben estar entre 0 y 180`() {
        val errores = accionHombro().copy(anguloInicio = -5f, anguloMeta = 200f).errores()
        assertTrue(errores.any { "inicio" in it })
        assertTrue(errores.any { "meta" in it })
    }

    @Test
    fun `el mantenimiento no puede superar el tiempo maximo ni ser negativo`() {
        assertTrue(accionHombro().copy(mantenerSeg = 9).errores().any { "mantenimiento" in it })
        assertTrue(accionHombro().copy(mantenerSeg = -1).errores().any { "mantenimiento" in it })
    }

    @Test
    fun `los landmarks deben ser validos y distintos`() {
        assertTrue(accionHombro().copy(puntoDistal = 40).errores().any { "landmarks" in it })
        assertTrue(accionHombro().copy(puntoDistal = PoseLandmark.HOMBRO_DERECHO).errores().any { "distintos" in it })
    }

    @Test
    fun `el espejo cambia todos los landmarks y el lado`() {
        val espejo = accionHombro().espejo()
        assertEquals(PoseLandmark.CADERA_IZQUIERDA, espejo.puntoReferencia)
        assertEquals(PoseLandmark.HOMBRO_IZQUIERDO, espejo.pivote)
        assertEquals(PoseLandmark.CODO_IZQUIERDO, espejo.puntoIntermedio)
        assertEquals(PoseLandmark.MUNECA_IZQUIERDA, espejo.puntoDistal)
        assertEquals(LadoCuerpo.IZQUIERDO, espejo.lado)
        assertEquals(accionHombro(), espejo.espejo())
    }

    @Test
    fun `el espejo invierte el sentido de giro explicito`() {
        assertEquals(SentidoGiro.ANTIHORARIO, accionHombro().copy(sentido = SentidoGiro.HORARIO).espejo().sentido)
        assertEquals(SentidoGiro.AUTOMATICO, accionHombro().espejo().sentido)
    }

    @Test
    fun `paraLado usa el espejo solo cuando el lado afectado es el opuesto`() {
        assertEquals(accionHombro(), accionHombro().paraLado(LadoAfectado.DERECHO))
        assertEquals(accionHombro().espejo(), accionHombro().paraLado(LadoAfectado.IZQUIERDO))
        assertEquals(accionHombro(), accionHombro().paraLado(LadoAfectado.AMBOS))
    }

    @Test
    fun `espejo de landmarks es su propio inverso`() {
        (PoseLandmark.MINIMO..PoseLandmark.MAXIMO).forEach { indice ->
            assertEquals(indice, PoseLandmark.espejo(PoseLandmark.espejo(indice)))
        }
        assertEquals(PoseLandmark.NARIZ, PoseLandmark.espejo(PoseLandmark.NARIZ))
    }

    @Test
    fun `los roles del hombro estan invertidos respecto a la tripleta`() {
        val roles = Articulacion.HOMBRO_DERECHO.roles()
        assertEquals(PoseLandmark.CADERA_DERECHA, roles.referencia)
        assertEquals(PoseLandmark.CODO_DERECHO, roles.distal)
        val rodilla = Articulacion.RODILLA_DERECHA.roles()
        assertEquals(PoseLandmark.CADERA_DERECHA, rodilla.referencia)
        assertEquals(PoseLandmark.TOBILLO_DERECHO, rodilla.distal)
    }

    @Test
    fun `el lado de una articulacion se deduce de su nombre`() {
        assertEquals(LadoCuerpo.IZQUIERDO, Articulacion.RODILLA_IZQUIERDA.lado())
        assertEquals(LadoCuerpo.DERECHO, Articulacion.HOMBRO_DERECHO.lado())
        assertEquals(LadoCuerpo.DERECHO, Articulacion.TRONCO.lado())
    }

    @Test
    fun `un ejercicio sin acciones usa una accion por defecto derivada del rango`() {
        val ejercicio = Ejercicio(
            nombre = "Abducción de hombro",
            descripcion = "",
            categoria = CategoriaEjercicio.MOVILIDAD,
            patronesReferencia = listOf(PatronReferencia(Articulacion.HOMBRO_DERECHO, 70f, 110f)),
            creadoPor = "fisio",
        )
        val acciones = ejercicio.accionesEfectivas()
        assertEquals(1, acciones.size)
        with(acciones.first()) {
            assertEquals(90f, anguloMeta)
            assertEquals(20f, tolerancia)
            assertEquals(0f, anguloInicio)
            assertEquals(PoseLandmark.HOMBRO_DERECHO, pivote)
            assertEquals(PoseLandmark.CADERA_DERECHA, puntoReferencia)
            assertEquals(PoseLandmark.CODO_DERECHO, puntoDistal)
            assertTrue(errores().isEmpty())
        }
    }

    @Test
    fun `un ejercicio con acciones las devuelve ordenadas y no usa el respaldo`() {
        val segunda = accionHombro().copy(orden = 2, nombre = "Bajar")
        val ejercicio = Ejercicio(
            nombre = "x",
            descripcion = "",
            categoria = CategoriaEjercicio.MOVILIDAD,
            acciones = listOf(segunda, accionHombro()),
            patronesReferencia = listOf(PatronReferencia(Articulacion.RODILLA_DERECHA, 90f, 140f)),
            creadoPor = "fisio",
        )
        assertEquals(listOf(1, 2), ejercicio.accionesEfectivas().map { it.orden })
    }
}
