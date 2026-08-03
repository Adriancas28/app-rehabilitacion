package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

// Design System — campo de texto genérico. Toda pantalla nueva usa este
// en vez de un OutlinedTextField suelto. Cuando `esPassword = true`,
// resuelve internamente el toggle de mostrar/ocultar contraseña (ícono de
// ojo) — antes ese estado y ese ícono se repetían a mano en cada
// formulario que pedía contraseña.
@Composable
fun CampoTexto(
    valor: String,
    onValorCambiado: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    iconoInicial: ImageVector? = null,
    esPassword: Boolean = false,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    soloUnaLinea: Boolean = true,
    lineasMinimas: Int = 1,
    habilitado: Boolean = true,
    mensajeError: String? = null,
) {
    var mostrarPassword by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = valor,
        onValueChange = onValorCambiado,
        label = { Text(etiqueta) },
        singleLine = soloUnaLinea,
        minLines = lineasMinimas,
        enabled = habilitado,
        isError = mensajeError != null,
        supportingText = mensajeError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        leadingIcon = iconoInicial?.let { { Icon(it, contentDescription = null) } },
        trailingIcon = if (esPassword) {
            {
                IconButton(onClick = { mostrarPassword = !mostrarPassword }) {
                    Icon(
                        imageVector = if (mostrarPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (mostrarPassword) "Ocultar contraseña" else "Ver contraseña",
                    )
                }
            }
        } else {
            null
        },
        visualTransformation = if (esPassword && !mostrarPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(keyboardType = if (esPassword) KeyboardType.Password else tipoTeclado),
        modifier = modifier.fillMaxWidth(),
    )
}
