package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sanna.rehabapp.core.theme.AmbarAlertaContenedor
import com.sanna.rehabapp.core.theme.AmbarAlertaTexto
import com.sanna.rehabapp.core.theme.GrisNeutroContenedor
import com.sanna.rehabapp.core.theme.GrisNeutroTexto
import com.sanna.rehabapp.core.theme.RojoErrorContenedor
import com.sanna.rehabapp.core.theme.RojoErrorTexto
import com.sanna.rehabapp.core.theme.VerdeExitoContenedor
import com.sanna.rehabapp.core.theme.VerdeExitoTexto

// Design System — badge/pill de estado semántico. Reemplaza cualquier
// "Box con background + Text" suelto para mostrar un estado (Activo,
// Completada, Corrige, Error, etc.).
enum class TipoBadge { EXITO, ADVERTENCIA, ERROR, NEUTRO }

@Composable
fun BadgeEstado(texto: String, tipo: TipoBadge, modifier: Modifier = Modifier) {
    val (fondo, contenido) = when (tipo) {
        TipoBadge.EXITO -> VerdeExitoContenedor to VerdeExitoTexto
        TipoBadge.ADVERTENCIA -> AmbarAlertaContenedor to AmbarAlertaTexto
        TipoBadge.ERROR -> RojoErrorContenedor to RojoErrorTexto
        TipoBadge.NEUTRO -> GrisNeutroContenedor to GrisNeutroTexto
    }
    Box(
        modifier = modifier
            .background(fondo, shape = RoundedCornerShape(percent = 50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text = texto, style = MaterialTheme.typography.labelSmall, color = contenido)
    }
}
