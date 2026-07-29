package com.sanna.rehabapp.domain.repository

import com.sanna.rehabapp.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

interface UsuarioRepository {
    suspend fun obtenerUsuario(uid: String): Usuario?

    fun observarPacientesDe(fisioterapeutaId: String): Flow<List<Usuario>>

    // HU01-CA06 — el fisioterapeuta registra/edita el diagnóstico de un
    // paciente que tiene asignado.
    suspend fun actualizarDiagnostico(pacienteId: String, diagnostico: String): Result<Unit>
}
