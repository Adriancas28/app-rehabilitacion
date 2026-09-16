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
    // HU03 (ampliación, Etapa 4): true solo si el fisioterapeuta
    // personalizó el ángulo objetivo para esta sesión puntual.
    val tieneAnguloPersonalizado: Boolean = false,
    // Nota clínica opcional a mostrar junto al aviso -- reutiliza
    // sesion.notas (HU03-CA05), no se agrega un campo nuevo.
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
            val tienePersonalizacion = sesion?.anguloMinOverride != null && sesion.anguloMaxOverride != null
            _uiState.update {
                it.copy(
                    ejercicio = ejercicio,
                    sesionPendiente = sesion?.estado == EstadoSesion.PENDIENTE,
                    tieneAnguloPersonalizado = tienePersonalizacion,
                    notaClinica = sesion?.notas?.takeIf { tienePersonalizacion && it.isNotBlank() },
                    cargando = false,
                )
            }
        }
    }
}
