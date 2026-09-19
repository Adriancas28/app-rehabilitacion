package com.sanna.rehabapp.feature.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.core.navigation.Rutas
import com.sanna.rehabapp.domain.model.EstadoSesion
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import com.sanna.rehabapp.domain.repository.SesionRepository
import com.sanna.rehabapp.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SesionDashboard(
    val numero: Int,
    val ejercicio: String,
    val fecha: String,
    // % de precisión de la sesión (porcentajeEjecucion del resultado).
    val porcentajeCorrectas: Int,
    // Repeticiones completadas sobre las asignadas.
    val porcentajeCompletado: Int,
)

data class AdminPacienteDashboardUiState(
    val nombre: String = "",
    val subtitulo: String = "",
    val cargando: Boolean = true,
    val mostrandoGrafico: Boolean = false,
    // Solo sesiones ejecutadas (con resultado), la más antigua primero: las
    // pendientes son planeadas, no ejecutadas.
    val sesiones: List<SesionDashboard> = emptyList(),
) {
    val sesionesEjecutadas: Int get() = sesiones.size
    val precisionPromedio: Int get() =
        if (sesiones.isEmpty()) 0 else Math.round(sesiones.map { it.porcentajeCorrectas }.average()).toInt()
}

// Etapa 2A (dashboard Admin) — progreso de UN paciente a partir de sus
// sesiones reales en Firestore (el admin puede leerlas por esAdmin() en las
// Security Rules): cuántas ejecutó, precisión promedio (promedio de los % de
// cada sesión) y el detalle por sesión.
@HiltViewModel
class AdminPacienteDashboardViewModel @Inject constructor(
    private val usuarioRepository: UsuarioRepository,
    sesionRepository: SesionRepository,
    ejercicioRepository: EjercicioRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val pacienteId: String = checkNotNull(savedStateHandle[Rutas.ARG_ADMIN_PACIENTE_ID])
    private val nombreInicial: String = savedStateHandle[Rutas.ARG_ADMIN_PACIENTE_NOMBRE] ?: ""

    private val cabecera = MutableStateFlow(nombreInicial to "")
    private val mostrandoGrafico = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val usuario = usuarioRepository.obtenerUsuario(pacienteId)
            val diagnostico = usuario?.diagnosticos?.firstOrNull()?.tipo?.etiqueta
            val estado = if (usuario?.activo != false) "Activo" else "Inactivo"
            cabecera.value = (usuario?.nombre ?: nombreInicial) to listOfNotNull(diagnostico, estado).joinToString(" · ")
        }
    }

    val uiState: StateFlow<AdminPacienteDashboardUiState> = combine(
        sesionRepository.observarSesionesDe(pacienteId),
        ejercicioRepository.observarEjercicios(),
        cabecera,
        mostrandoGrafico,
    ) { sesiones, ejercicios, (nombre, subtitulo), grafico ->
        val nombres = ejercicios.associate { it.id to it.nombre }
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val ejecutadas = sesiones
            .filter { it.estado == EstadoSesion.COMPLETADA && it.resultado != null }
            .sortedBy { it.fechaEjecucion ?: it.fechaAsignacion }
            .mapIndexed { indice, sesion ->
                val resultado = sesion.resultado!!
                SesionDashboard(
                    numero = indice + 1,
                    ejercicio = nombres[sesion.ejercicioId] ?: "Ejercicio",
                    fecha = (sesion.fechaEjecucion ?: sesion.fechaAsignacion)?.let(formato::format) ?: "—",
                    porcentajeCorrectas = Math.round(resultado.porcentajeEjecucion),
                    porcentajeCompletado = if (resultado.repeticionesAsignadas > 0) {
                        Math.round(resultado.repeticionesCompletadas * 100f / resultado.repeticionesAsignadas)
                    } else {
                        0
                    },
                )
            }
        AdminPacienteDashboardUiState(
            nombre = nombre,
            subtitulo = subtitulo,
            cargando = false,
            mostrandoGrafico = grafico,
            sesiones = ejecutadas,
        )
    }
        .catch { emit(AdminPacienteDashboardUiState(nombre = nombreInicial, cargando = false)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AdminPacienteDashboardUiState(nombre = nombreInicial),
        )

    fun onAlternarVista() {
        mostrandoGrafico.value = !mostrandoGrafico.value
    }
}
