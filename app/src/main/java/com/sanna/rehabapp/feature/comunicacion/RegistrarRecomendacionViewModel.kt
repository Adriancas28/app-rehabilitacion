package com.sanna.rehabapp.feature.comunicacion

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.core.navigation.Rutas
import com.sanna.rehabapp.domain.model.Recomendacion
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.RecomendacionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistrarRecomendacionUiState(
    val recomendaciones: List<Recomendacion> = emptyList(),
    val texto: String = "",
    val editandoId: String? = null,
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val error: String? = null,
)

// HU15 — el fisioterapeuta registra, edita y elimina recomendaciones sobre
// una sesión ya realizada (CA01-CA04).
@HiltViewModel
class RegistrarRecomendacionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val recomendacionRepository: RecomendacionRepository,
) : ViewModel() {

    private val pacienteId: String = checkNotNull(savedStateHandle[Rutas.ARG_PACIENTE_ID])
    private val sesionId: String = checkNotNull(savedStateHandle[Rutas.ARG_SESION_ID])

    private val _uiState = MutableStateFlow(RegistrarRecomendacionUiState())
    val uiState: StateFlow<RegistrarRecomendacionUiState> = _uiState

    init {
        viewModelScope.launch {
            recomendacionRepository.observarDe(pacienteId, sesionId)
                // Evita que un PERMISSION_DENIED por cierre de sesión mientras
                // esta pantalla sigue activa tumbe la app (excepción no atrapada).
                .catch { }
                .collect { lista -> _uiState.update { it.copy(recomendaciones = lista, cargando = false) } }
        }
    }

    fun onTextoCambiado(valor: String) = _uiState.update { it.copy(texto = valor, error = null) }

    // HU15-CA03 — precarga el texto de una recomendación existente para editarla.
    fun editar(recomendacion: Recomendacion) =
        _uiState.update { it.copy(editandoId = recomendacion.id, texto = recomendacion.texto) }

    fun cancelarEdicion() = _uiState.update { it.copy(editandoId = null, texto = "") }

    fun guardar() {
        val estado = _uiState.value
        val texto = estado.texto.trim()
        if (texto.isEmpty()) {
            _uiState.update { it.copy(error = "Escribe una recomendación.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, error = null) }
            val idEnEdicion = estado.editandoId
            val resultado = if (idEnEdicion != null) {
                recomendacionRepository.actualizar(pacienteId, sesionId, idEnEdicion, texto)
            } else {
                val fisioterapeutaId = authRepository.uidActual
                if (fisioterapeutaId == null) {
                    _uiState.update { it.copy(guardando = false, error = "Sesión inválida, vuelve a iniciar sesión.") }
                    return@launch
                }
                recomendacionRepository.crear(pacienteId, sesionId, fisioterapeutaId, texto)
            }
            resultado.fold(
                onSuccess = { _uiState.update { it.copy(guardando = false, texto = "", editandoId = null) } },
                onFailure = {
                    _uiState.update { it.copy(guardando = false, error = "No se pudo guardar la recomendación.") }
                },
            )
        }
    }

    // HU15-CA04 — eliminar una recomendación (la pantalla ya pide confirmación antes de llamar esto).
    fun eliminar(recomendacionId: String) {
        viewModelScope.launch {
            recomendacionRepository.eliminar(pacienteId, sesionId, recomendacionId)
        }
    }
}
