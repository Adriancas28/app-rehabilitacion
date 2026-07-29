package com.sanna.rehabapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RolTest {

    @Test
    fun `desdeFirestore mapea paciente, fisioterapeuta y admin correctamente`() {
        assertEquals(Rol.PACIENTE, Rol.desdeFirestore("paciente"))
        assertEquals(Rol.FISIOTERAPEUTA, Rol.desdeFirestore("fisioterapeuta"))
        assertEquals(Rol.ADMIN, Rol.desdeFirestore("admin"))
    }

    @Test
    fun `aFirestore es el inverso de desdeFirestore`() {
        assertEquals("paciente", Rol.PACIENTE.aFirestore())
        assertEquals("fisioterapeuta", Rol.FISIOTERAPEUTA.aFirestore())
        assertEquals("admin", Rol.ADMIN.aFirestore())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `desdeFirestore lanza excepcion con valor desconocido`() {
        Rol.desdeFirestore("superadmin")
    }
}
