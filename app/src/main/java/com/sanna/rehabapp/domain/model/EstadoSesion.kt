package com.sanna.rehabapp.domain.model

enum class EstadoSesion {
    PENDIENTE,
    COMPLETADA;

    fun aFirestore(): String = when (this) {
        PENDIENTE -> "asignada"
        COMPLETADA -> "completada"
    }

    companion object {
        fun desdeFirestore(valor: String?): EstadoSesion = when (valor) {
            "asignada", "en curso", "pendiente" -> PENDIENTE
            "completada" -> COMPLETADA
            else -> throw IllegalArgumentException("Estado desconocido: $valor")
        }
    }
}
