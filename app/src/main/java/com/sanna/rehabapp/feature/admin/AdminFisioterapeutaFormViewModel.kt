package com.sanna.rehabapp.feature.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.core.navigation.Rutas
import com.sanna.rehabapp.domain.model.Genero
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
                it.copy(
                    nombre = usuario?.nombre ?: "",
                    email = usuario?.email ?: "",
                    edad = usuario?.edad?.toString() ?: "",
                    genero = usuario?.genero,
                    numeroContacto = usuario?.numeroContacto ?: "",
                    especialidad = usuario?.especialidad ?: "",
                    numeroColegiatura = usuario?.numeroColegiatura ?: "",
                    cargando = false,
                )
            }
        }
    }

    private fun AdminUsuarioFormUiState.sinError(campo: String) =
        copy(error = null, errores = errores - campo)

    fun onNombreCambiado(valor: String) = _uiState.update { it.copy(nombre = valor).sinError("nombre") }
    fun onEmailCambiado(valor: String) = _uiState.update { it.copy(email = valor).sinError("email") }
    fun onPasswordCambiado(valor: String) = _uiState.update { it.copy(password = valor).sinError("password") }
    fun onEdadCambiado(valor: String) =
        _uiState.update { it.copy(edad = ValidacionesAdmin.soloDigitos(valor, 3)).sinError("edad") }
    fun onGeneroCambiado(valor: Genero) = _uiState.update { it.copy(genero = valor).sinError("genero") }
    fun onNumeroContactoCambiado(valor: String) = _uiState.update {
        it.copy(numeroContacto = ValidacionesAdmin.soloDigitos(valor, 15)).sinError("contacto")
    }
    fun onEspecialidadCambiado(valor: String) = _uiState.update { it.copy(especialidad = valor, error = null) }
    fun onNumeroColegiaturaCambiado(valor: String) = _uiState.update { it.copy(numeroColegiatura = valor, error = null) }

    fun guardar() {
        // ERR-ADM-005: protección contra doble toque (estado marcado de forma síncrona).
        if (_uiState.value.guardando || _uiState.value.cargando) return
        val estado = _uiState.value
        val errores = buildMap {
            ValidacionesAdmin.nombre(estado.nombre)?.let { put("nombre", it) }
            if (!esEdicion) {
                ValidacionesAdmin.email(estado.email)?.let { put("email", it) }
                ValidacionesAdmin.password(estado.password)?.let { put("password", it) }
            }
            ValidacionesAdmin.edad(estado.edad)?.let { put("edad", it) }
            ValidacionesAdmin.contacto(estado.numeroContacto)?.let { put("contacto", it) }
            if (estado.genero == null) put("genero", "Selecciona el género.")
        }
        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(errores = errores, error = "Corrige los campos marcados.") }
            return
        }
        val edadInt = estado.edad.toInt()
        val genero = estado.genero!!
        _uiState.update { it.copy(guardando = true, error = null, errores = emptyMap()) }
        viewModelScope.launch {
            val resultado = if (esEdicion) {
                adminRepository.actualizarFisioterapeuta(
                    usuarioIdArg!!,
                    estado.nombre.trim(),
                    estado.email.trim(),
                    edadInt,
                    genero,
                    estado.numeroContacto.trim(),
                    estado.especialidad.trim().ifBlank { null },
                    estado.numeroColegiatura.trim().ifBlank { null },
                )
            } else {
                adminRepository.crearFisioterapeuta(
                    estado.nombre.trim(),
                    estado.email.trim(),
                    estado.password,
                    edadInt,
                    genero,
                    estado.numeroContacto.trim(),
                    estado.especialidad.trim().ifBlank { null },
                    estado.numeroColegiatura.trim().ifBlank { null },
                )
            }
            resultado.fold(
                onSuccess = { _uiState.update { it.copy(guardando = false, guardadoExitoso = true) } },
                onFailure = { fallo ->
                    _uiState.update {
                        it.copy(guardando = false, error = ValidacionesAdmin.mensajeDeErrorGuardado(fallo))
                    }
                },
            )
        }
    }
}
