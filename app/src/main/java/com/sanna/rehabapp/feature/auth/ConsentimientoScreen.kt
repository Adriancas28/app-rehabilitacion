package com.sanna.rehabapp.feature.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(text = "Consentimiento informado", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Antes de continuar, es importante que sepas cómo tratamos tu " +
                "información, conforme a la Ley N.° 29733 de Protección de Datos Personales:",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        PUNTOS_CONSENTIMIENTO.forEach { punto ->
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(text = "• ", style = MaterialTheme.typography.bodyMedium)
                Text(text = punto, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = viewModel::aceptar,
            enabled = !uiState.aceptando && uiState.rolResuelto != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState.aceptando) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Acepto y quiero continuar")
            }
        }
    }
}
