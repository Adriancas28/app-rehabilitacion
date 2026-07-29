package com.sanna.rehabapp.feature.paciente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.model.EstadoSesion
import com.sanna.rehabapp.domain.model.Sesion
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import com.sanna.rehabapp.domain.repository.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// HU11/HU13: la sesión completada junto a su ejercicio ya resuelto, para
// mostrar el nombre real en la lista y poder abrir su detalle de resultado.
data class SesionCompletada(val sesionId: String, val sesion: Sesion, val ejercicio: Ejercicio)

data class HistorialSesionesUiState(
    val sesiones: List<SesionCompletada> = emptyList(),
    val cargando: Boolean = true,
)

// HU13 — el paciente consulta su historial de sesiones ya realizadas,
// de la más reciente a la más antigua (CA03), con mensaje de ausencia si
// todavía no completó ninguna (CA04).
@HiltViewModel
class HistorialSesionesViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sesionRepository: SesionRepository,
    private val ejercicioRepository: EjercicioRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialSesionesUiState())
    val uiState: StateFlow<HistorialSesionesUiState> = _uiState

    init {
        observarHistorial()
    }

    private fun observarHistorial() {
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
                // observarSesionesDe(pacienteId) ya viene ordenado por
                // fechaAsignacion descendente (más reciente primero).
                sesiones
                    .filter { it.estado == EstadoSesion.COMPLETADA }
                    .mapNotNull { sesion ->
                        ejerciciosPorId[sesion.ejercicioId]?.let { ejercicio ->
                            SesionCompletada(sesionId = sesion.id, sesion = sesion, ejercicio = ejercicio)
                        }
                    }
            }
                .catch { }
                .collect { lista -> _uiState.update { it.copy(sesiones = lista, cargando = false) } }
        }
    }
}
