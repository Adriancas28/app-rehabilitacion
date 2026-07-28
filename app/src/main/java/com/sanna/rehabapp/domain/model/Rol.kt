package com.sanna.rehabapp.domain.model

enum class Rol {
    PACIENTE,
    FISIOTERAPEUTA;

    fun aFirestore(): String = when (this) {
        PACIENTE -> "paciente"
        FISIOTERAPEUTA -> "fisioterapeuta"
    }

    companion object {
        fun desdeFirestore(valor: String?): Rol = when (valor) {
            "paciente" -> PACIENTE
            "fisioterapeuta" -> FISIOTERAPEUTA
            else -> throw IllegalArgumentException("Rol desconocido: $valor")
        }
    }
}
