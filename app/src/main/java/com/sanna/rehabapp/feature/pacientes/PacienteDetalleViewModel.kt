package com.sanna.rehabapp.feature.pacientes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.core.navigation.Rutas
import com.sanna.rehabapp.domain.model.EstadoSesion
import com.sanna.rehabapp.domain.model.Sesion
import com.sanna.rehabapp.domain.model.Usuario
import com.sanna.rehabapp.domain.repository.SesionRepository
import com.sanna.rehabapp.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PacienteDetalleUiState(
    val paciente: Usuario? = null,
    val sesiones: List<Sesion> = emptyList(),
    val cargando: Boolean = true,
) {
    val sesionesCompletadas: Int
        get() = sesiones.count { it.estado == EstadoSesion.COMPLETADA }
}

// HU01-CA02/CA03 — detalle de un paciente: su información y sus sesiones.
@HiltViewModel
class PacienteDetalleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val usuarioRepository: UsuarioRepository,
    private val sesionRepository: SesionRepository,
) : ViewModel() {

    private val pacienteId: String = checkNotNull(savedStateHandle[Rutas.ARG_PACIENTE_ID])

    private val _uiState = MutableStateFlow(PacienteDetalleUiState())
    val uiState: StateFlow<PacienteDetalleUiState> = _uiState

    init {
        cargarPaciente()
        observarSesiones()
    }

    private fun cargarPaciente() {
        viewModelScope.launch {
            val paciente = usuarioRepository.obtenerUsuario(pacienteId)
            _uiState.update { it.copy(paciente = paciente, cargando = false) }
        }
    }

    private fun observarSesiones() {
        viewModelScope.launch {
            sesionRepository.observarSesionesDe(pacienteId).collect { lista ->
                _uiState.update { it.copy(sesiones = lista) }
            }
        }
    }
}
