package com.sanna.rehabapp.feature.ejercicios

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanna.rehabapp.core.navigation.Rutas
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
import javax.inject.Inject

data class EjercicioFormUiState(
    val nombre: String = "",
    val descripcion: String = "",
    val categoria: String = "",
    val anguloMin: String = "",
    val anguloMax: String = "",
    val materialUrlActual: String = "",
    val archivoSeleccionado: Uri? = null,
    val cargando: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null,
    val guardadoExitoso: Boolean = false,
)

// HU02 — registrar (CA01, CA02), asociar material (CA03) y modificar (CA05)
// un ejercicio terapéutico.
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
                        anguloMin = ejercicio.patronReferencia?.anguloMin?.toString() ?: "",
                        anguloMax = ejercicio.patronReferencia?.anguloMax?.toString() ?: "",
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
    fun onAnguloMinCambiado(valor: String) = _uiState.update { it.copy(anguloMin = valor) }
    fun onAnguloMaxCambiado(valor: String) = _uiState.update { it.copy(anguloMax = valor) }
    fun onArchivoSeleccionado(uri: Uri?) = _uiState.update { it.copy(archivoSeleccionado = uri) }

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
        val patron = if (estado.anguloMin.isNotBlank() && estado.anguloMax.isNotBlank()) {
            PatronReferencia(
                anguloMin = estado.anguloMin.toFloatOrNull() ?: 0f,
                anguloMax = estado.anguloMax.toFloatOrNull() ?: 0f,
            )
        } else {
            null
        }

        val ejercicio = Ejercicio(
            id = ejercicioIdArg ?: "",
            nombre = estado.nombre.trim(),
            descripcion = estado.descripcion.trim(),
            categoria = estado.categoria.trim(),
            materialUrl = estado.materialUrlActual,
            patronReferencia = patron,
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
