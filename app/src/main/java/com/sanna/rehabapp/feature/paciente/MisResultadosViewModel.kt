package com.sanna.rehabapp.feature.paciente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.domain.model.EstadoSesion
import com.sanna.rehabapp.domain.model.ResultadoSesion
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import com.sanna.rehabapp.domain.repository.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SesionRealizada(
    val sesionId: String,
    val nombreEjercicio: String,
    val fecha: Date?,
    val resultado: ResultadoSesion,
)

data class MisResultadosUiState(
    // Más reciente primero.
    val sesiones: List<SesionRealizada> = emptyList(),
    val cargando: Boolean = true,
)

// "Mis resultados": lista de todas las sesiones realizadas por el paciente,
// la más reciente primero. El detalle de cada una vive en su propia pantalla
// (DetalleResultadoScreen).
@HiltViewModel
class MisResultadosViewModel @Inject constructor(
    authRepository: AuthRepository,
    sesionRepository: SesionRepository,
    ejercicioRepository: EjercicioRepository,
) : ViewModel() {

    val uiState: StateFlow<MisResultadosUiState> = run {
        val id = authRepository.uidActual ?: return@run MutableStateFlow(MisResultadosUiState(cargando = false))
        combine(
            sesionRepository.observarSesionesDe(id),
            ejercicioRepository.observarEjercicios(),
        ) { sesiones, ejercicios ->
            val nombres = ejercicios.associate { it.id to it.nombre }
            val realizadas = sesiones
                .filter { it.estado == EstadoSesion.COMPLETADA }
                .mapNotNull { sesion ->
                    sesion.resultado?.let {
                        SesionRealizada(
                            sesionId = sesion.id,
                            nombreEjercicio = nombres[sesion.ejercicioId] ?: "Ejercicio",
                            fecha = sesion.fechaEjecucion ?: sesion.fechaAsignacion,
                            resultado = it,
                        )
                    }
                }
                .sortedByDescending { it.fecha }
            MisResultadosUiState(sesiones = realizadas, cargando = false)
        }
            .catch { emit(MisResultadosUiState(cargando = false)) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MisResultadosUiState(),
            )
    }
}
