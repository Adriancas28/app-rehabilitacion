package com.sanna.rehabapp.feature.pacientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.domain.model.Usuario
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import com.sanna.rehabapp.domain.repository.SesionRepository
import com.sanna.rehabapp.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PacientesUiState(
    val pacientesFiltrados: List<Usuario> = emptyList(),
    val totalPacientes: Int = 0,
    // Dashboard del fisioterapeuta (ampliación visual, no HU propia): las
    // mismas métricas que ya expone HU01/HU02, solo agregadas en tarjetas.
    val sesionesHoy: Int = 0,
    val totalEjercicios: Int = 0,
    val consultaBusqueda: String = "",
    val cargando: Boolean = true,
)

// HU01 — gestionar pacientes asignados al fisioterapeuta autenticado.
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PacientesViewModel @Inject constructor(
    authRepository: AuthRepository,
    usuarioRepository: UsuarioRepository,
    sesionRepository: SesionRepository,
    ejercicioRepository: EjercicioRepository,
) : ViewModel() {

    private val consultaBusqueda = MutableStateFlow("")
    private val fisioterapeutaId = authRepository.uidActual

    private val pacientes = fisioterapeutaId
        ?.let { usuarioRepository.observarPacientesDe(it) }
        ?: flowOf(emptyList())

    // No hay una consulta agregada de "sesiones de todos mis pacientes hoy"
    // (requeriría una collection group query nueva); se arma combinando la
    // consulta por paciente que ya existe y usa HU01/HU12/HU14.
    private val sesionesHoy = pacientes.flatMapLatest { lista ->
        if (lista.isEmpty()) {
            flowOf(0)
        } else {
            combine(
                lista.map { paciente -> sesionRepository.observarSesionesDe(paciente.uid, fisioterapeutaId) },
            ) { listasDeSesiones ->
                listasDeSesiones.sumOf { sesiones -> sesiones.count { it.fechaAsignacion?.esHoy() == true } }
            }
        }
    }

    private val totalEjercicios = ejercicioRepository.observarEjercicios()
        .map { lista -> lista.count { it.activo } }
        .catch { emit(0) }

    val uiState: StateFlow<PacientesUiState> = combine(
        pacientes,
        consultaBusqueda,
        sesionesHoy,
        totalEjercicios,
    ) { lista, consulta, sesionesHoyCantidad, ejerciciosCantidad ->
        PacientesUiState(
            pacientesFiltrados = filtrarPacientes(lista, consulta),
            totalPacientes = lista.size,
            sesionesHoy = sesionesHoyCantidad,
            totalEjercicios = ejerciciosCantidad,
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

private fun Date.esHoy(): Boolean {
    val calendarFecha = Calendar.getInstance().apply { time = this@esHoy }
    val calendarHoy = Calendar.getInstance()
    return calendarFecha.get(Calendar.YEAR) == calendarHoy.get(Calendar.YEAR) &&
        calendarFecha.get(Calendar.DAY_OF_YEAR) == calendarHoy.get(Calendar.DAY_OF_YEAR)
}
