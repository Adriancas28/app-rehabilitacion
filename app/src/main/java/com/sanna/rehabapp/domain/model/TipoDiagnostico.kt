package com.sanna.rehabapp.domain.model

// HU01-CA06 (revisión Sprint 3): catálogo cerrado de diagnósticos frecuentes
// en rehabilitación musculoesquelética, en vez de texto libre — permite
// filtrar/agrupar pacientes por diagnóstico de forma consistente.
enum class TipoDiagnostico(val etiqueta: String) {
    TENDINITIS_HOMBRO("Tendinitis de hombro"),
    LESION_RODILLA("Lesión de rodilla"),
    LUMBALGIA("Lumbalgia"),
    POST_OPERATORIO("Post-operatorio"),
    ESGUINCE_TOBILLO("Esguince de tobillo"),
    FORTALECIMIENTO_LUMBAR("Fortalecimiento lumbar"),
    OTRO("Otro");

    fun aFirestore(): String = name

    companion object {
        // Pacientes registrados antes de este cambio pueden no tener este
        // campo (o tenerlo como texto libre); se resuelve a null en vez de
        // lanzar una excepción, igual que Articulacion.desdeFirestoreOrNull.
        fun desdeFirestoreOrNull(valor: String?): TipoDiagnostico? = entries.find { it.name == valor }
    }
}
