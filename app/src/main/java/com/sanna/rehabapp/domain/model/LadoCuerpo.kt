package com.sanna.rehabapp.domain.model

// Lado del cuerpo con el que está definida una acción (HU02). A diferencia de
// LadoAfectado (dato del paciente), aquí no existe "Ambos": una acción mide un
// único segmento; para el otro lado se usa su espejo (Accion.paraLado).
enum class LadoCuerpo(val etiqueta: String) {
    DERECHO("Derecho"),
    IZQUIERDO("Izquierdo"),
    ;

    fun opuesto(): LadoCuerpo = if (this == DERECHO) IZQUIERDO else DERECHO

    fun aFirestore(): String = etiqueta

    companion object {
        fun desdeFirestoreOrNull(valor: String?): LadoCuerpo? =
            entries.find { it.name.equals(valor, true) || it.etiqueta.equals(valor, true) }
    }
}
