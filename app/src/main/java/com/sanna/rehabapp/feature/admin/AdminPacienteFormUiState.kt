package com.sanna.rehabapp.feature.admin

import com.sanna.rehabapp.domain.model.Genero
import com.sanna.rehabapp.domain.model.LadoAfectado
import com.sanna.rehabapp.domain.model.TipoDiagnostico

// HU20-CA02 (actualizado): el registro de un paciente captura además DNI,
// edad, género, número de contacto y uno o más diagnósticos — datos que el
// fisioterapeuta no necesita (ver AdminUsuarioFormUiState), por eso este
// formulario tiene su propio estado.
data class AdminPacienteFormUiState(
    val nombre: String = "",
    val email: String = "",
    val password: String = "",
    val dni: String = "",
    val edad: String = "",
    val diagnosticosSeleccionados: Set<TipoDiagnostico> = emptySet(),
    // HU20 (ampliación): lado del cuerpo afectado, un único valor para
    // todo el paciente (no por diagnóstico) — lo usa el monitoreo (HU07/08)
    // para saber qué lado medir.
    val ladoAfectado: LadoAfectado = LadoAfectado.DERECHO,
    // Actualización del modelo de datos (HU20-CA02/CA03):
    val genero: Genero? = null,
    val numeroContacto: String = "",
    val cargando: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null,
    val guardadoExitoso: Boolean = false,
)
