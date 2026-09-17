package com.sanna.rehabapp.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.domain.model.Usuario
import com.sanna.rehabapp.domain.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// Etapa 2A (dashboard Admin) — punto de entrada: lista de pacientes, igual
// que AdminPacientesScreen, pero cada tarjeta abre el dashboard de
// progreso de ESE paciente (AdminPacienteDashboardScreen) en vez de un
// formulario de edición.
data class AdminDashboardUiState(
    val pacientes: List<Usuario> = emptyList(),
    val cargando: Boolean = true,
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
) : ViewModel() {

    val uiState: StateFlow<AdminDashboardUiState> = adminRepository.observarPacientes()
        .map { pacientes -> AdminDashboardUiState(pacientes = pacientes, cargando = false) }
        .catch { }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AdminDashboardUiState(),
        )
}
