package com.sanna.rehabapp.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanna.rehabapp.core.designsystem.BotonPrimario
import com.sanna.rehabapp.core.designsystem.TarjetaBase
import com.sanna.rehabapp.core.theme.Spacing
import com.sanna.rehabapp.domain.model.Rol

private val PUNTOS_CONSENTIMIENTO = listOf(
    "El análisis de tu postura mediante la cámara se procesa por completo en tu dispositivo (Edge AI).",
    "Nunca se graba ni se sube video o imágenes a la nube: solo se guardan métricas numéricas (ángulos y porcentajes de ejecución).",
    "Tu fisioterapeuta asignado podrá ver el resultado de tus sesiones para dar seguimiento a tu tratamiento.",
    "Puedes usar la app sin conexión; tus datos se sincronizan cuando recuperas internet.",
)

@Composable
fun ConsentimientoScreen(
    onAceptado: (Rol) -> Unit,
    viewModel: ConsentimientoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.consentimientoOtorgado) {
        if (uiState.consentimientoOtorgado) {
            uiState.rolResuelto?.let(onAceptado)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.PrivacyTip,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(modifier = Modifier.height(Spacing.lg - 4.dp))
        Text(text = "Consentimiento informado", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = "Antes de continuar, es importante que sepas cómo tratamos tu " +
                "información, conforme a la Ley N.° 29733 de Protección de Datos Personales:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(Spacing.lg - 4.dp))
        TarjetaBase {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm + 6.dp)) {
                PUNTOS_CONSENTIMIENTO.forEach { punto ->
                    Row {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm + 2.dp))
                        Text(text = punto, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.lg + 4.dp))
        BotonPrimario(
            texto = "Acepto y quiero continuar",
            onClick = viewModel::aceptar,
            habilitado = !uiState.aceptando && uiState.rolResuelto != null,
            cargando = uiState.aceptando,
        )
    }
}
