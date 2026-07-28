package com.sanna.rehabapp.core.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

data class ItemBarraLateral(
    val etiqueta: String,
    val icono: ImageVector,
    val seleccionado: Boolean,
    val onClick: () -> Unit,
)

// Barra lateral izquierda plegable (como el mockup de referencia),
// compartida por fisioterapeuta y administrador — cada uno con sus
// propias pestañas. Se pliega sola después de elegir una, o con el
// ícono de menú (☰) que cada pantalla agrega en su propio topBar.
@Composable
fun ScaffoldConBarraLateral(
    items: List<ItemBarraLateral>,
    topBar: @Composable (onAlternarMenu: () -> Unit) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    var menuVisible by remember { mutableStateOf(true) }

    Row(modifier = Modifier.fillMaxSize()) {
        if (menuVisible) {
            NavigationRail {
                items.forEach { item ->
                    NavigationRailItem(
                        selected = item.seleccionado,
                        onClick = {
                            item.onClick()
                            menuVisible = false
                        },
                        icon = { Icon(item.icono, contentDescription = null) },
                        label = { Text(item.etiqueta) },
                    )
                }
            }
        }
        Scaffold(
            modifier = Modifier.weight(1f),
            topBar = { topBar { menuVisible = !menuVisible } },
        ) { padding -> content(padding) }
    }
}
