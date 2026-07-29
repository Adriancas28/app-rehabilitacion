package com.sanna.rehabapp.feature.paciente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.model.EstadoSesion
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import com.sanna.rehabapp.domain.repository.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// HU04-CA02 — el ejercicio ya resuelto (no solo su id) junto a la sesión
// que lo asignó, para poder abrir su detalle (HU04-CA03).
data class EjercicioAsignado(val sesionId: String, val ejercicio: Ejercicio)

data class EjerciciosAsignadosUiState(
    val ejerciciosAsignados: List<EjercicioAsignado> = emptyList(),
    val cargando: Boolean = true,
)

// HU04 — el paciente visualiza los ejercicios de sus sesiones pendientes.
@HiltViewModel
class EjerciciosAsignadosViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sesionRepository: SesionRepository,
    private val ejercicioRepository: EjercicioRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EjerciciosAsignadosUiState())
    val uiState: StateFlow<EjerciciosAsignadosUiState> = _uiState

    init {
        observarEjerciciosAsignados()
    }

    private fun observarEjerciciosAsignados() {
        val pacienteId = authRepository.uidActual
        if (pacienteId == null) {
            _uiState.update { it.copy(cargando = false) }
            return
        }
        viewModelScope.launch {
            combine(
                sesionRepository.observarSesionesDe(pacienteId),
                ejercicioRepository.observarEjercicios(),
            ) { sesiones, ejercicios ->
                val ejerciciosPorId = ejercicios.associateBy { it.id }
                sesiones
                    .filter { it.estado == EstadoSesion.PENDIENTE }
                    .mapNotNull { sesion ->
                        ejerciciosPorId[sesion.ejercicioId]?.let { ejercicio ->
                            EjercicioAsignado(sesionId = sesion.id, ejercicio = ejercicio)
                        }
                    }
            }.collect { lista ->
                _uiState.update { it.copy(ejerciciosAsignados = lista, cargando = false) }
            }
        }
    }
}
