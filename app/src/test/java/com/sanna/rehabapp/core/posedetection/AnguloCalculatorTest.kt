package com.sanna.rehabapp.core.posedetection

import org.junit.Assert.assertEquals
import org.junit.Test

class AnguloCalculatorTest {

    @Test
    fun `angulo recto entre dos segmentos perpendiculares es 90 grados`() {
        val angulo = calcularAngulo(
            inicio = Punto3D(1f, 0f, 0f),
            vertice = Punto3D(0f, 0f, 0f),
            fin = Punto3D(0f, 1f, 0f),
        )
        assertEquals(90f, angulo, 0.01f)
    }

    @Test
    fun `segmentos opuestos (articulacion extendida) dan 180 grados`() {
        val angulo = calcularAngulo(
            inicio = Punto3D(-1f, 0f, 0f),
            vertice = Punto3D(0f, 0f, 0f),
            fin = Punto3D(1f, 0f, 0f),
        )
        assertEquals(180f, angulo, 0.01f)
    }

    @Test
    fun `segmentos en la misma direccion (articulacion muy flexionada) dan 0 grados`() {
        val angulo = calcularAngulo(
            inicio = Punto3D(1f, 0f, 0f),
            vertice = Punto3D(0f, 0f, 0f),
            fin = Punto3D(2f, 0f, 0f),
        )
        assertEquals(0f, angulo, 0.01f)
    }

    @Test
    fun `punto degenerado (vertice igual a inicio) no lanza excepcion y devuelve 0`() {
        val angulo = calcularAngulo(
            inicio = Punto3D(0f, 0f, 0f),
            vertice = Punto3D(0f, 0f, 0f),
            fin = Punto3D(1f, 1f, 0f),
        )
        assertEquals(0f, angulo, 0.01f)
    }

    @Test
    fun `funciona tambien considerando la profundidad (z)`() {
        val angulo = calcularAngulo(
            inicio = Punto3D(0f, 0f, 1f),
            vertice = Punto3D(0f, 0f, 0f),
            fin = Punto3D(0f, 1f, 0f),
        )
        assertEquals(90f, angulo, 0.01f)
    }
}
