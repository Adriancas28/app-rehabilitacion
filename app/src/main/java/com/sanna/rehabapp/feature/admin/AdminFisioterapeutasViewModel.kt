package com.sanna.rehabapp.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.domain.model.Usuario
import com.sanna.rehabapp.domain.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminFisioterapeutasUiState(
    val fisioterapeutas: List<Usuario> = emptyList(),
    val cargando: Boolean = true,
)

// HU21 — gestionar cuentas de fisioterapeutas desde el panel de administrador.
@HiltViewModel
class AdminFisioterapeutasViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminFisioterapeutasUiState())
    val uiState: StateFlow<AdminFisioterapeutasUiState> = _uiState

    init {
        viewModelScope.launch {
            adminRepository.observarFisioterapeutas().collect { lista ->
                _uiState.update { it.copy(fisioterapeutas = lista, cargando = false) }
            }
        }
    }

    fun eliminar(uid: String) {
        viewModelScope.launch { adminRepository.eliminarUsuario(uid) }
    }
}
