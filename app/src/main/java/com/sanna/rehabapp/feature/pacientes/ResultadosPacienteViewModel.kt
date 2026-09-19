package com.sanna.rehabapp.feature.pacientes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.core.navigation.Rutas
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.model.EstadoSesion
import com.sanna.rehabapp.domain.model.Sesion
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import com.sanna.rehabapp.domain.repository.SesionRepository
import com.sanna.rehabapp.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class EstadoFila { COMPLETADA, INCOMPLETA, POR_HACER }

data class FilaSesion(
    val sesion: Sesion,
    val ejercicio: Ejercicio?,
    val estado: EstadoFila,
)

data class ResultadosPacienteUiState(
    val nombrePaciente: String = "",
    val filas: List<FilaSesion> = emptyList(),
    val ejerciciosDisponibles: List<Ejercicio> = emptyList(),
    val filtroEjercicioId: String? = null,
    val filtroPeriodo: PeriodoFiltro = PeriodoFiltro.TODOS,
    val cargando: Boolean = true,
)

// Todas las sesiones de UN paciente -- completas, incompletas (finalizadas
// antes de tiempo, con menos repeticiones de las asignadas) y por hacer --
// en una sola lista, con los filtros de HU18-CA03 (período y ejercicio).
@HiltViewModel
class ResultadosPacienteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    authRepository: AuthRepository,
    sesionRepository: SesionRepository,
    ejercicioRepository: EjercicioRepository,
    private val usuarioRepository: UsuarioRepository,
) : ViewModel() {

    private val pacienteId: String = checkNotNull(savedStateHandle[Rutas.ARG_PACIENTE_ID])

    private val nombrePaciente = MutableStateFlow("")
    private val filtroEjercicioId = MutableStateFlow<String?>(null)
    private val filtroPeriodo = MutableStateFlow(PeriodoFiltro.TODOS)

    init {
        viewModelScope.launch {
            nombrePaciente.value = usuarioRepository.obtenerUsuario(pacienteId)?.nombre.orEmpty()
        }
    }

    val uiState: StateFlow<ResultadosPacienteUiState> = combine(
        sesionRepository.observarSesionesDe(pacienteId, authRepository.uidActual),
        ejercicioRepository.observarEjercicios(),
        nombrePaciente,
        filtroEjercicioId,
        filtroPeriodo,
    ) { sesiones, ejercicios, nombre, filtroEjercicio, periodo ->
        val ejerciciosPorId = ejercicios.associateBy { it.id }
        val filas = sesiones
            .filter { filtroEjercicio == null || it.ejercicioId == filtroEjercicio }
            .filter { cumplePeriodo(it.fechaEjecucion ?: it.fechaAsignacion, periodo) }
            .map { FilaSesion(it, ejerciciosPorId[it.ejercicioId], estadoDe(it)) }
        ResultadosPacienteUiState(
            nombrePaciente = nombre,
            filas = filas,
            ejerciciosDisponibles = ejercicios,
            filtroEjercicioId = filtroEjercicio,
            filtroPeriodo = periodo,
            cargando = false,
        )
    }
        .catch { emit(ResultadosPacienteUiState(cargando = false)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ResultadosPacienteUiState(),
        )

    fun onFiltroEjercicioCambiado(ejercicioId: String?) {
        filtroEjercicioId.value = ejercicioId
    }

    fun onFiltroPeriodoCambiado(periodo: PeriodoFiltro) {
        filtroPeriodo.value = periodo
    }

    private fun estadoDe(sesion: Sesion): EstadoFila {
        val resultado = sesion.resultado
        return when {
            sesion.estado == EstadoSesion.PENDIENTE && resultado == null -> EstadoFila.POR_HACER
            resultado != null && resultado.repeticionesAsignadas > 0 &&
                resultado.repeticionesCompletadas < resultado.repeticionesAsignadas -> EstadoFila.INCOMPLETA
            sesion.estado == EstadoSesion.COMPLETADA -> EstadoFila.COMPLETADA
            else -> EstadoFila.POR_HACER
        }
    }
}
