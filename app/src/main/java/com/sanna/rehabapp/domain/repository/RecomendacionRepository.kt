package com.sanna.rehabapp.domain.repository

import com.sanna.rehabapp.domain.model.Recomendacion
import kotlinx.coroutines.flow.Flow

interface RecomendacionRepository {
    // HU15-CA01/HU16-CA01 — observa las recomendaciones de una sesión
    // puntual; lo usa tanto el fisioterapeuta (gestionarlas) como el
    // paciente (consultarlas), con el mismo Flow.
    fun observarDe(pacienteId: String, sesionId: String): Flow<List<Recomendacion>>

    // HU15-CA02 — registrar una nueva recomendación sobre una sesión.
    suspend fun crear(
        pacienteId: String,
        sesionId: String,
        fisioterapeutaId: String,
        texto: String,
    ): Result<Unit>

    // HU15-CA03 — actualizar el texto de una recomendación ya registrada.
    suspend fun actualizar(
        pacienteId: String,
        sesionId: String,
        recomendacionId: String,
        texto: String,
    ): Result<Unit>

    // HU15-CA04 — eliminar una recomendación.
    suspend fun eliminar(pacienteId: String, sesionId: String, recomendacionId: String): Result<Unit>
}
