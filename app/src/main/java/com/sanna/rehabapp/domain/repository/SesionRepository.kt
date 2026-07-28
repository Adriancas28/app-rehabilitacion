package com.sanna.rehabapp.domain.repository

import com.sanna.rehabapp.domain.model.ResultadoSesion

// HU17 — el mecanismo de guardado en sí; quien lo invoca es la ejecución
// de sesión (HU06-09, Sprint 3).
interface SesionRepository {
    suspend fun guardarResultado(
        pacienteId: String,
        sesionId: String,
        resultado: ResultadoSesion,
    ): Result<Unit>
}
