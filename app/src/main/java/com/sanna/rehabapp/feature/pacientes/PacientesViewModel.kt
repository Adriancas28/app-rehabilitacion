package com.sanna.rehabapp.feature.pacientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.domain.model.Usuario
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PacientesUiState(
    val pacientesFiltrados: List<Usuario> = emptyList(),
    val totalPacientes: Int = 0,
    val consultaBusqueda: String = "",
    val cargando: Boolean = true,
)

// HU01 — gestionar pacientes asignados al fisioterapeuta autenticado.
@HiltViewModel
class PacientesViewModel @Inject constructor(
    authRepository: AuthRepository,
    usuarioRepository: UsuarioRepository,
) : ViewModel() {

    private val consultaBusqueda = MutableStateFlow("")

    private val pacientes = authRepository.uidActual
        ?.let { fisioterapeutaId -> usuarioRepository.observarPacientesDe(fisioterapeutaId) }
        ?: flowOf(emptyList())

    val uiState: StateFlow<PacientesUiState> = combine(
        pacientes,
        consultaBusqueda,
    ) { lista, consulta ->
        PacientesUiState(
            pacientesFiltrados = filtrarPacientes(lista, consulta),
            totalPacientes = lista.size,
            consultaBusqueda = consulta,
            cargando = false,
        )
    }
        // Evita que un PERMISSION_DENIED por cierre de sesión mientras esta
        // pantalla sigue activa tumbe la app (excepción no atrapada).
        .catch { }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PacientesUiState(),
        )

    fun onConsultaCambiada(valor: String) {
        consultaBusqueda.value = valor
    }
}
