package com.sanna.rehabapp.feature.pacientes

import com.sanna.rehabapp.domain.model.Rol
import com.sanna.rehabapp.domain.model.Usuario
import org.junit.Assert.assertEquals
import org.junit.Test

class FiltrarPacientesTest {

    private val pacientes = listOf(
        Usuario(uid = "1", nombre = "María López", email = "maria@correo.com", rol = Rol.PACIENTE),
        Usuario(uid = "2", nombre = "Juan Pérez", email = "juan@correo.com", rol = Rol.PACIENTE),
        Usuario(uid = "3", nombre = "Ana Torres", email = "ana.torres@correo.com", rol = Rol.PACIENTE),
    )

    @Test
    fun `consulta vacia devuelve todos los pacientes`() {
        assertEquals(pacientes, filtrarPacientes(pacientes, ""))
    }

    @Test
    fun `filtra por nombre sin distinguir mayusculas`() {
        assertEquals(listOf(pacientes[1]), filtrarPacientes(pacientes, "juan"))
    }

    @Test
    fun `filtra por correo`() {
        assertEquals(listOf(pacientes[2]), filtrarPacientes(pacientes, "ana.torres"))
    }

    @Test
    fun `sin coincidencias devuelve lista vacia`() {
        assertEquals(emptyList<Usuario>(), filtrarPacientes(pacientes, "carlos"))
    }
}
