package com.sanna.rehabapp.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val EsquemaClaro = lightColorScheme(
    primary = TealPrimario,
    onPrimary = GrisSuperficie,
    primaryContainer = TealContenedorClaro,
    onPrimaryContainer = TealOnContenedorClaro,
    background = GrisFondo,
    surface = GrisSuperficie,
    onSurface = GrisTextoPrincipal,
    error = ErrorColor,
)

private val EsquemaOscuro = darkColorScheme(
    primary = TealContenedorClaro,
    onPrimary = TealOnContenedorClaro,
    primaryContainer = TealPrimarioOscuro,
    onPrimaryContainer = TealContenedorClaro,
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
        content = content,
    )
}
