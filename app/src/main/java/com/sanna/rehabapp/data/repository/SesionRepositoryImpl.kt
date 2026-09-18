package com.sanna.rehabapp.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.sanna.rehabapp.domain.model.AnguloDetectado
import com.sanna.rehabapp.domain.model.DetalleRepeticion
import com.sanna.rehabapp.domain.model.ErrorDetectado
import com.sanna.rehabapp.domain.model.EstadoSesion
import com.sanna.rehabapp.domain.model.ResultadoSesion
import com.sanna.rehabapp.domain.model.Sesion
import com.sanna.rehabapp.domain.repository.SesionRepository
import java.util.Date
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val COLECCION_USUARIOS = "usuarios"
private const val SUBCOLECCION_SESIONES = "sesiones"

class SesionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: com.google.firebase.storage.FirebaseStorage,
) : SesionRepository {

    override suspend fun guardarResultado(
        pacienteId: String,
        sesionId: String,
        resultado: ResultadoSesion,
    ): Result<Unit> = runCatching {
        val datos = mapOf(
            "estado" to "completada",
            "fechaEjecucion" to FieldValue.serverTimestamp(),
            "resultado" to mapOf(
                "angulosDetectados" to resultado.angulosDetectados.map {
                    mapOf(
                        "articulacion" to it.articulacion,
                        "anguloDetectado" to it.anguloDetectado,
                        "anguloEsperado" to it.anguloEsperado,
                        "desviacion" to it.desviacion,
                    )
                },
                "desviacionPromedio" to resultado.desviacionPromedio,
                "porcentajeEjecucion" to resultado.porcentajeEjecucion,
                "erroresDetectados" to resultado.erroresDetectados.map {
                    mapOf(
                        "articulacion" to it.articulacion,
                        "tipo" to it.tipo,
                        "repeticiones" to it.repeticiones,
                    )
                },
                "repeticionesCompletadas" to resultado.repeticionesCompletadas,
                "repeticionesAsignadas" to resultado.repeticionesAsignadas,
                "repeticionesCorrectas" to resultado.repeticionesCorrectas,
                "detallePorRepeticion" to resultado.detallePorRepeticion.map { detalle ->
                    mapOf(
                        "numero" to detalle.numero,
                        "porcentajeEjecucion" to detalle.porcentajeEjecucion,
                        "errores" to detalle.errores.map {
                            mapOf(
                                "articulacion" to it.articulacion,
                                "tipo" to it.tipo,
                                "repeticiones" to it.repeticiones,
                                "anguloDetectado" to it.anguloDetectado,
                                "anguloEsperado" to it.anguloEsperado,
                                "segundo" to it.segundo,
                            )
                        },
                    )
                },
            ),
            "sincronizado" to true,
        )
        firestore.collection(COLECCION_USUARIOS)
            .document(pacienteId)
            .collection(SUBCOLECCION_SESIONES)
            .document(sesionId)
            .set(datos, SetOptions.merge())
            .await()
        Unit
    }

    override fun observarSesionesDe(pacienteId: String, fisioterapeutaId: String?): Flow<List<Sesion>> = callbackFlow {
        val coleccion = firestore.collection(COLECCION_USUARIOS)
            .document(pacienteId)
            .collection(SUBCOLECCION_SESIONES)

        // Con filtro por fisioterapeutaId no se encadena orderBy: combinar un
        // whereEqualTo con un orderBy en otro campo requeriría un índice
        // compuesto nuevo en Firestore. Se ordena en memoria en su lugar.
        val consulta: Query = if (fisioterapeutaId != null) {
            coleccion.whereEqualTo("fisioterapeutaId", fisioterapeutaId)
        } else {
            coleccion.orderBy("fechaAsignacion", Query.Direction.DESCENDING)
        }

        val registro = consulta.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val sesiones = snapshot?.documents?.mapNotNull { it.toSesion() } ?: emptyList()
            trySend(if (fisioterapeutaId != null) sesiones.sortedByDescending { it.fechaAsignacion } else sesiones)
        }
        awaitClose { registro.remove() }
    }

    // HU18-CA01/CA03 — todas las sesiones de todos los pacientes de este
    // fisioterapeuta. Necesita el índice compuesto (fisioterapeutaId asc +
    // fechaAsignacion desc) ya declarado en firestore.indexes.json.
    override fun observarTodasLasSesionesDe(fisioterapeutaId: String): Flow<List<Sesion>> = callbackFlow {
        val registro = firestore.collectionGroup(SUBCOLECCION_SESIONES)
            .whereEqualTo("fisioterapeutaId", fisioterapeutaId)
            .orderBy("fechaAsignacion", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.toSesion() } ?: emptyList())
            }
        awaitClose { registro.remove() }
    }

    // Etapa 2A (dashboard Admin) — sin whereEqualTo/orderBy a propósito:
    // el admin necesita el agregado completo, no una vista filtrada, y así
    // no requiere un índice compuesto nuevo (ver Firestore Security Rules,
    // esAdmin() en allow list).
    override fun observarTodasLasSesiones(): Flow<List<Sesion>> = callbackFlow {
        val registro = firestore.collectionGroup(SUBCOLECCION_SESIONES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.toSesion() } ?: emptyList())
            }
        awaitClose { registro.remove() }
    }

    override suspend fun subirVideoSesion(
        pacienteId: String,
        sesionId: String,
        archivo: java.io.File,
    ): Result<Unit> = runCatching {
        val referencia = storage.reference.child("sesiones/$pacienteId/$sesionId.mp4")
        val metadatos = com.google.firebase.storage.StorageMetadata.Builder().setContentType("video/mp4").build()
        referencia.putFile(android.net.Uri.fromFile(archivo), metadatos).await()
        val url = referencia.downloadUrl.await().toString()
        firestore.collection(COLECCION_USUARIOS)
            .document(pacienteId)
            .collection(SUBCOLECCION_SESIONES)
            .document(sesionId)
            .set(mapOf("videoUrl" to url), SetOptions.merge())
            .await()
        Unit
    }

    override suspend fun obtenerSesion(pacienteId: String, sesionId: String): Sesion? {
        val snapshot = firestore.collection(COLECCION_USUARIOS)
            .document(pacienteId)
            .collection(SUBCOLECCION_SESIONES)
            .document(sesionId)
            .get()
            .await()
        return snapshot.toSesion()
    }

    override suspend fun asignarSesion(
        pacienteId: String,
        ejercicioId: String,
        fisioterapeutaId: String,
        fechaAsignacion: Date,
        notas: String?,
        repeticiones: Int?,
        duracionSegundos: Int?,
        anguloMinOverride: Float?,
        anguloMaxOverride: Float?,
    ): Result<Unit> = runCatching {
        val datos = mapOf(
            "ejercicioId" to ejercicioId,
            "fisioterapeutaId" to fisioterapeutaId,
            "fechaAsignacion" to fechaAsignacion,
            "estado" to EstadoSesion.PENDIENTE.aFirestore(),
            "notas" to notas,
            "repeticiones" to repeticiones,
            "duracionSegundos" to duracionSegundos,
            "anguloMinOverride" to anguloMinOverride,
            "anguloMaxOverride" to anguloMaxOverride,
            "sincronizado" to true,
        )
        firestore.collection(COLECCION_USUARIOS)
            .document(pacienteId)
            .collection(SUBCOLECCION_SESIONES)
            .add(datos)
            .await()
        Unit
    }

    override suspend fun actualizarSesion(
        pacienteId: String,
        sesionId: String,
        ejercicioId: String,
        fechaAsignacion: Date,
        notas: String?,
        repeticiones: Int?,
        duracionSegundos: Int?,
        anguloMinOverride: Float?,
        anguloMaxOverride: Float?,
    ): Result<Unit> = runCatching {
        val datos = mapOf(
            "ejercicioId" to ejercicioId,
            "fechaAsignacion" to fechaAsignacion,
            "notas" to notas,
            "repeticiones" to repeticiones,
            "duracionSegundos" to duracionSegundos,
            "anguloMinOverride" to anguloMinOverride,
            "anguloMaxOverride" to anguloMaxOverride,
        )
        firestore.collection(COLECCION_USUARIOS)
            .document(pacienteId)
            .collection(SUBCOLECCION_SESIONES)
            .document(sesionId)
            .set(datos, SetOptions.merge())
            .await()
        Unit
    }
}

private fun DocumentSnapshot.toSesion(): Sesion? {
    if (!exists()) return null
    val estadoStr = getString("estado") ?: return null
    val resultadoMap = get("resultado") as? Map<*, *>
    val resultado = resultadoMap?.let {
        ResultadoSesion(
            angulosDetectados = (it["angulosDetectados"] as? List<*>)
                ?.mapNotNull { valor -> (valor as? Map<*, *>)?.toAnguloDetectado() }
                ?: emptyList(),
            desviacionPromedio = (it["desviacionPromedio"] as? Number)?.toFloat() ?: 0f,
            porcentajeEjecucion = (it["porcentajeEjecucion"] as? Number)?.toFloat() ?: 0f,
            erroresDetectados = (it["erroresDetectados"] as? List<*>)
                ?.mapNotNull { entrada -> (entrada as? Map<*, *>)?.toErrorDetectado() }
                ?: emptyList(),
            repeticionesCompletadas = (it["repeticionesCompletadas"] as? Number)?.toInt() ?: 0,
            repeticionesAsignadas = (it["repeticionesAsignadas"] as? Number)?.toInt() ?: 0,
            repeticionesCorrectas = (it["repeticionesCorrectas"] as? Number)?.toInt() ?: 0,
            detallePorRepeticion = (it["detallePorRepeticion"] as? List<*>)
                ?.mapNotNull { entrada -> (entrada as? Map<*, *>)?.toDetalleRepeticion() }
                ?: emptyList(),
        )
    }
    return Sesion(
        id = id,
        // HU18-CA01: el documento padre de la subcolección sesiones es
        // siempre usuarios/{pacienteId}, tanto en consultas de un paciente
        // puntual como en la collection group query agregada.
        pacienteId = reference.parent.parent?.id,
        ejercicioId = getString("ejercicioId") ?: "",
        fisioterapeutaId = getString("fisioterapeutaId") ?: "",
        fechaAsignacion = getDate("fechaAsignacion"),
        fechaEjecucion = getDate("fechaEjecucion"),
        estado = EstadoSesion.desdeFirestore(estadoStr),
        notas = getString("notas"),
        repeticiones = (get("repeticiones") as? Number)?.toInt(),
        duracionSegundos = (get("duracionSegundos") as? Number)?.toInt(),
        anguloMinOverride = (get("anguloMinOverride") as? Number)?.toFloat(),
        anguloMaxOverride = (get("anguloMaxOverride") as? Number)?.toFloat(),
        resultado = resultado,
        videoUrl = getString("videoUrl"),
        sincronizado = getBoolean("sincronizado") ?: true,
    )
}

private fun Map<*, *>.toErrorDetectado(): ErrorDetectado? {
    val articulacion = this["articulacion"] as? String ?: return null
    val tipo = this["tipo"] as? String ?: return null
    val repeticiones = (this["repeticiones"] as? Number)?.toInt() ?: 1
    return ErrorDetectado(
        articulacion = articulacion,
        tipo = tipo,
        repeticiones = repeticiones,
        anguloDetectado = (this["anguloDetectado"] as? Number)?.toFloat(),
        anguloEsperado = (this["anguloEsperado"] as? Number)?.toFloat(),
        segundo = (this["segundo"] as? Number)?.toInt(),
    )
}

private fun Map<*, *>.toDetalleRepeticion(): DetalleRepeticion? {
    val numero = (this["numero"] as? Number)?.toInt() ?: return null
    return DetalleRepeticion(
        numero = numero,
        porcentajeEjecucion = (this["porcentajeEjecucion"] as? Number)?.toFloat() ?: 0f,
        errores = (this["errores"] as? List<*>)
            ?.mapNotNull { entrada -> (entrada as? Map<*, *>)?.toErrorDetectado() }
            ?: emptyList(),
    )
}

private fun Map<*, *>.toAnguloDetectado(): AnguloDetectado? {
    val articulacion = this["articulacion"] as? String ?: return null
    val anguloDetectado = (this["anguloDetectado"] as? Number)?.toFloat() ?: return null
    return AnguloDetectado(
        articulacion = articulacion,
        anguloDetectado = anguloDetectado,
        anguloEsperado = (this["anguloEsperado"] as? Number)?.toFloat(),
        desviacion = (this["desviacion"] as? Number)?.toFloat(),
    )
}
