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
    val loginExitoso: Boolean = false,
)

// RNF02-CA01: valida la autenticación contra Firebase Auth.
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    avisoLogin: com.sanna.rehabapp.core.navigation.AvisoLogin,
) : ViewModel() {

    // ERR-ADM-009: aviso pendiente (p. ej. cuenta desactivada) al volver al login.
    private val _uiState = MutableStateFlow(LoginUiState(error = avisoLogin.consumir()))
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onEmailChange(valor: String) {
        _uiState.update { it.copy(email = valor, error = null) }
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
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(estado.email.trim()).matches()) {
            _uiState.update { it.copy(error = "El correo no tiene un formato válido (ej. nombre@correo.com).") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            authRepository.login(estado.email.trim(), estado.password).fold(
                onSuccess = {
                    _uiState.update { it.copy(cargando = false, loginExitoso = true) }
                },
                onFailure = { fallo ->
                    val mensaje = when (fallo) {
                        is com.google.firebase.FirebaseNetworkException ->
                            "Sin conexión a internet. Revisa tu red e intenta de nuevo."
                        is com.google.firebase.auth.FirebaseAuthInvalidUserException,
                        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                            "Correo o contraseña incorrectos."
                        else -> "No se pudo iniciar sesión. Intenta de nuevo."
                    }
                    _uiState.update { it.copy(cargando = false, error = mensaje) }
                },
            )
        }
    }
}
