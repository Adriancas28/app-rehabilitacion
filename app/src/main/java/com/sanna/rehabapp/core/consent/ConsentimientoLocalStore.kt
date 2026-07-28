package com.sanna.rehabapp.core.consent

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStoreConsentimiento by preferencesDataStore(name = "consentimiento_informado")
private val CLAVE_ACEPTADO = booleanPreferencesKey("aceptado")

// RNF06-CA04: recuerda localmente que el usuario ya aceptó el consentimiento
// informado de tratamiento de datos. Es una preferencia del dispositivo, no
// se sincroniza a Firestore.
@Singleton
class ConsentimientoLocalStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val consentimientoAceptado: Flow<Boolean> = context.dataStoreConsentimiento.data
        .map { preferencias -> preferencias[CLAVE_ACEPTADO] ?: false }

    suspend fun marcarAceptado() {
        context.dataStoreConsentimiento.edit { preferencias ->
            preferencias[CLAVE_ACEPTADO] = true
        }
    }
}
