package com.sanna.rehabapp.core.feedback

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStoreRetroalimentacion by preferencesDataStore(name = "ajustes_retroalimentacion")
private val CLAVE_VOZ = booleanPreferencesKey("voz_activa")
private val CLAVE_VIBRACION = booleanPreferencesKey("vibracion_activa")

// HU10 — interruptores de voz y vibración de la guía visual. Son preferencias del
// dispositivo (no se sincronizan a Firestore) y por defecto están activados.
@Singleton
class AjustesRetroalimentacion @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val vozActiva: Flow<Boolean> = context.dataStoreRetroalimentacion.data.map { it[CLAVE_VOZ] ?: true }
    val vibracionActiva: Flow<Boolean> = context.dataStoreRetroalimentacion.data.map { it[CLAVE_VIBRACION] ?: true }

    suspend fun cambiarVoz(activa: Boolean) {
        context.dataStoreRetroalimentacion.edit { it[CLAVE_VOZ] = activa }
    }

    suspend fun cambiarVibracion(activa: Boolean) {
        context.dataStoreRetroalimentacion.edit { it[CLAVE_VIBRACION] = activa }
    }
}
