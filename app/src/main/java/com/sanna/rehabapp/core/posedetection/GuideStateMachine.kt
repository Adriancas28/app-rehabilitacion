package com.sanna.rehabapp.core.posedetection

import com.sanna.rehabapp.domain.model.Accion
import com.sanna.rehabapp.domain.model.ResultadoAccion
import kotlin.math.abs
import kotlin.math.ceil

// Guía visual en vivo, etapa 4: máquina de estados GENÉRICA repetición → acción.
// Pura (sin Android): el tiempo llega como parámetro, así que se prueba con
// cuadros simulados. No conoce ningún ejercicio: solo recorre la lista de
// acciones que se le entrega.

// Qué mensaje corresponde mostrar o decir; la interfaz y la voz lo traducen a texto.
enum class MensajeGuia { REUBICARSE, AVANZAR_MAS, REGRESAR_UN_POCO, MANTENER, ACCION_CUMPLIDA }

sealed interface EventoGuia {
    data class AccionTerminada(val repeticion: Int, val resultado: ResultadoAccion) : EventoGuia
    data class RepeticionCompletada(val numero: Int, val resultados: List<ResultadoAccion>) : EventoGuia
    data object SesionTerminada : EventoGuia

    // Más de `limiteSesionSinPoseMs` sin cuadros confiables: la sesión debe finalizar.
    data object AusenciaProlongada : EventoGuia
}

data class GuideUiState(
    val accion: Accion? = null,
    val guia: GuideFrame? = null,
    val repeticion: Int = 1,
    val totalRepeticiones: Int = 1,
    // Posición (desde 1) de la acción actual y total de acciones por repetición.
    val numeroAccion: Int = 1,
    val totalAcciones: Int = 1,
    // Segundos que faltan para completar el mantenimiento; null si la acción no lo pide o aún no está en zona.
    val segundosMantener: Int? = null,
    val segundosRestantesAccion: Int = 0,
    // false si el último cuadro no sirvió (hay que pedir al paciente reubicarse).
    val confiable: Boolean = true,
    val mensaje: MensajeGuia? = null,
    val terminada: Boolean = false,
    val enPausa: Boolean = false,
)

class GuideStateMachine(
    private val acciones: List<Accion>,
    private val totalRepeticiones: Int,
    repeticionInicial: Int = 1,
    // Sin cuadros confiables durante este tiempo, la acción actual se da por no completada.
    private val limiteAccionSinPoseMs: Long = 20_000L,
    // Sin cuadros confiables durante este tiempo, se avisa que la sesión debe finalizar.
    private val limiteSesionSinPoseMs: Long = 60_000L,
) {
    private var repeticionActual = repeticionInicial
    private var indiceAccion = 0
    private var tiempoAccionMs = 0L
    private var enZonaDesdeMs: Long? = null
    private var mejorAngulo: Float? = null
    private var ultimoInstanteMs: Long? = null
    private var ausenciaTotalDesdeMs: Long? = null
    private var ausenciaAccionDesdeMs: Long? = null
    private var terminada = false
    private var enPausa = false
    private var ultimoEstado = GuideUiState(totalRepeticiones = totalRepeticiones)

    private val resultadosRepeticionActual = mutableListOf<ResultadoAccion>()
    private val cerradas = mutableListOf<List<ResultadoAccion>>()

    init {
        require(acciones.isNotEmpty()) { "El ejercicio necesita al menos una acción" }
        terminada = repeticionActual > totalRepeticiones
        ultimoEstado = construirEstado(null, confiable = true, mensaje = null, held = 0L)
    }

    val estado: GuideUiState get() = ultimoEstado

    // Resultados de las repeticiones ya cerradas, en orden.
    val repeticionesCerradas: List<List<ResultadoAccion>> get() = cerradas.toList()

    // Acciones de la repetición en curso que ya terminaron (para guardar un resultado parcial).
    val accionesDeRepeticionAbierta: List<ResultadoAccion> get() = resultadosRepeticionActual.toList()

    fun pausar() {
        enPausa = true
        ultimoEstado = ultimoEstado.copy(enPausa = true)
    }

    // Al reanudar (ej. tras la pausa entre repeticiones) el reloj de la acción arranca de cero.
    fun reanudar(ahoraMs: Long) {
        enPausa = false
        ultimoInstanteMs = ahoraMs
        tiempoAccionMs = 0L
        enZonaDesdeMs = null
        ausenciaTotalDesdeMs = null
        ausenciaAccionDesdeMs = null
        ultimoEstado = ultimoEstado.copy(enPausa = false)
    }

    // `guia` es null cuando el cuadro no es confiable (sin persona o landmarks ocultos).
    fun procesar(guia: GuideFrame?, ahoraMs: Long): List<EventoGuia> {
        val anterior = ultimoInstanteMs
        ultimoInstanteMs = ahoraMs
        if (terminada || enPausa) return emptyList()
        // Un salto grande entre cuadros (cámara detenida) no debe gastar el tiempo de la acción.
        val dt = if (anterior == null) 0L else (ahoraMs - anterior).coerceIn(0L, DT_MAXIMO_MS)
        val eventos = mutableListOf<EventoGuia>()
        val accion = acciones[indiceAccion]

        if (guia == null) {
            val desdeTotal = ausenciaTotalDesdeMs ?: ahoraMs.also { ausenciaTotalDesdeMs = it }
            val desdeAccion = ausenciaAccionDesdeMs ?: ahoraMs.also { ausenciaAccionDesdeMs = it }
            // El mantenimiento queda congelado mientras no hay pose confiable.
            enZonaDesdeMs = enZonaDesdeMs?.plus(dt)
            when {
                ahoraMs - desdeTotal >= limiteSesionSinPoseMs -> {
                    terminada = true
                    eventos += EventoGuia.AusenciaProlongada
                }
                ahoraMs - desdeAccion >= limiteAccionSinPoseMs -> {
                    cerrarAccion(accion, completada = false, eventos = eventos)
                    ausenciaAccionDesdeMs = ahoraMs
                }
            }
            ultimoEstado = construirEstado(null, confiable = false, mensaje = MensajeGuia.REUBICARSE, held = 0L)
            return eventos
        }

        ausenciaTotalDesdeMs = null
        ausenciaAccionDesdeMs = null
        tiempoAccionMs += dt
        mejorAngulo = mejorEntre(mejorAngulo, guia.anguloActual, accion)

        val heldMs: Long
        if (guia.enZona) {
            val desde = enZonaDesdeMs ?: ahoraMs.also { enZonaDesdeMs = it }
            heldMs = ahoraMs - desde
        } else {
            enZonaDesdeMs = null
            heldMs = 0L
        }

        val cumplida = guia.enZona && heldMs >= accion.mantenerSeg * 1000L
        val sinTiempo = tiempoAccionMs >= accion.tiempoMaxSeg * 1000L
        val mensaje = when {
            cumplida -> MensajeGuia.ACCION_CUMPLIDA
            guia.enZona -> MensajeGuia.MANTENER
            noLlegoAun(guia.anguloActual, accion) -> MensajeGuia.AVANZAR_MAS
            else -> MensajeGuia.REGRESAR_UN_POCO
        }
        val estadoAntesDeCerrar = construirEstado(guia, confiable = true, mensaje = mensaje, held = heldMs)

        when {
            cumplida -> cerrarAccion(accion, completada = true, eventos = eventos)
            sinTiempo -> cerrarAccion(accion, completada = false, eventos = eventos)
        }
        ultimoEstado = if (eventos.isEmpty()) estadoAntesDeCerrar else construirEstado(null, confiable = true, mensaje = mensaje.takeIf { cumplida }, held = 0L)
        return eventos
    }

    private fun cerrarAccion(accion: Accion, completada: Boolean, eventos: MutableList<EventoGuia>) {
        val mejor = mejorAngulo ?: accion.anguloInicio
        val resultado = ResultadoAccion(
            ordenAccion = accion.orden,
            anguloMaximo = mejor,
            completada = completada,
            tiempoSeg = tiempoAccionMs / 1000f,
            desviacionAngular = if (completada) 0f else (abs(mejor - accion.anguloMeta) - accion.tolerancia).coerceAtLeast(0f),
        )
        resultadosRepeticionActual += resultado
        eventos += EventoGuia.AccionTerminada(repeticionActual, resultado)
        tiempoAccionMs = 0L
        enZonaDesdeMs = null
        mejorAngulo = null
        indiceAccion += 1
        if (indiceAccion >= acciones.size) {
            val copia = resultadosRepeticionActual.toList()
            cerradas += copia
            resultadosRepeticionActual.clear()
            eventos += EventoGuia.RepeticionCompletada(repeticionActual, copia)
            indiceAccion = 0
            repeticionActual += 1
            if (repeticionActual > totalRepeticiones) {
                terminada = true
                eventos += EventoGuia.SesionTerminada
            }
        }
    }

    // Para metas mayores que el inicio el "mejor" ángulo es el máximo; para metas menores, el mínimo.
    private fun mejorEntre(actual: Float?, nuevo: Float, accion: Accion): Float =
        when {
            actual == null -> nuevo
            accion.anguloMeta >= accion.anguloInicio -> maxOf(actual, nuevo)
            else -> minOf(actual, nuevo)
        }

    private fun noLlegoAun(angulo: Float, accion: Accion): Boolean =
        if (accion.anguloMeta >= accion.anguloInicio) angulo < accion.anguloMeta else angulo > accion.anguloMeta

    private fun construirEstado(guia: GuideFrame?, confiable: Boolean, mensaje: MensajeGuia?, held: Long): GuideUiState {
        val accion = acciones[indiceAccion.coerceIn(0, acciones.lastIndex)]
        val restanteMs = (accion.tiempoMaxSeg * 1000L - tiempoAccionMs).coerceAtLeast(0L)
        val mantener = if (accion.mantenerSeg > 0 && guia?.enZona == true) {
            ceil((accion.mantenerSeg * 1000L - held).coerceAtLeast(0L) / 1000.0).toInt()
        } else {
            null
        }
        return GuideUiState(
            accion = accion,
            guia = guia,
            repeticion = repeticionActual.coerceAtMost(totalRepeticiones),
            totalRepeticiones = totalRepeticiones,
            numeroAccion = indiceAccion + 1,
            totalAcciones = acciones.size,
            segundosMantener = mantener,
            segundosRestantesAccion = ceil(restanteMs / 1000.0).toInt(),
            confiable = confiable,
            mensaje = mensaje,
            terminada = terminada,
            enPausa = enPausa,
        )
    }

    private companion object {
        const val DT_MAXIMO_MS = 500L
    }
}
