package com.sanna.rehabapp.core.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Design System (rediseño visual): escala de espaciado única — para que
// ninguna pantalla nueva invente su propio padding/spacer suelto. Se usa
// como `Spacing.md`, etc. en vez de valores .dp sueltos repetidos.
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
}
