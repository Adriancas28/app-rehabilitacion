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
    // Firestore Security Rules valida las consultas de tipo "listar" contra
    // la forma de la consulta, no documento por documento: el paciente
    // (dueño) puede pedir todo sin filtro, pero un fisioterapeuta necesita
    // que la propia consulta quede acotada a `fisioterapeutaId` para que la
    // regla "resource.data.fisioterapeutaId == request.auth.uid" se pueda
    // validar — de lo contrario Firestore la rechaza con PERMISSION_DENIED
    // aunque el fisioterapeuta sí tenga acceso a esos documentos.
    fun observarSesionesDe(pacienteId: String, fisioterapeutaId: String? = null): Flow<List<Sesion>>

    // HU03-CA03 — cargar una sesión puntual para editarla.
    suspend fun obtenerSesion(pacienteId: String, sesionId: String): Sesion?

    // HU03-CA02 — el fisioterapeuta asigna una nueva sesión a un paciente.
    // HU03-CA05: nota opcional del fisioterapeuta sobre esta sesión.
    suspend fun asignarSesion(
        pacienteId: String,
        ejercicioId: String,
        fisioterapeutaId: String,
        fechaAsignacion: Date,
        notas: String? = null,
    ): Result<Unit>

    // HU03-CA03 — modificar el ejercicio, la fecha o la nota de una sesión pendiente.
    suspend fun actualizarSesion(
        pacienteId: String,
        sesionId: String,
        ejercicioId: String,
        fechaAsignacion: Date,
        notas: String? = null,
    ): Result<Unit>
}
