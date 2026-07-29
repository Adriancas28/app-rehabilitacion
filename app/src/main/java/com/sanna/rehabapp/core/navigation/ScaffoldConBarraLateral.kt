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
// propias pestañas. `menuVisible` se recibe desde afuera (hoisted en
// RehabNavHost) en vez de manejarse aquí con `remember`, porque cada
// pantalla de la barra es un destino de navegación distinto: si el
// estado viviera dentro de este composable, se reiniciaría a "visible"
// cada vez que Compose monta la pantalla destino, y la barra se vería
// siempre abierta pese a haberse plegado antes de navegar.
@Composable
fun ScaffoldConBarraLateral(
    menuVisible: Boolean,
    onCambiarMenuVisible: (Boolean) -> Unit,
    items: List<ItemBarraLateral>,
    topBar: @Composable (onAlternarMenu: () -> Unit) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        if (menuVisible) {
            NavigationRail {
                items.forEach { item ->
                    NavigationRailItem(
                        selected = item.seleccionado,
                        onClick = {
                            item.onClick()
                            onCambiarMenuVisible(false)
                        },
                        icon = { Icon(item.icono, contentDescription = null) },
                        label = { Text(item.etiqueta) },
                    )
                }
            }
        }
        Scaffold(
            modifier = Modifier.weight(1f),
            topBar = { topBar { onCambiarMenuVisible(!menuVisible) } },
        ) { padding -> content(padding) }
    }
}
