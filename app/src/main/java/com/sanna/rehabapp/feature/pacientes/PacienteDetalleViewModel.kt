package com.sanna.rehabapp.feature.pacientes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.core.navigation.Rutas
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.model.EstadoSesion
import com.sanna.rehabapp.domain.model.Sesion
import com.sanna.rehabapp.domain.model.TipoDiagnostico
import com.sanna.rehabapp.domain.model.Usuario
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import com.sanna.rehabapp.domain.repository.SesionRepository
import com.sanna.rehabapp.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PacienteDetalleUiState(
    val paciente: Usuario? = null,
    val sesiones: List<Sesion> = emptyList(),
    val ejerciciosPorId: Map<String, Ejercicio> = emptyMap(),
    // HU12-CA03: filtra tanto la lista de sesiones como el resumen de abajo.
    val filtroPeriodo: PeriodoFiltro = PeriodoFiltro.TODOS,
    val cargando: Boolean = true,
) {
    val sesionesFiltradas: List<Sesion>
        get() = sesiones.filter { cumplePeriodo(it.fechaAsignacion, filtroPeriodo) }

    val sesionesCompletadas: Int
        get() = sesionesFiltradas.count { it.estado == EstadoSesion.COMPLETADA }

    // HU12-CA01/CA02: progreso general (promedio del % de ejecución) sobre
    // las sesiones completadas que cumplen el filtro — la evolución
    // comparativa entre sesiones ya se ve en la lista de abajo, cada una
    // con su propio %.
    val porcentajePromedio: Float
        get() = sesionesFiltradas
            .mapNotNull { it.resultado?.porcentajeEjecucion }
            .let { valores -> if (valores.isEmpty()) 0f else valores.average().toFloat() }
}

// HU01-CA02/CA03 — detalle de un paciente: su información y sus sesiones.
// HU03 — desde aquí también se asigna/edita una sesión (ver pacienteId).
// HU12 — el fisioterapeuta visualiza el progreso y la evolución del
// paciente (CA01-CA03); se actualiza sola con cada sesión nueva por ser
// un Flow en vivo (CA04).
@HiltViewModel
class PacienteDetalleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val usuarioRepository: UsuarioRepository,
    private val sesionRepository: SesionRepository,
    private val ejercicioRepository: EjercicioRepository,
) : ViewModel() {

    val pacienteId: String = checkNotNull(savedStateHandle[Rutas.ARG_PACIENTE_ID])

    private val _uiState = MutableStateFlow(PacienteDetalleUiState())
    val uiState: StateFlow<PacienteDetalleUiState> = _uiState

    init {
        cargarPaciente()
        observarSesiones()
        observarEjercicios()
    }

    private fun cargarPaciente() {
        viewModelScope.launch {
            val paciente = usuarioRepository.obtenerUsuario(pacienteId)
            _uiState.update { it.copy(paciente = paciente, cargando = false) }
        }
    }

    // HU01-CA06 — el fisioterapeuta elige el diagnóstico del paciente de un
    // catálogo cerrado (TipoDiagnostico), no texto libre.
    fun actualizarDiagnostico(tipoDiagnostico: TipoDiagnostico) {
        viewModelScope.launch {
            usuarioRepository.actualizarDiagnostico(pacienteId, tipoDiagnostico).onSuccess {
                _uiState.update { estado ->
                    estado.copy(paciente = estado.paciente?.copy(tipoDiagnostico = tipoDiagnostico))
                }
            }
        }
    }

    fun onFiltroPeriodoCambiado(periodo: PeriodoFiltro) {
        _uiState.update { it.copy(filtroPeriodo = periodo) }
    }

    private fun observarSesiones() {
        val fisioterapeutaId = authRepository.uidActual ?: return
        viewModelScope.launch {
            sesionRepository.observarSesionesDe(pacienteId, fisioterapeutaId)
                // Evita que un PERMISSION_DENIED por cierre de sesión mientras
                // esta pantalla sigue activa tumbe la app (excepción no atrapada).
                .catch { }
                .collect { lista ->
                    _uiState.update { it.copy(sesiones = lista) }
                }
        }
    }

    private fun observarEjercicios() {
        viewModelScope.launch {
            ejercicioRepository.observarEjercicios()
                .catch { }
                .collect { lista ->
                    _uiState.update { it.copy(ejerciciosPorId = lista.associateBy { ejercicio -> ejercicio.id }) }
                }
        }
    }
}
