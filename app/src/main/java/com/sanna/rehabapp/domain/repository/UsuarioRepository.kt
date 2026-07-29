package com.sanna.rehabapp.domain.repository

import com.sanna.rehabapp.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

interface UsuarioRepository {
    suspend fun obtenerUsuario(uid: String): Usuario?

    fun observarPacientesDe(fisioterapeutaId: String): Flow<List<Usuario>>
}
