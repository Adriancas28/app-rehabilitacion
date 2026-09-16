package com.sanna.rehabapp.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.domain.model.EstadoSesion
import com.sanna.rehabapp.domain.repository.AdminRepository
import com.sanna.rehabapp.domain.repository.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

// Etapa 2A (dashboard Admin) — una barra por fisioterapeuta, con su nombre
// y el % de adherencia (sesiones completadas / asignadas) de todos sus
// pacientes. GraficoBarras no muestra etiquetas, así que la lista ordenada
// se muestra aparte, en una tabla simple debajo del gráfico.
data class AdherenciaFisioterapeuta(
    val nombre: String,
    val porcentajeAdherencia: Float,
    val sesionesCompletadas: Int,
    val sesionesAsignadas: Int,
)

data class AdminDashboardUiState(
    val totalPacientes: Int = 0,
    val totalPacientesActivos: Int = 0,
    val totalFisioterapeutas: Int = 0,
    val totalFisioterapeutasActivos: Int = 0,
    val totalSesionesCompletadas: Int = 0,
    val totalSesionesAsignadas: Int = 0,
    val porcentajeAdherenciaGlobal: Float = 0f,
    val promedioCalidadEjecucion: Float = 0f,
    val adherenciaPorFisioterapeuta: List<AdherenciaFisioterapeuta> = emptyList(),
    val cargando: Boolean = true,
)

// Etapa 2A (dashboard Admin, ampliación acordada — no es una HU del
// backlog original, es refinamiento técnico sobre la Épica 07). Reutiliza
// AdminRepository (ya usado por HU20/HU21) y el nuevo
// SesionRepository.observarTodasLasSesiones() (sin filtro, solo accesible
// para el rol admin vía Firestore Security Rules).
@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val sesionRepository: SesionRepository,
) : ViewModel() {

    val uiState: StateFlow<AdminDashboardUiState> = combine(
        adminRepository.observarPacientes(),
        adminRepository.observarFisioterapeutas(),
        sesionRepository.observarTodasLasSesiones(),
    ) { pacientes, fisioterapeutas, sesiones ->
        val completadas = sesiones.filter { it.estado == EstadoSesion.COMPLETADA }
        val porcentajeGlobal = if (sesiones.isNotEmpty()) {
            completadas.size * 100f / sesiones.size
        } else {
            0f
        }
        val calidadPromedio = completadas
            .mapNotNull { it.resultado?.porcentajeEjecucion }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toFloat() ?: 0f

        val sesionesPorFisio = sesiones.groupBy { it.fisioterapeutaId }
        val adherencia = fisioterapeutas
            .map { fisio ->
                val sesionesDelFisio = sesionesPorFisio[fisio.uid].orEmpty()
                val completadasDelFisio = sesionesDelFisio.count { it.estado == EstadoSesion.COMPLETADA }
                AdherenciaFisioterapeuta(
                    nombre = fisio.nombre,
                    porcentajeAdherencia = if (sesionesDelFisio.isNotEmpty()) {
                        completadasDelFisio * 100f / sesionesDelFisio.size
                    } else {
                        0f
                    },
                    sesionesCompletadas = completadasDelFisio,
                    sesionesAsignadas = sesionesDelFisio.size,
                )
            }
            .filter { it.sesionesAsignadas > 0 }
            .sortedByDescending { it.porcentajeAdherencia }

        AdminDashboardUiState(
            totalPacientes = pacientes.size,
            totalPacientesActivos = pacientes.count { it.activo },
            totalFisioterapeutas = fisioterapeutas.size,
            totalFisioterapeutasActivos = fisioterapeutas.count { it.activo },
            totalSesionesCompletadas = completadas.size,
            totalSesionesAsignadas = sesiones.size,
            porcentajeAdherenciaGlobal = porcentajeGlobal,
            promedioCalidadEjecucion = calidadPromedio,
            adherenciaPorFisioterapeuta = adherencia,
            cargando = false,
        )
    }
        // Mismo criterio que AdminPacientesViewModel: evita que un
        // PERMISSION_DENIED por cierre de sesión tumbe la app.
        .catch { }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AdminDashboardUiState(),
        )
}
