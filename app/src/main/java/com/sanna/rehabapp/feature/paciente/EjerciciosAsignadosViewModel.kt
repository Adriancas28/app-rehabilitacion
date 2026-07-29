package com.sanna.rehabapp.feature.paciente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.model.EstadoSesion
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import com.sanna.rehabapp.domain.repository.SesionRepository
import com.sanna.rehabapp.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// HU04-CA02 — el ejercicio ya resuelto (no solo su id) junto a la sesión
// que lo asignó, para poder abrir su detalle (HU04-CA03). fechaAsignacion y
// repeticiones vienen de la sesión (con el override de HU03-CA06 ya
// resuelto), para mostrarlos en la tarjeta de "próxima sesión".
data class EjercicioAsignado(
    val sesionId: String,
    val ejercicio: Ejercicio,
    val fechaAsignacion: Date?,
    val repeticiones: Int,
)

// HU06-CA09 — una sesión que se finalizó antes de tiempo (no completó
// todas sus repeticiones asignadas) y todavía se puede reanudar.
data class SesionReanudable(
    val sesionId: String,
    val ejercicio: Ejercicio,
    val repeticionesCompletadas: Int,
    val repeticionesAsignadas: Int,
)

data class EjerciciosAsignadosUiState(
    val nombrePaciente: String = "",
    val ejerciciosAsignados: List<EjercicioAsignado> = emptyList(),
    val sesionesReanudables: List<SesionReanudable> = emptyList(),
    val cargando: Boolean = true,
)

// HU04 — el paciente visualiza los ejercicios de sus sesiones pendientes.
@HiltViewModel
class EjerciciosAsignadosViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val usuarioRepository: UsuarioRepository,
    private val sesionRepository: SesionRepository,
    private val ejercicioRepository: EjercicioRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EjerciciosAsignadosUiState())
    val uiState: StateFlow<EjerciciosAsignadosUiState> = _uiState

    init {
        cargarPaciente()
        observarEjerciciosAsignados()
    }

    private fun cargarPaciente() {
        val pacienteId = authRepository.uidActual ?: return
        viewModelScope.launch {
            val paciente = usuarioRepository.obtenerUsuario(pacienteId)
            _uiState.update { it.copy(nombrePaciente = paciente?.nombre ?: "") }
        }
    }

    private fun observarEjerciciosAsignados() {
        val pacienteId = authRepository.uidActual
        if (pacienteId == null) {
            _uiState.update { it.copy(cargando = false) }
            return
        }
        viewModelScope.launch {
            combine(
                sesionRepository.observarSesionesDe(pacienteId),
                ejercicioRepository.observarEjercicios(),
            ) { sesiones, ejercicios ->
                val ejerciciosPorId = ejercicios.associateBy { it.id }
                val asignados = sesiones
                    .filter { it.estado == EstadoSesion.PENDIENTE }
                    .mapNotNull { sesion ->
                        ejerciciosPorId[sesion.ejercicioId]?.let { ejercicio ->
                            EjercicioAsignado(
                                sesionId = sesion.id,
                                ejercicio = ejercicio,
                                fechaAsignacion = sesion.fechaAsignacion,
                                repeticiones = sesion.repeticiones ?: ejercicio.repeticiones,
                            )
                        }
                    }
                // HU06-CA09 — sesiones finalizadas antes de tiempo que no
                // llegaron a completar todas las repeticiones asignadas.
                val reanudables = sesiones
                    .filter { it.estado == EstadoSesion.COMPLETADA }
                    .mapNotNull { sesion ->
                        val resultado = sesion.resultado ?: return@mapNotNull null
                        if (resultado.repeticionesCompletadas >= resultado.repeticionesAsignadas) return@mapNotNull null
                        ejerciciosPorId[sesion.ejercicioId]?.let { ejercicio ->
                            SesionReanudable(
                                sesionId = sesion.id,
                                ejercicio = ejercicio,
                                repeticionesCompletadas = resultado.repeticionesCompletadas,
                                repeticionesAsignadas = resultado.repeticionesAsignadas,
                            )
                        }
                    }
                asignados to reanudables
            }
                // Evita que un PERMISSION_DENIED por cierre de sesión mientras
                // esta pantalla sigue activa tumbe la app (excepción no atrapada).
                .catch { }
                .collect { (asignados, reanudables) ->
                    _uiState.update {
                        it.copy(ejerciciosAsignados = asignados, sesionesReanudables = reanudables, cargando = false)
                    }
                }
        }
    }
}
