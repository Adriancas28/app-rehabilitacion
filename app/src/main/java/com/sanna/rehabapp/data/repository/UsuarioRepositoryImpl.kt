package com.sanna.rehabapp.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.sanna.rehabapp.domain.model.Rol
import com.sanna.rehabapp.domain.model.Usuario
import com.sanna.rehabapp.domain.repository.UsuarioRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val COLECCION_USUARIOS = "usuarios"

class UsuarioRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : UsuarioRepository {

    override suspend fun obtenerUsuario(uid: String): Usuario? {
        val snapshot = firestore.collection(COLECCION_USUARIOS).document(uid).get().await()
        return snapshot.toUsuario()
    }

    override fun observarPacientesDe(fisioterapeutaId: String): Flow<List<Usuario>> = callbackFlow {
        val registro = firestore.collection(COLECCION_USUARIOS)
            .whereEqualTo("fisioterapeutaId", fisioterapeutaId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.toUsuario() } ?: emptyList())
            }
        awaitClose { registro.remove() }
    }
}

private fun DocumentSnapshot.toUsuario(): Usuario? {
    if (!exists()) return null
    val rolStr = getString("rol") ?: return null
    return Usuario(
        uid = id,
        nombre = getString("nombre") ?: "",
        email = getString("email") ?: "",
        rol = Rol.desdeFirestore(rolStr),
        fisioterapeutaId = getString("fisioterapeutaId"),
        fechaRegistro = getDate("fechaRegistro"),
    )
}
