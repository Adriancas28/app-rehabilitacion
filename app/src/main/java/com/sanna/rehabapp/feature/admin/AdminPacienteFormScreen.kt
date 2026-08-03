package com.sanna.rehabapp.feature.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.BotonPrimario
import com.sanna.rehabapp.core.designsystem.CampoTexto
import com.sanna.rehabapp.core.designsystem.ChecklistAgrupado
import com.sanna.rehabapp.core.theme.Spacing
import com.sanna.rehabapp.domain.model.TipoDiagnostico
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AdminPacienteFormScreen(
    onGuardado: () -> Unit,
    onVolver: () -> Unit,
    viewModel: AdminPacienteFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Confirmación visible (Snackbar) antes de volver a la lista, para que
    // el CRUD de paciente no sea una acción silenciosa.
    LaunchedEffect(uiState.guardadoExitoso) {
        if (uiState.guardadoExitoso) {
            // No se espera a que el Snackbar termine de mostrarse (duraría
            // varios segundos) — se lanza aparte y se navega tras una
            // pausa breve, para que alcance a verse antes de salir.
            launch { snackbarHostState.showSnackbar(if (viewModel.esEdicion) "Paciente actualizado." else "Paciente creado.") }
            delay(700)
            onGuardado()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BarraSuperior(
                titulo = if (viewModel.esEdicion) "Editar paciente" else "Registrar paciente",
                onNavegarAtras = onVolver,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.md)
                .verticalScroll(rememberScrollState()),
        ) {
            CampoTexto(
                valor = uiState.nombre,
                onValorCambiado = viewModel::onNombreCambiado,
                etiqueta = "Nombre completo",
            )
            Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
            CampoTexto(
                valor = uiState.email,
                onValorCambiado = viewModel::onEmailCambiado,
                etiqueta = "Correo electrónico",
                tipoTeclado = KeyboardType.Email,
                habilitado = !viewModel.esEdicion,
            )
            if (!viewModel.esEdicion) {
                Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                CampoTexto(
                    valor = uiState.password,
                    onValorCambiado = viewModel::onPasswordCambiado,
                    etiqueta = "Contraseña",
                    esPassword = true,
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
            CampoTexto(
                valor = uiState.dni,
                onValorCambiado = viewModel::onDniCambiado,
                etiqueta = "DNI",
                tipoTeclado = KeyboardType.Number,
            )
            Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
            CampoTexto(
                valor = uiState.edad,
                onValorCambiado = viewModel::onEdadCambiado,
                etiqueta = "Edad",
                tipoTeclado = KeyboardType.Number,
            )
            Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
            Text(text = "Diagnóstico(s)", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(Spacing.sm))
            ChecklistAgrupado(
                opciones = TipoDiagnostico.entries,
                seleccionados = uiState.diagnosticosSeleccionados,
                agruparPor = { it.regionCorporal },
                etiquetaDeOpcion = { it.etiqueta },
                onAlternar = viewModel::onDiagnosticoAlternado,
            )

            uiState.error?.let { mensaje ->
                Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                Text(text = mensaje, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
            BotonPrimario(
                texto = if (viewModel.esEdicion) "Guardar cambios" else "Crear cuenta",
                onClick = viewModel::guardar,
                habilitado = !uiState.guardando && !uiState.cargando,
                cargando = uiState.guardando,
            )
        }
    }
}
