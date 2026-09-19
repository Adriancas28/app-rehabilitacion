package com.sanna.rehabapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sanna.rehabapp.domain.model.DiagnosticoRegistrado
import com.sanna.rehabapp.domain.model.Usuario
import com.sanna.rehabapp.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UsuarioRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val usuarios: UsuariosFirestore,
) : UsuarioRepository {

    override suspend fun obtenerUsuario(uid: String): Usuario? = usuarios.leer(uid)

    override fun observarPacientesDe(fisioterapeutaId: String): Flow<List<Usuario>> =
        usuarios.observarPacientes(fisioterapeutaId)

    override suspend fun actualizarDiagnosticos(
        pacienteId: String,
        diagnosticos: List<DiagnosticoRegistrado>,
    ): Result<Unit> = runCatching {
        usuarios.reemplazarDiagnosticos(pacienteId, diagnosticos)
    }

    override suspend fun actualizarNombre(uid: String, nombre: String): Result<Unit> = runCatching {
        firestore.collection(COL_USUARIOS)
            .document(uid)
            .set(mapOf("nombre" to nombre), SetOptions.merge())
            .await()
        Unit
    }
}
