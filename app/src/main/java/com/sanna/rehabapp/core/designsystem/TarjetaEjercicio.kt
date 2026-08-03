package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// Design System — tarjeta de ejercicio para grillas de 2 columnas:
// cabecera con ícono destacado + menú "⋮" opcional, nombre, y hasta 2
// líneas de metadatos (categoría, duración, nivel, etc.).
@Composable
fun TarjetaEjercicio(
    icono: ImageVector,
    nombre: String,
    lineaSecundaria: String,
    modifier: Modifier = Modifier,
    lineaTerciaria: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    menu: (@Composable (cerrar: () -> Unit) -> Unit)? = null,
) {
    var menuAbierto by remember { mutableStateOf(false) }
    val elevacion = CardDefaults.cardElevation(defaultElevation = 2.dp)
    val forma = MaterialTheme.shapes.large

    @Composable
    fun contenido() {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                Icon(
                    icono,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp),
                )
                if (menu != null) {
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        IconButton(onClick = { menuAbierto = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "Más opciones",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                            menu { menuAbierto = false }
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = nombre,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lineaSecundaria,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (lineaTerciaria != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) { lineaTerciaria() }
                }
            }
        }
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            elevation = elevacion,
            shape = forma,
            modifier = modifier.fillMaxWidth(),
        ) { contenido() }
    } else {
        Card(
            elevation = elevacion,
            shape = forma,
            modifier = modifier.fillMaxWidth(),
        ) { contenido() }
    }
}
