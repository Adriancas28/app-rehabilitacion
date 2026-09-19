package com.sanna.rehabapp.feature.paciente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.domain.model.EstadoSesion
import com.sanna.rehabapp.domain.model.Recomendacion
import com.sanna.rehabapp.domain.model.ResultadoSesion
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import com.sanna.rehabapp.domain.repository.RecomendacionRepository
import com.sanna.rehabapp.domain.repository.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
    val seleccionadaId: String? = null,
    // Recomendaciones que el fisioterapeuta YA guardó para la sesión seleccionada.
    val recomendaciones: List<Recomendacion> = emptyList(),
    val cargando: Boolean = true,
) {
    val seleccionada: SesionRealizada? get() = sesiones.find { it.sesionId == seleccionadaId }
}

// "Mis resultados": historial de sesiones realizadas (la más reciente
// seleccionada por defecto) y, debajo, el detalle de la seleccionada con la
// recomendación de su fisioterapeuta. Lee las recomendaciones de la MISMA
// subcolección donde el fisioterapeuta las guarda (RecomendacionRepository),
// así que solo aparece lo que él ya guardó.
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MisResultadosViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    sesionRepository: SesionRepository,
    ejercicioRepository: EjercicioRepository,
    private val recomendacionRepository: RecomendacionRepository,
) : ViewModel() {

    private val pacienteId = authRepository.uidActual
    private val seleccionManual = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MisResultadosUiState> = run {
        val id = pacienteId ?: return@run MutableStateFlow(MisResultadosUiState(cargando = false))
        combine(
            sesionRepository.observarSesionesDe(id),
            ejercicioRepository.observarEjercicios(),
            seleccionManual,
        ) { sesiones, ejercicios, manual ->
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
            val seleccionada = realizadas.find { it.sesionId == manual }?.sesionId ?: realizadas.firstOrNull()?.sesionId
            MisResultadosUiState(sesiones = realizadas, seleccionadaId = seleccionada, cargando = false)
        }
            .flatMapLatest { base ->
                val seleccionada = base.seleccionadaId
                if (seleccionada == null) {
                    kotlinx.coroutines.flow.flowOf(base)
                } else {
                    recomendacionRepository.observarDe(id, seleccionada)
                        .catch { emit(emptyList()) }
                        .map { recomendaciones -> base.copy(recomendaciones = recomendaciones) }
                }
            }
            .catch { emit(MisResultadosUiState(cargando = false)) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MisResultadosUiState(),
            )
    }

    fun onSesionSeleccionada(sesionId: String) {
        seleccionManual.value = sesionId
    }
}
