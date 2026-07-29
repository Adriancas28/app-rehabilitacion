package com.sanna.rehabapp.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.sanna.rehabapp.domain.model.Articulacion
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.model.PatronReferencia
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

private const val COLECCION_EJERCICIOS = "ejercicios"

class EjercicioRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
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
            "duracionSegundos" to ejercicio.duracionSegundos,
            "repeticiones" to ejercicio.repeticiones,
            "patronesReferencia" to ejercicio.patronesReferencia.map {
                mapOf(
                    "articulacion" to it.articulacion.aFirestore(),
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
        // HU05-CA02 necesita distinguir imagen de video al reproducir el
        // material; sin la extensión en el nombre no hay forma de saberlo
        // a partir de la sola downloadUrl (son URLs firmadas, no el path
        // original del archivo).
        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(context.contentResolver.getType(archivo))
        val nombreArchivo = UUID.randomUUID().toString() + if (extension != null) ".$extension" else ""
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
        duracionSegundos = getLong("duracionSegundos")?.toInt() ?: 30,
        repeticiones = getLong("repeticiones")?.toInt() ?: 1,
        patronesReferencia = patrones,
        creadoPor = getString("creadoPor") ?: "",
        fechaCreacion = getDate("fechaCreacion"),
        activo = getBoolean("activo") ?: true,
    )
}

private fun Map<*, *>.toPatronReferencia(): PatronReferencia? {
    // Ejercicios creados antes de Sprint 3 pueden tener articulacion como
    // texto libre; ya no son un valor valido del enum y se descartan aqui.
    val articulacion = Articulacion.desdeFirestoreOrNull(this["articulacion"] as? String) ?: return null
    return PatronReferencia(
        articulacion = articulacion,
        anguloMin = (this["anguloMin"] as? Number)?.toFloat() ?: 0f,
        anguloMax = (this["anguloMax"] as? Number)?.toFloat() ?: 0f,
    )
}
