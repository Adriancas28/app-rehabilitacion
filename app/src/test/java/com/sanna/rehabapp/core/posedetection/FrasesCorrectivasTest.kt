package com.sanna.rehabapp.core.posedetection

import com.sanna.rehabapp.domain.model.Articulacion
import org.junit.Assert.assertEquals
import org.junit.Test

class FrasesCorrectivasTest {

    @Test
    fun `rango incompleto en articulacion femenina usa el articulo correcto`() {
        val medicion = MedicionArticulacion(Articulacion.RODILLA_DERECHA, angulo = 60f, anguloMin = 90f, anguloMax = 120f)
        assertEquals("Flexiona más la rodilla derecha.", fraseCorrectiva(medicion))
    }

    @Test
    fun `desviacion angular en articulacion masculina usa el articulo correcto`() {
        val medicion = MedicionArticulacion(Articulacion.HOMBRO_IZQUIERDO, angulo = 200f, anguloMin = 90f, anguloMax = 120f)
        assertEquals("No fuerces tanto el hombro izquierdo.", fraseCorrectiva(medicion))
    }
}
