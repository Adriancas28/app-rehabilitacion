package com.sanna.rehabapp.feature.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

// ERR-ADM-001/008: validaciones del formulario de cuentas.
class ValidacionesAdminTest {

    @Test
    fun `dni exige exactamente 8 digitos`() {
        assertNull(ValidacionesAdmin.dni("70000001"))
        assertNotNull(ValidacionesAdmin.dni("AB"))
        assertNotNull(ValidacionesAdmin.dni("1234567"))
        assertNotNull(ValidacionesAdmin.dni("123456789"))
        assertNotNull(ValidacionesAdmin.dni("1234567a"))
    }

    @Test
    fun `edad entre 1 y 120 solo digitos`() {
        assertNull(ValidacionesAdmin.edad("35"))
        assertNotNull(ValidacionesAdmin.edad("0"))
        assertNotNull(ValidacionesAdmin.edad("200"))
        assertNotNull(ValidacionesAdmin.edad("-5"))
        assertNotNull(ValidacionesAdmin.edad("abc"))
    }

    @Test
    fun `contacto solo digitos entre 7 y 15`() {
        assertNull(ValidacionesAdmin.contacto("987654321"))
        assertNotNull(ValidacionesAdmin.contacto("xyz"))
        assertNotNull(ValidacionesAdmin.contacto("12345"))
        assertNotNull(ValidacionesAdmin.contacto("9876 54321"))
    }

    @Test
    fun `soloDigitos filtra letras y recorta`() {
        assertEquals("123", ValidacionesAdmin.soloDigitos("a1b-2c3", 8))
        assertEquals("1234", ValidacionesAdmin.soloDigitos("123456", 4))
    }

    @Test
    fun `email y password`() {
        assertNull(ValidacionesAdmin.email("qa@correo.com"))
        assertNotNull(ValidacionesAdmin.email("sin-arroba"))
        assertNull(ValidacionesAdmin.password("123456"))
        assertNotNull(ValidacionesAdmin.password("123"))
    }
}
