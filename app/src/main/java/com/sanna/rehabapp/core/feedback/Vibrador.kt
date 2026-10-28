package com.sanna.rehabapp.core.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

// HU10 — retroalimentación háptica de la guía visual. Es una interfaz para que el
// ViewModel se pruebe sin dispositivo (en las pruebas se usa un doble).
interface Vibrador {
    // Pulso corto: una acción de la repetición terminó.
    fun pulsoAccion()

    // Pulso largo: se completó una repetición.
    fun pulsoRepeticion()
}

private const val DURACION_PULSO_ACCION_MS = 60L
private const val DURACION_PULSO_REPETICION_MS = 220L

@Singleton
class VibradorAndroid @Inject constructor(
    @ApplicationContext private val contexto: Context,
) : Vibrador {

    private val vibrador: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            contexto.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            contexto.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun pulsoAccion() = vibrar(DURACION_PULSO_ACCION_MS)

    override fun pulsoRepeticion() = vibrar(DURACION_PULSO_REPETICION_MS)

    private fun vibrar(milisegundos: Long) {
        val v = vibrador ?: return
        if (!v.hasVibrator()) return
        runCatching { v.vibrate(VibrationEffect.createOneShot(milisegundos, VibrationEffect.DEFAULT_AMPLITUDE)) }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FeedbackModule {
    @Binds
    @Singleton
    abstract fun ligarVibrador(impl: VibradorAndroid): Vibrador
}
