package com.sanna.rehabapp.feature.pacientes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.core.navigation.Rutas
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.model.EstadoSesion
import com.sanna.rehabapp.domain.model.Sesion
import com.sanna.rehabapp.domain.model.Usuario
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import com.sanna.rehabapp.domain.repository.SesionRepository
import com.sanna.rehabapp.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PacienteDetalleUiState(
    val paciente: Usuario? = null,
    val sesiones: List<Sesion> = emptyList(),
    val ejerciciosPorId: Map<String, Ejercicio> = emptyMap(),
    val cargando: Boolean = true,
) {
    val sesionesCompletadas: Int
        get() = sesiones.count { it.estado == EstadoSesion.COMPLETADA }
}

// HU01-CA02/CA03 — detalle de un paciente: su información y sus sesiones.
// HU03 — desde aquí también se asigna/edita una sesión (ver pacienteId).
@HiltViewModel
class PacienteDetalleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val usuarioRepository: UsuarioRepository,
    private val sesionRepository: SesionRepository,
    private val ejercicioRepository: EjercicioRepository,
) : ViewModel() {

    val pacienteId: String = checkNotNull(savedStateHandle[Rutas.ARG_PACIENTE_ID])

    private val _uiState = MutableStateFlow(PacienteDetalleUiState())
    val uiState: StateFlow<PacienteDetalleUiState> = _uiState

    init {
        cargarPaciente()
        observarSesiones()
        observarEjercicios()
    }

    private fun cargarPaciente() {
        viewModelScope.launch {
            val paciente = usuarioRepository.obtenerUsuario(pacienteId)
            _uiState.update { it.copy(paciente = paciente, cargando = false) }
        }
    }

    // HU01-CA06 — el fisioterapeuta registra/edita el diagnóstico del paciente.
    fun actualizarDiagnostico(diagnostico: String) {
        viewModelScope.launch {
            usuarioRepository.actualizarDiagnostico(pacienteId, diagnostico).onSuccess {
                _uiState.update { estado -> estado.copy(paciente = estado.paciente?.copy(diagnostico = diagnostico)) }
            }
        }
    }

    private fun observarSesiones() {
        val fisioterapeutaId = authRepository.uidActual ?: return
        viewModelScope.launch {
            sesionRepository.observarSesionesDe(pacienteId, fisioterapeutaId)
                // Evita que un PERMISSION_DENIED por cierre de sesión mientras
                // esta pantalla sigue activa tumbe la app (excepción no atrapada).
                .catch { }
                .collect { lista ->
                    _uiState.update { it.copy(sesiones = lista) }
                }
        }
    }

    private fun observarEjercicios() {
        viewModelScope.launch {
            ejercicioRepository.observarEjercicios()
                .catch { }
                .collect { lista ->
                    _uiState.update { it.copy(ejerciciosPorId = lista.associateBy { ejercicio -> ejercicio.id }) }
                }
        }
    }
}
