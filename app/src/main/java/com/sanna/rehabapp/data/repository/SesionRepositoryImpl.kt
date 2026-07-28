package com.sanna.rehabapp.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sanna.rehabapp.domain.model.ResultadoSesion
import com.sanna.rehabapp.domain.repository.SesionRepository
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
}
