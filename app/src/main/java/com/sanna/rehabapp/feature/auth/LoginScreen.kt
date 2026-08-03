package com.sanna.rehabapp.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.R
import com.sanna.rehabapp.core.designsystem.BotonPrimario
import com.sanna.rehabapp.core.designsystem.CampoTexto
import com.sanna.rehabapp.core.theme.Spacing

@Composable
fun LoginScreen(
    onLoginExitoso: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    // Validación client-side mínima (campos no vacíos) para evitar un
    // round-trip al backend innecesario; las credenciales incorrectas
    // se siguen validando del lado del servidor (uiState.error).
    val puedeEnviar = uiState.email.isNotBlank() && uiState.password.isNotBlank()

    LaunchedEffect(uiState.loginExitoso) {
        if (uiState.loginExitoso) onLoginExitoso()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Encabezado proporcional al alto disponible (no un valor fijo en dp):
        // así el logo queda centrado en la franja superior, hasta más o menos
        // la mitad de la pantalla, en vez de quedar pegado arriba en
        // dispositivos más altos. En el mockup esa franja es una foto; acá
        // usamos el color primario ya que no tenemos una foto real todavía.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        ),
                    ),
                    shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.ic_sanna_logo_completo),
                    contentDescription = "Clínica SANNA",
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(32.dp)),
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Rehabilitación en casa",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
        ) {
            CampoTexto(
                valor = uiState.email,
                onValorCambiado = viewModel::onEmailChange,
                etiqueta = "Correo electrónico",
                iconoInicial = Icons.Filled.Email,
                tipoTeclado = KeyboardType.Email,
                imeAction = ImeAction.Next,
            )
            Spacer(modifier = Modifier.height(Spacing.sm + Spacing.xs))
            CampoTexto(
                valor = uiState.password,
                onValorCambiado = viewModel::onPasswordChange,
                etiqueta = "Contraseña",
                iconoInicial = Icons.Filled.Lock,
                esPassword = true,
                imeAction = ImeAction.Done,
                alPresionarIme = { if (puedeEnviar) viewModel.iniciarSesion() },
            )

            uiState.error?.let { mensaje ->
                Spacer(modifier = Modifier.height(Spacing.xs))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium,
                        )
                        .padding(Spacing.sm + Spacing.xs),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = mensaje,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
            BotonPrimario(
                texto = "Iniciar sesión",
                onClick = viewModel::iniciarSesion,
                habilitado = puedeEnviar && !uiState.cargando,
                cargando = uiState.cargando,
            )
        }
    }
}
