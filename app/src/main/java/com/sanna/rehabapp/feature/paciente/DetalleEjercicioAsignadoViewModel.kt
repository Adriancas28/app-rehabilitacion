package com.sanna.rehabapp.feature.paciente

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.core.navigation.Rutas
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.model.EstadoSesion
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.model.Recomendacion
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import com.sanna.rehabapp.domain.repository.RecomendacionRepository
import com.sanna.rehabapp.domain.repository.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetalleEjercicioAsignadoUiState(
    val ejercicio: Ejercicio? = null,
    // Se puede iniciar (o reanudar) solo si la sesión no está completa.
    val sesionPendiente: Boolean = false,
    val reanudable: Boolean = false,
    // Rango de ángulo que la IA medirá en ESTA sesión: el personalizado por
    // el fisioterapeuta (HU03-CA08) o, si no hay, el del ejercicio. Ya
    // formateado ("90°–140°"); null si el ejercicio no define ninguno.
    val anguloObjetivo: String? = null,
    // Repeticiones de esta sesión: el override del fisio o el valor del ejercicio.
    val repeticiones: Int = 0,
    // Nota del fisioterapeuta al asignar la sesión (HU03-CA05), si dejó una.
    val notaClinica: String? = null,
    // Recomendaciones que el fisioterapeuta registró para esta sesión (HU16).
    val recomendaciones: List<Recomendacion> = emptyList(),
    val cargando: Boolean = true,
)

// HU04-CA02/CA03 — detalle de un ejercicio asignado. HU05 agrega aquí
// mismo la sección de material terapéutico (imagen/video).
@HiltViewModel
class DetalleEjercicioAsignadoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val sesionRepository: SesionRepository,
    private val ejercicioRepository: EjercicioRepository,
    private val recomendacionRepository: RecomendacionRepository,
) : ViewModel() {

    val sesionId: String = checkNotNull(savedStateHandle[Rutas.ARG_SESION_ID])

    private val _uiState = MutableStateFlow(DetalleEjercicioAsignadoUiState())
    val uiState: StateFlow<DetalleEjercicioAsignadoUiState> = _uiState

    init {
        cargar()
    }

    private fun cargar() {
        val pacienteId = authRepository.uidActual
        if (pacienteId == null) {
            _uiState.update { it.copy(cargando = false) }
            return
        }
        viewModelScope.launch {
            recomendacionRepository.observarDe(pacienteId, sesionId)
                .catch { emit(emptyList()) }
                .collect { lista -> _uiState.update { it.copy(recomendaciones = lista) } }
        }
        // ERR-PAC-002: la sesión se observa en vivo; así, al completarla y volver
        // con "Salir", el botón "Iniciar sesión" desaparece sin recargar.
        viewModelScope.launch {
            var ejercicioCargado: Ejercicio? = null
            var ejercicioIdCargado: String? = null
            sesionRepository.observarSesionesDe(pacienteId)
                .catch { _uiState.update { it.copy(cargando = false) } }
                .collect { sesiones ->
                    val sesion = sesiones.firstOrNull { it.id == sesionId }
                    if (sesion != null && sesion.ejercicioId != ejercicioIdCargado) {
                        ejercicioCargado = ejercicioRepository.obtenerEjercicio(sesion.ejercicioId)
                        ejercicioIdCargado = sesion.ejercicioId
                    }
                    val ejercicio = if (sesion != null) ejercicioCargado else null
                    val rango = if (sesion?.anguloMinOverride != null && sesion.anguloMaxOverride != null) {
                        sesion.anguloMinOverride to sesion.anguloMaxOverride
                    } else {
                        ejercicio?.patronesReferencia?.firstOrNull()?.let { it.anguloMin to it.anguloMax }
                    }
                    val asignadas = sesion?.repeticiones ?: ejercicio?.repeticiones ?: 0
                    val hechas = sesion?.resultado?.repeticionesCompletadas ?: 0
                    val pendiente = sesion?.estado == EstadoSesion.PENDIENTE
                    val reanudable = sesion?.estado == EstadoSesion.COMPLETADA && hechas < asignadas
                    _uiState.update {
                        it.copy(
                            ejercicio = ejercicio,
                            sesionPendiente = pendiente || reanudable,
                            reanudable = reanudable,
                            anguloObjetivo = rango?.let { (min, max) -> "${min.toInt()}°–${max.toInt()}°" },
                            repeticiones = asignadas,
                            notaClinica = sesion?.notas?.takeIf { n -> n.isNotBlank() },
                            cargando = false,
                        )
                    }
                }
        }
    }
}
