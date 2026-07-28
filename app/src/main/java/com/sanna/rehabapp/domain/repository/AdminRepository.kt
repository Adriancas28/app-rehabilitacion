package com.sanna.rehabapp.domain.repository

import com.sanna.rehabapp.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

// HU20/HU21 — el administrador gestiona cuentas de pacientes y
// fisioterapeutas desde la propia app.
interface AdminRepository {
    fun observarPacientes(): Flow<List<Usuario>>

    fun observarFisioterapeutas(): Flow<List<Usuario>>

    suspend fun crearPaciente(nombre: String, email: String, password: String): Result<Unit>

    suspend fun crearFisioterapeuta(nombre: String, email: String, password: String): Result<Unit>

    suspend fun actualizarUsuario(uid: String, nombre: String, email: String): Result<Unit>

    // Nota: solo elimina el documento en Firestore. El registro de Firebase
    // Auth de OTRO usuario no se puede borrar desde el cliente sin Admin
    // SDK/Cloud Functions (descartadas en este proyecto) — sin su documento,
    // RaizViewModel ya lo desloguea igual al no encontrar su perfil.
    suspend fun eliminarUsuario(uid: String): Result<Unit>

    // HU20-CA05: solo debe llamarse si el paciente aún no tiene
    // fisioterapeuta asignado; la UI oculta la acción en ese caso.
    suspend fun asignarFisioterapeuta(pacienteId: String, fisioterapeutaId: String): Result<Unit>
}
