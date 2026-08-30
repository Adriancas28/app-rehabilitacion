package com.sanna.rehabapp.domain.repository

import com.sanna.rehabapp.domain.model.Genero
import com.sanna.rehabapp.domain.model.LadoAfectado
import com.sanna.rehabapp.domain.model.TipoDiagnostico
import com.sanna.rehabapp.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

// HU20/HU21 — el administrador gestiona cuentas de pacientes y
// fisioterapeutas desde la propia app.
interface AdminRepository {
    fun observarPacientes(): Flow<List<Usuario>>

    fun observarFisioterapeutas(): Flow<List<Usuario>>

    // HU20-CA02 (actualizado): además de los datos de cuenta, el admin
    // captura DNI, edad, género, número de contacto y uno o más
    // diagnósticos del paciente al registrarlo.
    suspend fun crearPaciente(
        nombre: String,
        email: String,
        password: String,
        dni: String,
        edad: Int,
        diagnosticos: List<TipoDiagnostico>,
        ladoAfectado: LadoAfectado,
        genero: Genero,
        numeroContacto: String,
    ): Result<Unit>

    // HU21-CA02 (actualizado): especialidad y número de colegiatura son
    // opcionales, el resto de datos son obligatorios.
    suspend fun crearFisioterapeuta(
        nombre: String,
        email: String,
        password: String,
        edad: Int,
        genero: Genero,
        numeroContacto: String,
        especialidad: String?,
        numeroColegiatura: String?,
    ): Result<Unit>

    suspend fun actualizarUsuario(uid: String, nombre: String, email: String): Result<Unit>

    // HU20-CA03 (actualizado): editar los datos propios de un paciente (a
    // diferencia de actualizarUsuario, que sirve para ambos roles).
    suspend fun actualizarPaciente(
        uid: String,
        nombre: String,
        email: String,
        dni: String,
        edad: Int,
        diagnosticos: List<TipoDiagnostico>,
        ladoAfectado: LadoAfectado,
        genero: Genero,
        numeroContacto: String,
    ): Result<Unit>

    // HU21-CA03 (actualizado): editar los datos propios de un
    // fisioterapeuta.
    suspend fun actualizarFisioterapeuta(
        uid: String,
        nombre: String,
        email: String,
        edad: Int,
        genero: Genero,
        numeroContacto: String,
        especialidad: String?,
        numeroColegiatura: String?,
    ): Result<Unit>

    // Recomendación del modelo E-R-SANNA: desactivar una cuenta (reversible)
    // en vez de eliminarla directamente — el admin puede elegir cualquiera
    // de las dos acciones, esta no reemplaza a eliminarUsuario.
    suspend fun cambiarEstadoActivoUsuario(uid: String, activo: Boolean): Result<Unit>

    // Nota: solo elimina el documento en Firestore. El registro de Firebase
    // Auth de OTRO usuario no se puede borrar desde el cliente sin Admin
    // SDK/Cloud Functions (descartadas en este proyecto) — sin su documento,
    // RaizViewModel ya lo desloguea igual al no encontrar su perfil.
    suspend fun eliminarUsuario(uid: String): Result<Unit>

    // HU20-CA05: solo debe llamarse si el paciente aún no tiene
    // fisioterapeuta asignado; la UI oculta la acción en ese caso.
    suspend fun asignarFisioterapeuta(pacienteId: String, fisioterapeutaId: String): Result<Unit>
}
