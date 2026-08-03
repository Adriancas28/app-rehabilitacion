package com.sanna.rehabapp.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val EsquemaClaro = lightColorScheme(
    primary = TealPrimario,
    onPrimary = GrisSuperficie,
    primaryContainer = TealContenedorClaro,
    onPrimaryContainer = TealOnContenedorClaro,
    background = GrisFondo,
    surface = GrisSuperficie,
    onSurface = GrisTextoPrincipal,
    onSurfaceVariant = GrisTextoSecundario,
    outline = GrisBorde,
    error = ErrorColor,
)

private val EsquemaOscuro = darkColorScheme(
    primary = TealContenedorClaro,
    onPrimary = TealOnContenedorClaro,
    primaryContainer = TealPrimarioOscuro,
    onPrimaryContainer = TealContenedorClaro,
)

// Design System (rediseño visual): esquinas muy redondeadas en todo —
// inputs/botones/cajas de error comparten `medium` en el código existente,
// tarjetas usan `large`, chips/elementos compactos usan `small`. Los
// nuevos componentes pill de core/designsystem usan su propia forma fija
// en vez de esta escala (ver Botones.kt).
private val FormasApp = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun AppRehabilitacionTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDarkTheme) EsquemaOscuro else EsquemaClaro
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Tipografia,
        shapes = FormasApp,
        content = content,
    )
}
