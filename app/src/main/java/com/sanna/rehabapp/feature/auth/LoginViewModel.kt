package com.sanna.rehabapp.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null,
    val loginExitoso: Boolean = false,
)

// RNF02-CA01: valida la autenticación contra Firebase Auth.
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onEmailChange(valor: String) {
        _uiState.update { it.copy(email = valor, error = null, mensaje = null) }
    }

    fun onPasswordChange(valor: String) {
        _uiState.update { it.copy(password = valor, error = null) }
    }

    fun iniciarSesion() {
        val estado = _uiState.value
        if (estado.email.isBlank() || estado.password.isBlank()) {
            _uiState.update { it.copy(error = "Ingresa tu correo y contraseña.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null, mensaje = null) }
            authRepository.login(estado.email.trim(), estado.password).fold(
                onSuccess = {
                    _uiState.update { it.copy(cargando = false, loginExitoso = true) }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            error = "No se pudo iniciar sesión. Verifica tus credenciales.",
                        )
                    }
                },
            )
        }
    }

    fun recuperarContrasena() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Ingresa tu correo para recuperar tu contraseña.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null, mensaje = null) }
            authRepository.enviarCorreoRecuperacion(email).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            mensaje = "Te enviamos un correo a $email para restablecer tu contraseña.",
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            error = "No se pudo enviar el correo. Verifica que sea el correo correcto.",
                        )
                    }
                },
            )
        }
    }
}
