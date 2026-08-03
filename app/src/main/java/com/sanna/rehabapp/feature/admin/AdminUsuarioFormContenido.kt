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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sanna.rehabapp.core.designsystem.BarraSuperior
import com.sanna.rehabapp.core.designsystem.BotonPrimario
import com.sanna.rehabapp.core.designsystem.CampoTexto
import com.sanna.rehabapp.core.theme.Spacing

@Composable
fun AdminUsuarioFormContenido(
    titulo: String,
    esEdicion: Boolean,
    uiState: AdminUsuarioFormUiState,
    onNombreCambiado: (String) -> Unit,
    onEmailCambiado: (String) -> Unit,
    onPasswordCambiado: (String) -> Unit,
    onGuardar: () -> Unit,
    onVolver: () -> Unit,
) {
    Scaffold(
        topBar = { BarraSuperior(titulo = titulo, onNavegarAtras = onVolver) },
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
                onValorCambiado = onNombreCambiado,
                etiqueta = "Nombre completo",
            )
            Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
            CampoTexto(
                valor = uiState.email,
                onValorCambiado = onEmailCambiado,
                etiqueta = "Correo electrónico",
                tipoTeclado = KeyboardType.Email,
                habilitado = !esEdicion,
            )
            if (!esEdicion) {
                Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                CampoTexto(
                    valor = uiState.password,
                    onValorCambiado = onPasswordCambiado,
                    etiqueta = "Contraseña",
                    esPassword = true,
                )
            }

            uiState.error?.let { mensaje ->
                Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
                Text(text = mensaje, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
            BotonPrimario(
                texto = if (esEdicion) "Guardar cambios" else "Crear cuenta",
                onClick = onGuardar,
                habilitado = !uiState.guardando && !uiState.cargando,
                cargando = uiState.guardando,
            )
        }
    }
}
