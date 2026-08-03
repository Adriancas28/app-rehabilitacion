package com.sanna.rehabapp.core.theme

import androidx.compose.ui.graphics.Color

// Design System (rediseño visual, ampliación acordada): paleta extraída de
// la imagen de referencia de 10 pantallas — ver CLAUDE.md, sección
// "Design System". Reemplaza la paleta verde anterior del logo.
val TealPrimario = Color(0xFF12A79B)
val TealPrimarioOscuro = Color(0xFF0C7A71)
val TealContenedorClaro = Color(0xFFD6F1EE)
val TealOnContenedorClaro = Color(0xFF07332F)
val GrisFondo = Color(0xFFF5F6F8)
val GrisSuperficie = Color(0xFFFFFFFF)
val GrisTextoPrincipal = Color(0xFF1F2937)
val GrisTextoSecundario = Color(0xFF6B7280)
val GrisBorde = Color(0xFFE3E5E8)
val ErrorColor = Color(0xFFD64545)

// Colores semánticos (no son el acento de marca): estados tipo píldora,
// alertas suaves, etc. — para no reutilizar el teal donde el significado
// es distinto (éxito/advertencia/error/neutro).
val VerdeExitoContenedor = Color(0xFFE3F3E9)
val VerdeExitoTexto = Color(0xFF1E7A46)
val AmbarAlertaContenedor = Color(0xFFFBF0DF)
val AmbarAlertaTexto = Color(0xFFB26A00)
val RojoErrorContenedor = Color(0xFFFBE4E1)
val RojoErrorTexto = Color(0xFFB23B23)
val GrisNeutroContenedor = Color(0xFFE9EAEC)
val GrisNeutroTexto = Color(0xFF5B6069)
