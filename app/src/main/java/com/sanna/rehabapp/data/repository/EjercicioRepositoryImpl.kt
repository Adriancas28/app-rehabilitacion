package com.sanna.rehabapp.data.repository

import android.net.Uri
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.model.PatronReferencia
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

private const val COLECCION_EJERCICIOS = "ejercicios"

class EjercicioRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) : EjercicioRepository {

    override fun observarEjercicios(): Flow<List<Ejercicio>> = callbackFlow {
        val registro = firestore.collection(COLECCION_EJERCICIOS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.toEjercicio() } ?: emptyList())
            }
        awaitClose { registro.remove() }
    }

    override suspend fun obtenerEjercicio(id: String): Ejercicio? {
        val snapshot = firestore.collection(COLECCION_EJERCICIOS).document(id).get().await()
        return snapshot.toEjercicio()
    }

    override suspend fun guardarEjercicio(ejercicio: Ejercicio, archivoMaterial: Uri?): Result<Unit> = runCatching {
        val docRef = if (ejercicio.id.isBlank()) {
            firestore.collection(COLECCION_EJERCICIOS).document()
        } else {
            firestore.collection(COLECCION_EJERCICIOS).document(ejercicio.id)
        }

        val materialUrl = if (archivoMaterial != null) {
            subirMaterial(docRef.id, archivoMaterial)
        } else {
            ejercicio.materialUrl
        }

        val datos = mapOf(
            "nombre" to ejercicio.nombre,
            "descripcion" to ejercicio.descripcion,
            "categoria" to ejercicio.categoria,
            "materialUrl" to materialUrl,
            "patronesReferencia" to ejercicio.patronesReferencia.map {
                mapOf(
                    "articulacion" to it.articulacion,
                    "anguloMin" to it.anguloMin,
                    "anguloMax" to it.anguloMax,
                )
            },
            "creadoPor" to ejercicio.creadoPor,
            "fechaCreacion" to (ejercicio.fechaCreacion ?: FieldValue.serverTimestamp()),
            "activo" to ejercicio.activo,
        )
        docRef.set(datos).await()
        Unit
    }

    override suspend fun eliminarEjercicio(id: String): Result<Unit> = runCatching {
        firestore.collection(COLECCION_EJERCICIOS).document(id).delete().await()
        Unit
    }

    private suspend fun subirMaterial(ejercicioId: String, archivo: Uri): String {
        val nombreArchivo = UUID.randomUUID().toString()
        val referencia = storage.reference.child("ejercicios/$ejercicioId/$nombreArchivo")
        referencia.putFile(archivo).await()
        return referencia.downloadUrl.await().toString()
    }
}

private fun DocumentSnapshot.toEjercicio(): Ejercicio? {
    if (!exists()) return null
    val patrones = (get("patronesReferencia") as? List<*>)
        ?.mapNotNull { (it as? Map<*, *>)?.toPatronReferencia() }
        ?: emptyList()
    return Ejercicio(
        id = id,
        nombre = getString("nombre") ?: "",
        descripcion = getString("descripcion") ?: "",
        categoria = getString("categoria") ?: "",
        materialUrl = getString("materialUrl") ?: "",
        patronesReferencia = patrones,
        creadoPor = getString("creadoPor") ?: "",
        fechaCreacion = getDate("fechaCreacion"),
        activo = getBoolean("activo") ?: true,
    )
}

private fun Map<*, *>.toPatronReferencia(): PatronReferencia? {
    val articulacion = this["articulacion"] as? String ?: return null
    return PatronReferencia(
        articulacion = articulacion,
        anguloMin = (this["anguloMin"] as? Number)?.toFloat() ?: 0f,
        anguloMax = (this["anguloMax"] as? Number)?.toFloat() ?: 0f,
    )
}
