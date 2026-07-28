package com.sanna.rehabapp.domain.model

enum class Rol {
    PACIENTE,
    FISIOTERAPEUTA,
    ADMIN;

    fun aFirestore(): String = when (this) {
        PACIENTE -> "paciente"
        FISIOTERAPEUTA -> "fisioterapeuta"
        ADMIN -> "admin"
    }

    companion object {
        fun desdeFirestore(valor: String?): Rol = when (valor) {
            "paciente" -> PACIENTE
            "fisioterapeuta" -> FISIOTERAPEUTA
            "admin" -> ADMIN
            else -> throw IllegalArgumentException("Rol desconocido: $valor")
        }
    }
}
