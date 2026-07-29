package com.sanna.rehabapp.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val EsquemaClaro = lightColorScheme(
    primary = VerdePrimario,
    onPrimary = GrisSuperficie,
    primaryContainer = VerdeContenedorClaro,
    onPrimaryContainer = VerdeOnContenedorClaro,
    background = GrisFondo,
    surface = GrisSuperficie,
    onSurface = GrisTextoPrincipal,
    error = ErrorColor,
)

private val EsquemaOscuro = darkColorScheme(
    primary = VerdeContenedorClaro,
    onPrimary = VerdeOnContenedorClaro,
    primaryContainer = VerdePrimarioOscuro,
    onPrimaryContainer = VerdeContenedorClaro,
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
