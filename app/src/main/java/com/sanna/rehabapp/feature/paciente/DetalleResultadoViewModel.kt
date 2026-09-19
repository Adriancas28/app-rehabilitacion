package com.sanna.rehabapp.feature.paciente

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.core.navigation.Rutas
import com.sanna.rehabapp.domain.model.Recomendacion
import com.sanna.rehabapp.domain.model.ResultadoSesion
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import com.sanna.rehabapp.domain.repository.RecomendacionRepository
import com.sanna.rehabapp.domain.repository.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetalleResultadoUiState(
    val nombreEjercicio: String = "",
    val fecha: Date? = null,
    val resultado: ResultadoSesion? = null,
    // Recomendaciones que el fisioterapeuta YA guardó para esta sesión.
    val recomendaciones: List<Recomendacion> = emptyList(),
    val cargando: Boolean = true,
)

// Detalle de UNA sesión realizada (se abre desde "Mis resultados"):
// repeticiones, promedio, detalle por repetición y recomendaciones del
// fisioterapeuta, leídas de la misma colección donde él las guarda.
@HiltViewModel
class DetalleResultadoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val sesionRepository: SesionRepository,
    private val ejercicioRepository: EjercicioRepository,
    private val recomendacionRepository: RecomendacionRepository,
) : ViewModel() {

    private val sesionId: String = checkNotNull(savedStateHandle[Rutas.ARG_SESION_ID])

    private val _uiState = MutableStateFlow(DetalleResultadoUiState())
    val uiState: StateFlow<DetalleResultadoUiState> = _uiState

    init {
        val pacienteId = authRepository.uidActual
        if (pacienteId == null) {
            _uiState.update { it.copy(cargando = false) }
        } else {
            viewModelScope.launch {
                val sesion = sesionRepository.obtenerSesion(pacienteId, sesionId)
                val ejercicio = sesion?.let { ejercicioRepository.obtenerEjercicio(it.ejercicioId) }
                _uiState.update {
                    it.copy(
                        nombreEjercicio = ejercicio?.nombre ?: "Resultado",
                        fecha = sesion?.fechaEjecucion ?: sesion?.fechaAsignacion,
                        resultado = sesion?.resultado,
                        cargando = false,
                    )
                }
            }
            viewModelScope.launch {
                recomendacionRepository.observarDe(pacienteId, sesionId)
                    .catch { emit(emptyList()) }
                    .collect { lista -> _uiState.update { it.copy(recomendaciones = lista) } }
            }
        }
    }
}
