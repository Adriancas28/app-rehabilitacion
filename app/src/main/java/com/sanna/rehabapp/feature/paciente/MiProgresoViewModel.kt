package com.sanna.rehabapp.feature.paciente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.domain.model.EstadoSesion
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MiProgresoUiState(
    val sesionesRealizadas: Int = 0,
    val promedioGeneral: Float = 0f,
    // % de precisión de cada sesión realizada, la más antigua primero
    // (Sesión 1, Sesión 2, ...).
    val precisionPorSesion: List<Float> = emptyList(),
    val cargando: Boolean = true,
)

// HU12 — evolución general del paciente: total de sesiones realizadas,
// promedio de precisión de todas y el gráfico de puntos sesión a sesión.
// Se actualiza solo con cada sesión nueva (Flow en vivo, HU12-CA04).
@HiltViewModel
class MiProgresoViewModel @Inject constructor(
    authRepository: AuthRepository,
    sesionRepository: SesionRepository,
) : ViewModel() {

    val uiState: StateFlow<MiProgresoUiState> = run {
        val pacienteId = authRepository.uidActual
            ?: return@run MutableStateFlow(MiProgresoUiState(cargando = false))
        sesionRepository.observarSesionesDe(pacienteId)
            .map { sesiones ->
                val realizadas = sesiones
                    .filter { it.estado == EstadoSesion.COMPLETADA && it.resultado != null }
                    .sortedBy { it.fechaEjecucion ?: it.fechaAsignacion }
                val precisiones = realizadas.mapNotNull { it.resultado?.porcentajeEjecucion }
                MiProgresoUiState(
                    sesionesRealizadas = realizadas.size,
                    promedioGeneral = if (precisiones.isEmpty()) 0f else precisiones.average().toFloat(),
                    precisionPorSesion = precisiones,
                    cargando = false,
                )
            }
            .catch { emit(MiProgresoUiState(cargando = false)) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MiProgresoUiState(),
            )
    }
}
