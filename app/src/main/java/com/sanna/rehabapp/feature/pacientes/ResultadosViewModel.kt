package com.sanna.rehabapp.feature.pacientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.model.EstadoSesion
import com.sanna.rehabapp.domain.model.Sesion
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import com.sanna.rehabapp.domain.repository.SesionRepository
import com.sanna.rehabapp.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

// HU18-CA02 (Sprint 4) — el detalle de una sesión puntual ya se resuelve
// en FisioResultadoSesionScreen; aquí solo se necesita lo suficiente para
// la fila de la lista agregada.
data class SesionConDetalle(
    val sesion: Sesion,
    val nombrePaciente: String,
    val ejercicio: Ejercicio?,
)

enum class PeriodoFiltro(val etiqueta: String) {
    TODOS("Todos"),
    ULTIMA_SEMANA("Última semana"),
    ULTIMO_MES("Último mes"),
}

data class ResultadosUiState(
    val sesiones: List<SesionConDetalle> = emptyList(),
    val ejerciciosDisponibles: List<Ejercicio> = emptyList(),
    val filtroEjercicioId: String? = null,
    val filtroPeriodo: PeriodoFiltro = PeriodoFiltro.TODOS,
    val cargando: Boolean = true,
)

// HU18-CA01/CA03 — el fisioterapeuta consulta todas las sesiones y
// resultados registrados de sus pacientes (no de uno puntual, ver
// PacienteDetalleScreen para eso), con filtro por fecha o ejercicio.
@HiltViewModel
class ResultadosViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sesionRepository: SesionRepository,
    private val usuarioRepository: UsuarioRepository,
    private val ejercicioRepository: EjercicioRepository,
) : ViewModel() {

    private val filtroEjercicioId = MutableStateFlow<String?>(null)
    private val filtroPeriodo = MutableStateFlow(PeriodoFiltro.TODOS)

    private val fisioterapeutaId = authRepository.uidActual

    val uiState: StateFlow<ResultadosUiState> = run {
        val idFisio = fisioterapeutaId
        if (idFisio == null) {
            return@run MutableStateFlow(ResultadosUiState(cargando = false))
        }
        combine(
            sesionRepository.observarTodasLasSesionesDe(idFisio),
            usuarioRepository.observarPacientesDe(idFisio),
            ejercicioRepository.observarEjercicios(),
            filtroEjercicioId,
            filtroPeriodo,
        ) { sesiones, pacientes, ejercicios, filtroEjercicio, periodo ->
            val nombresPorId = pacientes.associate { it.uid to it.nombre }
            val ejerciciosPorId = ejercicios.associateBy { it.id }
            val sesionesFiltradas = sesiones
                .filter { it.estado == EstadoSesion.COMPLETADA }
                .filter { filtroEjercicio == null || it.ejercicioId == filtroEjercicio }
                .filter { cumplePeriodo(it.fechaAsignacion, periodo) }
                .map { sesion ->
                    SesionConDetalle(
                        sesion = sesion,
                        nombrePaciente = sesion.pacienteId?.let { nombresPorId[it] } ?: "Paciente",
                        ejercicio = ejerciciosPorId[sesion.ejercicioId],
                    )
                }
            ResultadosUiState(
                sesiones = sesionesFiltradas,
                ejerciciosDisponibles = ejercicios,
                filtroEjercicioId = filtroEjercicio,
                filtroPeriodo = periodo,
                cargando = false,
            )
        }
            .catch { }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ResultadosUiState(),
            )
    }

    fun onFiltroEjercicioCambiado(ejercicioId: String?) {
        filtroEjercicioId.value = ejercicioId
    }

    fun onFiltroPeriodoCambiado(periodo: PeriodoFiltro) {
        filtroPeriodo.value = periodo
    }
}

private fun cumplePeriodo(fecha: Date?, periodo: PeriodoFiltro): Boolean {
    if (periodo == PeriodoFiltro.TODOS || fecha == null) return true
    val dias = if (periodo == PeriodoFiltro.ULTIMA_SEMANA) 7 else 30
    val limite = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -dias) }.time
    return fecha.after(limite)
}
