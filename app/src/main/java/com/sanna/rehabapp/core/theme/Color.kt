package com.sanna.rehabapp.core.theme

import androidx.compose.ui.graphics.Color

// Paleta extraída del logo real de la clínica (sanna.png) — verde #00B011.
val VerdePrimario = Color(0xFF00B011)
val VerdePrimarioOscuro = Color(0xFF007A0C)
val VerdeContenedorClaro = Color(0xFFC8F5C9)
val VerdeOnContenedorClaro = Color(0xFF072B0A)
val GrisFondo = Color(0xFFFAFDFA)
val GrisSuperficie = Color(0xFFFFFFFF)
val GrisTextoPrincipal = Color(0xFF1A1C19)
val GrisTextoSecundario = Color(0xFF49493F)
val GrisBorde = Color(0xFFDCE5D8)
val ErrorColor = Color(0xFFBA1A1A)

// Colores semánticos (no son el acento de marca): estados tipo píldora,
// alertas suaves, etc. — para no reutilizar el verde donde el significado
// es distinto (éxito/advertencia). El verde de éxito se mantiene separado
// del verde de marca aunque sean visualmente cercanos, porque comunican
// cosas distintas (marca vs. resultado positivo).
val VerdeExitoContenedor = Color(0xFFE3F3E9)
val VerdeExitoTexto = Color(0xFF1E7A46)
val AmbarAlertaContenedor = Color(0xFFFBF0DF)
val AmbarAlertaTexto = Color(0xFFB26A00)
