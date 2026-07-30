package com.sanna.rehabapp.domain.model

// Catálogo de diagnósticos (ampliación acordada): 13 diagnósticos frecuentes
// en rehabilitación musculoesquelética, agrupados por región corporal — más
// específico clínicamente que el catálogo genérico anterior de 7 valores.
// La app no genera diagnósticos: el fisioterapeuta registra el que ya
// obtuvo de su propia evaluación clínica; el sistema solo lo usa para
// sugerir ejercicios relacionados (HU03-CA05), nunca para decidir por su
// cuenta.
enum class TipoDiagnostico(val etiqueta: String, val regionCorporal: String) {
    PINZAMIENTO_SUBACROMIAL("Síndrome de pinzamiento subacromial", "Hombro"),
    CAPSULITIS_ADHESIVA("Capsulitis adhesiva (hombro congelado)", "Hombro"),
    POST_QUIRURGICO_MANGUITO_ROTADOR("Rehabilitación post-quirúrgica de manguito rotador", "Hombro"),
    RIGIDEZ_POSTRAUMATICA_CODO("Rigidez postraumática de codo", "Codo"),
    POST_ARTROPLASTIA_CADERA("Rehabilitación post-artroplastia de cadera", "Cadera"),
    OSTEOARTROSIS_CADERA("Osteoartrosis de cadera", "Cadera"),
    POST_RECONSTRUCCION_LCA("Post-reconstrucción de ligamento cruzado anterior", "Rodilla"),
    OSTEOARTROSIS_RODILLA("Osteoartrosis de rodilla", "Rodilla"),
    SINDROME_DOLOR_FEMOROPATELAR("Síndrome de dolor femoropatelar", "Rodilla"),
    ESGUINCE_TOBILLO("Esguince de tobillo (fase funcional)", "Tobillo"),
    LUMBALGIA_MECANICA("Lumbalgia mecánica / dolor lumbar inespecífico", "Columna"),
    DEBILIDAD_MUSCULAR("Debilidad muscular / desacondicionamiento físico", "General"),
    ALTERACIONES_EQUILIBRIO("Alteraciones del equilibrio / riesgo de caídas", "General");

    fun aFirestore(): String = name

    companion object {
        // Pacientes registrados antes de este cambio (o con el catálogo
        // genérico anterior de 7 valores) se resuelven a null en vez de
        // lanzar una excepción, igual que Articulacion.desdeFirestoreOrNull.
        fun desdeFirestoreOrNull(valor: String?): TipoDiagnostico? = entries.find { it.name == valor }
    }
}
