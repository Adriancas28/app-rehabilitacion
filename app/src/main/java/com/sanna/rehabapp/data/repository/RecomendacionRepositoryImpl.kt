package com.sanna.rehabapp.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.sanna.rehabapp.domain.model.Recomendacion
import com.sanna.rehabapp.domain.repository.RecomendacionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val COLECCION_USUARIOS = "usuarios"
private const val SUBCOLECCION_SESIONES = "sesiones"
private const val SUBCOLECCION_RECOMENDACIONES = "recomendaciones"

class RecomendacionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : RecomendacionRepository {

    private fun coleccion(pacienteId: String, sesionId: String) = firestore
        .collection(COLECCION_USUARIOS)
        .document(pacienteId)
        .collection(SUBCOLECCION_SESIONES)
        .document(sesionId)
        .collection(SUBCOLECCION_RECOMENDACIONES)

    override fun observarDe(pacienteId: String, sesionId: String): Flow<List<Recomendacion>> = callbackFlow {
        val registro = coleccion(pacienteId, sesionId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.toRecomendacion() } ?: emptyList())
            }
        awaitClose { registro.remove() }
    }

    override suspend fun crear(
        pacienteId: String,
        sesionId: String,
        fisioterapeutaId: String,
        texto: String,
    ): Result<Unit> = runCatching {
        val datos = mapOf(
            "fisioterapeutaId" to fisioterapeutaId,
            "texto" to texto,
            "fecha" to FieldValue.serverTimestamp(),
        )
        coleccion(pacienteId, sesionId).add(datos).await()
        Unit
    }

    override suspend fun actualizar(
        pacienteId: String,
        sesionId: String,
        recomendacionId: String,
        texto: String,
    ): Result<Unit> = runCatching {
        coleccion(pacienteId, sesionId)
            .document(recomendacionId)
            .set(mapOf("texto" to texto), SetOptions.merge())
            .await()
        Unit
    }

    override suspend fun eliminar(pacienteId: String, sesionId: String, recomendacionId: String): Result<Unit> =
        runCatching {
            coleccion(pacienteId, sesionId).document(recomendacionId).delete().await()
            Unit
        }
}

private fun DocumentSnapshot.toRecomendacion(): Recomendacion? {
    if (!exists()) return null
    val fisioterapeutaId = getString("fisioterapeutaId") ?: return null
    val texto = getString("texto") ?: return null
    return Recomendacion(
        id = id,
        fisioterapeutaId = fisioterapeutaId,
        texto = texto,
        fecha = getDate("fecha"),
    )
}
