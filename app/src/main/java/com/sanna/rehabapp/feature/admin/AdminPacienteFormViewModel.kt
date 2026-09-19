package com.sanna.rehabapp.feature.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.core.navigation.Rutas
import com.sanna.rehabapp.domain.model.Genero
import com.sanna.rehabapp.domain.model.LadoAfectado
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
// incluyendo DNI, edad y uno o más diagnósticos (revisión acordada).
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
                    diagnosticosSeleccionados = usuario?.diagnosticos?.map { d -> d.tipo }?.toSet() ?: emptySet(),
                    ladoAfectado = usuario?.ladoAfectado ?: LadoAfectado.DERECHO,
                    genero = usuario?.genero,
                    numeroContacto = usuario?.numeroContacto ?: "",
                    cargando = false,
                )
            }
        }
    }

    private fun AdminPacienteFormUiState.sinError(campo: String) =
        copy(error = null, errores = errores - campo)

    fun onNombreCambiado(valor: String) = _uiState.update { it.copy(nombre = valor).sinError("nombre") }
    fun onEmailCambiado(valor: String) = _uiState.update { it.copy(email = valor).sinError("email") }
    fun onPasswordCambiado(valor: String) = _uiState.update { it.copy(password = valor).sinError("password") }
    // ERR-ADM-001/008: la entrada numérica se filtra (solo dígitos, con tope).
    fun onDniCambiado(valor: String) =
        _uiState.update { it.copy(dni = ValidacionesAdmin.soloDigitos(valor, 8)).sinError("dni") }
    fun onEdadCambiado(valor: String) =
        _uiState.update { it.copy(edad = ValidacionesAdmin.soloDigitos(valor, 3)).sinError("edad") }

    fun onLadoCambiado(lado: LadoAfectado) = _uiState.update { it.copy(ladoAfectado = lado, error = null) }
    fun onGeneroCambiado(genero: Genero) = _uiState.update { it.copy(genero = genero).sinError("genero") }
    fun onNumeroContactoCambiado(valor: String) = _uiState.update {
        it.copy(numeroContacto = ValidacionesAdmin.soloDigitos(valor, 15)).sinError("contacto")
    }

    fun onDiagnosticoAlternado(tipo: TipoDiagnostico) = _uiState.update { estado ->
        val nuevos = if (tipo in estado.diagnosticosSeleccionados) {
            estado.diagnosticosSeleccionados - tipo
        } else {
            estado.diagnosticosSeleccionados + tipo
        }
        estado.copy(diagnosticosSeleccionados = nuevos).sinError("diagnosticos")
    }

    fun guardar() {
        // ERR-ADM-005: protección contra doble toque (el estado se marca de
        // forma síncrona, antes de lanzar la corrutina).
        if (_uiState.value.guardando || _uiState.value.cargando) return
        val estado = _uiState.value
        val errores = buildMap {
            ValidacionesAdmin.nombre(estado.nombre)?.let { put("nombre", it) }
            if (!esEdicion) {
                ValidacionesAdmin.email(estado.email)?.let { put("email", it) }
                ValidacionesAdmin.password(estado.password)?.let { put("password", it) }
            }
            ValidacionesAdmin.dni(estado.dni)?.let { put("dni", it) }
            ValidacionesAdmin.edad(estado.edad)?.let { put("edad", it) }
            ValidacionesAdmin.contacto(estado.numeroContacto)?.let { put("contacto", it) }
            if (estado.genero == null) put("genero", "Selecciona el género.")
            if (estado.diagnosticosSeleccionados.isEmpty()) put("diagnosticos", "Selecciona al menos un diagnóstico.")
        }
        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(errores = errores, error = "Corrige los campos marcados.") }
            return
        }
        val edadInt = estado.edad.toInt()
        val genero = estado.genero!!
        _uiState.update { it.copy(guardando = true, error = null, errores = emptyMap()) }
        val diagnosticos = estado.diagnosticosSeleccionados.toList()
        viewModelScope.launch {
            // ERR-ADM-002: el DNI debe ser único entre pacientes.
            val dniDuplicado = runCatching { adminRepository.existeDni(estado.dni.trim(), usuarioIdArg) }
                .getOrDefault(false)
            if (dniDuplicado) {
                _uiState.update {
                    it.copy(
                        guardando = false,
                        errores = mapOf("dni" to "Ya existe otro paciente con este DNI."),
                        error = "Corrige los campos marcados.",
                    )
                }
                return@launch
            }
            val resultado = if (esEdicion) {
                adminRepository.actualizarPaciente(
                    usuarioIdArg!!,
                    estado.nombre.trim(),
                    estado.email.trim(),
                    estado.dni.trim(),
                    edadInt,
                    diagnosticos,
                    estado.ladoAfectado,
                    genero,
                    estado.numeroContacto.trim(),
                )
            } else {
                adminRepository.crearPaciente(
                    estado.nombre.trim(),
                    estado.email.trim(),
                    estado.password,
                    estado.dni.trim(),
                    edadInt,
                    diagnosticos,
                    estado.ladoAfectado,
                    genero,
                    estado.numeroContacto.trim(),
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
