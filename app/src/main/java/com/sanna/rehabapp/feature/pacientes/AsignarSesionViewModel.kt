package com.sanna.rehabapp.feature.pacientes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.core.navigation.Rutas
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import com.sanna.rehabapp.domain.repository.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AsignarSesionUiState(
    val ejercicios: List<Ejercicio> = emptyList(),
    val ejercicioSeleccionadoId: String? = null,
    val fechaAsignacion: Date? = null,
    val cargando: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null,
    val guardadoExitoso: Boolean = false,
)

// HU03 — asignar (CA01, CA02) o editar (CA03) una sesión terapéutica.
@HiltViewModel
class AsignarSesionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val ejercicioRepository: EjercicioRepository,
    private val sesionRepository: SesionRepository,
) : ViewModel() {

    private val pacienteId: String = checkNotNull(savedStateHandle[Rutas.ARG_PACIENTE_ID])
    private val sesionIdArg: String? = savedStateHandle[Rutas.ARG_SESION_ID]
    val esEdicion: Boolean get() = !sesionIdArg.isNullOrBlank()

    private val _uiState = MutableStateFlow(AsignarSesionUiState())
    val uiState: StateFlow<AsignarSesionUiState> = _uiState

    init {
        observarEjercicios()
        if (esEdicion) cargarSesion(sesionIdArg!!)
    }

    private fun observarEjercicios() {
        viewModelScope.launch {
            ejercicioRepository.observarEjercicios().collect { lista ->
                _uiState.update { it.copy(ejercicios = lista.filter { ejercicio -> ejercicio.activo }) }
            }
        }
    }

    private fun cargarSesion(sesionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            val sesion = sesionRepository.obtenerSesion(pacienteId, sesionId)
            _uiState.update {
                if (sesion != null) {
                    it.copy(
                        ejercicioSeleccionadoId = sesion.ejercicioId,
                        fechaAsignacion = sesion.fechaAsignacion,
                        cargando = false,
                    )
                } else {
                    it.copy(cargando = false, error = "No se encontró la sesión.")
                }
            }
        }
    }

    fun onEjercicioSeleccionado(ejercicioId: String) =
        _uiState.update { it.copy(ejercicioSeleccionadoId = ejercicioId, error = null) }

    fun onFechaSeleccionada(fecha: Date) =
        _uiState.update { it.copy(fechaAsignacion = fecha, error = null) }

    fun guardar() {
        val estado = _uiState.value
        val ejercicioId = estado.ejercicioSeleccionadoId
        val fecha = estado.fechaAsignacion
        if (ejercicioId == null || fecha == null) {
            _uiState.update { it.copy(error = "Selecciona un ejercicio y una fecha.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, error = null) }
            val resultado = if (esEdicion) {
                sesionRepository.actualizarSesion(pacienteId, sesionIdArg!!, ejercicioId, fecha)
            } else {
                val fisioterapeutaId = authRepository.uidActual
                if (fisioterapeutaId == null) {
                    _uiState.update { it.copy(guardando = false, error = "Sesión inválida, vuelve a iniciar sesión.") }
                    return@launch
                }
                sesionRepository.asignarSesion(pacienteId, ejercicioId, fisioterapeutaId, fecha)
            }
            resultado.fold(
                onSuccess = { _uiState.update { it.copy(guardando = false, guardadoExitoso = true) } },
                onFailure = {
                    _uiState.update { it.copy(guardando = false, error = "No se pudo guardar la sesión.") }
                },
            )
        }
    }
}
