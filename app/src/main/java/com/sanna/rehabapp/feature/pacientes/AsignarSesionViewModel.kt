package com.sanna.rehabapp.feature.pacientes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.core.navigation.Rutas
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.model.TipoDiagnostico
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// HU03-CA06: opciones fijas del selector de repeticiones al asignar/editar
// una sesión (no texto libre: mismo criterio que Articulacion/TipoDiagnostico).
val OPCIONES_REPETICIONES = listOf(1, 3, 6, 9, 12)

data class AsignarSesionUiState(
    val ejercicios: List<Ejercicio> = emptyList(),
    // HU03-CA05 (ampliación): diagnósticos del paciente, para resaltar
    // primero los ejercicios sugeridos al elegir uno.
    val diagnosticosPaciente: List<TipoDiagnostico> = emptyList(),
    val ejercicioSeleccionadoId: String? = null,
    val fechaAsignacion: Date? = null,
    val notas: String = "",
    val repeticiones: Int? = null,
    // HU03-CA06 (ampliacion): override de la duracion por repeticion,
    // como texto (mismo patron que "edad" en el formulario de paciente)
    // para permitir edicion libre en segundos -- a diferencia de
    // repeticiones, que usa un selector de opciones fijas.
    val duracionSegundosTexto: String = "",
    // HU03 (ampliación, Etapa 4): personalizar el ángulo objetivo (min/max)
    // solo para esta sesión/paciente. `personalizarAngulo` es el checkbox;
    // los textos solo importan si está marcado -- mismo patrón texto libre
    // que duracionSegundosTexto.
    val personalizarAngulo: Boolean = false,
    val anguloMinTexto: String = "",
    val anguloMaxTexto: String = "",
    val cargando: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null,
    val guardadoExitoso: Boolean = false,
) {
    // Los ejercicios sugeridos para el/los diagnóstico(s) del paciente van
    // primero, sin excluir al resto del catálogo (orden estable).
    val ejerciciosOrdenados: List<Ejercicio>
        get() = ejercicios.sortedByDescending { esSugerido(it) }

    fun esSugerido(ejercicio: Ejercicio): Boolean =
        diagnosticosPaciente.isNotEmpty() && ejercicio.diagnosticosAplicables.any { it in diagnosticosPaciente }
}

// HU03 — asignar (CA01, CA02) o editar (CA03) una sesión terapéutica.
@HiltViewModel
class AsignarSesionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val ejercicioRepository: EjercicioRepository,
    private val sesionRepository: SesionRepository,
    private val usuarioRepository: UsuarioRepository,
) : ViewModel() {

    private val pacienteId: String = checkNotNull(savedStateHandle[Rutas.ARG_PACIENTE_ID])
    private val sesionIdArg: String? = savedStateHandle[Rutas.ARG_SESION_ID]
    val esEdicion: Boolean get() = !sesionIdArg.isNullOrBlank()

    private val _uiState = MutableStateFlow(AsignarSesionUiState())
    val uiState: StateFlow<AsignarSesionUiState> = _uiState

    init {
        observarEjercicios()
        cargarDiagnosticosPaciente()
        if (esEdicion) cargarSesion(sesionIdArg!!)
    }

    private fun cargarDiagnosticosPaciente() {
        viewModelScope.launch {
            val paciente = usuarioRepository.obtenerUsuario(pacienteId)
            _uiState.update { it.copy(diagnosticosPaciente = paciente?.diagnosticos?.map { d -> d.tipo } ?: emptyList()) }
        }
    }

    private fun observarEjercicios() {
        viewModelScope.launch {
            ejercicioRepository.observarEjercicios()
                .catch { }
                .collect { lista ->
                    _uiState.update { it.copy(ejercicios = lista.filter { ejercicio -> ejercicio.activo }) }
                }
        }
    }

    private fun cargarSesion(sesionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            val sesion = sesionRepository.obtenerSesion(pacienteId, sesionId)
            _uiState.update {
                if (sesion != null) {
                    it.copy(
                        ejercicioSeleccionadoId = sesion.ejercicioId,
                        fechaAsignacion = sesion.fechaAsignacion,
                        notas = sesion.notas ?: "",
                        repeticiones = sesion.repeticiones,
                        duracionSegundosTexto = sesion.duracionSegundos?.toString() ?: "",
                        personalizarAngulo = sesion.anguloMinOverride != null && sesion.anguloMaxOverride != null,
                        anguloMinTexto = sesion.anguloMinOverride?.toString() ?: "",
                        anguloMaxTexto = sesion.anguloMaxOverride?.toString() ?: "",
                        cargando = false,
                    )
                } else {
                    it.copy(cargando = false, error = "No se encontró la sesión.")
                }
            }
        }
    }

    // HU03-CA06 — al elegir un ejercicio se precarga, dentro de las
    // opciones fijas del selector, la más cercana a su valor por defecto.
    fun onEjercicioSeleccionado(ejercicioId: String) {
        val ejercicio = _uiState.value.ejercicios.find { it.id == ejercicioId }
        _uiState.update {
            it.copy(
                ejercicioSeleccionadoId = ejercicioId,
                repeticiones = ejercicio?.let { seleccionado -> valorMasCercano(seleccionado.repeticiones) }
                    ?: it.repeticiones,
                duracionSegundosTexto = ejercicio?.duracionSegundos?.toString() ?: it.duracionSegundosTexto,
                anguloMinTexto = ejercicio?.patronesReferencia?.firstOrNull()?.anguloMin?.toString()
                    ?: it.anguloMinTexto,
                anguloMaxTexto = ejercicio?.patronesReferencia?.firstOrNull()?.anguloMax?.toString()
                    ?: it.anguloMaxTexto,
                error = null,
            )
        }
    }

    fun onPersonalizarAnguloCambiado(valor: Boolean) = _uiState.update { it.copy(personalizarAngulo = valor) }

    fun onAnguloMinCambiado(valor: String) = _uiState.update { it.copy(anguloMinTexto = valor) }

    fun onAnguloMaxCambiado(valor: String) = _uiState.update { it.copy(anguloMaxTexto = valor) }

    fun onFechaSeleccionada(fecha: Date) =
        _uiState.update { it.copy(fechaAsignacion = fecha, error = null) }

    fun onNotasCambiadas(valor: String) = _uiState.update { it.copy(notas = valor) }

    fun onRepeticionesCambiadas(valor: Int) = _uiState.update { it.copy(repeticiones = valor) }

    fun onDuracionSegundosCambiada(valor: String) = _uiState.update { it.copy(duracionSegundosTexto = valor) }

    fun guardar() {
        if (_uiState.value.guardando) return
        val estado = _uiState.value
        val ejercicioId = estado.ejercicioSeleccionadoId
        val fecha = estado.fechaAsignacion
        if (ejercicioId == null || fecha == null) {
            _uiState.update { it.copy(error = "Selecciona un ejercicio y una fecha.") }
            return
        }
        // ERR-FIS-001/003: duración y repeticiones deben ser enteros > 0.
        val duracionSegundos = estado.duracionSegundosTexto.trim().toIntOrNull()
        if (duracionSegundos == null || duracionSegundos <= 0) {
            _uiState.update { it.copy(error = "La duración por repetición debe ser un número entero mayor que 0.") }
            return
        }
        val repeticiones = estado.repeticiones
        if (repeticiones == null || repeticiones <= 0) {
            _uiState.update { it.copy(error = "Las repeticiones deben ser un número entero mayor que 0.") }
            return
        }
        val notas = estado.notas.trim().ifBlank { null }
        // HU03 (ampliación, Etapa 4): el override de ángulo solo se envía si el
        // checkbox está marcado. ERR-FIS-002/003: en ese caso mínimo y máximo
        // son obligatorios, numéricos, y el mínimo debe ser menor que el máximo.
        var anguloMinOverride: Float? = null
        var anguloMaxOverride: Float? = null
        if (estado.personalizarAngulo) {
            val minimo = estado.anguloMinTexto.trim().toFloatOrNull()?.takeIf { it.isFinite() }
            val maximo = estado.anguloMaxTexto.trim().toFloatOrNull()?.takeIf { it.isFinite() }
            val mensaje = when {
                minimo == null || maximo == null ->
                    "Con \"Personalizar ángulo\" activado, ingresa un ángulo mínimo y un máximo válidos."
                minimo < 0f || maximo > 180f -> "Los ángulos deben estar entre 0° y 180°."
                minimo >= maximo -> "El ángulo mínimo debe ser menor que el máximo."
                else -> null
            }
            if (mensaje != null) {
                _uiState.update { it.copy(error = mensaje) }
                return
            }
            anguloMinOverride = minimo
            anguloMaxOverride = maximo
        }
        // Se marca de forma síncrona: protege contra doble toque.
        _uiState.update { it.copy(guardando = true, error = null) }

        viewModelScope.launch {
            val resultado = if (esEdicion) {
                sesionRepository.actualizarSesion(
                    pacienteId,
                    sesionIdArg!!,
                    ejercicioId,
                    fecha,
                    notas,
                    repeticiones,
                    duracionSegundos,
                    anguloMinOverride,
                    anguloMaxOverride,
                )
            } else {
                val fisioterapeutaId = authRepository.uidActual
                if (fisioterapeutaId == null) {
                    _uiState.update { it.copy(guardando = false, error = "Sesión inválida, vuelve a iniciar sesión.") }
                    return@launch
                }
                sesionRepository.asignarSesion(
                    pacienteId,
                    ejercicioId,
                    fisioterapeutaId,
                    fecha,
                    notas,
                    repeticiones,
                    duracionSegundos,
                    anguloMinOverride,
                    anguloMaxOverride,
                )
            }
            resultado.fold(
                onSuccess = { _uiState.update { it.copy(guardando = false, guardadoExitoso = true) } },
                onFailure = {
                    _uiState.update { it.copy(guardando = false, error = "No se pudo guardar la sesión.") }
                },
            )
        }
    }
}

private fun valorMasCercano(valor: Int): Int =
    OPCIONES_REPETICIONES.minByOrNull { kotlin.math.abs(it - valor) } ?: OPCIONES_REPETICIONES.first()
