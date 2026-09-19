package com.sanna.rehabapp.feature.pacientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.domain.model.Usuario
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class PeriodoFiltro(val etiqueta: String) {
    TODOS("Todos"),
    ULTIMA_SEMANA("Última semana"),
    ULTIMO_MES("Último mes"),
}

data class ResultadosUiState(
    val pacientes: List<Usuario> = emptyList(),
    val cargando: Boolean = true,
)

// HU18-CA01 — la pestaña "Resultados" del fisioterapeuta abre primero la
// lista de SUS pacientes; al elegir uno se ven todas sus sesiones
// (ResultadosPacienteScreen) y de ahí el detalle de cada una.
@HiltViewModel
class ResultadosViewModel @Inject constructor(
    authRepository: AuthRepository,
    usuarioRepository: UsuarioRepository,
) : ViewModel() {

    val uiState: StateFlow<ResultadosUiState> = run {
        val idFisio = authRepository.uidActual
            ?: return@run MutableStateFlow(ResultadosUiState(cargando = false))
        usuarioRepository.observarPacientesDe(idFisio)
            .map { pacientes -> ResultadosUiState(pacientes = pacientes.sortedBy { it.nombre }, cargando = false) }
            .catch { emit(ResultadosUiState(cargando = false)) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ResultadosUiState(),
            )
    }
}

// No es private: PacienteDetalleViewModel (HU12) y ResultadosPacienteViewModel
// reusan el mismo criterio de filtro por período.
fun cumplePeriodo(fecha: Date?, periodo: PeriodoFiltro): Boolean {
    if (periodo == PeriodoFiltro.TODOS || fecha == null) return true
    val dias = if (periodo == PeriodoFiltro.ULTIMA_SEMANA) 7 else 30
    val limite = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -dias) }.time
    return fecha.after(limite)
}
