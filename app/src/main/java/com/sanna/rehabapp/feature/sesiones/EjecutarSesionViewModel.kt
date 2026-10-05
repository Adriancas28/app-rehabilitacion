package com.sanna.rehabapp.feature.sesiones

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.sanna.rehabapp.core.navigation.Rutas
import com.sanna.rehabapp.core.posedetection.EventoGuia
import com.sanna.rehabapp.core.posedetection.FiltroLandmarks
import com.sanna.rehabapp.core.posedetection.GuideStateMachine
import com.sanna.rehabapp.core.posedetection.GuideUiState
import com.sanna.rehabapp.core.posedetection.LandmarksFrame
import com.sanna.rehabapp.core.posedetection.MensajeGuia
import com.sanna.rehabapp.core.posedetection.PoseGuideCalculator
import com.sanna.rehabapp.core.posedetection.aFrame
import com.sanna.rehabapp.core.posedetection.construirResultadoPorAcciones
import com.sanna.rehabapp.core.posedetection.fraseDeGuia
import com.sanna.rehabapp.core.posedetection.mergearResultados
import com.sanna.rehabapp.domain.model.Accion
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.model.EstadoSesion
import com.sanna.rehabapp.domain.model.LadoAfectado
import com.sanna.rehabapp.domain.model.ResultadoSesion
import com.sanna.rehabapp.domain.model.VistaCamara
import com.sanna.rehabapp.domain.model.accionesEfectivas
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import com.sanna.rehabapp.domain.repository.SesionRepository
import com.sanna.rehabapp.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SEGUNDOS_DESCANSO_ENTRE_REPETICIONES = 5

// HU06-CA02 (ampliación): cuenta regresiva de preparación antes de medir la
// primera acción, para que el paciente se acomode frente a la cámara.
private const val SEGUNDOS_PREPARACION_INICIAL = 10

// HU06-CA06 (ampliación): a partir de cuántos segundos de descanso restantes
// se avisa por voz que viene la siguiente repetición.
private const val SEGUNDOS_AVISO_SIGUIENTE_REPETICION = 3

// HU10: no repetir el mismo mensaje por voz más seguido que esto.
private const val INTERVALO_REPETIR_VOZ_MS = 3_000L

// "Sigue el recorrido" solo se dice si el paciente lleva este tiempo sin llegar a la zona.
private const val ESPERA_ANTES_DE_INSISTIR_MS = 3_000L

// HU10: un evento de voz nuevo cada vez que hay que decir algo — el
// `instante` garantiza que la pantalla vuelva a hablar aunque el mensaje
// sea idéntico al anterior (LaunchedEffect solo reacciona a valores
// distintos).
data class EventoVoz(val mensaje: String, val instante: Long)

data class EjecutarSesionUiState(
    val ejercicio: Ejercicio? = null,
    // HU03-CA06: override de repeticiones para esta sesión puntual; si es
    // null, se usa el valor por defecto del ejercicio.
    val repeticionesOverride: Int? = null,
    val cargando: Boolean = true,
    val sesionIniciada: Boolean = false,
    // HU06-CA02 (ampliación): cuenta regresiva antes de que arranque la medición.
    val enPreparacion: Boolean = false,
    val segundosPreparacion: Int = 0,
    val repeticionActual: Int = 1,
    // HU06-CA07/HU11: repeticiones cerradas (todas sus acciones terminaron);
    // puede ser menos que totalRepeticiones si se finalizó antes.
    val repeticionesCompletadas: Int = 0,
    // Tiempo que le queda a la acción actual y su tiempo máximo (para el anillo de progreso).
    val segundosRestantes: Int = 0,
    val duracionAccionSegundos: Int = 1,
    // HU06-CA06: pausa breve entre repeticiones antes de que arranque la siguiente.
    val enDescanso: Boolean = false,
    val segundosDescanso: Int = 0,
    val sesionCompletada: Boolean = false,
    // La sesión se finalizó sola porque el paciente estuvo demasiado tiempo fuera de cuadro.
    val finalizadaPorAusencia: Boolean = false,
    val error: String? = null,
    // HU10 — indicador mínimo de corrección y evento de voz (ver EventoVoz).
    val enCorreccion: Boolean = false,
    val eventoVoz: EventoVoz? = null,
) {
    val totalRepeticiones: Int get() = repeticionesOverride ?: ejercicio?.repeticiones ?: 1
    val duracionRepeticionSegundos: Int get() = duracionAccionSegundos
}

// HU06 — ejecutar una sesión terapéutica: cargar el ejercicio asignado y
// recorrer, repetición por repetición y acción por acción, el movimiento del
// paciente con la máquina de estados de la guía visual (GuideStateMachine).
// Al terminar se guarda el resultado consolidado (HU17, via HU08/HU09).
@HiltViewModel
class EjecutarSesionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val sesionRepository: SesionRepository,
    private val ejercicioRepository: EjercicioRepository,
    private val usuarioRepository: UsuarioRepository,
    private val subidorVideoSesion: com.sanna.rehabapp.core.camera.SubidorVideoSesion,
) : ViewModel() {

    // No privado: la Screen lo necesita para navegar al resultado (HU11-CA01)
    // en cuanto la sesión se completa.
    val sesionId: String = checkNotNull(savedStateHandle[Rutas.ARG_SESION_ID])
    private val pacienteId: String? = authRepository.uidActual

    private val _uiState = MutableStateFlow(EjecutarSesionUiState())
    val uiState: StateFlow<EjecutarSesionUiState> = _uiState

    // Guía visual: último cuadro de pose (suavizado con One Euro) y estado de la
    // acción actual. Se actualizan en cada cuadro; solo los consumen el overlay y
    // el HUD, por eso van separados de uiState.
    private val filtroLandmarks = FiltroLandmarks()
    private val _cuadroPose = MutableStateFlow<LandmarksFrame?>(null)
    val cuadroPose: StateFlow<LandmarksFrame?> = _cuadroPose
    private val _guiaUi = MutableStateFlow(GuideUiState())
    val guiaUi: StateFlow<GuideUiState> = _guiaUi

    private val candado = Any()
    private var maquina: GuideStateMachine? = null
    private var accionesSesion: List<Accion> = emptyList()
    private var ultimoSigno: Int? = null
    private var jobCicloSesion: Job? = null

    // HU10: estado del debounce de voz.
    private var ultimoMensajeVoz: MensajeGuia? = null
    private var instanteUltimaVoz = 0L
    private var mensajeActual: MensajeGuia? = null
    private var mensajeDesdeMs = 0L
    private var insistenciaDicha = false

    // HU06-CA09 — si la sesión ya tenía un resultado parcial (se finalizó
    // antes de tiempo previamente), se reanuda desde la siguiente
    // repetición en vez de repetir lo ya medido.
    private var numeroRepeticionInicial = 1
    private var resultadoPrevio: ResultadoSesion? = null

    init {
        cargarEjercicio()
    }

    private fun cargarEjercicio() {
        val idPaciente = pacienteId
        if (idPaciente == null) {
            _uiState.update { it.copy(cargando = false, error = "Sesión inválida, vuelve a iniciar sesión.") }
            return
        }
        viewModelScope.launch {
            val sesion = sesionRepository.obtenerSesion(idPaciente, sesionId)
            // ERR-PAC-002: una sesión ya completa no se puede volver a ejecutar.
            if (sesion != null && sesion.estado == EstadoSesion.COMPLETADA) {
                val total = sesion.repeticiones ?: sesion.resultado?.repeticionesAsignadas ?: 0
                val hechas = sesion.resultado?.repeticionesCompletadas ?: 0
                if (hechas >= total) {
                    _uiState.update { it.copy(cargando = false, error = "Esta sesión ya fue completada.") }
                    return@launch
                }
            }
            val ejercicio = sesion?.let { ejercicioRepository.obtenerEjercicio(it.ejercicioId) }
            if (sesion == null || ejercicio == null) {
                _uiState.update { it.copy(cargando = false, error = "No se encontró el ejercicio asignado.") }
                return@launch
            }
            // HU20 (ampliación): las acciones se definen sobre un lado; se mide el
            // lado que el administrador indicó como afectado para este paciente.
            val ladoAfectado = usuarioRepository.obtenerUsuario(idPaciente)?.ladoAfectado ?: LadoAfectado.DERECHO
            val acciones = aplicarAnguloPersonalizado(
                ejercicio.accionesEfectivas().map { it.paraLado(ladoAfectado) },
                sesion.anguloMinOverride,
                sesion.anguloMaxOverride,
            )
            if (acciones.isEmpty()) {
                _uiState.update { it.copy(cargando = false, error = "Este ejercicio no tiene acciones ni rango de referencia.") }
                return@launch
            }
            val totalRepeticiones = sesion.repeticiones ?: ejercicio.repeticiones
            val resultadoAnterior = sesion.resultado
            if (resultadoAnterior != null && resultadoAnterior.repeticionesCompletadas < totalRepeticiones) {
                resultadoPrevio = resultadoAnterior
                numeroRepeticionInicial = resultadoAnterior.repeticionesCompletadas + 1
            }
            accionesSesion = acciones
            val nueva = GuideStateMachine(acciones, totalRepeticiones, numeroRepeticionInicial)
            nueva.pausar() // no mide hasta que termine la cuenta regresiva
            synchronized(candado) { maquina = nueva }
            _guiaUi.value = nueva.estado
            _uiState.update {
                it.copy(
                    ejercicio = ejercicio,
                    repeticionesOverride = sesion.repeticiones,
                    repeticionActual = numeroRepeticionInicial,
                    repeticionesCompletadas = resultadoAnterior?.repeticionesCompletadas ?: 0,
                    segundosRestantes = acciones.first().tiempoMaxSeg,
                    duracionAccionSegundos = acciones.first().tiempoMaxSeg,
                    cargando = false,
                )
            }
        }
    }

    // HU03-CA08 (ampliación): el ángulo personalizado por el fisioterapeuta reemplaza la
    // meta de la PRIMERA acción (punto medio del rango) y ajusta su tolerancia al ancho del rango.
    private fun aplicarAnguloPersonalizado(acciones: List<Accion>, minimo: Float?, maximo: Float?): List<Accion> {
        if (minimo == null || maximo == null || acciones.isEmpty()) return acciones
        val primera = acciones.first().copy(
            anguloMeta = (minimo + maximo) / 2f,
            tolerancia = ((maximo - minimo) / 2f).coerceAtLeast(1f),
        )
        return listOf(primera) + acciones.drop(1)
    }

    // HU06-CA02: cuenta regresiva de preparación y arranque de la guía.
    fun iniciarSesion() {
        if (_uiState.value.sesionIniciada) return
        _uiState.update { it.copy(sesionIniciada = true) }
        jobCicloSesion = viewModelScope.launch {
            _uiState.update { it.copy(enPreparacion = true, segundosPreparacion = SEGUNDOS_PREPARACION_INICIAL) }
            while (_uiState.value.segundosPreparacion > 0) {
                delay(1_000)
                _uiState.update { it.copy(segundosPreparacion = it.segundosPreparacion - 1) }
            }
            synchronized(candado) { maquina?.reanudar(ahoraMs()) }
            _uiState.update { it.copy(enPreparacion = false) }
        }
    }

    private fun ahoraMs(): Long = android.os.SystemClock.elapsedRealtime()

    // HU07/HU08 — cada resultado de MediaPipe en vivo se suaviza y se evalúa contra la
    // acción actual. Se llama desde el hilo de MediaPipe.
    fun procesarResultadoPose(resultado: PoseLandmarkerResult, anchoImagen: Int, altoImagen: Int) {
        val frame = resultado.aFrame(anchoImagen, altoImagen)?.let { filtroLandmarks.aplicar(it) }
        _cuadroPose.value = frame
        val estado = _uiState.value
        if (!estado.sesionIniciada || estado.sesionCompletada || estado.enDescanso || estado.enPreparacion) return

        val ahora = ahoraMs()
        val eventos: List<EventoGuia>
        val ui: GuideUiState
        synchronized(candado) {
            val m = maquina ?: return
            val accion = m.estado.accion ?: return
            val guia = frame?.let { PoseGuideCalculator.calcular(it, accion, ultimoSigno) }
            guia?.let { ultimoSigno = it.signoGiro }
            eventos = m.procesar(guia, ahora)
            ui = m.estado
        }
        _guiaUi.value = ui
        _uiState.update {
            it.copy(
                repeticionActual = ui.repeticion,
                segundosRestantes = ui.segundosRestantesAccion,
                duracionAccionSegundos = ui.accion?.tiempoMaxSeg ?: it.duracionAccionSegundos,
                enCorreccion = ui.mensaje == MensajeGuia.REGRESAR_UN_POCO || ui.mensaje == MensajeGuia.REUBICARSE,
            )
        }
        decirMensaje(ui, ahora)
        eventos.forEach(::manejarEvento)
    }

    private fun manejarEvento(evento: EventoGuia) {
        when (evento) {
            is EventoGuia.AccionTerminada -> ultimoSigno = null
            is EventoGuia.RepeticionCompletada -> {
                val total = _uiState.value.totalRepeticiones
                _uiState.update { it.copy(repeticionesCompletadas = evento.numero) }
                if (evento.numero < total) iniciarDescanso(evento.numero)
            }
            EventoGuia.SesionTerminada -> finalizarSesion()
            EventoGuia.AusenciaProlongada -> {
                _uiState.update { it.copy(finalizadaPorAusencia = true) }
                finalizarSesion()
            }
        }
    }

    // HU06-CA06: pausa breve entre repeticiones, con aviso por voz de la siguiente.
    private fun iniciarDescanso(repeticionTerminada: Int) {
        synchronized(candado) { maquina?.pausar() }
        jobCicloSesion = viewModelScope.launch {
            _uiState.update { it.copy(enDescanso = true, segundosDescanso = SEGUNDOS_DESCANSO_ENTRE_REPETICIONES) }
            while (_uiState.value.segundosDescanso > 0) {
                delay(1_000)
                val restante = _uiState.value.segundosDescanso - 1
                _uiState.update { it.copy(segundosDescanso = restante) }
                if (restante == SEGUNDOS_AVISO_SIGUIENTE_REPETICION) {
                    _uiState.update {
                        it.copy(eventoVoz = EventoVoz("Prepárate, sigue la repetición ${repeticionTerminada + 1}.", System.currentTimeMillis()))
                    }
                }
            }
            synchronized(candado) { maquina?.reanudar(ahoraMs()) }
            ultimoSigno = null
            _uiState.update { it.copy(enDescanso = false) }
        }
    }

    // HU10-CA06: voz según el mensaje de la guía, sin repetir el mismo antes de INTERVALO_REPETIR_VOZ_MS.
    private fun decirMensaje(ui: GuideUiState, ahora: Long) {
        val mensaje = ui.mensaje ?: return
        if (mensaje != mensajeActual) {
            mensajeActual = mensaje
            mensajeDesdeMs = ahora
            insistenciaDicha = false
        }
        val corresponde = when (mensaje) {
            // Mientras avanza normalmente no se habla; solo se insiste si se queda sin llegar.
            MensajeGuia.AVANZAR_MAS -> !insistenciaDicha && ahora - mensajeDesdeMs >= ESPERA_ANTES_DE_INSISTIR_MS
            else -> mensaje != ultimoMensajeVoz
        }
        if (!corresponde || ahora - instanteUltimaVoz < INTERVALO_REPETIR_VOZ_MS) return
        if (mensaje == MensajeGuia.AVANZAR_MAS) insistenciaDicha = true
        ultimoMensajeVoz = mensaje
        instanteUltimaVoz = ahora
        val vista = _uiState.value.ejercicio?.vistaCamara ?: VistaCamara.FRONTAL
        _uiState.update { it.copy(eventoVoz = EventoVoz(fraseDeGuia(mensaje, ui.accion, vista), System.currentTimeMillis())) }
    }

    // Parte 3 (video): la cámara avisa cuando el archivo ya está completo.
    // Solo se sube si la sesión terminó con resultado guardado; si el
    // paciente abandonó con "Salir" (sin registrar nada) el video se descarta.
    fun onVideoGrabado(archivo: java.io.File) {
        val idPaciente = pacienteId
        if (idPaciente != null && _uiState.value.sesionCompletada) {
            subidorVideoSesion.subir(idPaciente, sesionId, archivo)
        } else {
            archivo.delete()
        }
    }

    fun onErrorCamara(mensaje: String) {
        _uiState.update { it.copy(error = mensaje) }
    }

    // El paciente puede terminar antes de completar todas las repeticiones;
    // se registra igual con lo medido hasta ese momento (mejor eso que
    // perder por completo lo ya ejecutado).
    fun finalizarAntesDeTiempo() {
        if (!_uiState.value.sesionIniciada || _uiState.value.sesionCompletada) return
        jobCicloSesion?.cancel()
        finalizarSesion()
    }

    // HU06-CA04/CA05: concluye la sesión y registra el resultado.
    // HU06-CA09: si venía de una reanudación, combina lo nuevo con lo que
    // ya estaba guardado en vez de sobrescribirlo.
    private fun finalizarSesion() {
        val idPaciente = pacienteId ?: return
        if (_uiState.value.sesionCompletada) return
        val cerradas = synchronized(candado) {
            maquina?.pausar()
            maquina?.repeticionesCerradas
        } ?: return
        val estado = _uiState.value
        val resultadoNuevo = construirResultadoPorAcciones(
            acciones = accionesSesion,
            repeticiones = cerradas,
            repeticionesCompletadas = numeroRepeticionInicial - 1 + cerradas.size,
            repeticionesAsignadas = estado.totalRepeticiones,
            numeroRepeticionInicial = numeroRepeticionInicial,
        )
        val resultadoFinal = resultadoPrevio?.let { mergearResultados(it, resultadoNuevo) } ?: resultadoNuevo
        viewModelScope.launch {
            sesionRepository.guardarResultado(idPaciente, sesionId, resultadoFinal)
            _uiState.update { it.copy(sesionCompletada = true) }
        }
    }
}
