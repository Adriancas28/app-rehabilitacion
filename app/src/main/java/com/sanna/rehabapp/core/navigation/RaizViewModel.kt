package com.sanna.rehabapp.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.core.consent.ConsentimientoLocalStore
import com.sanna.rehabapp.domain.model.Rol
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DestinoInicial {
    data object Cargando : DestinoInicial
    data object Login : DestinoInicial
    data class Consentimiento(val rol: Rol) : DestinoInicial
    data class Grafo(val rol: Rol) : DestinoInicial
}

// RNF02-CA01: decide a dónde va el usuario según si hay una sesión válida
// (Firebase Auth persiste la sesión entre reinicios de la app) y, de haberla,
// si ya aceptó el consentimiento informado (RNF06-CA04).
@HiltViewModel
class RaizViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val usuarioRepository: UsuarioRepository,
    private val consentimientoLocalStore: ConsentimientoLocalStore,
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
            if (usuario == null) {
                authRepository.logout()
                _destino.value = DestinoInicial.Login
                return@launch
            }
            // RNF06 trata sobre el tratamiento de los datos biométricos del
            // PACIENTE (cámara/pose); al fisioterapeuta no le aplica ese aviso.
            if (usuario.rol == Rol.FISIOTERAPEUTA) {
                _destino.value = DestinoInicial.Grafo(usuario.rol)
                return@launch
            }
            val yaAcepto = consentimientoLocalStore.consentimientoAceptado.first()
            _destino.value = if (yaAcepto) {
                DestinoInicial.Grafo(usuario.rol)
            } else {
                DestinoInicial.Consentimiento(usuario.rol)
            }
        }
    }
}
