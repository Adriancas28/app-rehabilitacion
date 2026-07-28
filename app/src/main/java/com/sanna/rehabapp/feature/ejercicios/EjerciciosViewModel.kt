package com.sanna.rehabapp.feature.ejercicios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EjerciciosUiState(
    val ejercicios: List<Ejercicio> = emptyList(),
    val cargando: Boolean = true,
)

// HU02-CA04 — lista de ejercicios registrados.
@HiltViewModel
class EjerciciosViewModel @Inject constructor(
    private val ejercicioRepository: EjercicioRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EjerciciosUiState())
    val uiState: StateFlow<EjerciciosUiState> = _uiState

    init {
        viewModelScope.launch {
            ejercicioRepository.observarEjercicios().collect { lista ->
                _uiState.update { it.copy(ejercicios = lista, cargando = false) }
            }
        }
    }

    // HU02-CA06
    fun eliminar(id: String) {
        viewModelScope.launch {
            ejercicioRepository.eliminarEjercicio(id)
        }
    }
}
