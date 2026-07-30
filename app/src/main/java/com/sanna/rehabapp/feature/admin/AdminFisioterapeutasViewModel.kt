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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminFisioterapeutasUiState(
    val fisioterapeutas: List<Usuario> = emptyList(),
    // Relación uno (fisioterapeuta) a muchos (pacientes): cuántos pacientes
    // tiene asignados cada fisioterapeuta, para mostrarlo en su tarjeta —
    // simétrico a lo que ya muestra AdminPacientesScreen del lado del paciente.
    val pacientesPorFisioterapeuta: Map<String, Int> = emptyMap(),
    val cargando: Boolean = true,
    // Confirmación (Snackbar) de la última acción CRUD — eliminar.
    val mensaje: String? = null,
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
                // Conserva el mensaje de confirmación pendiente aunque el
                // listener en vivo vuelva a emitir mientras se muestra.
                .collect { estado -> _uiState.update { actual -> estado.copy(mensaje = actual.mensaje) } }
        }
    }

    fun eliminar(uid: String, nombre: String) {
        viewModelScope.launch {
            adminRepository.eliminarUsuario(uid).fold(
                onSuccess = { _uiState.update { it.copy(mensaje = "Se eliminó a $nombre.") } },
                onFailure = { _uiState.update { it.copy(mensaje = "No se pudo eliminar a $nombre.") } },
            )
        }
    }

    fun mensajeMostrado() = _uiState.update { it.copy(mensaje = null) }
}
