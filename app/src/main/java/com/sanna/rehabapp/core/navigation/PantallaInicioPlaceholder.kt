package com.sanna.rehabapp.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Placeholder temporal para el home de cada rol hasta que su HU
// correspondiente (HU01/HU02 fisioterapeuta, HU04 paciente) reemplace esta
// pantalla en un commit posterior.
@Composable
internal fun PantallaInicioPlaceholder(titulo: String, onCerrarSesion: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = titulo, style = MaterialTheme.typography.headlineSmall)
        Text(text = "Próximamente disponible.", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onCerrarSesion, modifier = Modifier.padding(top = 24.dp)) {
            Text("Cerrar sesión")
        }
    }
}
