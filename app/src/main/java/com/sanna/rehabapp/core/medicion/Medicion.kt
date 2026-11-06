package com.sanna.rehabapp.core.medicion

import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.sanna.rehabapp.BuildConfig
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap

// Instrumentación para las mediciones de validación (EDT 1.8.6). Solo escribe en logcat con la
// etiqueta MEDICION; no cambia la lógica de la app y queda inactiva salvo que se compile con
// -PmedirRendimiento=true. Todos los instantes usan SystemClock.elapsedRealtimeNanos().
//
// Formato de cada línea (clave=valor, tiempos en nanosegundos):
//   evento=sesion ...            al iniciar la sesión (dispositivo, compilación, ejercicio)
//   evento=fase valor=...        preparacion | monitoreo | descanso | fin
//   evento=repeticion n=N        una repetición completada
//   evento=resultado ts llegada resultado   MediaPipe entregó el resultado de un fotograma
//   evento=sin_resultado ts llegada         el fotograma llegó pero no produjo resultado
//   evento=dibujo ts dibujo      la guía de ese resultado se dibujó en pantalla
//   evento=angulo grados         ángulo calculado, como máximo 2 veces por segundo
const val ETIQUETA_MEDICION = "MEDICION"

class RegistroMedicion(
    private val escribir: (String) -> Unit,
    private val ahoraNs: () -> Long,
    private val limiteSinResultadoNs: Long = 2_000_000_000L,
    private val intervaloAnguloNs: Long = 500_000_000L,
) {
    private val llegadas = ConcurrentHashMap<Long, Long>()
    private val publicados = IdentityHashMap<Any, Long>()
    private var ultimoPublicado: Any? = null
    private var ultimoAnguloNs: Long? = null

    fun sesion(datos: String) = escribir("evento=sesion $datos")

    fun fase(valor: String) = escribir("evento=fase valor=$valor t=${ahoraNs()}")

    fun repeticion(numero: Int) = escribir("evento=repeticion n=$numero t=${ahoraNs()}")

    // El fotograma llegó al analizador de CameraX (antes de convertirlo y enviarlo a MediaPipe).
    fun llegada(tsMs: Long, llegadaNs: Long) {
        llegadas[tsMs] = llegadaNs
        val ahora = ahoraNs()
        val vencidos = llegadas.entries.filter { ahora - it.value > limiteSinResultadoNs }
        for (e in vencidos) {
            if (llegadas.remove(e.key, e.value)) escribir("evento=sin_resultado ts=${e.key} llegada=${e.value}")
        }
    }

    // MediaPipe entregó el resultado del fotograma con esa marca de tiempo.
    fun resultado(tsMs: Long) {
        val t = ahoraNs()
        val llegada = llegadas.remove(tsMs) ?: -1L
        escribir("evento=resultado ts=$tsMs llegada=$llegada resultado=$t")
    }

    // El ViewModel publicó el estado de la guía calculado con el resultado de ese fotograma.
    fun publicado(estado: Any, tsMs: Long) {
        synchronized(publicados) {
            if (estado === ultimoPublicado) return
            ultimoPublicado = estado
            if (publicados.size > 64) publicados.clear()
            publicados[estado] = tsMs
        }
    }

    // La guía de ese estado se dibujó; solo se registra la primera vez que se dibuja cada estado.
    fun dibujo(estado: Any) {
        val ts = synchronized(publicados) { publicados.remove(estado) } ?: return
        escribir("evento=dibujo ts=$ts dibujo=${ahoraNs()}")
    }

    fun angulo(grados: Float?) {
        if (grados == null) return
        val t = ahoraNs()
        val anterior = ultimoAnguloNs
        if (anterior != null && t - anterior < intervaloAnguloNs) return
        ultimoAnguloNs = t
        escribir("evento=angulo grados=${"%.2f".format(java.util.Locale.US, grados)} t=$t")
    }
}

object Medicion {
    val activa: Boolean = BuildConfig.MEDICION_ACTIVA

    private val registro: RegistroMedicion? =
        if (activa) RegistroMedicion(
            escribir = { Log.i(ETIQUETA_MEDICION, it) },
            ahoraNs = { SystemClock.elapsedRealtimeNanos() },
        ) else null

    fun ahoraNs(): Long = SystemClock.elapsedRealtimeNanos()

    fun sesion(ejercicio: String?, repeticiones: Int) {
        val r = registro ?: return
        val nombre = (ejercicio ?: "desconocido").replace(' ', '_')
        r.sesion(
            "dispositivo=${Build.MANUFACTURER}_${Build.MODEL}".replace(' ', '_') +
                " android=${Build.VERSION.RELEASE} sdk=${Build.VERSION.SDK_INT} abi=${Build.SUPPORTED_ABIS.firstOrNull()}" +
                " compilacion=${BuildConfig.BUILD_TYPE} depurable=${BuildConfig.DEBUG}" +
                " ejercicio=$nombre repeticiones_asignadas=$repeticiones t=${SystemClock.elapsedRealtimeNanos()}",
        )
    }

    fun fase(valor: String) { registro?.fase(valor) }
    fun repeticion(numero: Int) { registro?.repeticion(numero) }
    fun llegada(tsMs: Long, llegadaNs: Long) { registro?.llegada(tsMs, llegadaNs) }
    fun resultado(tsMs: Long) { registro?.resultado(tsMs) }
    fun publicado(estado: Any, tsMs: Long) { registro?.publicado(estado, tsMs) }
    fun dibujo(estado: Any) { registro?.dibujo(estado) }
    fun angulo(grados: Float?) { registro?.angulo(grados) }
}
