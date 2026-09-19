package com.sanna.rehabapp.feature.pacientes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.core.navigation.Rutas
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.model.ResultadoSesion
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import com.sanna.rehabapp.domain.repository.RecomendacionRepository
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
    // Parte 3 (video): null si la sesión no tiene video grabado (sesiones
    // anteriores a esta función, o dispositivo sin soporte de grabación).
    val videoUrl: String? = null,
    // HU15 (ampliación, Etapa 4): campo de recomendación embebido en esta
    // misma pantalla (Idea 9 del mockup) -- solo para CREAR una nueva
    // rápidamente; editar/eliminar/ver el historial sigue en la pantalla
    // dedicada (onRegistrarRecomendacion), que no se reemplaza.
    val recomendacionTexto: String = "",
    val guardandoRecomendacion: Boolean = false,
    val recomendacionGuardada: Boolean = false,
)

// HU18-CA02/CA04 — el fisioterapeuta ve el resultado de una sesión ya
// completada, incluido el desglose por repetición, antes de decidir qué
// recomendación registrar (HU15).
@HiltViewModel
class FisioResultadoSesionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sesionRepository: SesionRepository,
    private val ejercicioRepository: EjercicioRepository,
    private val recomendacionRepository: RecomendacionRepository,
    private val authRepository: AuthRepository,
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
            _uiState.update {
                it.copy(
                    ejercicio = ejercicio,
                    resultado = sesion?.resultado,
                    videoUrl = sesion?.videoUrl,
                    cargando = false,
                )
            }
        }
    }

    fun onRecomendacionTextoCambiado(valor: String) =
        _uiState.update { it.copy(recomendacionTexto = valor.take(com.sanna.rehabapp.feature.comunicacion.LIMITE_RECOMENDACION), recomendacionGuardada = false) }

    fun guardarRecomendacion() {
        val texto = _uiState.value.recomendacionTexto.trim()
        val fisioterapeutaId = authRepository.uidActual
        if (texto.isBlank() || fisioterapeutaId == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(guardandoRecomendacion = true) }
            val resultado = recomendacionRepository.crear(pacienteId, sesionId, fisioterapeutaId, texto)
            _uiState.update {
                if (resultado.isSuccess) {
                    it.copy(guardandoRecomendacion = false, recomendacionTexto = "", recomendacionGuardada = true)
                } else {
                    it.copy(guardandoRecomendacion = false)
                }
            }
        }
    }
}
