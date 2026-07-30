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

data class AdminPacientesUiState(
    val pacientes: List<Usuario> = emptyList(),
    val fisioterapeutas: List<Usuario> = emptyList(),
    val cargando: Boolean = true,
    // Confirmación (Snackbar) de la última acción CRUD — eliminar/asignar.
    val mensaje: String? = null,
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
            }
                // Evita que un PERMISSION_DENIED por cierre de sesión mientras
                // esta pantalla sigue activa tumbe la app (excepción no atrapada).
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

    // HU20-CA05
    fun asignarFisioterapeuta(pacienteId: String, fisioterapeutaId: String, nombreFisio: String) {
        viewModelScope.launch {
            adminRepository.asignarFisioterapeuta(pacienteId, fisioterapeutaId).fold(
                onSuccess = { _uiState.update { it.copy(mensaje = "Se asignó a $nombreFisio.") } },
                onFailure = { _uiState.update { it.copy(mensaje = "No se pudo asignar el fisioterapeuta.") } },
            )
        }
    }

    fun mensajeMostrado() = _uiState.update { it.copy(mensaje = null) }
}
