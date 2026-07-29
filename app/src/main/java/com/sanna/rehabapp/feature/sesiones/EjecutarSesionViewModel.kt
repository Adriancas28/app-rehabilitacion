package com.sanna.rehabapp.feature.sesiones

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.sanna.rehabapp.core.navigation.Rutas
import com.sanna.rehabapp.core.posedetection.MedicionArticulacion
import com.sanna.rehabapp.core.posedetection.ProcesadorMovimiento
import com.sanna.rehabapp.core.posedetection.fraseCorrectiva
import com.sanna.rehabapp.core.posedetection.mergearResultados
import com.sanna.rehabapp.domain.model.Articulacion
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.model.ResultadoSesion
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import com.sanna.rehabapp.domain.repository.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SEGUNDOS_DESCANSO_ENTRE_REPETICIONES = 5

// HU10-CA06: no repetir la misma corrección por voz más seguido que esto
// (evita hablar en cada frame mientras el error persiste).
private const val INTERVALO_REPETIR_VOZ_MS = 4_000L

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
    val repeticionActual: Int = 1,
    // HU06-CA07/HU11: repeticiones que llegaron a completar su tiempo
    // completo (puede ser menos que totalRepeticiones si se finalizó antes).
    val repeticionesCompletadas: Int = 0,
    val segundosRestantes: Int = 0,
    // HU06-CA06: pausa breve entre repeticiones antes de que arranque la siguiente.
    val enDescanso: Boolean = false,
    val segundosDescanso: Int = 0,
    val sesionCompletada: Boolean = false,
    val error: String? = null,
    // HU10 — retroalimentación en vivo: ícono mínimo (sin texto en
    // pantalla) + un evento de voz cada vez que hay que decir una
    // corrección nueva (ver EventoVoz).
    val enCorreccion: Boolean = false,
    val eventoVoz: EventoVoz? = null,
) {
    val totalRepeticiones: Int get() = repeticionesOverride ?: ejercicio?.repeticiones ?: 1
}

// HU06 — ejecutar una sesión terapéutica: cargar el ejercicio asignado
// (CA01), iniciar el monitoreo por un tiempo determinado (CA02/CA03/CA04),
// y al finalizar guardar el resultado consolidado (CA05, via HU08).
@HiltViewModel
class EjecutarSesionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val sesionRepository: SesionRepository,
    private val ejercicioRepository: EjercicioRepository,
) : ViewModel() {

    private val sesionId: String = checkNotNull(savedStateHandle[Rutas.ARG_SESION_ID])
    private val pacienteId: String? = authRepository.uidActual

    private val _uiState = MutableStateFlow(EjecutarSesionUiState())
    val uiState: StateFlow<EjecutarSesionUiState> = _uiState

    private var procesadorMovimiento: ProcesadorMovimiento? = null
    private var jobCicloRepeticiones: Job? = null

    // HU10-CA06: estado del debounce de voz (no vive en el UiState porque
    // no es algo que la UI necesite leer, solo el propio ViewModel).
    private var ultimaCorreccionHablada: Pair<Articulacion, String>? = null
    private var instanteUltimaVoz = 0L

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
            val ejercicio = sesion?.let { ejercicioRepository.obtenerEjercicio(it.ejercicioId) }
            if (ejercicio != null) {
                procesadorMovimiento = ProcesadorMovimiento(ejercicio)
                val totalRepeticiones = sesion?.repeticiones ?: ejercicio.repeticiones
                val resultadoAnterior = sesion?.resultado
                if (resultadoAnterior != null && resultadoAnterior.repeticionesCompletadas < totalRepeticiones) {
                    resultadoPrevio = resultadoAnterior
                    numeroRepeticionInicial = resultadoAnterior.repeticionesCompletadas + 1
                }
                _uiState.update {
                    it.copy(
                        ejercicio = ejercicio,
                        repeticionesOverride = sesion?.repeticiones,
                        repeticionActual = numeroRepeticionInicial,
                        repeticionesCompletadas = resultadoAnterior?.repeticionesCompletadas ?: 0,
                        segundosRestantes = ejercicio.duracionSegundos,
                        cargando = false,
                    )
                }
            } else {
                _uiState.update { it.copy(cargando = false, error = "No se encontró el ejercicio asignado.") }
            }
        }
    }

    // HU06-CA02: habilita la ejecución y arranca el cronómetro.
    // HU06-CA06: repite el ciclo de monitoreo tantas veces como
    // repeticiones tenga el ejercicio, con una pausa entre cada una; todas
    // se acumulan en el mismo ProcesadorMovimiento (un único resultado).
    fun iniciarSesion() {
        if (_uiState.value.sesionIniciada) return
        val totalRepeticiones = _uiState.value.totalRepeticiones
        _uiState.update { it.copy(sesionIniciada = true) }
        jobCicloRepeticiones = viewModelScope.launch {
            for (repeticion in numeroRepeticionInicial..totalRepeticiones) {
                _uiState.update { it.copy(repeticionActual = repeticion, segundosRestantes = it.ejercicio?.duracionSegundos ?: 0) }
                procesadorMovimiento?.marcarNuevaRepeticion()
                while (_uiState.value.segundosRestantes > 0) {
                    delay(1_000)
                    _uiState.update { it.copy(segundosRestantes = it.segundosRestantes - 1) }
                }
                _uiState.update { it.copy(repeticionesCompletadas = repeticion) }
                if (repeticion < totalRepeticiones) {
                    _uiState.update { it.copy(enDescanso = true, segundosDescanso = SEGUNDOS_DESCANSO_ENTRE_REPETICIONES) }
                    while (_uiState.value.segundosDescanso > 0) {
                        delay(1_000)
                        _uiState.update { it.copy(segundosDescanso = it.segundosDescanso - 1) }
                    }
                    _uiState.update { it.copy(enDescanso = false) }
                }
            }
            finalizarSesion()
        }
    }

    // HU07/HU08 — cada resultado de MediaPipe en vivo se mide contra el
    // patronesReferencia del ejercicio mientras la sesión está activa.
    // HU10 — con esa misma medición se decide la retroalimentación en vivo.
    fun procesarResultadoPose(resultado: PoseLandmarkerResult) {
        val estado = _uiState.value
        // En descanso entre repeticiones no se mide: el paciente no está
        // ejecutando el ejercicio en ese momento.
        if (!estado.sesionIniciada || estado.sesionCompletada || estado.enDescanso) return
        val mediciones = procesadorMovimiento?.procesarResultado(resultado) ?: return
        actualizarRetroalimentacionEnVivo(mediciones)
    }

    // HU10-CA01/CA02/CA06: si alguna articulación medida en este frame está
    // fuera de rango, se elige la de mayor desviación para corregir. Si ya
    // se estaba avisando de la misma corrección, solo se repite la voz
    // pasado INTERVALO_REPETIR_VOZ_MS (evita hablar en cada frame).
    private fun actualizarRetroalimentacionEnVivo(mediciones: List<MedicionArticulacion>) {
        val peorMedicion = mediciones.filterNot { it.dentroDeRango }.maxByOrNull { it.desviacion }
        if (peorMedicion == null) {
            _uiState.update { it.copy(enCorreccion = false) }
            ultimaCorreccionHablada = null
            return
        }
        _uiState.update { it.copy(enCorreccion = true) }

        val claveCorreccion = peorMedicion.articulacion to peorMedicion.tipoDeError
        val ahora = System.currentTimeMillis()
        val cambioDeCorreccion = claveCorreccion != ultimaCorreccionHablada
        val pasoElIntervalo = ahora - instanteUltimaVoz >= INTERVALO_REPETIR_VOZ_MS
        if (cambioDeCorreccion || pasoElIntervalo) {
            ultimaCorreccionHablada = claveCorreccion
            instanteUltimaVoz = ahora
            _uiState.update { it.copy(eventoVoz = EventoVoz(fraseCorrectiva(peorMedicion), ahora)) }
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
        jobCicloRepeticiones?.cancel()
        finalizarSesion()
    }

    // HU06-CA04/CA05: finaliza el tiempo establecido y registra el resultado.
    // HU06-CA09: si venía de una reanudación, combina lo nuevo con lo que
    // ya estaba guardado en vez de sobrescribirlo.
    private fun finalizarSesion() {
        val idPaciente = pacienteId ?: return
        val estado = _uiState.value
        val resultadoNuevo = procesadorMovimiento?.generarResultado(
            repeticionesCompletadas = estado.repeticionesCompletadas,
            repeticionesAsignadas = estado.totalRepeticiones,
            numeroRepeticionInicial = numeroRepeticionInicial,
        ) ?: return
        val resultadoFinal = resultadoPrevio?.let { mergearResultados(it, resultadoNuevo) } ?: resultadoNuevo
        viewModelScope.launch {
            sesionRepository.guardarResultado(idPaciente, sesionId, resultadoFinal)
            _uiState.update { it.copy(sesionCompletada = true) }
        }
    }
}
