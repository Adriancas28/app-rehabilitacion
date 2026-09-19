package com.sanna.rehabapp.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.sanna.rehabapp.domain.model.DetalleRepeticion
import com.sanna.rehabapp.domain.model.ErrorDetectado
import com.sanna.rehabapp.domain.model.EstadoSesion
import com.sanna.rehabapp.domain.model.ResultadoSesion
import com.sanna.rehabapp.domain.model.Sesion
import com.sanna.rehabapp.domain.repository.SesionRepository
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SesionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: com.google.firebase.storage.FirebaseStorage,
) : SesionRepository {

    private fun documento(sesionId: String) = firestore.collection(COL_SESIONES).document(sesionId)

    // Diccionario de datos: `sesiones` es una colección de nivel superior y el
    // resultado va aplanado en el propio documento (porcentajeEjecucion,
    // desviacionPromedio, repeticiones[], repeticionesCompletadas/Correctas).
    // Cada elemento de `repeticiones` lleva {numero, porcentaje,
    // desviacionAngular, error, articulacion} del diccionario y además
    // `errores` (todos los errores de esa repetición con ángulos y segundo),
    // ampliación necesaria para el detalle del fisioterapeuta (HU18-CA05).
    override suspend fun guardarResultado(
        pacienteId: String,
        sesionId: String,
        resultado: ResultadoSesion,
    ): Result<Unit> = runCatching {
        val datos = mapOf(
            "estado" to EstadoSesion.COMPLETADA.aFirestore(),
            "fechaEjecucion" to FieldValue.serverTimestamp(),
            "porcentajeEjecucion" to resultado.porcentajeEjecucion,
            "desviacionPromedio" to resultado.desviacionPromedio,
            "repeticionesAsignadas" to resultado.repeticionesAsignadas,
            "repeticionesCompletadas" to resultado.repeticionesCompletadas,
            "repeticionesCorrectas" to resultado.repeticionesCorrectas,
            "repeticiones" to resultado.detallePorRepeticion.map { detalle ->
                val principal = detalle.errores.firstOrNull()
                val desviaciones = detalle.errores.mapNotNull { e ->
                    val detectado = e.anguloDetectado
                    val esperado = e.anguloEsperado
                    if (detectado != null && esperado != null) Math.abs(detectado - esperado) else null
                }
                mapOf(
                    "numero" to detalle.numero,
                    "porcentaje" to detalle.porcentajeEjecucion,
                    "desviacionAngular" to (if (desviaciones.isEmpty()) 0f else desviaciones.average().toFloat()),
                    "error" to principal?.tipo,
                    "articulacion" to principal?.articulacion,
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
        )
        documento(sesionId).set(datos, SetOptions.merge()).await()
        Unit
    }

    // Ordenar en memoria evita índices compuestos.
    override fun observarSesionesDe(pacienteId: String, fisioterapeutaId: String?): Flow<List<Sesion>> {
        var consulta: Query = firestore.collection(COL_SESIONES).whereEqualTo("pacienteId", pacienteId)
        if (fisioterapeutaId != null) consulta = consulta.whereEqualTo("fisioterapeutaId", fisioterapeutaId)
        return escuchar(consulta).map { snap -> ordenar(snap.documents) }
    }

    // HU18-CA01/CA03 — todas las sesiones de todos los pacientes de este fisioterapeuta.
    override fun observarTodasLasSesionesDe(fisioterapeutaId: String): Flow<List<Sesion>> =
        escuchar(firestore.collection(COL_SESIONES).whereEqualTo("fisioterapeutaId", fisioterapeutaId))
            .map { snap -> ordenar(snap.documents) }

    // Etapa 2A (dashboard Admin) — sin filtro: el admin necesita el agregado
    // completo (esAdmin() en las Security Rules).
    override fun observarTodasLasSesiones(): Flow<List<Sesion>> =
        escuchar(firestore.collection(COL_SESIONES)).map { snap -> ordenar(snap.documents) }

    private fun ordenar(documentos: List<DocumentSnapshot>): List<Sesion> =
        documentos.mapNotNull { it.toSesion() }.sortedByDescending { it.fechaAsignacion }

    override suspend fun subirVideoSesion(
        pacienteId: String,
        sesionId: String,
        archivo: java.io.File,
    ): Result<Unit> = runCatching {
        val referencia = storage.reference.child("sesiones/$pacienteId/$sesionId.mp4")
        val metadatos = com.google.firebase.storage.StorageMetadata.Builder().setContentType("video/mp4").build()
        referencia.putFile(android.net.Uri.fromFile(archivo), metadatos).await()
        val url = referencia.downloadUrl.await().toString()
        documento(sesionId).set(mapOf("videoUrl" to url), SetOptions.merge()).await()
        Unit
    }

    override suspend fun obtenerSesion(pacienteId: String, sesionId: String): Sesion? =
        documento(sesionId).get().await().toSesion()

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
            "pacienteId" to pacienteId,
            "ejercicioId" to ejercicioId,
            "fisioterapeutaId" to fisioterapeutaId,
            "fechaAsignacion" to fechaAsignacion,
            "estado" to EstadoSesion.PENDIENTE.aFirestore(),
            "nota" to notas,
            "duracionSegundos" to duracionSegundos,
            "anguloMinOverride" to anguloMinOverride,
            "anguloMaxOverride" to anguloMaxOverride,
        ) + planDeRepeticiones(ejercicioId, repeticiones, duracionSegundos)
        firestore.collection(COL_SESIONES).add(datos).await()
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
            "nota" to notas,
            "duracionSegundos" to duracionSegundos,
            "anguloMinOverride" to anguloMinOverride,
            "anguloMaxOverride" to anguloMaxOverride,
        ) + planDeRepeticiones(ejercicioId, repeticiones, duracionSegundos)
        documento(sesionId).set(datos, SetOptions.merge()).await()
        Unit
    }

    // repeticionesAsignadas y duracionEstimada (diccionario): se fijan al
    // asignar con el override del fisioterapeuta o, si no hay, los valores por
    // defecto del ejercicio.
    private suspend fun planDeRepeticiones(
        ejercicioId: String,
        repeticiones: Int?,
        duracionSegundos: Int?,
    ): Map<String, Any> {
        val plan = firestore.collection(COL_EJERCICIOS).document(ejercicioId).get().await()
            .get("repeticiones") as? Map<*, *>
        val cantidad = repeticiones ?: (plan?.get("cantidad") as? Number)?.toInt() ?: 1
        val duracion = duracionSegundos ?: (plan?.get("duracionSeg") as? Number)?.toInt() ?: 30
        return mapOf("repeticionesAsignadas" to cantidad, "duracionEstimada" to cantidad * duracion)
    }
}

private fun DocumentSnapshot.toSesion(): Sesion? {
    if (!exists()) return null
    val estadoStr = getString("estado") ?: return null
    val detalle = (get("repeticiones") as? List<*>)
        ?.mapNotNull { entrada -> (entrada as? Map<*, *>)?.toDetalleRepeticion() }
        ?: emptyList()
    val asignadas = (get("repeticionesAsignadas") as? Number)?.toInt()
    val porcentaje = (get("porcentajeEjecucion") as? Number)?.toFloat()
    val resultado = porcentaje?.let {
        ResultadoSesion(
            desviacionPromedio = (get("desviacionPromedio") as? Number)?.toFloat() ?: 0f,
            porcentajeEjecucion = it,
            // Agregado por tipo de error, derivado del detalle por repetición
            // (el diccionario no lo guarda aparte).
            erroresDetectados = detalle.flatMap { d -> d.errores }
                .groupBy { e -> e.articulacion to e.tipo }
                .map { (clave, lista) ->
                    ErrorDetectado(
                        articulacion = clave.first,
                        tipo = clave.second,
                        repeticiones = lista.sumOf { e -> e.repeticiones },
                    )
                },
            repeticionesCompletadas = (get("repeticionesCompletadas") as? Number)?.toInt() ?: 0,
            repeticionesAsignadas = asignadas ?: 0,
            repeticionesCorrectas = (get("repeticionesCorrectas") as? Number)?.toInt() ?: 0,
            detallePorRepeticion = detalle,
        )
    }
    return Sesion(
        id = id,
        pacienteId = getString("pacienteId"),
        ejercicioId = getString("ejercicioId") ?: "",
        fisioterapeutaId = getString("fisioterapeutaId") ?: "",
        fechaAsignacion = getDate("fechaAsignacion"),
        fechaEjecucion = getDate("fechaEjecucion"),
        estado = EstadoSesion.desdeFirestore(estadoStr),
        notas = getString("nota"),
        repeticiones = asignadas,
        duracionSegundos = (get("duracionSegundos") as? Number)?.toInt(),
        anguloMinOverride = (get("anguloMinOverride") as? Number)?.toFloat(),
        anguloMaxOverride = (get("anguloMaxOverride") as? Number)?.toFloat(),
        resultado = resultado,
        videoUrl = getString("videoUrl"),
    )
}

private fun Map<*, *>.toErrorDetectado(): ErrorDetectado? {
    val articulacion = this["articulacion"] as? String ?: return null
    val tipo = this["tipo"] as? String ?: return null
    return ErrorDetectado(
        articulacion = articulacion,
        tipo = tipo,
        repeticiones = (this["repeticiones"] as? Number)?.toInt() ?: 1,
        anguloDetectado = (this["anguloDetectado"] as? Number)?.toFloat(),
        anguloEsperado = (this["anguloEsperado"] as? Number)?.toFloat(),
        segundo = (this["segundo"] as? Number)?.toInt(),
    )
}

private fun Map<*, *>.toDetalleRepeticion(): DetalleRepeticion? {
    val numero = (this["numero"] as? Number)?.toInt() ?: return null
    val errores = (this["errores"] as? List<*>)
        ?.mapNotNull { entrada -> (entrada as? Map<*, *>)?.toErrorDetectado() }
        // Documentos que solo traen el error principal del diccionario.
        ?: listOfNotNull(
            (this["error"] as? String)?.let { tipo ->
                ErrorDetectado(articulacion = this["articulacion"] as? String ?: "", tipo = tipo)
            },
        )
    return DetalleRepeticion(
        numero = numero,
        porcentajeEjecucion = (this["porcentaje"] as? Number)?.toFloat() ?: 0f,
        errores = errores,
    )
}
