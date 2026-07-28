package com.sanna.rehabapp.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.sanna.rehabapp.domain.model.EstadoSesion
import com.sanna.rehabapp.domain.model.ResultadoSesion
import com.sanna.rehabapp.domain.model.Sesion
import com.sanna.rehabapp.domain.repository.SesionRepository
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
                "angulosDetectados" to resultado.angulosDetectados,
                "desviacionPromedio" to resultado.desviacionPromedio,
                "porcentajeEjecucion" to resultado.porcentajeEjecucion,
                "tipoError" to resultado.tipoError,
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
}

private fun DocumentSnapshot.toSesion(): Sesion? {
    if (!exists()) return null
    val estadoStr = getString("estado") ?: return null
    val resultadoMap = get("resultado") as? Map<*, *>
    val resultado = resultadoMap?.let {
        ResultadoSesion(
            angulosDetectados = (it["angulosDetectados"] as? List<*>)
                ?.mapNotNull { valor -> (valor as? Number)?.toFloat() }
                ?: emptyList(),
            desviacionPromedio = (it["desviacionPromedio"] as? Number)?.toFloat() ?: 0f,
            porcentajeEjecucion = (it["porcentajeEjecucion"] as? Number)?.toFloat() ?: 0f,
            tipoError = it["tipoError"] as? String,
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
