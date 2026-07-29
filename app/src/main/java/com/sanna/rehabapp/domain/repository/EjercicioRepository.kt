package com.sanna.rehabapp.domain.repository

import android.net.Uri
import com.sanna.rehabapp.domain.model.Ejercicio
import kotlinx.coroutines.flow.Flow

interface EjercicioRepository {
    fun observarEjercicios(): Flow<List<Ejercicio>>

    suspend fun obtenerEjercicio(id: String): Ejercicio?

    suspend fun guardarEjercicio(ejercicio: Ejercicio, archivoMaterial: Uri?): Result<Unit>

    suspend fun eliminarEjercicio(id: String): Result<Unit>
}
