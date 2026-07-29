package com.sanna.rehabapp.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.sanna.rehabapp.domain.model.AnguloDetectado
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

    override fun observarSesionesDe(pacienteId: String): Flow<List<Sesion>> = callbackFlow {
        val registro = firestore.collection(COLECCION_USUARIOS)
            .document(pacienteId)
            .collection(SUBCOLECCION_SESIONES)
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
    ): Result<Unit> = runCatching {
        val datos = mapOf(
            "ejercicioId" to ejercicioId,
            "fisioterapeutaId" to fisioterapeutaId,
            "fechaAsignacion" to fechaAsignacion,
            "estado" to EstadoSesion.PENDIENTE.aFirestore(),
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
    ): Result<Unit> = runCatching {
        val datos = mapOf(
            "ejercicioId" to ejercicioId,
            "fechaAsignacion" to fechaAsignacion,
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
        )
    }
    return Sesion(
        id = id,
        ejercicioId = getString("ejercicioId") ?: "",
        fisioterapeutaId = getString("fisioterapeutaId") ?: "",
        fechaAsignacion = getDate("fechaAsignacion"),
        fechaEjecucion = getDate("fechaEjecucion"),
        estado = EstadoSesion.desdeFirestore(estadoStr),
        resultado = resultado,
        sincronizado = getBoolean("sincronizado") ?: true,
    )
}

private fun Map<*, *>.toErrorDetectado(): ErrorDetectado? {
    val articulacion = this["articulacion"] as? String ?: return null
    val tipo = this["tipo"] as? String ?: return null
    val repeticiones = (this["repeticiones"] as? Number)?.toInt() ?: 1
    return ErrorDetectado(articulacion = articulacion, tipo = tipo, repeticiones = repeticiones)
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
