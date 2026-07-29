package com.sanna.rehabapp.feature.paciente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.model.EstadoSesion
import com.sanna.rehabapp.domain.model.Sesion
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import com.sanna.rehabapp.domain.repository.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// HU11/HU13: la sesión completada junto a su ejercicio ya resuelto, para
// mostrar el nombre real en la lista y poder abrir su detalle de resultado.
data class SesionCompletada(val sesionId: String, val sesion: Sesion, val ejercicio: Ejercicio)

// HU12-CA03: rango para filtrar el historial y recalcular el resumen —
// mismo criterio que el filtro de ResultadosScreen (fisio).
enum class PeriodoProgreso(val etiqueta: String) {
    TODOS("Todo"),
    ULTIMA_SEMANA("Última semana"),
    ULTIMO_MES("Último mes"),
}

// HU12-CA01/CA02: resumen agregado sobre las sesiones que cumplen el
// filtro actual — % general, cuántas se completaron y la racha de días
// consecutivos con al menos una sesión.
data class ResumenProgreso(
    val porcentajeGeneral: Float = 0f,
    val sesionesCompletadas: Int = 0,
    val rachaActual: Int = 0,
)

data class HistorialSesionesUiState(
    val sesiones: List<SesionCompletada> = emptyList(),
    val resumen: ResumenProgreso = ResumenProgreso(),
    val filtroPeriodo: PeriodoProgreso = PeriodoProgreso.TODOS,
    val cargando: Boolean = true,
)

// HU13 — el paciente consulta su historial de sesiones ya realizadas,
// de la más reciente a la más antigua (CA03), con mensaje de ausencia si
// todavía no completó ninguna (CA04).
// HU12 — además ve un resumen de su progreso (% general, racha) y puede
// filtrar por período (CA01-CA03); se actualiza solo con cada sesión
// nueva por ser un Flow en vivo (CA04).
@HiltViewModel
class HistorialSesionesViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sesionRepository: SesionRepository,
    private val ejercicioRepository: EjercicioRepository,
) : ViewModel() {

    private val filtroPeriodo = MutableStateFlow(PeriodoProgreso.TODOS)

    private val _uiState = MutableStateFlow(HistorialSesionesUiState())
    val uiState: StateFlow<HistorialSesionesUiState> = _uiState

    init {
        observarHistorial()
    }

    fun onFiltroPeriodoCambiado(periodo: PeriodoProgreso) {
        filtroPeriodo.value = periodo
    }

    private fun observarHistorial() {
        val pacienteId = authRepository.uidActual
        if (pacienteId == null) {
            _uiState.update { it.copy(cargando = false) }
            return
        }
        viewModelScope.launch {
            combine(
                sesionRepository.observarSesionesDe(pacienteId),
                ejercicioRepository.observarEjercicios(),
                filtroPeriodo,
            ) { sesiones, ejercicios, periodo ->
                val ejerciciosPorId = ejercicios.associateBy { it.id }
                val completadas = sesiones
                    .filter { it.estado == EstadoSesion.COMPLETADA }
                    .filter { cumplePeriodo(it.fechaEjecucion, periodo) }
                    .mapNotNull { sesion ->
                        ejerciciosPorId[sesion.ejercicioId]?.let { ejercicio ->
                            SesionCompletada(sesionId = sesion.id, sesion = sesion, ejercicio = ejercicio)
                        }
                    }
                val resumen = ResumenProgreso(
                    porcentajeGeneral = completadas
                        .mapNotNull { it.sesion.resultado?.porcentajeEjecucion }
                        .let { valores -> if (valores.isEmpty()) 0f else valores.average().toFloat() },
                    sesionesCompletadas = completadas.size,
                    rachaActual = calcularRacha(completadas.mapNotNull { it.sesion.fechaEjecucion }),
                )
                Triple(completadas, resumen, periodo)
            }
                .catch { }
                .collect { (lista, resumen, periodo) ->
                    _uiState.update {
                        it.copy(sesiones = lista, resumen = resumen, filtroPeriodo = periodo, cargando = false)
                    }
                }
        }
    }
}

private fun cumplePeriodo(fecha: Date?, periodo: PeriodoProgreso): Boolean {
    if (periodo == PeriodoProgreso.TODOS || fecha == null) return true
    val dias = if (periodo == PeriodoProgreso.ULTIMA_SEMANA) 7 else 30
    val limite = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -dias) }.time
    return fecha.after(limite)
}
