package com.sanna.rehabapp.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.core.consent.ConsentimientoLocalStore
import com.sanna.rehabapp.domain.model.Rol
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConsentimientoUiState(
    val rolResuelto: Rol? = null,
    val aceptando: Boolean = false,
    val consentimientoOtorgado: Boolean = false,
)

// RNF06-CA04: presenta el consentimiento informado en el primer uso.
@HiltViewModel
class ConsentimientoViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val usuarioRepository: UsuarioRepository,
    private val consentimientoLocalStore: ConsentimientoLocalStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsentimientoUiState())
    val uiState: StateFlow<ConsentimientoUiState> = _uiState

    init {
        viewModelScope.launch {
            val uid = authRepository.uidActual ?: return@launch
            val usuario = usuarioRepository.obtenerUsuario(uid) ?: return@launch
            _uiState.update { it.copy(rolResuelto = usuario.rol) }
        }
    }

    fun aceptar() {
        if (_uiState.value.rolResuelto == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(aceptando = true) }
            consentimientoLocalStore.marcarAceptado()
            _uiState.update { it.copy(aceptando = false, consentimientoOtorgado = true) }
        }
    }
}
