package com.sanna.rehabapp.domain.repository

import android.net.Uri
import com.sanna.rehabapp.domain.model.Ejercicio
import kotlinx.coroutines.flow.Flow

interface EjercicioRepository {
    fun observarEjercicios(): Flow<List<Ejercicio>>

    suspend fun obtenerEjercicio(id: String): Ejercicio?

    suspend fun guardarEjercicio(ejercicio: Ejercicio, archivoMaterial: Uri?): Result<Unit>

    suspend fun eliminarEjercicio(id: String): Result<Unit>

    // HU02-CA10 (actualización del modelo de datos): activar/desactivar un
    // ejercicio en vez de eliminarlo, cuando ya tiene sesiones históricas
    // asociadas — lo oculta de la asignación (HU03) sin perder su historial.
    suspend fun cambiarEstadoActivo(id: String, activo: Boolean): Result<Unit>
}
