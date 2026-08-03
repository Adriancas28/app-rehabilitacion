package com.sanna.rehabapp.core.designsystem

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

// Design System — diálogo de confirmación estándar (eliminar, descartar
// cambios, etc.). Usar este en vez de un AlertDialog armado a mano.
@Composable
fun DialogoConfirmacion(
    titulo: String,
    mensaje: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    textoConfirmar: String = "Confirmar",
    textoCancelar: String = "Cancelar",
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = { TextButton(onClick = onConfirmar) { Text(textoConfirmar) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text(textoCancelar) } },
    )
}
