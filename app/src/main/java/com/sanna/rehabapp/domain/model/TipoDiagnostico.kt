package com.sanna.rehabapp.domain.model

// Catálogo de diagnósticos del MVP (reemplaza al catálogo anterior de 13
// valores): 3 condiciones musculoesqueléticas, elegidas con respaldo
// bibliográfico real (OMS, Guía de Práctica Clínica de EsSalud 2025, y un
// estudio de 366 pacientes en un centro de rehabilitación de Lima) y
// viabilidad técnica con MediaPipe Pose — ver
// "Catalogo_Ejercicios_MVP_SANNA_Especificaciones.docx". La app no genera
// diagnósticos: el fisioterapeuta registra el que ya obtuvo de su propia
// evaluación clínica; el sistema solo lo usa para sugerir ejercicios
// relacionados (HU03-CA07), nunca para decidir por su cuenta.
enum class TipoDiagnostico(val etiqueta: String, val regionCorporal: String) {
    LUMBALGIA_INESPECIFICA("Lumbalgia inespecífica", "Columna"),
    OSTEOARTROSIS_RODILLA("Osteoartritis de rodilla (gonartrosis)", "Rodilla"),
    SINDROME_DOLOR_SUBACROMIAL("Síndrome de dolor subacromial / hombro doloroso", "Hombro");

    fun aFirestore(): String = name

    companion object {
        // Pacientes registrados antes de este cambio (o con el catálogo
        // genérico anterior de 7 valores) se resuelven a null en vez de
        // lanzar una excepción, igual que Articulacion.desdeFirestoreOrNull.
        fun desdeFirestoreOrNull(valor: String?): TipoDiagnostico? = entries.find { it.name == valor }
    }
}
