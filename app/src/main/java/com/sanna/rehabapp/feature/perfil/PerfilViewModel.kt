package com.sanna.rehabapp.feature.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.domain.model.Rol
import com.sanna.rehabapp.domain.model.Usuario
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PerfilUiState(
    val usuario: Usuario? = null,
    val nombreFisioterapeuta: String? = null,
    val cargando: Boolean = true,
    val nombre: String = "",
    val guardando: Boolean = false,
    val mensaje: String? = null,
)

// HU22/HU23 — el paciente/fisioterapeuta consulta su propia cuenta y solo
// puede editar su nombre; el resto de atributos (correo, DNI, edad,
// diagnóstico(s), fisioterapeuta asignado) quedan en solo lectura porque
// siguen siendo responsabilidad del administrador (HU20/HU21) o, en el
// caso del diagnóstico, del fisioterapeuta (HU01-CA06).
@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val usuarioRepository: UsuarioRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PerfilUiState())
    val uiState: StateFlow<PerfilUiState> = _uiState

    init {
        cargarPerfil()
    }

    private fun cargarPerfil() {
        val uid = authRepository.uidActual
        if (uid == null) {
            _uiState.update { it.copy(cargando = false) }
            return
        }
        viewModelScope.launch {
            val usuario = usuarioRepository.obtenerUsuario(uid)
            val nombreFisioterapeuta = if (usuario?.rol == Rol.PACIENTE) {
                usuario.fisioterapeutaId?.let { usuarioRepository.obtenerUsuario(it)?.nombre }
            } else {
                null
            }
            _uiState.update {
                it.copy(
                    usuario = usuario,
                    nombre = usuario?.nombre ?: "",
                    nombreFisioterapeuta = nombreFisioterapeuta,
                    cargando = false,
                )
            }
        }
    }

    fun onNombreCambiado(valor: String) {
        _uiState.update { it.copy(nombre = valor) }
    }

    fun guardarNombre() {
        val usuario = _uiState.value.usuario ?: return
        val nuevoNombre = _uiState.value.nombre.trim()
        if (nuevoNombre.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true) }
            usuarioRepository.actualizarNombre(usuario.uid, nuevoNombre)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            guardando = false,
                            usuario = usuario.copy(nombre = nuevoNombre),
                            mensaje = "Nombre actualizado.",
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(guardando = false, mensaje = "No se pudo actualizar el nombre.") }
                }
        }
    }

    fun mensajeMostrado() {
        _uiState.update { it.copy(mensaje = null) }
    }

    fun cerrarSesion() = authRepository.logout()
}
