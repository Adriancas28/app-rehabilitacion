package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

// Design System — selector de una opción de una lista cerrada (catálogo,
// enum, etc.). Reemplaza el patrón repetido "Box + OutlinedTextField
// readOnly + Box clickable superpuesto + DropdownMenu" que existía suelto
// en varias pantallas (selector de articulación, ejercicio, diagnóstico...).
// No usar para texto libre — eso sigue siendo un OutlinedTextField normal.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectorDropdown(
    valorSeleccionado: T?,
    opciones: List<T>,
    etiquetaDeOpcion: (T) -> String,
    onSeleccionar: (T) -> Unit,
    modifier: Modifier = Modifier,
    etiqueta: String? = null,
    placeholder: String = "Selecciona una opción",
    habilitado: Boolean = true,
) {
    var menuAbierto by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = valorSeleccionado?.let(etiquetaDeOpcion) ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = habilitado,
            label = etiqueta?.let { { Text(it) } },
            placeholder = { Text(placeholder) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (habilitado) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { menuAbierto = true },
            )
        }
        DropdownMenu(
            expanded = menuAbierto,
            onDismissRequest = { menuAbierto = false },
            modifier = Modifier.fillMaxWidth(),
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(etiquetaDeOpcion(opcion)) },
                    onClick = {
                        onSeleccionar(opcion)
                        menuAbierto = false
                    },
                )
            }
        }
    }
}
