package com.sanna.rehabapp.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.domain.model.Rol
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DestinoInicial {
    data object Cargando : DestinoInicial
    data object Login : DestinoInicial
    data class Grafo(val rol: Rol) : DestinoInicial
}

// RNF02-CA01: decide a dónde va el usuario según si hay una sesión válida
// (Firebase Auth persiste la sesión entre reinicios de la app).
@HiltViewModel
class RaizViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val usuarioRepository: UsuarioRepository,
) : ViewModel() {

    private val _destino = MutableStateFlow<DestinoInicial>(DestinoInicial.Cargando)
    val destino: StateFlow<DestinoInicial> = _destino

    init {
        resolverDestino()
    }

    private fun resolverDestino() {
        viewModelScope.launch {
            val uid = authRepository.uidActual
            if (uid == null) {
                _destino.value = DestinoInicial.Login
                return@launch
            }
            val usuario = usuarioRepository.obtenerUsuario(uid)
            _destino.value = if (usuario != null) {
                DestinoInicial.Grafo(usuario.rol)
            } else {
                authRepository.logout()
                DestinoInicial.Login
            }
        }
    }
}
