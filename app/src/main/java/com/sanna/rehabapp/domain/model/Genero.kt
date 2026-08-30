package com.sanna.rehabapp.domain.model

// HU20/HU21/HU22/HU23 (actualización del modelo de datos): catálogo cerrado
// de género — dato de contacto, no clínico; no se usa para ninguna lógica
// de análisis de movimiento (a diferencia de LadoAfectado).
enum class Genero(val etiqueta: String) {
    MASCULINO("Masculino"),
    FEMENINO("Femenino"),
    OTRO("Otro"),
    ;

    fun aFirestore(): String = name

    companion object {
        fun desdeFirestoreOrNull(valor: String?): Genero? = entries.find { it.name == valor }
    }
}
