package com.sanna.rehabapp.feature.paciente

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.core.navigation.Rutas
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.model.EstadoSesion
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import com.sanna.rehabapp.domain.repository.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetalleEjercicioAsignadoUiState(
    val ejercicio: Ejercicio? = null,
    val sesionPendiente: Boolean = false,
    // Rango de ángulo que la IA medirá en ESTA sesión: el personalizado por
    // el fisioterapeuta (HU03-CA08) o, si no hay, el del ejercicio. Ya
    // formateado ("90°–140°"); null si el ejercicio no define ninguno.
    val anguloObjetivo: String? = null,
    // Repeticiones de esta sesión: el override del fisio o el valor del ejercicio.
    val repeticiones: Int = 0,
    // Nota del fisioterapeuta al asignar la sesión (HU03-CA05), si dejó una.
    val notaClinica: String? = null,
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
            val sesion = sesionRepository.obtenerSesion(pacienteId, sesionId)
            val ejercicio = sesion?.let { ejercicioRepository.obtenerEjercicio(it.ejercicioId) }
            val rango = if (sesion?.anguloMinOverride != null && sesion.anguloMaxOverride != null) {
                sesion.anguloMinOverride to sesion.anguloMaxOverride
            } else {
                ejercicio?.patronesReferencia?.firstOrNull()?.let { it.anguloMin to it.anguloMax }
            }
            _uiState.update {
                it.copy(
                    ejercicio = ejercicio,
                    sesionPendiente = sesion?.estado == EstadoSesion.PENDIENTE,
                    anguloObjetivo = rango?.let { (min, max) -> "${min.toInt()}°–${max.toInt()}°" },
                    repeticiones = sesion?.repeticiones ?: ejercicio?.repeticiones ?: 0,
                    notaClinica = sesion?.notas?.takeIf { it.isNotBlank() },
                    cargando = false,
                )
            }
        }
    }
}
