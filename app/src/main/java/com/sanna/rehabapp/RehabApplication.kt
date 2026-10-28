package com.sanna.rehabapp

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RehabApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Pruebas locales: BuildConfig.USAR_EMULADOR_FIREBASE es false por defecto y siempre
        // false en release; solo es true en un build debug compilado con
        // -PusarEmuladorFirebase=true. Debe ejecutarse antes de cualquier otro uso de Firebase.
        if (BuildConfig.USAR_EMULADOR_FIREBASE) {
            // "localhost" del dispositivo apunta al PC mediante `adb reverse`.
            FirebaseAuth.getInstance().useEmulator("localhost", 9099)
            FirebaseFirestore.getInstance().apply {
                useEmulator("localhost", 8080)
                firestoreSettings = firestoreSettings { isPersistenceEnabled = false }
            }
        }
    }
}
