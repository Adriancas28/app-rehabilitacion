package com.sanna.rehabapp.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.domain.model.Usuario
import com.sanna.rehabapp.domain.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminFisioterapeutasUiState(
    val fisioterapeutas: List<Usuario> = emptyList(),
    // Relación uno (fisioterapeuta) a muchos (pacientes): cuántos pacientes
    // tiene asignados cada fisioterapeuta, para mostrarlo en su tarjeta —
    // simétrico a lo que ya muestra AdminPacientesScreen del lado del paciente.
    val pacientesPorFisioterapeuta: Map<String, Int> = emptyMap(),
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
            combine(
                adminRepository.observarFisioterapeutas(),
                adminRepository.observarPacientes(),
            ) { fisioterapeutas, pacientes ->
                AdminFisioterapeutasUiState(
                    fisioterapeutas = fisioterapeutas,
                    pacientesPorFisioterapeuta = pacientes
                        .mapNotNull { it.fisioterapeutaId }
                        .groupingBy { it }
                        .eachCount(),
                    cargando = false,
                )
            }
                .catch { }
                .collect { estado -> _uiState.value = estado }
        }
    }

    fun eliminar(uid: String) {
        viewModelScope.launch { adminRepository.eliminarUsuario(uid) }
    }
}
