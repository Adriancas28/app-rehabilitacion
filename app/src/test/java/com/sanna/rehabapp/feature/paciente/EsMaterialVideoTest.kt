package com.sanna.rehabapp.feature.paciente

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EsMaterialVideoTest {

    @Test
    fun `url de imagen no se considera video`() {
        assertFalse(esMaterialVideo("https://firebasestorage.googleapis.com/ejercicios/abc/uuid.jpg?alt=media"))
    }

    @Test
    fun `url de video mp4 se detecta correctamente`() {
        assertTrue(esMaterialVideo("https://firebasestorage.googleapis.com/ejercicios/abc/uuid.mp4?alt=media&token=123"))
    }

    @Test
    fun `deteccion no distingue mayusculas ni depende de la query`() {
        assertTrue(esMaterialVideo("https://firebasestorage.googleapis.com/ejercicios/abc/UUID.WEBM?alt=media"))
    }

    @Test
    fun `url sin extension no se considera video`() {
        assertFalse(esMaterialVideo("https://firebasestorage.googleapis.com/ejercicios/abc/uuid?alt=media"))
    }
}
