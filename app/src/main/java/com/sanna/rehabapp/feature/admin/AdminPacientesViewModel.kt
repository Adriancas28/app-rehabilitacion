package com.sanna.rehabapp.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.domain.model.Usuario
import com.sanna.rehabapp.domain.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminPacientesUiState(
    val pacientes: List<Usuario> = emptyList(),
    val fisioterapeutas: List<Usuario> = emptyList(),
    val cargando: Boolean = true,
)

// HU20 — gestionar cuentas de pacientes desde el panel de administrador.
@HiltViewModel
class AdminPacientesViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminPacientesUiState())
    val uiState: StateFlow<AdminPacientesUiState> = _uiState

    init {
        viewModelScope.launch {
            combine(
                adminRepository.observarPacientes(),
                adminRepository.observarFisioterapeutas(),
            ) { pacientes, fisioterapeutas ->
                AdminPacientesUiState(pacientes = pacientes, fisioterapeutas = fisioterapeutas, cargando = false)
            }.collect { estado -> _uiState.value = estado }
        }
    }

    fun eliminar(uid: String) {
        viewModelScope.launch { adminRepository.eliminarUsuario(uid) }
    }

    // HU20-CA05
    fun asignarFisioterapeuta(pacienteId: String, fisioterapeutaId: String) {
        viewModelScope.launch { adminRepository.asignarFisioterapeuta(pacienteId, fisioterapeutaId) }
    }
}
