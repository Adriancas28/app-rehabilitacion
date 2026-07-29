package com.sanna.rehabapp.feature.ejercicios

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.core.navigation.Rutas
import com.sanna.rehabapp.domain.model.Articulacion
import com.sanna.rehabapp.domain.model.Ejercicio
import com.sanna.rehabapp.domain.model.PatronReferencia
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

// Fila editable del formulario — una por articulación con su ROM esperado.
data class PatronReferenciaFila(
    val idFila: String = UUID.randomUUID().toString(),
    val articulacion: Articulacion? = null,
    val anguloMin: String = "",
    val anguloMax: String = "",
)

data class EjercicioFormUiState(
    val nombre: String = "",
    val descripcion: String = "",
    val categoria: String = "",
    val duracionSegundos: String = "30",
    val patronesReferencia: List<PatronReferenciaFila> = emptyList(),
    val materialUrlActual: String = "",
    val archivoSeleccionado: Uri? = null,
    val cargando: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null,
    val guardadoExitoso: Boolean = false,
)

// HU02 — registrar (CA01, CA02), asociar material (CA03) y modificar (CA05)
// un ejercicio terapéutico. HU08-CA01/CA02: un ejercicio puede tener varias
// articulaciones, cada una con su propio ROM de referencia.
@HiltViewModel
class EjercicioFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val ejercicioRepository: EjercicioRepository,
) : ViewModel() {

    private val ejercicioIdArg: String? = savedStateHandle[Rutas.ARG_EJERCICIO_ID]
    val esEdicion: Boolean get() = !ejercicioIdArg.isNullOrBlank()

    private var creadoPorOriginal: String? = null
    private var fechaCreacionOriginal: Date? = null

    private val _uiState = MutableStateFlow(EjercicioFormUiState())
    val uiState: StateFlow<EjercicioFormUiState> = _uiState

    init {
        if (esEdicion) cargarEjercicio(ejercicioIdArg!!)
    }

    private fun cargarEjercicio(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            val ejercicio = ejercicioRepository.obtenerEjercicio(id)
            if (ejercicio != null) {
                creadoPorOriginal = ejercicio.creadoPor
                fechaCreacionOriginal = ejercicio.fechaCreacion
                _uiState.update {
                    it.copy(
                        nombre = ejercicio.nombre,
                        descripcion = ejercicio.descripcion,
                        categoria = ejercicio.categoria,
                        duracionSegundos = ejercicio.duracionSegundos.toString(),
                        patronesReferencia = ejercicio.patronesReferencia.map { patron ->
                            PatronReferenciaFila(
                                articulacion = patron.articulacion,
                                anguloMin = patron.anguloMin.toString(),
                                anguloMax = patron.anguloMax.toString(),
                            )
                        },
                        materialUrlActual = ejercicio.materialUrl,
                        cargando = false,
                    )
                }
            } else {
                _uiState.update { it.copy(cargando = false) }
            }
        }
    }

    fun onNombreCambiado(valor: String) = _uiState.update { it.copy(nombre = valor, error = null) }
    fun onDescripcionCambiada(valor: String) = _uiState.update { it.copy(descripcion = valor, error = null) }
    fun onCategoriaCambiada(valor: String) = _uiState.update { it.copy(categoria = valor, error = null) }
    fun onDuracionCambiada(valor: String) = _uiState.update { it.copy(duracionSegundos = valor, error = null) }
    fun onArchivoSeleccionado(uri: Uri?) = _uiState.update { it.copy(archivoSeleccionado = uri) }

    fun agregarArticulacion() {
        _uiState.update { it.copy(patronesReferencia = it.patronesReferencia + PatronReferenciaFila()) }
    }

    fun eliminarArticulacion(idFila: String) {
        _uiState.update { it.copy(patronesReferencia = it.patronesReferencia.filterNot { fila -> fila.idFila == idFila }) }
    }

    fun onArticulacionCambiada(idFila: String, valor: Articulacion) = actualizarFila(idFila) { it.copy(articulacion = valor) }
    fun onAnguloMinCambiado(idFila: String, valor: String) = actualizarFila(idFila) { it.copy(anguloMin = valor) }
    fun onAnguloMaxCambiado(idFila: String, valor: String) = actualizarFila(idFila) { it.copy(anguloMax = valor) }

    private fun actualizarFila(idFila: String, transformar: (PatronReferenciaFila) -> PatronReferenciaFila) {
        _uiState.update { estado ->
            estado.copy(
                patronesReferencia = estado.patronesReferencia.map { fila ->
                    if (fila.idFila == idFila) transformar(fila) else fila
                },
            )
        }
    }

    fun guardar() {
        val estado = _uiState.value
        if (estado.nombre.isBlank() || estado.descripcion.isBlank() || estado.categoria.isBlank()) {
            _uiState.update { it.copy(error = "Completa nombre, descripción y categoría.") }
            return
        }
        val uid = authRepository.uidActual
        if (uid == null) {
            _uiState.update { it.copy(error = "Sesión inválida, vuelve a iniciar sesión.") }
            return
        }
        val duracion = estado.duracionSegundos.toIntOrNull()
        if (duracion == null || duracion <= 0) {
            _uiState.update { it.copy(error = "La duración debe ser un número de segundos mayor a cero.") }
            return
        }

        // Filas incompletas (sin articulación o sin ambos ángulos) se ignoran
        // en silencio: el ROM es opcional en HU02, se puede completar después.
        val patrones = estado.patronesReferencia.mapNotNull { fila ->
            val articulacion = fila.articulacion ?: return@mapNotNull null
            val min = fila.anguloMin.toFloatOrNull() ?: return@mapNotNull null
            val max = fila.anguloMax.toFloatOrNull() ?: return@mapNotNull null
            PatronReferencia(articulacion = articulacion, anguloMin = min, anguloMax = max)
        }

        val ejercicio = Ejercicio(
            id = ejercicioIdArg ?: "",
            nombre = estado.nombre.trim(),
            descripcion = estado.descripcion.trim(),
            categoria = estado.categoria.trim(),
            materialUrl = estado.materialUrlActual,
            duracionSegundos = duracion,
            patronesReferencia = patrones,
            creadoPor = creadoPorOriginal ?: uid,
            fechaCreacion = fechaCreacionOriginal,
        )

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, error = null) }
            ejercicioRepository.guardarEjercicio(ejercicio, estado.archivoSeleccionado).fold(
                onSuccess = { _uiState.update { it.copy(guardando = false, guardadoExitoso = true) } },
                onFailure = {
                    _uiState.update {
                        it.copy(guardando = false, error = "No se pudo guardar el ejercicio.")
                    }
                },
            )
        }
    }
}
