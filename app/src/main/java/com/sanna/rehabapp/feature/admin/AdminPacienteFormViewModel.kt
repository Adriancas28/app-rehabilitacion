package com.sanna.rehabapp.feature.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.core.navigation.Rutas
import com.sanna.rehabapp.domain.model.TipoDiagnostico
import com.sanna.rehabapp.domain.repository.AdminRepository
import com.sanna.rehabapp.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// HU20-CA02/CA03: registrar y editar un paciente desde el panel de admin,
// incluyendo DNI, edad y diagnóstico (revisión acordada).
@HiltViewModel
class AdminPacienteFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val adminRepository: AdminRepository,
    private val usuarioRepository: UsuarioRepository,
) : ViewModel() {

    private val usuarioIdArg: String? = savedStateHandle[Rutas.ARG_ADMIN_USUARIO_ID]
    val esEdicion: Boolean get() = !usuarioIdArg.isNullOrBlank()

    private val _uiState = MutableStateFlow(AdminPacienteFormUiState())
    val uiState: StateFlow<AdminPacienteFormUiState> = _uiState

    init {
        if (esEdicion) cargarUsuario(usuarioIdArg!!)
    }

    private fun cargarUsuario(uid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            val usuario = usuarioRepository.obtenerUsuario(uid)
            _uiState.update {
                it.copy(
                    nombre = usuario?.nombre ?: "",
                    email = usuario?.email ?: "",
                    dni = usuario?.dni ?: "",
                    edad = usuario?.edad?.toString() ?: "",
                    tipoDiagnostico = usuario?.tipoDiagnostico,
                    cargando = false,
                )
            }
        }
    }

    fun onNombreCambiado(valor: String) = _uiState.update { it.copy(nombre = valor, error = null) }
    fun onEmailCambiado(valor: String) = _uiState.update { it.copy(email = valor, error = null) }
    fun onPasswordCambiado(valor: String) = _uiState.update { it.copy(password = valor, error = null) }
    fun onDniCambiado(valor: String) = _uiState.update { it.copy(dni = valor, error = null) }
    fun onEdadCambiado(valor: String) = _uiState.update { it.copy(edad = valor, error = null) }
    fun onTipoDiagnosticoCambiado(valor: TipoDiagnostico) =
        _uiState.update { it.copy(tipoDiagnostico = valor, error = null) }

    fun guardar() {
        val estado = _uiState.value
        val edadInt = estado.edad.toIntOrNull()
        if (estado.nombre.isBlank() || estado.email.isBlank() ||
            (!esEdicion && estado.password.isBlank()) ||
            estado.dni.isBlank() || edadInt == null || edadInt <= 0 || estado.tipoDiagnostico == null
        ) {
            _uiState.update { it.copy(error = "Completa todos los campos requeridos.") }
            return
        }
        val tipoDiagnostico = estado.tipoDiagnostico
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, error = null) }
            val resultado = if (esEdicion) {
                adminRepository.actualizarPaciente(
                    usuarioIdArg!!,
                    estado.nombre.trim(),
                    estado.email.trim(),
                    estado.dni.trim(),
                    edadInt,
                    tipoDiagnostico,
                )
            } else {
                adminRepository.crearPaciente(
                    estado.nombre.trim(),
                    estado.email.trim(),
                    estado.password,
                    estado.dni.trim(),
                    edadInt,
                    tipoDiagnostico,
                )
            }
            resultado.fold(
                onSuccess = { _uiState.update { it.copy(guardando = false, guardadoExitoso = true) } },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            guardando = false,
                            error = "No se pudo guardar. Verifica los datos e intenta de nuevo.",
                        )
                    }
                },
            )
        }
    }
}
