package com.sanna.rehabapp.domain.model

import java.util.Date

// HU15/HU16 — observación que el fisioterapeuta registra sobre una sesión
// ya realizada; se cuelga de esa sesión (no es independiente), como ya
// estaba definido en el modelo de datos desde Sprint 1.
data class Recomendacion(
    val id: String = "",
    val fisioterapeutaId: String,
    val texto: String,
    val fecha: Date? = null,
)
