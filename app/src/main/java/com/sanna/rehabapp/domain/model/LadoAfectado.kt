package com.sanna.rehabapp.domain.model

// HU20 (ampliación): lado del cuerpo que el administrador indica como
// afectado al registrar al paciente. El sistema lo usa únicamente para
// decidir qué lado (derecho/izquierdo) medir durante el monitoreo (HU07/
// HU08) — nunca lo infiere ni lo decide por su cuenta, coherente con que
// esta app es de seguimiento, no de diagnóstico: el dato siempre lo define
// el administrador.
enum class LadoAfectado(val etiqueta: String) {
    DERECHO("Derecho"),
    IZQUIERDO("Izquierdo"),
    AMBOS("Ambos"),
    ;

    fun aFirestore(): String = name

    companion object {
        fun desdeFirestoreOrNull(valor: String?): LadoAfectado? = entries.find { it.name == valor }
    }
}
