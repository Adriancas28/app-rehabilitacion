package com.sanna.rehabapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArticulacionTest {

    @Test
    fun `aFirestore y desdeFirestoreOrNull hacen ida y vuelta`() {
        Articulacion.entries.forEach { articulacion ->
            assertEquals(articulacion, Articulacion.desdeFirestoreOrNull(articulacion.aFirestore()))
        }
    }

    @Test
    fun `texto libre de antes de Sprint 3 no rompe el mapeo, devuelve null`() {
        assertNull(Articulacion.desdeFirestoreOrNull("rodilla derecha"))
        assertNull(Articulacion.desdeFirestoreOrNull(null))
    }
}
