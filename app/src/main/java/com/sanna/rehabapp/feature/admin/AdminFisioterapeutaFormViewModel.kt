package com.sanna.rehabapp.feature.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.core.navigation.Rutas
import com.sanna.rehabapp.domain.repository.AdminRepository
import com.sanna.rehabapp.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// HU21-CA02/CA03: registrar y editar un fisioterapeuta desde el panel de admin.
@HiltViewModel
class AdminFisioterapeutaFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val adminRepository: AdminRepository,
    private val usuarioRepository: UsuarioRepository,
) : ViewModel() {

    private val usuarioIdArg: String? = savedStateHandle[Rutas.ARG_ADMIN_USUARIO_ID]
    val esEdicion: Boolean get() = !usuarioIdArg.isNullOrBlank()

    private val _uiState = MutableStateFlow(AdminUsuarioFormUiState())
    val uiState: StateFlow<AdminUsuarioFormUiState> = _uiState

    init {
        if (esEdicion) cargarUsuario(usuarioIdArg!!)
    }

    private fun cargarUsuario(uid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            val usuario = usuarioRepository.obtenerUsuario(uid)
            _uiState.update {
                it.copy(nombre = usuario?.nombre ?: "", email = usuario?.email ?: "", cargando = false)
            }
        }
    }

    fun onNombreCambiado(valor: String) = _uiState.update { it.copy(nombre = valor, error = null) }
    fun onEmailCambiado(valor: String) = _uiState.update { it.copy(email = valor, error = null) }
    fun onPasswordCambiado(valor: String) = _uiState.update { it.copy(password = valor, error = null) }

    fun guardar() {
        val estado = _uiState.value
        if (estado.nombre.isBlank() || estado.email.isBlank() || (!esEdicion && estado.password.isBlank())) {
            _uiState.update { it.copy(error = "Completa todos los campos requeridos.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, error = null) }
            val resultado = if (esEdicion) {
                adminRepository.actualizarUsuario(usuarioIdArg!!, estado.nombre.trim(), estado.email.trim())
            } else {
                adminRepository.crearFisioterapeuta(estado.nombre.trim(), estado.email.trim(), estado.password)
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
