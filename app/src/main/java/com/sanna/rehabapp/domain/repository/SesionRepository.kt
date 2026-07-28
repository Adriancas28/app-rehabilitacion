package com.sanna.rehabapp.domain.repository

import com.sanna.rehabapp.domain.model.ResultadoSesion
import com.sanna.rehabapp.domain.model.Sesion
import kotlinx.coroutines.flow.Flow

interface SesionRepository {
    // HU17 — el mecanismo de guardado en sí; quien lo invoca es la ejecución
    // de sesión (HU06-09, Sprint 3).
    suspend fun guardarResultado(
        pacienteId: String,
        sesionId: String,
        resultado: ResultadoSesion,
    ): Result<Unit>

    // HU01-CA03 — sesiones registradas de un paciente, para su detalle.
    fun observarSesionesDe(pacienteId: String): Flow<List<Sesion>>
}
