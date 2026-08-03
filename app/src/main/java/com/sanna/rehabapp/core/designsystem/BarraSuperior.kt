package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Design System — barra superior estándar: fondo color primario, texto/
// íconos blancos. Recibe navegación atrás O botón de menú (nunca ambos a
// la vez), y un slot de acciones a la derecha.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperior(
    titulo: String,
    modifier: Modifier = Modifier,
    onNavegarAtras: (() -> Unit)? = null,
    onAlternarMenu: (() -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(titulo) },
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        navigationIcon = {
            when {
                onNavegarAtras != null -> IconButton(onClick = onNavegarAtras) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                }
                onAlternarMenu != null -> IconButton(onClick = onAlternarMenu) {
                    Icon(Icons.Filled.Menu, contentDescription = "Mostrar/ocultar menú")
                }
            }
        },
        actions = acciones,
    )
}
