package com.sanna.rehabapp.domain.model

// Guía visual en vivo (HU02): posición en que el paciente debe ubicarse frente
// a la cámara para que el ejercicio se pueda medir. Cada ejercicio define una.
enum class VistaCamara(val etiqueta: String) {
    LATERAL("Lateral"),
    FRONTAL("Frontal"),
    OBLICUA("Oblicua"),
    ;

    fun aFirestore(): String = etiqueta

    companion object {
        fun desdeFirestoreOrNull(valor: String?): VistaCamara? =
            entries.find { it.name.equals(valor, true) || it.etiqueta.equals(valor, true) }
    }
}
