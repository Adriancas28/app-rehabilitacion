package com.sanna.rehabapp.feature.pacientes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.core.navigation.Rutas
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.model.ResultadoSesion
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import com.sanna.rehabapp.domain.repository.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FisioResultadoSesionUiState(
    val ejercicio: Ejercicio? = null,
    val resultado: ResultadoSesion? = null,
    val cargando: Boolean = true,
)

// HU18-CA02/CA04 — el fisioterapeuta ve el resultado de una sesión ya
// completada, incluido el desglose por repetición, antes de decidir qué
// recomendación registrar (HU15).
@HiltViewModel
class FisioResultadoSesionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sesionRepository: SesionRepository,
    private val ejercicioRepository: EjercicioRepository,
) : ViewModel() {

    val pacienteId: String = checkNotNull(savedStateHandle[Rutas.ARG_PACIENTE_ID])
    val sesionId: String = checkNotNull(savedStateHandle[Rutas.ARG_SESION_ID])

    private val _uiState = MutableStateFlow(FisioResultadoSesionUiState())
    val uiState: StateFlow<FisioResultadoSesionUiState> = _uiState

    init {
        cargar()
    }

    private fun cargar() {
        viewModelScope.launch {
            val sesion = sesionRepository.obtenerSesion(pacienteId, sesionId)
            val ejercicio = sesion?.let { ejercicioRepository.obtenerEjercicio(it.ejercicioId) }
            _uiState.update { it.copy(ejercicio = ejercicio, resultado = sesion?.resultado, cargando = false) }
        }
    }
}
