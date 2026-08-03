package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// Design System — botones primario/secundario/outline: forma pill fija
// (independiente de MaterialTheme.shapes, que sigue usando la escala
// general para lo aún no migrado a estos componentes). Toda pantalla
// nueva debe usar estos 3, no crear un Button/OutlinedButton suelto.
private val FormaPillBoton = RoundedCornerShape(percent = 50)
private val AlturaBoton = 52.dp

@Composable
fun BotonPrimario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    icono: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = habilitado && !cargando,
        shape = FormaPillBoton,
        modifier = Modifier.fillMaxWidth().height(AlturaBoton).then(modifier),
    ) {
        when {
            cargando -> CircularProgressIndicator(
                modifier = Modifier.height(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            else -> {
                if (icono != null) {
                    Icon(icono, contentDescription = null, modifier = Modifier.height(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(texto, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
fun BotonSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = habilitado,
        shape = FormaPillBoton,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth().height(AlturaBoton).then(modifier),
    ) {
        Text(texto, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun BotonOutline(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    esDestructivo: Boolean = false,
    icono: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado && !cargando,
        shape = FormaPillBoton,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (esDestructivo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        ),
        modifier = Modifier.fillMaxWidth().height(AlturaBoton).then(modifier),
    ) {
        if (cargando) {
            CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(texto, style = MaterialTheme.typography.titleSmall)
            return@OutlinedButton
        }
        if (icono != null) {
            Icon(icono, contentDescription = null, modifier = Modifier.height(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(texto, style = MaterialTheme.typography.titleSmall)
    }
}
