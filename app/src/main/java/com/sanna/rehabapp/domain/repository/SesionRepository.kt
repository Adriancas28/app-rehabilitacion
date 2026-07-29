package com.sanna.rehabapp.domain.repository

import com.sanna.rehabapp.domain.model.ResultadoSesion
import com.sanna.rehabapp.domain.model.Sesion
import java.util.Date
import kotlinx.coroutines.flow.Flow

interface SesionRepository {
    // HU17 — el mecanismo de guardado en sí; quien lo invoca es la ejecución
    // de sesión (HU06-09, Sprint 3).
    suspend fun guardarResultado(
        pacienteId: String,
        sesionId: String,
        resultado: ResultadoSesion,
    ): Result<Unit>

    // HU01-CA03 / HU04-CA01 — sesiones registradas de un paciente; lo usa
    // tanto el detalle del fisioterapeuta como el home del propio paciente.
    fun observarSesionesDe(pacienteId: String): Flow<List<Sesion>>

    // HU03-CA02 — el fisioterapeuta asigna una nueva sesión a un paciente.
    suspend fun asignarSesion(
        pacienteId: String,
        ejercicioId: String,
        fisioterapeutaId: String,
        fechaAsignacion: Date,
    ): Result<Unit>

    // HU03-CA03 — modificar el ejercicio o la fecha de una sesión pendiente.
    suspend fun actualizarSesion(
        pacienteId: String,
        sesionId: String,
        ejercicioId: String,
        fechaAsignacion: Date,
    ): Result<Unit>
}
