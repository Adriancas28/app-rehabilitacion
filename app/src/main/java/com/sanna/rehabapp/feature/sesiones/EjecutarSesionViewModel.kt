package com.sanna.rehabapp.feature.sesiones

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.sanna.rehabapp.core.navigation.Rutas
import com.sanna.rehabapp.core.posedetection.ProcesadorMovimiento
import com.sanna.rehabapp.domain.model.Ejercicio
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
                _uiState.update {
                    it.copy(
                        ejercicio = ejercicio,
                        repeticionesOverride = sesion?.repeticiones,
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
            for (repeticion in 1..totalRepeticiones) {
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
    fun procesarResultadoPose(resultado: PoseLandmarkerResult) {
        val estado = _uiState.value
        // En descanso entre repeticiones no se mide: el paciente no está
        // ejecutando el ejercicio en ese momento.
        if (!estado.sesionIniciada || estado.sesionCompletada || estado.enDescanso) return
        procesadorMovimiento?.procesarResultado(resultado)
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
    private fun finalizarSesion() {
        val idPaciente = pacienteId ?: return
        val estado = _uiState.value
        val resultado = procesadorMovimiento?.generarResultado(
            repeticionesCompletadas = estado.repeticionesCompletadas,
            repeticionesAsignadas = estado.totalRepeticiones,
        ) ?: return
        viewModelScope.launch {
            sesionRepository.guardarResultado(idPaciente, sesionId, resultado)
            _uiState.update { it.copy(sesionCompletada = true) }
        }
    }
}
