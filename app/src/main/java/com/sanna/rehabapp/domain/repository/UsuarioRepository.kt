package com.sanna.rehabapp.domain.repository

import com.sanna.rehabapp.domain.model.DiagnosticoRegistrado
import com.sanna.rehabapp.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

interface UsuarioRepository {
    suspend fun obtenerUsuario(uid: String): Usuario?

    fun observarPacientesDe(fisioterapeutaId: String): Flow<List<Usuario>>

    // HU01-CA06 (ampliación) — el fisioterapeuta asigna/edita los
    // diagnósticos de un paciente (uno o más), cada uno de un catálogo
    // cerrado; reemplaza la lista completa (la UI ya calcula qué fechas
    // conservar de los diagnósticos que seguían marcados).
    suspend fun actualizarDiagnosticos(pacienteId: String, diagnosticos: List<DiagnosticoRegistrado>): Result<Unit>

    // HU22-CA02/HU23-CA02 — el propio paciente/fisioterapeuta edita su
    // nombre desde la pantalla de Perfil.
    suspend fun actualizarNombre(uid: String, nombre: String): Result<Unit>
}
