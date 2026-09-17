package com.sanna.rehabapp.feature.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.core.navigation.Rutas
import com.sanna.rehabapp.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Dato estático de demostración -- por el momento no hay datos reales de
// sesiones ejecutadas por el paciente en Firestore para este dashboard
// (es distinto del resultado de sesión ya existente en el resto de la
// app), así que se muestra igual para cualquier paciente seleccionado,
// tal como el mockup, hasta que exista una fuente de datos real.
data class SesionDashboard(
    val numero: Int,
    val ejercicio: String,
    val fecha: String,
    val porcentajeCorrectas: Int,
    val porcentajeCompletado: Int,
)

val SESIONES_DEMO = listOf(
    SesionDashboard(1, "Flexión de rodilla", "12/05/2024", 80, 100),
    SesionDashboard(2, "Flexión de rodilla", "14/05/2024", 88, 100),
    SesionDashboard(3, "Sentadilla asistida", "17/05/2024", 65, 75),
)

data class AdminPacienteDashboardUiState(
    val nombre: String = "",
    val subtitulo: String = "",
    val cargando: Boolean = true,
    val mostrandoGrafico: Boolean = false,
    val sesiones: List<SesionDashboard> = SESIONES_DEMO,
    // Repeticiones correctas/con error acumuladas de la demo (mockup) --
    // no se derivan de los porcentajes de arriba porque el mockup las
    // muestra como conteo aparte, no como promedio.
    val repeticionesCorrectas: Int = 27,
    val repeticionesErrores: Int = 8,
) {
    val sesionesEjecutadas: Int get() = sesiones.size
    val precisionPromedio: Int get() =
        if (sesiones.isEmpty()) 0 else sesiones.sumOf { it.porcentajeCorrectas } / sesiones.size
}

@HiltViewModel
class AdminPacienteDashboardViewModel @Inject constructor(
    private val usuarioRepository: UsuarioRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val pacienteId: String = checkNotNull(savedStateHandle[Rutas.ARG_ADMIN_PACIENTE_ID])
    private val nombreInicial: String = savedStateHandle[Rutas.ARG_ADMIN_PACIENTE_NOMBRE] ?: ""

    private val _uiState = MutableStateFlow(AdminPacienteDashboardUiState(nombre = nombreInicial))
    val uiState: StateFlow<AdminPacienteDashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val usuario = usuarioRepository.obtenerUsuario(pacienteId)
            val diagnostico = usuario?.diagnosticos?.firstOrNull()?.tipo?.etiqueta
            val estado = if (usuario?.activo != false) "Activo" else "Inactivo"
            _uiState.value = _uiState.value.copy(
                nombre = usuario?.nombre ?: nombreInicial,
                subtitulo = listOfNotNull(diagnostico, estado).joinToString(" · "),
                cargando = false,
            )
        }
    }

    fun onAlternarVista() {
        _uiState.value = _uiState.value.copy(mostrandoGrafico = !_uiState.value.mostrandoGrafico)
    }
}
